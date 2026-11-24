# The patches call the extension by name: its classes keep their names and members.
-keep class app.blockblast.extension.** {
  *;
}

# No optimization: the patches replace the bodies of the flag getters (NetBlock.blockAds and
# NetBlock.blockTelemetry), so their values must not be inlined into the callers.
-dontoptimize
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,SourceFile,LineNumberTable

# The AppLovin classes come from the app at runtime.
-dontwarn **
