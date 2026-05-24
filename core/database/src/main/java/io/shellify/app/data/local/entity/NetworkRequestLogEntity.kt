package io.shellify.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "network_request_logs",
    foreignKeys = [
        ForeignKey(
            entity = WebAppEntity::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("app_id"), Index("session_id"), Index("timestamp")],
)
data class NetworkRequestLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "app_id") val appId: Long,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val hostname: String,
    val url: String,
    @ColumnInfo(name = "is_blocked") val isBlocked: Boolean,
    val timestamp: Long,
)
