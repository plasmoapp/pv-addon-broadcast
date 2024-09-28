import java.net.URI

plugins {
    id("su.plo.crowdin.plugin")
}

dependencies {
    compileOnly(project(":common"))

    compileOnly(libs.plasmovoice.proxy)
}

crowdin {
    url = URI.create("https://github.com/plasmoapp/plasmo-voice-crowdin/archive/refs/heads/addons.zip").toURL()
    sourceFileName = "proxy/broadcast.toml"
    resourceDir = "broadcast/proxy/languages"
    createList = true
}
