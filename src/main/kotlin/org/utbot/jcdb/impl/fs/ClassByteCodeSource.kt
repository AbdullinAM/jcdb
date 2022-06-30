package org.utbot.jcdb.impl.fs

import kotlinx.collections.immutable.toImmutableList
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.utbot.jcdb.api.ByteCodeLocation
import org.utbot.jcdb.impl.suspendableLazy
import org.utbot.jcdb.impl.types.AnnotationInfo
import org.utbot.jcdb.impl.types.ClassInfo
import org.utbot.jcdb.impl.types.FieldInfo
import org.utbot.jcdb.impl.types.MethodInfo
import java.io.InputStream

abstract class ClassByteCodeSource(val location: ByteCodeLocation, val className: String) {

    abstract suspend fun info(): ClassInfo
    abstract suspend fun fullByteCode(): ClassNode

    abstract fun onAfterIndexing()

    abstract fun load(input: InputStream)

    protected suspend fun classInputStream(): InputStream? {
        return location.resolve(className)
    }

    protected fun ClassNode.asClassInfo() = ClassInfo(
        name = Type.getObjectType(name).className,
        access = access,

        outerClass = outerClassName(),
        innerClasses = innerClasses.map {
            Type.getObjectType(it.name).className
        },
        outerMethod = outerMethod,
        outerMethodDesc = outerMethodDesc,
        superClass = superName?.let { Type.getObjectType(it).className },
        interfaces = interfaces.map { Type.getObjectType(it).className }.toImmutableList(),
        methods = methods.map { it.asMethodInfo() }.toImmutableList(),
        fields = fields.map { it.asFieldInfo() }.toImmutableList(),
        annotations = visibleAnnotations.orEmpty().map { it.asAnnotationInfo() }.toImmutableList()
    )

    private fun ClassNode.outerClassName(): String? {
        val direct = outerClass?.let { Type.getObjectType(it).className }
        if (direct == null && innerClasses.size == 1) {
            val outerClass = innerClasses.firstOrNull { it.name == name }
            if (outerClass != null) {
                return Type.getObjectType(outerClass.outerName).className
            }
        }
        return direct
    }

    private fun AnnotationNode.asAnnotationInfo() = AnnotationInfo(
        className = Type.getType(desc).className
    )

    private fun MethodNode.asMethodInfo() = MethodInfo(
        name = name,
        desc = desc,
        access = access,
        returnType = Type.getReturnType(desc).className,
        parameters = Type.getArgumentTypes(desc).map { it.className }.toImmutableList(),
        annotations = visibleAnnotations.orEmpty().map { it.asAnnotationInfo() }.toImmutableList()
    )

    private fun FieldNode.asFieldInfo() = FieldInfo(
        name = name,
        access = access,
        type = Type.getType(desc).className,
        annotations = visibleAnnotations.orEmpty().map { it.asAnnotationInfo() }.toImmutableList()
    )

    suspend fun loadMethod(name: String, desc: String): MethodNode? {
        return fullByteCode().methods.first { it.name == name && it.desc == desc }
    }

}


class LazyByteCodeSource(location: ByteCodeLocation, className: String) :
    ClassByteCodeSource(location, className) {

    private lateinit var classInfo: ClassInfo

    @Volatile
    private var classNode: ClassNode? = null

    override fun load(input: InputStream) {
        val bytes = input.use { it.readBytes() }
        val classNode = ClassNode(Opcodes.ASM9)
        ClassReader(bytes).accept(classNode, ClassReader.EXPAND_FRAMES)

        this.classNode = classNode
        this.classInfo = classNode.asClassInfo()
    }

    override suspend fun info(): ClassInfo {
        if (this::classInfo.isInitialized) {
            return classInfo
        }
        return (this.classNode ?: fullByteCode()).asClassInfo().also {
            classInfo = it
        }
    }

    override fun onAfterIndexing() {
        classNode = null
    }

    override suspend fun fullByteCode(): ClassNode {
        val node = classNode
        if (node != null) {
            return node
        }
        val bytes = classInputStream()?.use { it.readBytes() }
        bytes ?: throw IllegalStateException("can't find bytecode for class $className in $location")
        return ClassNode(Opcodes.ASM9).also {
            ClassReader(bytes).accept(it, ClassReader.EXPAND_FRAMES)
        }
    }
}

class ExpandedByteCodeSource(location: ByteCodeLocation, className: String) :
    ClassByteCodeSource(location, className) {

    @Volatile
    private var cachedByteCode: ClassNode? = null

    private val lazyClassInfo = suspendableLazy {
        fullByteCode().asClassInfo()
    }

    override fun load(input: InputStream) {
        val bytes = input.use { it.readBytes() }
        val classNode = ClassNode(Opcodes.ASM9)
        ClassReader(bytes).accept(classNode, ClassReader.EXPAND_FRAMES)
        cachedByteCode = classNode
    }

    override suspend fun info() = lazyClassInfo()
    override suspend fun fullByteCode(): ClassNode {
        val cached = cachedByteCode
        if (cached == null) {
            val bytes = classInputStream()?.use { it.readBytes() }
            bytes ?: throw IllegalStateException("can't find bytecode for class $className in $location")
            val classNode = ClassNode(Opcodes.ASM9).also {
                ClassReader(bytes).accept(it, ClassReader.EXPAND_FRAMES)
            }
            cachedByteCode = classNode
            return classNode
        }
        return cached
    }

    override fun onAfterIndexing() {
        // do nothing. this is expected to be hot code
    }

}