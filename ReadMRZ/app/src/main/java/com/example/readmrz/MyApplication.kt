package com.example.readmrz

import android.app.Application


class MyApplication : Application() {
    override fun onCreate() {
        context = this
        super.onCreate()
    }

    companion object {
        // get context
        var context: MyApplication? = null
            private set

    }
}