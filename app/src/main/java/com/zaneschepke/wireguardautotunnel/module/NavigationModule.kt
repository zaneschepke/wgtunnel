package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import androidx.navigation.NavHostController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.NavigationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object NavigationModule {

	@Provides
	@ActivityRetainedScoped
	fun provideNestedNavController(@ApplicationContext context: Context): NavHostController {
		return NavigationService(context).navController
	}
}
