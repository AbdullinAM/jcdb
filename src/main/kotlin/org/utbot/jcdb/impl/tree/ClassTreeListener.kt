package org.utbot.jcdb.impl.tree

interface ClassTreeListener {

    /**
     * method called when bytecode is read from file
     * @param nodeWithLoadedByteCode - class node with fully loaded node
     */
    suspend fun notifyOnByteCodeLoaded(nodeWithLoadedByteCode: ClassNode, classTree: ClassTree)

}