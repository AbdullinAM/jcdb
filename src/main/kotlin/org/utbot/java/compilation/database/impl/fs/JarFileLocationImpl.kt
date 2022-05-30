package org.utbot.java.compilation.database.impl.fs

import mu.KLogging
import org.utbot.java.compilation.database.api.ByteCodeLoader
import org.utbot.java.compilation.database.api.md5
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.streams.toList

class JarFileLocationImpl(
    val file: File,
    private val syncLoadClassesOnlyFrom: List<String>?
) : AbstractByteCodeLocation() {
    companion object : KLogging()

    override fun getCurrentId(): String {
        return (file.absolutePath + file.lastModified()).md5()
    }

    override fun createRefreshed() = JarFileLocationImpl(file, syncLoadClassesOnlyFrom)

    override suspend fun loader(): ByteCodeLoader? {
        try {
            val sync = jarClasses() ?: return null
            val classes = sync.second.mapValues { (className, jar) ->
                when (className.matchesOneOf(syncLoadClassesOnlyFrom)) {
                    true -> jar.first.getInputStream(jar.second)
                    else -> null // lazy
                }
            }
            val allClasses = LoadingContainerImpl(classes) { sync.first.close() }
            return ByteCodeLoaderImpl(this, allClasses) {
                val (jar, foundClasses) = jarClasses() ?: return@ByteCodeLoaderImpl null
                LoadingContainerImpl(foundClasses.filterKeys { className ->
                    !className.matchesOneOf(syncLoadClassesOnlyFrom)
                }.mapValues { (_, jar) ->
                    jar.first.getInputStream(jar.second)
                }) {
                    jar.close()
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "error loading classes from build folder: ${file.absolutePath}. returning empty loader" }
            return null
        }
    }

    override suspend fun resolve(classFullName: String): InputStream? {
        val jar = jarFile() ?: return null
        val jarEntryName = classFullName.replace(".", "/") + ".class"
        val jarEntry = jar.getJarEntry(jarEntryName)
        return object : BufferedInputStream(jar.getInputStream(jarEntry)) {
            override fun close() {
                super.close()
                jar.close()
            }
        }
    }

    private fun jarClasses(): Pair<JarFile, Map<String, Pair<JarFile, JarEntry>>>? {
        val jarFile = jarFile() ?: return null

        return jarFile to jarFile.stream().filter { it.name.endsWith(".class") }.map {
            val className = it.name.removeSuffix(".class").replace("/", ".")
            className to (jarFile to it)
        }.toList().toMap()
    }

    private fun jarFile(): JarFile? {
        if (!file.exists() || !file.isFile) {
            return null
        }

        try {
            return JarFile(file)
        } catch (e: Exception) {
            logger.warn(e) { "error processing jar located ${file.absolutePath}" }
            return null
        }
    }

    override fun toString(): String = file.absolutePath
}