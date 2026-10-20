package com.example.fuelonwheelsapp.signIn;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.fuelonwheelsapp.dashboard.DashboardActivity;
import com.example.fuelonwheelsapp.dashboard.User;
import com.example.fuelonwheelsapp.databinding.ActivitySetupProfileBinding;
import com.example.fuelonwheelsapp.viewModels.UserViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Random;

public class SetupProfileActivity extends AppCompatActivity {

    private ActivitySetupProfileBinding binding;
    private UserViewModel userViewModel;
    private ProgressDialog progressDialog;
    private String fullNameOfUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        progressDialog.setMessage("Signing In");
        progressDialog.show();

        isProfileCompleteUsingLiveData();
    }

    private void isProfileCompleteUsingLiveData() {
        userViewModel.getResponseAsUserProfile().observe(this, user -> {
            progressDialog.dismiss();
            if (user != null && user.getFullName() != null
                    && user.getFullName().length() > 0
                    && user.getEmail() != null
                    && user.getEmail().length() > 0
                    && user.getPhoneNo() != null
                    && user.getPhoneNo().length() > 0) {
                fullNameOfUser = user.getFullName();
                gotoDashboardActivity();
                finish();
            } else {
                binding.setupContainer.setVisibility(View.VISIBLE);
                setUpViews();
                binding.setupBtnNext.setOnClickListener(view -> {
                    checkValidations(); // save and send to Dashboard
                });
            }
        });
    }

    private void checkValidations() {
        String fullName = binding.setupEtFname.getText().toString().trim();
        String phone = binding.setupEtPhone.getText().toString().trim();
        String email = binding.setupEtEmail.getText().toString().trim();
        String otp = generateRandomOTP();

        if (TextUtils.isEmpty(fullName)) {
            binding.setupEtFname.setError("Field cannot be empty");
            binding.setupEtFname.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            binding.setupEtPhone.setError("Field cannot be empty");
            binding.setupEtPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.setupEtEmail.setError("Field cannot be empty");
            binding.setupEtEmail.requestFocus();
            return;
        }

        User user = new User(fullName, email, phone, otp);
        fullNameOfUser = fullName;
        saveUserDetails(user);
    }

    private String generateRandomOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private void saveUserDetails(User user) {
        progressDialog.setMessage("Saving details");
        progressDialog.show();
        userViewModel.saveUserDetailsAsProfile(user);
        userViewModel.dataSavedSuccessfully.observe(this, aBoolean -> {
            if (aBoolean) {
                progressDialog.dismiss();
            } else {
                progressDialog.dismiss();
                Toast.makeText(SetupProfileActivity.this, "Some error occurred while saving profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void gotoDashboardActivity() {
        Intent intent = new Intent(SetupProfileActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fullName", fullNameOfUser);
        startActivity(intent);
    }

    private void setUpViews() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            if (firebaseUser.getPhoneNumber() != null && firebaseUser.getPhoneNumber().length() != 0) {
                binding.setupEtPhone.setText(firebaseUser.getPhoneNumber());
                binding.setupEtPhone.setEnabled(false);
            }
            if (firebaseUser.getEmail() != null && firebaseUser.getEmail().length() != 0) {
                binding.setupEtEmail.setText(firebaseUser.getEmail());
                binding.setupEtFname.setText(firebaseUser.getDisplayName());
                binding.setupEtEmail.setEnabled(false);
            }
        }
    }
}
