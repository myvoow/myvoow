group = "app.blockblast"

patches {
    about {
        name = "Block Blast Patches"
        description = "No ads, no telemetry, instant rewards for Block Blast!"
        source = "git@github.com:myvoow/blockblast-patches.git"
        author = "myvoow"
        contact = "https://github.com/myvoow/blockblast-patches/issues"
        website = "https://github.com/myvoow/blockblast-patches"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available to the generatePatchesList task
// but never bundled into the patches.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Writes patches-list.json from the built patches"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
