package com.kbretro.babyregistry.data.db

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.AppointmentKind
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.GrowthEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Crea una base de datos con el esquema v1 (fase 1), la migra a v2 y comprueba que Room
 * acepta el esquema resultante y que los datos se conservan.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {

    private val v1Schema = listOf(
        "CREATE TABLE IF NOT EXISTS `babies` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`name` TEXT NOT NULL, `birthDate` INTEGER NOT NULL, `sex` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`babyId` INTEGER NOT NULL, `type` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, " +
            "`leftMillis` INTEGER NOT NULL, `rightMillis` INTEGER NOT NULL, `activeSide` TEXT, " +
            "`activeSideSince` INTEGER, `lastSide` TEXT, `amountMl` INTEGER, `bottleContent` TEXT, `food` TEXT, " +
            "`isNewFood` INTEGER NOT NULL, `reaction` TEXT, `diaperKind` TEXT, `stoolColor` TEXT, `notes` TEXT, " +
            "FOREIGN KEY(`babyId`) REFERENCES `babies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_events_babyId` ON `events` (`babyId`)",
        "CREATE INDEX IF NOT EXISTS `index_events_startTime` ON `events` (`startTime`)",
        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
        "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '5e0f7b63136c0925e9930e305d86976c')",
    )

    @Test
    fun `migrar de v1 a v2 conserva los datos`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            v1Schema.forEach(db::execSQL)
            db.execSQL("INSERT INTO babies (id, name, birthDate, sex, createdAt) VALUES (1, 'Leo', 20000, 'BOY', 0)")
            db.execSQL(
                "INSERT INTO events (id, babyId, type, startTime, endTime, leftMillis, rightMillis, isNewFood, " +
                    "amountMl, bottleContent) VALUES (1, 1, 'BOTTLE', 1000, 1000, 0, 0, 0, 120, 'FORMULA')",
            )
            db.version = 1
        }

        val room = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        // Abrir la base de datos ejecuta la migración y la validación del esquema de Room.
        room.openHelper.writableDatabase

        runBlocking {
            val baby = room.babyDao().observeFirst().first()
            assertEquals("Leo", baby?.name)

            val event = room.eventDao().observeAll(1).first().single()
            assertEquals(EventType.BOTTLE, event.type)
            assertEquals(120, event.amountMl)
            assertNull(event.temperatureC)

            room.growthDao().insert(GrowthEntity(babyId = 1, date = LocalDate.of(2026, 1, 1), weightKg = 4.2))
            assertEquals(4.2, room.growthDao().observeAll(1).first().single().weightKg!!, 0.0)

            room.appointmentDao().insert(
                AppointmentEntity(babyId = 1, time = 5000, kind = AppointmentKind.VACCINE, title = "2 meses"),
            )
            assertEquals(1, room.appointmentDao().observeAll(1).first().size)
        }
        room.close()
    }
}
