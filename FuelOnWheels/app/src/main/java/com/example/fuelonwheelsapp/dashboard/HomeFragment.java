package com.example.fuelonwheelsapp.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
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
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.viewModels.FOWViewModel;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

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

import java.io.File;
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
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 2001;

    // 1. Google Maps Roadmap Tile Source (Crystal-clear Google Maps cartography, zero watermarks, zero 403 blocks)
    public static final org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase GOOGLE_ROADMAP =
            new org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                    "GoogleRoadmap",
                    0, 20, 256, ".png",
                    new String[]{
                            "https://mt0.google.com/vt/lyrs=m&hl=en",
                            "https://mt1.google.com/vt/lyrs=m&hl=en",
                            "https://mt2.google.com/vt/lyrs=m&hl=en",
                            "https://mt3.google.com/vt/lyrs=m&hl=en"
                    },
                    "© Google"
            ) {
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl() +
                            "&x=" + org.osmdroid.util.MapTileIndex.getX(pMapTileIndex) +
                            "&y=" + org.osmdroid.util.MapTileIndex.getY(pMapTileIndex) +
                            "&z=" + org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex);
                }
            };

    // 2. Google Maps Hybrid Satellite Tile Source (Crisp satellite imagery with labeled streets)
    public static final org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase GOOGLE_HYBRID =
            new org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                    "GoogleHybrid",
                    0, 20, 256, ".png",
                    new String[]{
                            "https://mt0.google.com/vt/lyrs=y&hl=en",
                            "https://mt1.google.com/vt/lyrs=y&hl=en",
                            "https://mt2.google.com/vt/lyrs=y&hl=en",
                            "https://mt3.google.com/vt/lyrs=y&hl=en"
                    },
                    "© Google"
            ) {
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl() +
                            "&x=" + org.osmdroid.util.MapTileIndex.getX(pMapTileIndex) +
                            "&y=" + org.osmdroid.util.MapTileIndex.getY(pMapTileIndex) +
                            "&z=" + org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex);
                }
            };

    // 3. High-Detail OpenStreetMap Humanitarian (Global CDN, 100% free, zero watermarks)
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
    private int currentLayerIndex = 0;
    private IMapController mapController;
    private LocationManager locationManager;
    private TextView tvCurrentAddress;
    private TextView tvGpsBadge;
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
            config.setTileDownloadThreads((short) 6);
            config.setCacheMapTileCount((short) 64);
            config.setCacheMapTileOvershoot((short) 32);
            config.setTileFileSystemCacheMaxBytes(350L * 1024 * 1024);
            config.setTileFileSystemCacheTrimBytes(280L * 1024 * 1024);
            purgeOldWatermarkCache(ctx);
        }

        if (getActivity() != null) {
            viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void purgeOldWatermarkCache(Context ctx) {
        if (ctx == null) return;
        try {
            File cacheDir = Configuration.getInstance().getOsmdroidTileCache();
            if (cacheDir != null && cacheDir.exists()) {
                File[] children = cacheDir.listFiles();
                if (children != null) {
                    for (File f : children) {
                        String name = f.getName().toLowerCase();
                        if (name.contains("carto") || name.contains("mapnik")) {
                            deleteFolderRecursively(f);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void deleteFolderRecursively(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteFolderRecursively(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        configureMap(view);

        tvCurrentAddress = view.findViewById(R.id.tv_current_address);
        tvGpsBadge = view.findViewById(R.id.tv_gps_badge);
        fabRecenter = view.findViewById(R.id.fab_recenter);

        View locationCard = view.findViewById(R.id.home_location_card);
        if (locationCard != null) {
            locationCard.setOnClickListener(v -> checkAndPromptLocation());
        }

        if (fabRecenter != null) {
            fabRecenter.setOnClickListener(v -> {
                if (currentUserGeoPoint != null && mapController != null) {
                    mapController.animateTo(currentUserGeoPoint, 17.5, 500L);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Centered on your GPS location", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    checkAndPromptLocation();
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

        return view;
    }

    private void toggleMapLayer() {
        if (map == null || getContext() == null) return;
        currentLayerIndex = (currentLayerIndex + 1) % 3;
        switch (currentLayerIndex) {
            case 0:
                map.setTileSource(GOOGLE_ROADMAP);
                map.setTilesScaledToDpi(false);
                Toast.makeText(getContext(), "Map: Google Maps Roadmap", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                map.setTileSource(GOOGLE_HYBRID);
                map.setTilesScaledToDpi(false);
                Toast.makeText(getContext(), "Map: Google Maps Satellite / Hybrid", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                map.setTileSource(OSM_HOT);
                map.setTilesScaledToDpi(false);
                Toast.makeText(getContext(), "Map: OpenStreetMap Humanitarian", Toast.LENGTH_SHORT).show();
                break;
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

        // Whenever user comes to map section, actively check & prompt location
        checkAndPromptLocation();
    }

    public void checkAndPromptLocation() {
        if (!isAdded() || getActivity() == null) return;

        // Step 1: Check runtime permissions
        boolean fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted || !coarseGranted) {
            updateGpsBadge(false, "ENABLE GPS");
            if (tvCurrentAddress != null) {
                tvCurrentAddress.setText("Tap to allow location access...");
            }
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE
            );
            return;
        }

        // Step 2: Check device GPS toggle (Hardware / Google Play Services)
        checkDeviceLocationSettingsAndStart();
    }

    private void checkDeviceLocationSettingsAndStart() {
        if (!isAdded() || getActivity() == null) return;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(2.0f)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true); // Always pop up the Google Play Services location dialog if GPS is OFF

        SettingsClient client = LocationServices.getSettingsClient(requireActivity());
        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(requireActivity(), response -> {
                    // Location settings are satisfied, start GPS tracking
                    updateGpsBadge(true, "GPS ACTIVE");
                    startLocationTracking(locationRequest);
                })
                .addOnFailureListener(requireActivity(), e -> {
                    updateGpsBadge(false, "ENABLE GPS");
                    if (tvCurrentAddress != null && currentUserGeoPoint == null) {
                        tvCurrentAddress.setText("Device GPS is turned off. Tap to enable.");
                    }
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            startIntentSenderForResult(
                                    resolvable.getResolution().getIntentSender(),
                                    REQUEST_CHECK_SETTINGS,
                                    null, 0, 0, 0, null
                            );
                        } catch (Exception sendEx) {
                            showFallbackGpsDialog();
                        }
                    } else {
                        showFallbackGpsDialog();
                    }
                });
    }

    private void showFallbackGpsDialog() {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Enable Device Location")
                .setMessage("Refuel Anywhere requires your GPS location to pinpoint your exact fuel delivery spot.\n\nPlease turn on location services.")
                .setPositiveButton("Turn On Location", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    } catch (Exception ex) {
                        Toast.makeText(getContext(), "Please enable Location in Phone Settings", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(getContext(), "GPS disabled. Tap the target button on the map to retry.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                // User enabled location via Google prompt!
                if (getContext() != null) {
                    Toast.makeText(getContext(), "GPS location enabled", Toast.LENGTH_SHORT).show();
                }
                checkDeviceLocationSettingsAndStart();
            } else {
                showFallbackGpsDialog();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean granted = false;
            for (int res : grantResults) {
                if (res == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                checkDeviceLocationSettingsAndStart();
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Location permission is required for doorstep fuel delivery", Toast.LENGTH_LONG).show();
                }
                loadSavedFallbackLocation();
            }
        }
    }

    private void updateGpsBadge(boolean active, String text) {
        if (tvGpsBadge == null || !isAdded()) return;
        tvGpsBadge.setText(text);
        if (active) {
            tvGpsBadge.setBackgroundResource(R.drawable.badge_pill_blue);
            tvGpsBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pill_blue_text));
        } else {
            tvGpsBadge.setBackgroundResource(R.drawable.badge_pill_amber);
            tvGpsBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pill_amber_text));
        }
    }

    private void startLocationTracking(LocationRequest locationRequest) {
        if (!isAdded() || getActivity() == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            if (fusedLocationClient == null) {
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

            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    updateLocation(location);
                }
            });

            fusedLocationClient.requestLocationUpdates(locationRequest, fusedLocationCallback, Looper.getMainLooper());
        } catch (Exception e) {
            Log.e(TAG, "FusedLocationProvider start error: " + e.getMessage());
            startHardwareLocationFallback();
        }
    }

    private void startHardwareLocationFallback() {
        if (locationManager == null || !isAdded()) return;
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

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
            Log.e(TAG, "Hardware location fallback error: " + e.getMessage());
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            updateLocation(location);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            updateGpsBadge(false, "ENABLE GPS");
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            updateGpsBadge(true, "GPS ACTIVE");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    private void updateLocation(Location loc) {
        if (loc == null || map == null || !isAdded()) return;

        // Ignore inaccurate fixes (> 80 meters) when we already have an accurate fix
        if (currentUserGeoPoint != null && loc.hasAccuracy() && loc.getAccuracy() > 80.0f) {
            return;
        }

        GeoPoint geoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        currentUserGeoPoint = geoPoint;
        updateGpsBadge(true, "GPS ACTIVE");

        if (viewModel != null) {
            viewModel.setUserGeoPoint(geoPoint);
        }

        // Dynamic GPS Accuracy Circle
        if (accuracyCircle == null) {
            accuracyCircle = new Polygon(map);
            accuracyCircle.setId("accuracyCircle");
            accuracyCircle.setFillColor(0x182563EB); // Soft translucent blue
            accuracyCircle.setStrokeColor(0x502563EB);
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

        if (tvCurrentAddress != null && (tvCurrentAddress.getText() == null || tvCurrentAddress.getText().toString().isEmpty() || tvCurrentAddress.getText().toString().startsWith("Detecting") || tvCurrentAddress.getText().toString().startsWith("Lat:") || tvCurrentAddress.getText().toString().startsWith("Tap") || tvCurrentAddress.getText().toString().startsWith("Device GPS"))) {
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

    private void loadSavedFallbackLocation() {
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

    private void configureMap(View view) {
        map = (MapView) view.findViewById(R.id.map);
        if (map == null) return;

        // Wipe out any old CartoDB watermark files and 403 blocked Mapnik files from disk cache
        purgeOldWatermarkCache(getContext());

        // 1. Crystal-Clear Google Maps Roadmap Tiles (Direct, fast, zero watermarks, zero 403 blocks)
        map.setTileSource(GOOGLE_ROADMAP);
        map.setDestroyMode(false);

        // 2. Crisp 1:1 pixel rendering - zero stretching, zero blurriness
        map.setTilesScaledToDpi(false);
        map.setMultiTouchControls(true);
        map.setFlingEnabled(true);
        map.setMinZoomLevel(4.0);
        map.setMaxZoomLevel(20.0);

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

        // Check & prompt location whenever user comes back to this map section
        checkAndPromptLocation();
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
