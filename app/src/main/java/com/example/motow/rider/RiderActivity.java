package com.example.motow.rider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.motow.NotifyActivity;
import com.example.motow.R;
import com.example.motow.databinding.ActivityRiderBinding;
import com.example.motow.vehicles.RegisterVehicleActivity;
import com.example.motow.vehicles.Vehicle;
import com.example.motow.vehicles.VehicleAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback{

    private ActivityRiderBinding binding;

    // Google map
    private GoogleMap mMap;
    private Boolean check = false;

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;
    private CollectionReference vehicleRef;

    // Recycler View
    private RecyclerView recyclerView;
    private VehicleAdapter vehicleAdapter;

    // Tower
    private String towerId, tCurrentVehicle;
    private LatLng towerLocation;
    private Double tLatitude, tLongitude;

    public String currentVehicleId, processId;
    private LatLng currentLocation;
    private CircleOptions circleOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRiderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();
        vehicleRef = fStore.collection("Vehicles");

        supportMapFragment();

        // Vehicle list
        setUpRecyclerView();

        // Get vehicle id on item clicked for recycler view
        vehicleAdapter.setOnItemClickListener(new VehicleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
                currentVehicleId = documentSnapshot.getId();
            }
        });

        loadUserDetails();
        setListeners();
    }

    private void setListeners() {
        // Nav bar buttons
        binding.chatButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ChatActivity.class));
            finish();
        });
        binding.notifyBtn.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), NotifyActivity.class));
            finish();
        });
        binding.manageBtn.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), RiderManageActivity.class));
            finish();
        });

        // Buttons listener
        binding.requestBtn.setOnClickListener(view -> {
            binding.requestBtn.setVisibility(View.GONE);
            binding.selectVehicle.setVisibility(View.VISIBLE);

            loadVehicles();
        });
        binding.backBtn.setOnClickListener(view -> {
            binding.selectVehicle.setVisibility(View.GONE);
            binding.requestBtn.setVisibility(View.VISIBLE);
        });
        binding.noCurrentVehicle.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), RegisterVehicleActivity.class));
            finish();
        });
        binding.confirmBtn.setOnClickListener(view -> {
            binding.selectVehicle.setVisibility(View.GONE);
            binding.cancelBtn.setVisibility(View.VISIBLE);
            binding.searchText.setVisibility(View.VISIBLE);

            // Update rider's current vehicle in database
            updateCurrentVehicle();

            // Find tower
            getAssistance();
        });
        binding.okBtn.setOnClickListener(view -> {
            binding.towerBar.setVisibility(View.VISIBLE);
            binding.towerContainer.setVisibility(View.GONE);
            binding.towerBarStatus.setText("Assistance is On The Way!");
        });
        binding.cancelBtn.setOnClickListener(view -> {
            binding.searchText.setVisibility(View.GONE);
            binding.cancelBtn.setVisibility(View.GONE);
            binding.requestBtn.setVisibility(View.VISIBLE);
        });
        binding.towerBar.setOnClickListener(view -> {
            binding.towerContainer.setVisibility(View.VISIBLE);
            binding.towerBar.setVisibility(View.GONE);
            binding.towerBarStatus.setText("Assistance is on the way!");
        });
    }

    private void loadUserDetails() {
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        binding.userName.setText("Hi, " + documentSnapshot.getString("name") + "!");
                        byte[] bytes = Base64.decode(documentSnapshot.getString("image"), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        binding.welcomePfp.setImageBitmap(bitmap);
                    }
                });
    }

    private void loadVehicles() {
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.getString("currentVehicle") == null) {
                            binding.confirmBtn.setVisibility(View.GONE);
                            binding.noCurrentVehicle.setVisibility(View.VISIBLE);
                        } else {
                            binding.confirmBtn.setVisibility(View.VISIBLE);
                            binding.noCurrentVehicle.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void supportMapFragment() {
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

    private void getAssistance() {
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
                                    binding.cancelBtn.setVisibility(View.GONE);
                                    binding.searchText.setVisibility(View.GONE);
                                    binding.requestBtn.setVisibility(View.VISIBLE);
                                } else if (distance[0] < circleOptions.getRadius()) {
                                    // Inside the radius
                                    towerId = null;
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
                                                }
                                            });

                                    // Create processes
                                    createProcess(towerId);

                                    fStore.collection("Processes")
                                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                @Override
                                                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                                    for(QueryDocumentSnapshot documentSnapshot:value){
                                                        // Check process ongoing
                                                        fStore.collection("Processes")
                                                                .whereEqualTo("riderId", userId)
                                                                .whereEqualTo("towerId", towerId)
                                                                .whereEqualTo("processStatus", "ongoing")
                                                                .whereEqualTo("processId", processId)
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if(task.isSuccessful()){
                                                                            for(QueryDocumentSnapshot document: task.getResult()){
                                                                                // Display tower's detail
                                                                                displayTowerInfo(towerId);
                                                                                mMap.addMarker(new MarkerOptions().position(towerLocation).title(document.getString("fullName")).icon(BitmapDescriptorFactory.fromResource(R.drawable.tow_truck)));
                                                                            }
                                                                        }
                                                                    }
                                                                });

                                                        // Check process rejected
                                                       fStore.collection("Processes")
                                                                .whereEqualTo("riderId", userId)
                                                                .whereEqualTo("towerId", towerId)
                                                                .whereEqualTo("processStatus", "rejected")
                                                                .whereEqualTo("processId", processId)
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if(task.isSuccessful()){
                                                                            for(QueryDocumentSnapshot document: task.getResult()){
                                                                                getAssistance();
                                                                            }
                                                                        }
                                                                    }
                                                                });

                                                        // Check process towed
                                                        fStore.collection("Processes")
                                                                .whereEqualTo("riderId", userId)
                                                                .whereEqualTo("towerId", towerId)
                                                                .whereEqualTo("processStatus", "towed")
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if(task.isSuccessful()){
                                                                            for(QueryDocumentSnapshot document: task.getResult()){
                                                                                binding.towerBarStatus.setText("Vehicle has been towed");
                                                                            }
                                                                        }
                                                                    }
                                                                });

                                                        // Check process complete
                                                        fStore.collection("Processes")
                                                                .whereEqualTo("riderId", userId)
                                                                .whereEqualTo("towerId", towerId)
                                                                .whereEqualTo("processStatus", "completed")
                                                                .whereEqualTo("processId", processId)
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if(task.isSuccessful()){
                                                                            for(QueryDocumentSnapshot document: task.getResult()){
                                                                                binding.paymentBtn.setVisibility(View.VISIBLE);

                                                                                binding.towerBarStatus.setText("Delivered to a workshop");
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    }
                });
    }

    private void displayTowerInfo(String towerId) {
        binding.cancelBtn.setVisibility(View.GONE);
        binding.searchText.setVisibility(View.GONE);
        binding.towerContainer.setVisibility(View.VISIBLE);

        // Display tower's info
        fStore.collection("Users")
                .document(towerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        binding.towerName.setText(documentSnapshot.getString("name"));
                        binding.towerType.setText(documentSnapshot.getString("companyName"));
                        byte[] bytes = Base64.decode(documentSnapshot.getString("image"), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        binding.towerBarPfp.setImageBitmap(bitmap);
                        binding.towerPfp.setImageBitmap(bitmap);

                        tCurrentVehicle = documentSnapshot.getString("currentVehicle");

                        // Get tower's current vehicle info
                        fStore.collection("Vehicles")
                                .document(tCurrentVehicle)
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        binding.towerVehicle.setText(documentSnapshot.getString("brand") + " " + documentSnapshot.getString("model") + " (" + documentSnapshot.getString("color") + ")");
                                        binding.towerPlate.setText(documentSnapshot.getString("plateNumber"));
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
        process.put("processStatus", "requesting");
        process.put("date", date);
        process.put("time", time);

        // Add process table in database
        fStore.collection("Processes")
                .add(process)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        processId = documentReference.getId();
                        Map<String, Object> updateProcessId = new HashMap<>();
                        updateProcessId.put("processId", processId);
                        fStore.collection("Processes")
                                .document(processId)
                                .update(updateProcessId);
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