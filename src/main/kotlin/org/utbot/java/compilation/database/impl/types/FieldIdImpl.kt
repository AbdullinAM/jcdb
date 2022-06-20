package org.utbot.java.compilation.database.impl.types

import org.utbot.java.compilation.database.api.ClassId
import org.utbot.java.compilation.database.api.FieldId
import org.utbot.java.compilation.database.impl.ClassIdService

class FieldIdImpl(
    override val classId: ClassId,
    private val info: FieldInfo,
    private val classIdService: ClassIdService
) : FieldId {

    override val name: String
        get() = info.name

    private val lazyType by lazy(LazyThreadSafetyMode.NONE) {
        classIdService.toClassId(info.type)
    }

    private val lazyAnnotations by lazy(LazyThreadSafetyMode.NONE) {
        info.annotations.mapNotNull { classIdService.toClassId(it.className) }

    }

    override suspend fun access() = info.access
    override suspend fun type() = lazyType

    override suspend fun annotations() = lazyAnnotations

}