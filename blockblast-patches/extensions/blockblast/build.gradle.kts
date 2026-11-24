extension {
    name = "extensions/blockblast.mpe"
}

android {
    namespace = "app.blockblast.extension"
}

dependencies {
    // AppLovin MAX classes, present in the app at runtime.
    compileOnly(project(":stub"))
}
