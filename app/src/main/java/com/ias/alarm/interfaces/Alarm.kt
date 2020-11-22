package com.ias.alarm.interfaces

import com.ias.alarm.model.AlarmValue
import com.ias.alarm.model.Alarmtone

interface Alarm {
    fun enable(enable: Boolean)
    fun snooze()
    fun snooze(hourOfDay: Int, minute: Int)
    fun dismiss()
    fun groupDismiss()
    fun requestSkip()
    fun isSkipping(): Boolean
    fun delete()

    /** Change something and commit */
    fun edit(func: AlarmValue.() -> AlarmValue)
    val id: Int
    val labelOrDefault: String
    val alarmtone: Alarmtone
    val data: AlarmValue
}