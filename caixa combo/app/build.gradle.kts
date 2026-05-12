plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

import java.util.Properties

// Carregar propriedades do local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.seucaixa.caixacombo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.seucaixa.caixacombo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += listOf("device", "empresa")
    productFlavors {
        // Device flavors
        create("p2b") {
            dimension = "device"
            applicationIdSuffix = ".p2b"
            versionNameSuffix = "-p2b"
        }
        create("checkoutpos") {
            dimension = "device"
            applicationIdSuffix = ".checkoutpos"
            versionNameSuffix = "-checkoutpos"
        }

        // Empresa flavors (white-label)
        create("caixacombo") {
            dimension = "empresa"
            applicationId = "com.seucaixa.caixacombo"
            versionNameSuffix = ""
        }
        create("empresa1") {
            dimension = "empresa"
            applicationId = "com.empresa1.caixa"
            versionNameSuffix = "-empresa1"
        }
        create("empresa2") {
            dimension = "empresa"
            applicationId = "com.empresa2.caixa"
            versionNameSuffix = "-empresa2"
        }
    }

    signingConfigs {
        create("release") {
            // Configure estas variáveis em local.properties ou em ambiente CI/CD
            // Exemplo em local.properties:
            // RELEASE_KEYSTORE_FILE=/caminho/para/keystore.jks
            // RELEASE_KEYSTORE_PASSWORD=sua_senha
            // RELEASE_KEY_ALIAS=seu_alias
            // RELEASE_KEY_PASSWORD=sua_senha_alias
            
            val keystoreFile = localProperties["RELEASE_KEYSTORE_FILE"] as String?
            val keystorePassword = localProperties["RELEASE_KEYSTORE_PASSWORD"] as String?
            val keyAliasName = localProperties["RELEASE_KEY_ALIAS"] as String?
            val keyPasswordValue = localProperties["RELEASE_KEY_PASSWORD"] as String?

            if (keystoreFile != null && keystorePassword != null && keyAliasName != null && keyPasswordValue != null) {
                storeFile = rootProject.file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAliasName
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val flavorName = variant.flavorName
                val outputFileName = when (flavorName) {
                    "p2b" -> "CaixaComboP2B.apk"
                    "checkoutpos" -> "caixacombocheckoutpos.apk"
                    else -> "CaixaCombo.apk"
                }
                output.outputFileName = outputFileName
            }
    }
    
    splits {
        abi {
            isEnable = false
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
        buildConfig = true
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose UI
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Room Database (SQLite)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // WorkManager (para backup automático offline)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coil para carregamento de imagens
    implementation("io.coil-kt:coil-compose:2.5.0")

    // EncryptedSharedPreferences para dados sensíveis (Stone compliance)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Sunmi Printer SDK para V1/V2 compatibilidade
    implementation("com.sunmi:printerx:latest.release")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
