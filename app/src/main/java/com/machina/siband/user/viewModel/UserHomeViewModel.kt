package com.machina.siband.user.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.machina.siband.user.model.Lantai
import com.machina.siband.user.model.Lantai.Companion.toLantai
import com.machina.siband.user.model.LaporanBase
import com.machina.siband.user.model.LaporanBase.Companion.toLaporanBase
import com.machina.siband.user.model.LaporanRuangan
import com.machina.siband.user.model.LaporanRuangan.Companion.toLaporanRuangan
import com.machina.siband.user.model.Ruangan
import com.machina.siband.user.repository.UserFirebaseStorageRepo
import com.machina.siband.user.repository.UserFirestoreRepo
import kotlinx.coroutines.*
class UserHomeViewModel: ViewModel() {

    // List of Lantai Object
    private val _listLantai = MutableLiveData<List<Lantai>>()
    val listLantai: LiveData<List<Lantai>> = _listLantai

    // Array of nama of each Lantai object, used to populate spinner
    private val _arrayListLantai = MutableLiveData<Array<String>>()
    var arrayListLantai: LiveData<Array<String>> = _arrayListLantai

    // Value of selectedLantai Object, used to populate recyclerAdapter
    private val _selectedLantai = MutableLiveData<Lantai>()
    val selectedLantai: LiveData<Lantai> = _selectedLantai

    // Value of selected Lantai position to keep spinner consistent
    private var _selectedPosition: Int = 0
    val selectedPosition get() = _selectedPosition

    // List of possible report of a ruangan
    private var _listLaporanRuangan = MutableLiveData<List<LaporanRuangan>>()
    val listLaporanRuangan: LiveData<List<LaporanRuangan>> = _listLaporanRuangan

    // List of submitted LaporanBase
    private var _listLaporanBase = MutableLiveData<List<LaporanBase>>()
    val listLaporanBase: LiveData<List<LaporanBase>> = _listLaporanBase

    private var _listLaporanNoProgressYet = MutableLiveData<List<LaporanRuangan>>()
    val listLaporanNoProgressYet: LiveData<List<LaporanRuangan>> = _listLaporanNoProgressYet

    private var _listLaporanOnProgress = MutableLiveData<List<LaporanRuangan>>()
    val listLaporanOnProgress: LiveData<List<LaporanRuangan>> = _listLaporanOnProgress

    private var _listLaporanDone = MutableLiveData<List<LaporanRuangan>>()
    val listLaporanDone: LiveData<List<LaporanRuangan>> = _listLaporanDone

    val imagesUri = mutableListOf<Uri>()

    val downloadImagesUri = mutableListOf<String>()

    // All listener to a firestore
    private lateinit var lantaiListener: ListenerRegistration

    init {
        attachLantaiListener()
    }

    private fun fetchListLaporanRuangan() {
        val tempMutable = mutableListOf<LaporanRuangan>()
        val laporanBase = listLaporanBase.value
        if (laporanBase.isNullOrEmpty()) return

        // Call data for each item in
        for (item in laporanBase) {
            UserFirestoreRepo.getListLaporanRuanganRef(item.email, item.tanggal, item.lokasi).get()
                .addOnSuccessListener { coll ->

                    var temp = coll.mapNotNull { it.toLaporanRuangan() }
                    temp = temp.filter { it.tipe.isNotEmpty() }
                    tempMutable.addAll(temp)
                    _listLaporanRuangan.value = tempMutable
                    viewModelScope.launch(Dispatchers.Default) {
                        filterListLaporan()
                    }
                }
                .addOnFailureListener { error ->
                    sendCrashlytics("Failed to fetch LaporanRuangan", error)
                }
        }
    }

    private suspend fun filterListLaporan() {
        val listLaporan =  _listLaporanRuangan.value
        println(Thread.currentThread().name)
        if (listLaporan != null) {
            var temp = listLaporan.filter { it.status == NO_PROGRESS }
            setListLaporanNoProgressYet(temp)
//            for (item in temp) Log.d(TAG, "item : ${item.nama}, status : ${item.status}")

            temp = listLaporan.filter { it.status == ON_PROGRESS }
            setListLaporanOnProgress(temp)
//            for (item in temp) Log.d(TAG, "item : ${item.nama}, status : ${item.status}")

            temp = listLaporan.filter { it.status == DONE }
            setListLaporanDone(temp)
//            for (item in temp) Log.d(TAG, "item : ${item.nama}, status : ${item.status}")
        }
    }

    private suspend fun setListLaporanNoProgressYet(data: List<LaporanRuangan>) {
        withContext(Dispatchers.Main) {
            _listLaporanNoProgressYet.value = data
        }
    }

    private suspend fun setListLaporanOnProgress(data: List<LaporanRuangan>) {
        withContext(Dispatchers.Main) {
            _listLaporanOnProgress.value = data
        }
    }

    private suspend fun setListLaporanDone(data: List<LaporanRuangan>) {
        withContext(Dispatchers.Main) {
            _listLaporanDone.value = data
        }
    }



    fun getListLaporanBase(email: String) {
        UserFirestoreRepo.getUserListLaporanRuanganRef(email)
            .get()
            .addOnSuccessListener { snapShot ->
                val tempList = snapShot.documents.mapNotNull { it.toLaporanBase() }
                _listLaporanBase.value = tempList
                fetchListLaporanRuangan()
            }
            .addOnFailureListener {
                sendCrashlytics("Error when fetching ListLaporanBase", it)
            }
    }

    fun putFormPelaporan(laporanBase: LaporanBase, laporanRuangan: LaporanRuangan) {
        val baseReference = UserFirestoreRepo.getLaporanBaseRef(laporanBase.email, laporanBase.tanggal, laporanBase.lokasi)
        val laporanReference = UserFirestoreRepo.getLaporanRuanganRef(laporanBase.email, laporanRuangan.tanggal, laporanRuangan.lokasi, laporanRuangan.nama)

        baseReference.set(laporanBase)
            .addOnSuccessListener {
                laporanReference.set(laporanRuangan)
                    .addOnFailureListener { exception ->
                        sendCrashlytics("Cannot put LaporanRuangan with Pelaporan", exception)
                    }
            }
    }

    private fun putNewImage(laporanRuangan: LaporanRuangan, dokumentasi: List<Uri>) {
        val email = "admin@gmail.com"
        val tanggal = "29-04-2021"
        val lokasi = laporanRuangan.lokasi
        val nama = laporanRuangan.nama

        val laporanRef = UserFirestoreRepo.getLaporanRuanganRef(email, tanggal, lokasi, nama)

        dokumentasi.forEachIndexed { index, uri ->
            UserFirebaseStorageRepo.getLaporanImageRef(email, tanggal, lokasi, "${nama}${index}")
                .putFile(uri)
                .addOnSuccessListener {

                }

        }
    }



    fun putNewLaporanRuangan(laporanRuangan: LaporanRuangan,  dokumentasi: List<Uri>) {
        val email = "admin@gmail.com"
        val tanggal = "29-04-2021"
        val lokasi = laporanRuangan.lokasi
        val nama = laporanRuangan.nama

        UserFirestoreRepo.getLaporanRuanganRef(email, tanggal, lokasi, nama)
            .set(laporanRuangan)
            .addOnSuccessListener {
                putNewImage(laporanRuangan, dokumentasi)
            }
            .addOnFailureListener {
                exception -> sendCrashlytics("An error occured when trying to upload new laporan", exception)
            }
    }


    /**
     * Put the local change that has been made
     * to listKeluhanRuangan to the database
     *
     * @param email
     * @param tanggal date that this function is called
     * @param lokasi
     */
    fun putLaporanLantai(email: String, tanggal: String, lokasi: String) {
        val listRef = mutableListOf<DocumentReference>()
        val listTemp = listLaporanRuangan.value
        val baseLaporanRef = UserFirestoreRepo.getLaporanBaseRef(email, tanggal, lokasi)

        baseLaporanRef.update("isSubmitted", true)
//
//        if (listTemp != null) {
//            for (item in listTemp) {
//                val tempRef = UserFirestoreRepo.getLaporanRuanganRef(email, tanggal, lokasi, item.nama)
//                listRef.add(tempRef)
//            }
//
//            val db = Firebase.firestore
//            db.runBatch { batch ->
//                for ((counter, item) in listRef.withIndex()) {
//                    batch.set(item, listTemp[counter])
//                }
//            }
//            .addOnFailureListener { e ->
//                sendCrashlytics("An error occurred while batch write", e)
//            }
//        }
    }


    /**
     * Aplly change on Invididual Laporan Form
     * to a Local Storage before submitted
     *
     * @param nama name of the item in DetailRuangan
     * @param newTipe value of new Tipe (Sedang, Darurat)
     * @param newKeterangan value of new Keterangan
     */
//    fun applyLocalChangeLaporan(nama: String, newTipe: String, newKeterangan: String) {
//        viewModelScope.launch(Dispatchers.Default) {
//            mapListLaporanRuanganDefault(nama, newTipe, newKeterangan)
//        }
//    }
//
//    private suspend fun mapListLaporanRuanganDefault(nama: String, newTipe: String, newKeterangan: String) {
//        val newList = _listLaporanRuangan.value?.map {
//            if (it.nama == nama) {
//                if (it.status.isBlank()) {
//                    it.copy(tipe = newTipe, keterangan = newKeterangan, isChecked = true, status = "No Progress Yet")
//                } else {
//                    it.copy(tipe = newTipe, keterangan = newKeterangan, isChecked = true)
//                }
//            } else {
//                it.copy()
//            }
//        }
//
//        if (newList != null) {
//            changeListLaporanRuanganMain(newList)
//        }
//    }

    private suspend fun changeListLaporanRuanganMain(newList: List<LaporanRuangan>) {
        withContext(Dispatchers.Main) {
            _listLaporanRuangan.value = newList
        }
    }


    // Convert param into mutableList
    private fun convertToMutableList(arrayTemp: ArrayList<*>): MutableList<Ruangan> {
        val listTemp = mutableListOf<Ruangan>()
        if (arrayTemp[0] != null && arrayTemp[0] is String) {
            for (item in arrayTemp) {
                val ruangan = Ruangan(item as String, false)
                listTemp.add(ruangan)
            }
        }
        return listTemp
    }

    // this function only called one time in one day, when user open ListLaporan for the first time
    // Generate dummy laporan in appropriate path
    private fun setListLaporanRuangan(idLantai: String, email: String, tanggal: String, lokasi: String) {
        val laporanBaseRef = UserFirestoreRepo.getLaporanBaseRef(email, tanggal, lokasi)
        val laporanBase = LaporanBase(lokasi, email, tanggal,false)
        val listDetailRuanganRef = UserFirestoreRepo.getListDetailRuanganRef(idLantai, lokasi)

        laporanBaseRef.set(laporanBase)
                .addOnSuccessListener {
                    listDetailRuanganRef.get()
                            .addOnSuccessListener { docs ->
                                val arrayTemp = docs.get("list-keluhan")
                                if (arrayTemp != null) {
                                    val listTemp = convertToMutableList(arrayTemp as ArrayList<*>)
                                    for (item in listTemp)
                                        putLaporanRuangan(email, tanggal, lokasi, item.nama)
                                    getListLaporanRuangan(idLantai, email, tanggal, lokasi)
                                }
                            }
                            .addOnFailureListener {
                                sendCrashlytics("Failed to put LaporanRuangan", it)
                            }
                }
                .addOnFailureListener {
                    sendCrashlytics("Failed to put LaporanBase", it)
                }

    }

    /**
     * This method used to instantiate dummy laporan data on database
     * Only called once by setListLaporan()
     *
     * @param email an email of logged in user.
     * @param tanggal date of laporan.
     * @param lokasi location of the created laporan.
     * @param nama item name.
     * @return nothing.
     */
    private fun putLaporanRuangan(email: String, tanggal: String, lokasi: String, nama: String) {
        val emptyLaporan = LaporanRuangan(nama, nama, lokasi, tanggal)
        UserFirestoreRepo.getLaporanRuanganRef(email, tanggal, lokasi, nama)
                .set(emptyLaporan)
    }

    fun clearLaporanRuangan() {
        _listLaporanRuangan.value = mutableListOf()
        _listLaporanNoProgressYet.value = mutableListOf()
        _listLaporanOnProgress.value = mutableListOf()
        _listLaporanDone.value = mutableListOf()
    }

    // Get the list of report of some Ruangan at current date
    fun getListLaporanRuangan(idLantai: String, email: String, tanggal: String, lokasi: String) {
        UserFirestoreRepo.getListLaporanRuanganRef(email, tanggal, lokasi).get()
                .addOnSuccessListener { coll ->
                    if (coll.isEmpty) {
                        setListLaporanRuangan(idLantai, email, tanggal, lokasi)
                    } else {
                        _listLaporanRuangan.value = coll.mapNotNull { it.toLaporanRuangan() }
                    }
                }
                .addOnFailureListener { error ->
                    sendCrashlytics("Failed to fetch LaporanRuangan", error)
                }

    }

    // Change the selected value in spinner
    fun setSelectedLantai(newValue: Lantai, position: Int) {
        _selectedLantai.value = newValue
        _selectedPosition = position
    }

    // Attach snapshotListener to _listLantai
    private fun attachLantaiListener() {
        lantaiListener = UserFirestoreRepo
            .getListLantaiRef()
            .addSnapshotListener { value, error ->
                if (error != null) sendCrashlytics("error fetching list lantai", error)

                val lantaiTemp: MutableList<Lantai> = mutableListOf()
                for (item in value!!) {
                    val lantai = item.toLantai()
                    if (lantai != null) lantaiTemp.add(lantai)
                }

                _listLantai.value = lantaiTemp

                populateArrayListLantai(lantaiTemp)
        }
    }

    private fun populateArrayListLantai(mutableList: MutableList<Lantai>) {
        val tempList = mutableListOf<String>()
        for (item in mutableList)
            tempList.add(item.nama)

        _arrayListLantai.value = tempList.toTypedArray()
    }

    private fun sendCrashlytics(message: String, error: Exception) {
        Log.e(TAG, message, error)
        FirebaseCrashlytics.getInstance().log(message)
        FirebaseCrashlytics.getInstance().recordException(error)
    }

    override fun onCleared() {
        super.onCleared()
        lantaiListener.remove()
    }
    companion object {
        private const val TAG = "UserHome"
        const val NO_PROGRESS = "No Progress Yet"
        const val ON_PROGRESS = "On Progress"
        const val DONE = "Done"
    }
}