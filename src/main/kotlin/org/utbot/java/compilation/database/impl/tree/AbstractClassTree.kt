package org.utbot.java.compilation.database.impl.tree

import org.utbot.java.compilation.database.impl.fs.ClassByteCodeSource

abstract class AbstractClassTree<PACKAGE_NODE : AbstractNode<PACKAGE_NODE>, CLASS_NODE: AbstractNode<PACKAGE_NODE>> {

    protected abstract val rootNode: PACKAGE_NODE

    fun addClass(source: ClassByteCodeSource): CLASS_NODE {
        val splitted = source.className.splitted
        val simpleClassName = splitted[splitted.size - 1]
        val version = source.location.id
        if (splitted.size == 1) {
            return rootNode.findClassOrNew(simpleClassName, version, source)
        }
        var node = rootNode
        var index = 0
        while (index < splitted.size - 1) {
            val subfolderName = splitted[index]
            node = node.findPackageOrNew(subfolderName)
            index++
        }
        return node.findClassOrNew(simpleClassName, version, source)
    }

    abstract fun PACKAGE_NODE.findClassOrNew(simpleClassName: String, version: String, source: ClassByteCodeSource): CLASS_NODE
    abstract fun PACKAGE_NODE.findPackageOrNew(subfolderName: String): PACKAGE_NODE

    protected val String.splitted get() = this.split(".")
}