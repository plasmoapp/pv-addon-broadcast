plugins {
    id("su.plo.crowdin.plugin")
}

dependencies {
    compileOnly(project(":common"))

    compileOnly(libs.plasmovoice.proxy)
}

crowdin {
    projectId = "plasmo-voice-addons"
    sourceFileName = "proxy/broadcast.toml"
    resourceDir = "broadcast/proxy/languages"
    createList = true
}
