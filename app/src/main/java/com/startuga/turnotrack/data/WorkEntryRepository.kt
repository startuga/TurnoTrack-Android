package com.startuga.turnotrack.data

import com.startuga.turnotrack.data.db.WorkEntryDao
import com.startuga.turnotrack.data.db.toDomain
import com.startuga.turnotrack.data.db.toEntity
import com.startuga.turnotrack.domain.WorkEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class WorkEntryRepository(private val dao: WorkEntryDao) {

    val entries: Flow<List<WorkEntry>> =
        dao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    suspend fun getAll(): List<WorkEntry> = dao.getAll().mapNotNull { it.toDomain() }

    suspend fun get(date: LocalDate): WorkEntry? = dao.getByDate(date.toString())?.toDomain()

    suspend fun save(entry: WorkEntry) = dao.upsert(entry.toEntity())

    /** Devolve o registo apagado (para poder anular), ou null se não existia. */
    suspend fun delete(date: LocalDate): WorkEntry? {
        val existing = get(date) ?: return null
        dao.deleteByDate(date.toString())
        return existing
    }

    suspend fun replaceAll(entries: List<WorkEntry>) =
        dao.replaceAll(entries.map { it.toEntity() })
}
