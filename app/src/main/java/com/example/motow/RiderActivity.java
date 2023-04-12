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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    String userId;
    CollectionReference vehicleRef;

    // Recycler View
    RecyclerView recyclerView;
    VehicleAdapter vehicleAdapter;

    // Nav bar
    ImageView pfp, chatBtn, notifybtn, manageBtn, backBtn;

    // Interface
    TextView userName, searchText, towerName, towerType, towerVehicle, towerPlate, towerBarStatus;
    Button requestBtn, confirmBtn, cancelBtn, okBtn, paymentBtn;
    RelativeLayout selectVehicle, towerContainer, towerBar;
    public String vehicleId, towerId, currentVehicleId, currentPlateNum, tCurrentVehicle, currentProcessId;
    String towerStringLatitude, towerStringLongitude, currentTowerId;
    Double towerLatitude, towerLongitude, tLatitude, tLongitude;
    LatLng towerLocation, currentLocation;

    CircleOptions circleOptions;

    private Marker mSelectedMarker = null;
    private ArrayList<Marker> mTripMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();
        vehicleRef = fStore.collection("Vehicles");

        // Welcome bar
        userName = findViewById(R.id.user_name);
        pfp = findViewById(R.id.welcome_pfp);

        // Nav bar
        manageBtn = findViewById(R.id.manage_btn);

        // Tower found container
        towerContainer = findViewById(R.id.tower_container);
        towerName = findViewById(R.id.tower_name);
        towerType = findViewById(R.id.tower_type);
        towerVehicle = findViewById(R.id.tower_vehicle);
        towerPlate = findViewById(R.id.tower_plate);

        // Tower bar
        towerBar = findViewById(R.id.tower_bar);
        towerBarStatus = findViewById(R.id.tower_bar_status);

        // Vehicle list
        selectVehicle = findViewById(R.id.select_vehicle);
        setUpRecyclerView();

        // Other interface
        backBtn = findViewById(R.id.back_btn);
        requestBtn = findViewById(R.id.request_btn);
        confirmBtn = findViewById(R.id.confirm_btn);
        searchText = findViewById(R.id.search_text);
        cancelBtn = findViewById(R.id.cancel_btn);
        okBtn = findViewById(R.id.ok_btn);
        paymentBtn = findViewById(R.id.payment_btn);

        // Set profile picture
        pfp.setImageDrawable(getResources().getDrawable(R.drawable.default_pfp));

        // Display rider name
        DocumentReference documentReference = fStore.collection("Users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                userName.setText("Hi, " + value.getString("fullName") + "!");
            }
        });

        // Nav bar buttons
        manageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RiderManageActivity.class));
                finish();
            }
        });

        // Buttons listener
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

                // Update rider's current vehicle in database
                updateCurrentVehicle();

                // Check online tower & location
                fStore.collection("Users")
                        .whereEqualTo("isTower", "1")
                        .whereEqualTo("status", "online")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(task.isSuccessful()){
                                    for (QueryDocumentSnapshot document : task.getResult()){
                                        tLatitude = document.getDouble("latitude");
                                        tLongitude = document.getDouble("longitude");

                                        float[] distance = new float[2];
                                        Location.distanceBetween( tLatitude, tLongitude, circleOptions.getCenter().latitude, circleOptions.getCenter().longitude, distance);

                                        // Check if tower's location is within the circle
                                        if(distance[0] > circleOptions.getRadius()){
                                            // Outside the radius
                                            Toast.makeText(RiderActivity.this, "No tower currently available in the area.", Toast.LENGTH_SHORT).show();
                                            cancelBtn.setVisibility(View.GONE);
                                            searchText.setVisibility(View.GONE);
                                            requestBtn.setVisibility(View.VISIBLE);
                                        } else if (distance[0] < circleOptions.getRadius()) {
                                            // Inside the radius
                                            towerId = document.getString("userId");
                                            //Toast.makeText(RiderActivity.this, "Inside", Toast.LENGTH_SHORT).show();

                                            // Add tower's marker
                                            fStore.collection("Users")
                                                    .document(towerId)
                                                            .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                                                                    mMap.clear();
                                                                    double tCurrentLatitude = value.getDouble("latitude");
                                                                    double tCurrentLongitude = value.getDouble("longitude");

                                                                    towerLocation = new LatLng(tCurrentLatitude, tCurrentLongitude);
                                                                    mMap.addMarker(new MarkerOptions().position(towerLocation).title(value.getString("fullName")).icon(BitmapDescriptorFactory.fromResource(R.drawable.tow_truck)));
                                                                }
                                                            });

                                            // Display tower's detail
                                            displayTowerInfo(towerId);

                                            // Create processes
                                            createProcess(towerId);
                                        }
                                    }
                                }
                            }
                        });
            }
        });
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                towerBar.setVisibility(View.VISIBLE);
                towerContainer.setVisibility(View.INVISIBLE);
                towerBarStatus.setText("Assistance is On The Way!");
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

        // Get vehicle id on item clicked for recycler view
        vehicleAdapter.setOnItemClickListener(new VehicleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
                currentVehicleId = documentSnapshot.getId();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
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
                    // Rider's coordinate
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Update current coordinate in database
                    updateCurrentLocation(latitude, longitude);

                    currentLocation = new LatLng(latitude, longitude);

                    // Rider's area radius
                    circleOptions = new CircleOptions()
                            .center(currentLocation)
                            .radius(10000)
                            .strokeWidth(2)
                            .strokeColor(Color.BLUE)
                            .fillColor(Color.parseColor("#500084d3"));

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                    mMap.addCircle(circleOptions);
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

    private void updateCurrentVehicle() {
        Map<String, Object> infoUpdate = new HashMap<>();
        infoUpdate.put("currentVehicle", currentVehicleId);

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

    private void displayTowerInfo(String towerId) {
        cancelBtn.setVisibility(View.GONE);
        searchText.setVisibility(View.GONE);
        towerContainer.setVisibility(View.VISIBLE);

        // Display tower's info
        fStore.collection("Users")
                .document(towerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        towerName.setText(documentSnapshot.getString("fullName"));
                        towerType.setText(documentSnapshot.getString("providerType"));

                        tCurrentVehicle = documentSnapshot.getString("currentVehicle");

                        // Get tower's current vehicle info
                        fStore.collection("Vehicles")
                                .document(tCurrentVehicle)
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        towerVehicle.setText(documentSnapshot.getString("brand") + " " + documentSnapshot.getString("model") + " (" + documentSnapshot.getString("color") + ")");
                                        towerPlate.setText(documentSnapshot.getString("plateNumber"));
                                    }
                                });
                    }
                });
    }

    // Create process table
    private void createProcess(String towerId) {
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

        // Add process table in database
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