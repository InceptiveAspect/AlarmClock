package com.ias.alarm.wakelock

interface Wakelocks {
    fun acquireServiceLock()

    fun releaseServiceLock()
}