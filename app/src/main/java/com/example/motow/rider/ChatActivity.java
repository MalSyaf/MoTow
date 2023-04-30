package com.example.motow.rider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.motow.chats.Chats;
import com.example.motow.chats.ChatsAdapter;
import com.example.motow.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.checkerframework.checker.units.qual.A;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;
    private CollectionReference chatRef;

    // Recycler View
    private ArrayList<Chats> chatsArrayList;
    private ChatsAdapter chatsAdapter;

    private String towerId, riderId, number;
    private static final int REQUEST_CALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();
        chatRef = fStore.collection("Chats");

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
                                                                    // Display username
                                                                    fStore.collection("Users")
                                                                            .document(towerId)
                                                                            .get()
                                                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                                @Override
                                                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                                    binding.name.setText(documentSnapshot.getString("name"));
                                                                                }
                                                                            });
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

        binding.chatRecycler.setHasFixedSize(true);
        binding.chatRecycler.setLayoutManager(new LinearLayoutManager(this));
        chatsArrayList = new ArrayList<>();
        chatsAdapter = new ChatsAdapter(ChatActivity.this, chatsArrayList, userId);
        setListeners();
        eventChangeListener();
        //init();
        //listenMessages();
    }

    private void eventChangeListener() {
        fStore.collection("Chats")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if(error != null) {
                            return;
                        }
                        for(DocumentChange dc : value.getDocumentChanges()) {
                            if(dc.getType() == DocumentChange.Type.ADDED) {
                                chatsArrayList.add(dc.getDocument().toObject(Chats.class));
                            }

                            chatsAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    /*private void init() {
        chatMessages = new ArrayList<>();
        chatsAdapter = new ChatsAdapter(
                chatMessages,
                userId
        );
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.chatRecycler.setAdapter(chatsAdapter);
        binding.chatRecycler.setLayoutManager(layoutManager);
    }*/

    private void setListeners() {
        binding.backBtn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), RiderActivity.class));
        });

        binding.callBtn.setOnClickListener(view -> {
            makePhoneCall();
        });

        binding.layoutSend.setOnClickListener(view -> {
            sendMessage();
        });
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put("senderId", userId);
        message.put("receiverId", towerId);
        message.put("message", binding.inputMessage.getText().toString());
        message.put("timeStamp", new Date());
        fStore.collection("Chats")
                .add(message);
        binding.inputMessage.setText(null);
    }

    /*private void listenMessages() {
        fStore.collection("Chats")
                .whereEqualTo("senderId", userId)
                .whereEqualTo("receiverId", towerId)
                .addSnapshotListener(eventListener);
        fStore.collection("Chats")
                .whereEqualTo("senderId", towerId)
                .whereEqualTo("receiverId", userId)
                .addSnapshotListener(eventListener);
    }*/

    /*private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null) {
            return;
        }
        if(value != null) {
            int count = chatMessages.size();
            for(DocumentChange documentChange : value.getDocumentChanges()) {
                if(documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString("senderId");
                    chatMessage.receiverId = documentChange.getDocument().getString("receiverId");
                    chatMessage.message = documentChange.getDocument().getString("message");
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate("timeStamp"));
                    chatMessage.dateObject = documentChange.getDocument().getDate("timeStamp");
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if(count == 0) {
                chatsAdapter.notifyDataSetChanged();
            } else {
                chatsAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecycler.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecycler.setVisibility(View.VISIBLE);
        }
    };*/

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
                                                number = documentSnapshot.getString("contact");
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
                                                number = documentSnapshot.getString("contact");
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

}