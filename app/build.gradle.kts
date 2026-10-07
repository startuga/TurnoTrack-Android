import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Chave de assinatura fixa: garante que cada APK novo instala por cima do anterior
// (mantendo os dados). Ver README > "Assinatura".
val signingProps = Properties().apply {
    val file = rootProject.file("signing/signing.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// No GitHub Actions o número do build vira versionCode, para cada APK ser uma atualização.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.startuga.turnotrack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.startuga.turnotrack"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "2.0.$buildNumber"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(signingProps.getProperty("storeFile", "signing/turnotrack.jks"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
