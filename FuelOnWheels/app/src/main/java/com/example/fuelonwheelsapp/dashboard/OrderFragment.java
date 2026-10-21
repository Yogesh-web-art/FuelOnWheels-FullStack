package com.example.fuelonwheelsapp.dashboard;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionInflater;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.fuelonwheelsapp.viewModels.FOWViewModel;
import com.example.fuelonwheelsapp.R;

public class OrderFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private Button petrolAdd, dieselAdd, petrolMinus, petrolPlus, dieselMinus, dieselPlus;
    private TextView petrolQuant, dieselQuant;
    private int petrolQuantInt = 0;
    private int dieselQuantInt = 0;
    private FOWViewModel viewModel;

    public OrderFragment() {
        super(R.layout.fragment_order);
    }

    public static OrderFragment newInstance(String param1, String param2) {
        OrderFragment fragment = new OrderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.slide_up));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        petrolAdd = view.findViewById(R.id.petrol_add);
        petrolMinus = view.findViewById(R.id.petrol_minus);
        petrolQuant = view.findViewById(R.id.petrol_quant);
        petrolPlus = view.findViewById(R.id.petrol_plus);
        dieselAdd = view.findViewById(R.id.diesel_add);
        dieselMinus = view.findViewById(R.id.diesel_minus);
        dieselQuant = view.findViewById(R.id.diesel_quant);
        dieselPlus = view.findViewById(R.id.diesel_plus);
        viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);

        // Pre-load from viewModel if already selected
        if (viewModel.getPetrolQuantInt() != null && viewModel.getPetrolQuantInt() > 0) {
            petrolQuantInt = viewModel.getPetrolQuantInt();
        }
        if (viewModel.getDieselQuantInt() != null && viewModel.getDieselQuantInt() > 0) {
            dieselQuantInt = viewModel.getDieselQuantInt();
        }

        updatePetrolQuant(petrolQuantInt);
        updateDieselQuant(dieselQuantInt);

        petrolAdd.setOnClickListener(v -> {
            petrolQuantInt = 5; // Default 5L on 1st tap (standard minimum for fuel delivery)
            updatePetrolQuant(petrolQuantInt);
        });

        petrolMinus.setOnClickListener(v -> {
            if (petrolQuantInt > 1) {
                petrolQuantInt--;
            } else {
                petrolQuantInt = 0;
            }
            updatePetrolQuant(petrolQuantInt);
        });

        petrolPlus.setOnClickListener(v -> {
            if (petrolQuantInt < 100) {
                petrolQuantInt++;
                updatePetrolQuant(petrolQuantInt);
            }
        });

        dieselAdd.setOnClickListener(v -> {
            dieselQuantInt = 5; // Default 5L on 1st tap
            updateDieselQuant(dieselQuantInt);
        });

        dieselMinus.setOnClickListener(v -> {
            if (dieselQuantInt > 1) {
                dieselQuantInt--;
            } else {
                dieselQuantInt = 0;
            }
            updateDieselQuant(dieselQuantInt);
        });

        dieselPlus.setOnClickListener(v -> {
            if (dieselQuantInt < 200) {
                dieselQuantInt++;
                updateDieselQuant(dieselQuantInt);
            }
        });
    }

    private void updatePetrolQuant(int quant) {
        petrolQuantInt = quant;
        if (quant <= 0) {
            petrolMinus.setVisibility(View.GONE);
            petrolQuant.setVisibility(View.GONE);
            petrolPlus.setVisibility(View.GONE);
            petrolAdd.setVisibility(View.VISIBLE);
        } else {
            petrolMinus.setVisibility(View.VISIBLE);
            petrolQuant.setVisibility(View.VISIBLE);
            petrolPlus.setVisibility(View.VISIBLE);
            petrolAdd.setVisibility(View.GONE);
            petrolQuant.setText(quant + "L");
        }
        viewModel.setPetrolQuantInt(quant);
    }

    private void updateDieselQuant(int quant) {
        dieselQuantInt = quant;
        if (quant <= 0) {
            dieselMinus.setVisibility(View.GONE);
            dieselQuant.setVisibility(View.GONE);
            dieselPlus.setVisibility(View.GONE);
            dieselAdd.setVisibility(View.VISIBLE);
        } else {
            dieselMinus.setVisibility(View.VISIBLE);
            dieselQuant.setVisibility(View.VISIBLE);
            dieselPlus.setVisibility(View.VISIBLE);
            dieselAdd.setVisibility(View.GONE);
            dieselQuant.setText(quant + "L");
        }
        viewModel.setDieselQuantInt(quant);
    }
}
