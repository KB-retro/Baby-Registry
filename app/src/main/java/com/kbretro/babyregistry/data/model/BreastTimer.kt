package com.kbretro.babyregistry.data.model

/** Lógica pura del temporizador de pecho con cambio de lado y pausa. */
object BreastTimer {

    fun start(babyId: Long, side: BreastSide, now: Long) = EventEntity(
        babyId = babyId,
        type = EventType.BREAST,
        startTime = now,
        activeSide = side,
        activeSideSince = now,
        lastSide = side,
    )

    /** Pulsar el lado activo lo pausa; pulsar el otro cambia de lado. */
    fun tapSide(event: EventEntity, side: BreastSide, now: Long): EventEntity {
        val wasActive = event.activeSide
        val paused = accumulate(event, now)
        return if (wasActive == side) {
            paused.copy(lastSide = side)
        } else {
            paused.copy(activeSide = side, activeSideSince = now, lastSide = side)
        }
    }

    fun stop(event: EventEntity, now: Long): EventEntity = accumulate(event, now).copy(endTime = now)

    fun sideMillis(event: EventEntity, side: BreastSide, now: Long): Long {
        val stored = if (side == BreastSide.LEFT) event.leftMillis else event.rightMillis
        val since = event.activeSideSince
        val running = if (event.activeSide == side && since != null) (now - since).coerceAtLeast(0) else 0
        return stored + running
    }

    fun totalMillis(event: EventEntity, now: Long): Long =
        sideMillis(event, BreastSide.LEFT, now) + sideMillis(event, BreastSide.RIGHT, now)

    private fun accumulate(event: EventEntity, now: Long): EventEntity {
        val side = event.activeSide ?: return event
        val since = event.activeSideSince ?: return event.copy(activeSide = null)
        val elapsed = (now - since).coerceAtLeast(0)
        return when (side) {
            BreastSide.LEFT -> event.copy(leftMillis = event.leftMillis + elapsed)
            BreastSide.RIGHT -> event.copy(rightMillis = event.rightMillis + elapsed)
        }.copy(activeSide = null, activeSideSince = null)
    }
}
