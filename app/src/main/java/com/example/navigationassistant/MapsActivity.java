package com.example.navigationassistant;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.navigationassistant.models.PolylineData;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TrafficModel;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnPolylineClickListener {

    //Google Map instance
    private GoogleMap mMap;

    //Tag for errors
    private static final String TAG = MapsActivity.class.getSimpleName();

    // A default location (Istanbul) and default zoom to use when location permission is not granted.
    private final LatLng mDefaultLocation = new LatLng(41, 29);
    private static final int DEFAULT_ZOOM = 15;

    //Addresses for voice recognition navigation
    private final LatLng homeCesme = new LatLng(38.31650, 26.38591);
    private final LatLng workCesme = new LatLng(38.28198, 26.37073);
    private final LatLng homeIst = new LatLng(41.19459, 29.04670);
    private final LatLng workIst = new LatLng(41.10496, 29.02260);

    //Coordinates for Location bias
    private final LatLng CesmeSouthWest = new LatLng(38.27509, 26.34397);
    private final LatLng CesmeNorthEast = new LatLng(38.33702, 26.43143);
    private final RectangularBounds CesmeBounds = RectangularBounds.newInstance(CesmeSouthWest, CesmeNorthEast);
    private final LatLng IstSouthWest = new LatLng(41.18492, 29.04177);
    private final LatLng IstNorthEast = new LatLng(41.19647, 29.05794);
    private final RectangularBounds IstBounds = RectangularBounds.newInstance(IstSouthWest, IstNorthEast);

    //Lists for voice recognition
    private List<String> market = new ArrayList<>(
            Arrays.asList("groceries", "market", "egg", "milk", "diaper", "yogurt"));
    private List<String> manav = new ArrayList<>(
            Arrays.asList("fruit", "vegetable", "tomato", "cucumber", "pepper", "orange"));
    private List<String> cafe = new ArrayList<>(
            Arrays.asList("cafe", "coffee", "relax", "chill", "tea", "time"));

    //Gas station preference
    private boolean stationPreferenceExists = false;
    private String preferredGasStation;
    private List<String> gasStations = new ArrayList<>(
            Arrays.asList("bp", "shell", "opet", "petrol ofisi")
    );

    //Permissions
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    //Google Directions
    private GeoApiContext mGeoApiContext = null;

    //ArrayList containing routes data
    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();

    //request code for Google speech recognition intent
    private final int REQ_CODE = 100;
    private final int REQ_CODE_EMERGENCY = 101;

    //Text to Speech
    private TextToSpeech t1;

    //Map Fragment
    private SupportMapFragment mapFragment;

    //Places API client
    private PlacesClient placesClient;

    //Destination marker to show durations
    private Marker destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);

                    //make speech 40% faster than original
                    t1.setSpeechRate((float)1.4);
                }
            }
        });

        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));

        // Create a new Places client instance
        placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setTypeFilter(TypeFilter.ESTABLISHMENT);
        autocompleteFragment.setLocationBias(IstBounds);
        autocompleteFragment.setCountry("TR");

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place: " + place.getName());
                AddPlaceMarker(place.getLatLng(), place.getName());
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onPause(){
        if(t1 != null){
            t1.stop();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (t1 != null) {
            t1.shutdown();
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setRotateGesturesEnabled(false);

        if(mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder().apiKey(getString(R.string.google_maps_key)).build();
        }

        //Set Listener for marker Info window
        mMap.setOnInfoWindowClickListener(this);

        //Set Listener for Polyline
        mMap.setOnPolylineClickListener(this);

        // Setting a click event handler for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                AddMarker(latLng);
            }
        });

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    private Marker AddMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions();

        // Setting the position for the marker
        markerOptions.position(latLng);

        // Make the marker draggable
        markerOptions.draggable(true);

        // Setting the title and snippet for the marker.
        markerOptions.title("Show routes");
        markerOptions.snippet(null);

        // Clears the previously touched position
        mMap.clear();

        // Animating to the touched position
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

        // Placing a marker on the touched position
        Marker m1 = mMap.addMarker(markerOptions);

        // Show title and snippet immediately
        m1.showInfoWindow();

        return m1;
    }

    private Marker AddPlaceMarker(LatLng latLng, String title) {
        // Setting the position for the marker
        MarkerOptions myMarkerOptions = new MarkerOptions();

        myMarkerOptions.position(latLng);

        // Setting the title and snippet for the marker.
        myMarkerOptions.title(title);
        myMarkerOptions.snippet("Tap to show routes");

        // Placing a marker on the touched position
        Marker m1 = mMap.addMarker(myMarkerOptions);

        return m1;
    }

    private Marker AddPlaceMarkerColored(LatLng latLng, String title) {
        // Setting the position for the marker
        MarkerOptions myMarkerOptions = new MarkerOptions();

        myMarkerOptions.position(latLng);

        // Setting the title and snippet for the marker.
        myMarkerOptions.title(title);
        myMarkerOptions.snippet("Tap to show routes");

        //Change marker color
        myMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        // Placing a marker on the touched position
        Marker m1 = mMap.addMarker(myMarkerOptions);

        return m1;
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                // get location button
                View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).
                        getParent()).findViewById(Integer.parseInt("2"));
                // get its layout parameters
                RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                // move it down by readjusting margins
                rlp.setMargins(0, 180, 30, 0);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void checkAndSetGasStationPreference(Marker marker) {
        if (!stationPreferenceExists) {
            for (String myGasStation: gasStations) {
                if (marker.getTitle().toLowerCase().contains(myGasStation)) {
                    preferredGasStation = myGasStation;
                    stationPreferenceExists = true;
                    break;
                }
            }
        }
    }

    private void calculateDirections(Marker marker){
        destination = marker;

        checkAndSetGasStationPreference(marker);

        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.departureTimeNow();
        directions.mode(TravelMode.DRIVING);
        directions.trafficModel(TrafficModel.OPTIMISTIC);
        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(
                        mLastKnownLocation.getLatitude(),
                        mLastKnownLocation.getLongitude()
                )
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());

                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );
            }
        });
    }

    private void addPolylinesToMap(final DirectionsResult result){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                if(!mPolyLinesData.isEmpty()) {
                    for(PolylineData pd: mPolyLinesData) {
                        pd.getPolyline().remove();
                    }
                    mPolyLinesData.clear();
                    mPolyLinesData = new ArrayList<>();
                }

                double durationInTraffic = 99999999;
                String totalDuration = "";
                String inTrafficDuration = "";
                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for(com.google.maps.model.LatLng latLng: decodedPath){

                        //Log.d(TAG, "run: latlng: " + latLng.toString());

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(getApplicationContext(), R.color.darkGrey));
                    polyline.setClickable(true);
                    mPolyLinesData.add(new PolylineData(polyline, route.legs[0]));

                    //Highlight the route with shortest durationInTraffic
                    double tempDuration = (route.legs[0].durationInTraffic == null) ? 0 : route.legs[0].durationInTraffic.inSeconds;
                    if (tempDuration < durationInTraffic) {
                        durationInTraffic = tempDuration;
                        onPolylineClick(polyline);
                        zoomRoute(polyline.getPoints());
                        totalDuration = route.legs[0].duration.humanReadable
                                .substring(0, route.legs[0].duration.humanReadable.length() - 5);
                        inTrafficDuration = route.legs[0].durationInTraffic.humanReadable
                                .substring(0, route.legs[0].durationInTraffic.humanReadable.length() - 5);
                    }
                }
                StartNavigationSpeech(totalDuration, inTrafficDuration);
            }
        });
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        calculateDirections(marker);
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        for(PolylineData polylineData: mPolyLinesData){
            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());
            if(polyline.getId().equals(polylineData.getPolyline().getId())){
                polylineData.getPolyline().setColor(ContextCompat.getColor(this, R.color.blue));
                polylineData.getPolyline().setZIndex(1);

                if (destination.getTitle().equals("Show routes")) {
                    destination.setTitle("Duration");
                }
                destination.setSnippet("In Traffic: " + polylineData.getLeg().durationInTraffic.humanReadable +
                                    ", Total: " + polylineData.getLeg().duration);

                destination.showInfoWindow();
            }
            else{
                polylineData.getPolyline().setColor(ContextCompat.getColor(this, R.color.darkGrey));
                polylineData.getPolyline().setZIndex(0);
            }
        }
    }

    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (mMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 360;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }

    //Called when the user touches reset button
    public void resetMap(View view){
        if(mMap != null) {
            mMap.clear();

            if(!mPolyLinesData.isEmpty()){
                mPolyLinesData.clear();
                mPolyLinesData = new ArrayList<>();
            }
        }
    }

    //Called when user touches the microphone button
    public void RecognizeSpeech(View v) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...");
        try {
            startActivityForResult(intent, REQ_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Sorry your device not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String str = (String)result.get(0);
                    //TODO: manav and bakery together
                    if (str.contains("home")) {
                        resetMap(getCurrentFocus());
                        Marker home = AddMarker(homeIst);
                        calculateDirections(home);
                    } else if (str.contains("work")) {
                        resetMap(getCurrentFocus());
                        Marker work = AddMarker(workIst);
                        calculateDirections(work);
                    } else if (str.contains("all") && (str.contains("gas") || str.contains("fuel"))) {
                        resetMap(getCurrentFocus());
                        stationPreferenceExists = false;
                        LocationRequest("total");
                        LocationRequest("petrol ofisi");
                        LocationRequest("bp");
                        StartPlacesSpeech("gas stations");
                    } else if (str.contains("gas") || str.contains("fuel")) {
                        resetMap(getCurrentFocus());
                        if (stationPreferenceExists) {
                            LocationRequest(preferredGasStation);
                            StartPlacesSpeech(preferredGasStation);
                        } else {
                            LocationRequest("total");
                            LocationRequest("petrol ofisi");
                            LocationRequest("bp");
                            StartPlacesSpeech("gas stations");
                        }
                    } else if (str.contains("cheese") && str.contains("wine")) {
                        resetMap(getCurrentFocus());
                        LocationRequest("market");
                        LocationRequestColored("tekel");
                        t1.speak("Showing markets and liquor stores", TextToSpeech.QUEUE_FLUSH, null);
                    } else if (str.contains("eat") || str.contains("hungry") || str.contains("restaurant")) {
                        resetMap(getCurrentFocus());
                        LocationRequest("restoran");
                        StartPlacesSpeech("restaurants");
                    } else if (str.contains("medicine") || str.contains("pharmacy") || str.contains("disinfectant")) {
                        resetMap(getCurrentFocus());
                        LocationRequest("eczane");
                        StartPlacesSpeech("pharmacies");
                        while (t1.isSpeaking()){} //wait for speech to finish
                        t1.speak("get well soon", TextToSpeech.QUEUE_FLUSH, null);
                    } else if (str.contains("alcohol") || str.contains("wine") || str.contains("beer")) {
                        resetMap(getCurrentFocus());
                        LocationRequest("tekel");
                        StartPlacesSpeech("liquor stores");
                    } else if (str.contains("bleed") || str.contains("hurt") || str.contains("hospital")) {
                        resetMap(getCurrentFocus());
                        LocationRequest("hastane");
                        StartPlacesSpeech("hospitals");
                        while (t1.isSpeaking()){} //wait for speech to finish
                        CheckAndCallEmergencyServices();
                    } else if (str.contains("bread") || str.contains("bakery")) {
                        resetMap(getCurrentFocus());
                        LocationRequest("fırın");
                        StartPlacesSpeech("bakery");
                    } else if (containsFromList(str, market)) {
                        resetMap(getCurrentFocus());
                        LocationRequest("market");
                        StartPlacesSpeech("markets");
                    } else if (containsFromList(str, manav)) {
                        resetMap(getCurrentFocus());
                        LocationRequest("manav");
                        StartPlacesSpeech("greengrocers");
                    } else if (containsFromList(str, cafe)) {
                        resetMap(getCurrentFocus());
                        LocationRequest("cafe");
                        StartPlacesSpeech("cafes");
                    }
                }
                break;
            }
            case REQ_CODE_EMERGENCY: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList myResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String string = (String) myResult.get(0);
                    if (string.toLowerCase().contains("yes") || string.toLowerCase().contains("please")) {
                        Uri number = Uri.parse("tel:112");
                        Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
                        startActivity(callIntent);
                    } else if (string.contains("medicine") || string.contains("disinfectant")) {
                        t1.speak("okay", TextToSpeech.QUEUE_FLUSH, null);
                        resetMap(getCurrentFocus());
                        LocationRequest("eczane");
                        while (t1.isSpeaking()){} //wait for speech to finish
                        StartPlacesSpeech("pharmacies");
                        while (t1.isSpeaking()){} //wait for speech to finish
                        t1.speak("get well soon", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
            }
        }
    }

    private void CheckAndCallEmergencyServices() {
        t1.speak("Should I call emergency services?", TextToSpeech.QUEUE_FLUSH, null);
        while (t1.isSpeaking()){} //wait for speech to finish
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Call Emergency Services?");
        try {
            startActivityForResult(intent, REQ_CODE_EMERGENCY);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Sorry your device not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean containsFromList(String input, List<String> list) {
        for (String s: list) {
            if (input.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private void StartNavigationSpeech(String totalDuration, String inTrafficDuration) {
        // Pick one of these in random for speech
        String[] SpeechOptions = {
                "I suggest this route.",
                "Let's go!",
                "I highlighted the best route.",
                "Here is the best route.",
                "Let's drive!"};
        int random = new Random().nextInt(5);
        String toSpeak = SpeechOptions[random];
                //.concat(" It takes " + inTrafficDuration + "minutes in traffic and " + totalDuration + " in total.");

        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void StartPlacesSpeech(String place) {
        // Pick one of these in random for speech
        String[] SpeechOptions = {
                "There are a few " + place + " nearby.",
                "I suggest one of these " + place,
                "I recommend these " + place,
                "Here are a few options.",
                "Try these " + place};
        int random = new Random().nextInt(5);
        String toSpeak = SpeechOptions[random];

        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void LocationRequest(String query) {
        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling fetchPlace()).
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        // Use the builder to create a FindAutocompletePredictionsRequest.
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                // Call either setLocationBias() OR setLocationRestriction().
                .setLocationBias(IstBounds)
                //.setLocationRestriction(IstBounds)
                .setOrigin(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
                .setCountry("TR")
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setSessionToken(token)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                String placeId = prediction.getPlaceId();

                FetchPlaceRequest fprequest = FetchPlaceRequest.builder(placeId,
                        Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME)).build();

                placesClient.fetchPlace(fprequest).addOnSuccessListener((p1) -> {
                    AddPlaceMarker(p1.getPlace().getLatLng(), p1.getPlace().getName());
                });
            }



        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e(TAG, "Place not found: " + apiException.getStatusCode());
            }
        });
    }

    private void LocationRequestColored(String query) {
        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling fetchPlace()).
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        // Use the builder to create a FindAutocompletePredictionsRequest.
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                // Call either setLocationBias() OR setLocationRestriction().
                .setLocationBias(IstBounds)
                //.setLocationRestriction(IstBounds)
                .setOrigin(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
                .setCountry("TR")
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setSessionToken(token)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                String placeId = prediction.getPlaceId();

                FetchPlaceRequest fprequest = FetchPlaceRequest.builder(placeId,
                        Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME)).build();

                placesClient.fetchPlace(fprequest).addOnSuccessListener((p1) -> {
                    AddPlaceMarkerColored(p1.getPlace().getLatLng(), p1.getPlace().getName());
                });
            }



        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e(TAG, "Place not found: " + apiException.getStatusCode());
            }
        });
    }
}
