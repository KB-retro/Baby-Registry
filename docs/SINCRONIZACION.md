# Sincronización entre cuidadores

Objetivo: que varios cuidadores (madre, padre, abuelos…) compartan los mismos bebés y registros en
tiempo real, cada uno con su móvil y su cuenta de Google. La app sigue funcionando sin conexión:
los cambios se guardan en el móvil y se envían al recuperar la conexión.

## Qué está hecho (base de datos v6)

- Cada registro (bebé, evento, medida, cita, hito) tiene un **`uuid`** global y un **`updatedAt`**.
  Los `id` numéricos son locales de cada móvil y no se envían.
- Toda modificación local actualiza `updatedAt` (`TrackerRepository`) y todo borrado queda en la
  tabla **`sync_deletions`**, para propagarlo.
- `SyncRepository.localChanges(since)` reúne lo cambiado desde la última sincronización y
  `SyncRepository.applyRemote(changes)` aplica lo recibido con la regla **gana la modificación más
  reciente** (un borrado solo gana si es posterior a la última edición). Probado en
  `SyncRepositoryTest` simulando dos móviles.
- `SyncBackend` define lo que debe ofrecer el servidor; `NoSyncBackend` es la implementación vacía
  mientras no haya proyecto de Firebase.

## Qué falta (necesita el proyecto de Firebase)

1. Crear el proyecto en https://console.firebase.google.com y añadir la app Android
   `com.kbretro.babyregistry` con el SHA-1 de la clave de depuración
   (`58:84:92:B5:D2:03:70:F4:69:E8:D9:C9:28:28:27:9B:15:E8:1A:4A`) y, al publicar, el de la clave de subida.
2. Activar **Authentication → Google** y **Firestore** (ubicación `eur3`).
3. Añadir `app/google-services.json` al repositorio y el plugin `com.google.gms.google-services`.
4. Implementar `FirebaseSyncBackend` (Auth + Firestore) y un `SyncWorker` periódico, más la
   pantalla "Familia" (crear familia, código de invitación, unirse, miembros).
5. Publicar las reglas de `firestore.rules`.

## Estructura en Firestore

```
families/{familyId}
    name, inviteCode, members: [uid…], createdAt
families/{familyId}/babies/{babyUuid}
families/{familyId}/events/{uuid}         + babyUuid
families/{familyId}/growth/{uuid}         + babyUuid
families/{familyId}/appointments/{uuid}   + babyUuid
families/{familyId}/milestones/{uuid}     + babyUuid
families/{familyId}/deletions/{uuid}      entity, deletedAt
invites/{inviteCode}                      familyId (para unirse con el código)
```

Cada documento lleva `updatedAt`; para descargar cambios se consulta `updatedAt > última sincronización`.
Las fotos irán a Firebase Storage en `families/{familyId}/photos/{uuid}.jpg`.
