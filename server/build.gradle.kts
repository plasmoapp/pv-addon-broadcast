val plasmoVoiceVersion: String by rootProject

plugins {
    id("su.plo.crowdin.plugin")
}

dependencies {
    compileOnly(project(":common"))

    compileOnly("su.plo.voice.api:server:$plasmoVoiceVersion")
}

plasmoCrowdin {
    projectId = "plasmo-voice-addons"
    sourceFileName = "server/broadcast.toml"
    resourceDir = "broadcast/server/languages"
    createList = true
}
