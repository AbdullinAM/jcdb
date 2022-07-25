package org.utbot.jcdb.impl.fs

import org.utbot.jcdb.api.ByteCodeLoader
import org.utbot.jcdb.api.ByteCodeLocation
import org.utbot.jcdb.api.ClassLoadingContainer
import org.utbot.jcdb.api.LocationScope
import org.utbot.jcdb.impl.tree.ClassTree
import org.utbot.jcdb.impl.tree.LibraryClassTree
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
suspend fun ByteCodeLoader.load(classTree: ClassTree): Pair<LibraryClassTree, suspend () -> Unit> {
    val libraryTree = LibraryClassTree(location)
    val sync = allResources()
    sync.classesToLoad.forEach {
        val source = when {
            it.value != null -> ExpandedByteCodeSource(location, it.key)
            else -> LazyByteCodeSource(location, it.key)
        }
        val libraryNode = libraryTree.addClass(source)
        it.value?.let {
            libraryNode.source.load(it)
        }
    }
    sync.close()
    return libraryTree to {
        val async = asyncResources()()
        async?.classesToLoad?.forEach { (name, stream) ->
            val node = classTree.firstClassNodeOrNull(name)
            if (stream != null && node != null) {
                node.source.load(stream)
            } else {
                println("GETTING NULL STREAM OR NODE FOR $name")
            }
        }
        async?.close()
    }
}

/**
 * limits scope for search base on location. That means that sometimes there is no need to search for subclasses of
 * library class inside java runtime.
 *
 * @param location target location
 */
fun Collection<ByteCodeLocation>.relevantLocations(location: ByteCodeLocation?): Collection<ByteCodeLocation> {
    if (location?.scope != LocationScope.APP) {
        return this
    }
    return filter { it.scope == LocationScope.APP }
}