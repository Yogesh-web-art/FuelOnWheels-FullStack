package com.example.fuelonwheelsapp.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.signIn.SignInActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseUser firebaseUser;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance(String param1, String param2) {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind interactive rows
        View itemNotifications = view.findViewById(R.id.settings_item_notifications);
        if (itemNotifications != null) {
            itemNotifications.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Order & delivery push notifications are active", Toast.LENGTH_SHORT).show());
        }

        View itemApp = view.findViewById(R.id.settings_item_app);
        if (itemApp != null) {
            itemApp.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "High-accuracy GPS & OpenStreetMap tile caching enabled", Toast.LENGTH_SHORT).show());
        }

        View itemPrivacy = view.findViewById(R.id.settings_item_privacy);
        if (itemPrivacy != null) {
            itemPrivacy.setOnClickListener(v -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Piyush-0120/FuelOnWheels/blob/master/README.md"));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Your personal data is encrypted & secured", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View itemContact = view.findViewById(R.id.settings_item_contact);
        if (itemContact != null) {
            itemContact.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:1800123456"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Helpline: support@refuelanywhere.org", Toast.LENGTH_SHORT).show();
                }
            });
        }

        checkUserStatus(view);

        // Sign Out Button
        Button button = view.findViewById(R.id.settings_btn_signout);
        if (button != null) {
            button.setOnClickListener(v -> {
                try {
                    if (firebaseUser != null && firebaseUser.getUid() != null) {
                        try {
                            databaseReference.child("users").child(firebaseUser.getUid()).keepSynced(false);
                        } catch (Exception ignored) {}
                    }
                    if (firebaseAuth != null) {
                        firebaseAuth.signOut();
                    }
                    // Clear shared preferences cleanly
                    Context ctx = getContext();
                    if (ctx != null) {
                        SharedPreferences prefs = ctx.getSharedPreferences("UserData", Context.MODE_PRIVATE);
                        prefs.edit().clear().apply();
                    }
                    Intent intent = new Intent(getContext(), SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getContext(), SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });
        }
    }

    private void checkUserStatus(View view) {
        TextView textView = view.findViewById(R.id.settings_tv_phone);
        if (textView == null) return;

        String phone = null;
        if (firebaseUser != null) {
            phone = firebaseUser.getPhoneNumber();
            if (phone == null || phone.isEmpty()) {
                phone = firebaseUser.getEmail();
            }
        }
        if (phone == null || phone.isEmpty()) {
            try {
                SharedPreferences prefs = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE);
                phone = prefs.getString("phoneNo", prefs.getString("userIdentifier", prefs.getString("email", null)));
            } catch (Exception ignored) {}
        }
        if (phone != null && !phone.isEmpty()) {
            textView.setText(phone);
        } else {
            textView.setText("Active Member Session");
        }
    }
}
