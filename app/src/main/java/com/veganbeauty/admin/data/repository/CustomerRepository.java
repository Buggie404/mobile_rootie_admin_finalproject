package com.veganbeauty.admin.data.repository;

import androidx.lifecycle.LiveData;
import com.veganbeauty.admin.data.local.dao.CustomerDao;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;

import java.util.List;

public class CustomerRepository {
    private final CustomerDao customerDao;
    private final FirebaseService firebaseService;

    public CustomerRepository(CustomerDao customerDao, FirebaseService firebaseService) {
        this.customerDao = customerDao;
        this.firebaseService = firebaseService;
    }

    public LiveData<List<CustomerEntity>> getAllCustomers() {
        return customerDao.getAllLiveData();
    }

    public void syncFromFirebase() {
        List<CustomerEntity> remoteList = firebaseService.fetchAllCustomers();
        if (!remoteList.isEmpty()) {
            customerDao.insertAllSync(remoteList);
        }
    }
}
