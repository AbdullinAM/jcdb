package org.utbot.jcdb.impl.vfs

import org.utbot.jcdb.api.RegisteredLocation
import org.utbot.jcdb.impl.fs.ClassByteCodeSource
import java.util.concurrent.ConcurrentHashMap

open class GlobalClassesVfs : AbstractClassVfs<PackageVfsItem, ClassVfsItem>() {

    public override val rootItem = PackageVfsItem(null, null)

    override fun PackageVfsItem.findClassOrNew(
        simpleClassName: String,
        source: ClassByteCodeSource
    ): ClassVfsItem {
        val nameIndex = classes.getOrPut(simpleClassName) {
            ConcurrentHashMap<Long, ClassVfsItem>()
        }
        return nameIndex.getOrPut(source.locationId) {
            ClassVfsItem(simpleClassName, this, source)
        }
    }

    override fun PackageVfsItem.findPackageOrNew(subfolderName: String): PackageVfsItem {
        return subpackages.getOrPut(subfolderName) {
            PackageVfsItem(subfolderName, this)
        }
    }

    fun findClassNodeOrNull(codeLocation: RegisteredLocation, fullName: String): ClassVfsItem? {
        val splitted = fullName.splitted
        val simpleClassName = splitted[splitted.size - 1]
        return findPackage(splitted)?.firstClassOrNull(simpleClassName, codeLocation.id)
    }

    fun firstClassNodeOrNull(fullName: String, predicate: (Long) -> Boolean = { true }): ClassVfsItem? {
        val splitted = fullName.splitted
        val simpleClassName = splitted[splitted.size - 1]
        return findPackage(splitted)?.firstClassOrNull(simpleClassName, predicate)
    }

    fun filterClassNodes(fullName: String, predicate: (ClassVfsItem) -> Boolean = { true }): List<ClassVfsItem> {
        val splitted = fullName.splitted
        val simpleClassName = splitted[splitted.size - 1]
        return findPackage(splitted)?.filterClassNodes(simpleClassName, predicate).orEmpty()
    }

    private fun findPackage(splitted: List<String>): PackageVfsItem? {
        var node: PackageVfsItem? = rootItem
        var index = 0
        while (index < (splitted.size - 1)) {
            if (node == null) {
                return null
            }
            val subfolderName = splitted[index]
            node = node.findPackageOrNull(subfolderName)
            index++
        }
        return node
    }

    fun visit(visitor: VfsVisitor) {
        rootItem.visit(visitor)
    }

}
