pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven("https://repo.plo.su")
    }
}

rootProject.name = "pv-addon-broadcast"

include("jar", "common", "proxy", "server")
