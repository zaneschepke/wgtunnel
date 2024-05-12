package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.TunnelConfigs
import kotlinx.coroutines.flow.Flow

class RoomTunnelConfigRepository(private val tunnelConfigDao: TunnelConfigDao) :
    TunnelConfigRepository {
    override fun getTunnelConfigsFlow(): Flow<TunnelConfigs> {
        return tunnelConfigDao.getAllFlow()
    }

    override suspend fun getAll(): TunnelConfigs {
        return tunnelConfigDao.getAll()
    }

    override suspend fun save(tunnelConfig: TunnelConfig) {
        tunnelConfigDao.save(tunnelConfig)
    }

    override suspend fun updatePrimaryTunnel(tunnelConfig: TunnelConfig?) {
        tunnelConfigDao.resetPrimaryTunnel()
        tunnelConfig?.let {
            save(
                it.copy(
                    isPrimaryTunnel = true,
                ),
            )
        }

    }

    override suspend fun updateMobileDataTunnel(tunnelConfig: TunnelConfig?) {
        tunnelConfigDao.resetMobileDataTunnel()
        tunnelConfig?.let {
            save(
                it.copy(
                    isMobileDataTunnel = true,
                ),
            )
        }
    }

    override suspend fun delete(tunnelConfig: TunnelConfig) {
        tunnelConfigDao.delete(tunnelConfig)
    }

    override suspend fun getById(id: Int): TunnelConfig? {
        return tunnelConfigDao.getById(id.toLong())
    }

    override suspend fun count(): Int {
        return tunnelConfigDao.count().toInt()
    }

    override suspend fun findByTunnelName(name: String): TunnelConfig? {
        return tunnelConfigDao.getByName(name)
    }

    override suspend fun findByTunnelNetworksName(name: String): TunnelConfigs {
        return tunnelConfigDao.findByTunnelNetworkName(name)
    }

    override suspend fun findByMobileDataTunnel(): TunnelConfigs {
        return tunnelConfigDao.findByMobileDataTunnel()
    }

    override suspend fun findPrimary(): TunnelConfigs {
        return tunnelConfigDao.findByPrimary()
    }
}
