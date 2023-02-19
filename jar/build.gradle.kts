dependencies {
    compileOnly("su.plo.voice.api:server:2.0.0+ALPHA")
    compileOnly("su.plo.voice.api:proxy:2.0.0+ALPHA")

    annotationProcessor("su.plo.voice.api:server:2.0.0+ALPHA")
}

val platforms = setOf(
    project(":common"),
    project(":proxy"),
    project(":server")
)

sourceSets {
    main {
        java {
            srcDir(platforms.map { it.sourceSets.main.get().java.srcDirs }.flatten())
        }

        resources {
            srcDir(platforms.map { it.sourceSets.main.get().resources.srcDirs }.flatten())
        }
    }
}
