package com.example.fuelonwheelsapp.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionInflater;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fuelonwheelsapp.viewModels.FOWViewModel;
import com.example.fuelonwheelsapp.R;

public class OrderFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // View References
    private TextView tabFuel, tabEv, tabRepair, tabSos;
    private LinearLayout containerFuel, containerEv, containerRepair, containerSos;
    private TextView tvOrderTitle, tvOrderSubtitle, tvOrderEtaBadge;

    // Fuel Controls
    private Button petrolAdd, dieselAdd, petrolMinus, petrolPlus, dieselMinus, dieselPlus;
    private TextView petrolQuant, dieselQuant;
    private int petrolQuantInt = 0;
    private int dieselQuantInt = 0;

    // EV Controls
    private TextView btnEvCar, btnEvBike;
    private Button btnSelectEvBoost, btnSelectEvTopup;

    // Repair Controls
    private Button btnSelectRepairTyre, btnSelectRepairBattery, btnSelectRepairMinor, btnSelectRepairTowing;

    // Community SOS Controls
    private Button btnSelectSos15, btnSelectSos25;

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
        viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);

        // Header Views
        tvOrderTitle = view.findViewById(R.id.tv_order_title);
        tvOrderSubtitle = view.findViewById(R.id.tv_order_subtitle);
        tvOrderEtaBadge = view.findViewById(R.id.tv_order_eta_badge);

        // Service Tabs
        tabFuel = view.findViewById(R.id.tab_fuel);
        tabEv = view.findViewById(R.id.tab_ev);
        tabRepair = view.findViewById(R.id.tab_repair);
        tabSos = view.findViewById(R.id.tab_sos);

        // Service Containers
        containerFuel = view.findViewById(R.id.container_fuel);
        containerEv = view.findViewById(R.id.container_ev);
        containerRepair = view.findViewById(R.id.container_repair);
        containerSos = view.findViewById(R.id.container_sos);

        // Fuel Controls
        petrolAdd = view.findViewById(R.id.petrol_add);
        petrolMinus = view.findViewById(R.id.petrol_minus);
        petrolQuant = view.findViewById(R.id.petrol_quant);
        petrolPlus = view.findViewById(R.id.petrol_plus);
        dieselAdd = view.findViewById(R.id.diesel_add);
        dieselMinus = view.findViewById(R.id.diesel_minus);
        dieselQuant = view.findViewById(R.id.diesel_quant);
        dieselPlus = view.findViewById(R.id.diesel_plus);

        // EV Controls
        btnEvCar = view.findViewById(R.id.btn_ev_car);
        btnEvBike = view.findViewById(R.id.btn_ev_bike);
        btnSelectEvBoost = view.findViewById(R.id.btn_select_ev_boost);
        btnSelectEvTopup = view.findViewById(R.id.btn_select_ev_topup);

        // Repair Controls
        btnSelectRepairTyre = view.findViewById(R.id.btn_select_repair_tyre);
        btnSelectRepairBattery = view.findViewById(R.id.btn_select_repair_battery);
        btnSelectRepairMinor = view.findViewById(R.id.btn_select_repair_minor);
        btnSelectRepairTowing = view.findViewById(R.id.btn_select_repair_towing);

        // SOS Controls
        btnSelectSos15 = view.findViewById(R.id.btn_select_sos_15);
        btnSelectSos25 = view.findViewById(R.id.btn_select_sos_25);

        // Tab click listeners
        tabFuel.setOnClickListener(v -> switchServiceTab(FOWViewModel.SERVICE_FUEL));
        tabEv.setOnClickListener(v -> switchServiceTab(FOWViewModel.SERVICE_EV));
        tabRepair.setOnClickListener(v -> switchServiceTab(FOWViewModel.SERVICE_REPAIR));
        tabSos.setOnClickListener(v -> switchServiceTab(FOWViewModel.SERVICE_SOS));

        // Setup Fuel listeners
        setupFuelListeners();

        // Setup EV listeners
        setupEvListeners();

        // Setup Repair listeners
        setupRepairListeners();

        // Setup SOS listeners
        setupSosListeners();

        // Initialize to saved ViewModel state or default
        switchServiceTab(viewModel.getSelectedServiceType());
    }

    private void switchServiceTab(int serviceType) {
        viewModel.setSelectedServiceType(serviceType);

        // Reset tab styles
        int activeBg = R.drawable.tab_selected_bg;
        int inactiveBg = R.drawable.tab_unselected_bg;
        int activeText = Color.WHITE;
        int inactiveText = ContextCompat.getColor(requireContext(), R.color.brand_navy);

        tabFuel.setBackgroundResource(serviceType == FOWViewModel.SERVICE_FUEL ? activeBg : inactiveBg);
        tabFuel.setTextColor(serviceType == FOWViewModel.SERVICE_FUEL ? activeText : inactiveText);

        tabEv.setBackgroundResource(serviceType == FOWViewModel.SERVICE_EV ? activeBg : inactiveBg);
        tabEv.setTextColor(serviceType == FOWViewModel.SERVICE_EV ? activeText : inactiveText);

        tabRepair.setBackgroundResource(serviceType == FOWViewModel.SERVICE_REPAIR ? activeBg : inactiveBg);
        tabRepair.setTextColor(serviceType == FOWViewModel.SERVICE_REPAIR ? activeText : inactiveText);

        tabSos.setBackgroundResource(serviceType == FOWViewModel.SERVICE_SOS ? activeBg : inactiveBg);
        tabSos.setTextColor(serviceType == FOWViewModel.SERVICE_SOS ? activeText : inactiveText);

        // Toggle container visibility
        containerFuel.setVisibility(serviceType == FOWViewModel.SERVICE_FUEL ? View.VISIBLE : View.GONE);
        containerEv.setVisibility(serviceType == FOWViewModel.SERVICE_EV ? View.VISIBLE : View.GONE);
        containerRepair.setVisibility(serviceType == FOWViewModel.SERVICE_REPAIR ? View.VISIBLE : View.GONE);
        containerSos.setVisibility(serviceType == FOWViewModel.SERVICE_SOS ? View.VISIBLE : View.GONE);

        // Update headers
        switch (serviceType) {
            case FOWViewModel.SERVICE_FUEL:
                tvOrderTitle.setText("Fuel Refill (Bowsers)");
                tvOrderSubtitle.setText("Doorstep certified fuel dispenser");
                tvOrderEtaBadge.setText("15-20 mins");
                break;
            case FOWViewModel.SERVICE_EV:
                tvOrderTitle.setText("EV Fast-Charge Boost");
                tvOrderSubtitle.setText("Mobile DC fast-charging power van");
                tvOrderEtaBadge.setText("15-18 mins");
                break;
            case FOWViewModel.SERVICE_REPAIR:
                tvOrderTitle.setText("24/7 Roadside Repair");
                tvOrderSubtitle.setText("Mobile mechanic on bike/van to your location");
                tvOrderEtaBadge.setText("8-12 mins");
                break;
            case FOWViewModel.SERVICE_SOS:
                tvOrderTitle.setText("Community Fuel SOS");
                tvOrderSubtitle.setText("Nearby drivers share emergency fuel for bounty");
                tvOrderEtaBadge.setText("4-8 mins");
                break;
        }
    }

    private void setupFuelListeners() {
        if (viewModel.getPetrolQuantInt() != null && viewModel.getPetrolQuantInt() > 0) {
            petrolQuantInt = viewModel.getPetrolQuantInt();
        }
        if (viewModel.getDieselQuantInt() != null && viewModel.getDieselQuantInt() > 0) {
            dieselQuantInt = viewModel.getDieselQuantInt();
        }

        updatePetrolQuant(petrolQuantInt);
        updateDieselQuant(dieselQuantInt);

        petrolAdd.setOnClickListener(v -> {
            petrolQuantInt = 5;
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
            dieselQuantInt = 5;
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

    private void setupEvListeners() {
        btnEvCar.setOnClickListener(v -> {
            viewModel.setEvVehicleType("4-Wheeler (CCS2)");
            btnEvCar.setBackgroundResource(R.drawable.tab_selected_bg);
            btnEvCar.setTextColor(Color.WHITE);
            btnEvBike.setBackgroundResource(R.drawable.tab_unselected_bg);
            btnEvBike.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_navy));
        });

        btnEvBike.setOnClickListener(v -> {
            viewModel.setEvVehicleType("2-Wheeler (16A)");
            btnEvBike.setBackgroundResource(R.drawable.tab_selected_bg);
            btnEvBike.setTextColor(Color.WHITE);
            btnEvCar.setBackgroundResource(R.drawable.tab_unselected_bg);
            btnEvCar.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_navy));
        });

        btnSelectEvBoost.setOnClickListener(v -> {
            viewModel.setEvPlanTitle("SOS Emergency Boost (10 kWh)");
            viewModel.setEvKwh(10);
            viewModel.setEvPrice(249);
            btnSelectEvBoost.setText("✓ SELECTED");
            btnSelectEvTopup.setText("+ SELECT");
            Toast.makeText(requireContext(), "Selected: 10 kWh SOS Boost (Rs. 249)", Toast.LENGTH_SHORT).show();
        });

        btnSelectEvTopup.setOnClickListener(v -> {
            viewModel.setEvPlanTitle("Full Battery Top-Up (25 kWh)");
            viewModel.setEvKwh(25);
            viewModel.setEvPrice(549);
            btnSelectEvTopup.setText("✓ SELECTED");
            btnSelectEvBoost.setText("+ SELECT");
            Toast.makeText(requireContext(), "Selected: 25 kWh Full Top-Up (Rs. 549)", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRepairListeners() {
        btnSelectRepairTyre.setOnClickListener(v -> {
            selectRepairService("🛞 Flat Tyre / Puncture Repair", 199, btnSelectRepairTyre);
        });

        btnSelectRepairBattery.setOnClickListener(v -> {
            selectRepairService("🔋 12V Battery Jumpstart", 249, btnSelectRepairBattery);
        });

        btnSelectRepairMinor.setOnClickListener(v -> {
            selectRepairService("🛠️ Minor Mechanical / Clutch Fix", 349, btnSelectRepairMinor);
        });

        btnSelectRepairTowing.setOnClickListener(v -> {
            selectRepairService("🚜 Emergency Flatbed Towing", 999, btnSelectRepairTowing);
        });
    }

    private void selectRepairService(String title, int price, Button selectedBtn) {
        viewModel.setRepairIssueTitle(title);
        viewModel.setRepairPrice(price);

        btnSelectRepairTyre.setText("+ SELECT");
        btnSelectRepairBattery.setText("+ SELECT");
        btnSelectRepairMinor.setText("+ SELECT");
        btnSelectRepairTowing.setText("+ SELECT");

        selectedBtn.setText("✓ SELECTED");
        Toast.makeText(requireContext(), "Selected: " + title + " (Rs. " + price + ")", Toast.LENGTH_SHORT).show();
    }

    private void setupSosListeners() {
        btnSelectSos15.setOnClickListener(v -> {
            viewModel.setSosTitle("🚨 Biker SOS Fuel (1.5L)");
            viewModel.setSosPrice(250);
            btnSelectSos15.setText("✓ SELECTED");
            btnSelectSos25.setText("+ REQUEST");
            Toast.makeText(requireContext(), "Selected: 1.5L SOS Fuel (Rs. 250)", Toast.LENGTH_SHORT).show();
        });

        btnSelectSos25.setOnClickListener(v -> {
            viewModel.setSosTitle("🚨 Biker SOS Fuel (2.5L)");
            viewModel.setSosPrice(350);
            btnSelectSos25.setText("✓ SELECTED");
            btnSelectSos15.setText("+ REQUEST");
            Toast.makeText(requireContext(), "Selected: 2.5L SOS Fuel (Rs. 350)", Toast.LENGTH_SHORT).show();
        });
    }
}
