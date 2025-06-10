package com.example.resqlink;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.io.File;

import android.Manifest;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private IMapController mapController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initializeing OSMDroid configuration (MUST BE BEFORE setContentView)
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Context ctx = getApplicationContext();
        Configuration.getInstance().setOsmdroidBasePath(new File(ctx.getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(ctx.getCacheDir(), "tiles"));



        setContentView(R.layout.activity_map);

        // Get hospital data from intent
        double hospitalLat = getIntent().getDoubleExtra("hospital_lat", 0);
        double hospitalLon = getIntent().getDoubleExtra("hospital_lon", 0);
        String hospitalName = getIntent().getStringExtra("hospital_name");

        // Initialize map
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Set map center
        GeoPoint startPoint = new GeoPoint(hospitalLat, hospitalLon);
        GeoPoint hospitalPoint = new GeoPoint(hospitalLat, hospitalLon);  // This creates the missing point
        mapController = mapView.getController();
        mapController.setZoom(15);
        mapController.setCenter(startPoint);

        // Add hospital marker with proper size
        Marker hospitalMarker = new Marker(mapView);
        hospitalMarker.setPosition(hospitalPoint);
        hospitalMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        hospitalMarker.setTitle(hospitalName);

        // Use a properly sized marker (create a 48x48px PNG and put it in res/drawable)
        hospitalMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_hospital_marker));
        hospitalMarker.setInfoWindow(new MarkerInfoWindow(org.osmdroid.library.R.layout.bonuspack_bubble, mapView));

        mapView.getOverlays().add(hospitalMarker);

        // Add marker for user location (if available)
        // You can get this from the previous activity or request location again

        //adding current location marker :
        addCurrentLocationMarker();

    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }


    //for map marker :
    private void addCurrentLocationMarker() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                            Marker currentMarker = new Marker(mapView);
                            currentMarker.setPosition(currentPoint);
                            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            currentMarker.setTitle("You are here");
                            currentMarker.setIcon(ContextCompat.getDrawable(this, org.osmdroid.mapsforge.R.drawable.ic_menu_mylocation));
                            mapView.getOverlays().add(currentMarker);

                            mapView.invalidate(); // Refresh map
                        }
                    });
        }
    }
    private void addHospitalMarker(GeoPoint point, String name) {
        Marker hospitalMarker = new Marker(mapView) {
            @Override
            public void draw(Canvas canvas, MapView mapView, boolean shadow) {
                super.draw(canvas, mapView, false); // Disable shadow for better visibility
            }
        };

        hospitalMarker.setPosition(point);
        hospitalMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        hospitalMarker.setTitle(name);

        // Set the vector drawable as marker icon
        hospitalMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_hospital_marker));

        // Custom info window
        hospitalMarker.setInfoWindow(new MarkerInfoWindow(org.osmdroid.library.R.layout.bonuspack_bubble, mapView) {
            @Override
            public void onOpen(Object item) {
                super.onOpen(item);
                // Customize info window here
            }
        });

        mapView.getOverlays().add(hospitalMarker);

        // Optional: Add pulsing animation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            hospitalMarker.setDraggable(false);
            hospitalMarker.setPanToView(true);
        }
    }
}