package com.noureldin.dscan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.noureldin.dscan.data.local.converters.DateTypeConverter
import com.noureldin.dscan.data.local.dao.PdfDao
import com.noureldin.dscan.data.models.PdfEntity

@Database(entities = [PdfEntity::class], version = 1, exportSchema = false)
@TypeConverters(DateTypeConverter::class)
abstract class PdfDatabase: RoomDatabase() {
    abstract val pdfDao: PdfDao

    companion object{
        @Volatile
        private var INSTANCES : PdfDatabase? = null

        fun getInstance(context: Context): PdfDatabase{
            synchronized(this){
                return INSTANCES ?: Room.databaseBuilder(
                    context.applicationContext,
                    PdfDatabase::class.java,
                    "pdf_db"
                ).build().also {
                    INSTANCES = it
                }
            }
        }
    }
}