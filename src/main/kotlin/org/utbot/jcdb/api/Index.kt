package org.utbot.jcdb.api

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.InputStream
import java.io.OutputStream

/**
 * index builder
 */
interface ByteCodeLocationIndexBuilder<T> {

    fun index(classNode: ClassNode)

    fun index(classNode: ClassNode, methodNode: MethodNode)

    fun build(location: ByteCodeLocation): ByteCodeLocationIndex<T>

}

interface ByteCodeLocationIndex<T> {

    val location: ByteCodeLocation

    fun query(term: String): Sequence<T>
}

interface IndexInstaller<T, INDEX: ByteCodeLocationIndex<T>> {

    val key: String

    fun newBuilder() : ByteCodeLocationIndexBuilder<T>

    fun serialize(index: INDEX, out: OutputStream)

    fun deserialize(stream: InputStream): INDEX?

}
