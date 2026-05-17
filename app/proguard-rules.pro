-keepattributes SourceFile,LineNumberTable
-keep class io.shellify.app.domain.model.** { *; }
-keep class io.shellify.app.data.local.entity.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
# snakeyaml is a transitive dependency that references java.beans.* which doesn't exist on Android
-dontwarn java.beans.**
-dontwarn org.yaml.snakeyaml.**

# GeckoView: all Java classes are called back from native libxul.so via JNI.
# R8's aggressive optimizations remove methods it considers unreachable from Java,
# causing null function-pointer dereferences (SIGSEGV) in the native launcher thread.
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-keep interface org.mozilla.geckoview.** { *; }
-keepclassmembers class org.mozilla.geckoview.** { *; }
-keepclassmembers class * extends org.mozilla.geckoview.GeckoSession$* { *; }
-dontwarn org.mozilla.geckoview.**
