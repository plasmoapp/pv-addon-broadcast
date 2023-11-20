plugins {
    id("su.plo.crowdin.plugin")
}

dependencies {
    compileOnly(project(":common"))

    compileOnly(libs.plasmovoice.server)
}

crowdin {
    projectId = "plasmo-voice-addons"
    sourceFileName = "server/broadcast.toml"
    resourceDir = "broadcast/server/languages"
    createList = true
}
