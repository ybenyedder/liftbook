# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.hevyclone.app.**$$serializer { *; }
-keepclassmembers class com.hevyclone.app.** { *** Companion; }
-keepclasseswithmembers class com.hevyclone.app.** { kotlinx.serialization.KSerializer serializer(...); }
