package com.example.motow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class TowerActivity extends FragmentActivity implements OnMapReadyCallback {

    // Google Map
    private GoogleMap mMap;
    private Boolean check = false;

    // Firebase
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    String userId;
    CollectionReference vehicleRef;

    // Rider container
    private RelativeLayout riderContainer;
    private TextView riderName, riderVehicle, riderPlate;
    private Button acceptBtn, rejectBtn;

    // Interface
    TextView userName;
    ImageView pfp, chatBtn, notifybtn, manageBtn;
    Button offlineBtn, onlineBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tower);

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();
        vehicleRef = fStore.collection("Vehicles");

        // Rider container
        riderContainer = findViewById(R.id.rider_container);
        riderName = findViewById(R.id.rider_name);
        riderVehicle = findViewById(R.id.rider_vehicle);
        riderPlate = findViewById(R.id.rider_plate);
        acceptBtn = findViewById(R.id.accept_btn);
        rejectBtn = findViewById(R.id.reject_btn);

        userName = findViewById(R.id.user_name);

        manageBtn = findViewById(R.id.manage_btn);
        pfp = findViewById(R.id.welcome_pfp);

        offlineBtn = findViewById(R.id.offline_btn);
        onlineBtn = findViewById(R.id.online_btn);

        userId = fAuth.getCurrentUser().getUid();

        pfp.setImageDrawable(getResources().getDrawable(R.drawable.default_pfp));

        // Navbar buttons
        manageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), TowerManageActivity.class));
                finish();
            }
        });

        // Button listeners
        offlineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeStatusToOnline();
            }
        });
        onlineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                offlineBtn.setVisibility(View.VISIBLE);
                onlineBtn.setVisibility(View.INVISIBLE);

                changeStatusToOffline();
            }
        });

        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.getString("status") == "online"){
                            getAssistance();
                        }
                    }
                });

        DocumentReference documentReference = fStore.collection("Users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                userName.setText("Hi, " + value.getString("fullName") + "!");
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (check) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng currentLocation = new LatLng(latitude, longitude);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18f));

                    Map<String, Object> infoUpdate = new HashMap<>();
                    infoUpdate.put("latitude", latitude);
                    infoUpdate.put("longitude", longitude);

                    fStore.collection("Users")
                            .document(userId)
                            .update(infoUpdate)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    //
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(TowerActivity.this, "Coordinate Error", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
            @Override
            public void onProviderEnabled(@NonNull String provider) {
                //
            }
            @Override
            public void onProviderDisabled(@NonNull String provider) {
                //
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        check = true;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private void getAssistance() {
        fStore.collection("Processes")
                .whereEqualTo("towerId", userId)
                .whereEqualTo("processStatus", "ongoing")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot document : task.getResult()){
                                String riderId = document.getString("riderId");
                                fStore.collection("Users")
                                        .document(riderId)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                riderContainer.setVisibility(View.VISIBLE);
                                                riderName.setText(documentSnapshot.getString("fullName"));
                                                String riderCurrentVehicle = documentSnapshot.getString("currentVehicle");
                                                fStore.collection("Vehicles")
                                                        .document(riderCurrentVehicle)
                                                        .get()
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                riderVehicle.setText(documentSnapshot.getString("brand") + " " + documentSnapshot.getString("model") + " (" + documentSnapshot.getString("color") + ")");
                                                                riderPlate.setText(documentSnapshot.getString("plateNumber"));
                                                            }
                                                        });
                                            }
                                        });
                            }
                        }
                    }
                });
    }

    private void changeStatusToOnline() {
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.getString("companyId") != null & documentSnapshot.getString("currentVehicle") != null){
                            Map<String, Object> infoUpdate = new HashMap<>();
                            infoUpdate.put("status", "online");

                            fStore.collection("Users")
                                    .document(userId)
                                    .update(infoUpdate)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            offlineBtn.setVisibility(View.INVISIBLE);
                                            onlineBtn.setVisibility(View.VISIBLE);
                                            Toast.makeText(TowerActivity.this, "You are online!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(TowerActivity.this, "Register company and vehicle", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void changeStatusToOffline() {
        Map<String, Object> infoUpdate = new HashMap<>();
        infoUpdate.put("status", "offline");

        fStore.collection("Users")
                .document(userId)
                .update(infoUpdate)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(TowerActivity.this, "You are offline!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}