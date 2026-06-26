# MockGPS Standalone — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standalone Android App that mocks GPS + NETWORK location without a PC — map UI, place search, static + walker modes, favorites/recents.

**Architecture:** Single-Activity Compose app. `MockLocationService` is a Foreground Service with static and walker modes. `MainViewModel` binds to the service and exposes `UiState`. `Room` persists favorites and recents. OSMDroid renders the map inside an `AndroidView`.

**Tech Stack:** Kotlin 1.9.24, Compose BOM 2024.06.00, OSMDroid 6.1.18, Nominatim (java.net.URL + org.json), Room 2.6.1, KSP 1.9.24-1.0.20, Coroutines 1.8.1

## Global Constraints

- `minSdk = 31` (`ProviderProperties` API requirement)
- `targetSdk = 35`, `compileSdk = 35`
- Package: `com.mockgps.standalone`
- Project root (= Android root): `~/Desktop/MockGPS-standalone/`
- All user-visible strings in `app/src/main/res/values/strings.xml`
- Commit after every task: `feat: <task description>`
- Local unit tests: `./gradlew test`
- Device tests (requires connected device): `./gradlew connectedAndroidTest`

---

## File Map

```
MockGPS-standalone/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts                       (root)
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/mockgps/standalone/
        │   │   ├── MockGpsApplication.kt         OSMDroid 初始化
        │   │   ├── MainActivity.kt               Service bind + 電池豁免提示
        │   │   ├── util/
        │   │   │   └── GeoMath.kt                方位角 / 距離 / 目標點計算
        │   │   ├── data/
        │   │   │   ├── model/
        │   │   │   │   ├── FavoriteEntity.kt
        │   │   │   │   └── RecentEntity.kt
        │   │   │   ├── db/
        │   │   │   │   ├── FavoriteDao.kt
        │   │   │   │   ├── RecentDao.kt
        │   │   │   │   └── AppDatabase.kt
        │   │   │   └── repository/
        │   │   │       └── LocationRepository.kt
        │   │   ├── service/
        │   │   │   ├── MockLocationService.kt    Static + Walker 模式
        │   │   │   └── WalkerState.kt            Walker 步進邏輯（可獨立測試）
        │   │   └── ui/
        │   │       ├── MainViewModel.kt
        │   │       ├── screen/
        │   │       │   └── MainScreen.kt         頂層組合
        │   │       └── component/
        │   │           ├── OsmMapView.kt         OSMDroid AndroidView 包裝
        │   │           ├── BottomSheetContent.kt 座標 / 模式 / Walker 設定
        │   │           ├── SearchBar.kt          Nominatim 搜尋
        │   │           └── LocationList.kt       收藏 / 最近 Tabs
        │   └── res/
        │       └── values/
        │           ├── strings.xml
        │           └── themes.xml
        ├── test/
        │   └── java/com/mockgps/standalone/
        │       ├── util/GeoMathTest.kt
        │       └── service/WalkerStateTest.kt
        └── androidTest/
            └── java/com/mockgps/standalone/
                └── data/LocationRepositoryTest.kt
```

---

### Task 1: Project Scaffold

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/mockgps/standalone/MockGpsApplication.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

**Interfaces:**
- Produces: buildable Android project skeleton, all subsequent tasks add files inside this structure

- [ ] **Step 1: Create `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.4.2"
kotlin = "1.9.24"
ksp = "1.9.24-1.0.20"
coreKtx = "1.13.1"
activityCompose = "1.9.0"
composeBom = "2024.06.00"
lifecycleRuntimeKtx = "2.8.2"
room = "2.6.1"
coroutines = "1.8.1"
osmdroid = "6.1.18"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
osmdroid = { group = "org.osmdroid", name = "osmdroid-android", version.ref = "osmdroid" }
junit = { group = "junit", name = "junit", version = "4.13.2" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version = "1.2.1" }
androidx-test-runner = { group = "androidx.test", name = "runner", version = "1.6.1" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Create `gradle/wrapper/gradle-wrapper.properties`**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 3: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MockGPS"
include(":app")
```

- [ ] **Step 4: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 5: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 6: Create `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mockgps.standalone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mockgps.standalone"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.osmdroid)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
```

- [ ] **Step 7: Create `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".MockGpsApplication"
        android:label="@string/app_name"
        android:theme="@style/Theme.MockGPS"
        android:allowBackup="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.MockLocationService"
            android:foregroundServiceType="location"
            android:exported="false" />
    </application>
</manifest>
```

- [ ] **Step 8: Create `app/src/main/java/com/mockgps/standalone/MockGpsApplication.kt`**

```kotlin
package com.mockgps.standalone

import android.app.Application
import org.osmdroid.config.Configuration

class MockGpsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            load(this@MockGpsApplication, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "MockGPS-Standalone/1.0"
        }
    }
}
```

- [ ] **Step 9: Create `app/src/main/res/values/strings.xml`**

```xml
<resources>
    <string name="app_name">MockGPS</string>
    <string name="notif_channel_name">Mock GPS</string>
    <string name="notif_action_stop">停止</string>
    <string name="status_idle">待機中</string>
    <string name="status_static">靜態模擬中 %1$.5f, %2$.5f</string>
    <string name="status_walker">漫步中 %1$.5f, %2$.5f</string>
    <string name="status_no_provider">⚠️ 請先在開發人員選項選此 App 為模擬位置</string>
    <string name="btn_start">開始</string>
    <string name="btn_stop">停止</string>
    <string name="mode_static">靜態</string>
    <string name="mode_walker">漫步</string>
    <string name="label_latitude">緯度</string>
    <string name="label_longitude">經度</string>
    <string name="label_radius">半徑</string>
    <string name="label_speed">速度</string>
    <string name="tab_favorites">收藏</string>
    <string name="tab_recents">最近</string>
    <string name="search_placeholder">搜尋地點…</string>
    <string name="goto_dev_settings">前往開發人員選項</string>
    <string name="battery_dialog_title">建議關閉電池最佳化</string>
    <string name="battery_dialog_body">為避免 Android 在背景將服務終止，請允許此 App 不受電池限制。</string>
    <string name="battery_dialog_ok">前往設定</string>
    <string name="battery_dialog_skip">略過</string>
    <string name="copied">已複製座標</string>
</resources>
```

- [ ] **Step 10: Create `app/src/main/res/values/themes.xml`**

```xml
<resources>
    <style name="Theme.MockGPS" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 11: Download gradlew**

Run from `~/Desktop/MockGPS-standalone/`:
```bash
gradle wrapper --gradle-version 8.7
```
If Gradle is not installed locally, copy `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` from the existing `~/Desktop/Pikmin-auto/android-app/` project.

```bash
cp ~/Desktop/Pikmin-auto/android-app/gradlew ~/Desktop/MockGPS-standalone/
cp ~/Desktop/Pikmin-auto/android-app/gradlew.bat ~/Desktop/MockGPS-standalone/
cp ~/Desktop/Pikmin-auto/android-app/gradle/wrapper/gradle-wrapper.jar \
   ~/Desktop/MockGPS-standalone/gradle/wrapper/
```

- [ ] **Step 12: Verify sync**

```bash
cd ~/Desktop/MockGPS-standalone
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 13: Commit**

```bash
git add .
git commit -m "feat: project scaffold — build files, manifest, Application class"
```

---

### Task 2: GeoMath Utility

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/util/GeoMath.kt`
- Create: `app/src/test/java/com/mockgps/standalone/util/GeoMathTest.kt`

**Interfaces:**
- Produces:
  - `GeoMath.distanceMeters(lat1, lon1, lat2, lon2): Double`
  - `GeoMath.bearing(lat1, lon1, lat2, lon2): Double` — degrees [0, 360)
  - `GeoMath.destination(lat, lon, bearingDeg, distanceM): Pair<Double, Double>`
  - `GeoMath.randomPointInCircle(centerLat, centerLon, radiusM): Pair<Double, Double>`

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/mockgps/standalone/util/GeoMathTest.kt
package com.mockgps.standalone.util

import org.junit.Assert.*
import org.junit.Test

class GeoMathTest {

    @Test
    fun `distanceMeters same point is zero`() {
        assertEquals(0.0, GeoMath.distanceMeters(25.0, 121.0, 25.0, 121.0), 0.001)
    }

    @Test
    fun `distanceMeters Taipei to Kaohsiung approx 307km`() {
        val d = GeoMath.distanceMeters(25.0330, 121.5654, 22.6273, 120.3014)
        assertTrue("expected ~307000 but was $d", d in 290_000.0..320_000.0)
    }

    @Test
    fun `bearing due north is 0 degrees`() {
        assertEquals(0.0, GeoMath.bearing(0.0, 0.0, 1.0, 0.0), 1.0)
    }

    @Test
    fun `bearing due east is 90 degrees`() {
        assertEquals(90.0, GeoMath.bearing(0.0, 0.0, 0.0, 1.0), 1.0)
    }

    @Test
    fun `bearing due south is 180 degrees`() {
        assertEquals(180.0, GeoMath.bearing(1.0, 0.0, 0.0, 0.0), 1.0)
    }

    @Test
    fun `destination 1000m north increases latitude`() {
        val (lat2, lon2) = GeoMath.destination(25.0, 121.0, 0.0, 1000.0)
        assertTrue(lat2 > 25.0)
        assertEquals(121.0, lon2, 0.001)
    }

    @Test
    fun `destination round-trip is close to origin`() {
        val (lat2, lon2) = GeoMath.destination(25.0330, 121.5654, 45.0, 500.0)
        val (lat3, lon3) = GeoMath.destination(lat2, lon2, 225.0, 500.0)
        assertEquals(25.0330, lat3, 0.0001)
        assertEquals(121.5654, lon3, 0.0001)
    }

    @Test
    fun `randomPointInCircle stays within radius`() {
        repeat(200) {
            val (lat, lon) = GeoMath.randomPointInCircle(25.0330, 121.5654, 300.0)
            val d = GeoMath.distanceMeters(25.0330, 121.5654, lat, lon)
            assertTrue("point $d m from center, expected ≤ 300", d <= 301.0)
        }
    }
}
```

- [ ] **Step 2: Run tests — verify FAIL**

```bash
./gradlew test --tests "com.mockgps.standalone.util.GeoMathTest"
```
Expected: `FAILED` (GeoMath not found)

- [ ] **Step 3: Implement `GeoMath.kt`**

```kotlin
// app/src/main/java/com/mockgps/standalone/util/GeoMath.kt
package com.mockgps.standalone.util

import kotlin.math.*
import kotlin.random.Random

object GeoMath {
    private const val R = 6_371_000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val φ1 = Math.toRadians(lat1); val φ2 = Math.toRadians(lat2)
        val dφ = Math.toRadians(lat2 - lat1)
        val dλ = Math.toRadians(lon2 - lon1)
        val a = sin(dφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(dλ / 2).pow(2)
        return 2 * R * asin(sqrt(a))
    }

    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val φ1 = Math.toRadians(lat1); val φ2 = Math.toRadians(lat2)
        val dλ = Math.toRadians(lon2 - lon1)
        val y = sin(dλ) * cos(φ2)
        val x = cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(dλ)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun destination(lat: Double, lon: Double, bearingDeg: Double, distanceM: Double): Pair<Double, Double> {
        val φ1 = Math.toRadians(lat); val λ1 = Math.toRadians(lon)
        val θ = Math.toRadians(bearingDeg); val δ = distanceM / R
        val φ2 = asin(sin(φ1) * cos(δ) + cos(φ1) * sin(δ) * cos(θ))
        val λ2 = λ1 + atan2(sin(θ) * sin(δ) * cos(φ1), cos(δ) - sin(φ1) * sin(φ2))
        return Pair(Math.toDegrees(φ2), Math.toDegrees(λ2))
    }

    fun randomPointInCircle(centerLat: Double, centerLon: Double, radiusM: Double): Pair<Double, Double> {
        val r = radiusM * sqrt(Random.nextDouble())
        val θ = Random.nextDouble() * 360.0
        return destination(centerLat, centerLon, θ, r)
    }
}
```

- [ ] **Step 4: Run tests — verify PASS**

```bash
./gradlew test --tests "com.mockgps.standalone.util.GeoMathTest"
```
Expected: `8 tests, 8 passed`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/util/GeoMath.kt \
        app/src/test/java/com/mockgps/standalone/util/GeoMathTest.kt
git commit -m "feat: GeoMath utility — haversine, bearing, destination, randomPointInCircle"
```

---

### Task 3: Data Layer

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/data/model/FavoriteEntity.kt`
- Create: `app/src/main/java/com/mockgps/standalone/data/model/RecentEntity.kt`
- Create: `app/src/main/java/com/mockgps/standalone/data/db/FavoriteDao.kt`
- Create: `app/src/main/java/com/mockgps/standalone/data/db/RecentDao.kt`
- Create: `app/src/main/java/com/mockgps/standalone/data/db/AppDatabase.kt`
- Create: `app/src/main/java/com/mockgps/standalone/data/repository/LocationRepository.kt`
- Create: `app/src/androidTest/java/com/mockgps/standalone/data/LocationRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `LocationRepository(db: AppDatabase)` — constructor
  - `LocationRepository.favorites: Flow<List<FavoriteEntity>>`
  - `LocationRepository.recents: Flow<List<RecentEntity>>`
  - `LocationRepository.addFavorite(name: String, lat: Double, lon: Double)`
  - `LocationRepository.deleteFavorite(id: Long)`
  - `LocationRepository.addRecent(name: String?, lat: Double, lon: Double)`
  - `AppDatabase.instance(context): AppDatabase` — companion singleton
- Consumed by: `MainViewModel` (Task 6)

- [ ] **Step 1: Write entities**

```kotlin
// FavoriteEntity.kt
package com.mockgps.standalone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lat: Double,
    val lon: Double,
    val createdAt: Long = System.currentTimeMillis()
)
```

```kotlin
// RecentEntity.kt
package com.mockgps.standalone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val lat: Double,
    val lon: Double,
    val usedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: Write DAOs**

```kotlin
// FavoriteDao.kt
package com.mockgps.standalone.data.db

import androidx.room.*
import com.mockgps.standalone.data.model.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

```kotlin
// RecentDao.kt
package com.mockgps.standalone.data.db

import androidx.room.*
import com.mockgps.standalone.data.model.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY usedAt DESC LIMIT 10")
    fun getLatest(): Flow<List<RecentEntity>>

    @Insert
    suspend fun insert(entity: RecentEntity)

    @Query("DELETE FROM recents WHERE id NOT IN (SELECT id FROM recents ORDER BY usedAt DESC LIMIT 10)")
    suspend fun trimToTen()
}
```

- [ ] **Step 3: Create AppDatabase**

```kotlin
// AppDatabase.kt
package com.mockgps.standalone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mockgps.standalone.data.model.FavoriteEntity
import com.mockgps.standalone.data.model.RecentEntity

@Database(entities = [FavoriteEntity::class, RecentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun instance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mockgps.db")
                .build().also { INSTANCE = it }
        }
    }
}
```

- [ ] **Step 4: Create LocationRepository**

```kotlin
// LocationRepository.kt
package com.mockgps.standalone.data.repository

import com.mockgps.standalone.data.db.AppDatabase
import com.mockgps.standalone.data.model.FavoriteEntity
import com.mockgps.standalone.data.model.RecentEntity

class LocationRepository(db: AppDatabase) {
    private val favoriteDao = db.favoriteDao()
    private val recentDao = db.recentDao()

    val favorites = favoriteDao.getAll()
    val recents = recentDao.getLatest()

    suspend fun addFavorite(name: String, lat: Double, lon: Double) {
        favoriteDao.insert(FavoriteEntity(name = name, lat = lat, lon = lon))
    }

    suspend fun deleteFavorite(id: Long) {
        favoriteDao.deleteById(id)
    }

    suspend fun addRecent(name: String?, lat: Double, lon: Double) {
        recentDao.insert(RecentEntity(name = name, lat = lat, lon = lon))
        recentDao.trimToTen()
    }
}
```

- [ ] **Step 5: Write device tests**

```kotlin
// LocationRepositoryTest.kt
package com.mockgps.standalone.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mockgps.standalone.data.db.AppDatabase
import com.mockgps.standalone.data.repository.LocationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: LocationRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = LocationRepository(db)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun addAndRetrieveFavorite() = runTest {
        repo.addFavorite("台北車站", 25.0478, 121.5170)
        val list = repo.favorites.first()
        assertEquals(1, list.size)
        assertEquals("台北車站", list[0].name)
    }

    @Test
    fun deleteFavorite() = runTest {
        repo.addFavorite("台北", 25.0, 121.0)
        val id = repo.favorites.first()[0].id
        repo.deleteFavorite(id)
        assertTrue(repo.favorites.first().isEmpty())
    }

    @Test
    fun recentsTrimsToTen() = runTest {
        repeat(15) { repo.addRecent("地點$it", 25.0 + it * 0.001, 121.0) }
        val list = repo.recents.first()
        assertEquals(10, list.size)
    }
}
```

- [ ] **Step 6: Run tests on device**

```bash
./gradlew connectedAndroidTest --tests "com.mockgps.standalone.data.LocationRepositoryTest"
```
Expected: `3 tests, 3 passed`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/data \
        app/src/androidTest/java/com/mockgps/standalone/data
git commit -m "feat: data layer — Room entities, DAOs, AppDatabase, LocationRepository"
```

---

### Task 4: MockLocationService — Static Mode + Notification

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/service/MockLocationService.kt`

**Interfaces:**
- Produces:
  - `MockLocationService.setLocation(lat: Double, lon: Double)`
  - `MockLocationService.stopMocking()`
  - `MockLocationService.isRunning: Boolean`
  - `MockLocationService.currentLat: Double`
  - `MockLocationService.currentLon: Double`
  - `MockLocationService.providerReady: Boolean`
  - `MockLocationService.LocalBinder` inner class with `getService(): MockLocationService`
  - Broadcast action `"com.mockgps.ACTION_STOP"` stops the service from the notification button
- Consumed by: `MainViewModel` (Task 6)

- [ ] **Step 1: Implement MockLocationService (static mode)**

```kotlin
// app/src/main/java/com/mockgps/standalone/service/MockLocationService.kt
package com.mockgps.standalone.service

import android.app.*
import android.content.Intent
import android.location.*
import android.location.provider.ProviderProperties
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mockgps.standalone.R
import com.mockgps.standalone.MainActivity
import kotlinx.coroutines.*

class MockLocationService : Service() {

    companion object {
        const val TAG = "MockGPS"
        const val CHANNEL_ID = "mock_gps"
        const val NOTIF_ID = 1
        const val ACTION_STOP = "com.mockgps.ACTION_STOP"
        var instance: MockLocationService? = null
    }

    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var currentLat = 25.0330
    var currentLon = 121.5654
    var isRunning = false
    var providerReady = false

    private val pushTask = object : Runnable {
        override fun run() {
            if (isRunning) {
                pushLocation(currentLat, currentLon)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotifChannel()
        startForeground(NOTIF_ID, buildNotif(getString(R.string.status_idle)))
        providerReady = initProviders()
        if (!providerReady) updateNotif(getString(R.string.status_no_provider))
    }

    private fun initProviders(): Boolean = try {
        runCatching { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER) }
        locationManager.addTestProvider(
            LocationManager.GPS_PROVIDER,
            false, false, false, false, true, true, true,
            ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE
        )
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)

        runCatching { locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER) }
        locationManager.addTestProvider(
            LocationManager.NETWORK_PROVIDER,
            false, false, false, false, false, false, false,
            ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_COARSE
        )
        locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        Log.d(TAG, "Providers initialized")
        true
    } catch (e: SecurityException) {
        Log.e(TAG, "Not selected as mock location app: ${e.message}")
        false
    } catch (e: Exception) {
        Log.e(TAG, "initProviders failed: ${e.message}")
        false
    }

    fun setLocation(lat: Double, lon: Double) {
        if (!providerReady) { providerReady = initProviders() }
        if (!providerReady) { updateNotif(getString(R.string.status_no_provider)); return }
        currentLat = lat; currentLon = lon
        if (!isRunning) { isRunning = true; handler.post(pushTask) }
        updateNotif(getString(R.string.status_static, lat, lon))
    }

    fun stopMocking() {
        isRunning = false
        handler.removeCallbacks(pushTask)
        serviceScope.coroutineContext.cancelChildren()
        runCatching { locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false) }
        runCatching { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER) }
        runCatching { locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false) }
        runCatching { locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER) }
        providerReady = false
        updateNotif(getString(R.string.status_idle))
    }

    fun pushLocation(lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtimeNanos()
        val jitter = (Math.random() * 2 - 1).toFloat()
        runCatching {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, Location(LocationManager.GPS_PROVIDER).apply {
                latitude = lat; longitude = lon; altitude = 10.0
                accuracy = 3.0f + jitter; time = now; elapsedRealtimeNanos = elapsed
            })
        }
        runCatching {
            locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, Location(LocationManager.NETWORK_PROVIDER).apply {
                latitude = lat; longitude = lon; altitude = 10.0
                accuracy = 20.0f; time = now; elapsedRealtimeNanos = elapsed
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopMocking()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService() = this@MockLocationService
    }

    override fun onDestroy() {
        instance = null
        stopMocking()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotifChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotif(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MockLocationService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.notif_action_stop), stopIntent)
            .build()
    }

    fun updateNotif(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotif(text))
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/service/MockLocationService.kt
git commit -m "feat: MockLocationService static mode — GPS+NETWORK mock, foreground notif with stop action"
```

---

### Task 5: WalkerState + MockLocationService Walker Mode

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/service/WalkerState.kt`
- Modify: `app/src/main/java/com/mockgps/standalone/service/MockLocationService.kt`
- Create: `app/src/test/java/com/mockgps/standalone/service/WalkerStateTest.kt`

**Interfaces:**
- Consumes: `GeoMath.distanceMeters`, `GeoMath.bearing`, `GeoMath.destination`, `GeoMath.randomPointInCircle`
- Produces:
  - `WalkerState(centerLat, centerLon, radiusM, speedMs)` — constructor
  - `WalkerState.step(): Pair<Double, Double>` — advance one second, return new position
  - `WalkerState.currentLat: Double`, `WalkerState.currentLon: Double`
  - `MockLocationService.startWalker(centerLat, centerLon, radiusM, speedMs)`

- [ ] **Step 1: Write failing tests for WalkerState**

```kotlin
// app/src/test/java/com/mockgps/standalone/service/WalkerStateTest.kt
package com.mockgps.standalone.service

import com.mockgps.standalone.util.GeoMath
import org.junit.Assert.*
import org.junit.Test

class WalkerStateTest {

    @Test
    fun `step changes position`() {
        val state = WalkerState(25.0, 121.0, 300.0, 3.0)
        val (lat, lon) = state.step()
        assertFalse("position must change", lat == 25.0 && lon == 121.0)
    }

    @Test
    fun `position is tracked in state fields`() {
        val state = WalkerState(25.0, 121.0, 300.0, 3.0)
        val (lat, lon) = state.step()
        assertEquals(lat, state.currentLat, 0.0)
        assertEquals(lon, state.currentLon, 0.0)
    }

    @Test
    fun `after 300 steps stays within radius plus buffer`() {
        val state = WalkerState(25.0330, 121.5654, 300.0, 5.0)
        repeat(300) { state.step() }
        val dist = GeoMath.distanceMeters(25.0330, 121.5654, state.currentLat, state.currentLon)
        assertTrue("dist=$dist, expected ≤ 350", dist <= 350.0)
    }
}
```

- [ ] **Step 2: Run — verify FAIL**

```bash
./gradlew test --tests "com.mockgps.standalone.service.WalkerStateTest"
```
Expected: `FAILED` (WalkerState not found)

- [ ] **Step 3: Implement `WalkerState.kt`**

```kotlin
// app/src/main/java/com/mockgps/standalone/service/WalkerState.kt
package com.mockgps.standalone.service

import com.mockgps.standalone.util.GeoMath
import kotlin.random.Random

class WalkerState(
    private val centerLat: Double,
    private val centerLon: Double,
    private val radiusM: Double,
    private val speedMs: Double
) {
    var currentLat: Double = centerLat
    var currentLon: Double = centerLon
    private var targetLat: Double
    private var targetLon: Double

    init {
        val t = GeoMath.randomPointInCircle(centerLat, centerLon, radiusM)
        targetLat = t.first; targetLon = t.second
    }

    fun step(): Pair<Double, Double> {
        val dist = GeoMath.distanceMeters(currentLat, currentLon, targetLat, targetLon)
        if (dist < 5.0) pickNewTarget()
        val bearing = GeoMath.bearing(currentLat, currentLon, targetLat, targetLon)
        val step = (speedMs + (Random.nextDouble() - 0.5)).coerceAtLeast(0.1)
        val next = GeoMath.destination(currentLat, currentLon, bearing, step)
        currentLat = next.first; currentLon = next.second
        return next
    }

    private fun pickNewTarget() {
        val t = GeoMath.randomPointInCircle(centerLat, centerLon, radiusM)
        targetLat = t.first; targetLon = t.second
    }
}
```

- [ ] **Step 4: Run — verify PASS**

```bash
./gradlew test --tests "com.mockgps.standalone.service.WalkerStateTest"
```
Expected: `3 tests, 3 passed`

- [ ] **Step 5: Add `startWalker` to MockLocationService**

Add these members and method to `MockLocationService.kt` (inside the class, after `setLocation`):

```kotlin
// Add at class level:
private var walkerJob: Job? = null

// Add method:
fun startWalker(centerLat: Double, centerLon: Double, radiusM: Double, speedMs: Double) {
    if (!providerReady) { providerReady = initProviders() }
    if (!providerReady) { updateNotif(getString(R.string.status_no_provider)); return }
    // Cancel any existing static push or walker
    isRunning = false
    handler.removeCallbacks(pushTask)
    walkerJob?.cancel()
    currentLat = centerLat; currentLon = centerLon
    isRunning = true
    walkerJob = serviceScope.launch {
        val walker = WalkerState(centerLat, centerLon, radiusM, speedMs)
        while (isActive) {
            val (lat, lon) = walker.step()
            currentLat = lat; currentLon = lon
            pushLocation(lat, lon)
            updateNotif(getString(R.string.status_walker, lat, lon))
            delay(1000)
        }
    }
}
```

Also update `stopMocking()` — `serviceScope.coroutineContext.cancelChildren()` already cancels `walkerJob`, so no change needed there.

- [ ] **Step 6: Build check**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/service/WalkerState.kt \
        app/src/main/java/com/mockgps/standalone/service/MockLocationService.kt \
        app/src/test/java/com/mockgps/standalone/service/WalkerStateTest.kt
git commit -m "feat: WalkerState + MockLocationService walker mode"
```

---

### Task 6: MainViewModel

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/ui/MainViewModel.kt`

**Interfaces:**
- Consumes: `MockLocationService.LocalBinder`, `LocationRepository`, `GeoMath`
- Produces:
  - `MainViewModel.uiState: StateFlow<UiState>`
  - `MainViewModel.favorites: StateFlow<List<FavoriteEntity>>`
  - `MainViewModel.recents: StateFlow<List<RecentEntity>>`
  - `MainViewModel.onMapTap(lat, lon)`
  - `MainViewModel.onStartStatic()`
  - `MainViewModel.onStartWalker()`
  - `MainViewModel.onStop()`
  - `MainViewModel.onSearch(query: String)`
  - `MainViewModel.onSearchResultSelected(result: SearchResult)`
  - `MainViewModel.onAddFavorite(name: String)`
  - `MainViewModel.onDeleteFavorite(id: Long)`
  - `MainViewModel.onLocationListItemTapped(lat: Double, lon: Double, name: String?)`
  - `MainViewModel.onModeChange(mode: MockMode)`
  - `MainViewModel.onRadiusChanged(meters: Float)`
  - `MainViewModel.onSpeedChanged(ms: Float)`
  - `data class UiState(lat, lon, mode, radius, speed, isRunning, providerReady, searchQuery, searchResults, isSearching)`
  - `enum class MockMode { STATIC, WALKER }`
  - `data class SearchResult(name, displayName, lat, lon)`
- Consumed by: `MainScreen` (Task 11)

- [ ] **Step 1: Implement MainViewModel**

```kotlin
// app/src/main/java/com/mockgps/standalone/ui/MainViewModel.kt
package com.mockgps.standalone.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mockgps.standalone.data.model.FavoriteEntity
import com.mockgps.standalone.data.model.RecentEntity
import com.mockgps.standalone.data.db.AppDatabase
import com.mockgps.standalone.data.repository.LocationRepository
import com.mockgps.standalone.service.MockLocationService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class MockMode { STATIC, WALKER }

data class SearchResult(val name: String, val displayName: String, val lat: Double, val lon: Double)

data class UiState(
    val lat: Double = 25.0330,
    val lon: Double = 121.5654,
    val mode: MockMode = MockMode.STATIC,
    val walkerRadius: Float = 300f,
    val walkerSpeed: Float = 3f,
    val isRunning: Boolean = false,
    val providerReady: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LocationRepository(AppDatabase.instance(app))

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val favorites: StateFlow<List<FavoriteEntity>> = repo.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recents: StateFlow<List<RecentEntity>> = repo.recents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var service: MockLocationService? = null
    private var searchJob: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as MockLocationService.LocalBinder).getService()
            _uiState.update { it.copy(isRunning = service?.isRunning ?: false, providerReady = service?.providerReady ?: true) }
        }
        override fun onServiceDisconnected(name: ComponentName) { service = null }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, MockLocationService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        runCatching { context.unbindService(connection) }
    }

    fun onMapTap(lat: Double, lon: Double) {
        _uiState.update { it.copy(lat = lat, lon = lon) }
    }

    fun onStartStatic() {
        val s = _uiState.value
        service?.setLocation(s.lat, s.lon)
        _uiState.update { it.copy(isRunning = true, mode = MockMode.STATIC) }
        viewModelScope.launch { repo.addRecent(null, s.lat, s.lon) }
    }

    fun onStartWalker() {
        val s = _uiState.value
        service?.startWalker(s.lat, s.lon, s.walkerRadius.toDouble(), s.walkerSpeed.toDouble())
        _uiState.update { it.copy(isRunning = true, mode = MockMode.WALKER) }
        viewModelScope.launch { repo.addRecent(null, s.lat, s.lon) }
    }

    fun onStop() {
        service?.stopMocking()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun onSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) { _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }; return }
        searchJob = viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(isSearching = true) }
            val results = runCatching { nominatimSearch(query) }.getOrDefault(emptyList())
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun onSearchResultSelected(result: SearchResult) {
        _uiState.update { it.copy(lat = result.lat, lon = result.lon, searchQuery = result.name, searchResults = emptyList()) }
        viewModelScope.launch { repo.addRecent(result.name, result.lat, result.lon) }
    }

    fun onAddFavorite(name: String) {
        val s = _uiState.value
        viewModelScope.launch { repo.addFavorite(name, s.lat, s.lon) }
    }

    fun onDeleteFavorite(id: Long) {
        viewModelScope.launch { repo.deleteFavorite(id) }
    }

    fun onLocationListItemTapped(lat: Double, lon: Double, name: String?) {
        _uiState.update { it.copy(lat = lat, lon = lon) }
    }

    fun onModeChange(mode: MockMode) { _uiState.update { it.copy(mode = mode) } }
    fun onRadiusChanged(meters: Float) { _uiState.update { it.copy(walkerRadius = meters) } }
    fun onSpeedChanged(ms: Float) { _uiState.update { it.copy(walkerSpeed = ms) } }

    private suspend fun nominatimSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5&accept-language=zh-TW,zh,en")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "MockGPS-Standalone/1.0")
        try {
            val body = conn.inputStream.bufferedReader().readText()
            val arr = JSONArray(body)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val display = obj.getString("display_name")
                SearchResult(
                    name = display.substringBefore(",").trim(),
                    displayName = display,
                    lat = obj.getString("lat").toDouble(),
                    lon = obj.getString("lon").toDouble()
                )
            }
        } finally { conn.disconnect() }
    }
}
```

- [ ] **Step 2: Build check**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/ui/MainViewModel.kt
git commit -m "feat: MainViewModel — UiState, service binding, search, favorites/recents"
```

---

### Task 7: OSMDroid Map Component

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/ui/component/OsmMapView.kt`

**Interfaces:**
- Consumes: OSMDroid `MapView`, `Marker`, `Polygon`, `MapEventsOverlay`
- Produces:
  - `OsmMapView(lat, lon, walkerRadius, onLocationSelected, modifier)` — Composable
    - `walkerRadius: Float?` — null hides the circle overlay

- [ ] **Step 1: Implement OsmMapView**

```kotlin
// app/src/main/java/com/mockgps/standalone/ui/component/OsmMapView.kt
package com.mockgps.standalone.ui.component

import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun OsmMapView(
    lat: Double,
    lon: Double,
    walkerRadius: Float?,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapView = rememberMapView()
    var marker by remember { mutableStateOf<Marker?>(null) }
    var circle by remember { mutableStateOf<Polygon?>(null) }

    LaunchedEffect(lat, lon, walkerRadius) {
        val gp = GeoPoint(lat, lon)

        // Remove old overlays
        marker?.let { mapView.overlays.remove(it) }
        circle?.let { mapView.overlays.remove(it) }

        // Add draggable marker
        val m = Marker(mapView).apply {
            position = gp
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isDraggable = true
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(m: Marker) {}
                override fun onMarkerDragStart(m: Marker) {}
                override fun onMarkerDragEnd(m: Marker) {
                    onLocationSelected(m.position.latitude, m.position.longitude)
                }
            })
        }
        marker = m
        mapView.overlays.add(m)

        // Walker radius circle
        if (walkerRadius != null && walkerRadius > 0f) {
            val c = Polygon().apply {
                points = Polygon.pointsAsCircle(gp, walkerRadius.toDouble())
                fillPaint.color = Color.argb(40, 0, 120, 255)
                outlinePaint.color = Color.argb(180, 0, 120, 255)
                outlinePaint.strokeWidth = 3f
                isVisible = true
            }
            circle = c
            mapView.overlays.add(0, c)
        } else {
            circle = null
        }

        mapView.controller.animateTo(gp)
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

@Composable
private fun rememberMapView(): MapView {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            // Tap-to-place overlay (added at index 0 so it's below the pin)
            overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean = false
                override fun longPressHelper(p: GeoPoint): Boolean = false
            }))
        }
    }
}
```

Note: tap-to-place is handled by tapping the marker itself or dragging. Long-press on map for tap-to-place will be wired in `BottomSheetContent` via a separate `MapEventsOverlay` with `onLocationSelected` passed through.

- [ ] **Step 2: Build check**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/ui/component/OsmMapView.kt
git commit -m "feat: OsmMapView — OSMDroid AndroidView with draggable pin and walker circle overlay"
```

---

### Task 8: BottomSheet UI — Coords, Mode Toggle, Walker Config

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/ui/component/BottomSheetContent.kt`

**Interfaces:**
- Consumes: `UiState`, `MockMode`
- Produces:
  - `BottomSheetContent(uiState, onLatChange, onLonChange, onModeChange, onRadiusChange, onSpeedChange, onStart, onStop, content)` — Composable

- [ ] **Step 1: Implement BottomSheetContent**

```kotlin
// app/src/main/java/com/mockgps/standalone/ui/component/BottomSheetContent.kt
package com.mockgps.standalone.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mockgps.standalone.R
import com.mockgps.standalone.ui.MockMode
import com.mockgps.standalone.ui.UiState

@Composable
fun BottomSheetContent(
    uiState: UiState,
    onLatChange: (Double) -> Unit,
    onLonChange: (Double) -> Unit,
    onModeChange: (MockMode) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Coordinate row with copy
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoordField(
                label = stringResource(R.string.label_latitude),
                value = uiState.lat,
                onValueChange = onLatChange,
                modifier = Modifier.weight(1f)
            )
            CoordField(
                label = stringResource(R.string.label_longitude),
                value = uiState.lon,
                onValueChange = onLonChange,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                clipboard.setText(AnnotatedString("${uiState.lat}, ${uiState.lon}"))
            }) {
                Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copied))
            }
        }

        // Mode toggle
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MockMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                    selected = uiState.mode == mode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(idx, MockMode.entries.size),
                    label = { Text(if (mode == MockMode.STATIC) stringResource(R.string.mode_static) else stringResource(R.string.mode_walker)) }
                )
            }
        }

        // Walker config (only when WALKER mode)
        if (uiState.mode == MockMode.WALKER) {
            SliderRow(
                label = stringResource(R.string.label_radius),
                value = uiState.walkerRadius,
                valueRange = 50f..2000f,
                displayText = "${uiState.walkerRadius.toInt()} m",
                onValueChange = onRadiusChange
            )
            SliderRow(
                label = stringResource(R.string.label_speed),
                value = uiState.walkerSpeed,
                valueRange = 1f..10f,
                displayText = "${"%.1f".format(uiState.walkerSpeed)} m/s",
                onValueChange = onSpeedChange
            )
        }

        // Provider warning
        if (!uiState.providerReady) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.status_no_provider), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    }) { Text(stringResource(R.string.goto_dev_settings)) }
                }
            }
        }

        // Start / Stop
        Button(
            onClick = { if (uiState.isRunning) onStop() else if (uiState.mode == MockMode.STATIC) onStart() else onStart() },
            modifier = Modifier.fillMaxWidth(),
            colors = if (uiState.isRunning) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
        ) {
            Text(if (uiState.isRunning) stringResource(R.string.btn_stop) else stringResource(R.string.btn_start))
        }

        extraContent()
    }
}

@Composable
private fun CoordField(label: String, value: Double, onValueChange: (Double) -> Unit, modifier: Modifier) {
    var text by remember(value) { mutableStateOf("%.6f".format(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; it.toDoubleOrNull()?.let(onValueChange) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun SliderRow(label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, displayText: String, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(displayText, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}
```

Also add the Material Icons dependency to `app/build.gradle.kts`:
```kotlin
implementation("androidx.compose.material:material-icons-extended")
```
Add to the `[libraries]` section of `gradle/libs.versions.toml`:
```toml
androidx-compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
```
And use it in `app/build.gradle.kts`:
```kotlin
implementation(libs.androidx.compose.material.icons)
```

- [ ] **Step 2: Build check**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/ui/component/BottomSheetContent.kt \
        gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat: BottomSheetContent — coord input, mode toggle, walker sliders, provider warning"
```

---

### Task 9: Search Component

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/ui/component/SearchBar.kt`

**Interfaces:**
- Consumes: `UiState.searchQuery`, `UiState.searchResults`, `UiState.isSearching`
- Produces:
  - `LocationSearchBar(query, results, isSearching, onQueryChange, onResultSelected)` — Composable

- [ ] **Step 1: Implement SearchBar**

```kotlin
// app/src/main/java/com/mockgps/standalone/ui/component/SearchBar.kt
package com.mockgps.standalone.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mockgps.standalone.R
import com.mockgps.standalone.ui.SearchResult

@Composable
fun LocationSearchBar(
    query: String,
    results: List<SearchResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onResultSelected: (SearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (results.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(results) { result ->
                        ListItem(
                            headlineContent = { Text(result.name) },
                            supportingContent = { Text(result.displayName, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.clickable { onResultSelected(result) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build check**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/ui/component/SearchBar.kt
git commit -m "feat: LocationSearchBar — Nominatim-backed search with debounce via ViewModel"
```

---

### Task 10: Favorites / Recents List

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/ui/component/LocationList.kt`

**Interfaces:**
- Consumes: `FavoriteEntity`, `RecentEntity`
- Produces:
  - `LocationListTabs(favorites, recents, onFavoriteTap, onDeleteFavorite, onRecentTap)` — Composable

- [ ] **Step 1: Implement LocationListTabs**

```kotlin
// app/src/main/java/com/mockgps/standalone/ui/component/LocationList.kt
package com.mockgps.standalone.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mockgps.standalone.R
import com.mockgps.standalone.data.model.FavoriteEntity
import com.mockgps.standalone.data.model.RecentEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LocationListTabs(
    favorites: List<FavoriteEntity>,
    recents: List<RecentEntity>,
    onFavoriteTap: (FavoriteEntity) -> Unit,
    onDeleteFavorite: (Long) -> Unit,
    onRecentTap: (RecentEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.tab_favorites)) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.tab_recents)) })
        }
        when (selectedTab) {
            0 -> FavoritesList(favorites, onFavoriteTap, onDeleteFavorite)
            1 -> RecentsList(recents, onRecentTap)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesList(items: List<FavoriteEntity>, onTap: (FavoriteEntity) -> Unit, onDelete: (Long) -> Unit) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.padding(androidx.compose.ui.unit.dp * 16)) { Text("尚無收藏") }
        return
    }
    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
        items(items, key = { it.id }) { fav ->
            ListItem(
                headlineContent = { Text(fav.name) },
                supportingContent = { Text("%.5f, %.5f".format(fav.lat, fav.lon), style = MaterialTheme.typography.bodySmall) },
                trailingContent = {
                    IconButton(onClick = { onDelete(fav.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "刪除")
                    }
                },
                modifier = Modifier.combinedClickable(onClick = { onTap(fav) })
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun RecentsList(items: List<RecentEntity>, onTap: (RecentEntity) -> Unit) {
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    if (items.isEmpty()) {
        Box(modifier = Modifier.padding(androidx.compose.ui.unit.dp * 16)) { Text("尚無紀錄") }
        return
    }
    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
        items(items, key = { it.id }) { recent ->
            ListItem(
                headlineContent = { Text(recent.name ?: "%.5f, %.5f".format(recent.lat, recent.lon)) },
                supportingContent = { Text(fmt.format(Date(recent.usedAt)), style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.combinedClickable(onClick = { onTap(recent) })
            )
            HorizontalDivider()
        }
    }
}
```

- [ ] **Step 2: Build check**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/ui/component/LocationList.kt
git commit -m "feat: LocationListTabs — favorites (with delete) and recents tabs"
```

---

### Task 11: MainScreen + MainActivity Assembly + Battery Optimization Prompt

**Files:**
- Create: `app/src/main/java/com/mockgps/standalone/ui/screen/MainScreen.kt`
- Create: `app/src/main/java/com/mockgps/standalone/MainActivity.kt`

**Interfaces:**
- Consumes: all components from Tasks 7-10, `MainViewModel`
- Produces: runnable app

- [ ] **Step 1: Implement MainScreen**

```kotlin
// app/src/main/java/com/mockgps/standalone/ui/screen/MainScreen.kt
package com.mockgps.standalone.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mockgps.standalone.ui.MainViewModel
import com.mockgps.standalone.ui.MockMode
import com.mockgps.standalone.ui.component.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val recents by vm.recents.collectAsState()
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var favoriteNameInput by remember { mutableStateOf("") }

    val sheetState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 160.dp,
        sheetContent = {
            BottomSheetContent(
                uiState = uiState,
                onLatChange = { vm.onMapTap(it, uiState.lon) },
                onLonChange = { vm.onMapTap(uiState.lat, it) },
                onModeChange = vm::onModeChange,
                onRadiusChange = vm::onRadiusChanged,
                onSpeedChange = vm::onSpeedChanged,
                onStart = { if (uiState.mode == MockMode.WALKER) vm.onStartWalker() else vm.onStartStatic() },
                onStop = vm::onStop
            ) {
                LocationSearchBar(
                    query = uiState.searchQuery,
                    results = uiState.searchResults,
                    isSearching = uiState.isSearching,
                    onQueryChange = vm::onSearch,
                    onResultSelected = vm::onSearchResultSelected
                )
                Spacer(Modifier.height(8.dp))
                LocationListTabs(
                    favorites = favorites,
                    recents = recents,
                    onFavoriteTap = { vm.onLocationListItemTapped(it.lat, it.lon, it.name) },
                    onDeleteFavorite = vm::onDeleteFavorite,
                    onRecentTap = { vm.onLocationListItemTapped(it.lat, it.lon, it.name) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OsmMapView(
                lat = uiState.lat,
                lon = uiState.lon,
                walkerRadius = if (uiState.mode == MockMode.WALKER) uiState.walkerRadius else null,
                onLocationSelected = { lat, lon -> vm.onMapTap(lat, lon) },
                modifier = Modifier.fillMaxSize()
            )
            // FAB: re-center map
            FloatingActionButton(
                onClick = { /* map re-centers via lat/lon state in OsmMapView */ vm.onMapTap(uiState.lat, uiState.lon) },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) { Icon(Icons.Default.MyLocation, contentDescription = "置中") }

            // FAB: add favorite
            FloatingActionButton(
                onClick = { favoriteNameInput = ""; showAddFavoriteDialog = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) { Icon(Icons.Default.Star, contentDescription = "加入收藏") }
        }
    }

    if (showAddFavoriteDialog) {
        AlertDialog(
            onDismissRequest = { showAddFavoriteDialog = false },
            title = { Text("加入收藏") },
            text = {
                OutlinedTextField(
                    value = favoriteNameInput,
                    onValueChange = { favoriteNameInput = it },
                    label = { Text("名稱") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (favoriteNameInput.isNotBlank()) vm.onAddFavorite(favoriteNameInput)
                    showAddFavoriteDialog = false
                }) { Text("儲存") }
            },
            dismissButton = { TextButton(onClick = { showAddFavoriteDialog = false }) { Text("取消") } }
        )
    }
}
```

- [ ] **Step 2: Implement MainActivity with battery optimization prompt**

```kotlin
// app/src/main/java/com/mockgps/standalone/MainActivity.kt
package com.mockgps.standalone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.mockgps.standalone.ui.MainViewModel
import com.mockgps.standalone.ui.screen.MainScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val prefs by lazy { getSharedPreferences("mockgps_prefs", MODE_PRIVATE) }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val shouldShowBatteryPrompt = !prefs.getBoolean("battery_prompt_shown", false)
            && !isBatteryOptimizationIgnored()

        setContent {
            var showBatteryDialog by remember { mutableStateOf(shouldShowBatteryPrompt) }

            if (showBatteryDialog) {
                AlertDialog(
                    onDismissRequest = { showBatteryDialog = false },
                    title = { Text(stringResource(R.string.battery_dialog_title)) },
                    text = { Text(stringResource(R.string.battery_dialog_body)) },
                    confirmButton = {
                        TextButton(onClick = {
                            prefs.edit().putBoolean("battery_prompt_shown", true).apply()
                            showBatteryDialog = false
                            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:$packageName")))
                        }) { Text(stringResource(R.string.battery_dialog_ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            prefs.edit().putBoolean("battery_prompt_shown", true).apply()
                            showBatteryDialog = false
                        }) { Text(stringResource(R.string.battery_dialog_skip)) }
                    }
                )
            }

            MainScreen(vm = vm)
        }
    }

    override fun onStart() {
        super.onStart()
        vm.bindService(this)
    }

    override fun onStop() {
        super.onStop()
        vm.unbindService(this)
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}
```

- [ ] **Step 3: Full build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL` with an APK at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 4: Run all local tests**

```bash
./gradlew test
```
Expected: `11 tests, 11 passed` (GeoMathTest × 8 + WalkerStateTest × 3)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mockgps/standalone/ui/screen/MainScreen.kt \
        app/src/main/java/com/mockgps/standalone/MainActivity.kt \
        app/src/main/java/com/mockgps/standalone/ui/MainViewModel.kt
git commit -m "feat: MainScreen + MainActivity — full UI assembly, battery optimization prompt"
```

- [ ] **Step 6: Push**

```bash
git remote add origin <YOUR_REMOTE_URL>
git push -u origin main
```

---

## Verification Checklist (Manual — Requires Device)

1. Build + install: `./gradlew installDebug`
2. 確認手機「開發者選項 → 選取模擬位置 App」選擇 MockGPS
3. 開啟 App → 電池豁免 Dialog 出現 → 點「前往設定」並允許
4. 靜態模式：拖曳 Pin 到台北 → 點「開始」→ Google Maps 藍點移到台北
5. 搜尋「台北 101」→ 點選結果 → Pin 移動到 101 → 點「開始」→ Google Maps 確認
6. 漫步模式：切換至漫步 → 設半徑 500m → 速度 5 m/s → 點「開始」→ Google Maps 藍點在圓圈內移動
7. 通知列「停止」按鈕 → 確認服務終止
8. 加入收藏 → 重啟 App → 確認收藏仍存在
9. 最近紀錄超過 10 筆後確認只保留最新 10 筆
