package io.shellify.app.core.engine

import android.content.Context
import android.os.Build
import android.util.Log
import dalvik.system.BaseDexClassLoader
import java.io.File

object GeckoNativeLoader {

    private const val TAG = "GeckoNativeLoader"
    private val PRELOAD_ORDER = listOf("libmozglue.so", "liblgpllibs.so", "libxul.so")

    fun injectAndLoad(context: Context) {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val libDir = File(context.filesDir, "gecko_engine/lib/$abi")
        if (!libDir.exists()) return
        injectDirIntoClassLoader(context, libDir)
        val all = libDir.listFiles()?.filter { it.extension == "so" }?.associateBy { it.name }
            ?: return
        val order =
            PRELOAD_ORDER.mapNotNull { all[it] } + all.values.filter { it.name !in PRELOAD_ORDER }
        for (lib in order) {
            try {
                System.load(lib.absolutePath)
                Log.d(TAG, "Loaded ${lib.name}")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Could not load ${lib.name}: ${e.message}")
            }
        }
    }

    private fun injectDirIntoClassLoader(context: Context, libDir: File) {
        try {
            val cl = context.classLoader
            if (cl !is BaseDexClassLoader) return
            val pathList = BaseDexClassLoader::class.java
                .getDeclaredField("pathList").also { it.isAccessible = true }.get(cl) ?: return
            val nativeField = pathList.javaClass
                .getDeclaredField("nativeLibraryPathElements").also { it.isAccessible = true }
            val existing = nativeField.get(pathList) as Array<*>
            val elemClass = existing.javaClass.componentType ?: return
            // Avoid adding the same dir twice
            if (existing.any { e ->
                    try {
                        elemClass.getDeclaredField("path").also { it.isAccessible = true }
                            .get(e) == libDir
                    } catch (_: Exception) {
                        false
                    }
                }) return
            val newElem = try {
                elemClass.getDeclaredConstructor(File::class.java)
                    .also { it.isAccessible = true }.newInstance(libDir)
            } catch (_: NoSuchMethodException) {
                elemClass.constructors.firstOrNull { it.parameterTypes.size == 1 }
                    ?.also { it.isAccessible = true }?.newInstance(libDir)
            } ?: return
            val newArray = java.lang.reflect.Array.newInstance(elemClass, existing.size + 1)
            java.lang.reflect.Array.set(newArray, 0, newElem)
            existing.forEachIndexed { i, e -> java.lang.reflect.Array.set(newArray, i + 1, e) }
            nativeField.set(pathList, newArray)
            Log.i(TAG, "Injected native lib dir: $libDir")
        } catch (e: Exception) {
            Log.w(TAG, "Classloader injection failed: ${e.message}")
        }
    }
}
