package com.example.myapplication.data

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class AdminNotificationItem(
    val id: String,
    val category: String,
    val title: String,
    val body: String,
    val chipText: String,
    val timeAgo: String,
    val isUnread: Boolean = true,
    val timestampMs: Long = System.currentTimeMillis()
)

object AdminNotificationRepository {

    private const val COLLECTION = "adminNotifications"

    private val initialNotifications = listOf(
        AdminNotificationItem(
            id = "notif_1",
            category = "APPLICATIONS",
            title = "New Facility Application Submitted",
            body = "Area 4 Express Wash submitted documentation and specifications for review.",
            chipText = "Lilongwe",
            timeAgo = "10 mins ago"
        ),
        AdminNotificationItem(
            id = "notif_2",
            category = "REGISTRATIONS",
            title = "New Car Wash Owner Registered",
            body = "Kondwani Phiri registered as a Car Wash Owner.",
            chipText = "Verified User",
            timeAgo = "1 hour ago"
        )
    )

    private val notifications = mutableStateListOf<AdminNotificationItem>().apply { addAll(initialNotifications) }

    fun getAll(): List<AdminNotificationItem> = notifications.sortedByDescending { it.timestampMs }

    fun unreadCount(): Int = notifications.count { it.isUnread }

    fun markAllRead() {
        for (i in notifications.indices) {
            notifications[i] = notifications[i].copy(isUnread = false)
        }
    }

    suspend fun createNotification(
        category: String,
        title: String,
        body: String,
        chipText: String = "Platform Alert"
    ): Result<AdminNotificationItem> = withContext(Dispatchers.IO) {
        val id = "notif_${UUID.randomUUID()}"
        val item = AdminNotificationItem(
            id = id,
            category = category,
            title = title,
            body = body,
            chipText = chipText,
            timeAgo = "Just now",
            isUnread = true,
            timestampMs = System.currentTimeMillis()
        )

        try {
            val docMap = hashMapOf<String, Any>(
                "id" to item.id,
                "category" to item.category,
                "title" to item.title,
                "body" to item.body,
                "chipText" to item.chipText,
                "isUnread" to item.isUnread,
                "timestampMs" to item.timestampMs
            )
            FirebaseServices.db.collection(COLLECTION).document(id).set(docMap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        notifications.add(0, item)
        Result.success(item)
    }

    suspend fun fetchNotificationsFromFirestore(): Result<List<AdminNotificationItem>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = FirebaseServices.db.collection(COLLECTION).get().await()
            val fetched = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val category = doc.getString("category") ?: "SYSTEM"
                val title = doc.getString("title") ?: "System Alert"
                val body = doc.getString("body") ?: ""
                val chipText = doc.getString("chipText") ?: "Alert"
                val isUnread = doc.getBoolean("isUnread") ?: true
                val timestampMs = doc.getLong("timestampMs") ?: System.currentTimeMillis()

                AdminNotificationItem(
                    id = id,
                    category = category,
                    title = title,
                    body = body,
                    chipText = chipText,
                    timeAgo = "Recent",
                    isUnread = isUnread,
                    timestampMs = timestampMs
                )
            }

            if (fetched.isNotEmpty()) {
                notifications.clear()
                notifications.addAll(fetched.sortedByDescending { it.timestampMs })
            }

            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
