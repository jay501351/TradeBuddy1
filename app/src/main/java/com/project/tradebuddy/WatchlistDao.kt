package com.project.tradebuddy
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist")
    suspend fun getAll(): LiveData<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun Insert(item: WatchlistItem)

    @Delete
    suspend fun delete(item: WatchlistItem)
}