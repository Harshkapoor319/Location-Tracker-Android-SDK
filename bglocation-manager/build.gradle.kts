import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

// Automatically unpack classes.jar from any .aar placed in libs/ so Kotlin/Java compiler can read its classes
file("libs").listFiles { f -> f.extension == "aar" }?.forEach { aarFile ->
    val jarFile = file("libs/${aarFile.nameWithoutExtension}.jar")
    if (!jarFile.exists() || jarFile.lastModified() < aarFile.lastModified()) {
        try {
            val zip = ZipFile(aarFile)
            val entry = zip.getEntry("classes.jar")
            if (entry != null) {
                zip.getInputStream(entry).use { input ->
                    jarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            zip.close()
        } catch (e: Exception) {
            logger.warn("Could not extract classes.jar from ${aarFile.name}: ${e.message}")
        }
    }
}

android {
    namespace = "com.it.bglocation.manager"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    publishing {
        singleVariant("release")
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.bglocation"
                artifactId = "bglocation-manager"
                version = "1.0.0"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Export the compiled bytecode from libs/ transitively so consumer apps can access models
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    // Fallback to project during initial dev before first AAR export
    val hasLocalBinary = file("libs").listFiles { f -> f.extension == "jar" || f.extension == "aar" }?.isNotEmpty() == true
    if (!hasLocalBinary && findProject(":bglocation-sdk") != null) {
        implementation(project(":bglocation-sdk"))
    }

    // Required Google Play & AndroidX APIs
    api(libs.play.services.location)
    api(libs.androidx.work.runtime.ktx)
    api(libs.androidx.lifecycle.service)
    api(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
