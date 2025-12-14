package com.sathish.soundharajan.passwd.di

import android.content.Context
import android.content.SharedPreferences
import com.sathish.soundharajan.passwd.security.AndroidCryptoManager
import com.sathish.soundharajan.passwd.security.CryptoManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindCryptoManager(
        cryptoManager: AndroidCryptoManager
    ): CryptoManager

    companion object {
        @Provides
        @Singleton
        fun provideSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences {
            return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        }
    }
}
