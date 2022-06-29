package org.utbot.jcdb.impl

import org.utbot.jcdb.api.ByteCodeLocation
import org.utbot.jcdb.api.ByteCodeLocationIndex
import org.utbot.jcdb.api.IndexInstaller
import org.utbot.jcdb.impl.index.index
import org.utbot.jcdb.impl.tree.ClassNode
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

class IndexesRegistry(private val installers: List<IndexInstaller<*, *>>) : Closeable {

    private val indexes = ConcurrentHashMap<ByteCodeLocation, ConcurrentHashMap<String, ByteCodeLocationIndex<*>>>()

    suspend fun index(location: ByteCodeLocation, classes: Collection<ClassNode>) {
        val existedIndexes = indexes.getOrPut(location) {
            ConcurrentHashMap()
        }
        val builders = installers.associate { it.key to it.newBuilder() }
        classes.forEach { node ->
            installers.forEach { installer ->
                val builder = builders[installer.key]
                if (builder != null) {
                    index(node, builder)
                }
            }
            node.onAfterIndexing()
        }
        installers.forEach { installer ->
            val index = builders[installer.key]?.build(location)
            if (index != null) {
                existedIndexes.put(installer.key, index)
            }
        }
    }

    fun removeIndexes(location: ByteCodeLocation) {
        indexes.remove(location)
    }

    fun <T> findIndex(key: String, location: ByteCodeLocation): ByteCodeLocationIndex<T>? {
        return indexes[location]?.get(key) as? ByteCodeLocationIndex<T>
    }

    override fun close() {
        indexes.clear()
    }
}
