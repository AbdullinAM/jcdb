package org.utbot.java.compilation.database.api

import org.objectweb.asm.Opcodes
import java.security.MessageDigest

suspend fun Accessible.isPublic(): Boolean {
    return access() and Opcodes.ACC_PUBLIC != 0
}

suspend fun Accessible.isPrivate(): Boolean {
    return access() and Opcodes.ACC_PRIVATE != 0
}

suspend fun Accessible.isProtected(): Boolean {
    return access() and Opcodes.ACC_PROTECTED != 0
}

suspend fun Accessible.isStatic(): Boolean {
    return access() and Opcodes.ACC_STATIC != 0
}

suspend fun Accessible.isFinal(): Boolean {
    return access() and Opcodes.ACC_FINAL != 0
}

suspend fun Accessible.isSynchronized(): Boolean {
    return access() and Opcodes.ACC_SYNCHRONIZED != 0
}

suspend fun Accessible.isVolatile(): Boolean {
    return access() and Opcodes.ACC_VOLATILE != 0
}

suspend fun Accessible.isTransient(): Boolean {
    return access() and Opcodes.ACC_TRANSIENT != 0
}

suspend fun Accessible.isNative(): Boolean {
    return access() and Opcodes.ACC_NATIVE != 0
}

suspend fun Accessible.isInterface(): Boolean {
    return access() and Opcodes.ACC_INTERFACE != 0
}

suspend fun Accessible.isAbstract(): Boolean {
    return access() and Opcodes.ACC_ABSTRACT != 0
}

suspend fun Accessible.isStrict(): Boolean {
    return access() and Opcodes.ACC_STRICT != 0
}


fun String.md5(): String {
    return MessageDigest
        .getInstance("MD5")
        .digest(toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }
}

