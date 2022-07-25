package org.utbot.jcdb.impl.types

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.utbot.jcdb.api.*
import org.utbot.jcdb.api.ext.findClassOrNull

class ArrayClassIdImpl(override val elementClass: ClassId) : ArrayClassId {

    override val name = elementClass.name + "[]"
    override val simpleName = elementClass.simpleName + "[]"

    override val location: ByteCodeLocation?
        get() = elementClass.location

    override val classpath: ClasspathSet
        get() = elementClass.classpath

    override suspend fun byteCode(): ClassNode? {
        return null
    }

    override suspend fun innerClasses() = emptyList<ClassId>()

    override suspend fun outerClass() = null

    override suspend fun isAnonymous() = false

    override suspend fun signature() = Raw

    override suspend fun outerMethod() = null

    override suspend fun methods() = emptyList<MethodId>()

    override suspend fun superclass(): ClassId {
        return classpath.findClassOrNull<Any>() ?: throwClassNotFound<Any>()
    }

    override suspend fun interfaces() = emptyList<ClassId>()

    override suspend fun annotations() = emptyList<ClassId>()

    override suspend fun fields() = emptyList<FieldId>()

    override suspend fun access() = Opcodes.ACC_PUBLIC

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayClassIdImpl

        if (elementClass != other.elementClass) return false

        return true
    }

    override fun hashCode(): Int {
        return elementClass.hashCode()
    }

}

/**
 * Predefined arrays of primitive types
 */
val ClasspathSet.booleanArray get() = ArrayClassIdImpl(boolean)
val ClasspathSet.shortArray get() = ArrayClassIdImpl(short)
val ClasspathSet.intArray get() = ArrayClassIdImpl(int)
val ClasspathSet.longArray get() = ArrayClassIdImpl(long)
val ClasspathSet.floatArray get() = ArrayClassIdImpl(float)
val ClasspathSet.doubleArray get() = ArrayClassIdImpl(double)
val ClasspathSet.byteArray get() = ArrayClassIdImpl(byte)
val ClasspathSet.charArray get() = ArrayClassIdImpl(char)