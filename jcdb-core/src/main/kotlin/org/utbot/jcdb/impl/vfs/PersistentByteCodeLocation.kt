package org.utbot.jcdb.impl.vfs

import org.utbot.jcdb.api.JcByteCodeLocation
import org.utbot.jcdb.api.LocationType
import org.utbot.jcdb.api.RegisteredLocation
import org.utbot.jcdb.impl.fs.asByteCodeLocation
import org.utbot.jcdb.impl.storage.BytecodeLocationEntity
import java.io.File

class PersistentByteCodeLocation(
    override val id: Long,
    override val jcLocation: JcByteCodeLocation
) : RegisteredLocation {

    constructor(entity: BytecodeLocationEntity) : this(entity.id.value, entity.toJcLocation())

    val entity get() = BytecodeLocationEntity.findById(id) ?: throw IllegalStateException("Can't find location by id $id")
}


class RestoredJcByteCodeLocation(
    override val path: String,
    override val type: LocationType,
    override val hash: String
) : JcByteCodeLocation {

    override val jarOrFolder: File
        get() = File(path)

    override fun isChanged(): Boolean {
        val actual = createRefreshed() ?: return true
        return actual.hash != hash
    }

    override fun createRefreshed(): JcByteCodeLocation? {
        if (!jarOrFolder.exists()) {
            return null
        }
        return jarOrFolder.asByteCodeLocation(type == LocationType.RUNTIME)
    }

    override fun resolve(classFullName: String) = null

    override val classNames: Set<String>
        get() = emptySet()

    override val classes: Map<String, ByteArray>?
        get() = null
}


fun BytecodeLocationEntity.toJcLocation() = RestoredJcByteCodeLocation(
    path,
    LocationType.RUNTIME.takeIf { runtime } ?: LocationType.APP,
    hash)