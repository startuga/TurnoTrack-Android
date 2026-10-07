package com.startuga.turnotrack.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.startuga.turnotrack.domain.ShiftType
import com.startuga.turnotrack.domain.TimeFormat
import com.startuga.turnotrack.domain.WorkEntry
import java.time.LocalDate

/**
 * Tabela de registos. A data (yyyy-MM-dd) é a chave primária: um registo por dia.
 * As horas ficam em texto "HH:mm", tal como na web app.
 */
@Entity(tableName = "work_entries")
data class WorkEntryEntity(
    @PrimaryKey val date: String,
    val id: String,
    val type: String,
    @ColumnInfo(name = "is_holiday") val isHoliday: Boolean,
    @ColumnInfo(name = "scheduled_start") val scheduledStart: String?,
    @ColumnInfo(name = "scheduled_end") val scheduledEnd: String?,
    @ColumnInfo(name = "actual_start") val actualStart: String?,
    @ColumnInfo(name = "actual_end") val actualEnd: String?,
    @ColumnInfo(name = "overtime_start") val overtimeStart: String?,
    @ColumnInfo(name = "overtime_end") val overtimeEnd: String?,
    val notes: String?,
)

fun WorkEntry.toEntity() = WorkEntryEntity(
    date = date.toString(),
    id = id,
    type = type.name,
    isHoliday = isHoliday,
    scheduledStart = TimeFormat.format(scheduledStart),
    scheduledEnd = TimeFormat.format(scheduledEnd),
    actualStart = TimeFormat.format(actualStart),
    actualEnd = TimeFormat.format(actualEnd),
    overtimeStart = TimeFormat.format(overtimeStart),
    overtimeEnd = TimeFormat.format(overtimeEnd),
    notes = notes,
)

fun WorkEntryEntity.toDomain(): WorkEntry? {
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return WorkEntry(
        id = id,
        date = parsedDate,
        type = ShiftType.fromName(type) ?: ShiftType.REGULAR,
        isHoliday = isHoliday,
        scheduledStart = TimeFormat.parse(scheduledStart),
        scheduledEnd = TimeFormat.parse(scheduledEnd),
        actualStart = TimeFormat.parse(actualStart),
        actualEnd = TimeFormat.parse(actualEnd),
        overtimeStart = TimeFormat.parse(overtimeStart),
        overtimeEnd = TimeFormat.parse(overtimeEnd),
        notes = notes,
    )
}
