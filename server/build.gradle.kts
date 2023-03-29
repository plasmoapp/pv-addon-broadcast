plugins {
    id("su.plo.crowdin.plugin")
}

dependencies {
    compileOnly(project(":common"))

    compileOnly("su.plo.voice.api:server:2.0.0+ALPHA")
}

plasmoCrowdin {
    projectId = "plasmo-voice-addons"
    sourceFileName = "server/broadcast.toml"
    resourceDir = "broadcast/server/languages"
    createList = true
}
