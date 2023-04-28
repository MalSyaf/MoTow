package com.example.motow.tower;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.motow.NotifyActivity;
import com.example.motow.R;
import com.example.motow.databinding.ActivityTowerBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class TowerActivity extends FragmentActivity implements OnMapReadyCallback {

    private ActivityTowerBinding binding;

    // Google Map
    private GoogleMap mMap;
    private Boolean check = false;

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;
    private CollectionReference vehicleRef;

    private String riderId, currentProcessId, riderCurrentVehicle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTowerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();
        vehicleRef = fStore.collection("Vehicles");

        supportMapFragment();
        loadUserDetails();
        setListeners();
    }

    private void supportMapFragment() {
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

    private void setListeners() {
        // Navbar buttons
        binding.chatButton.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), TowerChatActivity.class));
            finish();
        });
        binding.notifyBtn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), NotifyActivity.class));
            finish();
        });
        binding.manageBtn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), TowerManageActivity.class));
            finish();
        });

        // Button listeners
        binding.offlineBtn.setOnClickListener(view -> {
            changeStatusToOnline();
            getAssistance(userId);
        });
        binding.onlineBtn.setOnClickListener(view -> {
            binding.offlineBtn.setVisibility(View.VISIBLE);
            binding.onlineBtn.setVisibility(View.GONE);
            changeStatusToOffline();
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

                            // Find request
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
                                                    binding.riderContainer.setVisibility(View.VISIBLE);
                                                    binding.onlineBtn.setVisibility(View.GONE);

                                                    currentProcessId = document.getId();

                                                    fStore.collection("Users")
                                                            .document(riderId)
                                                            .get()
                                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                    binding.riderName.setText(documentSnapshot.getString("fullName"));
                                                                    riderCurrentVehicle = documentSnapshot.getString("currentVehicle");
                                                                    fStore.collection("Vehicles")
                                                                            .document(riderCurrentVehicle)
                                                                            .get()
                                                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                    @Override
                                                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                            binding.riderVehicle.setText(documentSnapshot.getString("brand") + " " + documentSnapshot.getString("model") + " (" + documentSnapshot.getString("color") + ")");
                                                                            binding.riderPlate.setText(documentSnapshot.getString("plateNumber"));
                                                                        }
                                                                    });
                                                                }
                                                            });
                                                    binding.acceptBtn.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            binding.riderContainer.setVisibility(View.GONE);
                                                            binding.riderBar.setVisibility(View.VISIBLE);
                                                            binding.pickupBtn.setVisibility(View.VISIBLE);

                                                            HashMap<String, Object> informRider = new HashMap<>();
                                                            informRider.put("towerId", userId);
                                                            informRider.put("riderId", riderId);
                                                            informRider.put("message", "I'm on my way!");

                                                            fStore.collection("Chats")
                                                                    .add(informRider);

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
                                                                            binding.riderBarVehicle.setText(documentSnapshot.getString("brand") + " " + documentSnapshot.getString("model") + " (" + documentSnapshot.getString("color") + ")");
                                                                            binding.riderBarPlate.setText(documentSnapshot.getString("plateNumber"));
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

                                                    binding.rejectBtn.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            binding.riderContainer.setVisibility(View.GONE);
                                                            binding.offlineBtn.setVisibility(View.VISIBLE);

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

                                                            HashMap<String, Object> userStatus = new HashMap<>();
                                                            userStatus.put("status", "offline");

                                                            fStore.collection("Users")
                                                                    .document(userId)
                                                                    .update(userStatus)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            //
                                                                        }
                                                                    });
                                                        }
                                                    });

                                                    binding.pickupBtn.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            binding.pickupBtn.setVisibility(View.GONE);
                                                            binding.completeBtn.setVisibility(View.VISIBLE);

                                                            HashMap<String, Object> updateStatus = new HashMap<>();
                                                            updateStatus.put("processStatus", "towed");

                                                            fStore.collection("Processes")
                                                                    .document(currentProcessId)
                                                                    .update(updateStatus)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            Toast.makeText(TowerActivity.this, "Vehicle has been towed", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                        }
                                                    });

                                                    binding.completeBtn.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            binding.riderBar.setVisibility(View.GONE);
                                                            binding.completeBtn.setVisibility(View.GONE);
                                                            binding.onlineBtn.setVisibility(View.VISIBLE);

                                                            HashMap<String, Object> userStatus = new HashMap<>();
                                                            userStatus.put("status", "online");

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
                                                            updateStatus.put("processStatus", "completed");

                                                            fStore.collection("Processes")
                                                                    .document(currentProcessId)
                                                                    .update(updateStatus)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            Toast.makeText(TowerActivity.this, "Process has been completed", Toast.LENGTH_SHORT).show();
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
                        if(documentSnapshot.getString("companyRegNum") != null & documentSnapshot.getString("currentVehicle") != null & documentSnapshot.getString("providerType") != null){
                            Map<String, Object> infoUpdate = new HashMap<>();
                            infoUpdate.put("status", "online");

                            fStore.collection("Users")
                                    .document(userId)
                                    .update(infoUpdate)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            binding.offlineBtn.setVisibility(View.INVISIBLE);
                                            binding.onlineBtn.setVisibility(View.VISIBLE);
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