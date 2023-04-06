package com.example.motow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback{

    // Google Map
    private GoogleMap mMap;
    private Boolean check = false;

    // Firebase
    FirebaseAuth fAuth = FirebaseAuth.getInstance();
    FirebaseFirestore fStore = FirebaseFirestore.getInstance();
    String userId = fAuth.getCurrentUser().getUid();
    CollectionReference vehicleRef = fStore.collection("Vehicles");

    // Recycler View
    RecyclerView recyclerView;
    ArrayList<Vehicle> vehicleArrayList;
    VehicleAdapter vehicleAdapter;

    // Interface
    TextView userName, searchText, towerName, towerType, towerVehicle, towerPlate, towerBarStatus;
    ImageView pfp, chatBtn, notifybtn, manageBtn, backBtn;
    Button requestBtn, confirmBtn, cancelBtn, okBtn;
    RelativeLayout selectVehicle, towerContainer, towerBar;
    String vehicleId, towerId, currentVehicleId, currentPlateNum;
    public String towerStringLatitude, towerStringLongitude, currentTowerId;
    public Double towerLatitude, towerLongitude, tLatitude, tLongitude;
    public LatLng towerLocation, currentLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);

        userName = findViewById(R.id.user_name);
        searchText = findViewById(R.id.search_text);
        pfp = findViewById(R.id.welcome_pfp);

        manageBtn = findViewById(R.id.manage_btn);
        requestBtn = findViewById(R.id.request_btn);
        confirmBtn = findViewById(R.id.confirm_btn);
        cancelBtn = findViewById(R.id.cancel_btn);
        backBtn = findViewById(R.id.back_btn);
        okBtn = findViewById(R.id.ok_btn);

        // Tower Container Initialization
        towerContainer = findViewById(R.id.tower_container);
        towerName = findViewById(R.id.tower_name);
        towerType = findViewById(R.id.tower_type);
        towerVehicle = findViewById(R.id.tower_vehicle);
        towerPlate = findViewById(R.id.tower_plate);

        // Tower Bar Initialization
        towerBar = findViewById(R.id.tower_bar);
        towerBarStatus = findViewById(R.id.tower_bar_status);

        selectVehicle = findViewById(R.id.select_vehicle);
        setUpRecyclerView();

        // Profile picture
        pfp.setImageDrawable(getResources().getDrawable(R.drawable.default_pfp));

        // Display username
        DocumentReference documentReference = fStore.collection("Users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                userName.setText("Hi, " + value.getString("fullName") + "!");
            }
        });

        // Navbar buttons
        manageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RiderManageActivity.class));
                finish();
            }
        });

        requestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestBtn.setVisibility(View.INVISIBLE);
                selectVehicle.setVisibility(View.VISIBLE);
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectVehicle.setVisibility(View.INVISIBLE);
                requestBtn.setVisibility(View.VISIBLE);
            }
        });
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectVehicle.setVisibility(View.INVISIBLE);
                cancelBtn.setVisibility(View.VISIBLE);
                searchText.setVisibility(View.VISIBLE);

                updateCurrentVehicle();
                getAvailableTower();

            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchText.setVisibility(View.INVISIBLE);
                cancelBtn.setVisibility(View.INVISIBLE);
                requestBtn.setVisibility(View.VISIBLE);
            }
        });

        towerBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                towerContainer.setVisibility(View.VISIBLE);
                towerBar.setVisibility(View.INVISIBLE);
                towerBarStatus.setText("Assistance is on the way!");
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }



        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (check) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    currentLocation = new LatLng(latitude, longitude);


                    CircleOptions circleOptions = new CircleOptions()
                            .center(currentLocation)
                            .radius(10000)
                            .strokeWidth(2)
                            .strokeColor(Color.BLUE)
                            .fillColor(Color.parseColor("#500084d3"));

                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(currentLocation, 12)));
                    //mMap.addCircle(circleOptions);
                    updateCurrentLocation(latitude, longitude);

                    okBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            towerBar.setVisibility(View.VISIBLE);
                            towerContainer.setVisibility(View.INVISIBLE);
                            towerBarStatus.setText("Assistance is On The Way!");
                            fetchTowerId();

                        }
                    });

                    fStore.collection("Users")
                            .whereEqualTo("userId", currentTowerId)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if(task.isSuccessful()){
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            towerLatitude = document.getDouble("latitude");
                                            towerLongitude = document.getDouble("longitude");

                                            float[] distance = new float[2];
                                            Location.distanceBetween( towerLatitude, towerLongitude, circleOptions.getCenter().latitude, circleOptions.getCenter().longitude, distance);

                                            if( distance[0] > circleOptions.getRadius()  ){
                                                Toast.makeText(RiderActivity.this, "Outside", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(RiderActivity.this, "Inside", Toast.LENGTH_SHORT).show();
                                            }
                                            LatLng towerLocation = new LatLng(towerLatitude, towerLongitude);
                                            mMap.addMarker(new MarkerOptions().position(towerLocation).title("Assistance is On The Way!").icon(BitmapDescriptorFactory.fromResource(R.drawable.tow_truck)));
                                        }
                                    }
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

        vehicleAdapter.setOnItemClickListener(new VehicleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
                vehicleId = documentSnapshot.getId();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        check = true;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                        Toast.makeText(RiderActivity.this, "Coordinate Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchTowerId() {
        fStore.collection("Processes")
                .whereEqualTo("riderId", userId)
                .whereEqualTo("processStatus", "ongoing")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                currentTowerId = document.getString("towerId");
                            }
                        }
                    }
                });
    }

    private void getAvailableTower() {
        fStore.collection("Users")
                .whereEqualTo("status", "online")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                towerContainer.setVisibility(View.VISIBLE);
                                searchText.setVisibility(View.INVISIBLE);
                                cancelBtn.setVisibility(View.INVISIBLE);

                                towerId = document.getId();
                                currentVehicleId = document.getString("currentVehicle");

                                towerName.setText(document.getString("fullName"));
                                towerType.setText(document.getString("providerType"));

                                fStore.collection("Vehicles")
                                        .whereEqualTo("ownerId", towerId)
                                        .whereEqualTo("vehicleId", currentVehicleId)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if(task.isSuccessful()){
                                                    for (QueryDocumentSnapshot vehicle : task.getResult()) {
                                                        towerVehicle.setText(vehicle.getString("brand") + " " +vehicle.getString("model") + " " + "(" +vehicle.getString("color") + ")");
                                                        towerPlate.setText(vehicle.getString("plateNumber"));
                                                    }
                                                }
                                            }
                                        });

                                Date dateAndTime = Calendar.getInstance().getTime();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
                                String date = dateFormat.format(dateAndTime);
                                String time = timeFormat.format(dateAndTime);

                                Map<String, Object> process = new HashMap<>();
                                process.put("riderId", userId);
                                process.put("towerId", towerId);
                                process.put("processStatus", "ongoing");
                                process.put("paymentStatus", null);
                                process.put("date", date);
                                process.put("time", time);

                                fStore.collection("Processes")
                                        .add(process)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                String documentId = documentReference.getId();
                                                Map<String, Object> processId = new HashMap<>();
                                                processId.put("processId", documentId);
                                                fStore.collection("Processes")
                                                        .document(documentId)
                                                        .update(processId);
                                            }
                                        });
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RiderActivity.this, "No Provider Available", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateCurrentVehicle() {
        Map<String, Object> infoUpdate = new HashMap<>();
        infoUpdate.put("currentVehicle", vehicleId);

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
                        Toast.makeText(RiderActivity.this, "Error", Toast.LENGTH_SHORT).show();
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