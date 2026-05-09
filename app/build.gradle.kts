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

val mobileRedirectScheme = configValue("VOCABUILDARY_MOBILE_REDIRECT_SCHEME", "com.kptgames.vocabuildary")
val mobileRedirectHost = configValue("VOCABUILDARY_MOBILE_REDIRECT_HOST", "auth")
val mobileRedirectUri = configValue(
    "VOCABUILDARY_MOBILE_REDIRECT_URI",
    "$mobileRedirectScheme://$mobileRedirectHost"
)

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

        manifestPlaceholders["gatewayAuthRedirectScheme"] = mobileRedirectScheme
        manifestPlaceholders["gatewayAuthRedirectHost"] = mobileRedirectHost
        manifestPlaceholders["appAuthRedirectScheme"] = mobileRedirectScheme
        manifestPlaceholders["usesCleartextTraffic"] = "false"
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
            "VOCABUILDARY_AUTH_MODE",
            quoted(configValue("VOCABUILDARY_AUTH_MODE", "gateway-token"))
        )
        buildConfigField(
            "String",
            "VOCABUILDARY_MOBILE_AUTH_PATH",
            quoted(configValue("VOCABUILDARY_MOBILE_AUTH_PATH", "mobile/auth/start"))
        )
        buildConfigField(
            "String",
            "VOCABUILDARY_MOBILE_REDIRECT_URI",
            quoted(mobileRedirectUri)
        )
        buildConfigField("String", "OIDC_CLIENT_ID", quoted(configValue("OIDC_CLIENT_ID", "")))
        buildConfigField("String", "OIDC_REDIRECT_URI", quoted(configValue("OIDC_REDIRECT_URI", mobileRedirectUri)))
        buildConfigField("String", "OIDC_SCOPES", quoted(configValue("OIDC_SCOPES", "openid profile email")))
        buildConfigField("String", "OIDC_ISSUER", quoted(configValue("OIDC_ISSUER", "")))
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
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
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.appauth)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
