package com.example.fuelonwheelsapp.signIn;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // Pre-setup views and click listeners immediately so UI is always ready
        setUpViews();
        binding.setupContainer.setVisibility(View.VISIBLE);
        binding.setupBtnNext.setOnClickListener(view -> checkValidations());

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Checking profile...");
        progressDialog.show();

        // Safety timeout: dismiss loading dialog after 1.5s max so user is never stuck
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                binding.setupContainer.setVisibility(View.VISIBLE);
            }
        }, 1500);

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
        progressDialog.setMessage("Saving details...");
        progressDialog.show();

        // 1. Locally persist profile in SharedPreferences immediately
        android.content.SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        prefs.edit()
                .putString("fullName", user.getFullName())
                .putString("email", user.getEmail())
                .putString("phoneNo", user.getPhoneNo())
                .apply();

        // 2. Save via ViewModel/Firebase
        userViewModel.saveUserDetailsAsProfile(user);

        // 3. Fallback timer: proceed to dashboard within 2 seconds regardless of network latency
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                gotoDashboardActivity();
                finish();
            }
        }, 2000);

        userViewModel.dataSavedSuccessfully.observe(this, aBoolean -> {
            if (aBoolean != null && aBoolean) {
                progressDialog.dismiss();
                gotoDashboardActivity();
                finish();
            } else if (aBoolean != null) {
                progressDialog.dismiss();
                gotoDashboardActivity();
                finish();
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
        android.content.SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        String savedPhone = prefs.getString("userIdentifier", "");

        if (!TextUtils.isEmpty(savedPhone) && savedPhone.startsWith("+")) {
            binding.setupEtPhone.setText(savedPhone);
            binding.setupEtPhone.setEnabled(false);
        } else if (firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getPhoneNumber())) {
            binding.setupEtPhone.setText(firebaseUser.getPhoneNumber());
            binding.setupEtPhone.setEnabled(false);
        }

        if (firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getEmail())) {
            String email = firebaseUser.getEmail();
            if (!email.contains("@fuelonwheels.app") && !email.contains("@fow.")) {
                binding.setupEtEmail.setText(email);
                binding.setupEtEmail.setEnabled(false);
            }
            if (!TextUtils.isEmpty(firebaseUser.getDisplayName())) {
                binding.setupEtFname.setText(firebaseUser.getDisplayName());
            }
        }
    }
}
