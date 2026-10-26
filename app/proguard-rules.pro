# Reglas R8 para el release (minify + shrinkResources).
# La mayoría de las librerías (Room, Hilt, Retrofit 3, OkHttp, Coil, CameraX, ML Kit, osmdroid)
# traen sus propias consumer rules; aquí solo se agregan las específicas de la app.

# kotlinx.serialization: conservar serializers generados de los DTOs.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers @kotlinx.serialization.Serializable class cl.oyl.frutossecos.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class cl.oyl.frutossecos.**$$serializer { *; }

# Retrofit: interfaces de API con genéricos/suspend.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface cl.oyl.frutossecos.data.remote.api.** { *; }

# Enums guardados en Room por nombre.
-keepclassmembers enum cl.oyl.frutossecos.domain.model.** { *; }

# osmdroid
-dontwarn org.osmdroid.**
-dontwarn org.slf4j.**
