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

    // Service Types: 0 = Fuel (Petrol/Diesel), 1 = EV Fast-Charge, 2 = Roadside Repair, 3 = Community SOS
    public static final int SERVICE_FUEL = 0;
    public static final int SERVICE_EV = 1;
    public static final int SERVICE_REPAIR = 2;
    public static final int SERVICE_SOS = 3;

    private final MutableLiveData<Integer> selectedServiceType = new MutableLiveData<>(SERVICE_FUEL);

    // EV Charging State
    private final MutableLiveData<String> evVehicleType = new MutableLiveData<>("4-Wheeler (CCS2)");
    private final MutableLiveData<String> evPlanTitle = new MutableLiveData<>("");
    private final MutableLiveData<Integer> evKwh = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> evPrice = new MutableLiveData<>(0);

    // Roadside Repair / Mechanic State
    private final MutableLiveData<String> repairIssueTitle = new MutableLiveData<>("");
    private final MutableLiveData<Integer> repairPrice = new MutableLiveData<>(0);

    // Community SOS State
    private final MutableLiveData<String> sosTitle = new MutableLiveData<>("");
    private final MutableLiveData<Integer> sosPrice = new MutableLiveData<>(0);

    public int getSelectedServiceType() {
        Integer val = selectedServiceType.getValue();
        return val != null ? val : SERVICE_FUEL;
    }

    public void setSelectedServiceType(int type) {
        this.selectedServiceType.setValue(type);
    }

    public String getEvVehicleType() {
        String val = evVehicleType.getValue();
        return val != null ? val : "4-Wheeler (CCS2)";
    }

    public void setEvVehicleType(String type) {
        this.evVehicleType.setValue(type);
    }

    public String getEvPlanTitle() {
        String val = evPlanTitle.getValue();
        return val != null ? val : "";
    }

    public void setEvPlanTitle(String title) {
        this.evPlanTitle.setValue(title);
    }

    public int getEvKwh() {
        Integer val = evKwh.getValue();
        return val != null ? val : 0;
    }

    public void setEvKwh(int kwh) {
        this.evKwh.setValue(kwh);
    }

    public int getEvPrice() {
        Integer val = evPrice.getValue();
        return val != null ? val : 0;
    }

    public void setEvPrice(int price) {
        this.evPrice.setValue(price);
    }

    public String getRepairIssueTitle() {
        String val = repairIssueTitle.getValue();
        return val != null ? val : "";
    }

    public void setRepairIssueTitle(String title) {
        this.repairIssueTitle.setValue(title);
    }

    public int getRepairPrice() {
        Integer val = repairPrice.getValue();
        return val != null ? val : 0;
    }

    public void setRepairPrice(int price) {
        this.repairPrice.setValue(price);
    }

    public String getSosTitle() {
        String val = sosTitle.getValue();
        return val != null ? val : "";
    }

    public void setSosTitle(String title) {
        this.sosTitle.setValue(title);
    }

    public int getSosPrice() {
        Integer val = sosPrice.getValue();
        return val != null ? val : 0;
    }

    public void setSosPrice(int price) {
        this.sosPrice.setValue(price);
    }
}
