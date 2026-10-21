package com.example.fuelonwheelsapp.viewModels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fuelonwheelsapp.dashboard.User;
import com.example.fuelonwheelsapp.interfaces.LoadOrderListCallback;
import com.example.fuelonwheelsapp.interfaces.OrderCallback;
import com.example.fuelonwheelsapp.profile.orders.Order;
import com.example.fuelonwheelsapp.repositories.FOWRepository;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.GeoPoint;

public class FOWViewModel extends ViewModel {

    final FOWRepository repository = new FOWRepository();

    private final MutableLiveData<Integer> petrolQuantInt = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> dieselQuantInt = new MutableLiveData<>(0);
    private final MutableLiveData<String> orderLocation = new MutableLiveData<>();
    private final MutableLiveData<GeoPoint> userLocation = new MutableLiveData<>();
    private final MutableLiveData<GeoPoint> fuelCoordinates = new MutableLiveData<>();
    private final MutableLiveData<String> fuelLocation = new MutableLiveData<>();
    private final MutableLiveData<Boolean> buildRoad = new MutableLiveData<>();
    private final MutableLiveData<Road> road = new MutableLiveData<>();
    private final MutableLiveData<Boolean> delivered = new MutableLiveData<>();
    private final MutableLiveData<Order> order = new MutableLiveData<>();
    private final MutableLiveData<User> user = new MutableLiveData<>();

    // Real-Time Delivery tracking LiveData
    private final MutableLiveData<GeoPoint> driverCoordinates = new MutableLiveData<>();
    private final MutableLiveData<String> liveOrderStatus = new MutableLiveData<>();
    private final MutableLiveData<String> liveEta = new MutableLiveData<>();
    private final MutableLiveData<String> driverDetails = new MutableLiveData<>();

    public GeoPoint getUserGeoPoint() {
        return userLocation.getValue();
    }

    public void setUserGeoPoint(GeoPoint geoPoint) {
        this.userLocation.setValue(geoPoint);
    }

    public void setPetrolQuantInt(Integer petrolQuantInt) {
        this.petrolQuantInt.setValue(petrolQuantInt);
    }

    public Integer getPetrolQuantInt() {
        Integer val = petrolQuantInt.getValue();
        return val != null ? val : 0;
    }

    public void setDieselQuantInt(Integer dieselQuantInt) {
        this.dieselQuantInt.setValue(dieselQuantInt != null ? dieselQuantInt : 0);
    }

    public String getOrderLocation() {
        return orderLocation.getValue();
    }

    public void setOrderLocation(String orderLocation) {
        this.orderLocation.setValue(orderLocation);
    }

    public Integer getDieselQuantInt() {
        Integer val = dieselQuantInt.getValue();
        return val != null ? val : 0;
    }

    public void setFuelLocation(String address) {
        this.fuelLocation.setValue(address);
    }

    public String getFuelLocation() {
        return this.fuelLocation.getValue();
    }

    public void getResponseUsingCallback(Context context, GeoPoint geoPoint, Order order, OrderCallback callback) {
        repository.getResponseFromUsingCallback(context, geoPoint, order, callback);
    }

    public void getResponseAfterFetchingOrderList(LoadOrderListCallback callback) {
        repository.getOrderListFromDatabase(callback);
    }

    public LiveData<User> getResponseAsUserProfile() {
        return repository.getUserProfileResponseFromDatabase();
    }

    public LiveData<Boolean> updateUserProfile(String field, String data) {
        return repository.updateUserData(field, data);
    }

    public LiveData<GeoPoint> getFuelCoordinates() {
        return fuelCoordinates;
    }

    public void setFuelCoordinates(GeoPoint geoPoint) {
        this.fuelCoordinates.setValue(geoPoint);
    }

    public LiveData<Boolean> getBuildRoad() {
        return buildRoad;
    }

    public void setBuildRoad(Boolean bool) {
        this.buildRoad.setValue(bool);
    }

    public LiveData<Road> getRoad() {
        return road;
    }

    public void setRoad(Road road) {
        this.road.setValue(road);
    }

    public MutableLiveData<Boolean> getDelivered() {
        return delivered;
    }

    public void setDelivered(Boolean b) {
        this.delivered.setValue(b);
    }

    public MutableLiveData<Order> getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order.setValue(order);
    }

    public MutableLiveData<User> getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user.setValue(user);
    }

    // Real-Time Delivery tracking getters and setters
    public LiveData<GeoPoint> getDriverCoordinates() {
        return driverCoordinates;
    }

    public void setDriverCoordinates(GeoPoint point) {
        this.driverCoordinates.setValue(point);
    }

    public LiveData<String> getLiveOrderStatus() {
        return liveOrderStatus;
    }

    public void setLiveOrderStatus(String status) {
        this.liveOrderStatus.setValue(status);
    }

    public LiveData<String> getLiveEta() {
        return liveEta;
    }

    public void setLiveEta(String eta) {
        this.liveEta.setValue(eta);
    }

    public LiveData<String> getDriverDetails() {
        return driverDetails;
    }

    public void setDriverDetails(String details) {
        this.driverDetails.setValue(details);
    }
}
