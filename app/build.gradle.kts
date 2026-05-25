import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.mibusdemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mibusdemo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Leer API Key de local.properties
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        val apiKey = properties.getProperty("MAPS_API_KEY") ?: ""

        // Inyecta la clave en el Manifest
        manifestPlaceholders["MAPS_API_KEY"] = apiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Ubicación: Mantenemos solo la versión más reciente (21.3.0)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Firebase Firestore
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    // Para la utenticacion de usuario (correo/contraseña) Firebase Auth
    implementation("com.google.firebase:firebase-auth")


    // Gson para manejo de GeoJSON
    implementation("com.google.code.gson:gson:2.10.1")

    // ViewModel Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")

    // Librería necesaria para que el XML reconozca SupportMapFragment
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // Libreria para ktk de registerActivity
    implementation("androidx.core:core-ktx:1.13.1")
}