package org.utbot.jcdb.impl.storage.scheme

import jetbrains.exodus.bindings.IntegerBinding
import jetbrains.exodus.bindings.StringBinding
import jetbrains.exodus.env.Environment
import jetbrains.exodus.env.StoreConfig
import org.utbot.jcdb.impl.index.InMemeoryGlobalIdsStore
import java.util.concurrent.atomic.AtomicInteger


class GlobalIdStore(private val env: Environment) {

    private var counter = AtomicInteger()

    private val store = env.computeInTransaction { txn ->
        env.openStore(
            "GlobalIds",
            StoreConfig.WITHOUT_DUPLICATES,
            txn
        )
    }

    fun sync(globalIds: InMemeoryGlobalIdsStore) = synchronized(this) {
        val current = counter.get()
        val inMemoryCounter = globalIds.count
        if (inMemoryCounter != current) {
            val keys = globalIds.all(current, inMemoryCounter).toList()
            keys.windowed(10_000, step = 10_000, partialWindows = true) {
                env.executeInTransaction { txn ->
                    it.forEach {
                        val (k, v) = it
                        val key = IntegerBinding.intToEntry(k)
                        val value = StringBinding.stringToEntry(v)
                        store.put(txn, key, value)
                    }
                }
            }
            counter.set(globalIds.count)
        }
    }

    fun restore(globalIds: InMemeoryGlobalIdsStore) {
        globalIds.restore {
            env.executeInTransaction { txn ->
                store.openCursor(txn).use { cursor ->
                    while (cursor.next) {
                        globalIds.append(
                            IntegerBinding.entryToInt(cursor.key),
                            StringBinding.entryToString(cursor.value)
                        )
                    }
                }
            }
            counter.set(globalIds.count)
        }
    }
}