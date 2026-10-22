package com.example.fuelonwheelsapp.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.viewModels.FOWViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvName = view.findViewById(R.id.profile_user_name);
        TextView tvPhone = view.findViewById(R.id.profile_user_phone);

        // Populate user details safely from SharedPreferences / Firebase
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE);
            String savedName = prefs.getString("fullName", "");
            String savedPhone = prefs.getString("phoneNo", prefs.getString("userIdentifier", ""));

            FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fbUser != null) {
                if (savedPhone.isEmpty() && fbUser.getPhoneNumber() != null) {
                    savedPhone = fbUser.getPhoneNumber();
                }
                if (savedName.isEmpty() && fbUser.getDisplayName() != null) {
                    savedName = fbUser.getDisplayName();
                }
            }

            if (!savedName.isEmpty() && tvName != null) {
                tvName.setText(savedName);
            }
            if (!savedPhone.isEmpty() && tvPhone != null) {
                tvPhone.setText(savedPhone);
            }

            // Also observe ViewModel user profile
            FOWViewModel viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
            viewModel.getResponseAsUserProfile().observe(getViewLifecycleOwner(), user -> {
                if (!isAdded() || user == null) return;
                if (user.getFullName() != null && !user.getFullName().trim().isEmpty() && tvName != null) {
                    tvName.setText(user.getFullName());
                }
                if (user.getPhoneNo() != null && !user.getPhoneNo().trim().isEmpty() && tvPhone != null) {
                    tvPhone.setText(user.getPhoneNo());
                }
            });
        } catch (Exception ignored) {}

        ArrayList<ProfileItem> arrayList = new ArrayList<>();
        arrayList.add(new ProfileItem(R.drawable.ic_baseline_account_circle_24, "Personal Details", "Name, email and delivery security PIN"));
        arrayList.add(new ProfileItem(R.drawable.ic_baseline_local_gas_station_24, "Your Orders", "View past fuel deliveries and receipts"));
        arrayList.add(new ProfileItem(R.drawable.ic_baseline_payment_24, "Payment Methods", "Cash on Delivery, UPI and saved modes"));
        arrayList.add(new ProfileItem(R.drawable.ic_baseline_contact_support_24, "About Us", "App version, licenses and support"));

        RecyclerView recyclerView = view.findViewById(R.id.profile_menu);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(new ProfileAdapter(arrayList, (view1, position) -> {
            try {
                switch (position) {
                    case 0:
                        Navigation.findNavController(view1).navigate(R.id.action_profileFragment_to_accountFragment);
                        break;
                    case 1:
                        Navigation.findNavController(view1).navigate(R.id.action_profileFragment_to_orderListFragment);
                        break;
                    case 2:
                        Navigation.findNavController(view1).navigate(R.id.action_profileFragment_to_paymentSetUpFragment);
                        break;
                    case 3:
                        Navigation.findNavController(view1).navigate(R.id.action_profileFragment_to_aboutUsFragment);
                        break;
                    default:
                        Toast.makeText(view1.getContext(), arrayList.get(position).getItem() + " clicked", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(view1.getContext(), "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }));
    }
}
