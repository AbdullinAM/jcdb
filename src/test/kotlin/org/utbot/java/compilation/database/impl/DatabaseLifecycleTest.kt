package org.utbot.java.compilation.database.impl

import com.google.common.cache.AbstractCache
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.utbot.java.compilation.database.api.ClasspathSet
import org.utbot.java.compilation.database.compilationDatabase
import org.utbot.java.compilation.database.impl.fs.BuildFolderLocationImpl
import java.io.File
import java.nio.file.Files
import java.util.*

class DatabaseLifecycleTest : LibrariesMixin {

    private val db = runBlocking {
        compilationDatabase {
            useProcessJRE()
        } as CompilationDatabaseImpl
    }
    private val tempFolder = Files.createTempDirectory("classpath-copy-" + UUID.randomUUID()).toFile()

    private val testDirClone: File get() = File(tempFolder, "test")
    private val guavaLibClone: File get() = File(tempFolder, guavaLib.name)


    @BeforeEach
    fun cloneClasspath() {
        allClasspath.forEach {
            if (it.isFile) {
                it.copyTo(File(tempFolder, it.name))
            } else if (it.absolutePath.replace(File.separator, "/").contains("build/classes/kotlin/test")) {
                // copy only test kotlin resources
                it.copyRecursively(File(tempFolder, it.name), true)
            }
        }
    }

    @Test
    fun `refresh is working when build dir is removed`() = runBlocking {
        val cp = db.classpathSet(listOf(testDirClone))
        val fooClass = cp.findClassOrNull(Foo::class.java.name)
        assertNotNull(fooClass!!)

        assertTrue(testDirClone.deleteRecursively())
        assertThrows<IllegalStateException> {
            runBlocking {
                fooClass.methods().first().readBody()
            }
        }

        db.refresh()

        assertEquals(1, db.registry.snapshots.size)
        assertEquals(1, db.registry.usedButOutdated.size)
        assertEquals(1, db.registry.locations.filterIsInstance<BuildFolderLocationImpl>().size)

        assertNotNull(cp.findClassOrNull(Foo::class.java.name))
        cp.close()
        db.refresh()
        assertTrue(db.registry.snapshots.isEmpty())
        assertTrue(db.registry.usedButOutdated.isEmpty())
        assertTrue(db.registry.locations.all { it !is BuildFolderLocationImpl })
    }

    @Test
    fun `method could be read from build dir`() = runBlocking {
        val cp = db.classpathSet(listOf(testDirClone))
        val fooClass = cp.findClassOrNull(Foo::class.java.name)
        assertNotNull(fooClass!!)

        assertNotNull {
            runBlocking {
                fooClass.methods().first().readBody()
            }
        }
    }

    @Test
    fun `refresh is working when jar is removed`() = runBlocking {
        val cp = db.classpathSet(listOf(guavaLibClone))
        val abstractCacheClass = cp.findClassOrNull(AbstractCache::class.java.name)
        assertNotNull(abstractCacheClass!!)
        db.awaitBackgroundJobs() // is required for deleting jar

        assertTrue(guavaLibClone.delete())
        assertThrows<IllegalStateException> {
            runBlocking {
                abstractCacheClass.methods().first().readBody()
            }
        }

        db.refresh()
        assertEquals(1, db.registry.snapshots.size)
        assertEquals(1, db.registry.usedButOutdated.size)

        assertNotNull(cp.findClassOrNull(AbstractCache::class.java.name))
        cp.close()
        db.refresh()
        assertTrue(db.registry.snapshots.isEmpty())
        assertTrue(db.registry.usedButOutdated.isEmpty())
        assertEquals(db.javaRuntime.allLocations.size, db.registry.locations.size)
    }

    @Test
    fun `method body could be read from jar`() = runBlocking {
        val cp = db.classpathSet(listOf(guavaLibClone))
        val abstractCacheClass = cp.findClassOrNull(AbstractCache::class.java.name)
        assertNotNull(abstractCacheClass!!)

        assertNotNull {
            runBlocking {
                abstractCacheClass.methods().first().readBody()
            }
        }
    }

    @Test
    fun `simultaneous access to method body`() = runBlocking {
        val cps = (1..10).map { db.classpathSet(listOf(guavaLibClone)) }

        suspend fun ClasspathSet.accessMethod() {
            val abstractCacheClass = findClassOrNull(AbstractCache::class.java.name)
            assertNotNull(abstractCacheClass!!)

            assertNotNull(
                abstractCacheClass.methods().first().readBody()
            )
        }

        BackgroundScope.launch {
            cps.map {
                async {
                    it.accessMethod()
                    it.close()
                }
            }.joinAll()
        }.join()
        db.refresh()

        assertTrue(db.registry.snapshots.isEmpty())
        assertTrue(db.registry.usedButOutdated.isEmpty())
    }

    @AfterEach
    fun cleanup() {
        tempFolder.deleteRecursively()
    }
}