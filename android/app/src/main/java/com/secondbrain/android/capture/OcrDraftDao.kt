package com.secondbrain.android.capture

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrDraftDao {
    @Query("SELECT * FROM ocr_drafts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OcrDraft>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(draft: OcrDraft): Long

    @Query("DELETE FROM ocr_drafts WHERE id = :draftId")
    suspend fun delete(draftId: Long)
}
