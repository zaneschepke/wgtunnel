package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.TunnelConfigs
import kotlinx.coroutines.flow.Flow

interface TunnelConfigRepository {

    fun getTunnelConfigsFlow() : Flow<TunnelConfigs>
    suspend fun getAll() : TunnelConfigs
    suspend fun save(tunnelConfig: TunnelConfig)
    suspend fun delete(tunnelConfig: TunnelConfig)
    suspend fun count() : Int
}