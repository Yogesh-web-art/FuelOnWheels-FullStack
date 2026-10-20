package com.example.fuelonwheelsapp.signIn;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.databinding.ActivitySignInBinding;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private static final String TAG = "SIGNIN_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        binding.signInEtOtp.setEnabled(false);
        binding.signInBtnNext.setVisibility(View.GONE);
        binding.signInTvResend.setEnabled(false);

        // Auto-navigate if already authenticated
        if (firebaseAuth.getCurrentUser() != null) {
            navigateToSetupProfile();
            return;
        }

        setupPhoneAuthCallbacks();
        setupClickListeners();
    }

    private void setupPhoneAuthCallbacks() {
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

                String friendlyMessage = "SMS verification failed: " + e.getMessage();
                if (e instanceof FirebaseTooManyRequestsException) {
                    friendlyMessage = "Daily SMS quota exceeded or too many attempts. Please try again later or use Email Sign-In.";
                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    friendlyMessage = "Invalid phone number format. Please ensure you entered a valid 10-digit number.";
                }

                // Show helpful dialog with instant Email Login alternative
                new AlertDialog.Builder(SignInActivity.this)
                        .setTitle("Phone Verification Notice")
                        .setMessage(friendlyMessage + "\n\nTip: You can use Email Sign-In to log in immediately without waiting for SMS.")
                        .setPositiveButton("Use Email Sign-In", (dialog, which) -> showEmailLoginDialog())
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
                binding.signInEtOtp.setEnabled(true);
                binding.signInBtnNext.setVisibility(View.VISIBLE);
                binding.signInBtnGetOtp.setEnabled(false);
                binding.signInTvResend.setEnabled(true);

                Toast.makeText(SignInActivity.this, "OTP sent successfully!", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void setupClickListeners() {
        binding.signInBtnGetOtp.setOnClickListener(view -> {
            String phone = binding.signInEtPhoneNo.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(SignInActivity.this, "Please enter phone number!", Toast.LENGTH_SHORT).show();
            } else {
                startPhoneNumberVerification(phone);
            }
        });

        binding.signInTvResend.setOnClickListener(view -> {
            String phone = binding.signInEtPhoneNo.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(SignInActivity.this, "Please enter phone number!", Toast.LENGTH_SHORT).show();
            } else {
                resendVerificationCode(phone, forceResendingToken);
            }
        });

        binding.signInBtnNext.setOnClickListener(view -> {
            String code = binding.signInEtOtp.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(SignInActivity.this, "Please enter OTP!", Toast.LENGTH_SHORT).show();
            } else {
                verifyPhoneNumberWithCode(mVerificationId, code);
            }
        });

        binding.signInBtnEmailLogin.setOnClickListener(view -> showEmailLoginDialog());
    }

    private void showEmailLoginDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_email_login, null);
        EditText etEmail = dialogView.findViewById(R.id.dialog_et_email);
        EditText etPassword = dialogView.findViewById(R.id.dialog_et_password);
        Button btnLogin = dialogView.findViewById(R.id.dialog_btn_login);
        Button btnRegister = dialogView.findViewById(R.id.dialog_btn_register);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(SignInActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.setMessage("Signing in with Email...");
            progressDialog.show();

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        progressDialog.dismiss();
                        dialog.dismiss();
                        Toast.makeText(SignInActivity.this, "Welcome " + email, Toast.LENGTH_SHORT).show();
                        navigateToSetupProfile();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(SignInActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(SignInActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(SignInActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.setMessage("Creating your account...");
            progressDialog.show();

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        progressDialog.dismiss();
                        dialog.dismiss();
                        Toast.makeText(SignInActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        navigateToSetupProfile();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(SignInActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        dialog.show();
        etEmail.requestFocus();
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
        String clean = phone.replaceAll("\\s+", "").replaceAll("-", "");
        if (clean.startsWith("+91")) {
            clean = clean.substring(3);
        } else if (clean.startsWith("91") && clean.length() == 12) {
            clean = clean.substring(2);
        } else if (clean.startsWith("0")) {
            clean = clean.substring(1);
        }
        return clean;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        progressDialog.setMessage("Verifying OTP...");
        progressDialog.show();

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, code);

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
                    navigateToSetupProfile();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("OTP_ERROR", "Sign In Failed", e);
                    Toast.makeText(SignInActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToSetupProfile() {
        Intent intent = new Intent(SignInActivity.this, SetupProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
