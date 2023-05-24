package com.zaneschepke.wireguardautotunnel.repository

import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.awaitCallInTx
import io.objectbox.kotlin.toFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import javax.inject.Inject

class TunnelBox @Inject constructor(private val box : Box<TunnelConfig>,private val boxStore : BoxStore) : Repository<TunnelConfig> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val itemFlow = box.query().build().subscribe().toFlow()
    override fun init() {

    }

    override suspend fun save(t : TunnelConfig) {
        Timber.d("Saving tunnel config")
        boxStore.awaitCallInTx {
            box.put(t)
        }

    }

    override suspend fun saveAll(t : List<TunnelConfig>) {
        boxStore.awaitCallInTx {
            box.put(t)
        }
    }

    override suspend fun getById(id: Long): TunnelConfig? {
       return boxStore.awaitCallInTx {
            box[id]
        }
    }

    override suspend fun getAll(): List<TunnelConfig>? {
        return boxStore.awaitCallInTx {
            box.all
        }
    }

    override suspend fun delete(t : TunnelConfig): Boolean? {
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