package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LocalUserEntity::class,
        StoreSettingsEntity::class,
        ProductEntity::class,
        MerchantEntity::class,
        SupplierEntity::class,
        EmployeeEntity::class,
        JobTypeEntity::class,
        PayslipEntity::class,
        CashBoxEntity::class,
        CashFlowEntity::class,
        SaleEntity::class,
        PurchaseEntity::class,
        PurchaseReturnEntity::class,
        ExpenseEntity::class,
        DebtRecordEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AfiDatabase : RoomDatabase() {
    abstract fun afiDao(): AfiDao

    companion object {
        @Volatile
        private var INSTANCE: AfiDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payslips ADD COLUMN workDateMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE payslips SET workDateMillis = salaryDateMillis WHERE workDateMillis = 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debt_records ADD COLUMN paidAtMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `suppliers` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `suppliedItems` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payslips ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("UPDATE payslips SET paidAmount = totalAmount WHERE paid = 1 AND paidAmount = 0")
            }
        }

        fun getInstance(context: Context): AfiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AfiDatabase::class.java,
                    "afi_business_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
