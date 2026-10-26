package com.kbretro.babyregistry.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2: campos de salud en eventos y tablas de crecimiento y citas. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `events` ADD COLUMN `temperatureC` REAL")
        db.execSQL("ALTER TABLE `events` ADD COLUMN `medicineName` TEXT")
        db.execSQL("ALTER TABLE `events` ADD COLUMN `dose` TEXT")
        db.execSQL("ALTER TABLE `events` ADD COLUMN `reminderAt` INTEGER")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `growth` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`babyId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `weightKg` REAL, `lengthCm` REAL, " +
                "`headCm` REAL, `notes` TEXT, FOREIGN KEY(`babyId`) REFERENCES `babies`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_growth_babyId` ON `growth` (`babyId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `appointments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`babyId` INTEGER NOT NULL, `time` INTEGER NOT NULL, `kind` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`notes` TEXT, `reminderAt` INTEGER, `done` INTEGER NOT NULL, FOREIGN KEY(`babyId`) " +
                "REFERENCES `babies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointments_babyId` ON `appointments` (`babyId`)")
    }
}

/** v2 → v3: tipo de lactancia y registros ocultos por bebé. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `babies` ADD COLUMN `feedingType` TEXT")
        db.execSQL("ALTER TABLE `babies` ADD COLUMN `hiddenTypes` TEXT")
    }
}

/** v3 → v4: hitos. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `milestones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`babyId` INTEGER NOT NULL, `time` INTEGER NOT NULL, `kind` TEXT NOT NULL, `detail` TEXT, " +
                "`notes` TEXT, FOREIGN KEY(`babyId`) REFERENCES `babies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_milestones_babyId` ON `milestones` (`babyId`)")
    }
}

/** v4 → v5: fotos del bebé y de los hitos. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `babies` ADD COLUMN `photoPath` TEXT")
        db.execSQL("ALTER TABLE `milestones` ADD COLUMN `photoPath` TEXT")
    }
}

/**
 * v5 → v6: preparación para sincronizar entre varios móviles. Cada registro recibe un identificador
 * global (uuid) y una fecha de modificación, y se añade el registro de borrados.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("babies", "events", "growth", "appointments", "milestones").forEach { table ->
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE `$table` SET `uuid` = lower(hex(randomblob(16)))")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `$table` SET `updatedAt` = CAST(strftime('%s', 'now') AS INTEGER) * 1000")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_uuid` ON `$table` (`uuid`)")
        }
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_deletions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`entity` TEXT NOT NULL, `uuid` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL)",
        )
    }
}
