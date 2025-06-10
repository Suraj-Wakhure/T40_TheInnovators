package com.example.resqlink;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.content.Context;
import android.widget.Toast;
import android.view.View;


public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder> {
    private List<Hospital> hospitals;
    private Context context; // Needed for phone intent

    public HospitalAdapter(List<Hospital> hospitals, Context context) {
        this.hospitals = hospitals;
        this.context = context;
    }

    @NonNull
    @Override
    public HospitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hospital, parent, false);
        return new HospitalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HospitalViewHolder holder, int position) {
        Hospital hospital = hospitals.get(position);
        holder.bind(hospital);

    }

    @Override
    public int getItemCount() { return hospitals.size(); }

    public void updateHospitals(List<Hospital> newHospitals) {
        this.hospitals.clear();              // Clear old data
        this.hospitals.addAll(newHospitals); // Add new data
        notifyDataSetChanged();              // Notify adapter to refresh UI
    }


    class HospitalViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView, addressTextView, phoneTextView, distanceTextView;
        ImageView callButton;


        public HospitalViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.hospital_name);
            addressTextView = itemView.findViewById(R.id.hospital_address);
            phoneTextView = itemView.findViewById(R.id.hospital_phone);
            distanceTextView = itemView.findViewById(R.id.hospital_distance);
            callButton = itemView.findViewById(R.id.emergency_call_button);

        }

        public void bind(final Hospital hospital) {
            nameTextView.setText(hospital.getName());
            addressTextView.setText(hospital.getAddress());
            phoneTextView.setText(hospital.getPhone() != null ? "Phone: " + hospital.getPhone() : "Phone: Not available");
            distanceTextView.setText(String.format("%.1f km", hospital.getDistance()));

            // Click on entire card opens map
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MapActivity.class);
                intent.putExtra("hospital_lat", hospital.getLatitude());
                intent.putExtra("hospital_lon", hospital.getLongitude());
                intent.putExtra("hospital_name", hospital.getName());
                intent.putExtra("hospital_phone", hospital.getPhone());
                context.startActivity(intent);
            });

            // Emergency call button (existing code)
            callButton.setOnClickListener(v -> {
                if (hospital.getPhone() != null && !hospital.getPhone().isEmpty()) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + hospital.getPhone()));
                    context.startActivity(callIntent);
                } else {
                    Toast.makeText(context, "No emergency number available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

