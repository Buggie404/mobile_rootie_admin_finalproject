package com.veganbeauty.admin.data.repository

import androidx.lifecycle.LiveData
import com.veganbeauty.admin.data.local.dao.CustomerDao
import com.veganbeauty.admin.data.local.entities.CustomerEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val firebaseService: FirebaseService
) {

    val allCustomers: LiveData<List<CustomerEntity>> = customerDao.getAllLiveData()

    suspend fun syncFromFirebase() = withContext(Dispatchers.IO) {
        val remoteList = firebaseService.fetchAllCustomers()
        if (remoteList.isNotEmpty()) {
            customerDao.insertAllSync(remoteList)
        }
    }
}
