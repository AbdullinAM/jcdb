package org.utbot.java.compilation.database.impl.tree

interface ClassTreeListener {

    /**
     * method called when metaInfo is loaded for node
     * @param classNodeWithLoadedMeta - class node with fully loaded node
     */
    suspend fun notifyOnMetaLoaded(classNodeWithLoadedMeta: ClassNode, classTree: ClassTree)

}