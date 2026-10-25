package com.kbretro.babyregistry.reminders

import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.label
import javax.inject.Inject
import javax.inject.Singleton

/** Traduce eventos y citas a recordatorios concretos. */
@Singleton
class Reminders @Inject constructor(
    private val scheduler: ReminderScheduler,
    private val repo: TrackerRepository,
) {
    fun sync(event: EventEntity) {
        val key = ReminderScheduler.eventKey(event.id)
        val at = event.reminderAt
        if (at == null) {
            scheduler.cancel(key)
        } else {
            val name = event.medicineName ?: "medicación"
            scheduler.schedule(key, at, "Próxima dosis: $name", listOfNotNull(event.dose, "Hora de la siguiente toma").joinToString(" · "))
        }
    }

    fun sync(appointment: AppointmentEntity) {
        val key = ReminderScheduler.appointmentKey(appointment.id)
        val at = appointment.reminderAt
        if (at == null || appointment.done) {
            scheduler.cancel(key)
        } else {
            scheduler.schedule(
                key,
                at,
                "${appointment.kind.label}: ${appointment.title}",
                Fmt.dayTime(appointment.time),
            )
        }
    }

    fun cancelEvent(id: Long) = scheduler.cancel(ReminderScheduler.eventKey(id))

    fun cancelAppointment(id: Long) = scheduler.cancel(ReminderScheduler.appointmentKey(id))

    /** Las alarmas se pierden al reiniciar el móvil: se vuelven a programar desde la base de datos. */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        repo.pendingEventReminders(now).forEach(::sync)
        repo.pendingAppointmentReminders(now).forEach(::sync)
    }
}
