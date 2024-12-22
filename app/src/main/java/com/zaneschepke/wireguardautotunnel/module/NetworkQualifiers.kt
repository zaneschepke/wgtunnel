package com.zaneschepke.wireguardautotunnel.module

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Wifi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobileData

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Ethernet
