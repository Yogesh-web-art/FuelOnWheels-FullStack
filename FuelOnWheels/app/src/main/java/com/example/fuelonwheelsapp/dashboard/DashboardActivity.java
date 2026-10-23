package com.example.fuelonwheelsapp.dashboard;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Locale;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.profile.orders.Order;
import com.example.fuelonwheelsapp.viewModels.FOWViewModel;

public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";

    private int homeState = 0;
    private Button nextButton;
    private View deliveryDetails;
    private TextView eta;
    private FOWViewModel viewModel;
    private ProgressDialog progressDialog;
    private TextView address;
    private TextView fueladdress;
    private TextView dashboard_tv_distance;
    private TextView delivering_to;
    private String fullName;
    private TextView deliveryCode;

    private DatabaseReference activeOrderRef;
    private ValueEventListener activeOrderListener;
    private final Handler simulationHandler = new Handler(Looper.getMainLooper());

    private void safeDismissProgressDialog() {
        try {
            if (!isFinishing() && !isDestroyed() && progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        org.osmdroid.config.IConfigurationProvider config = org.osmdroid.config.Configuration.getInstance();
        config.setUserAgentValue("FuelOnWheelsApp/1.0 (support@fuelonwheels.org)");
        config.setTileDownloadThreads((short) 5);
        config.setCacheMapTileCount((short) 32);
        config.setCacheMapTileOvershoot((short) 16);
        config.setTileFileSystemCacheMaxBytes(250L * 1024 * 1024);
        config.setTileFileSystemCacheTrimBytes(200L * 1024 * 1024);
        setContentView(R.layout.activity_dashboard);

        Intent intent = getIntent();
        fullName = intent.getStringExtra("fullName");
        if (fullName == null || fullName.trim().isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
            fullName = prefs.getString("fullName", "Valued Customer");
        }

        homeState = 0;
        nextButton = findViewById(R.id.next_button);
        deliveryDetails = findViewById(R.id.delivery_details);
        delivering_to = findViewById(R.id.delivering_to);
        eta = findViewById(R.id.eta);
        dashboard_tv_distance = findViewById(R.id.dashboard_table_tv_distance);
        deliveryCode = findViewById(R.id.delivery_code);
        address = findViewById(R.id.delivery_address);
        fueladdress = findViewById(R.id.fuel_address);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Locating nearest fuel partner...");

        viewModel = new ViewModelProvider(this).get(FOWViewModel.class);

        nextButton.setOnClickListener(v -> {
            switch (homeState) {
                case 0:
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.fragmentContainerView2, OrderFragment.class, null)
                            .addToBackStack("order_setup")
                            .commit();
                    homeState = 1;
                    break;

                case 1:
                    int petrolQ = viewModel.getPetrolQuantInt() != null ? viewModel.getPetrolQuantInt() : 0;
                    int dieselQ = viewModel.getDieselQuantInt() != null ? viewModel.getDieselQuantInt() : 0;
                    if (petrolQ <= 0 && dieselQ <= 0) {
                        Toast.makeText(this, "Please select at least 1 Litre of fuel to continue", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.fragmentContainerView3, PaymentFragment.class, null)
                            .addToBackStack("payment")
                            .commit();
                    homeState = 2;
                    break;

                case 2:
                    Order order = new Order();
                    int p = viewModel.getPetrolQuantInt() != null ? viewModel.getPetrolQuantInt() : 0;
                    int d = viewModel.getDieselQuantInt() != null ? viewModel.getDieselQuantInt() : 0;
                    order.setnDiesel(String.valueOf(d));
                    order.setnPetrol(String.valueOf(p));
                    order.setDeliveryCharge("50");
                    order.setDieselPrice("94");
                    order.setPetrolPrice("100");
                    order.setTotalAmount(String.valueOf((d * 94 + p * 100 + 50)));
                    order.setPaymentMethod("COD");
                    order.setUserLocation(viewModel.getOrderLocation() != null ? viewModel.getOrderLocation() : "Current Location");

                    if (!isFinishing() && !isDestroyed()) {
                        progressDialog.show();
                    }

                    GeoPoint userPt = viewModel.getUserGeoPoint();
                    if (userPt == null) {
                        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
                        try {
                            double lat = Double.parseDouble(prefs.getString("userLatitude", "28.6139"));
                            double lon = Double.parseDouble(prefs.getString("userLongitude", "77.2090"));
                            userPt = new GeoPoint(lat, lon);
                        } catch (Exception ex) {
                            userPt = new GeoPoint(28.6139, 77.2090);
                        }
                        viewModel.setUserGeoPoint(userPt);
                    }

                    viewModel.getResponseUsingCallback(getApplicationContext(), userPt, order, response -> {
                        safeDismissProgressDialog();
                        if (response != null && response.exception == null && response.address != null) {
                            fueladdress.setText(response.address);
                            viewModel.setFuelCoordinates(response.fuelCoordinates);
                            viewModel.setFuelLocation(response.address);
                            viewModel.setRoad(response.road);
                            address.setText(viewModel.getOrderLocation() != null ? viewModel.getOrderLocation() : "Your Location");
                            delivering_to.setText(fullName);

                            // Delivery handshake verification PIN
                            String pin = "4829";
                            if (response.orderId != null && response.orderId.length() >= 4) {
                                int hash = Math.abs(response.orderId.hashCode() % 9000) + 1000;
                                pin = String.valueOf(hash);
                            }
                            deliveryCode.setText(pin);

                            if (response.road != null) {
                                viewModel.setBuildRoad(true);
                                viewModel.setDelivered(false);
                                dashboard_tv_distance.setText(String.format(Locale.getDefault(),
                                        "%.2f km (~%.0f mins)", response.road.mLength, Math.max(1.0, response.road.mDuration / 60)));
                            }

                            nextButton.setVisibility(View.GONE);
                            getSupportFragmentManager().beginTransaction()
                                    .setReorderingAllowed(true)
                                    .add(R.id.fragmentContainerView4, ThankYouFragment.class, null)
                                    .addToBackStack("thank_you")
                                    .commit();

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    nextButton.setVisibility(View.VISIBLE);
                                    getSupportFragmentManager().popBackStack("order_setup", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                }
                            }, 3000);

                            homeState = 0;
                            nextButton.setEnabled(false);
                            deliveryDetails.setVisibility(View.VISIBLE);
                            startRealtimeOrderTracking(response.orderId, response.road);
                        } else {
                            String err = (response != null && response.exception != null) ? response.exception : "Unable to place order. Please try again.";
                            Toast.makeText(DashboardActivity.this, err, Toast.LENGTH_SHORT).show();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    nextButton.setVisibility(View.VISIBLE);
                                    getSupportFragmentManager().popBackStack("order_setup", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                }
                            }, 1000);
                            homeState = 0;
                        }
                    });
                    break;
            }
            updateNxtBtn();
        });

        // Bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.homeFragment) {
                    nextButton.setVisibility(View.VISIBLE);
                    if (homeState == 0 && !nextButton.isEnabled()) {
                        deliveryDetails.setVisibility(View.VISIBLE);
                    }
                } else {
                    nextButton.setVisibility(View.GONE);
                    deliveryDetails.setVisibility(View.GONE);
                }
                getSupportFragmentManager().popBackStack("order_setup", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                homeState = 0;
                updateNxtBtn();

                if (destination.getId() == R.id.orderListFragment || destination.getId() == R.id.orderDetailFragment
                        || destination.getId() == R.id.accountFragment || destination.getId() == R.id.accountEditFragment
                        || destination.getId() == R.id.aboutUsFragment || destination.getId() == R.id.paymentSetUpFragment) {
                    bottomNavigationView.setVisibility(View.GONE);
                } else {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }
            });
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        homeState = Math.max(0, homeState - 1);
        updateNxtBtn();
    }

    private void startRealtimeOrderTracking(String orderId, Road road) {
        if (activeOrderRef != null && activeOrderListener != null) {
            activeOrderRef.removeEventListener(activeOrderListener);
        }
        activeOrderRef = FirebaseDatabase.getInstance().getReference("orderTree").child(orderId);
        activeOrderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                Order liveOrder = snapshot.getValue(Order.class);
                if (liveOrder != null) {
                    String status = liveOrder.getOrderStatus();
                    if (status != null) {
                        switch (status) {
                            case "PLACED":
                                eta.setText("Order Placed. Assigning driver...");
                                break;
                            case "ACCEPTED":
                                eta.setText("Driver: " + (liveOrder.getDriverName() != null ? liveOrder.getDriverName() : "Rajesh Kumar") + " (Assigned)");
                                break;
                            case "EN_ROUTE":
                                String etaText = liveOrder.getEtaMinutes() != null ? "ETA: " + liveOrder.getEtaMinutes() + " mins" : "Fuel En Route";
                                eta.setText(etaText);
                                break;
                            case "DELIVERED":
                                eta.setText("Fuel Delivered Successfully!");
                                viewModel.setDelivered(true);
                                viewModel.setBuildRoad(false);
                                viewModel.setFuelCoordinates(null);
                                viewModel.setFuelLocation(null);
                                viewModel.setRoad(null);
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        deliveryDetails.setVisibility(View.GONE);
                                        nextButton.setEnabled(true);
                                    }
                                }, 3000);
                                break;
                        }
                    }
                    if (liveOrder.getDriverLatitude() != null && liveOrder.getDriverLongitude() != null) {
                        viewModel.setDriverCoordinates(new GeoPoint(liveOrder.getDriverLatitude(), liveOrder.getDriverLongitude()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Order tracking cancelled: " + error.getMessage());
            }
        };

        activeOrderRef.addValueEventListener(activeOrderListener);
        simulateDriverMovementInFirebase(activeOrderRef, road);
    }

    private void simulateDriverMovementInFirebase(DatabaseReference orderRef, Road road) {
        simulationHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            orderRef.child("orderStatus").setValue("ACCEPTED");
            orderRef.child("driverName").setValue("Rajesh Kumar");
            orderRef.child("driverPhone").setValue("+91 98765 43210");
        }, 3000);

        simulationHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            orderRef.child("orderStatus").setValue("EN_ROUTE");

            if (road != null && road.mRouteHigh != null && !road.mRouteHigh.isEmpty()) {
                ArrayList<GeoPoint> points = road.mRouteHigh;
                int totalPoints = points.size();
                int steps = Math.min(totalPoints, 10);
                int interval = Math.max(1, totalPoints / steps);

                for (int i = 0; i < steps; i++) {
                    final int stepIndex = i;
                    final GeoPoint point = points.get(Math.min(stepIndex * interval, totalPoints - 1));
                    final int etaRemain = Math.max(1, (int) Math.round((road.mDuration / 60) * (1.0 - (double) stepIndex / steps)));

                    simulationHandler.postDelayed(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        orderRef.child("driverLatitude").setValue(point.getLatitude());
                        orderRef.child("driverLongitude").setValue(point.getLongitude());
                        orderRef.child("etaMinutes").setValue(String.valueOf(etaRemain));
                    }, (long) (i + 1) * 2500);
                }

                simulationHandler.postDelayed(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    GeoPoint lastPoint = points.get(totalPoints - 1);
                    orderRef.child("driverLatitude").setValue(lastPoint.getLatitude());
                    orderRef.child("driverLongitude").setValue(lastPoint.getLongitude());
                    orderRef.child("orderStatus").setValue("DELIVERED");
                    orderRef.child("etaMinutes").setValue("0");
                }, (long) (steps + 1) * 2500);
            } else {
                simulationHandler.postDelayed(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    orderRef.child("orderStatus").setValue("DELIVERED");
                }, 15000);
            }
        }, 6000);
    }

    public void updateNxtBtn() {
        switch (homeState) {
            case 0:
                nextButton.setText("Order");
                break;
            case 1:
                nextButton.setText("Next");
                break;
            case 2:
                nextButton.setText("Place Order");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (activeOrderRef != null && activeOrderListener != null) {
            try {
                activeOrderRef.removeEventListener(activeOrderListener);
            } catch (Exception ignored) {}
        }
        simulationHandler.removeCallbacksAndMessages(null);
        safeDismissProgressDialog();
        super.onDestroy();
    }
}
