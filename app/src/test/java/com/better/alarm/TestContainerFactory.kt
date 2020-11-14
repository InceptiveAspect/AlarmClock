package com.ias.alarm

import android.database.Cursor
import com.ias.alarm.model.AlarmStore
import com.ias.alarm.model.Calendars
import com.ias.alarm.model.ContainerFactory
import com.ias.alarm.persistance.PersistingContainerFactory
import com.ias.alarm.stores.InMemoryRxDataStoreFactory.Companion.inMemoryRxDataStore

/**
 * Created by Yuriy on 25.06.2017.
 */
class TestContainerFactory(private val calendars: Calendars) : ContainerFactory {
    private var idCounter: Int = 0
    val createdRecords = mutableListOf<AlarmStore>()

    override fun create(): AlarmStore {
        val inMemory = inMemoryRxDataStore(
                PersistingContainerFactory.create(
                        calendars = calendars,
                        idMapper = { _ -> idCounter++ }
                )
        )
        return object : AlarmStore {
            override var value = inMemory.value
            override fun observe() = inMemory.observe()
            override fun delete() {
                createdRecords.removeIf { it.value.id == value.id }
            }
        }
                .also { createdRecords.add(it) }
    }

    override fun create(cursor: Cursor): AlarmStore {
        throw UnsupportedOperationException()
    }
}
