package com.secondbrain.android.capture

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local archive drafts preserve captured study material until the server confirms the upload. */
@Entity(tableName = "study_archive_drafts")
data class StudyArchiveDraft(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val archiveType: String,
    val imagePath: String,
    val ocrText: String = "",
    val userAnswer: String = "",
    val correctAnswer: String = "",
    val explanation: String = "",
    val content: String = "",
    val subject: String = "考研数学",
    val sourceBook: String = "",
    val sourcePage: String = "",
    val chapter: String = "",
    val errorType: String = "不确定",
    val doubtType: String = "概念理解",
    val knowledgePoints: String = "",
    val userNote: String = "",
    val aiRaw: String = "",
    val aiConfidence: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
