package org.utbot.java.compilation.database.impl.fs

import org.utbot.java.compilation.database.api.ByteCodeLoader
import org.utbot.java.compilation.database.api.ByteCodeLocation
import org.utbot.java.compilation.database.api.ClassLoadingContainer
import org.utbot.java.compilation.database.impl.tree.ClassTree
import java.io.InputStream

class ByteCodeLoaderImpl(
    override val location: ByteCodeLocation,
    private val sync: ClassLoadingContainer,
    private val async: suspend () -> ClassLoadingContainer?
) : ByteCodeLoader {

    constructor(
        location: ByteCodeLocation,
        sync: Map<String, InputStream?>,
        async: suspend () -> Map<String, InputStream?>
    ) : this(location, ClassLoadingContainerImpl(sync), {
        ClassLoadingContainerImpl(async())
    })

    override suspend fun allResources() = sync

    override suspend fun asyncResources() = async

}

class ClassLoadingContainerImpl(
    override val classesToLoad: Map<String, InputStream?>,
    val onClose: () -> Unit = {}
) : ClassLoadingContainer {

    override fun close() {
        onClose()
    }
}


/**
 * load sync part into the tree and returns lambda that will do async part
 */
suspend fun ByteCodeLoader.load(classTree: ClassTree): suspend () -> Unit {
    val sync = allResources()
    sync.classesToLoad.forEach {
        ClassByteCodeSource(location = location, it.key).also { source ->
            val node = classTree.addClass(source)
            it.value?.let {
                source.preLoad(it)
                classTree.notifyOnMetaLoaded(node)
            }
        }
    }
    sync.close()
    return {
        val async = asyncResources()()
        async?.classesToLoad?.forEach { entry ->
            val node = classTree.firstClassNodeOrNull(entry.key)
            val stream = entry.value
            if (stream != null && node != null) {
                node.source.preLoad(stream)
                classTree.notifyOnMetaLoaded(node)
            }
        }
        async?.close()
    }
}
