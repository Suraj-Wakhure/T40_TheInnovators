package com.example.resqlink;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int IMAGE_PERMISSION_REQUEST_CODE = 1002;

    // Personal details
    private SharedPreferences sharedPreferences;
    private Button btnUpdate;
    private TextView name, email, phone;
    private ImageView profileImage;

    // Location
    private TextView locationTextView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    // Medical history
    private Button update;
    private String Healthinfo;
    private TextView tvMedicalInfo;

    private String mParam1;
    private String mParam2;

    // Image picker launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Initialize image picker launcher
        initializeImagePicker();
        initializeLocationCallback();
    }

    private void initializeImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            saveImageFromUri(selectedImageUri);
                        }
                    }
                });
    }


    private void initializeLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w("ProfileFragment", "Received null location result");
                    return;
                }

                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    updateLocationUI(lastLocation);
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initializeViews(view);
        loadProfileData();
        loadProfileImage();
        loadHealthData();
        setupClickListeners();
        requestLocationUpdates();

        return view;
    }

    private void initializeViews(View view) {
        btnUpdate = view.findViewById(R.id.btn_update_profile);
        name = view.findViewById(R.id.tv_name);
        email = view.findViewById(R.id.tv_email);
        phone = view.findViewById(R.id.tv_phone);
        profileImage = view.findViewById(R.id.profile_image);
        locationTextView = view.findViewById(R.id.tv_location);
        tvMedicalInfo = view.findViewById(R.id.tvMedicalInfo);
        update = view.findViewById(R.id.btnUpdate);

        sharedPreferences = requireActivity().getSharedPreferences("profile_personnal_data", MODE_PRIVATE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    private void setupClickListeners() {
        profileImage.setOnClickListener(v -> handleImageSelection());

        btnUpdate.setOnClickListener(v -> showUpdateDialog());
        update.setOnClickListener(v -> showUpdateMedicalHistoryDialog());
    }
    private void handleImageSelection() {
        Log.d("ProfileFragment", "Profile image clicked");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - no permission needed for image picker
            openImagePicker();
        } else {
            // For older versions, check storage permission
            if (checkImagePermission()) {
                Log.d("ProfileFragment", "Permissions granted, opening image picker");
                openImagePicker();
            } else {
                Log.d("ProfileFragment", "Requesting permissions");
                requestImagePermission();
            }
        }
    }

    // Simplified image picking methods
    private boolean checkImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true; // No permission needed for Android 13+
        }
        boolean result = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        Log.d("ProfileFragment", "Permission check result: " + result);
        return result;
    }

    private void requestImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openImagePicker(); // No permission needed for Android 13+
            return;
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // Explain why we need permission
            new AlertDialog.Builder(requireContext())
                    .setTitle("Permission Needed")
                    .setMessage("This permission is required to select your profile picture")
                    .setPositiveButton("OK", (dialog, which) -> {
                        ActivityCompat.requestPermissions(requireActivity(),
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                IMAGE_PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    IMAGE_PERMISSION_REQUEST_CODE);
        }
    }


    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error opening image picker", e);
            Toast.makeText(getContext(), "No image picker app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageFromUri(Uri uri) {
        try {
            // Get input stream from URI
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            // Set the image to ImageView
            profileImage.setImageBitmap(bitmap);

            // Save to internal storage
            File file = new File(requireContext().getFilesDir(), "profile_img.jpg");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                sharedPreferences.edit().putBoolean("hasImage", true).apply();
                Toast.makeText(getContext(), "Profile image saved", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadProfileImage() {
        if (sharedPreferences.getBoolean("hasImage", false)) {
            File file = new File(requireContext().getFilesDir(), "profile_img.jpg");
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap);
                }
            }
        }
    }

    // Personal details methods
    private void loadProfileData() {
        name.setText(sharedPreferences.getString("name", "Name"));
        email.setText(sharedPreferences.getString("email", "Email"));
        phone.setText(sharedPreferences.getString("phone", "Phone"));
    }

    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_personnal_detail_input, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText editName = dialogView.findViewById(R.id.profile_name);
        EditText editEmail = dialogView.findViewById(R.id.profile_email);
        EditText editPhone = dialogView.findViewById(R.id.profile_phone);
        Button btnSaveData = dialogView.findViewById(R.id.btnSaveData);

        btnSaveData.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();
            String newPhone = editPhone.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            sharedPreferences.edit()
                    .putString("name", newName)
                    .putString("email", newEmail)
                    .putString("phone", newPhone)
                    .apply();

            loadProfileData();
            dialog.dismiss();
            Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // Location methods
    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateLocationUI(Location location) {
        if (location != null) {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1);

                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);
                    locationTextView.setText(address);
                } else {
                    locationTextView.setText(String.format(Locale.getDefault(),
                            "Lat: %.4f, Long: %.4f",
                            location.getLatitude(),
                            location.getLongitude()));
                }
            } catch (IOException e) {
                locationTextView.setText(String.format(Locale.getDefault(),
                        "Lat: %.4f, Long: %.4f",
                        location.getLatitude(),
                        location.getLongitude()));
            }
        }
    }

    // Medical history methods
    private void showUpdateMedicalHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_health_input, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText editChronic = dialogView.findViewById(R.id.editChronicDiseases);
        EditText editSurgeries = dialogView.findViewById(R.id.editSurgeries);
        EditText editAllergies = dialogView.findViewById(R.id.editAllergies);
        EditText editMedications = dialogView.findViewById(R.id.editMedications);
        EditText editBloodGroup = dialogView.findViewById(R.id.editBloodGroup);
        EditText editLifestyle = dialogView.findViewById(R.id.editLifestyle);
        Button btnSaveData = dialogView.findViewById(R.id.btnSaveData);

        btnSaveData.setOnClickListener(v -> {
            String chronic = editChronic.getText().toString().trim();
            String surgeries = editSurgeries.getText().toString().trim();
            String allergies = editAllergies.getText().toString().trim();
            String meds = editMedications.getText().toString().trim();
            String bloodGroup = editBloodGroup.getText().toString().trim();
            String lifestyle = editLifestyle.getText().toString().trim();

            sharedPreferences.edit()
                    .putString("chronic", chronic)
                    .putString("surgeries", surgeries)
                    .putString("allergies", allergies)
                    .putString("medications", meds)
                    .putString("blood_group", bloodGroup)
                    .putString("lifestyle", lifestyle)
                    .apply();

            loadHealthData();
            dialog.dismiss();
            Toast.makeText(getContext(), "Medical info updated!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void loadHealthData() {
        String chronic = sharedPreferences.getString("chronic", "--");
        String surgeries = sharedPreferences.getString("surgeries", "--");
        String allergies = sharedPreferences.getString("allergies", "--");
        String meds = sharedPreferences.getString("medications", "--");
        String bloodGroup = sharedPreferences.getString("blood_group", "--");
        String lifestyle = sharedPreferences.getString("lifestyle", "--");

        Healthinfo = "\n\uD83D\uDE91 Medical Info:\n" +
                "\n\u2714\uFE0F Chronic Diseases: " + chronic +
                "\n\u2714\uFE0F Surgeries: " + surgeries +
                "\n\u2714\uFE0F Allergies: " + allergies +
                "\n\u2714\uFE0F Medications: " + meds +
                "\n\u2714\uFE0F Blood Group: " + bloodGroup +
                "\n\u2714\uFE0F Lifestyle: " + lifestyle;

        tvMedicalInfo.setText(Healthinfo);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == IMAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(requireContext(), "Storage permission needed to select images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }
}