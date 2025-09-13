package com.ivelosi.dnc

import android.content.Context
import android.os.Handler

class HandlerFactory(private val context: Context) {
    fun buildHandler(): Handler = Handler(context.mainLooper)
}