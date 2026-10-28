package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jf.baksmali.Baksmali
import org.jf.baksmali.BaksmaliOptions
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.DexFile
import java.io.File

object DexDecompiler {

    suspend fun listClasses(
        apkFile: File,
        dexName: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val dex = loadDex(apkFile, dexName)
            dex.classes.map { it.type }.sorted()
        }
    }

    suspend fun decompileClass(
        apkFile: File,
        dexName: String,
        classType: String,
        cacheDir: File
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheTag = apkFile.nameWithoutExtension + "_" +
                    apkFile.length() + "_" +
                    dexName.replace('.', '_')
            val outDir = File(cacheDir, "smali/$cacheTag")
            if (!outDir.exists()) outDir.mkdirs()

            val smaliRelPath = classType
                .removePrefix("L")
                .removeSuffix(";")
                .replace('$', '_') + ".smali"
            val smaliFile = File(outDir, smaliRelPath)

            if (!smaliFile.exists()) {
                val dex = loadDex(apkFile, dexName)
                val options = BaksmaliOptions()
                Baksmali.disassembleDexFile(dex, outDir, 4, options)
            }

            if (smaliFile.exists()) {
                smaliFile.readText()
            } else {
                throw IllegalStateException("未找到类：$classType")
            }
        }
    }

    private fun loadDex(apkFile: File, dexName: String): DexFile {
        val entry = DexFileFactory.loadDexEntry(
            apkFile,
            dexName,
            true,
            Opcodes.getDefault()
        )
        return entry.dexFile
    }

    fun clearCache(cacheDir: File) {
        runCatching { File(cacheDir, "smali").deleteRecursively() }
    }
}