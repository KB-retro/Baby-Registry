package com.kbretro.babyregistry.data.sync

/** Persona de la familia con acceso a los datos (madre, padre, abuela, cuidadora…). */
data class FamilyMember(val id: String, val name: String, val email: String?)

/** Grupo que comparte bebés y registros. Se une a él con un código de invitación. */
data class Family(val id: String, val name: String, val inviteCode: String, val members: List<FamilyMember>)

/**
 * Servidor de sincronización. La implementación prevista es Firebase (Auth con Google + Firestore),
 * que se añadirá cuando el proyecto de Firebase esté creado (ver docs/SINCRONIZACION.md).
 * La app funciona igual sin servidor: todo se guarda en el móvil.
 */
interface SyncBackend {
    /** false mientras no haya un proyecto de Firebase configurado. */
    val isAvailable: Boolean

    suspend fun signIn(): FamilyMember?
    suspend fun signOut()

    suspend fun currentFamily(): Family?
    suspend fun createFamily(name: String): Family
    suspend fun joinFamily(inviteCode: String): Family
    suspend fun leaveFamily()

    /** Sube los cambios locales. */
    suspend fun push(changes: ChangeSet)

    /** Descarga los cambios de los demás desde [since] (epoch millis). */
    suspend fun pull(since: Long): ChangeSet
}

/** Implementación vacía hasta que haya servidor. */
class NoSyncBackend : SyncBackend {
    override val isAvailable = false
    override suspend fun signIn(): FamilyMember? = null
    override suspend fun signOut() = Unit
    override suspend fun currentFamily(): Family? = null
    override suspend fun createFamily(name: String): Family = error("Sincronización no configurada")
    override suspend fun joinFamily(inviteCode: String): Family = error("Sincronización no configurada")
    override suspend fun leaveFamily() = Unit
    override suspend fun push(changes: ChangeSet) = Unit
    override suspend fun pull(since: Long) = ChangeSet()
}
