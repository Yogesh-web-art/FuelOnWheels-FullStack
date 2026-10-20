package com.example.fuelonwheelsapp.profile.account;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.placeholder.PlaceholderContent;
import com.example.fuelonwheelsapp.viewModels.FOWViewModel;

/**
 * A fragment representing a list of Items.
 */
public class AccountFragment extends Fragment {

    private FOWViewModel viewModel;
    private final static String TAG ="AccountFragment";
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AccountFragment() {
    }

    public static AccountFragment newInstance(int columnCount) {
        AccountFragment fragment = new AccountFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_list, container, false);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        ImageView imageView = view.findViewById(R.id.accountList_btn_back);
        imageView.setOnClickListener(view1 -> Navigation.findNavController(view1).navigate(R.id.action_accountFragment_to_profileFragment));
        // TODO: call this function once and use the field of viewmodel
        viewModel.getResponseAsUserProfile().observe(getViewLifecycleOwner(), user -> {
            progressBar.setVisibility(View.GONE);
            viewModel.setUser(user);
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.account_list);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            PlaceholderContent placeholderContent = new PlaceholderContent();
            placeholderContent.addPlaceholderItem(0, "Full name", user.getFullName());
            placeholderContent.addPlaceholderItem(0, "Phone Number", user.getPhoneNo());
            placeholderContent.addPlaceholderItem(0, "Email", user.getEmail());
            if (user.getOtp() != null && user.getOtp().length() > 0) {
                placeholderContent.addPlaceholderItem(0, "Delivery Verification OTP", user.getOtp());
            }
            recyclerView.setAdapter(new AccountRecyclerViewAdapter(placeholderContent.ITEMS, (view12, position) -> {
                if (position == 0 || position == 2) {
                    Bundle bundle = new Bundle();
                    bundle.putString("ContentType", placeholderContent.ITEMS.get(position).contentType);
                    bundle.putString("ContentDesc", placeholderContent.ITEMS.get(position).contentDescription);
                    Navigation.findNavController(view12).navigate(R.id.action_accountFragment_to_accountEditFragment, bundle);
                } else if (position == 1) {
                    Toast.makeText(context, "Phone number cannot be changed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Verification OTP is read-only and automatically updated", Toast.LENGTH_SHORT).show();
                }
            }));
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}