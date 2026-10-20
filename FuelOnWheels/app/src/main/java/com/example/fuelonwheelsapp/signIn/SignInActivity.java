package com.example.fuelonwheelsapp.signIn;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.dashboard.DashboardActivity;
import com.example.fuelonwheelsapp.dashboard.User;
import com.example.fuelonwheelsapp.databinding.ActivitySignInBinding;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private boolean isRegisterMode = false;
    private static final String TAG = "SignInActivity";
    private static final String MY_PREFS_NAME = "UserData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        // Auto-navigate if already authenticated
        if (firebaseAuth.getCurrentUser() != null) {
            navigateToDashboardOrSetup();
            return;
        }

        setupTabs();
        setupEmailAuth();
        setupPhoneAuth();
    }

    private void setupTabs() {
        binding.tabEmail.setOnClickListener(v -> selectTab(true));
        binding.tabPhone.setOnClickListener(v -> selectTab(false));
    }

    private void selectTab(boolean emailSelected) {
        if (emailSelected) {
            binding.tabEmail.setBackgroundResource(R.drawable.tab_selected_bg);
            binding.tabEmail.setTextColor(ContextCompat.getColor(this, R.color.white));

            binding.tabPhone.setBackground(null);
            binding.tabPhone.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            binding.layoutEmailForm.setVisibility(View.VISIBLE);
            binding.layoutPhoneForm.setVisibility(View.GONE);
        } else {
            binding.tabPhone.setBackgroundResource(R.drawable.tab_selected_bg);
            binding.tabPhone.setTextColor(ContextCompat.getColor(this, R.color.white));

            binding.tabEmail.setBackground(null);
            binding.tabEmail.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            binding.layoutEmailForm.setVisibility(View.GONE);
            binding.layoutPhoneForm.setVisibility(View.VISIBLE);
        }
    }

    private void setupEmailAuth() {
        binding.tvToggleAuthMode.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            if (isRegisterMode) {
                binding.tvFormTitle.setText("Create Account");
                binding.tvFormSubtitle.setText("Sign up for instant doorstep fuel delivery");
                binding.layoutFullName.setVisibility(View.VISIBLE);
                binding.btnEmailSubmit.setText("Create Account");
                binding.tvToggleAuthMode.setText("Already have an account? Sign In");
            } else {
                binding.tvFormTitle.setText("Welcome Back");
                binding.tvFormSubtitle.setText("Sign in to order fuel directly to your location");
                binding.layoutFullName.setVisibility(View.GONE);
                binding.btnEmailSubmit.setText("Sign In");
                binding.tvToggleAuthMode.setText("Don't have an account? Create One");
            }
        });

        binding.btnEmailSubmit.setOnClickListener(v -> handleEmailSubmit());
    }

    private void handleEmailSubmit() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String fullName = binding.etFullName.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return;
        }

        if (isRegisterMode) {
            if (TextUtils.isEmpty(fullName)) {
                binding.etFullName.setError("Full Name is required");
                binding.etFullName.requestFocus();
                return;
            }
            registerWithEmail(email, password, fullName);
        } else {
            signInWithEmail(email, password);
        }
    }

    private void signInWithEmail(String email, String password) {
        progressDialog.setMessage("Signing In...");
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignInActivity.this, "Welcome " + email, Toast.LENGTH_SHORT).show();
                    saveUserSession(email, email.split("@")[0]);
                    navigateToDashboardOrSetup();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Sign in failed: " + e.getMessage());

                    if (e instanceof FirebaseAuthInvalidUserException || 
                        (e.getMessage() != null && e.getMessage().toLowerCase().contains("no user record"))) {
                        // User does not exist - offer 1-tap account creation!
                        new AlertDialog.Builder(SignInActivity.this)
                                .setTitle("Account Not Found")
                                .setMessage("No account was found for " + email + ".\n\nWould you like to register this account now?")
                                .setPositiveButton("Create Account", (dialog, which) -> {
                                    String name = email.split("@")[0];
                                    registerWithEmail(email, password, name);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException ||
                               (e.getMessage() != null && e.getMessage().toLowerCase().contains("password"))) {
                        Toast.makeText(SignInActivity.this, "Incorrect password. Please verify your credentials.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SignInActivity.this, "Sign In Notice: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerWithEmail(String email, String password, String fullName) {
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignInActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                    // Save user details to Firebase Database
                    if (firebaseAuth.getCurrentUser() != null) {
                        String uid = firebaseAuth.getCurrentUser().getUid();
                        User newUser = new User(fullName, email, "9999999999", "123456");
                        try {
                            FirebaseDatabase.getInstance().getReference("users").child(uid).setValue(newUser);
                        } catch (Exception ignored) {}
                    }

                    saveUserSession(email, fullName);
                    navigateToDashboardOrSetup();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Registration failed: " + e.getMessage());
                    Toast.makeText(SignInActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupPhoneAuth() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted");
                if (credential.getSmsCode() != null) {
                    binding.signInEtOtp.setText(credential.getSmsCode());
                }
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                progressDialog.dismiss();
                binding.signInEtPhoneNo.setEnabled(true);
                Log.e("OTP_ERROR", "Verification Failed", e);

                String friendlyMessage = "SMS verification notice: " + e.getMessage();
                if (e instanceof FirebaseTooManyRequestsException) {
                    friendlyMessage = "Daily SMS quota exceeded. Please use Email Sign-In for instant access.";
                }

                new AlertDialog.Builder(SignInActivity.this)
                        .setTitle("Phone Verification")
                        .setMessage(friendlyMessage + "\n\nTip: You can use the Email Access tab to sign in immediately without SMS delays.")
                        .setPositiveButton("Switch to Email", (dialog, which) -> selectTab(true))
                        .setNegativeButton("OK", null)
                        .show();
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationId, token);
                Log.d(TAG, "onCodeSent: " + verificationId);
                mVerificationId = verificationId;
                forceResendingToken = token;
                progressDialog.dismiss();

                binding.signInEtPhoneNo.setEnabled(false);
                binding.layoutOtpInput.setVisibility(View.VISIBLE);
                binding.signInBtnGetOtp.setVisibility(View.GONE);

                Toast.makeText(SignInActivity.this, "OTP sent successfully!", Toast.LENGTH_SHORT).show();
                binding.signInEtOtp.requestFocus();
            }
        };

        binding.signInBtnGetOtp.setOnClickListener(view -> {
            String phone = binding.signInEtPhoneNo.getText().toString().trim();
            if (TextUtils.isEmpty(phone) || phone.length() < 10) {
                Toast.makeText(SignInActivity.this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            } else {
                startPhoneNumberVerification(phone);
            }
        });

        binding.signInTvResend.setOnClickListener(view -> {
            String phone = binding.signInEtPhoneNo.getText().toString().trim();
            if (!TextUtils.isEmpty(phone)) {
                resendVerificationCode(phone, forceResendingToken);
            }
        });

        binding.signInBtnNext.setOnClickListener(view -> {
            String code = binding.signInEtOtp.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(SignInActivity.this, "Please enter OTP", Toast.LENGTH_SHORT).show();
            } else {
                verifyPhoneNumberWithCode(mVerificationId, code);
            }
        });
    }

    private void startPhoneNumberVerification(String phone) {
        progressDialog.setMessage("Sending OTP...");
        progressDialog.show();

        String cleanPhone = cleanPhoneNumber(phone);
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber("+91" + cleanPhone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendVerificationCode(String phone, PhoneAuthProvider.ForceResendingToken token) {
        progressDialog.setMessage("Resending OTP...");
        progressDialog.show();

        String cleanPhone = cleanPhoneNumber(phone);
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber("+91" + cleanPhone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .setForceResendingToken(token)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private String cleanPhoneNumber(String phone) {
        String clean = phone.replaceAll("[^0-9]", "");
        if (clean.startsWith("91") && clean.length() == 12) {
            clean = clean.substring(2);
        } else if (clean.startsWith("0") && clean.length() == 11) {
            clean = clean.substring(1);
        }
        return clean;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        progressDialog.setMessage("Verifying OTP...");
        progressDialog.show();

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        progressDialog.setMessage("Signing In...");

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    progressDialog.dismiss();
                    String phone = firebaseAuth.getCurrentUser() != null ?
                            firebaseAuth.getCurrentUser().getPhoneNumber() : "";
                    Toast.makeText(SignInActivity.this, "Signed In as " + phone, Toast.LENGTH_SHORT).show();
                    saveUserSession(phone, "Customer");
                    navigateToDashboardOrSetup();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("OTP_ERROR", "Sign In Failed", e);
                    Toast.makeText(SignInActivity.this, "Sign In Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserSession(String identifier, String name) {
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString("userIdentifier", identifier);
        editor.putString("userName", name);
        editor.apply();
    }

    private void navigateToDashboardOrSetup() {
        Intent intent = new Intent(SignInActivity.this, SetupProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
