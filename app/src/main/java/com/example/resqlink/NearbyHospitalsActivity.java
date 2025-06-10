package com.example.resqlink;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.view.View;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.Manifest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NearbyHospitalsActivity extends AppCompatActivity {
    // UI Components
    private RecyclerView hospitalRecyclerView;
    private HospitalAdapter hospitalAdapter;
    private ProgressBar progressBar;

    // Location Services
    private FusedLocationProviderClient fusedLocationClient;

    // Data Storage
    private List<Hospital> hospitals = new ArrayList<>();
    private List<Hospital> localHospitals = new ArrayList<>(); // For local JSON data

    // SharedPreferences keys
    private static final String PREFS_NAME = "HospitalPrefs";
    private static final String KEY_HOSPITAL_NAME = "nearest_hospital_name";
    private static final String KEY_HOSPITAL_PHONE = "nearest_hospital_phone";
    private static final String KEY_HOSPITAL_DISTANCE = "nearest_hospital_distance";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_hospitals);

        // Initialize UI components
        initializeViews();

        // Setup RecyclerView with adapter and layout manager
        setupRecyclerView();

        // Load local hospital data
        loadLocalHospitalData();

        // Request necessary permissions
        requestPermissions();
        showCachedContact();

    }

    /**
     * Loads hospital data from local JSON file
     */
    private void loadLocalHospitalData() {
        try {
            InputStream is = getAssets().open("hospitals.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                localHospitals.add(new Hospital(
                        obj.getString("name"),
                        obj.getString("address"),
                        obj.getString("phone"),
                        obj.getDouble("latitude"),
                        obj.getDouble("longitude")
                ));
            }
        } catch (IOException | JSONException e) {
            Log.e("MainActivity", "Error loading local hospital data", e);
        }
    }

    /**
     * Initializes all view components
     */
    private void initializeViews() {
        hospitalRecyclerView = findViewById(R.id.hospital_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    /**
     * Sets up the RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        hospitalAdapter = new HospitalAdapter(hospitals, this);
        hospitalRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        hospitalRecyclerView.setAdapter(hospitalAdapter);
    }

    /**
     * Requests necessary permissions for location and phone calls
     */
    private void requestPermissions() {
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CALL_PHONE
                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            getLastLocation();
                        } else {
                            showToast("Permissions denied");
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    /**
     * Fetches the device's last known location
     */
    private void getLastLocation() {
        progressBar.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // First try local data
                        List<Hospital> nearbyLocalHospitals = findNearbyLocalHospitals(
                                location.getLatitude(),
                                location.getLongitude(),
                                5.0 // 5km radius
                        );

                        if (!nearbyLocalHospitals.isEmpty()) {
                            hospitals.addAll(nearbyLocalHospitals);
                            hospitalAdapter.updateHospitals(hospitals);




                            progressBar.setVisibility(View.GONE);
                        } else {
                            // Fall back to Overpass API if no local hospitals found
                            fetchNearbyHospitals(location.getLatitude(), location.getLongitude());
                        }
                    } else {
                        showToast("Unable to get location");
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * Finds nearby hospitals from local JSON data
     */
    private List<Hospital> findNearbyLocalHospitals(double userLat, double userLon, double radiusKm) {
        List<Hospital> nearbyHospitals = new ArrayList<>();

        for (Hospital hospital : localHospitals) {
            double distance = calculateDistance(
                    userLat, userLon,
                    hospital.getLatitude(), hospital.getLongitude()
            );

            if (distance <= radiusKm) {
                hospital.setDistance(distance);
                nearbyHospitals.add(hospital);
            }
        }

        // Sort by distance
        Collections.sort(nearbyHospitals, Comparator.comparingDouble(Hospital::getDistance));
        return nearbyHospitals;
    }

    /**
     * Fetches nearby hospitals from Overpass API
     * @param latitude Current latitude
     * @param longitude Current longitude
     */
    private void fetchNearbyHospitals(double latitude, double longitude) {
        String query = buildOverpassQuery(latitude, longitude);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://overpass-api.de/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OverpassApiService service = retrofit.create(OverpassApiService.class);
        Call<OverpassResponse> call = service.getHospitals(query);

        call.enqueue(new Callback<OverpassResponse>() {
            @Override
            public void onResponse(Call<OverpassResponse> call, Response<OverpassResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    processHospitalData(response.body(), latitude, longitude);
                } else {
                    showToast("Failed to fetch hospitals");
                }
            }

            @Override
            public void onFailure(Call<OverpassResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showToast("Error: " + t.getMessage());
            }
        });
    }

    /**
     * Builds Overpass API query string
     */
    private String buildOverpassQuery(double latitude, double longitude) {
        return String.format(
                "[out:json];" +
                        "(node[\"amenity\"=\"hospital\"](around:5000,%f,%f);" +
                        "way[\"amenity\"=\"hospital\"](around:5000,%f,%f);" +
                        "relation[\"amenity\"=\"hospital\"](around:5000,%f,%f);" +
                        ");out body;>;out skel qt;",
                latitude, longitude, latitude, longitude, latitude, longitude
        );
    }

    /**
     * Processes hospital data from API response
     */
    private void processHospitalData(OverpassResponse response, double userLat, double userLon) {
        List<Hospital> hospitalList = parseOverpassResponse(response, userLat, userLon);

        // Enhance with local data if available
        for (Hospital hospital : hospitalList) {
            if (hospital.getPhone() == null || hospital.getPhone().isEmpty()) {
                String localPhone = findLocalPhoneNumber(hospital.getName());
                if (localPhone != null) {
                    hospital.setPhone(localPhone);
                }
            }
        }

        hospitalAdapter.updateHospitals(hospitalList);

        // ADD THE FUNCTION CALL HERE FOR API LOADED HOSPITALS
        String nearestHospital = findNearestHospitalWithContact(userLat, userLon);
        if (nearestHospital != null) {
            showToast("Nearest hospital: " + nearestHospital);
        } else {
            showToast("No hospitals with contacts found nearby");
        }
        // END OF ADDED CODE
    }

    /**
     * Finds phone number from local data by hospital name
     */
    private String findLocalPhoneNumber(String hospitalName) {
        for (Hospital localHospital : localHospitals) {
            if (localHospital.getName().equalsIgnoreCase(hospitalName)) {
                return localHospital.getPhone();
            }
        }
        return null;
    }

    /**
     * Parses Overpass API response into Hospital objects
     */
    private List<Hospital> parseOverpassResponse(OverpassResponse response, double userLat, double userLon) {
        List<Hospital> hospitals = new ArrayList<>();

        for (OverpassResponse.Element element : response.elements) {
            if (element.tags != null && element.tags.containsKey("name")) {
                Hospital hospital = createHospitalFromElement(element, userLat, userLon);
                hospitals.add(hospital);
            }
        }

        // Sort by distance from user
        Collections.sort(hospitals, Comparator.comparingDouble(Hospital::getDistance));
        return hospitals;
    }

    /**
     * Creates Hospital object from Overpass element
     */
    private Hospital createHospitalFromElement(OverpassResponse.Element element, double userLat, double userLon) {
        String name = element.tags.get("name");
        String address = extractAddress(element.tags);
        String phone = extractPhoneNumber(element.tags);

        Hospital hospital = new Hospital(name, address, phone, element.lat, element.lon);
        hospital.setDistance(calculateDistance(userLat, userLon, element.lat, element.lon));
        return hospital;
    }

    /**
     * Extracts address information from OSM tags
     */
    private String extractAddress(Map<String, String> tags) {
        String street = tags.getOrDefault("addr:street", "");
        String houseNumber = tags.getOrDefault("addr:housenumber", "");
        String city = tags.getOrDefault("addr:city", "");

        if (!street.isEmpty()) {
            StringBuilder addressBuilder = new StringBuilder(street);
            if (!houseNumber.isEmpty()) {
                addressBuilder.append(" ").append(houseNumber);
            }
            if (!city.isEmpty()) {
                addressBuilder.append(", ").append(city);
            }
            return addressBuilder.toString();
        }

        // Fallback to other address tags
        String[] addressTags = {"addr:full", "address", "contact:street", "contact:address"};
        for (String tag : addressTags) {
            if (tags.containsKey(tag)) {
                return tags.get(tag);
            }
        }

        return "Address not available";
    }

    /**
     * Extracts phone number from OSM tags
     */
    private String extractPhoneNumber(Map<String, String> tags) {
        // List of possible phone number tags
        String[] phoneTags = {
                "phone", "contact:phone", "phone:emergency",
                "telephone", "emergency_phone", "healthcare:phone"
        };

        for (String tag : phoneTags) {
            if (tags.containsKey(tag) && isValidPhoneNumber(tags.get(tag))) {
                return formatPhoneNumber(tags.get(tag));
            }
        }
        return "";
    }

    /**
     * Validates phone number format
     */
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return false;

        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() >= 7;
    }

    /**
     * Formats phone number consistently
     */
    private String formatPhoneNumber(String phone) {
        return phone.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^0-9+]", "");
    }

    /**
     * Calculates distance between two coordinates using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Helper method to show toast messages
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    //
    /**
     * Finds the nearest hospital with a valid contact number
     * @param userLat Current latitude
     * @param userLon Current longitude
     * @return Formatted string with hospital name and phone, or null if none found
     */
    public String findNearestHospitalWithContact(double userLat, double userLon) {
        // Combine both local and API hospitals (if available)
        List<Hospital> allHospitals = new ArrayList<>();
        allHospitals.addAll(localHospitals);
        allHospitals.addAll(hospitals);

        // Filter hospitals with valid phone numbers
        List<Hospital> hospitalsWithContacts = new ArrayList<>();
        for (Hospital hospital : allHospitals) {
            if (hospital.getPhone() != null && !hospital.getPhone().isEmpty() && isValidPhoneNumber(hospital.getPhone())) {
                // Calculate distance for each hospital
                double distance = calculateDistance(userLat, userLon,
                        hospital.getLatitude(), hospital.getLongitude());
                hospital.setDistance(distance);
                hospitalsWithContacts.add(hospital);
            }
        }

        // Sort by distance
        Collections.sort(hospitalsWithContacts, Comparator.comparingDouble(Hospital::getDistance));

        // Debug logging
        Log.d("HospitalFinder", "Total hospitals with contacts: " + hospitalsWithContacts.size());
        for (Hospital h : hospitalsWithContacts) {
            Log.d("HospitalFinder", String.format(Locale.getDefault(),
                    "Hospital: %s, Phone: %s, Distance: %.2f km",
                    h.getName(), h.getPhone(), h.getDistance()));
        }

        if (!hospitalsWithContacts.isEmpty()) {
            Hospital nearest = hospitalsWithContacts.get(0);
            String result = String.format(Locale.getDefault(),
                    "%s - %s (%.1f km away)",
                    nearest.getName(), nearest.getPhone(), nearest.getDistance());

            // Cache the result
            cacheNearestHospitalContact(nearest);

            Log.d("HospitalFinder", "Nearest hospital with contact: " + result);
            return result;
        }

        Log.d("HospitalFinder", "No hospitals with valid contacts found");
        return null;
    }



    ///////////////////////----------------------------------------------
    //--------------------------for getting and caching the nearest hospital contact info : ---------------

    /**
     * Saves hospital contact info to SharedPreferences
     */
    private void cacheNearestHospitalContact(Hospital hospital) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_HOSPITAL_NAME, hospital.getName());
        editor.putString(KEY_HOSPITAL_PHONE, hospital.getPhone());
        editor.putFloat(KEY_HOSPITAL_DISTANCE, (float) hospital.getDistance());

        editor.apply();

        Log.d("HospitalCache", "Cached hospital: " + hospital.getName() +
                " | Phone: " + hospital.getPhone());
    }

    /**
     * Retrieves cached hospital contact info
     */
    private Hospital getCachedHospitalContact() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String name = prefs.getString(KEY_HOSPITAL_NAME, null);
        String phone = prefs.getString(KEY_HOSPITAL_PHONE, null);
        float distance = prefs.getFloat(KEY_HOSPITAL_DISTANCE, -1);

        if (name == null || phone == null) {
            return null;
        }

        // Create a minimal Hospital object with cached data
        Hospital hospital = new Hospital(name, "", phone, 0, 0);
        hospital.setDistance(distance);

        return hospital;
    }

    /**
     * Makes emergency call to cached hospital number
     */
    public void makeEmergencyCall() {
        Hospital cachedHospital = getCachedHospitalContact();

        if (cachedHospital == null || cachedHospital.getPhone() == null) {
            showToast("No emergency contact available");
            return;
        }

        // Format phone number for dialing (remove all non-digit characters except +)
        String phoneNumber = cachedHospital.getPhone().replaceAll("[^+0-9]", "");

        // Check CALL_PHONE permission
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, 101);
            return;
        }

        // Create the intent
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));

        try {
            startActivity(callIntent);
            Log.d("EmergencyCall", "Calling: " + phoneNumber);
        } catch (SecurityException e) {
            showToast("Call permission denied");
            Log.e("EmergencyCall", "Permission error", e);
        }
    }


    //////////////////////---- showing chached contact info : ---------------------------
    public void showCachedContact() {
        Hospital cached = getCachedHospitalContact();
        if (cached != null) {
            String info = String.format(Locale.getDefault(),
                    "Cached Hospital:\n%s\n%s\n%.1f km away",
                    cached.getName(),
                    cached.getPhone(),
                    cached.getDistance());
            showToast(info);
        } else {
            showToast("No hospital contact cached");
        }
    }


}