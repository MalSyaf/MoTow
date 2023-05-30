package com.example.motow.rider;

import static com.example.motow.utilities.App.CHANNEL_1_ID;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.motow.R;
import com.example.motow.chats.Chats;
import com.example.motow.chats.ChatsAdapter;
import com.example.motow.databinding.ActivityRiderBinding;
import com.example.motow.utilities.ForegroundService;
import com.example.motow.vehicles.ManageVehicleActivity;
import com.example.motow.vehicles.RegisterVehicleActivity;
import com.example.motow.vehicles.Vehicle;
import com.example.motow.vehicles.VehicleAdapter;
import com.example.motow.vehicles.VehicleListener;
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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback, VehicleListener {

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

    // Notification
    private NotificationManagerCompat notificationManager;

    // Recycler view
    ArrayList<Vehicle> vehicleArrayList;
    VehicleAdapter vehicleAdapter;

    // Chats
    private List<Chats> chatMessages;
    private ChatsAdapter chatsAdapter;

    // Operator
    private String operatorId, tCurrentVehicle, operatorVehicle;
    private LatLng operatorLocation;
    private Double tLatitude, tLongitude;

    private static final int REQUEST_CALL = 1;
    public String currentProcessId, riderVehicle;
    private LatLng currentLocation;
    private CircleOptions circleOptions;
    private ListenerRegistration getAssistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRiderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (!foreGroundServiceRunning()) {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            startForegroundService(serviceIntent);
        }

        // Firebase
        FirebaseAuth fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getUid();

        // Notification
        notificationManager = NotificationManagerCompat.from(this);

        // Stripe
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
        fetchApi();

        // Chat adapter
        chatMessages = new ArrayList<>();
        chatsAdapter = new ChatsAdapter(chatMessages, userId);
        binding.chatRecycler.setAdapter(chatsAdapter);

        supportMapFragment();
        loadUserDetails();
        setListeners();
        checkProcessStatus();
        setUpRecyclerView();
        eventChangeListener();
    }

    private void supportMapFragment() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);
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
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);
            return;
        }
        mMap.setMyLocationEnabled(true);

        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                        LatLng firstCamera = new LatLng(documentSnapshot.getDouble("latitude"), documentSnapshot.getDouble("longitude"));
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
                    byte[] bytes = Base64.decode(documentSnapshot.getString("pfp"), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.welcomePfp.setImageBitmap(bitmap);
                });
    }

    private void setListeners() {
        // Navbar listener
        binding.manageBtn.setOnClickListener(v ->
                fStore.collection("Users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.getString("status") != null) {
                                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                                alert.setTitle("No Changes Can Be Made!");
                                alert.setMessage("You are not allowed to manage account while in the process of assistance");
                                alert.create().show();
                            } else {
                                startActivity(new Intent(getApplicationContext(), RiderManageActivity.class));
                                finish();
                            }
                        }));

        // Chat listener
        binding.chatBtn.setOnClickListener(v -> {
            binding.chatLayout.setVisibility(View.VISIBLE);
            binding.chatBtn.setVisibility(View.GONE);
            binding.operatorBar.setVisibility(View.GONE);
            listenMessages();
            loadReceiverName();
        });
        binding.chatBackBtn.setOnClickListener(v -> {
            binding.chatLayout.setVisibility(View.GONE);
            binding.chatBtn.setVisibility(View.VISIBLE);
            binding.operatorBar.setVisibility(View.VISIBLE);
        });
        binding.callBtn.setOnClickListener(v ->
                makePhoneCall());
        binding.sendBtn.setOnClickListener(v -> {
            if (binding.inputMessage.getText().toString().isEmpty()) {
                Toast.makeText(this, "Type a message", Toast.LENGTH_SHORT).show();
            } else {
                sendMessage();
            }
        });

        // Buttons listener
        binding.requestBtn.setOnClickListener(view -> {
            binding.requestBtn.setVisibility(View.GONE);
            binding.selectVehicle.setVisibility(View.VISIBLE);
        });
        binding.vehicleBackBtn.setOnClickListener(view -> {
            binding.selectVehicle.setVisibility(View.GONE);
            binding.requestBtn.setVisibility(View.VISIBLE);
        });
        binding.noVehicleBtn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), RegisterVehicleActivity.class));
            finish();
        });
        binding.okBtn.setOnClickListener(view -> {
            binding.operatorBar.setVisibility(View.VISIBLE);
            binding.operatorContainer.setVisibility(View.GONE);
            getAssistance.remove();
        });
        binding.cancelBtn.setOnClickListener(view -> {
            binding.searchText.setVisibility(View.GONE);
            binding.cancelBtn.setVisibility(View.GONE);
            binding.requestBtn.setVisibility(View.VISIBLE);
            HashMap<String, Object> updateStatus = new HashMap<>();
            updateStatus.put("processStatus", "Canceled by Rider");
            fStore.collection("Processes")
                    .document(currentProcessId)
                    .update(updateStatus);
        });
        binding.operatorBar.setOnClickListener(view -> {
            binding.operatorContainer.setVisibility(View.VISIBLE);
            binding.operatorBar.setVisibility(View.GONE);
            binding.operatorBarStatus.setText("Assistance is on the way");
            binding.waitingText.setVisibility(View.GONE);
            fStore.collection("Users")
                    .document(operatorId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        LatLng firstCamera = new LatLng(documentSnapshot.getDouble("latitude"), documentSnapshot.getDouble("longitude"));
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(firstCamera, 15);
                        mMap.animateCamera(cameraUpdate);
                    });
        });
        binding.okCompleteBtn.setOnClickListener(view -> {
            mMap.clear();
            binding.requestBtn.setVisibility(View.VISIBLE);
            binding.completionContainer.setVisibility(View.GONE);
            operatorId = null;
            HashMap<String, Object> status = new HashMap<>();
            status.put("status", null);
            fStore.collection("Users")
                    .document(userId)
                    .update(status);
        });
        binding.addVehicle.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ManageVehicleActivity.class));
            finish();
        });

        // Payment Listeners
        binding.paymentBtn.setOnClickListener(v ->
                binding.selectPayMethod.setVisibility(View.VISIBLE));
        binding.payMethodBack.setOnClickListener(v ->
                binding.selectPayMethod.setVisibility(View.GONE));
        // Payment Method 1
        binding.cashQr.setOnClickListener(v -> {
            binding.donePayment.setVisibility(View.VISIBLE);
        });
        binding.donePayment.setOnClickListener(v -> {
            updatePayment();
        });
        // Payment Method 2
        binding.card.setOnClickListener(v -> {
            if (paymentIntentClientSecret != null) {
                paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret,
                        new PaymentSheet.Configuration("MoTow", configuration));
            }
        });
        binding.confirmBtn.setOnClickListener(view -> {
            binding.selectVehicle.setVisibility(View.GONE);
            binding.cancelBtn.setVisibility(View.VISIBLE);
            binding.searchText.setVisibility(View.VISIBLE);
            createProcess();

        });
        binding.okCancelBtn.setOnClickListener(view -> {
            mMap.clear();
            binding.cancelationContainer.setVisibility(View.GONE);
            binding.requestBtn.setVisibility(View.VISIBLE);
            HashMap<String, Object> status = new HashMap<>();
            status.put("status", null);
            fStore.collection("Users")
                    .document(userId)
                    .update(status);
        });
    }

    private void checkProcessStatus() {
        fStore.collection("Processes")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                       // for (QueryDocumentSnapshot ignored : value) {
                            // Check process ongoing
                            fStore.collection("Processes")
                                    .whereEqualTo("riderId", userId)
                                    .whereEqualTo("operatorId", operatorId)
                                    .whereEqualTo("processStatus", "Ongoing")
                                    .whereEqualTo("processId", currentProcessId)
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                binding.chatBtn.setVisibility(View.VISIBLE);
                                                binding.operatorBarStatus.setText("Assistance is on the way");
                                                // Display operator's detail
                                                displayOperatorInfo(operatorId);
                                                mMap.addMarker(new MarkerOptions().position(operatorLocation).title(document.getString("fullName")).icon(BitmapDescriptorFactory.fromResource(R.drawable.tow_truck)));
                                                fStore.collection("Users")
                                                        .document(operatorId)
                                                        .get()
                                                        .addOnSuccessListener(documentSnapshot1 -> {
                                                            LatLng firstCamera = new LatLng(documentSnapshot1.getDouble("latitude"), documentSnapshot1.getDouble("longitude"));
                                                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(firstCamera, 15);
                                                            mMap.animateCamera(cameraUpdate);
                                                        });
                                                notificationAssistanceOtw();
                                                // Update rider's status
                                                HashMap<String, Object> status = new HashMap<>();
                                                status.put("status", "inassistance");
                                                fStore.collection("Users")
                                                        .document(userId)
                                                        .update(status);
                                            }
                                        }
                                    });

                            // Assistance towed
                            fStore.collection("Processes")
                                    .whereEqualTo("riderId", userId)
                                    .whereEqualTo("operatorId", operatorId)
                                    .whereEqualTo("processId", currentProcessId)
                                    .whereEqualTo("processStatus", "Towed")
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot ignored2 : task.getResult()) {
                                                binding.paymentBtn.setVisibility(View.VISIBLE);
                                                binding.operatorBarStatus.setText("Vehicle has been towed");
                                                notificationTowed();
                                            }
                                        }
                                    });

                            // Assistance paid
                            fStore.collection("Processes")
                                    .whereEqualTo("riderId", userId)
                                    .whereEqualTo("operatorId", operatorId)
                                    .whereEqualTo("processId", currentProcessId)
                                    .whereEqualTo("processStatus", "Paid")
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                binding.operatorBarStatus.setText("Confirming payment");
                                                notificationConfirmation();
                                            }
                                        }
                                    });

                            // Assistance complete
                            fStore.collection("Processes")
                                    .whereEqualTo("riderId", userId)
                                    .whereEqualTo("operatorId", operatorId)
                                    .whereEqualTo("processId", currentProcessId)
                                    .whereEqualTo("processStatus", "Completed")
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                mMap.clear();
                                                binding.waitingText.setVisibility(View.GONE);
                                                binding.operatorBar.setVisibility(View.GONE);
                                                binding.chatBtn.setVisibility(View.GONE);

                                                HashMap<String, Object> status = new HashMap<>();
                                                status.put("status", null);
                                                fStore.collection("Users")
                                                        .document(userId)
                                                        .update(status);

                                                loadCompletion();
                                            }
                                        }
                                    });

                            // Assistance canceled
                            fStore.collection("Processes")
                                    .whereEqualTo("riderId", userId)
                                    .whereEqualTo("operatorId", operatorId)
                                    .whereEqualTo("processId", currentProcessId)
                                    .whereEqualTo("processStatus", "Canceled")
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot ignored3 : task.getResult()) {
                                                mMap.clear();
                                                binding.waitingText.setVisibility(View.GONE);
                                                binding.operatorBar.setVisibility(View.GONE);
                                                binding.chatBtn.setVisibility(View.GONE);
                                                binding.operatorBar.setVisibility(View.GONE);
                                                binding.waitingText.setVisibility(View.GONE);

                                                HashMap<String, Object> status = new HashMap<>();
                                                status.put("status", null);
                                                fStore.collection("Users")
                                                        .document(userId)
                                                        .update(status);

                                                loadCancelation();
                                            }
                                        }
                                    });
                        }
                  //  }
                });
    }

    private void loadCancelation() {
        binding.cancelationContainer.setVisibility(View.VISIBLE);
        fStore.collection("Users")
                .document(operatorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.operatorNameCancel.setText(documentSnapshot.getString("name"));
                    byte[] bytes = Base64.decode(documentSnapshot.getString("pfp"), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.operatorPfpCancel.setImageBitmap(bitmap);
                });
    }

    private void loadReceiverName() {
        fStore.collection("Users")
                .document(operatorId)
                .get()
                .addOnSuccessListener(documentSnapshot ->
                        binding.chatName.setText(documentSnapshot.getString("name")));
    }

    private void makePhoneCall() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        } else {
            fStore.collection("Users")
                    .document(operatorId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String contact = documentSnapshot.getString("contact");
                        String dial = "tel:" + contact;
                        startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                    });
        }
    }

    private void sendMessage() {
        HashMap<String, Object> sendMessage = new HashMap<>();
        sendMessage.put("senderId", userId);
        sendMessage.put("receiverId", operatorId);
        sendMessage.put("message", binding.inputMessage.getText().toString());
        sendMessage.put("timestamp", new Date());
        fStore.collection("Chats")
                .add(sendMessage);
        binding.inputMessage.setText("");
    }

    private void listenMessages() {
        fStore.collection("Chats")
                .whereEqualTo("senderId", userId)
                .whereEqualTo("receiverId", operatorId)
                .addSnapshotListener(eventListener);
        fStore.collection("Chats")
                .whereEqualTo("senderId", operatorId)
                .whereEqualTo("receiverId", userId)
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    Chats chatMessage = new Chats();
                    chatMessage.sender = documentChange.getDocument().getString("senderId");
                    chatMessage.receiver = documentChange.getDocument().getString("receiverId");
                    chatMessage.message = documentChange.getDocument().getString("message");
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate("timestamp"));
                    chatMessage.dateObject = documentChange.getDocument().getDate("timestamp");
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatsAdapter.notifyDataSetChanged();
            } else {
                chatsAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecycler.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecycler.setVisibility(View.VISIBLE);
        }
    };

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void updateCurrentLocation(double latitude, double longitude) {
        Map<String, Object> infoUpdate = new HashMap<>();
        infoUpdate.put("latitude", latitude);
        infoUpdate.put("longitude", longitude);
        fStore.collection("Users")
                .document(userId)
                .update(infoUpdate);
    }

    @Override
    public void onVehicleClicked(Vehicle vehicle) {
        HashMap<String, Object> updateVehicle = new HashMap<>();
        updateVehicle.put("currentVehicle", vehicle.vehicleId);
        fStore.collection("Users")
                .document(userId)
                .update(updateVehicle);
    }

    private void createProcess() {
        currentProcessId = null;
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    //riderVehicle = documentSnapshot.getString("currentVehicle");
                    Map<String, Object> process = new HashMap<>();
                    process.put("riderId", userId);
                    process.put("riderVehicle", documentSnapshot.getString("currentVehicle"));
                    process.put("operatorId", null);
                    process.put("operatorVehicle", null);
                    process.put("processStatus", "Requesting");
                    process.put("timestamp", new Date());
                    // Add process document in database
                    fStore.collection("Processes")
                            .add(process)
                            .addOnSuccessListener(documentReference -> {
                                currentProcessId = documentReference.getId();
                                Map<String, Object> updateProcessId = new HashMap<>();
                                updateProcessId.put("processId", currentProcessId);
                                fStore.collection("Processes")
                                        .document(currentProcessId)
                                        .update(updateProcessId);
                                getAssistance(currentProcessId);
                            });
                });
    }

    private void getAssistance(String currentProcessId) {
        operatorId = null;
        getAssistance = fStore.collection("Users")
                .whereEqualTo("isOperator", "1")
                .whereEqualTo("status", "online")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            return;
                        }
                        for (QueryDocumentSnapshot doc : value) {
                            tLatitude = doc.getDouble("latitude");
                            tLongitude = doc.getDouble("longitude");
                            float[] distance = new float[2];

                            Location.distanceBetween(tLatitude, tLongitude, circleOptions.getCenter().latitude, circleOptions.getCenter().longitude, distance);

                            if (distance[0] > circleOptions.getRadius()) {
                                // Outside the radius
                                Toast.makeText(RiderActivity.this, "No assistance currently available", Toast.LENGTH_SHORT).show();
                                HashMap<String, Object> updateStatus = new HashMap<>();
                                updateStatus.put("processStatus", "No operator in area");
                                fStore.collection("Processes")
                                                .document(currentProcessId)
                                                        .update(updateStatus);
                                binding.cancelBtn.setVisibility(View.GONE);
                                binding.searchText.setVisibility(View.GONE);
                                binding.requestBtn.setVisibility(View.VISIBLE);
                            } else if (distance[0] < circleOptions.getRadius()) {
                                // Inside the radius
                                operatorId = doc.getId();
                                operatorVehicle = doc.getString("currentVehicle");
                                // Add operator's id
                                HashMap<String, Object> operatorInfo = new HashMap<>();
                                operatorInfo.put("operatorId", operatorId);
                                operatorInfo.put("operatorVehicle", operatorVehicle);
                                fStore.collection("Processes")
                                        .document(currentProcessId)
                                        .update(operatorInfo);
                                // Add operator's marker
                                fStore.collection("Users")
                                        .document(operatorId)
                                        .addSnapshotListener((value1, error1) -> {
                                            mMap.clear();
                                            double tCurrentLatitude = value1.getDouble("latitude");
                                            double tCurrentLongitude = value1.getDouble("longitude");
                                            operatorLocation = new LatLng(tCurrentLatitude, tCurrentLongitude);
                                            mMap.addMarker(new MarkerOptions().position(operatorLocation).title(value1.getString("fullName")).icon(BitmapDescriptorFactory.fromResource(R.drawable.tow_truck)));
                                        });
                            }
                        }
                    }
                });
    }


    private void loadCompletion() {
        binding.completionContainer.setVisibility(View.VISIBLE);
        fStore.collection("Users")
                .document(operatorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.operatorNameComplete.setText(documentSnapshot.getString("name"));
                    byte[] bytes = Base64.decode(documentSnapshot.getString("pfp"), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.operatorPfpComplete.setImageBitmap(bitmap);
                });
    }

    private void displayOperatorInfo(String operatorId) {
        binding.cancelBtn.setVisibility(View.GONE);
        binding.searchText.setVisibility(View.GONE);
        binding.operatorContainer.setVisibility(View.VISIBLE);

        // Display operator's info
        fStore.collection("Users")
                .document(operatorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.operatorName.setText(documentSnapshot.getString("name"));
                    binding.operatorType.setText(documentSnapshot.getString("companyName"));
                    byte[] bytes = Base64.decode(documentSnapshot.getString("pfp"), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.operatorBarPfp.setImageBitmap(bitmap);
                    binding.operatorPfp.setImageBitmap(bitmap);

                    tCurrentVehicle = documentSnapshot.getString("currentVehicle");
                    // Get operator's current vehicle info
                    fStore.collection("Vehicles")
                            .document(tCurrentVehicle)
                            .get()
                            .addOnSuccessListener(documentSnapshot1 -> {
                                binding.operatorVehicle.setText(documentSnapshot1.getString("brand") + " " + documentSnapshot1.getString("model") + " (" + documentSnapshot1.getString("color") + ")");
                                binding.operatorPlate.setText(documentSnapshot1.getString("plateNumber"));
                            });
                });
    }

    private void updatePayment() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Cash or QR");
        alert.setMessage("Cash/QR payment is successful?");
        alert.setPositiveButton("YES", (dialogInterface, i) -> {
            HashMap<String, Object> updateStatus = new HashMap<>();
            updateStatus.put("processStatus", "Paid");
            fStore.collection("Processes")
                    .document(currentProcessId)
                    .update(updateStatus)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Payment success", Toast.LENGTH_SHORT).show();
                        binding.selectPayMethod.setVisibility(View.GONE);
                        binding.paymentBtn.setVisibility(View.GONE);
                        binding.waitingText.setVisibility(View.VISIBLE);
                    });
        }).setNegativeButton("No", (dialogInterface, i) -> {
            binding.donePayment.setVisibility(View.GONE);
        });
        alert.create().show();
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Payment has been canceled", Toast.LENGTH_SHORT).show();
        }
        if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            Toast.makeText(this, (((PaymentSheetResult.Failed) paymentSheetResult).getError().getMessage()), Toast.LENGTH_SHORT).show();
        }
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            HashMap<String, Object> updateStatus = new HashMap<>();
            updateStatus.put("processStatus", "Paid");
            fStore.collection("Processes")
                    .document(currentProcessId)
                    .update(updateStatus)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Payment success", Toast.LENGTH_SHORT).show();
                        binding.selectPayMethod.setVisibility(View.GONE);
                        binding.paymentBtn.setVisibility(View.GONE);
                        binding.waitingText.setVisibility(View.VISIBLE);
                    });
        }
    }

    private void setUpRecyclerView() {
        binding.vehicleRecycler.setHasFixedSize(true);
        binding.vehicleRecycler.setLayoutManager(new LinearLayoutManager(this));
        vehicleArrayList = new ArrayList<Vehicle>();
        vehicleAdapter = new VehicleAdapter(vehicleArrayList, this);
        binding.vehicleRecycler.setAdapter(vehicleAdapter);
    }

    private void eventChangeListener() {
        fStore.collection("Vehicles").whereEqualTo("ownerId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            return;
                        }
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                vehicleArrayList.add(dc.getDocument().toObject(Vehicle.class));
                            }
                        }
                        vehicleAdapter.notifyDataSetChanged();
                        if (value.isEmpty()) {
                            binding.noVehicleText.setVisibility(View.VISIBLE);
                            binding.noVehicleBtn.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void notificationAssistanceOtw() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("MoTow")
                .setContentText("Assistance is on the way")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, notification);
    }

    private void notificationConfirmation() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("MoTow")
                .setContentText("Your payment is being confirmed")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, notification);
    }

    private void notificationTowed() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("MoTow")
                .setContentText("Your vehicle has been towed")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, notification);
    }

    public void fetchApi() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://demo.codeseasy.com/apis/stripe/";
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
        }) {
            protected Map<String, String> getParams() {
                Map<String, String> paramV = new HashMap<>();
                paramV.put("param", "abc");
                return paramV;
            }
        };
        queue.add(stringRequest);
    }

    public boolean foreGroundServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (ForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}