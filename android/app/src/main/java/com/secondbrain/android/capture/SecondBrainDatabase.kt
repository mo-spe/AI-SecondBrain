package com.secondbrain.android.capture

import androidx.room.Database
import androidx.room.RoomDatabase

/** Keeps drafts on-device because OCR text must remain editable until the user confirms saving. */
@Database(entities = [OcrDraft::class], version = 1, exportSchema = false)
abstract class SecondBrainDatabase : RoomDatabase() {
    abstract fun ocrDraftDao(): OcrDraftDao
}
