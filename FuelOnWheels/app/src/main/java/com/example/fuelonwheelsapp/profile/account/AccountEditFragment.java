package com.example.fuelonwheelsapp.profile.account;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.databinding.FragmentAccountEditBinding;
import com.example.fuelonwheelsapp.viewModels.FOWViewModel;

public class AccountEditFragment extends Fragment {

    private static final String ARG_PARAM1 = "ContentType";
    private static final String ARG_PARAM2 = "ContentDesc";

    private String contentType = "Full Name";
    private String contentDescription = "";

    private FOWViewModel viewModel;
    private FragmentAccountEditBinding binding;

    public AccountEditFragment() {
        // Required empty public constructor
    }

    public static AccountEditFragment newInstance(String param1, String param2) {
        AccountEditFragment fragment = new AccountEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String cType = getArguments().getString(ARG_PARAM1);
            if (cType != null) contentType = cType;
            String cDesc = getArguments().getString(ARG_PARAM2);
            if (cDesc != null) contentDescription = cDesc;
        }
        viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAccountEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.accountEditContentType.setText("Update " + contentType);
        binding.textInputLayout.setHint(contentType);
        binding.accountEditEditName.setText(contentDescription);

        binding.accountEditBtnBack.setOnClickListener(view12 -> {
            try {
                Navigation.findNavController(view12).navigate(R.id.action_accountEditFragment_to_accountFragment);
            } catch (Exception ignored) {}
        });

        binding.accountEditBtnUpdate.setOnClickListener(view1 -> {
            if (binding.accountEditEditName.getText() == null) return;
            String data = binding.accountEditEditName.getText().toString().trim();

            if (data.length() <= 1) {
                Toast.makeText(view1.getContext(), "Please enter valid information", Toast.LENGTH_SHORT).show();
                return;
            }

            String field = "";
            if ("full name".equalsIgnoreCase(contentType)) {
                field = "fullName";
            } else if ("email".equalsIgnoreCase(contentType)) {
                field = "email";
            } else {
                Toast.makeText(view1.getContext(), "Action not supported", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.accountEditSubContainer.setAlpha(0.5f);
            binding.progressBar2.setVisibility(View.VISIBLE);
            binding.accountEditEditName.setEnabled(false);
            binding.accountEditBtnUpdate.setEnabled(false);

            final String fieldName = field;
            final String finalData = data;
            viewModel.updateUserProfile(fieldName, finalData).observe(getViewLifecycleOwner(), aBoolean -> {
                if (!isAdded()) return;
                binding.progressBar2.setVisibility(View.GONE);
                binding.accountEditEditName.setEnabled(true);
                binding.accountEditBtnUpdate.setEnabled(true);
                binding.accountEditSubContainer.setAlpha(1.0f);
                if (Boolean.TRUE.equals(aBoolean)) {
                    try {
                        requireContext().getSharedPreferences("UserData", android.content.Context.MODE_PRIVATE)
                                .edit().putString(fieldName, finalData).apply();
                    } catch (Exception ignored) {}
                    Toast.makeText(view1.getContext(), contentType + " updated successfully!", Toast.LENGTH_SHORT).show();
                    try {
                        Navigation.findNavController(view1).navigate(R.id.action_accountEditFragment_to_accountFragment);
                    } catch (Exception ignored) {}
                } else {
                    Toast.makeText(view1.getContext(), "Could not update. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
