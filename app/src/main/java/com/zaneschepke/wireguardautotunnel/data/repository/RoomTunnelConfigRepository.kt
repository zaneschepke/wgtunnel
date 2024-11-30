package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RoomTunnelConfigRepository(
	private val tunnelConfigDao: TunnelConfigDao,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) :
	TunnelConfigRepository {
	override fun getTunnelConfigsFlow(): Flow<TunnelConfigs> {
		return tunnelConfigDao.getAllFlow()
	}

	override suspend fun getAll(): TunnelConfigs {
		return withContext(ioDispatcher) { tunnelConfigDao.getAll() }
	}

	override suspend fun save(tunnelConfig: TunnelConfig) {
		withContext(ioDispatcher) {
			tunnelConfigDao.save(tunnelConfig)
		}
	}

	override suspend fun updatePrimaryTunnel(tunnelConfig: TunnelConfig?) {
		withContext(ioDispatcher) {
			tunnelConfigDao.resetPrimaryTunnel()
			tunnelConfig?.let {
				save(
					it.copy(
						isPrimaryTunnel = true,
					),
				)
			}
		}
	}

	override suspend fun updateMobileDataTunnel(tunnelConfig: TunnelConfig?) {
		withContext(ioDispatcher) {
			tunnelConfigDao.resetMobileDataTunnel()
			tunnelConfig?.let {
				save(
					it.copy(
						isMobileDataTunnel = true,
					),
				)
			}
		}
	}

	override suspend fun updateEthernetTunnel(tunnelConfig: TunnelConfig?) {
		withContext(ioDispatcher) {
			tunnelConfigDao.resetEthernetTunnel()
			tunnelConfig?.let {
				save(
					it.copy(
						isEthernetTunnel = true,
					),
				)
			}
		}
	}

	override suspend fun delete(tunnelConfig: TunnelConfig) {
		withContext(ioDispatcher) {
			tunnelConfigDao.delete(tunnelConfig)
		}
	}

	override suspend fun getById(id: Int): TunnelConfig? {
		return withContext(ioDispatcher) { tunnelConfigDao.getById(id.toLong()) }
	}

	override suspend fun getActive(): TunnelConfigs {
		return withContext(ioDispatcher) {
			tunnelConfigDao.getActive()
		}
	}

	override suspend fun count(): Int {
		return withContext(ioDispatcher) { tunnelConfigDao.count().toInt() }
	}

	override suspend fun findByTunnelName(name: String): TunnelConfig? {
		return withContext(ioDispatcher) { tunnelConfigDao.getByName(name) }
	}

	override suspend fun findByTunnelNetworksName(name: String): TunnelConfigs {
		return withContext(ioDispatcher) { tunnelConfigDao.findByTunnelNetworkName(name) }
	}

	override suspend fun findByMobileDataTunnel(): TunnelConfigs {
		return withContext(ioDispatcher) { tunnelConfigDao.findByMobileDataTunnel() }
	}

	override suspend fun findPrimary(): TunnelConfigs {
		return withContext(ioDispatcher) { tunnelConfigDao.findByPrimary() }
	}
}
