package com.startuga.turnotrack.data.backup

import com.startuga.turnotrack.domain.ShiftType
import com.startuga.turnotrack.domain.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class BackupCodecTest {

    /** Exemplo no formato exato que a web app exporta (JSON.stringify(entries, null, 2)). */
    private val webBackup = """
        [
          {
            "id": "1759831200000",
            "date": "2026-10-01",
            "type": "REGULAR",
            "isHoliday": false,
            "scheduledStart": "09:00",
            "scheduledEnd": "18:00",
            "actualStart": "09:10",
            "actualEnd": "18:00",
            "notes": "Reunião às 15h"
          },
          {
            "id": "1759917600000",
            "date": "2026-10-02",
            "type": "DAY_OFF"
          }
        ]
    """.trimIndent()

    @Test
    fun decodesWebAppBackup() {
        val result = BackupCodec.decode(webBackup) as BackupCodec.DecodeResult.Success
        assertEquals(2, result.entries.size)
        val first = result.entries[0]
        assertEquals("1759831200000", first.id)
        assertEquals(LocalDate.of(2026, 10, 1), first.date)
        assertEquals(ShiftType.REGULAR, first.type)
        assertEquals(LocalTime.of(9, 10), first.actualStart)
        assertEquals("Reunião às 15h", first.notes)
        assertEquals(ShiftType.DAY_OFF, result.entries[1].type)
        assertNull(result.entries[1].actualStart)
    }

    @Test
    fun roundTripKeepsEverything() {
        val entries = listOf(
            WorkEntry(
                id = "a", date = LocalDate.of(2026, 1, 2), type = ShiftType.REGULAR, isHoliday = true,
                scheduledStart = LocalTime.of(22, 0), scheduledEnd = LocalTime.of(6, 0),
                actualStart = LocalTime.of(22, 5), actualEnd = LocalTime.of(6, 0),
                overtimeStart = LocalTime.of(6, 0), overtimeEnd = LocalTime.of(7, 30),
                notes = "Noite",
            ),
            WorkEntry(id = "b", date = LocalDate.of(2026, 1, 1), type = ShiftType.VACATION),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(entries)) as BackupCodec.DecodeResult.Success
        assertEquals(entries.sortedBy { it.date }, decoded.entries)
    }

    @Test
    fun sanitizesLikeWebApp() {
        val json = """
            [
              {"date": "2026-02-03", "type": "UNKNOWN", "isHoliday": "sim", "actualStart": 900},
              {"date": "03/02/2026", "type": "REGULAR"},
              {"date": "2026-02-30", "type": "REGULAR"},
              {"type": "REGULAR"},
              "texto",
              {"id": "x", "date": "2026-02-04", "isHoliday": 0}
            ]
        """.trimIndent()
        val result = BackupCodec.decode(json) as BackupCodec.DecodeResult.Success
        assertEquals(2, result.entries.size)
        val first = result.entries[0]
        assertEquals(ShiftType.REGULAR, first.type) // tipo inválido → REGULAR
        assertTrue(first.isHoliday) // Boolean("sim") == true
        assertNull(first.actualStart) // não é texto → ignorado
        assertTrue(first.id.isNotBlank()) // id gerado
        assertFalse(result.entries[1].isHoliday)
    }

    @Test
    fun rejectsInvalidFiles() {
        assertTrue(BackupCodec.decode("isto não é json") is BackupCodec.DecodeResult.Failure)
        assertTrue(BackupCodec.decode("""{"date": "2026-01-01"}""") is BackupCodec.DecodeResult.Failure)
        assertTrue(BackupCodec.decode("""[{"foo": 1}]""") is BackupCodec.DecodeResult.Failure)
        val empty = BackupCodec.decode("[]") as BackupCodec.DecodeResult.Success
        assertTrue(empty.entries.isEmpty())
    }
}
