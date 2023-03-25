package com.example.motow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.motow.LoginActivity;
import com.example.motow.ManageVehicleActivity;
import com.example.motow.R;
import com.example.motow.RiderActivity;
import com.example.motow.RiderInfoActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.UUID;

public class RiderManageActivity extends AppCompatActivity {

    TextView logoutBtn, changePfp, personalInfo, manageVehicles, securityBtn, deleteAcc;
    ImageView pfp, homeBtn, chatBtn, notifyBtn;
    public Uri imageUri;
    FirebaseStorage storage;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_manage);

        personalInfo = findViewById(R.id.personal_info);
        manageVehicles = findViewById(R.id.manage_vehicles);

        logoutBtn = findViewById(R.id.logout_btn);
        homeBtn = findViewById(R.id.home_btn);
        pfp = findViewById(R.id.pfp);
        changePfp = findViewById(R.id.change_pfp);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        pfp.setImageDrawable(getResources().getDrawable(R.drawable.default_pfp));

        changePfp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });

        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RiderActivity.class));
                finish();
            }
        });

        personalInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RiderInfoActivity.class));
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
    }
}