package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        TaskItemEntity::class,
        ShoppingItemEntity::class,
        ExpenseEntity::class,
        OwnerAccountEntity::class,
        GoalEntity::class,
        ProjectEntity::class,
        RoutineEntity::class,
        ActionRecordEntity::class,
        MemoryItemEntity::class,
        FamilyMemberEntity::class,
        TimelineEventEntity::class,
        LearnedRoutineEntity::class,
        DailyReflectionEntity::class,
        WeeklyPatternEntity::class,
        LearningSettingEntity::class,
        LifeVaultItemEntity::class,
        PersistentIntentionEntity::class,
        PersistenceLedgerEntity::class,
        ObservationEntity::class,
        CandidatePatternEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun taskDao(): TaskDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun ownerAccountDao(): OwnerAccountDao
    abstract fun goalDao(): GoalDao
    abstract fun projectDao(): ProjectDao
    abstract fun routineDao(): RoutineDao
    abstract fun actionRecordDao(): ActionRecordDao
    abstract fun memoryDao(): MemoryDao
    abstract fun familyDao(): FamilyDao
    abstract fun timelineDao(): TimelineDao
    abstract fun learnedRoutineDao(): LearnedRoutineDao
    abstract fun dailyReflectionDao(): DailyReflectionDao
    abstract fun weeklyPatternDao(): WeeklyPatternDao
    abstract fun learningSettingDao(): LearningSettingDao
    abstract fun lifeVaultDao(): LifeVaultDao
    abstract fun persistentIntentionDao(): PersistentIntentionDao
    abstract fun persistenceLedgerDao(): PersistenceLedgerDao
    abstract fun observationDao(): ObservationDao
    abstract fun candidatePatternDao(): CandidatePatternDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_TO_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `life_vault_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL, `source` TEXT NOT NULL, `sourceDetail` TEXT, `numericValue` REAL, `isConfirmed` INTEGER NOT NULL, `isPendingConflict` INTEGER NOT NULL, `conflictingValue` TEXT, `previousValue` TEXT, `timestamp` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `persistent_intentions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `intentionId` TEXT NOT NULL, `title` TEXT NOT NULL, `targetAction` TEXT NOT NULL, `scheduledEpochMs` INTEGER NOT NULL, `recurrenceRule` TEXT NOT NULL, `state` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `persistence_ledger` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `intentionId` TEXT NOT NULL, `actionSummary` TEXT NOT NULL, `status` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `details` TEXT NOT NULL)")
            }
        }

        val MIGRATION_7_TO_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `observations` (`observationId` TEXT PRIMARY KEY NOT NULL, `ownerId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `timezone` TEXT NOT NULL, `dateString` TEXT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `source` TEXT NOT NULL, `domain` TEXT NOT NULL, `subject` TEXT NOT NULL, `eventType` TEXT NOT NULL, `action` TEXT NOT NULL, `context` TEXT NOT NULL, `entitiesJson` TEXT NOT NULL, `location` TEXT NOT NULL, `relatedRoutine` TEXT, `relatedPerson` TEXT, `confidence` REAL NOT NULL, `truthState` TEXT NOT NULL, `observedValue` TEXT NOT NULL, `metadataJson` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `candidate_patterns` (`patternId` TEXT PRIMARY KEY NOT NULL, `ownerId` TEXT NOT NULL, `domain` TEXT NOT NULL, `subject` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `evidenceCount` INTEGER NOT NULL, `supportingObservationIdsJson` TEXT NOT NULL, `contradictoryObservationIdsJson` TEXT NOT NULL, `confidence` REAL NOT NULL, `firstObserved` INTEGER NOT NULL, `lastObserved` INTEGER NOT NULL, `recurrence` TEXT NOT NULL, `expectedDaysJson` TEXT NOT NULL, `expectedTimeRange` TEXT NOT NULL, `expectedContext` TEXT NOT NULL, `exceptionsJson` TEXT NOT NULL, `status` TEXT NOT NULL, `explanation` TEXT NOT NULL, `truthState` TEXT NOT NULL, `isUserConfirmed` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vyb_database"
                )
                    .addMigrations(MIGRATION_6_TO_7, MIGRATION_7_TO_8)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
