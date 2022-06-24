package org.utbot.jcdb.impl

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.utbot.jcdb.api.ClasspathSet
import org.utbot.jcdb.api.FieldUsageMode
import org.utbot.jcdb.compilationDatabase
import org.utbot.jcdb.impl.fields.FieldA
import org.utbot.jcdb.impl.fields.FieldB
import org.utbot.jcdb.impl.index.ReversedUsagesIndex
import org.utbot.jcdb.impl.index.reversedUsagesExt
import org.utbot.jcdb.impl.methods.MethodA

class SearchReversedUsagesTest : LibrariesMixin {

    companion object : LibrariesMixin {

        private lateinit var cp: ClasspathSet

        @BeforeAll
        @JvmStatic
        fun setup() = runBlocking {
            val db = compilationDatabase {
                predefinedDirOrJars = allClasspath
                useProcessJavaRuntime()
                installIndexes(ReversedUsagesIndex)
            }
            cp = db.classpathSet(allClasspath)
        }
    }


    @Test
    fun `classes read fields`() {
        val usages = fieldsUsages<FieldA>(FieldUsageMode.READ)

        assertEquals(
            sortedMapOf(
                "a" to setOf(
                    "org.utbot.jcdb.impl.fields.FieldA#<init>",
                    "org.utbot.jcdb.impl.fields.FieldA#isPositive",
                    "org.utbot.jcdb.impl.fields.FieldA#useCPrivate",
                    "org.utbot.jcdb.impl.fields.FieldAImpl#hello"
                ),
                "b" to setOf(
                    "org.utbot.jcdb.impl.fields.FieldA#isPositive",
                    "org.utbot.jcdb.impl.fields.FieldA#useA",
                ),
                "fieldB" to setOf(
                    "org.utbot.jcdb.impl.fields.FieldA#useCPrivate",
                )
            ),
            usages
        )
    }

    @Test
    fun `classes write fields`() {
        val usages = fieldsUsages<FieldA>()
        assertEquals(
            sortedMapOf(
                "a" to setOf(
                    "org.utbot.jcdb.impl.fields.FieldA#<init>",
                    "org.utbot.jcdb.impl.fields.FieldA#useA",
                ),
                "b" to setOf(
                    "org.utbot.jcdb.impl.fields.FieldA#<init>"
                ),
                "fieldB" to setOf(
                    "org.utbot.jcdb.impl.fields.FieldA#<init>",
                )
            ),
            usages
        )
    }

    @Test
    fun `classes write fields coupled`() {
        val usages = fieldsUsages<FieldB>()
        assertEquals(
            sortedMapOf(
                "c" to setOf(
                    "org.utbot.jcdb.impl.fields.FakeFieldA#useCPrivate",
                    "org.utbot.jcdb.impl.fields.FieldA#useCPrivate",
                    "org.utbot.jcdb.impl.fields.FieldB#<init>",
                    "org.utbot.jcdb.impl.fields.FieldB#useCPrivate",
                )
            ),
            usages
        )
    }

    @Test
    fun `classes methods usages`() {
        val usages = methodsUsages<MethodA>()
        assertEquals(
            sortedMapOf(
                "<init>" to setOf(
                    "org.utbot.jcdb.impl.methods.MethodB#hoho",
                    "org.utbot.jcdb.impl.methods.MethodC#<init>"
                ),
                "hello" to setOf(
                    "org.utbot.jcdb.impl.methods.MethodB#hoho",
                    "org.utbot.jcdb.impl.methods.MethodC#hello",
                ),
                "hello1" to setOf(
                    "org.utbot.jcdb.impl.methods.MethodB#hoho"
                )
            ),
            usages
        )
    }





    private inline fun <reified T> fieldsUsages(mode: FieldUsageMode = FieldUsageMode.WRITE): Map<String, Set<String>> {
        return runBlocking {
            val classId = cp.findClassOrNull(T::class.java.name)
            assertNotNull(classId!!)
            val fields = classId.fields()
            val usageExt = cp.reversedUsagesExt

            fields.map {
                it.name to usageExt.findUsages(it, mode).map { it.classId.name + "#" + it.name }.toSortedSet()
            }
                .toMap()
                .filterNot { it.value.isEmpty() }
                .toSortedMap()
        }
    }

    private inline fun <reified T> methodsUsages(): Map<String, Set<String>> {
        return runBlocking {
            val classId = cp.findClassOrNull(T::class.java.name)
            assertNotNull(classId!!)
            val methods = classId.methods()
            val usageExt = cp.reversedUsagesExt

            methods.map {
                it.name to usageExt.findUsages(it).map { it.classId.name + "#" + it.name }.toSortedSet()
            }
                .toMap()
                .filterNot { it.value.isEmpty() }
                .toSortedMap()
        }
    }

    @AfterEach
    fun cleanup() {
        cp.close()
    }
}