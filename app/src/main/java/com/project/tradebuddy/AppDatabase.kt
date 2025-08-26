package com.project.tradebuddy

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchlistItem::class], version = 1)
abstract class AppDatabase : RoomDatabase(){
    abstract fun watchlistDao(): WatchlistDao
}