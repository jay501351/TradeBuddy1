package com.project.tradebuddy
import androidx.room.*

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist")
    suspend fun getAll(): List<WatchlistEntity>

    @Insert
    suspend fun insert(stock: WatchlistEntity)

    @Delete
    suspend fun delete(stock: WatchlistEntity)

}