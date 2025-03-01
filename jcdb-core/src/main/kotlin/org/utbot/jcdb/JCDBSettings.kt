package org.utbot.jcdb

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.utbot.jcdb.api.Hook
import org.utbot.jcdb.api.JCDB
import org.utbot.jcdb.api.JcFeature
import java.io.File

/**
 * Settings for database
 */
class JCDBSettings {

    /** watch file system changes delay */
    var watchFileSystemDelay: Int? = null
        private set

    /** persisted  */
    var persistentLocation: String? = null
        private set

    var persistentClearOnStart: Boolean? = null

    /** jar files which should be loaded right after database is created */
    var predefinedDirOrJars: List<File> = persistentListOf()
        private set

    var hooks: MutableList<(JCDB) -> Hook> = arrayListOf()
        private set

    /** mandatory setting for java runtime location */
    lateinit var jre: File

    /** feature to add */
    var features: List<JcFeature<*, *>> = emptyList()
        private set

    init {
        useProcessJavaRuntime()
    }

    /**
     * builder for persistent settings
     * @param location - file for db location
     * @param clearOnStart -if true old data from this folder will be dropped
     */
    fun persistent(location: String, clearOnStart: Boolean = false) = apply {
        persistentLocation = location
        persistentClearOnStart = clearOnStart
    }

    fun loadByteCode(files: List<File>) = apply {
        predefinedDirOrJars = (predefinedDirOrJars + files).toPersistentList()
    }

    /**
     * builder for watching file system changes
     * @param delay - delay between syncs
     */
    fun watchFileSystem(delay: Int = 10_000) = apply {
        watchFileSystemDelay = delay
    }

    /** builder for hooks */
    fun withHook(hook: (JCDB) -> Hook) = apply {
        hooks += hook
    }

    /**
     * use java from JAVA_HOME env variable
     */
    fun useJavaHomeJavaRuntime() = apply {
        val javaHome = System.getenv("JAVA_HOME") ?: throw IllegalArgumentException("JAVA_HOME is not set")
        jre = javaHome.asValidJRE()
    }

    /**
     * use java from current system process
     */
    fun useProcessJavaRuntime() = apply {
        val javaHome = System.getProperty("java.home") ?: throw IllegalArgumentException("java.home is not set")
        jre = javaHome.asValidJRE()
    }

    /**
     * use java from current system process
     */
    fun useJavaRuntime(runtime: File) = apply {
        jre = runtime.absolutePath.asValidJRE()
    }

    /**
     * install additional indexes
     */
    fun installFeatures(vararg feature: JcFeature<*, *>) = apply {
        features = features + feature.toList()
    }

    private fun String.asValidJRE(): File {
        val file = File(this)
        if (!file.exists()) {
            throw IllegalArgumentException("$this points to folder that do not exists")
        }
        return file
    }
}