package com.kbretro.babyregistry.data.transfer

import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.AppointmentKind
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.BottleContent
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.FeedingType
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.MilestoneKind
import com.kbretro.babyregistry.data.model.Sex
import com.kbretro.babyregistry.data.model.StoolColor
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Todos los datos de un bebé, tal como se guardan en la copia de seguridad. */
data class BabyBackup(
    val baby: BabyEntity,
    val events: List<EventEntity>,
    val growth: List<GrowthEntity>,
    val appointments: List<AppointmentEntity>,
    val milestones: List<MilestoneEntity> = emptyList(),
)

/**
 * Copia de seguridad completa en JSON. Las fechas van en ISO (yyyy-MM-dd) y las horas en epoch millis.
 * Los identificadores internos no se guardan: al restaurar se asignan nuevos.
 */
object BackupJson {
    const val APP = "baby-registry"
    const val VERSION = 1

    fun isBackup(root: JSONObject): Boolean = root.optString("app") == APP

    fun write(babies: List<BabyBackup>, exportedAt: Long): String = JSONObject()
        .put("app", APP)
        .put("version", VERSION)
        .put("exportedAt", exportedAt)
        .put("babies", JSONArray(babies.map(::babyToJson)))
        .toString(2)

    fun read(root: JSONObject): List<BabyBackup> {
        val babies = root.optJSONArray("babies") ?: return emptyList()
        return (0 until babies.length()).map { babyFromJson(babies.getJSONObject(it)) }
    }

    private fun babyToJson(b: BabyBackup): JSONObject = JSONObject()
        .put("name", b.baby.name)
        .put("birthDate", b.baby.birthDate.toString())
        .put("sex", b.baby.sex.name)
        .putOpt("feedingType", b.baby.feedingType?.name)
        .putOpt("hiddenTypes", b.baby.hiddenTypes)
        .put("events", JSONArray(b.events.map(::eventToJson)))
        .put("growth", JSONArray(b.growth.map(::growthToJson)))
        .put("appointments", JSONArray(b.appointments.map(::appointmentToJson)))
        .put("milestones", JSONArray(b.milestones.map(::milestoneToJson)))

    private fun babyFromJson(o: JSONObject): BabyBackup {
        val baby = BabyEntity(
            name = o.getString("name"),
            birthDate = LocalDate.parse(o.getString("birthDate")),
            sex = enumOrNull<Sex>(o.optString("sex")) ?: Sex.UNSPECIFIED,
            feedingType = enumOrNull<FeedingType>(o.optString("feedingType")),
            hiddenTypes = o.optStringOrNull("hiddenTypes"),
        )
        return BabyBackup(
            baby = baby,
            events = o.optJSONArray("events").objects().mapNotNull(::eventFromJson),
            growth = o.optJSONArray("growth").objects().map(::growthFromJson),
            appointments = o.optJSONArray("appointments").objects().mapNotNull(::appointmentFromJson),
            milestones = o.optJSONArray("milestones").objects().mapNotNull(::milestoneFromJson),
        )
    }

    private fun eventToJson(e: EventEntity): JSONObject = JSONObject()
        .put("type", e.type.name)
        .put("startTime", e.startTime)
        .putOpt("endTime", e.endTime)
        .put("leftMillis", e.leftMillis)
        .put("rightMillis", e.rightMillis)
        .putOpt("activeSide", e.activeSide?.name)
        .putOpt("activeSideSince", e.activeSideSince)
        .putOpt("lastSide", e.lastSide?.name)
        .putOpt("amountMl", e.amountMl)
        .putOpt("bottleContent", e.bottleContent?.name)
        .putOpt("food", e.food)
        .put("isNewFood", e.isNewFood)
        .putOpt("reaction", e.reaction)
        .putOpt("diaperKind", e.diaperKind?.name)
        .putOpt("stoolColor", e.stoolColor?.name)
        .putOpt("temperatureC", e.temperatureC)
        .putOpt("medicineName", e.medicineName)
        .putOpt("dose", e.dose)
        .putOpt("reminderAt", e.reminderAt)
        .putOpt("notes", e.notes)

    private fun eventFromJson(o: JSONObject): EventEntity? {
        val type = enumOrNull<EventType>(o.optString("type")) ?: return null
        return EventEntity(
            babyId = 0,
            type = type,
            startTime = o.getLong("startTime"),
            endTime = o.optLongOrNull("endTime"),
            leftMillis = o.optLong("leftMillis"),
            rightMillis = o.optLong("rightMillis"),
            activeSide = enumOrNull<BreastSide>(o.optString("activeSide")),
            activeSideSince = o.optLongOrNull("activeSideSince"),
            lastSide = enumOrNull<BreastSide>(o.optString("lastSide")),
            amountMl = if (o.has("amountMl")) o.getInt("amountMl") else null,
            bottleContent = enumOrNull<BottleContent>(o.optString("bottleContent")),
            food = o.optStringOrNull("food"),
            isNewFood = o.optBoolean("isNewFood"),
            reaction = o.optStringOrNull("reaction"),
            diaperKind = enumOrNull<DiaperKind>(o.optString("diaperKind")),
            stoolColor = enumOrNull<StoolColor>(o.optString("stoolColor")),
            temperatureC = if (o.has("temperatureC")) o.getDouble("temperatureC") else null,
            medicineName = o.optStringOrNull("medicineName"),
            dose = o.optStringOrNull("dose"),
            reminderAt = o.optLongOrNull("reminderAt"),
            notes = o.optStringOrNull("notes"),
        )
    }

    private fun growthToJson(g: GrowthEntity): JSONObject = JSONObject()
        .put("date", g.date.toString())
        .putOpt("weightKg", g.weightKg)
        .putOpt("lengthCm", g.lengthCm)
        .putOpt("headCm", g.headCm)
        .putOpt("notes", g.notes)

    private fun growthFromJson(o: JSONObject) = GrowthEntity(
        babyId = 0,
        date = LocalDate.parse(o.getString("date")),
        weightKg = if (o.has("weightKg")) o.getDouble("weightKg") else null,
        lengthCm = if (o.has("lengthCm")) o.getDouble("lengthCm") else null,
        headCm = if (o.has("headCm")) o.getDouble("headCm") else null,
        notes = o.optStringOrNull("notes"),
    )

    private fun appointmentToJson(a: AppointmentEntity): JSONObject = JSONObject()
        .put("time", a.time)
        .put("kind", a.kind.name)
        .put("title", a.title)
        .putOpt("notes", a.notes)
        .putOpt("reminderAt", a.reminderAt)
        .put("done", a.done)

    private fun appointmentFromJson(o: JSONObject): AppointmentEntity? {
        val kind = enumOrNull<AppointmentKind>(o.optString("kind")) ?: return null
        return AppointmentEntity(
            babyId = 0,
            time = o.getLong("time"),
            kind = kind,
            title = o.getString("title"),
            notes = o.optStringOrNull("notes"),
            reminderAt = o.optLongOrNull("reminderAt"),
            done = o.optBoolean("done"),
        )
    }

    private fun milestoneToJson(m: MilestoneEntity): JSONObject = JSONObject()
        .put("time", m.time)
        .put("kind", m.kind.name)
        .putOpt("detail", m.detail)
        .putOpt("notes", m.notes)

    private fun milestoneFromJson(o: JSONObject): MilestoneEntity? {
        val kind = enumOrNull<MilestoneKind>(o.optString("kind")) ?: return null
        return MilestoneEntity(
            babyId = 0,
            time = o.getLong("time"),
            kind = kind,
            detail = o.optStringOrNull("detail"),
            notes = o.optStringOrNull("notes"),
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        enumValues<T>().firstOrNull { it.name == name }

    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
