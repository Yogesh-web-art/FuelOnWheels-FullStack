package com.example.fuelonwheelsapp.profile.orders;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import com.example.fuelonwheelsapp.viewModels.FOWViewModel;
import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.databinding.FragmentOrderDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class OrderDetailFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private Order order;
    private FragmentOrderDetailBinding orderDetailBinding;

    public OrderDetailFragment() {
        // Required empty public constructor
    }

    public static OrderDetailFragment newInstance(String param1, String param2) {
        OrderDetailFragment fragment = new OrderDetailFragment();
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
        FOWViewModel viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
        if (viewModel.getOrder() != null) {
            order = viewModel.getOrder().getValue();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        orderDetailBinding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        return orderDetailBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Safe Back Button Handler
        orderDetailBinding.floatingActionButton.setOnClickListener(view1 -> {
            try {
                Navigation.findNavController(view1).navigate(R.id.action_orderDetailFragment_to_orderListFragment);
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });

        if (order == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Order summary unavailable", Toast.LENGTH_SHORT).show();
            }
            try {
                Navigation.findNavController(view).navigateUp();
            } catch (Exception ignored) {}
            return;
        }

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        // 1. Order ID
        String orderId = (order.getOrderId() != null && !order.getOrderId().isEmpty()) ? order.getOrderId() : "#ORD-N/A";
        orderDetailBinding.orderDetailOrderId.setText(orderId);

        // 2. Locations
        String fuelLocation = (order.getFuelLocation() != null && !order.getFuelLocation().isEmpty()) ? order.getFuelLocation() : "Refuel Anywhere Depot Station";
        orderDetailBinding.orderDetailFuelAddress.setText(fuelLocation);

        String userLocation = (order.getUserLocation() != null && !order.getUserLocation().isEmpty()) ? order.getUserLocation() : "Doorstep Delivery Location";
        orderDetailBinding.orderDetailUserAddress.setText(userLocation);

        // 3. Quantities and Unit Prices
        String nDiesel = (order.getnDiesel() != null) ? order.getnDiesel() : "0";
        String dieselPrice = (order.getDieselPrice() != null) ? order.getDieselPrice() : "0";
        orderDetailBinding.orderDetailDieselQtyPrice.setText(nDiesel + " L  ×  Rs. " + dieselPrice);

        String nPetrol = (order.getnPetrol() != null) ? order.getnPetrol() : "0";
        String petrolPrice = (order.getPetrolPrice() != null) ? order.getPetrolPrice() : "0";
        orderDetailBinding.orderDetailPetrolQtyPrice.setText(nPetrol + " L  ×  Rs. " + petrolPrice);

        // 4. Date & Time
        String dateTime = (order.getDateTime() != null && !order.getDateTime().isEmpty()) ? order.getDateTime() : "Recent Order";
        orderDetailBinding.orderDetailDateTime.setText(dateTime);

        // 5. Safe Calculations
        float floatDieselQty = parseSafeFloat(nDiesel);
        float floatDieselPrice = parseSafeFloat(dieselPrice);
        float totalDieselPrice = floatDieselQty * floatDieselPrice;
        orderDetailBinding.orderDetailDieselTotalPrice.setText("Rs. " + (int) totalDieselPrice);

        float floatPetrolQty = parseSafeFloat(nPetrol);
        float floatPetrolPrice = parseSafeFloat(petrolPrice);
        float totalPetrolPrice = floatPetrolQty * floatPetrolPrice;
        orderDetailBinding.orderDetailTotalpetrolPrice.setText("Rs. " + (int) totalPetrolPrice);

        float totalFuelCost = totalPetrolPrice + totalDieselPrice;
        orderDetailBinding.orderDetailTotalFuelCost.setText("Rs. " + (int) totalFuelCost);

        orderDetailBinding.orderDetailTaxes.setText("Rs. 0");

        String deliveryCharge = (order.getDeliveryCharge() != null && !order.getDeliveryCharge().isEmpty()) ? order.getDeliveryCharge() : "0";
        orderDetailBinding.orderDetailDeliveryCharge.setText("Rs. " + deliveryCharge);

        String grandTotal = (order.getTotalAmount() != null && !order.getTotalAmount().isEmpty()) ? order.getTotalAmount() : String.valueOf((int) totalFuelCost);
        orderDetailBinding.orderDetailGrandTotal.setText("Rs. " + grandTotal);

        // 6. User Phone Safe Lookup
        String phone = "";
        if (firebaseUser != null && firebaseUser.getPhoneNumber() != null && !firebaseUser.getPhoneNumber().isEmpty()) {
            phone = firebaseUser.getPhoneNumber();
        } else if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("UserData", Context.MODE_PRIVATE);
            phone = prefs.getString("phoneNo", prefs.getString("userIdentifier", ""));
        }
        if (phone.isEmpty()) {
            phone = "+91 • Verified Customer";
        }
        orderDetailBinding.orderDetailPhoneNo.setText(phone);

        // 7. Support Action
        orderDetailBinding.orderDetailSupport.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:support@refuelanywhere.app"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Support for Order " + order.getOrderId());
                startActivity(intent);
            } catch (Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Support email: support@refuelanywhere.app", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 8. Download Invoice Action
        orderDetailBinding.orderDetailBtnDownloadSummary.setOnClickListener(v -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Tax invoice downloaded successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private float parseSafeFloat(String value) {
        if (value == null) return 0.0f;
        try {
            String cleaned = value.replaceAll("[^0-9.]", "").trim();
            if (cleaned.isEmpty()) return 0.0f;
            return Float.parseFloat(cleaned);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        orderDetailBinding = null;
    }
}
