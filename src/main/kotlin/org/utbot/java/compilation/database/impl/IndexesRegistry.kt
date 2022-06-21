package org.utbot.java.compilation.database.impl

import org.utbot.java.compilation.database.api.ByteCodeLocation
import org.utbot.java.compilation.database.impl.index.*
import org.utbot.java.compilation.database.impl.tree.ClassNode
import java.util.concurrent.ConcurrentHashMap

typealias IndexBuilder<T> = (ByteCodeLocation) -> ByteCodeLocationIndexBuilder<T>

class IndexesRegistry {
    private val indexes = ConcurrentHashMap<ByteCodeLocation, ConcurrentHashMap<String, ByteCodeLocationIndex<*>>>()
    private val subtypesIndexBuilder: IndexBuilder<String> = { SubClassesIndexBuilder(it) }

    suspend fun index(location: ByteCodeLocation, classes: Collection<ClassNode>) {
        val subtypesIndex = location.index(classes, subtypesIndexBuilder)
        val existedIndexes = indexes.getOrPut(location) {
            ConcurrentHashMap()
        }
        existedIndexes[subtypesIndex.key] = subtypesIndex
    }

    fun removeIndexes(location: ByteCodeLocation) {
        indexes.remove(location)
    }

    fun subClassesIndex(location: ByteCodeLocation): ByteCodeLocationIndex<String>? {
        return indexes[location]?.get(SubClassesIndex.KEY) as? ByteCodeLocationIndex<String>
    }

}
