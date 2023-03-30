plugins {
    kotlin("jvm")
    id("su.plo.voice.plugin") version("1.0.0")
}

dependencies {
    compileOnly(project(":proxy"))
    compileOnly(project(":server"))

    compileOnly("su.plo.voice.api:server:2.0.0+ALPHA")
    compileOnly("su.plo.voice.api:proxy:2.0.0+ALPHA")

    annotationProcessor("su.plo.voice.api:server:2.0.0+ALPHA")
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
