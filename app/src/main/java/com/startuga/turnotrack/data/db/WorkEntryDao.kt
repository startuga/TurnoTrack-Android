package com.startuga.turnotrack.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkEntryDao {

    @Query("SELECT * FROM work_entries ORDER BY date")
    abstract fun observeAll(): Flow<List<WorkEntryEntity>>

    @Query("SELECT * FROM work_entries ORDER BY date")
    abstract suspend fun getAll(): List<WorkEntryEntity>

    @Query("SELECT * FROM work_entries WHERE date = :date")
    abstract suspend fun getByDate(date: String): WorkEntryEntity?

    @Upsert
    abstract suspend fun upsert(entry: WorkEntryEntity)

    @Query("DELETE FROM work_entries WHERE date = :date")
    abstract suspend fun deleteByDate(date: String): Int

    @Query("DELETE FROM work_entries")
    abstract suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entries: List<WorkEntryEntity>)

    /** Importar um backup substitui tudo, tal como na web app. */
    @Transaction
    open suspend fun replaceAll(entries: List<WorkEntryEntity>) {
        deleteAll()
        insertAll(entries)
    }
}
