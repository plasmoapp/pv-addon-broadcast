plugins {
    kotlin("jvm")
    id("su.plo.voice.plugin.entrypoints")
}

dependencies {
    compileOnly(project(":proxy"))
    compileOnly(project(":server"))

    compileOnly(libs.plasmovoice.server)
    compileOnly(libs.plasmovoice.proxy)
}

val platforms = setOf(
    project(":common"),
    project(":proxy"),
    project(":server")
)

platforms.forEach { evaluationDependsOn(":${it.name}") }

sourceSets {
    main {
        java {
            srcDir(platforms.map { it.sourceSets.main.get().java.srcDirs }.flatten())
        }

        resources {
            println(platforms.map { it.sourceSets.main.get().resources.srcDirs }.flatten())
            srcDir(platforms.map { it.sourceSets.main.get().resources.srcDirs }.flatten())
        }
    }
}
