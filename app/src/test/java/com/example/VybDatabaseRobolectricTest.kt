package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.model.IntentType
import com.example.data.model.VybIntentPayload
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VybDatabaseRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testConversationAndMessageFlow() = runBlocking {
        val convDao = database.conversationDao()
        val msgDao = database.messageDao()

        val convId = convDao.insertConversation(
            ConversationEntity(
                title = "Trip to Tokyo",
                preview = "3-day itinerary",
                intentType = IntentType.TRAVEL.name
            )
        )

        msgDao.insertMessage(
            MessageEntity(
                conversationId = convId,
                sender = "USER",
                content = "Plan a 3-day trip to Tokyo"
            )
        )

        msgDao.insertMessage(
            MessageEntity(
                conversationId = convId,
                sender = "ASSISTANT",
                content = "Here is your 3-day Tokyo itinerary"
            )
        )

        val messages = msgDao.getMessagesForConversation(convId).first()
        assertEquals(2, messages.size)
        assertEquals("USER", messages[0].sender)
        assertEquals("ASSISTANT", messages[1].sender)
    }

    @Test
    fun testTaskAndShoppingOperations() = runBlocking {
        val taskDao = database.taskDao()
        val shoppingDao = database.shoppingDao()

        val taskId = taskDao.insertTask(
            TaskItemEntity(
                title = "Buy flight tickets",
                priority = "HIGH",
                category = "Travel",
                dueDate = "Tomorrow"
            )
        )

        val shoppingId = shoppingDao.insertShoppingItem(
            ShoppingItemEntity(
                name = "Avocados",
                category = "Produce",
                quantity = "3",
                estimatedCost = 4.50
            )
        )

        var allTasks = taskDao.getAllTasks().first()
        assertEquals(1, allTasks.size)
        assertFalse(allTasks[0].isCompleted)

        taskDao.setTaskCompleted(taskId, true)
        allTasks = taskDao.getAllTasks().first()
        assertTrue(allTasks[0].isCompleted)

        val shoppingList = shoppingDao.getAllShoppingItems().first()
        assertEquals(1, shoppingList.size)
        assertEquals("Avocados", shoppingList[0].name)
    }

    @Test
    fun testIntentPayloadSerialization() {
        val payload = VybIntentPayload(
            intentType = "DIET",
            title = "Keto Nutrition Plan",
            summary = "2000 kcal high-protein keto plan",
            suggestedFollowUps = listOf("Create grocery shopping list", "Modify for vegan")
        )

        val adapter = GeminiClient.moshi.adapter(VybIntentPayload::class.java)
        val json = adapter.toJson(payload)
        assertNotNull(json)

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals("DIET", deserialized?.intentType)
        assertEquals("Keto Nutrition Plan", deserialized?.title)
        assertEquals(2, deserialized?.suggestedFollowUps?.size)
    }
}
