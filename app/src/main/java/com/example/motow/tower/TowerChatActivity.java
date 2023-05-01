package com.example.motow.tower;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.motow.chats.Chats;
import com.example.motow.chats.ChatsAdapter;
import com.example.motow.R;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
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

public class TowerChatActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;
    private CollectionReference chatRef;

    // Recycler View
    private RecyclerView recyclerView;
    private ChatsAdapter chatsAdapter;

    // Interface
    private EditText inputMessage;
    private FrameLayout sendBtn;
    private ImageView callBtn;

    private String towerId, riderId, towerPhone, riderPhone, number;
    private static final int REQUEST_CALL = 1;
    public String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tower_chat);

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();
        chatRef = fStore.collection("Chats");

        // Interface
        inputMessage = findViewById(R.id.input_message);
        sendBtn = findViewById(R.id.layout_send);
        callBtn = findViewById(R.id.call_btn);

        //setUpRecyclerView();

        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makePhoneCall();
            }
        });

        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.getString("isRider") != null) {
                            fStore.collection("Processes")
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                            fStore.collection("Processes")
                                                    .whereEqualTo("riderId", userId)
                                                    .whereEqualTo("processStatus", "ongoing")
                                                    .get()
                                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                            if(task.isSuccessful()){
                                                                for(QueryDocumentSnapshot document: task.getResult()){
                                                                    towerId = document.getString("towerId");
                                                                    towerPhone = document.getString("phoneNumber");
                                                                }
                                                            }
                                                        }
                                                    });
                                        }
                                    });
                        }
                        if(documentSnapshot.getString("isTower") != null) {
                            fStore.collection("Processes")
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                            fStore.collection("Processes")
                                                    .whereEqualTo("towerId", userId)
                                                    .whereEqualTo("processStatus", "ongoing")
                                                    .get()
                                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                            if(task.isSuccessful()){
                                                                for(QueryDocumentSnapshot document: task.getResult()){
                                                                    riderId = document.getString("riderId");
                                                                }
                                                            }
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }
                });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String textMessage = inputMessage.getText().toString();

                if(TextUtils.isEmpty(textMessage)) {
                    Toast.makeText(TowerChatActivity.this, "Type a message", Toast.LENGTH_SHORT).show();
                    return;
                }

                fStore.collection("Users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if(documentSnapshot.getString("isRider") != null) {
                                    Date dateAndTime = Calendar.getInstance().getTime();
                                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
                                    String time = timeFormat.format(dateAndTime);

                                    HashMap<Object, String> sentMessage = new HashMap<>();
                                    sentMessage.put("riderId", userId);
                                    sentMessage.put("towerId", towerId);
                                    sentMessage.put("message", textMessage);
                                    sentMessage.put("time", time);

                                    fStore.collection("Chats")
                                            .add(sentMessage)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    chatId = documentReference.getId();
                                                    Map<String, Object> updateProcessId = new HashMap<>();
                                                    updateProcessId.put("processId", chatId);
                                                    fStore.collection("Processes")
                                                            .document(chatId)
                                                            .update(updateProcessId);
                                                }
                                            });
                                }
                                if(documentSnapshot.getString("isTower") != null) {
                                    Date dateAndTime = Calendar.getInstance().getTime();
                                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
                                    String time = timeFormat.format(dateAndTime);

                                    HashMap<String, Object> sentMessage = new HashMap<>();
                                    sentMessage.put("towerId", userId);
                                    sentMessage.put("riderId", riderId);
                                    sentMessage.put("message", textMessage);
                                    sentMessage.put("timeStamp", FieldValue.serverTimestamp());

                                    fStore.collection("Chats")
                                            .add(sentMessage)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    chatId = documentReference.getId();
                                                    Map<String, Object> updateProcessId = new HashMap<>();
                                                    updateProcessId.put("processId", chatId);
                                                    fStore.collection("Processes")
                                                            .document(chatId)
                                                            .update(updateProcessId);
                                                }
                                            });
                                }
                            }
                        });
                inputMessage.setText(null);
            }
        });
    }

    private void makePhoneCall() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        } else {
            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if(documentSnapshot.getString("isRider") != null) {
                                fStore.collection("Users")
                                        .document(towerId)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                number = documentSnapshot.getString("phoneNumber");
                                                String dial = "tel:" + number;
                                                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                                            }
                                        });
                            }
                            if(documentSnapshot.getString("isTower") != null) {
                                fStore.collection("Users")
                                        .document(riderId)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                number = documentSnapshot.getString("phoneNumber");
                                                String dial = "tel:" + number;
                                                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall();
            }
        }
    }

    /*private void setUpRecyclerView() {
        Query query = chatRef.whereEqualTo("towerId", userId).orderBy("timeStamp", Query.Direction.ASCENDING);
        FirestoreRecyclerOptions<Chats> options = new FirestoreRecyclerOptions.Builder<Chats>()
                .setQuery(query, Chats.class)
                .build();

        chatsAdapter = new ChatsAdapter(options);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        RecyclerView recyclerView = findViewById(R.id.chat_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatsAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        chatsAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        chatsAdapter.stopListening();
    }*/
}