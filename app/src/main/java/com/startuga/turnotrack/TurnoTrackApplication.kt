package com.startuga.turnotrack

import android.app.Application
import android.content.Context
import com.startuga.turnotrack.ai.WorkAssistant
import com.startuga.turnotrack.data.SettingsRepository
import com.startuga.turnotrack.data.WorkEntryRepository
import com.startuga.turnotrack.data.db.AppDatabase

class TurnoTrackApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Dependências partilhadas pela app (injeção manual, sem frameworks). */
class AppContainer(context: Context) {
    private val database = AppDatabase.build(context)
    val repository = WorkEntryRepository(database.workEntryDao())
    val settings = SettingsRepository(context)
    val assistant = WorkAssistant(repository)
}
