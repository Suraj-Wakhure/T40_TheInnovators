package com.example.resqlink;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.transition.MaterialContainerTransform;
import android.Manifest;

public class HomeFragment extends Fragment {

    Button triggerbtn, tutorialbtn;
    FusedLocationProviderClient fusedLocationClient;
    private BroadcastReceiver smsReceiver;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        // Initialize SMS receiver
        setupSmsReceiver();

        //triggers :
        triggerbtn = view.findViewById(R.id.btnTriggers);
        tutorialbtn = view.findViewById(R.id.btnTutorial);
        tutorialbtn.setOnClickListener(view1 -> {
            startActivity(new Intent(requireContext(), Tutorial1.class));
        });
        triggerbtn.setOnClickListener(view1 -> {
            startActivity(new Intent(getContext(), Triggers.class));
        });

        // In your Fragment's onViewCreated()
        View pulseCircle = view.findViewById(R.id.pulseCircle);
        Animation pulse = AnimationUtils.loadAnimation(getContext(), R.anim.pulse_animation);
        pulseCircle.startAnimation(pulse);

        Button sosButton = view.findViewById(R.id.sosButton);
        sosButton.setOnClickListener(v -> {
            String number = "7972408401";

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                return;
            }

            LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(getActivity(), "Please enable GPS/location services", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double lat = location.getLatitude();
                            double lng = location.getLongitude();
                            String locationLink = "https://www.google.com/maps?q=" + lat + "," + lng;

                            String message = "🚨 Emergency! I am in danger.\nMy Location: " + locationLink;

                            try {
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(number, null, message, null, null);
                                Toast.makeText(getActivity(), "SMS sent with location to " + number, Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(getActivity(), "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getActivity(), "Location is null. Try again in a moment.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getActivity(), "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            EmergencyAlertManager alertManager = new EmergencyAlertManager(
                    getActivity(),
                    "7972408401",
                    "HELP! I'm in an emergency. My location is being shared."
            );
            if (alertManager.hasRequiredPermissions()) {
                alertManager.triggerEmergencyAlert();
            }
        });


        setupCardClicks(view);

        return view;
    }

    private void setupSmsReceiver() {
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        if (pdus != null) {
                            for (Object pdu : pdus) {
                                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                                String message = smsMessage.getMessageBody();
                                String sender = smsMessage.getDisplayOriginatingAddress();

                                // Check if message contains location (Google Maps link)
                                if (message.contains("maps?q=")) {
                                    // Extract location from message
                                    int startIndex = message.indexOf("maps?q=") + 7;
                                    int endIndex = message.indexOf(",", startIndex);
                                    if (endIndex == -1) endIndex = message.length();

                                    String location = message.substring(startIndex, endIndex);
                                    Toast.makeText(getActivity(),
                                            "Received location from " + sender + ": " + location,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }
                }
            }
        };

        // Register the receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        requireActivity().registerReceiver(smsReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when fragment is destroyed
        if (smsReceiver != null) {
            try {
                requireActivity().unregisterReceiver(smsReceiver);
            } catch (Exception e) {
                // Receiver was not registered
            }
        }
    }

    private void setupCardClicks(View view) {
        int[] cardIds = {
                R.id.card_view_policeman,
                R.id.card_view_hospital,
                R.id.card_view_1,
                R.id.card_view_2
        };

        for (int id : cardIds) {
            view.findViewById(id).setOnClickListener(v -> {
                // Scale down animation
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(80)
                        .withEndAction(() -> {
                            // Scale back up with bounce
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(150)
                                    .setInterpolator(new OvershootInterpolator(1.5f))
                                    .start();

                            // Handle emergency call
                            String number = (id == R.id.card_view_policeman || id == R.id.card_view_1)
                                    ? "100" : "108";
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
                        })
                        .start();
            });
        }
    }

    private void handleEmergencyCall(int cardId) {
        String number = "";
        if (cardId == R.id.card_view_policeman || cardId == R.id.card_view_1) {
            number = "100";
        } else if (cardId == R.id.card_view_hospital || cardId == R.id.card_view_2) {
            number = "108";
        }

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        startActivity(intent);
    }
}