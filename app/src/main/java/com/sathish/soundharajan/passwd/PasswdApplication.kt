package com.sathish.soundharajan.passwd

import android.app.Application
import com.sathish.soundharajan.passwd.data.VaultManager
import com.sathish.soundharajan.passwd.security.AuthManager
import net.sqlcipher.database.SQLiteDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PasswdApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
        // Hilt will handle initialization of injected components
    }
}
