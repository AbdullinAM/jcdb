package org.utbot.jcdb.impl

import org.utbot.jcdb.api.ByteCodeLocation
import org.utbot.jcdb.api.ByteCodeLocationIndex
import org.utbot.jcdb.api.Feature
import org.utbot.jcdb.api.GlobalIdsStore
import org.utbot.jcdb.impl.index.index
import org.utbot.jcdb.impl.storage.PersistentEnvironment
import org.utbot.jcdb.impl.tree.ClassNode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

class FeaturesRegistry(
    private val persistence: PersistentEnvironment? = null,
    val globalIdsStore: GlobalIdsStore,
    val features: List<Feature<*, *>>
) : Closeable {
    private val indexes: ConcurrentHashMap<ByteCodeLocation, ConcurrentHashMap<String, ByteCodeLocationIndex<*>>> = ConcurrentHashMap()

    fun <T, INDEX : ByteCodeLocationIndex<T>> append(
        location: ByteCodeLocation,
        feature: Feature<T, INDEX>,
        index: INDEX
    ) {
        location.indexes[feature.key] = index
    }

    suspend fun index(location: ByteCodeLocation, classes: Collection<ClassNode>) {
        features.forEach { feature ->
            feature.index(location, classes)
        }
        classes.forEach { node ->
            node.onAfterIndexing()
        }
    }

    private suspend fun <T, INDEX : ByteCodeLocationIndex<T>> Feature<T, INDEX>.index(
        location: ByteCodeLocation,
        classes: Collection<ClassNode>
    ) {
        val builder = newBuilder(globalIdsStore)
        classes.forEach { node ->
            index(node, builder)
        }
        val index = builder.build(location)
        location.indexes[key] = index
        val entity = persistence?.locationStore?.findOrNew(location)
        if (entity != null) {
            val out = ByteArrayOutputStream()
            serialize(index, out)
            persistence?.transactional {
                entity.index(key, ByteArrayInputStream(out.toByteArray()))
            }
        }
    }

    private val ByteCodeLocation.indexes: ConcurrentHashMap<String, ByteCodeLocationIndex<*>>
        get() {
            return this@FeaturesRegistry.indexes.getOrPut(this) {
                ConcurrentHashMap()
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
