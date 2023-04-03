package com.example.motow;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class UserInfoActivity extends AppCompatActivity {

    // Firebase
    FirebaseAuth fAuth = FirebaseAuth.getInstance();
    FirebaseFirestore fStore = FirebaseFirestore.getInstance();
    String userId = fAuth.getCurrentUser().getUid();

    // Interface
    RelativeLayout companyLayout;
    ImageView backBtn;
    TextView fullName, email, phoneNo, editCompany;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        backBtn = findViewById(R.id.back_btn);

        fullName = findViewById(R.id.fullname);
        email = findViewById(R.id.email);
        phoneNo = findViewById(R.id.phone_no);
        editCompany = findViewById(R.id.edit_company);
        companyLayout = findViewById(R.id.company_layout);

        DocumentReference df = fStore.collection("Users").document(userId);
        // extract the data from the document
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // identify the user access level
                if (documentSnapshot.getString("isRider") != null) {
                    // user is a rider
                }
                if (documentSnapshot.getString("isTower") != null) {
                    // user is a rider
                    companyLayout.setVisibility(View.VISIBLE);
                    editCompany.setVisibility(View.VISIBLE);
                }
            }
        });

        editCompany.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), EditCompanyActivity.class));
                finish();
            }
        });

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
                fullName.setText(value.getString("fullName"));
                email.setText(value.getString("email"));
                phoneNo.setText(value.getString("phoneNumber"));
            }
        });
    }
}