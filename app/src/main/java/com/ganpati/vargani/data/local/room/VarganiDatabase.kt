package com.ganpati.vargani.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ganpati.vargani.data.local.room.dao.DonationDao
import com.ganpati.vargani.data.local.room.dao.ExpenseDao
import com.ganpati.vargani.data.local.room.dao.SettingsDao
import com.ganpati.vargani.data.local.room.dao.UserDao
import com.ganpati.vargani.data.local.room.entity.DonationEntity
import com.ganpati.vargani.data.local.room.entity.ExpenseEntity
import com.ganpati.vargani.data.local.room.entity.SettingsEntity
import com.ganpati.vargani.data.local.room.entity.UserEntity

/**
 * Single-source local database.
 * Future cloud sync can mirror these tables without changing domain models.
 */
@Database(
    entities = [
        DonationEntity::class,
        SettingsEntity::class,
        UserEntity::class,
        ExpenseEntity::class,
    ],
    version = 6,
    exportSchema = true
)
abstract class VarganiDatabase : RoomDatabase() {
    abstract fun donationDao(): DonationDao
    abstract fun settingsDao(): SettingsDao
    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN language_code TEXT NOT NULL DEFAULT 'en'"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        mobile TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_users_mobile ON users(mobile)"
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        amount REAL NOT NULL,
                        payment_mode TEXT NOT NULL,
                        paid_by TEXT NOT NULL,
                        date_epoch INTEGER NOT NULL,
                        time_epoch INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_title ON expenses(title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_category ON expenses(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_date_epoch ON expenses(date_epoch)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_amount ON expenses(amount)")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN upi_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE settings ADD COLUMN bank_name TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE settings ADD COLUMN account_number TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE settings ADD COLUMN ifsc TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE settings ADD COLUMN account_holder TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE settings ADD COLUMN qr_image_path TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN whatsapp_group_notify INTEGER NOT NULL DEFAULT 1",
                )
            }
        }
    }
}
