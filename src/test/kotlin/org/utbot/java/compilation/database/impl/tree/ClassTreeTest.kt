package org.utbot.java.compilation.database.impl.tree

import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.utbot.java.compilation.database.api.ByteCodeLocation
import org.utbot.java.compilation.database.impl.fs.ClassByteCodeSource

class ClassTreeTest {

    private val classTree = ClassTree()
    private val lib1 = DummyCodeLocation("xxx")
    private val lib2 = DummyCodeLocation("yyy")

    @Test
    fun `handle classes at top level`() {
        classTree.addClass(lib1.classSource("Simple"))
        assertNull(lib1.findNode("Simple1"))
        assertEquals("Simple", lib1.findNode("Simple")?.name)
        assertNull(lib2.findNode("Simple"))
    }

    @Test
    fun `handle classes at intermediate level`() {
        classTree.addClass(lib1.classSource("xxx.Simple"))
        classTree.addClass(lib2.classSource("xxx.Simple"))
        classTree.addClass(lib1.classSource("xxx.yyy.Simple"))

        assertEquals("Simple", lib1.findNode("xxx.Simple")?.name)
        assertEquals("Simple", lib2.findNode("xxx.Simple")?.name)
        assertNull(lib2.findNode("xxx.yyy.Simple"))

        lib1.findNode("xxx.yyy.Simple").let {
            assertEquals("Simple", it?.name)
            assertEquals("xxx.yyy.Simple", it?.fullName)
        }
    }

    @Test
    fun `handle classes at limited tree`() {
        val limitedTree = ClasspathClassTree(classTree, persistentListOf(lib1))
        classTree.addClass(lib2.classSource("xxx.Simple"))
        classTree.addClass(lib1.classSource("xxx.Simple"))
        classTree.addClass(lib2.classSource("xxx.zzz.Simple"))

        with(limitedTree.firstClassOrNull("xxx.Simple")) {
            assertNotNull(this!!)
            assertEquals("Simple", name)
            assertEquals(lib1, location)
        }

        assertNull(limitedTree.firstClassOrNull("xxx.zzz.Simple"))
    }

    @Test
    fun `dropping versions`() {
        val limitedTree = ClasspathClassTree(classTree, persistentListOf(lib1))

        classTree.addClass(lib2.classSource("xxx.Simple"))
        classTree.addClass(lib1.classSource("xxx.Simple"))
        classTree.addClass(lib2.classSource("xxx.zzz.Simple"))

        classTree.visit(RemoveVersionsVisitor(setOf(lib2)))

        with(limitedTree.firstClassOrNull("xxx.Simple")) {
            assertNotNull(this!!)
            assertEquals("Simple", name)
            assertEquals(lib1, location)
        }

        assertNull(limitedTree.firstClassOrNull("xxx.zzz.Simple"))
    }

    private fun ByteCodeLocation.findNode(name: String): ClassNode? {
        return classTree.findClassNodeOrNull(this, name)
    }

    private fun ByteCodeLocation.classSource(name: String): ClassByteCodeSource {
        return ClassByteCodeSource(
            apiLevel = apiLevel,
            className = name,
            location = this
        )
    }

}