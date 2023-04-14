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
import android.net.Uri;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;
    private CollectionReference vehicleRef;

    // Nav bar
    private ImageView pfp, chatBtn, notifybtn, manageBtn;

    // Rider container
    private String riderId;
    private RelativeLayout riderContainer;
    private TextView riderName, riderVehicle, riderPlate;
    private Button acceptBtn, rejectBtn;

    // Rider bar
    private RelativeLayout riderBar;
    private ImageView riderPfp;
    private TextView riderBarVehicle, riderBarPlate;

    // Interface
    private TextView userName;
    private Button offlineBtn, onlineBtn;
    private String currentProcessId, riderCurrentVehicle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tower);

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();
        vehicleRef = fStore.collection("Vehicles");

        // Nav bar
        manageBtn = findViewById(R.id.manage_btn);

        // Rider container
        riderContainer = findViewById(R.id.rider_container);
        riderName = findViewById(R.id.rider_name);
        riderVehicle = findViewById(R.id.rider_vehicle);
        riderPlate = findViewById(R.id.rider_plate);
        acceptBtn = findViewById(R.id.accept_btn);
        rejectBtn = findViewById(R.id.reject_btn);

        // Rider bar
        riderBar = findViewById(R.id.rider_bar);
        riderPfp = findViewById(R.id.rider_bar_pfp);
        riderBarVehicle = findViewById(R.id.rider_bar_vehicle);
        riderBarPlate = findViewById(R.id.rider_bar_plate);

        // Interface
        pfp = findViewById(R.id.welcome_pfp);
        userName = findViewById(R.id.user_name);
        offlineBtn = findViewById(R.id.offline_btn);
        onlineBtn = findViewById(R.id.online_btn);

        // Set profile picture
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
                getAssistance(userId);
            }
        });
        onlineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                offlineBtn.setVisibility(View.VISIBLE);
                onlineBtn.setVisibility(View.GONE);
                changeStatusToOffline();
            }
        });

        // Set username
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

                    // Update current coordinate in database
                    updateCurrentLocation(latitude, longitude);

                    LatLng currentLocation = new LatLng(latitude, longitude);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f));
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

    private void updateCurrentLocation(double latitude, double longitude) {
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
                        //
                    }
                });
    }

    private void getAssistance(String userId) {
        fStore.collection("Processes")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        for(QueryDocumentSnapshot documentSnapshot:value){
                            riderId = null;
                            riderId = documentSnapshot.getString("riderId");

                            // Find on-going process
                            fStore.collection("Processes")
                                    .whereEqualTo("towerId", userId)
                                    .whereEqualTo("riderId", riderId)
                                    .whereEqualTo("processStatus", "requesting")
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if(task.isSuccessful()){
                                                for(QueryDocumentSnapshot document: task.getResult()){
                                                    riderContainer.setVisibility(View.VISIBLE);
                                                    onlineBtn.setVisibility(View.GONE);

                                                    currentProcessId = document.getId();

                                                    fStore.collection("Users")
                                                            .document(riderId)
                                                            .get()
                                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                    riderName.setText(documentSnapshot.getString("fullName"));
                                                                    riderCurrentVehicle = documentSnapshot.getString("currentVehicle");
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
                                                    acceptBtn.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            riderContainer.setVisibility(View.GONE);
                                                            riderBar.setVisibility(View.VISIBLE);

                                                            HashMap<String, Object> userStatus = new HashMap<>();
                                                            userStatus.put("status", "onduty");

                                                            fStore.collection("Users")
                                                                    .document(userId)
                                                                    .update(userStatus)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            //
                                                                        }
                                                                    });

                                                            HashMap<String, Object> updateStatus = new HashMap<>();
                                                            updateStatus.put("processStatus", "ongoing");

                                                            fStore.collection("Processes")
                                                                    .document(currentProcessId)
                                                                    .update(updateStatus)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            Toast.makeText(TowerActivity.this, "Request has been accepted", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });

                                                            fStore.collection("Vehicles")
                                                                    .document(riderCurrentVehicle)
                                                                    .get()
                                                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                        @Override
                                                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                            riderBarVehicle.setText(documentSnapshot.getString("brand") + " " + documentSnapshot.getString("model") + " (" + documentSnapshot.getString("color") + ")");
                                                                            riderBarPlate.setText(documentSnapshot.getString("plateNumber"));
                                                                        }
                                                                    });

                                                            fStore.collection("Users")
                                                                    .document(riderId)
                                                                    .get()
                                                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                        @Override
                                                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                            double riderLatitude = documentSnapshot.getDouble("latitude");
                                                                            double riderLongitude = documentSnapshot.getDouble("longitude");

                                                                            LatLng riderLocation = new LatLng(riderLatitude, riderLongitude);
                                                                            mMap.addMarker(new MarkerOptions().position(riderLocation).title(documentSnapshot.getString("fullName")));

                                                                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + riderLatitude + "," + riderLongitude));
                                                                            intent.setPackage("com.google.android.apps.maps");

                                                                            if(intent.resolveActivity(getPackageManager()) != null){
                                                                                startActivity(intent);
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    });

                                                    rejectBtn.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            riderContainer.setVisibility(View.GONE);
                                                            offlineBtn.setVisibility(View.VISIBLE);

                                                            HashMap<String, Object> updateStatus = new HashMap<>();
                                                            updateStatus.put("processStatus", "rejected");

                                                            fStore.collection("Processes")
                                                                    .document(currentProcessId)
                                                                    .update(updateStatus)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            Toast.makeText(TowerActivity.this, "Request has been rejected", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    });
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