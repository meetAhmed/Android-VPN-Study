package com.moduscreate.vpn.study.extensions

import java.text.SimpleDateFormat
import java.util.*

fun Date.toReadableTime(): String {
    val df = SimpleDateFormat("hh:mm:ss", Locale.US)
    return df.format(this)
}