plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// ZajedniÄki potpis: Desktop\programi\keystore
rootProject.file("../keystore/signing.gradle").takeIf { it.exists() }?.let { apply(from = it) }

android {
    namespace = "com.gymtracker.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gymtracker.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 18
        versionName = "2.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // NAPOMENA (pre pravog objavljivanja na Play/produkciju):
            // 1) Ovaj release je potpisan DEBUG kljuÄem. Debug sertifikat ima drugaÄiji
            //    SHA-1 od produkcionog OAuth klijenta -> Google Sign-In / Drive backup
            //    NEÄ†E raditi u pravom release-u dok se ne kreira poseban release
            //    signingConfig (keystore) i njegov SHA-1 ne registruje u Google Cloud konzoli.
            // 2) isMinifyEnabled = false znaÄi da R8 ne radi, pa keep-pravila u
            //    proguard-rules.pro (za data.local.** / data.model.** koje Gson koristi
            //    refleksijom) nikad nisu ni testirana. Ako se ukljuÄi minify, OBAVEZNO
            //    testirati JSON backup/restore da polja modela ne budu uklonjena.
            // Trenutno namerno ostavljeno ovako da lokalni/sideload build i dalje radi.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Google Sign-In and Drive API
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20231128-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
