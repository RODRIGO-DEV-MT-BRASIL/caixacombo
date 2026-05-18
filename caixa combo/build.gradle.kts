// Root build.gradle.kts - Projeto CaixaCombo
// Repositórios são gerenciados pelo settings.gradle.kts (dependencyResolutionManagement)

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

tasks.register("installDebug") {
    group = "installation"
    description = "Alias to install the default debug APK variant."
    dependsOn(":app:installDebug")
}
