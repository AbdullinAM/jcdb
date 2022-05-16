package com.huawei.java.compilation.database.impl.tree

import com.huawei.java.compilation.database.api.ByteCodeLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ClassTreeTest {

    private val classTree = ClassTree()
    private val lib1 = DummyCodeLocation("xxx")
    private val lib2 = DummyCodeLocation("yyy")

    @Test
    fun `handle classes at top level`() {
        classTree.addClass(lib1, "Simple")
        assertNull(lib1.findNode("Simple1"))
        assertEquals("Simple", lib1.findNode("Simple")?.simpleName)
        assertNull(lib2.findNode("Simple"))
    }
    
    @Test
    fun `handle classes at intermediate level`() {
        classTree.addClass(lib1, "xxx.Simple")
        classTree.addClass(lib2, "xxx.Simple")
        classTree.addClass(lib1, "xxx.yyy.Simple")

        assertEquals("Simple", lib1.findNode("xxx.Simple")?.simpleName)
        assertEquals("Simple", lib2.findNode("xxx.Simple")?.simpleName)
        assertNull(lib2.findNode("xxx.yyy.Simple"))

        lib1.findNode("xxx.yyy.Simple").let {
            assertEquals("Simple", it?.simpleName)
            assertEquals("xxx.yyy.Simple", it?.fullName)
        }
    }
    
    private fun ByteCodeLocation.findNode(name: String): ClassNode? {
        return classTree.findClassNodeOrNull(this, name)
    }
    
}