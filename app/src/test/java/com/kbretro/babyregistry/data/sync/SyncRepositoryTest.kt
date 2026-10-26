package com.kbretro.babyregistry.data.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kbretro.babyregistry.data.db.AppDatabase
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.MilestoneKind
import com.kbretro.babyregistry.data.model.SyncDeletionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Simula dos móviles de la misma familia intercambiando cambios. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SyncRepositoryTest {
    private fun newDb() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    private lateinit var phoneA: AppDatabase
    private lateinit var phoneB: AppDatabase
    private lateinit var syncA: SyncRepository
    private lateinit var syncB: SyncRepository

    @Before
    fun open() {
        phoneA = newDb()
        phoneB = newDb()
        syncA = SyncRepository(phoneA)
        syncB = SyncRepository(phoneB)
    }

    @After
    fun close() {
        phoneA.close()
        phoneB.close()
    }

    @Test
    fun `los cambios de un movil llegan al otro y gana el mas reciente`() = runBlocking {
        // Cada móvil asigna sus propios id locales: se crea antes otro bebé en B para que no coincidan.
        phoneB.babyDao().insert(BabyEntity(name = "Otro", birthDate = LocalDate.of(2025, 1, 1)))

        val babyIdA = phoneA.babyDao().insert(BabyEntity(name = "Nico", birthDate = LocalDate.of(2026, 5, 1), updatedAt = 1_000))
        val eventIdA = phoneA.eventDao().insert(
            EventEntity(babyId = babyIdA, type = EventType.BOTTLE, startTime = 5_000, endTime = 5_000, amountMl = 90, updatedAt = 1_000),
        )
        phoneA.milestoneDao().insert(
            MilestoneEntity(babyId = babyIdA, time = 6_000, kind = MilestoneKind.FIRST_TOOTH, updatedAt = 1_000),
        )

        syncB.applyRemote(syncA.localChanges(since = 0))

        val nicoB = phoneB.syncDao().allBabies().single { it.name == "Nico" }
        val eventB = phoneB.eventDao().observeAll(nicoB.id).first().single()
        assertEquals(90, eventB.amountMl)
        assertEquals(phoneA.eventDao().getById(eventIdA)!!.uuid, eventB.uuid)
        assertEquals(1, phoneB.milestoneDao().observeAll(nicoB.id).first().size)

        // A corrige el biberón (más reciente): B lo actualiza.
        val eventA = phoneA.eventDao().getById(eventIdA)!!
        phoneA.eventDao().update(eventA.copy(amountMl = 120, updatedAt = 2_000))
        syncB.applyRemote(syncA.localChanges(since = 1_500))
        assertEquals(120, phoneB.eventDao().getById(eventB.id)!!.amountMl)

        // Un cambio antiguo que llega tarde no pisa el nuevo.
        syncB.applyRemote(ChangeSet(events = listOf(Synced(nicoB.uuid, eventA.copy(amountMl = 50, updatedAt = 1_500)))))
        assertEquals(120, phoneB.eventDao().getById(eventB.id)!!.amountMl)

        // A borra el biberón: también desaparece en B.
        phoneA.eventDao().delete(eventA)
        phoneA.syncDao().logDeletion(SyncDeletionEntity(entity = "event", uuid = eventA.uuid, deletedAt = 3_000))
        val changes = syncA.localChanges(since = 2_500)
        assertEquals(1, changes.deletions.size)
        syncB.applyRemote(changes)
        assertNull(phoneB.eventDao().getById(eventB.id))
    }

    @Test
    fun `un borrado anterior a una edicion no borra`() = runBlocking {
        val babyId = phoneB.babyDao().insert(BabyEntity(name = "Nico", birthDate = LocalDate.of(2026, 5, 1)))
        val event = EventEntity(babyId = babyId, type = EventType.DIAPER, startTime = 1, endTime = 1, updatedAt = 5_000)
        phoneB.eventDao().insert(event)

        syncB.applyRemote(ChangeSet(deletions = listOf(SyncDeletionEntity(entity = "event", uuid = event.uuid, deletedAt = 4_000))))
        assertTrue(phoneB.syncDao().eventByUuid(event.uuid) != null)
    }
}
