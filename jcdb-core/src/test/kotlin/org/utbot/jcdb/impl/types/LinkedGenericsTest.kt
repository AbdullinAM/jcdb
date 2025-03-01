package org.utbot.jcdb.impl.types

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.utbot.jcdb.api.JcClassType
import org.utbot.jcdb.api.JcTypeVariable
import org.utbot.jcdb.api.ext.findClass
import org.utbot.jcdb.api.isConstructor
import org.utbot.jcdb.impl.types.Generics.LinkedImpl
import org.utbot.jcdb.impl.types.Generics.SingleImpl

class LinkedGenericsTest : BaseTypesTest() {

    @Test
    fun `linked generics original parametrization`() = runBlocking {
        val partial = findClassType<LinkedImpl<*>>()
        with(partial.superType!!) {
            with(typeParameters.first()) {
                assertEquals("T", symbol)
                bounds.first().assertClassType<Any>()
            }

            with(typeParameters[1]) {
                assertEquals("W", symbol)
                assertEquals(1, bounds.size)
                assertEquals("java.util.List<T>", bounds[0].typeName)
            }
        }
    }

    @Test
    fun `linked generics current parametrization`() = runBlocking {
        val partial = findClassType<LinkedImpl<*>>()
        with(partial.superType!!) {
            with(typeArguments[0]) {
                assertClassType<String>()
            }

            with(typeArguments[1]) {
                this as JcTypeVariable
                assertEquals("W", symbol)
                assertEquals(1, bounds.size)
                assertEquals("java.util.List<java.lang.String>", bounds[0].typeName)
            }
        }
    }

    @Test
    fun `linked generics fields parametrization`() = runBlocking {
        val partial = findClassType<LinkedImpl<*>>()
        with(partial.superType!!) {
            val fields = fields
            assertEquals(3, fields.size)

            with(fields.first { it.name == "state" }) {
                assertEquals("state", name)
                fieldType.assertClassType<String>()
            }
            with(fields.first { it.name == "stateW" }) {
                assertEquals(
                    "java.util.List<java.lang.String>",
                    (fieldType as JcTypeVariable).bounds.first().typeName
                )
            }
            with(fields.first { it.name == "stateListW" }) {
                val resolvedType = fieldType.assertIsClass()
                assertEquals(cp.findClass<List<*>>(), resolvedType.jcClass)
                val shouldBeW = (resolvedType.typeArguments.first() as JcTypeVariable)
                assertEquals("java.util.List<java.lang.String>", shouldBeW.bounds.first().typeName)
            }
        }
    }


    @Test
    fun `generics applied for fields of super types`() {
        runBlocking {
            val superFooType = findClassType<SingleImpl>()
            with(superFooType.superType.assertIsClass()) {
                val fields = fields
                assertEquals(2, fields.size)

                with(fields.first()) {
                    assertEquals("state", name)
                    fieldType.assertClassType<String>()
                }
                with(fields.get(1)) {
                    assertEquals("stateList", name)
                    with(fieldType.assertIsClass()) {
                        assertEquals("java.util.ArrayList<java.lang.String>", typeName)
                    }
                }
            }
        }
    }

    @Test
    fun `direct generics from child types applied to methods`() {
        runBlocking {
            val superFooType = findClassType<SingleImpl>()
            val superType = superFooType.superType.assertIsClass()
            val methods = superType.declaredMethods.filterNot { it.method.isConstructor }
            assertEquals(2, methods.size)

            with(methods.first { it.method.name == "run1" }) {
                returnType.assertClassType<String>()
                parameters.first().type.assertClassType<String>()
            }
        }
    }

    @Test
    fun `custom generics from child types applied to methods`() {
        runBlocking {
            val superFooType = findClassType<SingleImpl>()
            val superType = superFooType.superType.assertIsClass()
            val methods = superType.declaredMethods.filterNot { it.method.isConstructor }
            assertEquals(2, methods.size)

            with(methods.first { it.method.name == "run2" }) {
                val params = parameters.first()
                val w = typeParameters.first()

                val bound = (params.type as JcClassType).typeArguments.first()
                assertEquals("W", (bound as? JcTypeVariable)?.symbol)
                assertEquals("W", w.symbol)
                bound as JcTypeVariable
                bound.bounds.first().assertClassType<String>()
            }
        }
    }

}