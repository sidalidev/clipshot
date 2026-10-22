// Test-only app: reads the clipboard like any other app would (Messages, Signal...) and logs whether the image opens.
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "dev.sidali.pastecheck"; compileSdk = 36
    defaultConfig { applicationId = "dev.sidali.pastecheck"; minSdk = 30; targetSdk = 36 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
