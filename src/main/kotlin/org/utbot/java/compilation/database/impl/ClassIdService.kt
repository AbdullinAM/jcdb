package org.utbot.java.compilation.database.impl

import kotlinx.collections.immutable.toImmutableMap
import org.utbot.java.compilation.database.api.ClassId
import org.utbot.java.compilation.database.api.MethodId
import org.utbot.java.compilation.database.impl.meta.ClassIdImpl
import org.utbot.java.compilation.database.impl.meta.MethodIdImpl
import org.utbot.java.compilation.database.impl.meta.MethodMetaInfo
import org.utbot.java.compilation.database.impl.meta.PredefinedPrimitive
import org.utbot.java.compilation.database.impl.tree.ClassNode
import org.utbot.java.compilation.database.impl.tree.ClasspathClassTree

class ClassIdService(internal val classpathClassTree: ClasspathClassTree) {

    companion object {
        private val predefinedClasses: Map<String, ClassId> =
            PredefinedPrimitive.values.associateBy { it.simpleName }.toImmutableMap()
    }

    fun toClassId(node: ClassNode?): ClassId? {
        node ?: return null
        return ClassIdImpl(node, this)
    }

    fun toClassId(fullName: String?): ClassId? {
        fullName ?: return null
        val predefinedClass = predefinedClasses[fullName]
        if (predefinedClass != null) {
            return predefinedClass
        }
        return toClassId(classpathClassTree.firstClassOrNull(fullName))
    }

    fun toMethodId(classId: ClassId, methodInfo: MethodMetaInfo, node: ClassNode): MethodId {
        return MethodIdImpl(methodInfo, node, classId, this)
    }

}