package com.example.motow.rider;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.motow.R;
import com.example.motow.databinding.ActivityRiderBinding;
import com.example.motow.utilities.ForegroundService;
import com.example.motow.vehicles.ManageVehicleActivity;
import com.example.motow.vehicles.RegisterVehicleActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.schedulers.SchedulerRunnableIntrospection;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback{

    private ActivityRiderBinding binding;

    // Google map
    private GoogleMap mMap;
    private Boolean check = false;

    // Firebase
    private FirebaseFirestore fStore;
    private String userId;

    // Stripe
    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;
    private PaymentSheet.CustomerConfiguration configuration;

    // Tower
    private String towerId, tCurrentVehicle;
    private LatLng towerLocation;
    private Double tLatitude, tLongitude;

    private static final int REQUEST_CALL = 1;
    public String processId;
    private LatLng currentLocation;
    private CircleOptions circleOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRiderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if(!foreGroundServiceRunning()) {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            startForegroundService(serviceIntent);
        }

        // Firebase
        FirebaseAuth fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getUid();

        // Stripe
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        fStore.collection("Processes")
                .whereEqualTo("riderId", userId)
                .whereEqualTo("towerId", towerId)
                .whereEqualTo("processStatus", "rejected")
                .whereEqualTo("processId", processId)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        for(QueryDocumentSnapshot document: task.getResult()){
                            fStore.collection("Processes")
                                    .document(document.getId())
                                    .delete();
                            getAssistance();
                        }
                    }
                });

        fStore.collection("Processes")
                .whereEqualTo("riderId", userId)
                .whereEqualTo("towerId", towerId)
                .whereEqualTo("processStatus", "requesting")
                .whereEqualTo("processId", processId)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        for(QueryDocumentSnapshot document: task.getResult()){
                            fStore.collection("Processes")
                                    .document(document.getId())
                                    .delete();
                        }
                    }
                });

        supportMapFragment();
        loadUserDetails();
        setListeners();
        fetchApi();
        checkProcessStatus();
    }

    public boolean foreGroundServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if(ForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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

        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    LatLng firstCamera = new LatLng(documentSnapshot.getDouble("latitude"),documentSnapshot.getDouble("longitude"));
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(firstCamera, 15);
                    mMap.moveCamera(cameraUpdate);
                });
    }

    private void loadUserDetails() {
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.userName.setText("Hi, " + documentSnapshot.getString("name") + "!");
                    byte[] bytes = Base64.decode(documentSnapshot.getString("image"), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.welcomePfp.setImageBitmap(bitmap);
                });
    }

    private void setListeners() {
        // Navbar listener
        binding.manageBtn.setOnClickListener(v -> {
            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if(documentSnapshot.getString("status") != null) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(this);
                            alert.setTitle("No Changes Can Be Made!");
                            alert.setMessage("You are not allowed to manage account while in the process of assistance");
                            alert.create().show();
                        } else {
                            startActivity(new Intent(getApplicationContext(), RiderManageActivity.class));
                            finish();
                        }
                    });
        });

        // Chat listener
        binding.chatButton.setOnClickListener(v -> {
            binding.chatLayout.setVisibility(View.VISIBLE);
            binding.chatButton.setVisibility(View.GONE);
            binding.towerBar.setVisibility(View.GONE);
            loadReceiverName();
        });
        binding.chatBackBtn.setOnClickListener(v -> {
            binding.chatLayout.setVisibility(View.GONE);
            binding.chatButton.setVisibility(View.VISIBLE);
            binding.towerBar.setVisibility(View.VISIBLE);
        });
        binding.callBtn.setOnClickListener(v ->
            makePhoneCall());

        // Buttons listener
        binding.requestBtn.setOnClickListener(view -> {
            binding.requestBtn.setVisibility(View.GONE);
            binding.selectVehicle.setVisibility(View.VISIBLE);
            loadCurrentVehicle();
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
            // Find tower
            getAssistance();
        });
        binding.okBtn.setOnClickListener(view -> {
            binding.towerBar.setVisibility(View.VISIBLE);
            binding.towerContainer.setVisibility(View.GONE);
            changeTowerStatus();
        });
        binding.cancelBtn.setOnClickListener(view -> {
            binding.searchText.setVisibility(View.GONE);
            binding.cancelBtn.setVisibility(View.GONE);
            binding.requestBtn.setVisibility(View.VISIBLE);
        });
        binding.towerBar.setOnClickListener(view -> {
            binding.towerContainer.setVisibility(View.VISIBLE);
            binding.towerBar.setVisibility(View.GONE);
            binding.towerBarStatus.setText("Assistance is on the way");
            binding.waiting.setVisibility(View.GONE);
            fStore.collection("Users")
                    .document(towerId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        LatLng firstCamera = new LatLng(documentSnapshot.getDouble("latitude"),documentSnapshot.getDouble("longitude"));
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(firstCamera, 15);
                        mMap.animateCamera(cameraUpdate);
                    });
        });
        binding.paymentBtn.setOnClickListener(view -> {
            if(paymentIntentClientSecret != null) {
                paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret,
                        new PaymentSheet.Configuration("MoTow", configuration));
            }
        });
        binding.okCompleteBtn.setOnClickListener(view -> {
            binding.requestBtn.setVisibility(View.VISIBLE);
            binding.completionContainer.setVisibility(View.GONE);
            deleteRequests();
            changeRiderStatus();
        });
        binding.changeVehicle.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ManageVehicleActivity.class));
            finish();
        });
    }

    private void deleteRequests() {
        fStore.collection("Processes")
                .whereEqualTo("towerId", userId)
                .whereEqualTo("processStatus", "rejected")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            fStore.collection("Processes")
                                    .document(document.getId())
                                    .delete();
                        }
                    }
                });
        fStore.collection("Processes")
                .whereEqualTo("towerId", userId)
                .whereEqualTo("processStatus", "requesting")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            fStore.collection("Processes")
                                    .document(document.getId())
                                    .delete();
                        }
                    }
                });
    }

    private void changeRiderStatus() {
        HashMap<String, Object> status = new HashMap<>();
        status.put("status", null);
        fStore.collection("Users")
                .document(userId)
                .update(status);
    }

    private void changeTowerStatus() {
        // Check process towed
        fStore.collection("Processes")
                .whereEqualTo("riderId", userId)
                .whereEqualTo("towerId", towerId)
                .whereEqualTo("processStatus", "towed")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        for(QueryDocumentSnapshot document: task.getResult()){
                            binding.towerBarStatus.setText("Vehicle has been towed");
                        }
                    }
                });

        // Check process paid
        fStore.collection("Processes")
                .whereEqualTo("riderId", userId)
                .whereEqualTo("towerId", towerId)
                .whereEqualTo("processId", processId)
                .whereEqualTo("processStatus", "paid")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        for(QueryDocumentSnapshot document: task.getResult()){
                            binding.towerBarStatus.setText("Confirming payment");
                        }
                    }
                });

        // Check process ongoing
        fStore.collection("Processes")
                .whereEqualTo("riderId", userId)
                .whereEqualTo("towerId", towerId)
                .whereEqualTo("processStatus", "ongoing")
                .whereEqualTo("processId", processId)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        for(QueryDocumentSnapshot document: task.getResult()){
                            binding.towerBarStatus.setText("Assistance is on the way");
                        }
                    }
                });
    }

    public void fetchApi() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://demo.codeseasy.com/apis/stripe/";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            configuration = new PaymentSheet.CustomerConfiguration(
                                    jsonObject.getString("customer"),
                                    jsonObject.getString("ephemeralKey")
                            );
                            paymentIntentClientSecret = jsonObject.getString("paymentIntent");
                            PaymentConfiguration.init(getApplicationContext(), jsonObject.getString("publishableKey"));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }){
            protected Map<String, String> getParams(){
                Map<String, String> paramV = new HashMap<>();
                paramV.put("param", "abc");
                return paramV;
            }
        };
        queue.add(stringRequest);
    }

    private void checkProcessStatus() {
        fStore.collection("Processes")
                .addSnapshotListener((value, error) -> {
                    for(QueryDocumentSnapshot documentSnapshot:value){
                        // Check process ongoing
                        fStore.collection("Processes")
                                .whereEqualTo("riderId", userId)
                                .whereEqualTo("towerId", towerId)
                                .whereEqualTo("processStatus", "ongoing")
                                .whereEqualTo("processId", processId)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful()){
                                        for(QueryDocumentSnapshot document: task.getResult()){
                                            binding.chatButton.setVisibility(View.VISIBLE);
                                            // Display tower's detail
                                            displayTowerInfo(towerId);
                                            mMap.addMarker(new MarkerOptions().position(towerLocation).title(document.getString("fullName")).icon(BitmapDescriptorFactory.fromResource(R.drawable.tow_truck)));
                                            fStore.collection("Users")
                                                    .document(towerId)
                                                    .get()
                                                    .addOnSuccessListener(documentSnapshot1 -> {
                                                        LatLng firstCamera = new LatLng(documentSnapshot1.getDouble("latitude"),documentSnapshot1.getDouble("longitude"));
                                                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(firstCamera, 15);
                                                        mMap.animateCamera(cameraUpdate);
                                                    });
                                        }
                                    }
                                });

                        // Check process towed
                        fStore.collection("Processes")
                                .whereEqualTo("riderId", userId)
                                .whereEqualTo("towerId", towerId)
                                .whereEqualTo("processStatus", "towed")
                                .get()
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful()){
                                        for(QueryDocumentSnapshot document: task.getResult()){
                                            binding.paymentBtn.setVisibility(View.VISIBLE);
                                            binding.towerBarStatus.setText("Vehicle has been towed");
                                        }
                                    }
                                });

                        // Check process completed
                        fStore.collection("Processes")
                                .whereEqualTo("riderId", userId)
                                .whereEqualTo("towerId", towerId)
                                .whereEqualTo("processId", processId)
                                .whereEqualTo("processStatus", "completed")
                                .get()
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful()){
                                        for(QueryDocumentSnapshot document: task.getResult()){
                                            mMap.clear();
                                            binding.waiting.setVisibility(View.GONE);
                                            binding.towerBar.setVisibility(View.GONE);
                                            binding.chatButton.setVisibility(View.GONE);

                                            HashMap<String, Object> status = new HashMap<>();
                                            status.put("status", null);
                                            fStore.collection("Users")
                                                    .document(userId)
                                                    .update(status);

                                            loadCompletion();
                                        }
                                    }
                                });

                        // Check process paid
                        fStore.collection("Processes")
                                .whereEqualTo("riderId", userId)
                                .whereEqualTo("towerId", towerId)
                                .whereEqualTo("processId", processId)
                                .whereEqualTo("processStatus", "paid")
                                .get()
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful()){
                                        for(QueryDocumentSnapshot document: task.getResult()){
                                            binding.towerBarStatus.setText("Confirming payment");
                                        }
                                    }
                                });
                    }
                });
    }

    private void loadReceiverName() {
        fStore.collection("Users")
                .document(towerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.chatName.setText(documentSnapshot.getString("name"));
                });
    }

    private void makePhoneCall() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        } else {
            fStore.collection("Users")
                    .document(towerId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String contact = documentSnapshot.getString("contact");
                        String dial = "tel:" + contact;
                        startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                    });
        }
    }

    private void loadCurrentVehicle() {
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.getString("currentVehicle") != null) {
                        binding.confirmBtn.setVisibility(View.VISIBLE);
                        binding.changeVehicle.setVisibility(View.VISIBLE);
                        binding.noCurrentVehicle.setVisibility(View.GONE);

                        String defaultVehicle = documentSnapshot.getString("currentVehicle");

                        fStore.collection("Vehicles")
                                .document(defaultVehicle)
                                .get()
                                .addOnSuccessListener(documentSnapshot1 -> {
                                    binding.displayPlate.setText(documentSnapshot1.getString("plateNumber"));
                                    binding.displayBrand.setText(documentSnapshot1.getString("brand"));
                                    binding.displayModel.setText(documentSnapshot1.getString("model"));
                                    binding.displayColor.setText(documentSnapshot1.getString("color"));
                                });
                    } else {
                        binding.confirmBtn.setVisibility(View.GONE);
                        binding.changeVehicle.setVisibility(View.GONE);
                        binding.vehicleContainer.setVisibility(View.GONE);
                        binding.noCurrentVehicle.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void updateCurrentLocation(double latitude, double longitude) {
        Map<String, Object> infoUpdate = new HashMap<>();
        infoUpdate.put("latitude", latitude);
        infoUpdate.put("longitude", longitude);
        fStore.collection("Users")
                .document(userId)
                .update(infoUpdate);
    }

    private void getAssistance() {
        fStore.collection("Users")
                .addSnapshotListener((value, error) ->
                    fStore.collection("Users")
                            .whereEqualTo("isTower", "1")
                            .whereEqualTo("status", "online")
                            .get()
                            .addOnCompleteListener(task -> {
                                for (QueryDocumentSnapshot document : task.getResult()){
                                    tLatitude = document.getDouble("latitude");
                                    tLongitude = document.getDouble("longitude");
                                    float[] distance = new float[2];

                                    Location.distanceBetween( tLatitude, tLongitude, circleOptions.getCenter().latitude, circleOptions.getCenter().longitude, distance);
                                    // Check if tower's location is within the circle
                                    if(distance[0] > circleOptions.getRadius()){
                                        // Outside the radius
                                        Toast.makeText(RiderActivity.this, "No assistance currently available in the area.", Toast.LENGTH_SHORT).show();
                                        binding.cancelBtn.setVisibility(View.GONE);
                                        binding.searchText.setVisibility(View.GONE);
                                        binding.requestBtn.setVisibility(View.VISIBLE);
                                    } else if (distance[0] < circleOptions.getRadius()) {
                                        // Inside the radius
                                        towerId = null;
                                        towerId = document.getString("userId");
                                        // Add tower's marker
                                        fStore.collection("Users")
                                                .document(towerId)
                                                .addSnapshotListener((value1, error1) -> {
                                                    mMap.clear();
                                                    double tCurrentLatitude = value1.getDouble("latitude");
                                                    double tCurrentLongitude = value1.getDouble("longitude");
                                                    towerLocation = new LatLng(tCurrentLatitude, tCurrentLongitude);
                                                });
                                        // Update rider's status
                                        HashMap<String, Object> status = new HashMap<>();
                                        status.put("status", "inassistance");
                                        fStore.collection("Users")
                                                .document(userId)
                                                .update(status);
                                        // Create processes
                                        createProcess(towerId);
                                    }
                                }
                            }));
    }

    private void loadCompletion() {
        binding.completionContainer.setVisibility(View.VISIBLE);
        fStore.collection("Users")
                .document(towerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.towerNameComplete.setText(documentSnapshot.getString("name"));
                    byte[] bytes = Base64.decode(documentSnapshot.getString("image"), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.towerPfpComplete.setImageBitmap(bitmap);
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
                .addOnSuccessListener(documentSnapshot -> {
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
                            .addOnSuccessListener(documentSnapshot1 -> {
                                binding.towerVehicle.setText(documentSnapshot1.getString("brand") + " " + documentSnapshot1.getString("model") + " (" + documentSnapshot1.getString("color") + ")");
                                binding.towerPlate.setText(documentSnapshot1.getString("plateNumber"));
                            });
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
                .addOnSuccessListener(documentReference -> {
                    processId = documentReference.getId();
                    Map<String, Object> updateProcessId = new HashMap<>();
                    updateProcessId.put("processId", processId);
                    fStore.collection("Processes")
                            .document(processId)
                            .update(updateProcessId);
                });
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        if(paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Payment has been canceled", Toast.LENGTH_SHORT).show();
        }
        if(paymentSheetResult instanceof PaymentSheetResult.Failed) {
            Toast.makeText(this, (((PaymentSheetResult.Failed) paymentSheetResult).getError().getMessage()), Toast.LENGTH_SHORT).show();
        }
        if(paymentSheetResult instanceof PaymentSheetResult.Completed) {
            HashMap<String, Object> updateStatus = new HashMap<>();
            updateStatus.put("processStatus", "paid");
            fStore.collection("Processes")
                    .document(processId)
                    .update(updateStatus)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Payment success", Toast.LENGTH_SHORT).show();
                        binding.paymentBtn.setVisibility(View.GONE);
                        binding.waiting.setVisibility(View.VISIBLE);
                    });
        }
    }
}