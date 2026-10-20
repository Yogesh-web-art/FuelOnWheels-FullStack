package com.example.fuelonwheelsapp.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fuelonwheelsapp.dashboard.Response;
import com.example.fuelonwheelsapp.dashboard.User;
import com.example.fuelonwheelsapp.interfaces.LoadOrderListCallback;
import com.example.fuelonwheelsapp.interfaces.OrderCallback;
import com.example.fuelonwheelsapp.profile.orders.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.bonuspack.location.NominatimPOIProvider;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FOWRepository {
    private static final String TAG = "FOWRepository";
    private Context context;
    private String fuelLocation;
    private GeoPoint fuelCoordinates;

    private final DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
    private final DatabaseReference orderReference = rootReference.child("orderTree");
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public FOWRepository() {
    }

    private DatabaseReference getProfileReference() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return rootReference.child("users").child(user.getUid());
        }
        return rootReference.child("users").child("guest");
    }

    public void getResponseFromUsingCallback(Context context, GeoPoint geoPoint, Order order, OrderCallback orderCallback) {
        Response response = new Response();
        this.context = context;
        if (geoPoint == null) {
            response.exception = "Location not available";
            orderCallback.onResponse(response);
            return;
        }

        // Modern background execution replacing deprecated AsyncTask
        executorService.execute(() -> {
            try {
                NominatimPOIProvider poiProvider = new NominatimPOIProvider(context.getPackageName());
                ArrayList<POI> pois = poiProvider.getPOICloseTo(geoPoint, "petrol", 50, 0.1);
                double radius = 0.1;
                while (pois.isEmpty() && radius <= 1.0) {
                    pois = poiProvider.getPOICloseTo(geoPoint, "petrol", 50, radius);
                    radius += 0.1;
                }

                if (pois.isEmpty()) {
                    mainHandler.post(() -> {
                        response.exception = "Sorry! Service not available at your location";
                        orderCallback.onResponse(response);
                    });
                    return;
                }

                POI poi = pois.get(0);
                fuelCoordinates = poi.mLocation;
                fuelLocation = poi.mDescription;
                order.setFuelLocation(poi.mDescription);

                ArrayList<GeoPoint> waypoints = new ArrayList<>();
                waypoints.add(geoPoint);
                waypoints.add(fuelCoordinates);

                RoadManager roadManager = new OSRMRoadManager(context, context.getPackageName());
                Road road = roadManager.getRoad(waypoints);

                mainHandler.post(() -> {
                    response.address = fuelLocation;
                    response.fuelCoordinates = fuelCoordinates;
                    response.road = road;

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
                    String currentDateandTime = sdf.format(new Date());
                    String phone = null;
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        phone = currentUser.getPhoneNumber();
                    }
                    String suffix = (phone != null && phone.length() > 3) ? phone.substring(3) :
                            (currentUser != null ? currentUser.getUid().substring(0, Math.min(5, currentUser.getUid().length())) : "guest");
                    String orderId = "O" + currentDateandTime + suffix;
                    response.orderId = orderId;

                    sdf = new SimpleDateFormat("dd-MM-yyyy|HH:mm:ss:SSS", Locale.getDefault());
                    String dateTime = sdf.format(new Date());

                    order.setDateTime(dateTime);
                    order.setOrderId(orderId);
                    order.setUserId(currentUser != null ? currentUser.getUid() : "guest");
                    order.setOrderStatus("PLACED");

                    Log.d(TAG, "Order generated: " + orderId);
                    saveOrderData(orderCallback, order, response);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in POI or road calculation: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    response.exception = "Failed to calculate delivery route: " + e.getMessage();
                    orderCallback.onResponse(response);
                });
            }
        });
    }

    private void saveOrderData(OrderCallback orderCallback, Order order, Response response) {
        String orderId = order.getOrderId();
        order.setOrderId(null);
        orderReference.child(orderId).setValue(order)
                .addOnSuccessListener(unused -> {
                    response.orderSavedToDatabase = true;
                    saveOrderHistoryToProfile(orderCallback, orderId, response);
                    Log.d(TAG, "Order Saved to DB successfully");
                })
                .addOnFailureListener(e -> {
                    response.orderSavedToDatabase = false;
                    response.exception = e.getMessage();
                    orderCallback.onResponse(response);
                    Log.d(TAG, Objects.requireNonNull(e.getMessage()));
                });
    }

    private void saveOrderHistoryToProfile(OrderCallback orderCallback, String orderId, Response response) {
        getProfileReference().child("orders").child(orderId).setValue(true)
                .addOnSuccessListener(unused -> {
                    response.orderSavedToDatabase = true;
                    Log.d(TAG, "OrderHistory Saved to Profile successfully");
                    orderCallback.onResponse(response);
                })
                .addOnFailureListener(e -> {
                    response.orderSavedToDatabase = false;
                    response.exception = e.getMessage();
                    orderCallback.onResponse(response);
                    Log.d(TAG, Objects.requireNonNull(e.getMessage()));
                });
    }

    public void getOrderListFromDatabase(LoadOrderListCallback loadOrderListCallback) {
        ArrayList<Order> orderArrayList = new ArrayList<>();
        ArrayList<String> finalOrderIds = new ArrayList<>();
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        finalOrderIds.add(orderSnapshot.getKey());
                        getOrderListWithDetailFromDatabase(loadOrderListCallback, finalOrderIds);
                    }
                } else {
                    loadOrderListCallback.onResponse(orderArrayList, "ListEmpty");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadOrderListCallback.onResponse(orderArrayList, error.getMessage());
            }
        };
        getProfileReference().child("orders").addValueEventListener(valueEventListener);
    }

    private void getOrderListWithDetailFromDatabase(LoadOrderListCallback loadOrderListCallback, ArrayList<String> orderIds) {
        ArrayList<Order> orderArrayList = new ArrayList<>();
        final String[] exception = {null};
        orderReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (String id : orderIds) {
                        Order order = snapshot.child(id).getValue(Order.class);
                        if (order != null) {
                            order.setOrderId(id);
                            orderArrayList.add(order);
                        }
                    }
                    loadOrderListCallback.onResponse(orderArrayList, exception[0]);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                exception[0] = error.getMessage();
            }
        });
    }

    public MutableLiveData<User> getUserProfileResponseFromDatabase() {
        MutableLiveData<User> mutableLiveData = new MutableLiveData<>();
        User[] user = {new User()};
        getProfileReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    user[0] = snapshot.getValue(User.class);
                }
                mutableLiveData.setValue(user[0]);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        return mutableLiveData;
    }

    public LiveData<Boolean> updateUserData(String field, String data) {
        MutableLiveData<Boolean> booleanMutableLiveData = new MutableLiveData<>();
        getProfileReference().child(field).setValue(data)
                .addOnSuccessListener(unused -> {
                    booleanMutableLiveData.setValue(true);
                    Log.d(TAG, "Updated successfully");
                })
                .addOnFailureListener(e -> {
                    booleanMutableLiveData.setValue(false);
                    Log.d(TAG, Objects.requireNonNull(e.getMessage()));
                });
        return booleanMutableLiveData;
    }
}
