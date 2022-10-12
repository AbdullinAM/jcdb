package org.utbot.jcdb.impl.types

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.utbot.jcdb.api.JcClassType
import org.utbot.jcdb.api.JcTypeVariable
import java.io.Closeable

class InnerTypesTest : BaseTypesTest() {

    @Test
    fun `inner classes linked to method`() {
        runBlocking {
            val classWithInners = findClassType<InnerClasses<*>>().assertClassType()
            val inners = classWithInners.innerTypes()
            assertEquals(4, inners.size)
            val methodBinded = inners.first { it.typeName == "org.utbot.jcdb.impl.types.InnerClasses\$1" }
            with(methodBinded.fields()) {
                with(first { it.name == "stateT" }) {
                    assertEquals("T", (fieldType() as JcTypeVariable).symbol)
                }
                with(first { it.name == "stateW" }) {
                    assertEquals("W", fieldType().typeName)
                }
            }
            with(methodBinded.outerMethod()!!) {
                assertEquals("run", name)
            }
            with(methodBinded.outerType()!!) {
                assertEquals("org.utbot.jcdb.impl.types.InnerClasses<W : java.lang.Object>", typeName)
            }
        }
    }
    @Test
    fun `get inner types`() {
        runBlocking {
            val innerClasses = findClassType<InnerClasses<*>>().innerTypes()
            assertEquals(4, innerClasses.size)
            with(innerClasses.first { it.typeName.contains("InnerState") }) {
                val fields = fields()
                assertEquals(2, fields.size)

                with(fields.first { it.name == "stateW" }) {
                    with(fieldType().assertIs<JcTypeVariable>()) {
                        assertEquals("W", symbol)
                    }
                }
            }

            with(innerClasses.first { it.typeName.contains("1") }) {
                val fields = fields()
                assertEquals(4, fields.size)
                interfaces().first().assertType<Runnable>()

                with(fields.first { it.name == "stateT" }) {
                    assertEquals("stateT", name)
                    with(fieldType().assertIs<JcTypeVariable>()) {
                        assertEquals("T", symbol)
                    }
                }
                with(fields.first { it.name == "stateW" }) {
                    assertEquals("stateW", name)
                    with(fieldType().assertIs<JcTypeVariable>()) {
                        assertEquals("W", symbol)
                    }
                }
            }
        }
    }

    @Test
    fun `parameterized inner type`() {
        runBlocking {
            with(innerState()) {
                fields().first { it.name == "stateW" }.fieldType().assertType<String>()
            }
        }
    }

    @Test
    fun `custom parameterization of method overrides outer class parameterization`() {
        runBlocking {
            with(innerState()) {
                with(methods().first { it.name == "method" }) {
                    with(returnType().assertIs<JcTypeVariable>()) {
                        assertEquals("W", symbol)
                        assertEquals("java.util.List<java.io.Closeable>", bounds.first().typeName)
                    }
                }
            }
        }
    }

    @Test
    fun `custom parameterization of inner type overrides outer class parameterization`() {
        runBlocking {
            with(innerState("state2")) {
                with(fields().first { it.name == "stateW" }) {
                    fieldType().assertType<Closeable>()
                }
                with(methods().first { it.name == "method" }) {
                    with(returnType().assertIs<JcTypeVariable>()) {
                        assertEquals("java.util.List<W extends java.io.Closeable>", typeName)
                    }
                }
            }
        }
    }

    private suspend fun innerState(fieldName: String = "state"): JcClassType {
        val stringInnerClasses = findClassType<InnerClasses<*>>().fields().first {
            it.name == "innerClasses"
        }.fieldType().assertClassType()
        return stringInnerClasses.fields().first { it.name == fieldName }.fieldType().assertClassType()
    }


}