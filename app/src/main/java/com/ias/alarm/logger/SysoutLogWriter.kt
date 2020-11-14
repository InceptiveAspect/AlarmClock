package com.ias.alarm.logger

import com.ias.alarm.logger.Logger.LogLevel

class SysoutLogWriter : LogWriter {
    override fun write(level: LogLevel, tag: String, message: String, e: Throwable?) {
        when (level) {
            LogLevel.INF, LogLevel.DBG -> {
                println("$tag, $message")
                e?.printStackTrace()
            }

            LogLevel.WRN, LogLevel.ERR -> {
                System.err.println("$tag, $message")
                e?.printStackTrace()
            }
        }
    }
}
