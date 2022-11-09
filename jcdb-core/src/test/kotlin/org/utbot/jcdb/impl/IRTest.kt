package org.utbot.jcdb.impl

import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import org.utbot.jcdb.api.JcClassOrInterface
import org.utbot.jcdb.api.ext.findClass
import org.utbot.jcdb.api.methods
import org.utbot.jcdb.api.packageName
import org.utbot.jcdb.impl.cfg.*
import java.net.URLClassLoader
import java.nio.file.Files

class IRTest : BaseTest() {
    val target = Files.createTempDirectory("jcdb-temp")

    companion object : WithDB()

    @Test
    fun `get ir of simple method`() {
        testClass(cp.findClass<IRExamples>())
    }

    @Test
    fun `get ir of algorithms lesson 1`() {
        testClass(cp.findClass<JavaTasks>())
    }

    @Test
    fun `get ir of binary search tree`() {
        testClass(cp.findClass<BinarySearchTree<*>>())
        testClass(cp.findClass<BinarySearchTree<*>.BinarySearchTreeIterator>())
    }

    private fun testClass(klass: JcClassOrInterface) {
        val classNode = klass.bytecode()
        classNode.methods = klass.methods.map {
            val oldBody = it.body()
            println()
            println("Old body: ${oldBody.print()}")
            val instructionList = it.instructionList()
            println("Instruction list: $instructionList")
            val newBody = MethodNodeBuilder(it, instructionList).build()
            println("New body: ${newBody.print()}")
            println()
            newBody
        }
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        val checker = CheckClassAdapter(cw)
        classNode.accept(checker)
        cw.toByteArray()
        val targetDir = target.resolve(klass.packageName.replace('.', '/'))
        val targetFile = targetDir.resolve("${klass.simpleName}.class").toFile().also {
            it.parentFile?.mkdirs()
        }
        targetFile.writeBytes(cw.toByteArray())

        val classloader = URLClassLoader(arrayOf(target.toUri().toURL()))
        classloader.loadClass(klass.name)
    }
}
