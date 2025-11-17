package com.project.tradebuddy

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class watchlistRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"

    private fun userWatchlistRef() =
        firestore.collection("users").document(userId).collection("watchlist")

    suspend fun getWatchlist(): List<Stock> {
        val snapshot = userWatchlistRef().get().await()
        return snapshot.documents.mapNotNull { it.toObject(Stock::class.java) }
    }

    suspend fun addStock(stock:Stock){
        userWatchlistRef().document(stock.symbol).set(stock).await()
    }

    suspend fun deleteStock(symbol: String){
        userWatchlistRef().document(symbol).delete().await()
    }
}