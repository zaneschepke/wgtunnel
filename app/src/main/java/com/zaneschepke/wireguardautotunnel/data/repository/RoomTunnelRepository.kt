package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.util.extensions.Tunnels
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomTunnelRepository(
	private val tunnelConfigDao: TunnelConfigDao,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TunnelRepository {

	override val flow = tunnelConfigDao.getAllFlow().flowOn(ioDispatcher).map { it.map { it.toTunnel() } }

	override suspend fun getAll(): Tunnels {
		return withContext(ioDispatcher) {
			tunnelConfigDao.getAll().map { it.toTunnel() }
		}
	}

	override suspend fun save(tunnelConf: TunnelConf) {
		withContext(ioDispatcher) {
			tunnelConfigDao.save(TunnelConfig.from(tunnelConf))
		}
	}

	override suspend fun saveAll(tunnelConfs: List<TunnelConf>) {
		withContext(ioDispatcher) {
			tunnelConfigDao.saveAll(tunnelConfs.map(TunnelConfig::from))
		}
	}

	override suspend fun updatePrimaryTunnel(tunnelConf: TunnelConf?) {
		withContext(ioDispatcher) {
			tunnelConfigDao.resetPrimaryTunnel()
			tunnelConf?.let {
				save(
					it.copy(
						isPrimaryTunnel = true,
					),
				)
			}
		}
	}

	override suspend fun updateMobileDataTunnel(tunnelConf: TunnelConf?) {
		withContext(ioDispatcher) {
			tunnelConfigDao.resetMobileDataTunnel()
			tunnelConf?.let {
				save(
					it.copy(
						isMobileDataTunnel = true,
					),
				)
			}
		}
	}

	override suspend fun updateEthernetTunnel(tunnelConf: TunnelConf?) {
		withContext(ioDispatcher) {
			tunnelConfigDao.resetEthernetTunnel()
			tunnelConf?.let {
				save(
					it.copy(
						isEthernetTunnel = true,
					),
				)
			}
		}
	}

	override suspend fun delete(tunnelConf: TunnelConf) {
		withContext(ioDispatcher) {
			tunnelConfigDao.delete(TunnelConfig.from(tunnelConf))
		}
	}

	override suspend fun getById(id: Int): TunnelConf? {
		return withContext(ioDispatcher) { tunnelConfigDao.getById(id.toLong())?.toTunnel() }
	}

	override suspend fun getActive(): Tunnels {
		return withContext(ioDispatcher) {
			tunnelConfigDao.getActive().map { it.toTunnel() }
		}
	}

	override suspend fun count(): Int {
		return withContext(ioDispatcher) { tunnelConfigDao.count().toInt() }
	}

	override suspend fun findByTunnelName(name: String): TunnelConf? {
		return withContext(ioDispatcher) { tunnelConfigDao.getByName(name)?.toTunnel() }
	}

	override suspend fun findByTunnelNetworksName(name: String): Tunnels {
		return withContext(ioDispatcher) { tunnelConfigDao.findByTunnelNetworkName(name).map { it.toTunnel() } }
	}

	override suspend fun findByMobileDataTunnel(): Tunnels {
		return withContext(ioDispatcher) { tunnelConfigDao.findByMobileDataTunnel().map { it.toTunnel() } }
	}

	override suspend fun findPrimary(): Tunnels {
		return withContext(ioDispatcher) { tunnelConfigDao.findByPrimary().map { it.toTunnel() } }
	}
}
