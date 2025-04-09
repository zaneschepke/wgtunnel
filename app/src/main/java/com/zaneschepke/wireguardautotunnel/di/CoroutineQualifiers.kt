package com.zaneschepke.wireguardautotunnel.di

import javax.inject.Qualifier

@Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class DefaultDispatcher

@Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class IoDispatcher

@Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class MainDispatcher

@Retention(AnnotationRetention.BINARY) @Qualifier annotation class MainImmediateDispatcher

@Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class ApplicationScope

@Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class ServiceScope
