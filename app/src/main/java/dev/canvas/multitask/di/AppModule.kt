package dev.canvas.multitask.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for app-level dependency injection.
 * ShizukuManager and PreferencesManager are already @Singleton @Inject,
 * so they are auto-provided by Hilt. This module is reserved for
 * dependencies that require manual construction.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Currently all dependencies are constructor-injected.
    // Add @Provides methods here for external or interface bindings as needed.
}
