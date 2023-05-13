package com.example.motow.admin.adminvehicles;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.motow.databinding.ActivityAdminVehicleDetailsBinding;
import com.example.motow.vehicles.Vehicle;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminVehicleDetailsActivity extends AppCompatActivity {

    private ActivityAdminVehicleDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminVehicleDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Vehicle vehicle = (Vehicle) getIntent().getSerializableExtra("vehicleId");

        loadVehicleDetails(vehicle);
        setListeners();
    }

    private void setListeners() {
        binding.backBtn.setOnClickListener(v -> startActivity(new Intent(this, AdminVehicleActivity.class)));
    }

    private void loadVehicleDetails(Vehicle vehicle) {
        FirebaseFirestore fStore = FirebaseFirestore.getInstance();
        fStore.collection("Vehicles")
                .document(vehicle.vehicleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    fStore.collection("Users")
                            .document(documentSnapshot.getString("ownerId"))
                            .get()
                            .addOnSuccessListener(documentSnapshot1 -> {
                                binding.ownerIc.setText(documentSnapshot1.getString("idNum"));
                                binding.ownerName.setText(documentSnapshot1.getString("name"));
                                binding.ownerContact.setText(documentSnapshot1.getString("contact"));
                            });
                    binding.plateNo.setText(documentSnapshot.getString("plateNumber"));
                    binding.brand.setText(documentSnapshot.getString("brand"));
                    binding.model.setText(documentSnapshot.getString("model"));
                    binding.color.setText(documentSnapshot.getString("color"));
                });
    }
}