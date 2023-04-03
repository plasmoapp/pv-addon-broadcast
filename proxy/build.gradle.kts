val plasmoVoiceVersion: String by rootProject

plugins {
    id("su.plo.crowdin.plugin")
}

dependencies {
    compileOnly(project(":common"))

    compileOnly("su.plo.voice.api:proxy:$plasmoVoiceVersion")
}

plasmoCrowdin {
    projectId = "plasmo-voice-addons"
    sourceFileName = "proxy/broadcast.toml"
    resourceDir = "broadcast/proxy/languages"
    createList = true
}
