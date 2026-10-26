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
