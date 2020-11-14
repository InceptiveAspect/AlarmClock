package com.ias.alarm.configuration

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.os.Vibrator
import android.telephony.TelephonyManager
import com.ias.alarm.alert.BackgroundNotifications
import com.ias.alarm.background.AlertServicePusher
import com.ias.alarm.background.KlaxonPlugin
import com.ias.alarm.background.PlayerWrapper
import com.ias.alarm.bugreports.BugReporter
import com.ias.alarm.interfaces.IAlarmsManager
import com.ias.alarm.logger.LogcatLogWriter
import com.ias.alarm.logger.Logger
import com.ias.alarm.logger.LoggerFactory
import com.ias.alarm.logger.StartupLogWriter
import com.ias.alarm.model.AlarmCore
import com.ias.alarm.model.AlarmCoreFactory
import com.ias.alarm.model.AlarmSetter
import com.ias.alarm.model.AlarmStateNotifier
import com.ias.alarm.model.Alarms
import com.ias.alarm.model.AlarmsScheduler
import com.ias.alarm.model.Calendars
import com.ias.alarm.model.ContainerFactory
import com.ias.alarm.model.IAlarmsScheduler
import com.ias.alarm.persistance.DatabaseQuery
import com.ias.alarm.persistance.PersistingContainerFactory
import com.ias.alarm.presenter.AlarmsListActivity
import com.ias.alarm.presenter.DynamicThemeHandler
import com.ias.alarm.presenter.ScheduledReceiver
import com.ias.alarm.presenter.ToastPresenter
import com.ias.alarm.stores.SharedRxDataStoreFactory
import com.ias.alarm.util.Optional
import com.ias.alarm.wakelock.WakeLockManager
import com.ias.alarm.wakelock.Wakelocks
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.koin.core.Koin
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import java.util.ArrayList
import java.util.Calendar

fun Scope.logger(tag: String): Logger {
    return get<LoggerFactory>().createLogger(tag)
}

fun Koin.logger(tag: String): Logger {
    return get<LoggerFactory>().createLogger(tag)
}

fun startKoin(context: Context): Koin {
    // The following line triggers the initialization of ACRA

    val module = module {
        single<DynamicThemeHandler> { DynamicThemeHandler(get()) }
        single<StartupLogWriter> { StartupLogWriter.create() }
        single<LoggerFactory> {
            Logger.factory(
                    LogcatLogWriter.create(),
                    get<StartupLogWriter>()
            )
        }
        single<BugReporter> { BugReporter(logger("BugReporter"), context, lazy { get<StartupLogWriter>() }) }
        factory<Context> { context }
        factory(named("dateFormatOverride")) { "none" }
        factory<Single<Boolean>>(named("dateFormat")) {
            Single.fromCallable {
                get<String>(named("dateFormatOverride")).let { if (it == "none") null else it.toBoolean() }
                        ?: android.text.format.DateFormat.is24HourFormat(context)
            }
        }

        single<Prefs> {
            val factory = SharedRxDataStoreFactory.create(get(), logger("preferences"))
            Prefs.create(get(named("dateFormat")), factory)
        }

        single<Store> {
            Store(
                    alarmsSubject = BehaviorSubject.createDefault(ArrayList()),
                    next = BehaviorSubject.createDefault<Optional<Store.Next>>(Optional.absent()),
                    sets = PublishSubject.create(),
                    events = PublishSubject.create())
        }

        factory { get<Context>().getSystemService(Context.ALARM_SERVICE) as AlarmManager }
        single<AlarmSetter> { AlarmSetter.AlarmSetterImpl(logger("AlarmSetter"), get(), get()) }
        factory { Calendars { Calendar.getInstance() } }
        single<AlarmsScheduler> { AlarmsScheduler(get(), logger("AlarmsScheduler"), get(), get(), get()) }
        factory<IAlarmsScheduler> { get<AlarmsScheduler>() }
        single<AlarmCore.IStateNotifier> { AlarmStateNotifier(get()) }
        single<ContainerFactory> { PersistingContainerFactory(get(), get()) }
        factory { get<Context>().contentResolver }
        single<DatabaseQuery> { DatabaseQuery(get(), get()) }
        single<AlarmCoreFactory> { AlarmCoreFactory(logger("AlarmCore"), get(), get(), get(), get(), get()) }
        single<Alarms> { Alarms(get(), get(), get(), get(), logger("Alarms")) }
        factory<IAlarmsManager> { get<Alarms>() }
        single { ScheduledReceiver(get(), get(), get(), get()) }
        single { ToastPresenter(get(), get()) }
        single { AlertServicePusher(get(), get(), get(), logger("AlertServicePusher")) }
        single { BackgroundNotifications(get(), get(), get(), get(), get()) }
        factory<Wakelocks> { get<WakeLockManager>() }
        factory<Scheduler> { AndroidSchedulers.mainThread() }
        single<WakeLockManager> { WakeLockManager(logger("WakeLockManager"), get()) }
        factory { get<Context>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
        factory { get<Context>().getSystemService(Context.POWER_SERVICE) as PowerManager }
        factory { get<Context>().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
        factory { get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
        factory { get<Context>().getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        factory { get<Context>().resources }

        factory(named("volumePreferenceDemo")) {
            KlaxonPlugin(
                    log = logger("VolumePreference"),
                    playerFactory = { PlayerWrapper(get(), get(), logger("VolumePreference")) },
                    prealarmVolume = get<Prefs>().preAlarmVolume.observe(),
                    fadeInTimeInMillis = Observable.just(100),
                    inCall = Observable.just(false),
                    scheduler = get()
            )
        }
    }

    return startKoin {
        modules(module)
        modules(AlarmsListActivity.uiStoreModule)
    }.koin
}

fun overrideIs24hoursFormatOverride(is24hours: Boolean) {
    loadKoinModules(module = module(override = true) {
        factory(named("dateFormatOverride")) { is24hours.toString() }
    })
}