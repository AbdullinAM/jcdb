package org.utbot.java.compilation.database.impl.tree

import org.utbot.java.compilation.database.api.ByteCodeLocation
import org.utbot.java.compilation.database.impl.fs.ByteCodeLoaderImpl

open class DummyCodeLocation(override val id: String) : ByteCodeLocation {
    override fun isChanged() = false

    override fun createRefreshed() = this

    override suspend fun resolve(classFullName: String) = null

    override suspend fun loader() = ByteCodeLoaderImpl(this, emptyMap()) { emptyMap() }
}

