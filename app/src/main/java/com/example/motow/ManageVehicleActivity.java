package com.example.motow;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class ManageVehicleActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<Vehicle> vehicleArrayList;
    VehicleAdapter vehicleAdapter;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    String userId, ownerId;

    ImageView backBtn;
    Button registerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_vehicle);

        backBtn = findViewById(R.id.back_btn);
        registerBtn = findViewById(R.id.register_btn);

        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();

        vehicleArrayList = new ArrayList<Vehicle>();
        vehicleAdapter = new VehicleAdapter(ManageVehicleActivity.this, vehicleArrayList);

        recyclerView.setAdapter(vehicleAdapter);

        EventChangeListener();

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RiderManageActivity.class));
                finish();
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RegisterVehicleActivity.class));
                finish();
            }
        });

        DocumentReference documentReference = fStore.collection("Vehicles").document();
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                ownerId = value.getString("ownerId");
            }
        });
    }

    private void EventChangeListener() {
        fStore.collection("Vehicles").whereEqualTo("ownerId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if(error!=null){
                            Log.e("Firestore error",error.getMessage());
                            return;
                        }

                        for (DocumentChange dc : value.getDocumentChanges()){
                                if(dc.getType() == DocumentChange.Type.ADDED){
                                    vehicleArrayList.add(dc.getDocument().toObject(Vehicle.class));
                                }
                                vehicleAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }
}