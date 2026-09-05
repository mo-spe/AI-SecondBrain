package com.secondbrain.android.capture

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A local-only capture draft exists to make confirmation explicit and recoverable offline. */
@Entity(tableName = "ocr_drafts")
data class OcrDraft(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val imageUri: String,
    val workspaceId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
