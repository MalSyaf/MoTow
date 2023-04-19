package com.example.motow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

public class RiderManageActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;

    // Interface
    private ImageView homeBtn, chatBtn, notifyBtn;
    private TextView riderName, personalInfo, manageVehicles, deleteAcc, cancelDelete, logoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_manage);

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();

        // Navbar
        homeBtn = findViewById(R.id.home_btn);
        chatBtn = findViewById(R.id.chat_btn);
        notifyBtn = findViewById(R.id.notify_btn);

        // Interface
        riderName = findViewById(R.id.rider_name);
        personalInfo = findViewById(R.id.personal_info);
        manageVehicles = findViewById(R.id.manage_vehicles);
        deleteAcc = findViewById(R.id.delete_account);
        cancelDelete = findViewById(R.id.cancel_delete);
        logoutBtn = findViewById(R.id.logout_btn);

        // Display username
        DocumentReference documentReference = fStore.collection("Users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                riderName.setText(value.getString("fullName"));
            }
        });

        // Navbar
        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RiderActivity.class));
                finish();
            }
        });

        // Manage interface
        personalInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), UserInfoActivity.class));
                finish();
            }
        });
        manageVehicles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ManageVehicleActivity.class));
                finish();
            }
        });
        deleteAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestDeletion();
            }
        });
        cancelDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelDeletion();
            }
        });
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });
    }

    private void requestDeletion() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Are you sure?");
        alert.setMessage("This account will not be accessible after 7 working days.");
        alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Delete request field
                HashMap<String, Object> delRequest = new HashMap<>();
                delRequest.put("delRequest", 1);

                fStore.collection("Users")
                        .document(userId)
                        .update(delRequest)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(RiderManageActivity.this, "Request has been sent", Toast.LENGTH_SHORT).show();
                            }
                        });

                deleteAcc.setVisibility(View.GONE);
                cancelDelete.setVisibility(View.VISIBLE);
            }
        });
        alert.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //
            }
        });
        alert.create().show();
    }

    private void cancelDeletion() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Are you sure?");
        alert.setMessage("Do you want to cancel the account deletion?");
        alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Add request field
                HashMap<String, Object> delRequest = new HashMap<>();
                delRequest.put("delRequest", FieldValue.delete());

                fStore.collection("Users")
                        .document(userId)
                        .update(delRequest)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(RiderManageActivity.this, "Account deletion has been canceled", Toast.LENGTH_SHORT).show();
                            }
                        });

                deleteAcc.setVisibility(View.VISIBLE);
                cancelDelete.setVisibility(View.GONE);
            }
        });
        alert.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //
            }
        });
        alert.create().show();
    }
}