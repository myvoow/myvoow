// Compile-only stubs of the AppLovin MAX API bundled in Block Blast. Never packaged: the extension
// uses compileOnly, and the real classes come from the app at runtime. Signatures match
// AppLovin MAX as shipped in Block Blast 11.0.6.
plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
