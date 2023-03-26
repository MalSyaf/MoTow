package com.example.motow;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class UserInfoActivity extends AppCompatActivity {

    ImageView backBtn;
    TextView fullName, email, phoneNo;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        backBtn = findViewById(R.id.back_btn);

        fullName = findViewById(R.id.fullname);
        email = findViewById(R.id.email);
        phoneNo = findViewById(R.id.phone_no);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DocumentReference df = fStore.collection("Users").document(userId);
                // extract the data from the document
                df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // identify the user access level
                        if (documentSnapshot.getString("isRider") != null) {
                            // user is a rider
                            Intent intent = new Intent(getApplicationContext(), RiderManageActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        if (documentSnapshot.getString("isTower") != null) {
                            // user is a rider
                            Intent intent = new Intent(getApplicationContext(), TowerManageActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
            }
        });

        DocumentReference documentReference = fStore.collection("Users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                fullName.setText("Full Name: " + value.getString("fullName"));
                email.setText("Email: " + value.getString("email"));
                phoneNo.setText("Phone Number: " + value.getString("phoneNumber"));
            }
        });
    }
}