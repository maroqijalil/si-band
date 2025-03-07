package com.machina.siband.model

import android.os.Parcelable
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.Parcelize

@Parcelize
data class LaporanBase(
  val lokasi: String,
  val email: String,
  val tanggal: String,
  val lastUpdated: String,
  val indexRuangan: Long,
  @field:JvmField
  val isSubmitted: Boolean
) : Parcelable {

  companion object {
    private const val TAG = "LaporanBase"

    fun DocumentSnapshot.toLaporanBase(): LaporanBase? {
      return try {
        val lokasi = getString("lokasi")!!
        val email = getString("email")!!
        val tanggal = getString("tanggal")!!
        val lastUpdated = getString("lastUpdated")!!
        val isSubmitted = getBoolean("isSubmitted")!!
        val indexRuangan = getLong("indexRuangan")!!
        Log.d(TAG, "Converted to LaporanBase")
        LaporanBase(lokasi, email, tanggal, lastUpdated, indexRuangan, isSubmitted)
      } catch (e: Exception) {
        Log.e(TAG, "Error converting snapshot to LaporanBase", e)
        FirebaseCrashlytics.getInstance().log("Error converting user profile")
        FirebaseCrashlytics.getInstance().setCustomKey("userId", id)
        FirebaseCrashlytics.getInstance().recordException(e)
        null
      }
    }
  }
}