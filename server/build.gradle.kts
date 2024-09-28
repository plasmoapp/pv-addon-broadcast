import java.net.URI

plugins {
    id("su.plo.crowdin.plugin")
}

dependencies {
    compileOnly(project(":common"))

    compileOnly(libs.plasmovoice.server)
}

crowdin {
    url = URI.create("https://github.com/plasmoapp/plasmo-voice-crowdin/archive/refs/heads/addons.zip").toURL()
    sourceFileName = "server/broadcast.toml"
    resourceDir = "broadcast/server/languages"
    createList = true
}
