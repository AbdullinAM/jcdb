package org.utbot.java.compilation.database.impl.tree

import org.utbot.java.compilation.database.api.ByteCodeLocation
import org.utbot.java.compilation.database.impl.LocationsRegistrySnapshot

/**
 * ClassTree view limited by number of `locations`
 */
class ClasspathClassTree(
    private val classTree: ClassTree,
    locations: List<ByteCodeLocation>
) {

    constructor(classTree: ClassTree, locationsRegistrySnapshot: LocationsRegistrySnapshot) : this(
        classTree,
        locationsRegistrySnapshot.locations
    )

    private val locationIds: Set<String> = locations.map { it.id }.toHashSet()


    fun firstClassOrNull(fullName: String): ClassNode? {
        return classTree.firstClassNodeOrNull(fullName) {
            locationIds.contains(it)
        }
    }
}