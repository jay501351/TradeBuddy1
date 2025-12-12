package com.project.tradebuddy

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WatchlistManager
 *
 * - Local persistence: SharedPreferences (existing behaviour preserved)
 * - Optional remote sync: Firestore under collection: users/{uid}/watchlists (single document "data")
 *
 * Usage:
 *  - Call setFirestoreUser(userId) when user signs in (pass null to disable).
 *  - Call uploadAllToFirestore() / downloadFromFirestore() to sync manually.
 *
 * Notes:
 *  - Firestore sync is optional and best-effort. Network errors are reported via callbacks.
 *  - By default downloadFromFirestore() will overwrite local watchlists (cloud wins).
 *    If you want different merge logic, modify mergeDownloadedMap().
 */
object WatchlistManager {
    private const val PREF_NAME = "watchlist_prefs"
    private const val KEY_WATCHLISTS = "watchlists_map"      // Map<String, List<StockSearchItem>>
    private const val KEY_CURRENT = "watchlist_current_name"

    private val gson = Gson()
    private val TAG = "WatchlistManager"

    // Firestore
    private var firestore: FirebaseFirestore? = null
    private var firestoreUserId: String? = null
    private const val FIRESTORE_COLLECTION = "users"
    private const val FIRESTORE_DOC = "watchlists" // document id under users/{uid}

    // --- Public API (unchanged signatures) ---

    // Get all watchlist names (ordered by insertion)
    fun getAllWatchlistNames(context: Context): List<String> {
        val map = loadMap(context)
        return map.keys.toList()
    }

    // Create a new watchlist (returns true if created, false if name exists)
    fun createWatchlist(context: Context, name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isEmpty()) return false
        val map = loadMap(context).toMutableMap()
        if (map.containsKey(normalized)) return false
        map[normalized] = emptyList()
        saveMap(context, map)
        // If there was no current watchlist, set this as current
        val current = getCurrentWatchlistName(context)
        if (current == null) setCurrentWatchlistName(context, normalized)
        return true
    }

    // Get the list by name (or empty list if not found)
    fun getWatchlistByName(context: Context, name: String): List<StockSearchItem> {
        val map = loadMap(context)
        return map[name] ?: emptyList()
    }

    // Add a stock to a named list (avoids duplicates). Returns true if added.
    fun addStockToList(context: Context, listName: String, stock: StockSearchItem): Boolean {
        val map = loadMap(context).toMutableMap()
        val list = map[listName]?.toMutableList() ?: mutableListOf()
        if (list.any { it.symbol == stock.symbol }) return false
        list.add(stock)
        map[listName] = list
        saveMap(context, map)
        return true
    }

    // Remove a stock from a named list
    fun removeStockFromList(context: Context, listName: String, symbol: String) {
        val map = loadMap(context).toMutableMap()
        val list = map[listName]?.toMutableList() ?: return
        list.removeAll { it.symbol == symbol }
        map[listName] = list
        saveMap(context, map)
    }

    // Get or create a default watchlist name, and return its list
    fun getWatchlist(context: Context): List<StockSearchItem> {
        val cur = getCurrentWatchlistName(context) ?: run {
            // create default if none
            createWatchlist(context, "Default")
            setCurrentWatchlistName(context, "Default")
            "Default"
        }
        return getWatchlistByName(context, cur)
    }

    // Set/get current watchlist name
    fun setCurrentWatchlistName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT, name).apply()
    }

    fun getCurrentWatchlistName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT, null)
    }

    // Delete an entire watchlist (if you want)
    fun deleteWatchlist(context: Context, name: String) {
        val map = loadMap(context).toMutableMap()
        if (!map.containsKey(name)) return
        map.remove(name)
        saveMap(context, map)
        val current = getCurrentWatchlistName(context)
        if (current == name) {
            // pick another or null
            val newCurrent = map.keys.firstOrNull()
            if (newCurrent != null) setCurrentWatchlistName(context, newCurrent) else {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefs.edit().remove(KEY_CURRENT).apply()
            }
        }
    }

    // --- Firestore helpers / API ---

    /**
     * Enable Firestore sync for a particular user id. Pass null to disable.
     * Call this after successful authentication (e.g. FirebaseAuth user.uid).
     */
    fun setFirestoreUser(userId: String?) {
        firestoreUserId = userId
        if (!userId.isNullOrEmpty()) {
            firestore = FirebaseFirestore.getInstance()
        } else {
            firestore = null
        }
    }

    fun isFirestoreEnabled(): Boolean = firestore != null && !firestoreUserId.isNullOrEmpty()

    /**
     * Upload the current local watchlists map to Firestore (overwrites remote doc).
     * Callback: onComplete(true, null) on success, onComplete(false, exceptionMessage) on failure.
     */
    fun uploadAllToFirestore(context: Context, onComplete: (Boolean, String?) -> Unit) {
        if (!isFirestoreEnabled()) {
            onComplete(false, "Firestore not enabled")
            return
        }

        val map = loadMap(context)
        val docRef = firestore!!
            .collection(FIRESTORE_COLLECTION)
            .document(firestoreUserId!!)
            .collection("sync").document(FIRESTORE_DOC)

        val payload = mapToSerializable(map)

        docRef.set(payload, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Uploaded watchlists to Firestore (user=$firestoreUserId)")
                onComplete(true, null)
            }
            .addOnFailureListener { ex ->
                Log.w(TAG, "Failed uploading watchlists: ${ex.message}", ex)
                onComplete(false, ex.message)
            }
    }

    /**
     * Download watchlists from Firestore and overwrite local storage.
     * onComplete(true, null) on success, otherwise onComplete(false, message).
     *
     * NOTE: This uses cloud -> local overwrite by default. If you want to merge, change mergeDownloadedMap().
     */
    fun downloadFromFirestore(context: Context, onComplete: (Boolean, String?) -> Unit) {
        if (!isFirestoreEnabled()) {
            onComplete(false, "Firestore not enabled")
            return
        }

        val docRef = firestore!!
            .collection(FIRESTORE_COLLECTION)
            .document(firestoreUserId!!)
            .collection("sync").document(FIRESTORE_DOC)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Log.d(TAG, "No remote watchlist document found for user=$firestoreUserId")
                    onComplete(true, null) // nothing to do
                    return@addOnSuccessListener
                }

                val remoteMap = snapshot.data
                if (remoteMap == null) {
                    onComplete(true, null)
                    return@addOnSuccessListener
                }

                // convert remoteMap back to Map<String, List<StockSearchItem>>
                val parsed = serializableToMap(remoteMap)
                if (parsed != null) {
                    // backup local first (in memory) — you can extend to write a local backup file
                    val localBackup = loadMap(context)

                    // default behavior — overwrite local with remote
                    saveMap(context, parsed)

                    // keep current watchlist if it exists otherwise set to first remote or Default
                    val current = getCurrentWatchlistName(context)
                    if (current == null) {
                        val first = parsed.keys.firstOrNull() ?: "Default"
                        setCurrentWatchlistName(context, first)
                    }

                    Log.d(TAG, "Downloaded and replaced local watchlists from Firestore")
                    onComplete(true, null)
                } else {
                    Log.w(TAG, "Failed to parse remote watchlists document into map")
                    onComplete(false, "Failed to parse remote watchlists")
                }
            }
            .addOnFailureListener { ex ->
                Log.w(TAG, "Failed downloading watchlists: ${ex.message}", ex)
                onComplete(false, ex.message)
            }
    }

    /**
     * Attempt a two-way sync:
     * - Download remote
     * - If remote exists, mergeRemoteWins() applies (remote overwrites or merges as implemented)
     * - Otherwise upload local
     *
     * onComplete callback mirrors others.
     */
    fun syncWithFirestore(context: Context, onComplete: (Boolean, String?) -> Unit) {
        if (!isFirestoreEnabled()) {
            onComplete(false, "Firestore not enabled")
            return
        }

        val docRef = firestore!!
            .collection(FIRESTORE_COLLECTION)
            .document(firestoreUserId!!)
            .collection("sync").document(FIRESTORE_DOC)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    // no remote -> upload local
                    uploadAllToFirestore(context, onComplete)
                    return@addOnSuccessListener
                }

                val remoteMap = snapshot.data
                val parsed = serializableToMap(remoteMap)
                if (parsed != null) {
                    // For now: remote wins (overwrite local). Adjust if you need merge logic.
                    saveMap(context, parsed)
                    onComplete(true, null)
                } else {
                    onComplete(false, "Failed parsing remote data")
                }
            }
            .addOnFailureListener { ex ->
                Log.w(TAG, "Sync failed reading remote: ${ex.message}", ex)
                onComplete(false, ex.message)
            }
    }

    // --- Internal persistence helpers (unchanged) ---

    private fun loadMap(context: Context): Map<String, List<StockSearchItem>> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_WATCHLISTS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, List<StockSearchItem>>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing watchlists JSON: ${e.message}", e)
            emptyMap()
        }
    }

    private fun saveMap(context: Context, map: Map<String, List<StockSearchItem>>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(map)
        prefs.edit().putString(KEY_WATCHLISTS, json).apply()
    }

    // --- Converters for Firestore-friendly payloads ---
    // Firestore doesn't know StockSearchItem directly; we convert to Map<String, Any>

    private fun stockToMap(s: StockSearchItem): Map<String, Any> {
        return mapOf(
            "symbol" to (s.symbol ?: ""),
            "instrument_name" to (s.instrument_name ?: ""),
            "exchange" to (s.exchange ?: ""),
            "country" to (s.country ?: ""),
            "currency" to (s.currency ?: "")
        )
    }

    private fun mapToStock(m: Map<*, *>?): StockSearchItem? {
        if (m == null) return null
        val symbol = m["symbol"] as? String ?: return null
        val name = (m["instrument_name"] as? String) ?: ""
        val exchange = (m["exchange"] as? String) ?: ""
        val country = (m["country"] as? String) ?: ""
        val currency = (m["currency"] as? String) ?: ""
        return StockSearchItem(symbol, name, exchange, country, currency)
    }

    // Convert full map -> serializable Firestore map
    private fun mapToSerializable(map: Map<String, List<StockSearchItem>>): Map<String, Any> {
        val out = mutableMapOf<String, Any>()
        for ((k, list) in map) {
            out[k] = list.map { stockToMap(it) }
        }
        // Also include a "meta" field if you want timestamps later
        return out
    }

    // Convert Firestore data -> typed map
    @Suppress("UNCHECKED_CAST")
    private fun serializableToMap(data: Map<String, Any>?): Map<String, List<StockSearchItem>>? {
        if (data == null) return null
        val out = mutableMapOf<String, List<StockSearchItem>>()
        for ((k, v) in data) {
            if (k == "_meta") continue // ignore meta if present
            when (v) {
                is List<*> -> {
                    val items = mutableListOf<StockSearchItem>()
                    for (elem in v) {
                        if (elem is Map<*, *>) {
                            val s = mapToStock(elem)
                            if (s != null) items.add(s)
                        }
                    }
                    out[k] = items
                }
                is Map<*, *> -> {
                    // If single object, try parse single stock
                    val s = mapToStock(v)
                    if (s != null) out[k] = listOf(s)
                }
                else -> {
                    // ignore unexpected types
                }
            }
        }
        return out
    }
}
