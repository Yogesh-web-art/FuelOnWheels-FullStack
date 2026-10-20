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

import java.util.Objects;

public class AccountEditFragment extends Fragment {

    private static final String ARG_PARAM1 = "ContentType";
    private static final String ARG_PARAM2 = "ContentDesc";


    private String contentType;
    private String contentDescription;

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
            contentType = getArguments().getString(ARG_PARAM1);
            contentDescription = getArguments().getString(ARG_PARAM2);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAccountEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.accountEditContentType.setText(getString(R.string.update_content_type, contentType));
        binding.textInputLayout.setHint(contentType);
        binding.accountEditEditName.setText(contentDescription);
        binding.accountEditBtnBack.setOnClickListener(view12 -> Navigation.findNavController(view12).navigate(R.id.action_accountEditFragment_to_accountFragment));
        binding.accountEditBtnUpdate.setOnClickListener(view1 -> {
            binding.accountEditSubContainer.setAlpha(0.5f);
            binding.progressBar2.setVisibility(View.VISIBLE);
            binding.accountEditEditName.setEnabled(false);
            binding.accountEditBtnUpdate.setEnabled(false);

            String data = Objects.requireNonNull(binding.accountEditEditName.getText()).toString();
            if(data.length()<=1){
                Toast.makeText(view1.getContext(), "Enter correct credentials", Toast.LENGTH_SHORT).show();
            }
            else{

                String field="";
                switch (contentType) {
                    case "Full name":
                        field ="fullName";break;
                    case "Email":
                        field ="email"; break;
                    default: Toast.makeText(view1.getContext(), "Action not allowed", Toast.LENGTH_SHORT).show();
                }
                if(field.equals("")){
                    Toast.makeText(view1.getContext(), "Sorry action could not be performed!", Toast.LENGTH_SHORT).show();
                }
                else{
                    viewModel.updateUserProfile(field, data).observe(getViewLifecycleOwner(), aBoolean -> {
                        binding.progressBar2.setVisibility(View.GONE);
                        binding.accountEditEditName.setEnabled(false);
                        binding.accountEditBtnUpdate.setEnabled(false);
                        binding.accountEditSubContainer.setAlpha(1.0f);
                        if(aBoolean){
                            Toast.makeText(view1.getContext(), contentType+" updated successfully!", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(view1).navigate(R.id.action_accountEditFragment_to_accountFragment);
                        }
                        else{
                            Toast.makeText(view1.getContext(), "Sorry action could not be performed!", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        });
    }
}