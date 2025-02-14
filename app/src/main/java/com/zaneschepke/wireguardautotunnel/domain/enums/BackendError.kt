package com.zaneschepke.wireguardautotunnel.domain.enums

sealed class BackendError() {
	data object DNS : BackendError()
	data object Unauthorized : BackendError()
	data object Config : BackendError()
}
