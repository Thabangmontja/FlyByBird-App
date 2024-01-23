package com.example.msimangapart3;

import static com.mapbox.maps.plugin.animation.CameraAnimationsUtils.getCamera;
import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;
import static com.mapbox.navigation.base.extensions.RouteOptionsExtensions.applyDefaultNavigationOptions;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.Bearing;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.bindgen.Expected;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor;
import com.mapbox.maps.plugin.animation.MapAnimationOptions;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.generated.LocationComponentSettings;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.route.NavigationRoute;
import com.mapbox.navigation.base.route.NavigationRouterCallback;
import com.mapbox.navigation.base.route.RouterFailure;
import com.mapbox.navigation.base.route.RouterOrigin;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.core.trip.session.LocationMatcherResult;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer;
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView;
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources;
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivityMap extends AppCompatActivity {

    MapView mapView;
    FloatingActionButton focusLocationBtn;
    ArrayList<String> locationId = new ArrayList<>();
    ArrayList<Double> latitudeList = new ArrayList<>();
    ArrayList<Double> longitudeList = new ArrayList<>();
    ArrayList<String> locationName = new ArrayList<>();
    ArrayList<String> latestSightDate = new ArrayList<>();

    private final NavigationLocationProvider navigationLocationProvider = new NavigationLocationProvider();
    private MapboxRouteLineView routeLineView;
    private MapboxRouteLineApi routeLineApi;

    //handles device location change
    private final LocationObserver locationObserver = new LocationObserver() {
        @Override
        public void onNewRawLocation(@NonNull Location location) {

        }

        @Override
        public void onNewLocationMatcherResult(@NonNull LocationMatcherResult locationMatcherResult) {
            Location location = locationMatcherResult.getEnhancedLocation();
            navigationLocationProvider.changePosition(location, locationMatcherResult.getKeyPoints(), null, null);
            if (focusLocation) {
                updateCamera(Point.fromLngLat(location.getLongitude(), location.getLatitude()), (double) location.getBearing());
            }
        }
    };

    //handles change in route and draws new route
    private final RoutesObserver routesObserver = new RoutesObserver() {
        @Override
        public void onRoutesChanged(@NonNull RoutesUpdatedResult routesUpdatedResult) {
            routeLineApi.setNavigationRoutes(routesUpdatedResult.getNavigationRoutes(), new MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>() {
                @Override
                public void accept(Expected<RouteLineError, RouteSetValue> routeLineErrorRouteSetValueExpected) {
                    Style style = mapView.getMapboxMap().getStyle();
                    if (style != null) {
                        routeLineView.renderRouteDrawData(style, routeLineErrorRouteSetValueExpected);
                    }
                }
            });
        }
    };
    boolean focusLocation = true;
    private MapboxNavigation mapboxNavigation;

    //Move MapHelper based on location
    private void updateCamera(Point point, Double bearing) {
        MapAnimationOptions animationOptions = new MapAnimationOptions.Builder().duration(1500L).build();
        CameraOptions cameraOptions = new CameraOptions.Builder().center(point).zoom(18.0).bearing(bearing).pitch(45.0)
                .padding(new EdgeInsets(1000.0, 0.0, 0.0, 0.0)).build();

        getCamera(mapView).easeTo(cameraOptions, animationOptions);
    }

    //listener to keep location updated based on user movement
    private final OnMoveListener onMoveListener = new OnMoveListener() {
        @Override
        public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {
            focusLocation = false;
            getGestures(mapView).removeOnMoveListener(this);
            focusLocationBtn.show();
        }

        @Override
        public boolean onMove(@NonNull MoveGestureDetector moveGestureDetector) {
            return false;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {

        }
    };
    //requests permissions from user
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                Toast.makeText(MainActivityMap.this, "Permission granted! Restart this app", Toast.LENGTH_SHORT).show();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_map);
        mapView = findViewById(R.id.mapView);
        focusLocationBtn = findViewById(R.id.focusLocation);

        MapboxRouteLineOptions options = new MapboxRouteLineOptions.Builder(this).withRouteLineResources(new RouteLineResources.Builder().build())
                .withRouteLineBelowLayerId(LocationComponentConstants.LOCATION_INDICATOR_LAYER).build();
        routeLineView = new MapboxRouteLineView(options);
        routeLineApi = new MapboxRouteLineApi(options);

        NavigationOptions navigationOptions = new NavigationOptions.Builder(this).accessToken(getString(R.string.mapbox_access_token)).build();

        MapboxNavigationApp.setup(navigationOptions);
        mapboxNavigation = new MapboxNavigation(navigationOptions);

        mapboxNavigation.registerRoutesObserver(routesObserver);
        mapboxNavigation.registerLocationObserver(locationObserver);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(MainActivityMap.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (ActivityCompat.checkSelfPermission(MainActivityMap.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivityMap.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            activityResultLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        } else {
            mapboxNavigation.startTripSession();
        }

        focusLocationBtn.hide();
//        LocationComponentPlugin locationComponentPlugin = getLocationComponent(mapView);
//        getGestures(mapView).addOnMoveListener(onMoveListener);
        //Always load street view first
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS);
        setUpMap();

//        mapView.getMapboxMap().loadStyleUri(Style.SATELLITE, new Style.OnStyleLoaded() {
//            @Override
//            public void onStyleLoaded(@NonNull Style style) {
//                setUpMap();
//                mapView.getMapboxMap().setCamera(new CameraOptions.Builder().zoom(20.0).build());
//                locationComponentPlugin.setEnabled(true);
//                locationComponentPlugin.setLocationProvider(navigationLocationProvider);
//                getGestures(mapView).addOnMoveListener(onMoveListener);
//                locationComponentPlugin.updateSettings(new Function1<LocationComponentSettings, Unit>() {
//                    @Override
//                    public Unit invoke(LocationComponentSettings locationComponentSettings) {
//                        locationComponentSettings.setEnabled(true);
//                        locationComponentSettings.setPulsingEnabled(true);
//                        return null;
//                    }
//                });
//
//                getHotspots();
//                //focus camera on user and add move lister to MapHelper
//                focusLocationBtn.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        focusLocation = true;
//                        getGestures(mapView).addOnMoveListener(onMoveListener);
//                        focusLocationBtn.hide();
//                    }
//                });
//            }
//        });

        ImageButton homeView = findViewById(R.id.homeView);
        ImageButton satelliteView = findViewById(R.id.satelliteView);
        ImageButton settings = findViewById(R.id.settings);
        ImageButton saveBird = findViewById(R.id.saveBird);
        ImageButton viewSaved = findViewById(R.id.viewSaved);

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivityMap.this, Settings.class);
                startActivity(intent);
            }
        });

        saveBird.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivityMap.this, SaveBird.class);
                startActivity(intent);
            }
        });

        viewSaved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivityMap.this, ViewCaptured.class);
                startActivity(intent);
            }
        });

        homeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapView != null) {
                    mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS);
                    setUpMap();
                }
            }
        });

        satelliteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapView != null) {
                    mapView.getMapboxMap().loadStyleUri(Style.SATELLITE_STREETS);
                    setUpMap();

                }
            }
        });


    }

    private void setUpMap(){
        LocationComponentPlugin locationComponentPlugin = getLocationComponent(mapView);
        getGestures(mapView).addOnMoveListener(onMoveListener);
        mapView.getMapboxMap().setCamera(new CameraOptions.Builder().zoom(20.0).build());
        locationComponentPlugin.setEnabled(true);
        locationComponentPlugin.setLocationProvider(navigationLocationProvider);
        getGestures(mapView).addOnMoveListener(onMoveListener);
        locationComponentPlugin.updateSettings(new Function1<LocationComponentSettings, Unit>() {
            @Override
            public Unit invoke(LocationComponentSettings locationComponentSettings) {
                locationComponentSettings.setEnabled(true);
                locationComponentSettings.setPulsingEnabled(true);
                return null;
            }
        });

        getHotspots();
        //focus camera on user and add move lister to MapHelper
        focusLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                focusLocation = true;
                getGestures(mapView).addOnMoveListener(onMoveListener);
                focusLocationBtn.hide();
            }
        });
    }
    @SuppressLint("MissingPermission")
    private void fetchRoute(Point point) {
        //get user current location and use that as a starting point
        LocationEngine locationEngine = LocationEngineProvider.getBestLocationEngine(MainActivityMap.this);
        locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
            @Override
            public void onSuccess(LocationEngineResult result) {
                Location location = result.getLastLocation();
                RouteOptions.Builder builder = RouteOptions.builder();
                Point origin = Point.fromLngLat(Objects.requireNonNull(location).getLongitude(), location.getLatitude());
                builder.coordinatesList(Arrays.asList(origin, point));

                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.location_pin);
                AnnotationPlugin annotationPlugin = AnnotationPluginImplKt.getAnnotations(mapView);
                PointAnnotationManager pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, mapView);
                PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions().withTextAnchor(TextAnchor.CENTER).withIconImage(bitmap)
                        .withPoint(point);
                pointAnnotationManager.create(pointAnnotationOptions);

                //Finds alternative routes and shortest ways along the way
                builder.alternatives(false);
                builder.profile(DirectionsCriteria.PROFILE_DRIVING);
                builder.bearingsList(Arrays.asList(Bearing.builder().angle(location.getBearing()).degrees(45.0).build(), null));
                applyDefaultNavigationOptions(builder);
                //Gets the route from start to destination
                mapboxNavigation.requestRoutes(builder.build(), new NavigationRouterCallback() {
                    @Override
                    public void onRoutesReady(@NonNull List<NavigationRoute> list, @NonNull RouterOrigin routerOrigin) {
                        //Displays routes on MapHelper
                        mapboxNavigation.setNavigationRoutes(list);
                        focusLocationBtn.performClick();
                    }

                    @Override
                    public void onFailure(@NonNull List<RouterFailure> list, @NonNull RouteOptions routeOptions) {
                        Toast.makeText(MainActivityMap.this, "Route request failed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCanceled(@NonNull RouteOptions routeOptions, @NonNull RouterOrigin routerOrigin) {

                    }
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapboxNavigation.onDestroy();
        mapboxNavigation.unregisterRoutesObserver(routesObserver);
        mapboxNavigation.unregisterLocationObserver(locationObserver);
    }

    private void getHotspots() {
        //Pass value of distance from user and the last sighting preferred
        String distance = "50"; //Default distance is 50km
        String lastSightingDays = "30"; //Max last sighting days is between 1 - 30
        FusedLocationProviderClient fusedLocationProviderClient;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivityMap.this, "Please grant permissions and reload app if unable to see map", Toast.LENGTH_LONG).show();
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(this, new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location == null) {
                    Toast.makeText(MainActivityMap.this, "Failed to get location. Please turn on location settings", Toast.LENGTH_LONG).show();
                } else {
                    //Request to get the nearby hotspots
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .build();
                    Request request = new Request.Builder()
                            .url("https://api.ebird.org/v2/ref/hotspot/geo?fmt=json&lat=" + location.getLatitude() + "&lng=" + location.getLongitude()
                                    + "&dist=" + distance + "&back=" + lastSightingDays)
                            .method("GET", null)
                            .build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            // Check if the request was successful (status code 200)
                            if (response.isSuccessful()) {
                                MainActivityMap.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Initialize Jackson ObjectMapper
                                        ObjectMapper objectMapper = new ObjectMapper();

                                        // Parse the JSON response string into a JsonNode
                                        com.fasterxml.jackson.databind.JsonNode jsonResponse = null;
                                        try {
                                            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.red_marker);
                                            AnnotationPlugin annotationPlugin = AnnotationPluginImplKt.getAnnotations(mapView);
                                            PointAnnotationManager pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, mapView);

                                            jsonResponse = objectMapper.readTree(response.body().string());
                                            // Iterate through the array of JSON objects
                                            for (com.fasterxml.jackson.databind.JsonNode locationNode : jsonResponse) {
                                                locationId.add(locationNode.get("locId").asText());
                                                locationName.add(locationNode.get("locName").asText());
                                                latitudeList.add(locationNode.get("lat").asDouble());
                                                longitudeList.add(locationNode.get("lng").asDouble());
                                                latestSightDate.add(locationNode.get("latestObsDt").asText());

                                                JsonObject jsonData = new JsonObject();
                                                jsonData.addProperty("locationId", locationNode.get("locId").asText());
                                                jsonData.addProperty("lat", locationNode.get("lat").asText());
                                                jsonData.addProperty("lng", locationNode.get("lng").asText());
                                                jsonData.addProperty("locName", locationNode.get("locName").asText());

                                                PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions().withTextAnchor(TextAnchor.CENTER).withIconImage(bitmap)
                                                        .withPoint(Point.fromLngLat(locationNode.get("lng").asDouble(), locationNode.get("lat").asDouble()))
                                                        .withData(jsonData);
                                                pointAnnotationManager.create(pointAnnotationOptions);
                                            }

                                            pointAnnotationManager.addClickListener(new OnPointAnnotationClickListener() {
                                                @Override
                                                public boolean onAnnotationClick(PointAnnotation annotation) {
                                                    onMarkerItemClick(annotation, pointAnnotationManager);
                                                    return true;
                                                }
                                            });
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                            } else {
                                Toast.makeText(MainActivityMap.this, "Failed to get nearby hotspot within a your radius", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void onMarkerItemClick(PointAnnotation marker, PointAnnotationManager pointAnnotationManager) {
        JsonElement jsonElement = marker.getData();

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.marker_details);
        dialog.setCancelable(false);

        TextView locationName = dialog.findViewById(R.id.viewLatitude);
        TextView distance = dialog.findViewById(R.id.viewLongitude);
        Button btnDelete = dialog.findViewById(R.id.btnDelete);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Access specific properties by their keys
        String locationId = jsonObject.get("locationId").getAsString();
        double lat = jsonObject.get("lat").getAsDouble();
        double lng = jsonObject.get("lng").getAsDouble();
        String locName = jsonObject.get("locName").getAsString();
        locationName.setText("Where = " + locName);

        LocationEngine locationEngine = LocationEngineProvider.getBestLocationEngine(MainActivityMap.this);
        locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
            @Override
            public void onSuccess(LocationEngineResult result) {
                Location location = result.getLastLocation();
                RouteOptions.Builder builder = RouteOptions.builder();
                Point origin = Point.fromLngLat(Objects.requireNonNull(location).getLongitude(), location.getLatitude());
                Point dest = Point.fromLngLat(lng, lat);
                builder.coordinatesList(Arrays.asList(origin, dest));
                //Finds alternative routes and shortest ways along the way
                builder.alternatives(false);
                builder.profile(DirectionsCriteria.PROFILE_DRIVING);
                builder.bearingsList(Arrays.asList(Bearing.builder().angle(location.getBearing()).degrees(45.0).build(), null));
                applyDefaultNavigationOptions(builder);
                //Gets the route from start to destination
                mapboxNavigation.requestRoutes(builder.build(), new NavigationRouterCallback() {
                    @Override
                    public void onRoutesReady(@NonNull List<NavigationRoute> list, @NonNull RouterOrigin routerOrigin) {
                        //Displays routes on MapHelper
                        distance.setText("Distance = " + calculateDistance(origin,dest ));
                    }

                    @Override
                    public void onFailure(@NonNull List<RouterFailure> list, @NonNull RouteOptions routeOptions) {
                        Toast.makeText(MainActivityMap.this, "Route request failed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCanceled(@NonNull RouteOptions routeOptions, @NonNull RouterOrigin routerOrigin) {

                    }
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        //Go to loacation
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAnnotation(pointAnnotationManager);
                dialog.dismiss();
                //Here save the locationId so I can be able to save the location and it's details
                fetchRoute(Point.fromLngLat(lng, lat));
            }
        });

        dialog.show();
    }

    private String calculateDistance(Point origin, Point dest) {
        //Get setting metrics from settings class
        int EARTH_RADIUS_KM = 6371;
        int EARTH_RADIUS_MILES = 3959;

        // Convert latitude and longitude from degrees to radians
        double lat1 = Math.toRadians(origin.latitude());
        double lon1 = Math.toRadians(origin.longitude());
        double lat2 = Math.toRadians(dest.latitude());
        double lon2 = Math.toRadians(dest.longitude());

        // Calculate the differences between latitudes and longitudes
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;
        DecimalFormat df = new DecimalFormat("#.##");

        return  df.format(distance) + " km";
    }

    private void clearAnnotation(PointAnnotationManager pointAnnotationManager){
        if (pointAnnotationManager != null) {
            pointAnnotationManager.deleteAll();
        }
    }

}