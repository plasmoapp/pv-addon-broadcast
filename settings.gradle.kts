pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.plasmoverse.com/snapshots")
        maven("https://repo.plasmoverse.com/releases")
        maven("https://jitpack.io/")
    }
}

rootProject.name = "pv-addon-broadcast"

val requestedTasks: List<String> = gradle.startParameter.taskNames

include("common", "proxy", "server")
if (requestedTasks.contains("build")) {
    include("jar")
}
