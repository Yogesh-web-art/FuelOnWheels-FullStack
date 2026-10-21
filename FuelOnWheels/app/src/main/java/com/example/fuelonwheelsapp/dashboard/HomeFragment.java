package com.example.fuelonwheelsapp.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.viewModels.FOWViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.Locale;
import android.os.Handler;

public class HomeFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String MY_PREFS_NAME = "UserData";

    private FOWViewModel viewModel;

    private static final String TAG = "HomeFragment";
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private static final XYTileSource OSM_DE = new XYTileSource("OSMDe", 0, 19, 256, ".png", new String[] { "https://tile.openstreetmap.de/" }, "OpenStreetMap contributors");
    private Context ctx;
    private IMapController mapController;
    private LocationManager locationManager;
    private TextView tvCurrentAddress;
    private View fabRecenter;
    private GeoPoint currentUserGeoPoint;
    private boolean hasCenteredMap = false;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback fusedLocationCallback;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        ctx = getContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue("FuelOnWheelsApp/1.0 (support@fuelonwheels.org)");
        viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        configureMap(view);

        tvCurrentAddress = view.findViewById(R.id.tv_current_address);
        fabRecenter = view.findViewById(R.id.fab_recenter);

        if (fabRecenter != null) {
            fabRecenter.setOnClickListener(v -> {
                if (currentUserGeoPoint != null && mapController != null) {
                    mapController.animateTo(currentUserGeoPoint, 17.0, 500L);
                    Toast.makeText(requireContext(), "Centered on your GPS location", Toast.LENGTH_SHORT).show();
                } else {
                    configureLocationManager();
                }
            });
        }

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        configureLocationManager();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");

        viewModel.getRoad().observe(getViewLifecycleOwner(), road -> {
            Log.d(TAG, "In viewModel.getRoad().observe");
            if (road != null && map != null) {
                Log.d(TAG, "Road variable is NOT null");
                Drawable poiIcon = ResourcesCompat.getDrawable(getResources(), org.osmdroid.library.R.drawable.marker_default_focused_base, null);
                Marker poiMarker = new Marker(map);
                poiMarker.setTitle("Fuel");
                poiMarker.setSnippet(viewModel.getFuelLocation());
                poiMarker.setPosition(viewModel.getFuelCoordinates().getValue());
                poiMarker.setIcon(poiIcon);
                poiMarker.setId("fuelMarker");
                map.getOverlays().add(poiMarker);
                Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                roadOverlay.setId("road");
                roadOverlay.getOutlinePaint().setStrokeWidth(30.0f);
                map.getOverlays().add(roadOverlay);
                map.invalidate();
            }
        });

        viewModel.getDriverCoordinates().observe(getViewLifecycleOwner(), geoPoint -> {
            if (geoPoint != null && map != null) {
                Marker driverMarker = null;
                for (int i = 0; i < map.getOverlays().size(); i++) {
                    Overlay overlay = map.getOverlays().get(i);
                    if (overlay instanceof Marker && "driverMarker".equals(((Marker) overlay).getId())) {
                        driverMarker = (Marker) overlay;
                        break;
                    }
                }
                if (driverMarker == null) {
                    driverMarker = new Marker(map);
                    driverMarker.setId("driverMarker");
                    driverMarker.setTitle("Fuel Delivery Partner");
                    driverMarker.setSnippet("En route with your fuel");
                    driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    Drawable truckIcon = ResourcesCompat.getDrawable(getResources(), org.osmdroid.library.R.drawable.osm_ic_follow_me, null);
                    if (truckIcon != null) {
                        driverMarker.setIcon(truckIcon);
                    }
                    map.getOverlays().add(driverMarker);
                }
                driverMarker.setPosition(geoPoint);
                map.invalidate();
            }
        });

        viewModel.getDelivered().observe(getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean != null && aBoolean && map != null) {
                for (int i = map.getOverlays().size() - 1; i >= 0; i--) {
                    try {
                        Overlay overlay = map.getOverlays().get(i);
                        if (overlay instanceof Polyline && "road".equals(((Polyline) overlay).getId())) {
                            map.getOverlays().remove(i);
                        } else if (overlay instanceof Marker &&
                                ("fuelMarker".equals(((Marker) overlay).getId()) || "driverMarker".equals(((Marker) overlay).getId()))) {
                            map.getOverlays().remove(i);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                map.invalidate();
            }
        });
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            updateLocation(location);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {}

        @Override
        public void onProviderEnabled(@NonNull String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    private void configureLocationManager() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsIfNecessary(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
            return;
        }

        // 1. Google Play Services Fused Location Provider (high precision, low battery, emulator friendly)
        try {
            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            }

            if (fusedLocationCallback == null) {
                fusedLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        if (locationResult == null) return;
                        for (Location location : locationResult.getLocations()) {
                            if (location != null) {
                                updateLocation(location);
                                break;
                            }
                        }
                    }
                };
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    updateLocation(location);
                }
            });

            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                    .setMinUpdateIntervalMillis(1500L)
                    .setMinUpdateDistanceMeters(2.0f)
                    .build();

            fusedLocationClient.requestLocationUpdates(locationRequest, fusedLocationCallback, Looper.getMainLooper());
        } catch (Exception e) {
            Log.e(TAG, "FusedLocationProvider error: " + e.getMessage());
        }

        // 2. Hardware GPS / Network fallback
        try {
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (lastKnown != null && currentUserGeoPoint == null) {
                updateLocation(lastKnown);
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 2.0f, locationListener);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 2.0f, locationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "LocationManager fallback error: " + e.getMessage());
        }
    }

    private void updateLocation(Location loc) {
        if (loc == null || map == null) return;
        Log.d(TAG, "updateLocation: lat=" + loc.getLatitude() + ", lon=" + loc.getLongitude());
        GeoPoint geoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        currentUserGeoPoint = geoPoint;
        viewModel.setUserGeoPoint(geoPoint);

        Marker startMarker = null;
        for (int i = 0; i < map.getOverlays().size(); i++) {
            Overlay overlay = map.getOverlays().get(i);
            if (overlay instanceof Marker && "userMarker".equals(((Marker) overlay).getId())) {
                startMarker = (Marker) overlay;
                break;
            }
        }

        if (startMarker == null) {
            startMarker = new Marker(map);
            startMarker.setId("userMarker");
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            Drawable markerIcon = ResourcesCompat.getDrawable(getResources(), org.osmdroid.library.R.drawable.marker_default, null);
            if (markerIcon != null) {
                startMarker.setIcon(markerIcon);
            }
            startMarker.setTitle("My Location");
            map.getOverlays().add(startMarker);
        }

        startMarker.setPosition(geoPoint);
        generateLocation(geoPoint, startMarker);
    }

    private void generateLocation(GeoPoint geoPoint, Marker startMarker) {
        String fallbackAdd = "Lat: " + String.format(Locale.US, "%.4f", geoPoint.getLatitude()) +
                ", Lng: " + String.format(Locale.US, "%.4f", geoPoint.getLongitude());

        viewModel.setOrderLocation(fallbackAdd);
        viewModel.setUserGeoPoint(geoPoint);

        if (tvCurrentAddress != null) {
            tvCurrentAddress.setText(fallbackAdd);
        }

        if (startMarker != null) {
            startMarker.setSnippet(fallbackAdd);
            startMarker.setSubDescription(fallbackAdd);
        }

        if (!hasCenteredMap && mapController != null) {
            hasCenteredMap = true;
            mapController.setZoom(16.5);
            mapController.setCenter(geoPoint);
        }

        try {
            Context act = getContext();
            if (act != null) {
                SharedPreferences.Editor editor = act.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putString("userLatitude", String.valueOf(geoPoint.getLatitude()));
                editor.putString("userLongitude", String.valueOf(geoPoint.getLongitude()));
                editor.apply();
            }
        } catch (Exception ignored) {}

        map.invalidate();

        // Perform Geocoding on background thread to prevent UI freezing/ANR
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Context bgContext = getContext();
                if (bgContext == null) return;
                Geocoder geocoder = new Geocoder(bgContext, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty() && addresses.get(0).getAddressLine(0) != null) {
                    String fullAdd = addresses.get(0).getAddressLine(0);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!isAdded()) return;
                        viewModel.setOrderLocation(fullAdd);
                        if (tvCurrentAddress != null) {
                            tvCurrentAddress.setText(fullAdd);
                        }
                        if (startMarker != null) {
                            startMarker.setSnippet(fullAdd);
                            startMarker.setSubDescription(fullAdd);
                        }
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "Geocoder notice: " + e.getMessage());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                configureLocationManager();
            } else {
                // Graceful fallback to default/saved coordinates without recursive dialog loops
                try {
                    Context c = getContext();
                    if (c != null) {
                        SharedPreferences prefs = c.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE);
                        double lat = Double.parseDouble(prefs.getString("userLatitude", "28.6139"));
                        double lon = Double.parseDouble(prefs.getString("userLongitude", "77.2090"));
                        Location fallback = new Location("saved");
                        fallback.setLatitude(lat);
                        fallback.setLongitude(lon);
                        updateLocation(fallback);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void configureMap(View view) {
        map = (MapView) view.findViewById(R.id.map);
        map.setTileSource(OSM_DE);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(map);
        mRotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(mRotationGestureOverlay);
        map.invalidate();
        Log.d(TAG, "in configureMap()");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue("FuelOnWheelsApp/1.0 (support@fuelonwheels.org)");
        map.onResume();

        if (viewModel.getRoad().getValue() != null && viewModel.getDelivered().getValue() != null) {
            if (!viewModel.getDelivered().getValue()) {
                Log.d(TAG, "onResume: handle home click between order");
                Drawable poiIcon = ResourcesCompat.getDrawable(getResources(), org.osmdroid.library.R.drawable.marker_default_focused_base, null);
                Marker poiMarker = new Marker(map);
                poiMarker.setTitle("Fuel");
                poiMarker.setSnippet(viewModel.getFuelLocation());
                poiMarker.setPosition(viewModel.getFuelCoordinates().getValue());
                poiMarker.setIcon(poiIcon);
                poiMarker.setId("fuelMarker");
                map.getOverlays().add(poiMarker);
                Polyline roadOverlay = RoadManager.buildRoadOverlay(viewModel.getRoad().getValue());
                roadOverlay.setId("road");
                roadOverlay.getOutlinePaint().setStrokeWidth(30.0f);
                map.getOverlays().add(roadOverlay);
                map.invalidate();
            }
        }

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            configureLocationManager();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Configuration.getInstance().save(ctx, prefs);
        map.onPause();
        if (fusedLocationClient != null && fusedLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(fusedLocationCallback);
        }
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }
}
