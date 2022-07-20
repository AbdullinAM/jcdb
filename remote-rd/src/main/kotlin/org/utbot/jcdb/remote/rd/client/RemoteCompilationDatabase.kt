package org.utbot.jcdb.remote.rd.client

import com.jetbrains.rd.framework.IdKind
import com.jetbrains.rd.framework.Identities
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.SocketWire
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import org.utbot.jcdb.api.ByteCodeLocation
import org.utbot.jcdb.api.ClasspathSet
import org.utbot.jcdb.api.CompilationDatabase
import org.utbot.jcdb.remote.rd.*
import java.io.File

class RemoteCompilationDatabase(port: Int) : CompilationDatabase {

    private val lifetimeDef = Lifetime.Eternal.createNested()
    private val scheduler = SingleThreadScheduler(lifetimeDef.lifetime, "rd-scheduler")

    private val clientProtocol = Protocol(
        "rd-client-$port",
        serializers,
        Identities(IdKind.Client),
        scheduler,
        SocketWire.Client(lifetimeDef.lifetime, scheduler, port),
        lifetimeDef.lifetime
    )

    private val getClasspath = GetClasspathResource().clientCall(clientProtocol)
    private val getClass = GetClassResource().clientCall(clientProtocol)
    private val closeClasspath = CloseClasspathResource().clientCall(clientProtocol)
    private val getSubClasses = GetSubClassesResource().clientCall(clientProtocol)

    private val getId = GetGlobalId().clientCall(clientProtocol)
    private val getName = GetGlobalName().clientCall(clientProtocol)

    init {
        scheduler.flush()
    }

    override val globalIdStore = RemoteGlobalIds(getName, getId)

    override suspend fun classpathSet(dirOrJars: List<File>): ClasspathSet {
        val id = getClasspath.startSuspending(GetClasspathReq(dirOrJars.map { it.absolutePath }.sorted()))
        return RemoteClasspathSet(
            id,
            this,
            close = closeClasspath,
            getClass = getClass,
            getSubClasses = getSubClasses
        )
    }

    override suspend fun load(dirOrJar: File): CompilationDatabase {
        TODO("Not yet implemented")
    }

    override suspend fun load(dirOrJars: List<File>): CompilationDatabase {
        TODO("Not yet implemented")
    }

    override suspend fun loadLocations(locations: List<ByteCodeLocation>): CompilationDatabase {
        TODO("Not yet implemented")
    }

    override suspend fun refresh() {
    }

    override fun watchFileSystemChanges() = this

    override suspend fun awaitBackgroundJobs() {
    }

    override fun close() {
        lifetimeDef.terminate()
    }
}


