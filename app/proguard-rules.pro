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
