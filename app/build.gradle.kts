import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing: ~/.clipshot/keystore.properties (never in the repo).
val signing = File(System.getProperty("user.home"), ".clipshot/keystore.properties")
val signingProps = Properties().apply { if (signing.exists()) signing.inputStream().use { load(it) } }

// Date of the latest internal security review, the newest docs/security/YYYY-MM-DD.md. Shown in About.
val securityReview: String = rootProject.file("docs/security").listFiles().orEmpty()
    .map { it.name }.filter { Regex("""\d{4}-\d{2}-\d{2}\.md""").matches(it) }
    .maxOrNull()?.removeSuffix(".md") ?: error("No review in docs/security/")

android {
    namespace = "dev.sidali.clipshot"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.sidali.clipshot"
        minSdk = 30
        targetSdk = 36
        versionCode = 7
        versionName = "1.3.2"
        buildConfigField("String", "SECURITY_REVIEW", "\"$securityReview\"")
    }
    signingConfigs {
        if (signing.exists()) create("release") {
            storeFile = file(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }
    // Two distributions from the same code:
    //  - full: GitHub releases, Obtainium, F-Droid. Includes the optional "remove from gallery" mode
    //    and Capture (Quick Settings tile and accessibility button, through an accessibility service).
    //  - play: Google Play. Same app without those two, because Play only allows "All files access"
    //    for file managers, backup and antivirus apps, and strictly limits accessibility services.
    flavorDimensions += "dist"
    productFlavors {
        create("full") {
            dimension = "dist"
            buildConfigField("boolean", "REMOVE_FROM_GALLERY", "true")
            buildConfigField("boolean", "CAPTURE", "true")
        }
        create("play") {
            dimension = "dist"
            buildConfigField("boolean", "REMOVE_FROM_GALLERY", "false")
            buildConfigField("boolean", "CAPTURE", "false")
        }
    }

    buildTypes {
        release {
            // Reproducible builds: no git commit id baked into the APK.
            vcsInfo.include = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // -Punsigned builds an unsigned APK, used to check that a build is reproducible.
            if (!project.hasProperty("unsigned")) signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    dependenciesInfo { includeInApk = false; includeInBundle = false }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.09.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    testImplementation("junit:junit:4.13.2")
}
