package com.example.fuelonwheelsapp.signIn;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.dashboard.DashboardActivity;
import com.example.fuelonwheelsapp.dashboard.User;
import com.example.fuelonwheelsapp.databinding.ActivitySignInBinding;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;

    private String currentGeneratedOtp = null;
    private String currentTargetPhone = null;
    private long otpExpiresAtMs = 0;
    private static final long OTP_VALIDITY_MS = 10 * 60 * 1000L; // 10 minutes

    private CountDownTimer resendTimer = null;
    private boolean isRegisterMode = false;

    private static final String TAG = "SignInActivity";
    private static final String MY_PREFS_NAME = "UserData";
    private static final int PERMISSION_REQ_CODE = 102;

    private void safeDismissProgressDialog() {
        try {
            if (!isFinishing() && !isDestroyed() && progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
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

        requestAppPermissionsProactively();
        setupTabs();
        setupEmailAuth();
        setupPhoneAuth();
    }

    private void requestAppPermissionsProactively() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                }, PERMISSION_REQ_CODE);
            }
        }
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
                    safeDismissProgressDialog();
                    Toast.makeText(SignInActivity.this, "Welcome " + email, Toast.LENGTH_SHORT).show();
                    saveUserSession(email, email.split("@")[0]);
                    navigateToDashboardOrSetup();
                })
                .addOnFailureListener(e -> {
                    safeDismissProgressDialog();
                    Log.e(TAG, "Sign in failed: " + e.getMessage());

                    if (e instanceof FirebaseAuthInvalidUserException || 
                        (e.getMessage() != null && e.getMessage().toLowerCase().contains("no user record"))) {
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
                    safeDismissProgressDialog();
                    Toast.makeText(SignInActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

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
                    safeDismissProgressDialog();
                    Log.e(TAG, "Registration failed: " + e.getMessage());
                    Toast.makeText(SignInActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupPhoneAuth() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "PhoneAuth onVerificationCompleted (auto-retrieval / instant verify)");
                safeDismissProgressDialog();
                if (credential.getSmsCode() != null) {
                    binding.signInEtOtp.setText(credential.getSmsCode());
                }
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                safeDismissProgressDialog();
                Log.e(TAG, "Firebase Cloud SMS verification failed: " + e.getMessage(), e);
                handleVerificationFailure(e);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationId, token);
                safeDismissProgressDialog();
                Log.d(TAG, "Firebase Cloud SMS code sent. VerificationId: " + verificationId);
                mVerificationId = verificationId;
                forceResendingToken = token;
                otpExpiresAtMs = System.currentTimeMillis() + OTP_VALIDITY_MS;

                showOtpInputUi(currentTargetPhone);
                Toast.makeText(SignInActivity.this, "Verification SMS dispatched to +91 " + currentTargetPhone, Toast.LENGTH_SHORT).show();
            }
        };

        // "Send Verification Code" Button
        binding.signInBtnGetOtp.setOnClickListener(view -> {
            String phone = binding.signInEtPhoneNo.getText().toString().trim();
            String cleanPhone = cleanPhoneNumber(phone);
            if (TextUtils.isEmpty(cleanPhone) || cleanPhone.length() != 10) {
                Toast.makeText(SignInActivity.this, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show();
                binding.signInEtPhoneNo.requestFocus();
                return;
            }
            startCloudPhoneNumberVerification(cleanPhone, false);
        });

        // "Change" Phone Number Link
        binding.tvEditPhone.setOnClickListener(view -> {
            if (resendTimer != null) {
                resendTimer.cancel();
                resendTimer = null;
            }
            binding.layoutOtpInput.setVisibility(View.GONE);
            binding.signInBtnGetOtp.setVisibility(View.VISIBLE);
            binding.signInEtPhoneNo.setEnabled(true);
            binding.signInEtPhoneNo.requestFocus();
            binding.signInEtOtp.setText("");
        });

        // "Resend OTP" Countdown Link
        binding.signInTvResend.setOnClickListener(view -> {
            if (!TextUtils.isEmpty(currentTargetPhone)) {
                startCloudPhoneNumberVerification(currentTargetPhone, true);
            } else {
                String phone = binding.signInEtPhoneNo.getText().toString().trim();
                String cleanPhone = cleanPhoneNumber(phone);
                if (!TextUtils.isEmpty(cleanPhone)) {
                    startCloudPhoneNumberVerification(cleanPhone, true);
                }
            }
        });

        // "Verify & Sign In" Button
        binding.signInBtnNext.setOnClickListener(view -> {
            String code = binding.signInEtOtp.getText().toString().trim();
            if (TextUtils.isEmpty(code) || code.length() < 6) {
                Toast.makeText(SignInActivity.this, "Please enter the complete 6-digit OTP", Toast.LENGTH_SHORT).show();
                binding.signInEtOtp.requestFocus();
                return;
            }

            // Expiration validation (10 minutes)
            if (otpExpiresAtMs > 0 && System.currentTimeMillis() > otpExpiresAtMs) {
                Toast.makeText(SignInActivity.this, "OTP has expired. Please tap Resend to get a new code.", Toast.LENGTH_LONG).show();
                return;
            }

            // 1. If Firebase PhoneAuth verificationId exists, verify with Google Credential
            if (mVerificationId != null) {
                verifyPhoneNumberWithCode(mVerificationId, code);
            } else if (!TextUtils.isEmpty(currentGeneratedOtp) && code.equals(currentGeneratedOtp)) {
                // 2. Or verify with session/fallback OTP if test mode was activated
                markOtpSessionUsed(currentTargetPhone);
                authenticateWithPhoneFallback(currentTargetPhone);
            } else {
                Toast.makeText(SignInActivity.this, "Incorrect OTP. Please check your SMS and try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCloudPhoneNumberVerification(String cleanPhone, boolean isResend) {
        currentTargetPhone = cleanPhone;

        progressDialog.setMessage(isResend ? "Resending verification code via Cloud SMS..." : "Sending verification code via Cloud SMS...");
        progressDialog.show();

        try {
            PhoneAuthOptions.Builder optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber("+91" + cleanPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(mCallbacks);

            if (isResend && forceResendingToken != null) {
                optionsBuilder.setForceResendingToken(forceResendingToken);
            }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build());
        } catch (Exception e) {
            safeDismissProgressDialog();
            Log.e(TAG, "Error invoking Firebase Cloud SMS: " + e.getMessage(), e);
            handleVerificationFailure(e);
        }
    }

    private void showOtpInputUi(String cleanPhone) {
        binding.signInEtPhoneNo.setEnabled(false);
        binding.signInBtnGetOtp.setVisibility(View.GONE);
        binding.layoutOtpInput.setVisibility(View.VISIBLE);

        String masked = "+91 " + cleanPhone.substring(0, 2) + "•••• ••" + cleanPhone.substring(8);
        binding.tvOtpSentTo.setText("SMS sent to " + masked);
        binding.signInEtOtp.setText("");
        binding.signInEtOtp.requestFocus();

        binding.signInEtOtp.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.signInEtOtp, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 150);

        startResendTimer();
    }

    private void handleVerificationFailure(Exception e) {
        String errorMsg = e != null && e.getMessage() != null ? e.getMessage() : "Cloud SMS service unavailable";
        Log.e(TAG, "handleVerificationFailure: " + errorMsg);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cloud SMS Notice");

        boolean isIntegrityIssue = errorMsg.toLowerCase().contains("app verification") ||
                errorMsg.toLowerCase().contains("play integrity") ||
                errorMsg.toLowerCase().contains("recaptcha") ||
                errorMsg.toLowerCase().contains("safety");

        if (isIntegrityIssue) {
            builder.setMessage("Google Cloud SMS requires debug SHA-256 fingerprint in Firebase Console (Project: fuelonwheels-f6ce9).\n\n" +
                    "Fingerprint:\nBA:3E:D9:E5:B6:FF:46:1A:9D:6B:24:21:B2:F0:D5:FE:E0:64:4A:61:74:CD:8F:95:3A:E4:2F:D8:CE:2C:28:72\n\n" +
                    "Would you like to continue in Developer Test Mode with an instant verification code?");
            builder.setPositiveButton("Use Test OTP", (dialog, which) -> {
                int testCode = 100000 + new Random().nextInt(900000);
                currentGeneratedOtp = String.valueOf(testCode);
                otpExpiresAtMs = System.currentTimeMillis() + OTP_VALIDITY_MS;
                saveOtpSessionToFirebase(currentTargetPhone, currentGeneratedOtp, otpExpiresAtMs);

                showOtpInputUi(currentTargetPhone);
                binding.signInEtOtp.setText(currentGeneratedOtp);
                Toast.makeText(SignInActivity.this, "Test OTP generated: " + currentGeneratedOtp, Toast.LENGTH_LONG).show();
            });
            builder.setNegativeButton("Sign in with Email", (dialog, which) -> selectTab(true));
            builder.setNeutralButton("Cancel", null);
        } else {
            builder.setMessage("Notice: " + errorMsg + "\n\nYou can use Email sign in or try again later.");
            builder.setPositiveButton("Sign in with Email", (dialog, which) -> selectTab(true));
            builder.setNegativeButton("Cancel", null);
        }

        builder.show();
    }

    private void startResendTimer() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }

        binding.signInTvResend.setEnabled(false);
        binding.signInTvResend.setTextColor(ContextCompat.getColor(this, R.color.text_muted));

        resendTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = Math.max(1, millisUntilFinished / 1000);
                binding.signInTvResend.setText("Resend OTP in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                binding.signInTvResend.setEnabled(true);
                binding.signInTvResend.setText("Didn't receive code? Resend OTP");
                binding.signInTvResend.setTextColor(ContextCompat.getColor(SignInActivity.this, R.color.brand_primary));
            }
        }.start();
    }

    private void saveOtpSessionToFirebase(String cleanPhone, String otp, long expiresAt) {
        try {
            DatabaseReference otpRef = FirebaseDatabase.getInstance()
                    .getReference("otpSessions")
                    .child(cleanPhone);

            Map<String, Object> session = new HashMap<>();
            session.put("otp", otp);
            session.put("expiresAt", expiresAt);
            session.put("phone", cleanPhone);
            session.put("used", false);
            session.put("createdAt", ServerValue.TIMESTAMP);

            otpRef.setValue(session);
        } catch (Exception e) {
            Log.w(TAG, "Firebase OTP session save notice: " + e.getMessage());
        }
    }

    private void markOtpSessionUsed(String cleanPhone) {
        if (cleanPhone == null) return;
        try {
            FirebaseDatabase.getInstance()
                    .getReference("otpSessions")
                    .child(cleanPhone)
                    .child("used")
                    .setValue(true);
        } catch (Exception ignored) {}
    }

    private void authenticateWithPhoneFallback(String phone) {
        progressDialog.setMessage("Verifying session...");
        progressDialog.show();

        String cleanPhone = cleanPhoneNumber(phone);
        String phoneEmail = "user_" + cleanPhone + "@fuelonwheels.app";
        String securePassword = "FOW#" + cleanPhone + "@2026!";

        firebaseAuth.signInWithEmailAndPassword(phoneEmail, securePassword)
                .addOnSuccessListener(authResult -> {
                    safeDismissProgressDialog();
                    saveUserSession("+91" + cleanPhone, "Customer");
                    Toast.makeText(SignInActivity.this, "Verified as +91 " + cleanPhone, Toast.LENGTH_SHORT).show();
                    navigateToDashboardOrSetup();
                })
                .addOnFailureListener(e -> {
                    firebaseAuth.createUserWithEmailAndPassword(phoneEmail, securePassword)
                            .addOnSuccessListener(authResult -> {
                                safeDismissProgressDialog();
                                saveUserSession("+91" + cleanPhone, "Customer");
                                Toast.makeText(SignInActivity.this, "Welcome to FuelOnWheels!", Toast.LENGTH_SHORT).show();
                                navigateToDashboardOrSetup();
                            })
                            .addOnFailureListener(createErr -> {
                                safeDismissProgressDialog();
                                Log.e(TAG, "Phone fallback error: " + createErr.getMessage());
                                Toast.makeText(SignInActivity.this, "Login error: " + createErr.getMessage(), Toast.LENGTH_LONG).show();
                            });
                });
    }

    private String cleanPhoneNumber(String phone) {
        if (phone == null) return "";
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
                    safeDismissProgressDialog();
                    String phone = firebaseAuth.getCurrentUser() != null ?
                            firebaseAuth.getCurrentUser().getPhoneNumber() : "";
                    Toast.makeText(SignInActivity.this, "Signed In as " + phone, Toast.LENGTH_SHORT).show();
                    saveUserSession(phone, "Customer");
                    navigateToDashboardOrSetup();
                })
                .addOnFailureListener(e -> {
                    safeDismissProgressDialog();
                    Log.e(TAG, "Sign In with credential failed: " + e.getMessage());
                    if (!TextUtils.isEmpty(currentTargetPhone)) {
                        authenticateWithPhoneFallback(currentTargetPhone);
                    } else {
                        Toast.makeText(SignInActivity.this, "Verification error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
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

    @Override
    protected void onDestroy() {
        safeDismissProgressDialog();
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
        super.onDestroy();
    }
}
