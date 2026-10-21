package com.example.fuelonwheelsapp.dashboard;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.fuelonwheelsapp.viewModels.FOWViewModel;
import com.example.fuelonwheelsapp.R;

public class PaymentFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private TextView petrolXQuant, dieselXQuant, petrolPrice, dieselPrice, totalPrice;
    private View petrolRow, dieselRow;

    public PaymentFragment() {
        super(R.layout.fragment_payment);
    }

    public static PaymentFragment newInstance(String param1, String param2) {
        PaymentFragment fragment = new PaymentFragment();
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
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.slide_up));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FOWViewModel viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
        petrolXQuant = view.findViewById(R.id.petrol_x_quant);
        dieselXQuant = view.findViewById(R.id.diesel_x_quant);
        petrolRow = view.findViewById(R.id.petrol_row);
        dieselRow = view.findViewById(R.id.diesel_row);
        petrolPrice = view.findViewById(R.id.petrol_price);
        dieselPrice = view.findViewById(R.id.diesel_price);
        totalPrice = view.findViewById(R.id.total_price);

        int petrol = (viewModel.getPetrolQuantInt() != null) ? viewModel.getPetrolQuantInt() : 0;
        int diesel = (viewModel.getDieselQuantInt() != null) ? viewModel.getDieselQuantInt() : 0;
        int total = (petrol * 100) + (diesel * 94) + 50;

        petrolXQuant.setText("Petrol x " + petrol + "L");
        petrolPrice.setText("₹" + (petrol * 100));
        dieselXQuant.setText("Diesel x " + diesel + "L");
        dieselPrice.setText("₹" + (diesel * 94));
        totalPrice.setText("₹" + total);

        petrolRow.setVisibility(petrol > 0 ? View.VISIBLE : View.GONE);
        dieselRow.setVisibility(diesel > 0 ? View.VISIBLE : View.GONE);
    }
}
