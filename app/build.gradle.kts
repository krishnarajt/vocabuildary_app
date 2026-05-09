plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun configValue(name: String, default: String): String {
    return providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(default)
        .get()
}

fun quoted(value: String): String {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

val redirectScheme = configValue("APP_AUTH_REDIRECT_SCHEME", "com.kptgames.vocabuildary")

android {
    namespace = "com.kptgames.vocabuildary"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kptgames.vocabuildary"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["appAuthRedirectScheme"] = redirectScheme
        buildConfigField(
            "String",
            "VOCABUILDARY_API_BASE_URL",
            quoted(
                configValue(
                    "VOCABUILDARY_API_BASE_URL",
                    "https://api-get-away.krishnarajthadesar.in/api/vocabuildary/"
                )
            )
        )
        buildConfigField(
            "String",
            "OIDC_ISSUER",
            quoted(configValue("OIDC_ISSUER", "https://auth.example.com/application/o/vocabuildary/"))
        )
        buildConfigField(
            "String",
            "OIDC_CLIENT_ID",
            quoted(configValue("OIDC_CLIENT_ID", "vocabuildary-android"))
        )
        buildConfigField(
            "String",
            "OIDC_REDIRECT_URI",
            quoted(configValue("OIDC_REDIRECT_URI", "$redirectScheme:/oauth2redirect"))
        )
        buildConfigField(
            "String",
            "OIDC_SCOPES",
            quoted(configValue("OIDC_SCOPES", "openid profile email offline_access"))
        )
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("net.openid:appauth:0.11.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
