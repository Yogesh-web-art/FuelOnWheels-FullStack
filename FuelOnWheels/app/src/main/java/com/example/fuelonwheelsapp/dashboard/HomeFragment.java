package com.example.fuelonwheelsapp.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
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
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String MY_PREFS_NAME = "UserData";
    private static final String TAG = "HomeFragment";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    // High-Resolution OpenStreetMap Humanitarian (HOT) Tile Source (Fast global CDN, sharp road labels, rich vibrant cartography, zero watermarks)
    public static final XYTileSource OSM_HOT = new XYTileSource(
            "OpenStreetMapHOT",
            0, 19, 256, ".png",
            new String[]{
                    "https://a.tile.openstreetmap.fr/hot/",
                    "https://b.tile.openstreetmap.fr/hot/"
            },
            "© OpenStreetMap contributors, Humanitarian OpenStreetMap Team"
    );

    private FOWViewModel viewModel;
    private MapView map = null;
    private boolean isHotLayer = true;
    private IMapController mapController;
    private LocationManager locationManager;
    private TextView tvCurrentAddress;
    private View fabRecenter;
    private GeoPoint currentUserGeoPoint;
    private GeoPoint lastGeocodedPoint;
    private boolean hasCenteredMap = false;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback fusedLocationCallback;
    private Polygon accuracyCircle = null;

    // Reusable single thread executor for geocoding to prevent thread leaks
    private final ExecutorService geoExecutor = Executors.newSingleThreadExecutor();

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

        Context ctx = getContext();
        if (ctx != null) {
            IConfigurationProvider config = Configuration.getInstance();
            config.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
            config.setUserAgentValue("FuelOnWheelsApp/1.0 (support@fuelonwheels.org)");
            config.setTileDownloadThreads((short) 5);
            config.setCacheMapTileCount((short) 48);
            config.setCacheMapTileOvershoot((short) 24);
            config.setTileFileSystemCacheMaxBytes(300L * 1024 * 1024);
            config.setTileFileSystemCacheTrimBytes(240L * 1024 * 1024);
        }

        if (getActivity() != null) {
            viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        configureMap(view);

        tvCurrentAddress = view.findViewById(R.id.tv_current_address);
        fabRecenter = view.findViewById(R.id.fab_recenter);

        if (fabRecenter != null) {
            fabRecenter.setOnClickListener(v -> {
                if (currentUserGeoPoint != null && mapController != null) {
                    mapController.animateTo(currentUserGeoPoint, 17.5, 500L);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Centered on your GPS location", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    configureLocationManager();
                }
            });
        }

        // Floating Map Controls: Zoom In, Zoom Out, Map Layers
        View btnZoomIn = view.findViewById(R.id.btn_zoom_in);
        if (btnZoomIn != null) {
            btnZoomIn.setOnClickListener(v -> {
                if (mapController != null) {
                    mapController.zoomIn();
                }
            });
        }

        View btnZoomOut = view.findViewById(R.id.btn_zoom_out);
        if (btnZoomOut != null) {
            btnZoomOut.setOnClickListener(v -> {
                if (mapController != null) {
                    mapController.zoomOut();
                }
            });
        }

        View btnMapLayer = view.findViewById(R.id.btn_map_layer);
        if (btnMapLayer != null) {
            btnMapLayer.setOnClickListener(v -> toggleMapLayer());
        }

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        configureLocationManager();
        return view;
    }

    private void toggleMapLayer() {
        if (map == null || getContext() == null) return;
        isHotLayer = !isHotLayer;
        if (isHotLayer) {
            map.setTileSource(OSM_HOT);
            Toast.makeText(getContext(), "Map: Humanitarian High-Detail", Toast.LENGTH_SHORT).show();
        } else {
            map.setTileSource(TileSourceFactory.MAPNIK);
            Toast.makeText(getContext(), "Map: OpenStreetMap Standard", Toast.LENGTH_SHORT).show();
        }
        map.getTileProvider().clearTileCache();
        map.invalidate();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (viewModel == null && getActivity() != null) {
            viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
        }

        if (viewModel != null) {
            viewModel.getRoad().observe(getViewLifecycleOwner(), road -> {
                if (road != null && map != null) {
                    renderRoadAndStation(road);
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
                        driverMarker.setAnchor(0.5f, 0.5f);
                        if (isAdded()) {
                            Drawable truckIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_map_delivery_truck, null);
                            if (truckIcon != null) {
                                driverMarker.setIcon(truckIcon);
                            }
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
                            if (overlay instanceof Polyline &&
                                    ("road".equals(((Polyline) overlay).getId()) || "roadCasing".equals(((Polyline) overlay).getId()))) {
                                map.getOverlays().remove(i);
                            } else if (overlay instanceof Marker &&
                                    ("fuelMarker".equals(((Marker) overlay).getId()) || "driverMarker".equals(((Marker) overlay).getId()))) {
                                map.getOverlays().remove(i);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error removing overlays: " + e.getMessage());
                        }
                    }
                    map.invalidate();
                }
            });
        }
    }

    private void renderRoadAndStation(Road road) {
        if (road == null || map == null || !isAdded()) return;

        // Remove existing road / casing / station markers
        for (int i = map.getOverlays().size() - 1; i >= 0; i--) {
            Overlay o = map.getOverlays().get(i);
            if (o instanceof Polyline && ("road".equals(((Polyline) o).getId()) || "roadCasing".equals(((Polyline) o).getId()))) {
                map.getOverlays().remove(i);
            } else if (o instanceof Marker && "fuelMarker".equals(((Marker) o).getId())) {
                map.getOverlays().remove(i);
            }
        }

        // 1. Station Marker
        Drawable stationPin = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_map_pin_station, null);
        Marker poiMarker = new Marker(map);
        poiMarker.setTitle("Fuel Dispenser Station");
        if (viewModel != null) {
            poiMarker.setSnippet(viewModel.getFuelLocation());
            if (viewModel.getFuelCoordinates().getValue() != null) {
                poiMarker.setPosition(viewModel.getFuelCoordinates().getValue());
            }
        }
        if (stationPin != null) {
            poiMarker.setIcon(stationPin);
        }
        poiMarker.setAnchor(0.5f, 0.90f);
        poiMarker.setId("fuelMarker");
        map.getOverlays().add(poiMarker);

        // 2. Soft glowing underlay aura/casing
        Polyline roadCasing = RoadManager.buildRoadOverlay(road);
        roadCasing.setId("roadCasing");
        Paint casingPaint = roadCasing.getOutlinePaint();
        casingPaint.setColor(0x352563EB); // Translucent blue glow
        casingPaint.setStrokeWidth(34.0f);
        casingPaint.setStrokeCap(Paint.Cap.ROUND);
        casingPaint.setStrokeJoin(Paint.Join.ROUND);
        casingPaint.setAntiAlias(true);
        map.getOverlays().add(roadCasing);

        // 3. Crisp electric-blue route foreground
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
        roadOverlay.setId("road");
        Paint roadPaint = roadOverlay.getOutlinePaint();
        roadPaint.setColor(0xFF2563EB); // Vibrant Royal/Electric Blue
        roadPaint.setStrokeWidth(16.0f);
        roadPaint.setStrokeCap(Paint.Cap.ROUND);
        roadPaint.setStrokeJoin(Paint.Join.ROUND);
        roadPaint.setAntiAlias(true);
        map.getOverlays().add(roadOverlay);

        map.invalidate();

        // 4. Smooth auto-framing with padding
        if (road.mBoundingBox != null) {
            map.post(() -> {
                try {
                    if (map != null) {
                        map.zoomToBoundingBox(road.mBoundingBox, true, 200);
                    }
                } catch (Exception ignored) {}
            });
        }
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
        if (!isAdded() || getActivity() == null) return;

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsIfNecessary(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        // 1. Google Play Services Fused Location Provider (high precision, low battery, smooth)
        boolean fusedSuccess = false;
        try {
            if (fusedLocationClient == null && getActivity() != null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            }

            if (fusedLocationCallback == null) {
                fusedLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        if (locationResult == null) return;
                        Location bestLoc = locationResult.getLastLocation();
                        if (bestLoc != null) {
                            updateLocation(bestLoc);
                        }
                    }
                };
            }

            if (fusedLocationClient != null) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        updateLocation(location);
                    }
                });

                LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
                        .setMinUpdateIntervalMillis(1500L)
                        .setMinUpdateDistanceMeters(2.0f)
                        .build();

                fusedLocationClient.requestLocationUpdates(locationRequest, fusedLocationCallback, Looper.getMainLooper());
                fusedSuccess = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "FusedLocationProvider error: " + e.getMessage());
        }

        // 2. Hardware GPS fallback only if FusedLocationClient failed
        if (!fusedSuccess && locationManager != null) {
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
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 2.0f, locationListener);
                }
            } catch (Exception e) {
                Log.e(TAG, "LocationManager fallback error: " + e.getMessage());
            }
        }
    }

    private void updateLocation(Location loc) {
        if (loc == null || map == null || !isAdded()) return;

        // Ignore inaccurate fixes (> 80 meters) when we already have an accurate fix
        if (currentUserGeoPoint != null && loc.hasAccuracy() && loc.getAccuracy() > 80.0f) {
            return;
        }

        GeoPoint geoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        currentUserGeoPoint = geoPoint;
        if (viewModel != null) {
            viewModel.setUserGeoPoint(geoPoint);
        }

        // Dynamic GPS Accuracy Circle
        if (accuracyCircle == null) {
            accuracyCircle = new Polygon(map);
            accuracyCircle.setId("accuracyCircle");
            accuracyCircle.setFillColor(0x152563EB); // Soft translucent blue
            accuracyCircle.setStrokeColor(0x402563EB);
            accuracyCircle.setStrokeWidth(2.0f);
            map.getOverlays().add(0, accuracyCircle);
        }
        if (loc.hasAccuracy() && loc.getAccuracy() > 0) {
            accuracyCircle.setPoints(Polygon.pointsAsCircle(geoPoint, Math.max(loc.getAccuracy(), 15.0)));
            accuracyCircle.setVisible(true);
        } else {
            accuracyCircle.setVisible(false);
        }

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
            startMarker.setAnchor(0.5f, 0.90f);
            Drawable markerIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_map_pin_user, null);
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

        if (viewModel != null) {
            if (viewModel.getOrderLocation() == null || viewModel.getOrderLocation().startsWith("Lat:")) {
                viewModel.setOrderLocation(fallbackAdd);
            }
            viewModel.setUserGeoPoint(geoPoint);
        }

        if (tvCurrentAddress != null && (tvCurrentAddress.getText() == null || tvCurrentAddress.getText().toString().isEmpty() || tvCurrentAddress.getText().toString().startsWith("Lat:"))) {
            tvCurrentAddress.setText(fallbackAdd);
        }

        if (startMarker != null) {
            startMarker.setSnippet(fallbackAdd);
            startMarker.setSubDescription(fallbackAdd);
        }

        if (!hasCenteredMap && mapController != null) {
            hasCenteredMap = true;
            mapController.setZoom(17.5);
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

        // Throttle geocoding: only perform if moved > 30 meters from last geocoded point or on first fix
        if (lastGeocodedPoint != null && lastGeocodedPoint.distanceToAsDouble(geoPoint) < 30.0) {
            return;
        }
        lastGeocodedPoint = geoPoint;

        // Perform Geocoding on shared background thread pool
        geoExecutor.execute(() -> {
            try {
                Context bgContext = getContext();
                if (bgContext == null) return;
                Geocoder geocoder = new Geocoder(bgContext, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty() && addresses.get(0).getAddressLine(0) != null) {
                    String fullAdd = addresses.get(0).getAddressLine(0);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!isAdded()) return;
                        if (viewModel != null) {
                            viewModel.setOrderLocation(fullAdd);
                        }
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
        if (!isAdded() || getActivity() == null) return;
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
        if (map == null) return;

        // 1. High-Detail OSM HOT tiles (Humanitarian OpenStreetMap: fast, clean, zero watermarks)
        map.setTileSource(OSM_HOT);
        map.setDestroyMode(false);

        // 2. High-DPI scaling: scales tiles to the device's native screen density for crisp clarity
        map.setTilesScaledToDpi(true);
        map.setMultiTouchControls(true);
        map.setFlingEnabled(true);

        // 3. Hide default clunky zoom buttons
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        mapController = map.getController();

        // 4. Two-finger rotation gesture
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(map);
        mRotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(mRotationGestureOverlay);

        // 5. Compass Overlay
        Context ctx = getContext();
        if (ctx != null) {
            try {
                CompassOverlay compassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx), map);
                compassOverlay.enableCompass();
                map.getOverlays().add(compassOverlay);
            } catch (Exception e) {
                Log.w(TAG, "Compass init warning: " + e.getMessage());
            }

            // 6. Distance Scale Bar Overlay (bottom-left alignment)
            try {
                ScaleBarOverlay scaleBar = new ScaleBarOverlay(map);
                scaleBar.setCentred(false);
                scaleBar.setAlignRight(false);
                scaleBar.setAlignBottom(true);
                scaleBar.setScaleBarOffset(40, 260);
                map.getOverlays().add(scaleBar);
            } catch (Exception e) {
                Log.w(TAG, "ScaleBar init warning: " + e.getMessage());
            }
        }

        map.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        Context ctx = getContext();
        if (ctx != null) {
            Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
            Configuration.getInstance().setUserAgentValue("FuelOnWheelsApp/1.0 (support@fuelonwheels.org)");
        }
        if (map != null) {
            map.onResume();
        }

        if (viewModel != null && viewModel.getRoad().getValue() != null && viewModel.getDelivered().getValue() != null) {
            if (!viewModel.getDelivered().getValue()) {
                renderRoadAndStation(viewModel.getRoad().getValue());
            }
        }

        if (isAdded() && getActivity() != null) {
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                configureLocationManager();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Context ctx = getContext();
        if (ctx != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            Configuration.getInstance().save(ctx, prefs);
        }
        if (map != null) {
            map.onPause();
        }
        if (fusedLocationClient != null && fusedLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(fusedLocationCallback);
        }
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onDestroyView() {
        if (map != null) {
            map.onDetach();
            map = null;
        }
        accuracyCircle = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        geoExecutor.shutdown();
        super.onDestroy();
    }
}
