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
        LifeVaultItemEntity::class
    ],
    version = 6,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vyb_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
