package com.example.fuelonwheelsapp.repositories;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fuelonwheelsapp.dashboard.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileRepository {
    private static final String TAG = "ProfileRepository";
    private final DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
    private User user;

    public ProfileRepository() {
    }

    private DatabaseReference getProfileRef() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "guest";
        return rootReference.child("users").child(uid);
    }

    public MutableLiveData<User> getResponseFromDatabase() {
        MutableLiveData<User> mutableLiveData = new MutableLiveData<>();
        getProfileRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(User.class);
                }
                mutableLiveData.setValue(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Profile fetch cancelled: " + error.getMessage());
            }
        });
        return mutableLiveData;
    }

    public LiveData<Boolean> saveUserData(User user) {
        MutableLiveData<Boolean> booleanMutableLiveData = new MutableLiveData<>();
        getProfileRef().setValue(user)
                .addOnSuccessListener(unused -> {
                    booleanMutableLiveData.setValue(true);
                    Log.d(TAG, "Saved profile successfully");
                })
                .addOnFailureListener(e -> {
                    booleanMutableLiveData.setValue(false);
                    Log.e(TAG, "Failed saving profile: " + e.getMessage());
                });
        return booleanMutableLiveData;
    }
}
