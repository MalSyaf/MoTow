package com.example.motow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.motow.test.MapsActivity;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class ManageVehicleActivity extends AppCompatActivity {

    //Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;
    private CollectionReference vehicleRef;

    // Recycler view
    RecyclerView recyclerView;
    VehicleAdapter vehicleAdapter;

    // Interface
    ImageView backBtn, vehicleImage;
    Button registerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_vehicle);

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        vehicleRef = fStore.collection("Vehicles");
        userId = fAuth.getCurrentUser().getUid();

        // Interface
        backBtn = findViewById(R.id.back_btn);
        registerBtn = findViewById(R.id.register_btn);

        // Recycler view
        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setUpRecyclerView();

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RegisterVehicleActivity.class));
                finish();
            }
        });
    }

    private void setUpRecyclerView() {
        Query query = vehicleRef.whereEqualTo("ownerId", userId);

        FirestoreRecyclerOptions<Vehicle> options = new FirestoreRecyclerOptions.Builder<Vehicle>()
                .setQuery(query, Vehicle.class)
                .build();

        vehicleAdapter = new VehicleAdapter(options);

        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(vehicleAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                vehicleAdapter.deleteItem(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(recyclerView);
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