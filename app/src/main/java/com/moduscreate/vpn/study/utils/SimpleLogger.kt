package com.moduscreate.vpn.study.utils

import android.util.Log

object SimpleLogger {
    private const val generalTag = "SimpleLogger"

    fun log(text: String?, tag: String = generalTag) {
        Log.i(tag, text ?: "Null")
    }
}