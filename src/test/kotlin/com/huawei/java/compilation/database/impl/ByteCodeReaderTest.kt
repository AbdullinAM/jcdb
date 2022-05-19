package com.huawei.java.compilation.database.impl

import com.huawei.java.compilation.database.ApiLevel
import com.huawei.java.compilation.database.impl.fs.asByteCodeLocation
import com.huawei.java.compilation.database.impl.fs.sources
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader


class ByteCodeReaderTest {

    @Test
    fun `read byte-code benchmark`() {
        val lib = guavaLib
        benchmark(20, "read bytecode") {
            runBlocking {
                lib.asByteCodeLocation(ApiLevel.ASM8).sources().toList()
            }
        }
    }


    @Test
    fun `read all classpath byte-code benchmark`() {
        val jars = allJars
        benchmark(10, "reading libraries bytecode") {
            runBlocking {
                CompilationDatabaseImpl(ApiLevel.ASM8).load(jars)
            }
        }
    }

    private fun benchmark(repeats: Int = 10, name: String, action: () -> Unit) {
        // warmup
        repeat(repeats / 2) {
            action()
        }

        // let's count
        repeat(repeats) {
            val start = System.currentTimeMillis()
            action()
            val end = System.currentTimeMillis()
            println("$it: $name took: ${end - start}ms")
        }

    }

    private val guavaLib: File
        get() {
            val cl = ClassLoader.getSystemClassLoader()
            val guavaUrl = (cl as URLClassLoader).urLs.first { it.path.contains("guava-31.1-jre.jar") }
            return File(guavaUrl.file).also {
                assertTrue(it.isFile && it.exists())
            }
        }

    private val allJars: List<File>
        get() {
            val cl = ClassLoader.getSystemClassLoader()
            val jars = (cl as URLClassLoader).urLs.filter { it.path.endsWith(".jar") }
//            val jdks = File(System.getenv("JAVA_HOME") + "\\jre\\lib\\").listFiles { file -> file.name.endsWith(".jar") }
//                .orEmpty().toList()
            return jars.map { File(it.file) } //+ jdks
        }
}