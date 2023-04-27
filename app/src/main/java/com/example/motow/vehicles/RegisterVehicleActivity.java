package com.example.motow.vehicles;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.motow.databinding.ActivityRegisterVehicleBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterVehicleActivity extends AppCompatActivity {

    private ActivityRegisterVehicleBinding binding;

    // Firebase
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterVehicleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();

        setListeners();
    }

    private void setListeners() {
        binding.backBtn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ManageVehicleActivity.class));
            finish();
        });

        binding.resetBtn.setOnClickListener(view -> {
            binding.registerPlate.setText(null);
            binding.registerBrand.setText(null);
            binding.registerModel.setText(null);
            binding.registerColor.setText(null);
        });

        binding.registerBtn.setOnClickListener(view -> {
            if(inputIsValid()) {
                registerVehicle();
            }
        });
    }

    private void registerVehicle() {
        Map<String, Object> vehicle = new HashMap<>();
        vehicle.put("ownerId", userId);
        vehicle.put("plateNumber", binding.registerPlate.getText().toString());
        vehicle.put("brand", binding.registerBrand.getText().toString());
        vehicle.put("model", binding.registerModel.getText().toString());
        vehicle.put("color", binding.registerColor.getText().toString());

        fStore.collection("Vehicles")
                .add(vehicle)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(RegisterVehicleActivity.this, "Vehicle Registered", Toast.LENGTH_SHORT).show();
                        binding.registerPlate.setText(null);
                        binding.registerBrand.setText(null);
                        binding.registerModel.setText(null);
                        binding.registerColor.setText(null);
                        String documentId = documentReference.getId();
                        Map<String, Object> vehicleId = new HashMap<>();
                        vehicleId.put("vehicleId", documentId);
                        fStore.collection("Vehicles")
                                .document(documentId)
                                .update(vehicleId);
                        Map<String, Object> userCurrentVehicle = new HashMap<>();
                        userCurrentVehicle.put("currentVehicle", documentId);
                        fStore.collection("Users")
                                .document(userId)
                                .update(userCurrentVehicle);

                        startActivity(new Intent(getApplicationContext(), ManageVehicleActivity.class));
                        finish();
                    }
                });
    }

    private Boolean inputIsValid() {
        if(binding.registerPlate.getText().toString().trim().isEmpty()) {
            showToast("Enter vehicle plate");
            return false;
        } else if (binding.registerBrand.getText().toString().trim().isEmpty()) {
            showToast("Enter vehicle brand");
            return false;
        } else if (binding.registerModel.getText().toString().trim().isEmpty()) {
            showToast("Enter vehicle model");
            return false;
        }else if (binding.registerColor.getText().toString().trim().isEmpty()) {
            showToast("Enter vehicle color");
            return false;
        }else {
            return true;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}