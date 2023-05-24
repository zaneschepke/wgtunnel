package com.zaneschepke.wireguardautotunnel.repository

import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.awaitCallInTx
import io.objectbox.kotlin.toFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject


class SettingsBox @Inject constructor(private val box : Box<Settings>, private val boxStore : BoxStore) : Repository<Settings> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val itemFlow = box.query().build().subscribe().toFlow()

    override fun init() {
        CoroutineScope(Dispatchers.IO).launch {
            if(getAll().isNullOrEmpty()) {
                save(Settings())
            }
        }
    }

    override suspend fun save(t : Settings) {
        boxStore.awaitCallInTx {
            box.put(t)
        }
    }

    override suspend fun saveAll(t : List<Settings>) {
        boxStore.awaitCallInTx {
            box.put(t)
        }
    }

    override suspend fun getById(id: Long): Settings? {
        return boxStore.awaitCallInTx {
            box[id]
        }
    }

    override suspend fun getAll(): List<Settings>? {
        return boxStore.awaitCallInTx {
            box.all
        }
    }

    override suspend fun delete(t : Settings): Boolean? {
        return boxStore.awaitCallInTx {
            box.remove(t)
        }
    }

    override suspend fun count() : Long? {
        return boxStore.awaitCallInTx {
            box.count()
        }
    }
}