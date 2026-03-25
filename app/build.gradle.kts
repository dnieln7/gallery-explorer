import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.kapt)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.com.google.dagger.hilt.android)
    alias(libs.plugins.io.gitlab.arturbosch.detekt)
    alias(libs.plugins.org.jetbrains.kotlinx.kover)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "xyz.dnieln7.galleryex"

    compileSdk = 35

    defaultConfig {
        applicationId = "xyz.dnieln7.galleryex"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    sourceSets {
        getByName("main").assets.srcDir("$projectDir/schemas")
    }

    detekt {
        toolVersion = libs.versions.detekt.get()
        buildUponDefaultConfig = true
        ignoredBuildTypes = listOf("release")
        baseline = file("${projectDir}/detekt/baseline.xml")
        config.setFrom("${rootDir}/config/detekt/config.yml")
        source.setFrom("src/main/java", "src/main/kotlin", "src/test/java", "src/test/kotlin")
    }

    tasks.withType<Detekt> {
        reports {
            xml.required.set(false)
            txt.required.set(false)
            sarif.required.set(false)
            html.required.set(true)
            html.outputLocation.set(file("${projectDir}/build/reports/detekt.html"))
            md.required.set(true)
            md.outputLocation.set(file("${projectDir}/build/reports/detekt.md"))
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.io.coil.kt.coil.video)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.com.jakewharton.timber)

    kapt(libs.com.google.dagger.hilt.android.compiler)
    implementation(libs.com.google.dagger.hilt.android)

    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.room.testing)

    implementation(libs.cafe.adriel.voyager.navigator)
    implementation(libs.cafe.adriel.voyager.hilt)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    detektPlugins(libs.io.gitlab.arturbosch.detekt.formatting)

    testImplementation(libs.junit)
    testImplementation(libs.io.mockk)
    testImplementation(libs.app.cash.turbine)
    testImplementation(libs.com.lemonappdev.konsist)
    testImplementation(libs.org.amshove.kluent.android)
    testImplementation(libs.org.jetbrains.kotlinx.coroutines.test)
    testImplementation(libs.org.robolectric)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.org.amshove.kluent.android)
    androidTestImplementation(libs.org.jetbrains.kotlinx.coroutines.test)
}
