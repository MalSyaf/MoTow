package com.example.motow.vehicles;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.motow.databinding.ActivityManageVehicleBinding;
import com.example.motow.rider.RiderManageActivity;
import com.example.motow.tower.TowerManageActivity;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;

public class ManageVehicleActivity extends AppCompatActivity {

    private ActivityManageVehicleBinding binding;

    //Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;
    private CollectionReference vehicleRef;

    // Recycler view
    VehicleAdapter vehicleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageVehicleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        vehicleRef = fStore.collection("Vehicles");
        userId = fAuth.getCurrentUser().getUid();

        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setUpRecyclerView();
        setListeners();
    }

    private void setListeners() {
        binding.backBtn.setOnClickListener(view -> {
            DocumentReference df = fStore.collection("Users").document(userId);
            // extract the data from the document
            df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    // identify the user access level
                    if (documentSnapshot.getString("isRider") != null) {
                        // user is a rider
                        Intent intent = new Intent(getApplicationContext(), RiderManageActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    if (documentSnapshot.getString("isTower") != null) {
                        // user is a rider
                        Intent intent = new Intent(getApplicationContext(), TowerManageActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        });
        binding.registerVehicle.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), RegisterVehicleActivity.class));
            finish();
        });
    }

    private void setUpRecyclerView() {
        Query query = vehicleRef.whereEqualTo("ownerId", userId);
        FirestoreRecyclerOptions<Vehicle> options = new FirestoreRecyclerOptions.Builder<Vehicle>()
                .setQuery(query, Vehicle.class)
                .build();

        vehicleAdapter = new VehicleAdapter(options);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(vehicleAdapter);

        vehicleAdapter.setOnItemClickListener(new VehicleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
                String id = documentSnapshot.getId();
                binding.makeDefault.setOnClickListener(view -> {
                    HashMap<String, Object> currentVehicle = new HashMap<>();
                    currentVehicle.put("currentVehicle", id);
                    fStore.collection("Users")
                            .document(userId)
                            .update(currentVehicle)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(ManageVehicleActivity.this, "Default vehicle has been changed", Toast.LENGTH_SHORT).show();
                            });
                });
                binding.deleteButton.setOnClickListener(view -> {
                    fStore.collection("Vehicles")
                            .document(id)
                            .delete();
                    HashMap<String, Object> currentVehicle = new HashMap<>();
                    currentVehicle.put("currentVehicle", null);
                    fStore.collection("Users")
                            .document(userId)
                            .update(currentVehicle);
                });
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        vehicleAdapter.startListening();
    }
    @Override
    protected void onStop() {
        super.onStop();
        vehicleAdapter.stopListening();
    }
}