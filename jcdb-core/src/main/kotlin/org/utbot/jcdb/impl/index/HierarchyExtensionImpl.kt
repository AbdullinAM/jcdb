package org.utbot.jcdb.impl.index

import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.utbot.jcdb.api.JCDB
import org.utbot.jcdb.api.JcClassOrInterface
import org.utbot.jcdb.api.JcClasspath
import org.utbot.jcdb.api.JcMethod
import org.utbot.jcdb.api.ext.HierarchyExtension
import org.utbot.jcdb.api.findMethodOrNull
import org.utbot.jcdb.impl.bytecode.JcClassOrInterfaceImpl
import org.utbot.jcdb.impl.fs.ClassSourceImpl
import java.sql.ResultSet

class HierarchyExtensionImpl(private val db: JCDB, private val cp: JcClasspath) : HierarchyExtension {

    companion object {
        private fun allHierarchyQuery(locationIds: String) = """
            WITH RECURSIVE Hierarchy(class_name_id, class_id) AS (
                SELECT Classes.name, ClassHierarchies.class_id FROM ClassHierarchies
                    JOIN Symbols ON Symbols.id = ClassHierarchies.super_id
                    JOIN Classes ON Classes.id = ClassHierarchies.class_id
                    WHERE Symbols.name = ?
                UNION ALL
                SELECT Classes.name, ClassHierarchies.class_id FROM ClassHierarchies
                    JOIN Classes ON Classes.id = ClassHierarchies.class_id
                    JOIN Hierarchy ON Hierarchy.class_name_id = ClassHierarchies.super_id)
            SELECT DISTINCT Classes.id, Classes.location_id,  Symbols.name as name_name, Classes.bytecode from Hierarchy
                JOIN Classes ON Classes.id = hierarchy.class_id
                JOIN Symbols ON Symbols.id = Classes.name
             WHERE location_id in ($locationIds)
        """.trimIndent()

        private fun directSubClassesQuery(locationIds: String) = """
            SELECT Classes.id, Classes.location_id, SymbolsName.name as name_name, Classes.bytecode FROM ClassHierarchies
                JOIN Symbols ON Symbols.id = ClassHierarchies.super_id
                JOIN Symbols as SymbolsName ON SymbolsName.id = Classes.name
                JOIN Classes ON Classes.id = ClassHierarchies.class_id
            WHERE Symbols.name = ? AND Classes.location_id in ($locationIds)
        """.trimIndent()

        fun <T : Any> String.query(args: Iterable<Pair<IColumnType, Any?>>, transform: (ResultSet) -> T): List<T> {
            val result = arrayListOf<T>()
            TransactionManager.current().exec(this, args, StatementType.SELECT) { rs ->
                while (rs.next()) {
                    result += transform(rs)
                }
            }
            return result
        }
    }

    override suspend fun findSubClasses(name: String, allHierarchy: Boolean): List<JcClassOrInterface> {
        val classId = cp.findClassOrNull(name) ?: return emptyList()
        return findSubClasses(classId, allHierarchy)
    }

    override suspend fun findSubClasses(classId: JcClassOrInterface, allHierarchy: Boolean): List<JcClassOrInterface> {
        db.awaitBackgroundJobs()
        val name = classId.name

        return cp.subClasses(name, allHierarchy).map { record ->
            JcClassOrInterfaceImpl(
                cp, ClassSourceImpl(
                    location = cp.registeredLocations.first { it.id == record.locationId },
                    className = record.name,
                    byteCode = record.byteCode
                )
            )
        }
    }

    override suspend fun findOverrides(methodId: JcMethod): List<JcMethod> {
        val desc = methodId.description
        val name = methodId.name
        val subClasses = cp.findSubClasses(methodId.jcClass, allHierarchy = true)
        return subClasses.mapNotNull {
            it.findMethodOrNull(name, desc) // todo this is wrong
        }
    }

    private fun JcClasspath.subClasses(name: String, allHierarchy: Boolean): List<ClassRecord> {
        val locationIds = registeredLocations.joinToString(", ") { it.id.toString() }
        return db.persistence.read {
            val query = if (allHierarchy) allHierarchyQuery(locationIds) else directSubClassesQuery(locationIds)
            query.query(listOf(VarCharColumnType() to name)) {
                ClassRecord(
                    id = it.getLong("id"),
                    byteCode = it.getBytes("bytecode"),
                    locationId = it.getLong("location_id"),
                    name = it.getString("name_name")
                )
            }
        }
    }
}

private data class ClassRecord(
    val id: Long,
    val byteCode: ByteArray,
    val locationId: Long,
    val name: String
)

val JcClasspath.hierarchyExt: HierarchyExtensionImpl
    get() {
        return HierarchyExtensionImpl(db, this)
    }