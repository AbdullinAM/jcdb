package org.utbot.java.compilation.database.impl.fs

import mu.KLogging
import org.utbot.java.compilation.database.api.ByteCodeLocation
import java.io.File

val logger = object : KLogging() {}.logger

fun File.asByteCodeLocation(loadClassesOnlyFrom: List<String>? = null): ByteCodeLocation {
    if (!exists()) {
        throw IllegalArgumentException("file $absolutePath doesn't exist")
    }
    if (isFile && name.endsWith(".jar")) {
        return JarFileLocationImpl(this, loadClassesOnlyFrom?.toList())
    } else if (isFile && name.endsWith(".jmod")) {
        return JmodByteCodeLocation(this, loadClassesOnlyFrom?.toList())
    } else if (!isFile) {
        return BuildFolderLocationImpl(this, loadClassesOnlyFrom)
    }
    throw IllegalArgumentException("file $absolutePath is not jar-file nor build dir folder")
}

fun List<File>.filterExisted(): List<File> = filter { file ->
    file.exists().also {
        if (!it) {
            logger.warn("${file.absolutePath} doesn't exists. make sure there is no mistake")
        }
    }
}

fun String.matchesOneOf(loadClassesOnlyFrom: List<String>?): Boolean {
    loadClassesOnlyFrom ?: return true
    return loadClassesOnlyFrom.any {
        startsWith(it)
    }
}