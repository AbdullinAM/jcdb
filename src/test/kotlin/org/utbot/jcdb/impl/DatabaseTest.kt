package org.utbot.jcdb.impl

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.utbot.jcdb.api.*
import org.utbot.jcdb.compilationDatabase
import org.w3c.dom.DocumentType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DatabaseTest : LibrariesMixin {

    companion object : LibrariesMixin {
        private lateinit var db: CompilationDatabase

        @BeforeAll
        @JvmStatic
        fun setup() {
            db = runBlocking {
                compilationDatabase {
                    predefinedDirOrJars = allClasspath
                    useProcessJavaRuntime()
                }
            }
        }
    }


    @Test
    fun `find class from build dir folder`() = runBlocking {
        val cp = db.classpathSet(allClasspath)
        val clazz = cp.findClassOrNull(Foo::class.java.name)
        assertNotNull(clazz!!)
        assertEquals(Foo::class.java.name, clazz.name)
        assertTrue(clazz.isFinal())
        assertTrue(clazz.isPublic())
        assertFalse(clazz.isInterface())

        val annotations = clazz.annotations()
        assertTrue(annotations.size > 1)
        assertNotNull(annotations.firstOrNull { it.name == Nested::class.java.name })

        val fields = clazz.fields()
        assertEquals(2, fields.size)

        with(fields.first()) {
            assertEquals("foo", name)
            assertEquals("int", type()?.name)
        }
        with(fields[1]) {
            assertEquals("bar", name)
            assertEquals(String::class.java.name, type()?.name)
        }

        val methods = clazz.methods()
        assertEquals(5, methods.size)
        with(methods.first { it.name == "smthPublic" }) {
            assertEquals(1, parameters().size)
            assertEquals("int", parameters().first().name)
            assertTrue(isPublic())
        }

        with(methods.first { it.name == "smthPrivate" }) {
            assertTrue(parameters().isEmpty())
            assertTrue(isPrivate())
        }
    }

    @Test
    fun `find lazy-loaded class`() = runBlocking {
        val cp = db.classpathSet(emptyList())
        val domClass = cp.findClassOrNull(org.w3c.dom.Document::class.java.name)
        assertNotNull(domClass!!)

        assertTrue(domClass.isPublic())
        assertTrue(domClass.isInterface())

        val methods = domClass.methods()
        assertTrue(methods.isNotEmpty())
        with(methods.first { it.name == "getDoctype" }) {
            assertTrue(parameters().isEmpty())
            assertEquals(DocumentType::class.java.name, returnType()?.name)
            assertTrue(isPublic())
        }
    }

    @Test
    fun `find sub-types from lazy loaded classes`() = runBlocking {
        val cp = db.classpathSet(emptyList())
        val domClass = cp.findClassOrNull(org.w3c.dom.Document::class.java.name)
        assertNotNull(domClass!!)

        with(cp.findSubClasses(java.util.AbstractMap::class.java.name)) {
            assertTrue(size > 10) {
                "expected more then 10 but got only: ${joinToString { it.name }}"
            }

            assertNotNull(firstOrNull { it.name == EnumMap::class.java.name })
            assertNotNull(firstOrNull { it.name == HashMap::class.java.name })
            assertNotNull(firstOrNull { it.name == WeakHashMap::class.java.name })
            assertNotNull(firstOrNull { it.name == TreeMap::class.java.name })
            assertNotNull(firstOrNull { it.name == ConcurrentHashMap::class.java.name })
        }

        with(cp.findSubClasses(org.w3c.dom.Document::class.java.name)) {
            assertTrue(isNotEmpty())
        }
    }

    @Test
    fun `find sub-types with all hierarchy`() = runBlocking {
        val cp = db.classpathSet(allClasspath)
        val clazz = cp.findClassOrNull(SuperDuper::class.java.name)
        assertNotNull(clazz!!)

        with(cp.findSubClasses(clazz, allHierarchy = true)) {
            assertEquals(4, size) {
                "expected more then 10 but got only: ${joinToString { it.name }}"
            }

            assertNotNull(firstOrNull { it.name == A::class.java.name })
            assertNotNull(firstOrNull { it.name == B::class.java.name })
            assertNotNull(firstOrNull { it.name == C::class.java.name })
            assertNotNull(firstOrNull { it.name == D::class.java.name })
        }
    }
}

