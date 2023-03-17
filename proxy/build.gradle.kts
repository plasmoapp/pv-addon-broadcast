dependencies {
    compileOnly(project(":common"))

    compileOnly("su.plo.voice.api:proxy:2.0.0+ALPHA")
}

plasmoCrowdin {
    projectId = "plasmo-voice-addons"
    sourceFileName = "proxy/broadcast.toml"
    resourceDir = "broadcast/proxy/languages"
    createList = true
}

tasks {
    processResources {
        dependsOn(plasmoCrowdinDownload)
    }
}
