package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.logcatter.LogcatReader
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.notification.WireGuardNotification
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
	@Singleton
	@ApplicationScope
	@Provides
	fun providesApplicationScope(@DefaultDispatcher defaultDispatcher: CoroutineDispatcher): CoroutineScope =
		CoroutineScope(SupervisorJob() + defaultDispatcher)

	@Singleton
	@Provides
	fun provideLogCollect(@ApplicationContext context: Context): LogReader {
		return LogcatReader.init(storageDir = context.filesDir.absolutePath)
	}

	@Singleton
	@Provides
	fun provideNotificationService(@ApplicationContext context: Context): NotificationService {
		return WireGuardNotification(context)
	}
}
