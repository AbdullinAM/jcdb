package org.utbot.jcdb.impl

import kotlinx.coroutines.*
import mu.KLogging
import org.utbot.jcdb.CompilationDatabaseSettings
import org.utbot.jcdb.api.ByteCodeLocation
import org.utbot.jcdb.api.ClasspathSet
import org.utbot.jcdb.api.CompilationDatabase
import org.utbot.jcdb.impl.fs.JavaRuntime
import org.utbot.jcdb.impl.fs.asByteCodeLocation
import org.utbot.jcdb.impl.fs.filterExisted
import org.utbot.jcdb.impl.fs.load
import org.utbot.jcdb.impl.index.Hierarchy
import org.utbot.jcdb.impl.tree.ClassTree
import org.utbot.jcdb.impl.tree.RemoveLocationsVisitor
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object BackgroundScope : CoroutineScope {
    override val coroutineContext = Dispatchers.IO + SupervisorJob()
}

class CompilationDatabaseImpl(private val settings: CompilationDatabaseSettings) : CompilationDatabase {
    companion object : KLogging()

    private val classTree = ClassTree()
    internal val javaRuntime = JavaRuntime(settings.jre)

    private val indexesRegistry = IndexesRegistry(listOf(Hierarchy) + settings.additionalIndexes)
    internal val registry = LocationsRegistry(indexesRegistry)

    private val backgroundJobs = ConcurrentHashMap<Int, Job>()

    private val isClosed = AtomicBoolean()
    private val jobId = AtomicInteger()

    suspend fun loadJavaLibraries() {
        assertNotClosed()
        javaRuntime.allLocations.loadAll()
    }

    override suspend fun classpathSet(dirOrJars: List<File>): ClasspathSet {
        assertNotClosed()
        val existedLocations = dirOrJars.filterExisted().map { it.asByteCodeLocation() }.also {
            it.loadAll()
        }
        val classpathSetLocations = existedLocations.toList() + javaRuntime.allLocations
        return ClasspathSetImpl(
            registry.snapshot(classpathSetLocations),
            indexesRegistry,
            this,
            classTree
        )
    }

    fun classpathSet(locations: List<ByteCodeLocation>): ClasspathSet {
        assertNotClosed()
        val classpathSetLocations = locations.toSet() + javaRuntime.allLocations
        return ClasspathSetImpl(
            registry.snapshot(classpathSetLocations.toList()),
            indexesRegistry,
            this,
            classTree
        )
    }

    override suspend fun load(dirOrJar: File) = apply {
        assertNotClosed()
        load(listOf(dirOrJar))
    }

    override suspend fun load(dirOrJars: List<File>) = apply {
        assertNotClosed()
        dirOrJars.filterExisted().map { it.asByteCodeLocation() }.loadAll()
    }

    private suspend fun List<ByteCodeLocation>.loadAll() = apply {
        val actions = ConcurrentLinkedQueue<Pair<ByteCodeLocation, suspend () -> Unit>>()

        val libraryTrees = withContext(Dispatchers.IO) {
            map { location ->
                async {
                    val loader = location.loader()
                    // here something may go wrong
                    if (loader != null) {
                        val (libraryTree, asyncJob) = loader.load(classTree)
                        actions.add(location to asyncJob)
                        registry.addLocation(location)
                        libraryTree
                    } else {
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()
        val locationClasses = libraryTrees.map {
            it.location to it.pushInto(classTree).values
        }.toMap()
        val backgroundJobId = jobId.incrementAndGet()
        backgroundJobs[backgroundJobId] = BackgroundScope.launch {
            val parentScope = this
            actions.map { (location, action) ->
                async {
                    if (parentScope.isActive) {
                        action()
                    }
                    val addedClasses = locationClasses[location]
                    if (addedClasses != null) {
                        if (parentScope.isActive) {
                            indexesRegistry.index(location, addedClasses)
                        }
                    }
                }
            }.joinAll()
            backgroundJobs.remove(backgroundJobId)
        }
    }

    override suspend fun refresh() {
        awaitBackgroundJobs()
        registry.refresh {
            listOf(it).loadAll()
        }
        val outdatedLocations = registry.cleanup()
        classTree.visit(RemoveLocationsVisitor(outdatedLocations))
    }

    override fun watchFileSystemChanges(): CompilationDatabase {
        val delay = settings.watchFileSystemChanges?.delay
        if (delay != null) { // just paranoid check
            BackgroundScope.launch {
                while (true) {
                    delay(delay)
                    refresh()
                }
            }
        }
        return this
    }

    override suspend fun awaitBackgroundJobs() {
        backgroundJobs.values.toList().joinAll()
    }

    override fun close() {
        isClosed.set(true)
        registry.close()
        backgroundJobs.values.forEach {
            it.cancel()
        }
        backgroundJobs.clear()
    }

    private fun assertNotClosed() {
        if (isClosed.get()) {
            throw IllegalStateException("Database is already closed")
        }
    }
}