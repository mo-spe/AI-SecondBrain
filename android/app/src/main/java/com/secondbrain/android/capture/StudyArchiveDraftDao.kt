package com.secondbrain.android.capture

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyArchiveDraftDao {
    @Query("SELECT * FROM study_archive_drafts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StudyArchiveDraft>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(draft: StudyArchiveDraft): Long

    @Query("DELETE FROM study_archive_drafts WHERE id = :id")
    suspend fun delete(id: Long)
}
