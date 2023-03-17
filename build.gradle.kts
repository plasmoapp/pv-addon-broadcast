plugins {
    id("java")
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
    apply(plugin = "su.plo.crowdin.plugin")

    dependencies {
        compileOnly("com.google.guava:guava:31.1-jre")
        compileOnly("com.google.code.gson:gson:2.9.0")
        compileOnly("org.jetbrains:annotations:23.0.0")
        compileOnly("org.projectlombok:lombok:1.18.24")
        compileOnly("su.plo.config:config:1.0.0")

        annotationProcessor("org.projectlombok:lombok:1.18.24")
        annotationProcessor("com.google.guava:guava:31.1-jre")
        annotationProcessor("com.google.code.gson:gson:2.9.0")
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

        maven {
            url = uri("https://repo.plo.su")
        }
    }
}
