package com.example.fuelonwheelsapp.signIn;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private String currentGeneratedOtp = null;
    private String currentTargetPhone = null;
    private long otpExpiresAtMs = 0;
    private static final long OTP_VALIDITY_MS = 10 * 60 * 1000L; // 10 minutes

    private CountDownTimer resendTimer = null;
    private CountDownTimer emailResendTimer = null;
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

        // Handle password reset deep link from email (if launched via action link)
        checkPasswordResetIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkPasswordResetIntent(intent);
    }

    private void requestAppPermissionsProactively() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.SEND_SMS
                }, PERMISSION_REQ_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.SEND_SMS
                }, PERMISSION_REQ_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String phone = binding.signInEtPhoneNo.getText().toString().trim();
                String cleanPhone = cleanPhoneNumber(phone);
                if (!TextUtils.isEmpty(cleanPhone) && cleanPhone.length() == 10) {
                    dispatchOtpSms(cleanPhone);
                }
            } else {
                Toast.makeText(this, "SMS permission is required so this phone's SIM can send the OTP", Toast.LENGTH_LONG).show();
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
                binding.layoutForgotPassword.setVisibility(View.GONE);
            } else {
                binding.tvFormTitle.setText("Welcome Back");
                binding.tvFormSubtitle.setText("Sign in to order fuel directly to your location");
                binding.layoutFullName.setVisibility(View.GONE);
                binding.btnEmailSubmit.setText("Sign In");
                binding.tvToggleAuthMode.setText("Don't have an account? Create One");
                binding.layoutForgotPassword.setVisibility(View.VISIBLE);
            }
        });

        // Toggle password visibility
        final boolean[] mainPassVisible = {false};
        binding.ivTogglePassword.setOnClickListener(v -> {
            mainPassVisible[0] = !mainPassVisible[0];
            if (mainPassVisible[0]) {
                binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_24);
            } else {
                binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off_24);
            }
            binding.etPassword.setSelection(binding.etPassword.getText().length());
        });

        // Forgot password button click
        binding.tvForgotPassword.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            showForgotPasswordDialog(email, null);
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
                        new AlertDialog.Builder(SignInActivity.this)
                                .setTitle("Incorrect Password")
                                .setMessage("The password you entered for " + email + " is incorrect.\n\nWould you like to reset your password?")
                                .setPositiveButton("Reset Password", (dialog, which) -> {
                                    showForgotPasswordDialog(email, null);
                                })
                                .setNegativeButton("Try Again", null)
                                .show();
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
        // "Send Verification Code" Button
        binding.signInBtnGetOtp.setOnClickListener(view -> {
            String phone = binding.signInEtPhoneNo.getText().toString().trim();
            String cleanPhone = cleanPhoneNumber(phone);
            if (TextUtils.isEmpty(cleanPhone) || cleanPhone.length() != 10) {
                Toast.makeText(SignInActivity.this, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show();
                binding.signInEtPhoneNo.requestFocus();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQ_CODE);
            } else {
                dispatchOtpSms(cleanPhone);
            }
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
                dispatchOtpSms(currentTargetPhone);
            } else {
                String phone = binding.signInEtPhoneNo.getText().toString().trim();
                String cleanPhone = cleanPhoneNumber(phone);
                if (!TextUtils.isEmpty(cleanPhone)) {
                    dispatchOtpSms(cleanPhone);
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

            // 1. Verify against in-memory generated OTP
            if (!TextUtils.isEmpty(currentGeneratedOtp) && code.equals(currentGeneratedOtp)) {
                markOtpSessionUsed(currentTargetPhone);
                authenticateWithPhoneFallback(currentTargetPhone);
                return;
            }

            // 2. Or verify against Firebase Realtime Database OTP session (for multi-device sync)
            verifyWithFirebaseSession(currentTargetPhone, code);
        });

        // Setup real-time background relay for other users
        setupFirebaseSmsRelay();
    }

    private void dispatchOtpSms(String cleanPhone) {
        currentTargetPhone = cleanPhone;

        // Generate secure 6-digit OTP
        int code = 100000 + new Random().nextInt(900000);
        currentGeneratedOtp = String.valueOf(code);
        otpExpiresAtMs = System.currentTimeMillis() + OTP_VALIDITY_MS;

        // Persist session to Firebase Realtime Database
        saveOtpSessionToFirebase(cleanPhone, currentGeneratedOtp, otpExpiresAtMs);

        // Transition UI to OTP input view
        showOtpInputUi(cleanPhone);

        // Send SMS via phone's hardware SIM card directly to the recipient
        boolean sent = sendSmsViaDeviceSim(cleanPhone, currentGeneratedOtp);

        // Post to Firebase SMS queue so any SIM-enabled device can assist if needed
        postToSmsRelayQueue(cleanPhone, currentGeneratedOtp, sent);

        if (sent) {
            Toast.makeText(this, "OTP SMS sent from SIM to +91 " + cleanPhone, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "OTP requested for +91 " + cleanPhone, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean sendSmsViaDeviceSim(String cleanPhone, String otp) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        try {
            SmsManager smsManager = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = getSystemService(SmsManager.class);
            }
            if (smsManager == null) {
                int subId = SubscriptionManager.getDefaultSmsSubscriptionId();
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
                } else {
                    smsManager = SmsManager.getDefault();
                }
            }

            if (smsManager != null) {
                String message = "Refuel Anywhere: " + otp + " is your verification OTP. Valid for 10 minutes. Do not share this OTP with anyone.";
                ArrayList<String> parts = smsManager.divideMessage(message);
                if (parts.size() > 1) {
                    smsManager.sendMultipartTextMessage("+91" + cleanPhone, null, parts, null, null);
                } else {
                    smsManager.sendTextMessage("+91" + cleanPhone, null, message, null, null);
                }
                Log.d(TAG, "SIM SMS successfully sent to +91" + cleanPhone);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS via SIM: " + e.getMessage(), e);
        }
        return false;
    }

    private void postToSmsRelayQueue(String cleanPhone, String otp, boolean alreadyDispatched) {
        try {
            DatabaseReference queueRef = FirebaseDatabase.getInstance()
                    .getReference("smsRelayQueue")
                    .child(cleanPhone);

            Map<String, Object> item = new HashMap<>();
            item.put("phone", cleanPhone);
            item.put("otp", otp);
            item.put("dispatched", alreadyDispatched);
            item.put("timestamp", ServerValue.TIMESTAMP);

            queueRef.setValue(item);
        } catch (Exception ignored) {}
    }

    private void setupFirebaseSmsRelay() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            DatabaseReference queueRef = FirebaseDatabase.getInstance().getReference("smsRelayQueue");
            queueRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                    processRelayItem(snapshot);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                    processRelayItem(snapshot);
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } catch (Exception ignored) {}
    }

    private void processRelayItem(DataSnapshot snapshot) {
        try {
            Boolean dispatched = snapshot.child("dispatched").getValue(Boolean.class);
            String phone = snapshot.child("phone").getValue(String.class);
            String otp = snapshot.child("otp").getValue(String.class);

            if (Boolean.FALSE.equals(dispatched) && !TextUtils.isEmpty(phone) && !TextUtils.isEmpty(otp)) {
                boolean sent = sendSmsViaDeviceSim(phone, otp);
                if (sent) {
                    snapshot.getRef().child("dispatched").setValue(true);
                }
            }
        } catch (Exception ignored) {}
    }

    private void verifyWithFirebaseSession(String phone, String inputCode) {
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Incorrect OTP. Please check your SMS and try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Verifying OTP...");
        progressDialog.show();

        FirebaseDatabase.getInstance().getReference("otpSessions").child(phone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        safeDismissProgressDialog();
                        if (snapshot.exists()) {
                            String storedOtp = snapshot.child("otp").getValue(String.class);
                            Long expiresAt = snapshot.child("expiresAt").getValue(Long.class);
                            Boolean used = snapshot.child("used").getValue(Boolean.class);

                            long now = System.currentTimeMillis();
                            if (expiresAt != null && now > expiresAt) {
                                Toast.makeText(SignInActivity.this, "OTP has expired. Tap Resend for a new code.", Toast.LENGTH_LONG).show();
                            } else if (Boolean.TRUE.equals(used)) {
                                Toast.makeText(SignInActivity.this, "This OTP has already been used.", Toast.LENGTH_SHORT).show();
                            } else if (storedOtp != null && storedOtp.equals(inputCode)) {
                                snapshot.getRef().child("used").setValue(true);
                                authenticateWithPhoneFallback(phone);
                            } else {
                                Toast.makeText(SignInActivity.this, "Incorrect OTP. Please check your SMS.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SignInActivity.this, "No active OTP session found. Tap Resend.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        safeDismissProgressDialog();
                        Toast.makeText(SignInActivity.this, "Verification network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
                                Toast.makeText(SignInActivity.this, "Welcome to Refuel Anywhere!", Toast.LENGTH_SHORT).show();
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

    private void showForgotPasswordDialog(String prefillEmail, String initialCode) {
        if (isFinishing() || isDestroyed()) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.setCancelable(true);

        ImageView ivClose = dialogView.findViewById(R.id.iv_close_dialog);
        View layoutStep1 = dialogView.findViewById(R.id.layout_step_request_code);
        EditText etForgotEmail = dialogView.findViewById(R.id.et_forgot_email);
        Button btnSendResetCode = dialogView.findViewById(R.id.btn_send_reset_code);

        View layoutStep2 = dialogView.findViewById(R.id.layout_step_verify_code);
        TextView tvVerifyNotice = dialogView.findViewById(R.id.tv_verify_notice);
        EditText etVerifyCode = dialogView.findViewById(R.id.et_verify_code);
        TextView btnPasteCode = dialogView.findViewById(R.id.btn_paste_code);
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        ImageView ivToggleNewPassword = dialogView.findViewById(R.id.iv_toggle_new_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_new_password);
        ImageView ivToggleConfirmPassword = dialogView.findViewById(R.id.iv_toggle_confirm_password);
        Button btnUpdatePassword = dialogView.findViewById(R.id.btn_update_password);
        TextView tvResendCode = dialogView.findViewById(R.id.tv_resend_reset_code);
        TextView tvChangeEmail = dialogView.findViewById(R.id.tv_change_email);

        final String[] targetResetEmail = new String[1];

        // Pre-fill email
        if (!TextUtils.isEmpty(prefillEmail)) {
            etForgotEmail.setText(prefillEmail);
            targetResetEmail[0] = prefillEmail;
        } else {
            String currentMainEmail = binding.etEmail.getText().toString().trim();
            if (!TextUtils.isEmpty(currentMainEmail)) {
                etForgotEmail.setText(currentMainEmail);
                targetResetEmail[0] = currentMainEmail;
            }
        }

        // Close action
        ivClose.setOnClickListener(v -> dialog.dismiss());

        // Password visibility toggles
        final boolean[] newPassVisible = {false};
        ivToggleNewPassword.setOnClickListener(v -> togglePasswordVisibility(etNewPassword, ivToggleNewPassword, newPassVisible));

        final boolean[] confirmPassVisible = {false};
        ivToggleConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, confirmPassVisible));

        // Paste button action
        btnPasteCode.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence clipText = cm.getPrimaryClip().getItemAt(0).getText();
                if (clipText != null) {
                    String extracted = extractActionCode(clipText.toString().trim());
                    etVerifyCode.setText(extracted);
                    Toast.makeText(SignInActivity.this, "Code pasted from clipboard", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(SignInActivity.this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Step 1: Send Reset Code / Link Button
        btnSendResetCode.setOnClickListener(v -> {
            String email = etForgotEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etForgotEmail.setError("Please enter a valid email address");
                etForgotEmail.requestFocus();
                return;
            }

            targetResetEmail[0] = email;
            progressDialog.setMessage("Sending verification code to " + email + "...");
            progressDialog.show();

            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> {
                        safeDismissProgressDialog();
                        Toast.makeText(SignInActivity.this, "Verification email sent to " + email, Toast.LENGTH_LONG).show();

                        savePasswordResetAuditLog(email);

                        layoutStep1.setVisibility(View.GONE);
                        layoutStep2.setVisibility(View.VISIBLE);
                        tvVerifyNotice.setText("Verification code & link sent to " + email + ".\nCheck your inbox and spam folder.");
                        etVerifyCode.requestFocus();

                        startEmailResendCountdown(tvResendCode, email);
                    })
                    .addOnFailureListener(e -> {
                        safeDismissProgressDialog();
                        Log.e(TAG, "Password reset email failed: " + e.getMessage());
                        if (e instanceof FirebaseAuthInvalidUserException ||
                                (e.getMessage() != null && e.getMessage().toLowerCase().contains("no user record"))) {
                            Toast.makeText(SignInActivity.this, "No account found for " + email + ". Please check spelling or register.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SignInActivity.this, "Unable to send reset email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Step 2: Resend Code
        tvResendCode.setOnClickListener(v -> {
            String email = targetResetEmail[0];
            if (TextUtils.isEmpty(email)) {
                email = etForgotEmail.getText().toString().trim();
            }
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(SignInActivity.this, "Please enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }

            final String finalEmail = email;
            progressDialog.setMessage("Resending verification email...");
            progressDialog.show();

            firebaseAuth.sendPasswordResetEmail(finalEmail)
                    .addOnSuccessListener(unused -> {
                        safeDismissProgressDialog();
                        Toast.makeText(SignInActivity.this, "New verification email sent to " + finalEmail, Toast.LENGTH_SHORT).show();
                        savePasswordResetAuditLog(finalEmail);
                        startEmailResendCountdown(tvResendCode, finalEmail);
                    })
                    .addOnFailureListener(e -> {
                        safeDismissProgressDialog();
                        Toast.makeText(SignInActivity.this, "Resend failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        // Step 2: Change Email link
        tvChangeEmail.setOnClickListener(v -> {
            if (emailResendTimer != null) {
                emailResendTimer.cancel();
                emailResendTimer = null;
            }
            layoutStep2.setVisibility(View.GONE);
            layoutStep1.setVisibility(View.VISIBLE);
            etForgotEmail.requestFocus();
        });

        // Step 2: Verify Code and Reset Password
        btnUpdatePassword.setOnClickListener(v -> {
            String rawCode = etVerifyCode.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(rawCode)) {
                etVerifyCode.setError("Please enter or paste the verification code");
                etVerifyCode.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                etNewPassword.requestFocus();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                etConfirmPassword.requestFocus();
                return;
            }

            String actionCode = extractActionCode(rawCode);

            progressDialog.setMessage("Verifying code & updating password...");
            progressDialog.show();

            firebaseAuth.verifyPasswordResetCode(actionCode)
                    .addOnSuccessListener(accountEmail -> {
                        firebaseAuth.confirmPasswordReset(actionCode, newPassword)
                                .addOnSuccessListener(unused -> {
                                    safeDismissProgressDialog();
                                    dialog.dismiss();

                                    new AlertDialog.Builder(SignInActivity.this)
                                            .setTitle("Password Reset Successful!")
                                            .setMessage("Your password for " + accountEmail + " has been reset.\n\nYou can now sign in with your new password.")
                                            .setPositiveButton("Sign In Now", (d, which) -> {
                                                selectTab(true);
                                                binding.etEmail.setText(accountEmail);
                                                binding.etPassword.setText(newPassword);
                                                handleEmailSubmit();
                                            })
                                            .setNegativeButton("Close", (d, which) -> {
                                                selectTab(true);
                                                binding.etEmail.setText(accountEmail);
                                                binding.etPassword.setText(newPassword);
                                            })
                                            .setCancelable(false)
                                            .show();
                                })
                                .addOnFailureListener(e -> {
                                    safeDismissProgressDialog();
                                    Log.e(TAG, "confirmPasswordReset failed: " + e.getMessage());
                                    Toast.makeText(SignInActivity.this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        safeDismissProgressDialog();
                        Log.e(TAG, "verifyPasswordResetCode failed: " + e.getMessage());
                        Toast.makeText(SignInActivity.this, "Invalid or expired verification code. Please check your email or request a new code.", Toast.LENGTH_LONG).show();
                    });
        });

        // If an initialCode was provided (e.g. via deep link)
        if (!TextUtils.isEmpty(initialCode)) {
            layoutStep1.setVisibility(View.GONE);
            layoutStep2.setVisibility(View.VISIBLE);
            String cleanCode = extractActionCode(initialCode);
            etVerifyCode.setText(cleanCode);

            progressDialog.setMessage("Validating code from email...");
            progressDialog.show();

            firebaseAuth.verifyPasswordResetCode(cleanCode)
                    .addOnSuccessListener(accountEmail -> {
                        safeDismissProgressDialog();
                        targetResetEmail[0] = accountEmail;
                        tvVerifyNotice.setText("Verified code for " + accountEmail + ".\nEnter your new password below.");
                        etNewPassword.requestFocus();
                    })
                    .addOnFailureListener(e -> {
                        safeDismissProgressDialog();
                        tvVerifyNotice.setText("Notice: Please ensure your code is valid.");
                    });
        }

        dialog.show();
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleView, boolean[] state) {
        state[0] = !state[0];
        if (state[0]) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility_24);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility_off_24);
        }
        editText.setSelection(editText.getText().length());
    }

    private void startEmailResendCountdown(TextView tvResend, String email) {
        if (emailResendTimer != null) {
            emailResendTimer.cancel();
        }

        tvResend.setEnabled(false);
        tvResend.setTextColor(ContextCompat.getColor(this, R.color.text_muted));

        emailResendTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = Math.max(1, millisUntilFinished / 1000);
                tvResend.setText("Resend email in " + sec + "s");
            }

            @Override
            public void onFinish() {
                tvResend.setEnabled(true);
                tvResend.setText("Didn't get email? Resend code");
                tvResend.setTextColor(ContextCompat.getColor(SignInActivity.this, R.color.brand_primary));
            }
        }.start();
    }

    private String extractActionCode(String input) {
        if (input == null) return "";
        input = input.trim();
        if (input.contains("oobCode=")) {
            try {
                Uri uri = Uri.parse(input);
                String code = uri.getQueryParameter("oobCode");
                if (!TextUtils.isEmpty(code)) {
                    return code.trim();
                }
            } catch (Exception ignored) {}

            int idx = input.indexOf("oobCode=");
            String sub = input.substring(idx + 8);
            int amp = sub.indexOf('&');
            if (amp != -1) {
                sub = sub.substring(0, amp);
            }
            return sub.trim();
        }
        return input;
    }

    private void savePasswordResetAuditLog(String email) {
        if (TextUtils.isEmpty(email)) return;
        try {
            String sanitized = email.replace(".", "_").replace("@", "_at_");
            DatabaseReference resetRef = FirebaseDatabase.getInstance()
                    .getReference("passwordResetRequests")
                    .child(sanitized);

            Map<String, Object> log = new HashMap<>();
            log.put("email", email);
            log.put("requestedAt", ServerValue.TIMESTAMP);
            log.put("status", "EMAIL_DISPATCHED");
            resetRef.setValue(log);
        } catch (Exception ignored) {}
    }

    private void checkPasswordResetIntent(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri uri = intent.getData();
        String mode = uri.getQueryParameter("mode");
        String oobCode = uri.getQueryParameter("oobCode");
        if ("resetPassword".equalsIgnoreCase(mode) && !TextUtils.isEmpty(oobCode)) {
            selectTab(true);
            showForgotPasswordDialog(null, oobCode);
        }
    }

    @Override
    protected void onDestroy() {
        safeDismissProgressDialog();
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
        if (emailResendTimer != null) {
            emailResendTimer.cancel();
            emailResendTimer = null;
        }
        super.onDestroy();
    }
}
