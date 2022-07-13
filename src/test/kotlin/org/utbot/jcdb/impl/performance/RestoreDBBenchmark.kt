package org.utbot.jcdb.impl.performance

import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import org.utbot.jcdb.api.CompilationDatabase
import org.utbot.jcdb.compilationDatabase
import org.utbot.jcdb.impl.LibrariesMixin
import java.nio.file.Files
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Fork(0)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.MILLISECONDS)
class RestoreDBBenchmark : LibrariesMixin {

    companion object {
        private val jdbcLocation = Files.createTempDirectory("jdbc").toFile().absolutePath
    }

    var db: CompilationDatabase? = null

    @Setup
    fun setup() {
        val tempDb = newDB()
        tempDb.close()
    }

    @Benchmark
    fun restore() {
        db = newDB()
    }

    @TearDown(Level.Iteration)
    fun clean() {
        db?.close()
        db = null
    }

    private fun newDB(): CompilationDatabase {
        return runBlocking {
            compilationDatabase {
                persistent {
                    location = jdbcLocation
                }
                predefinedDirOrJars = allClasspath
                useProcessJavaRuntime()
            }.also {
                it.awaitBackgroundJobs()
            }
        }
    }

}

fun main() {
    val test = RestoreDBBenchmark()
    test.setup()
    repeat(3) {
        println("iteration $it")
        val start = System.currentTimeMillis()
        test.restore()
        println("took ${System.currentTimeMillis() - start}ms")
        test.clean()
    }
}