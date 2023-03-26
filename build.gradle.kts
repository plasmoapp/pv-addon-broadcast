plugins {
    id("java")
    kotlin("jvm") version "1.6.10"
    id("su.plo.crowdin.plugin") version("1.0.0") apply(false)
}

group = "su.plo"
version = "1.0.0-SNAPSHOT"

tasks {
    jar {
        dependsOn(project(":jar").tasks.build)

        from(project(":jar").sourceSets.main.get().output)
    }
}

subprojects {
    apply(plugin = "java")

    dependencies {
        annotationProcessor("org.projectlombok:lombok:1.18.24")
    }

    tasks {
        java {
            toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://repo.plo.su")
    }
}
