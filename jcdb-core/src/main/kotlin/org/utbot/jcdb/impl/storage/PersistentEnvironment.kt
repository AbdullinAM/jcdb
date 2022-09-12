package org.utbot.jcdb.impl.storage

import mu.KLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import org.utbot.jcdb.api.ByteCodeLocation
import org.utbot.jcdb.api.LocationScope
import org.utbot.jcdb.impl.JCDBImpl
import org.utbot.jcdb.impl.fs.asByteCodeLocation
import org.utbot.jcdb.impl.index.Calls
import java.io.Closeable
import java.io.File

class PersistentEnvironment(location: File? = null, clearOnStart: Boolean) : Closeable {

    companion object : KLogging()

    private val dataSource = SQLiteDataSource(SQLiteConfig().also {
        it.setSynchronous(SQLiteConfig.SynchronousMode.OFF)
        it.setPageSize(32_768)
        it.setCacheSize(-8_000)
    }).also {
        it.url = "jdbc:sqlite:$location"
    }

    init {
        Database.connect(dataSource)
        transaction {
            if (clearOnStart) {
                SchemaUtils.drop(
                    Classpaths, ClasspathLocations, BytecodeLocations,
                    Classes, Symbols, ClassInterfaces, ClassInnerClasses, OuterClasses,
                    Methods, MethodParameters,
                    Fields,
                    Calls
                )
            }
            SchemaUtils.create(
                Classpaths, ClasspathLocations,
                BytecodeLocations,
                Classes, Symbols, ClassInterfaces, ClassInnerClasses, OuterClasses,
                Methods, MethodParameters,
                Fields,
                Calls
            )
        }
    }

    val locationStore = LocationStore(this)

    val allByteCodeLocations: List<Pair<BytecodeLocationEntity, ByteCodeLocation>>
        get() {
            return transaction {
                locationStore.all.mapNotNull {
                    try {
                        it to File(it.path).asByteCodeLocation(isRuntime = it.runtime)
                    } catch (e: Exception) {
                        null
                    }
                }.toList()
            }
        }

    fun save(db: JCDBImpl) {
        transaction {
            db.locationsRegistry.locations.forEach { location ->
                BytecodeLocationEntity.new {
                    path = location.path
                    runtime = location.scope == LocationScope.RUNTIME
                }
            }
        }
    }

    override fun close() {
    }
}

