package org.utbot.java.compilation.database.impl.tree

import org.utbot.java.compilation.database.impl.fs.ClassByteCodeSource
import java.util.concurrent.ConcurrentLinkedQueue

class ClassNode(
    simpleName: String,
    packageNode: PackageNode,
    val source: ClassByteCodeSource
) : AbstractNode(simpleName, packageNode) {

    override val name: String = simpleName

    val location get() = source.location

    private val _subTypes = ConcurrentLinkedQueue<ClassNode>()

    val subTypes: List<ClassNode> get() = _subTypes.toList()

    fun addSubType(subTypeNode: ClassNode) {
        _subTypes.add(subTypeNode)
    }

    fun removeSubTypesOfVersion(version: String) {
        val toBeRemoved = subTypes.filter { it.location.version == version }.toSet()
        _subTypes.removeAll(toBeRemoved)
    }

    suspend fun info() = source.meta()

}