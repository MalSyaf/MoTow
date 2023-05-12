package com.example.motow.admin.adminusers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.motow.admin.AdminActivity;
import com.example.motow.databinding.ActivityUserVerificationBinding;
import com.example.motow.users.Users;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class UserVerificationActivity extends AppCompatActivity {

    private ActivityUserVerificationBinding binding;
    private FirebaseFirestore fStore;
    private Users users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        users = (Users) getIntent().getSerializableExtra("userId");
        fStore = FirebaseFirestore.getInstance();

        setListeners();
        loadUserDetails(users);
    }

    private void setListeners() {
        binding.backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), AdminActivity.class);
            startActivity(intent);
        });
        binding.icBackBtn.setOnClickListener(v ->
                binding.icLayout.setVisibility(View.GONE));
        binding.icBtn.setOnClickListener(v ->
                binding.icLayout.setVisibility(View.VISIBLE));
        binding.licenseBackBtn.setOnClickListener(v ->
                binding.licenseLayout.setVisibility(View.GONE));
        binding.licenseBtn.setOnClickListener(v ->
                binding.licenseLayout.setVisibility(View.VISIBLE));

        binding.verifyButton.setOnClickListener(v ->
                verifyUser());
        binding.rejectButton.setOnClickListener(v ->
                rejectUser());
    }

    private void loadUserDetails(Users users) {
        fStore.collection("Users")
                .document(users.userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.name.setText(documentSnapshot.getString("name"));
                    // Profile image
                    byte[] pfpImage = Base64.decode(documentSnapshot.getString("pfp"), Base64.DEFAULT);
                    Bitmap pfp = BitmapFactory.decodeByteArray(pfpImage, 0, pfpImage.length);
                    binding.profileImage.setImageBitmap(pfp);
                    if (documentSnapshot.getString("isRider") != null) {
                        binding.accType.setText("Rider");
                    }
                    if (documentSnapshot.getString("isOperator") != null) {
                        binding.accType.setText("Operator");
                        binding.companyName.setText(documentSnapshot.getString("companyName"));
                        binding.companyRegNo.setText(documentSnapshot.getString("companyRegNum"));
                        // License image
                        byte[] licenseImage = Base64.decode(documentSnapshot.getString("license"), Base64.DEFAULT);
                        Bitmap license = BitmapFactory.decodeByteArray(licenseImage, 0, licenseImage.length);
                        binding.licenseImage.setImageBitmap(license);
                    }
                    binding.idNum.setText(documentSnapshot.getString("idNum"));
                    binding.email.setText(documentSnapshot.getString("email"));
                    binding.contact.setText(documentSnapshot.getString("contact"));
                    // IC image
                    byte[] icImage = Base64.decode(documentSnapshot.getString("ic"), Base64.DEFAULT);
                    Bitmap ic = BitmapFactory.decodeByteArray(icImage, 0, icImage.length);
                    binding.icImage.setImageBitmap(ic);
                    if (documentSnapshot.getString("isVerified") != null) {
                        binding.verifyButton.setVisibility(View.GONE);
                        binding.rejectButton.setVisibility(View.GONE);
                    }
                    if (documentSnapshot.getString("delRequest") != null) {
                        binding.verifyButton.setVisibility(View.GONE);
                        binding.rejectButton.setVisibility(View.GONE);
                        binding.deleteBtn.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void verifyUser() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Are you sure?");
        alert.setMessage("Do you want to verify this user?");
        alert.setPositiveButton("YES", (dialogInterface, i) -> {
            HashMap<String, Object> update = new HashMap<>();
            update.put("isVerified", "1");
            fStore.collection("Users")
                    .document(users.userId)
                    .update(update);
            Toast.makeText(this, "Verification success", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), AdminActivity.class);
            startActivity(intent);
            finish();
        }).setNegativeButton("NO", (dialogInterface, i) ->
                Toast.makeText(this, "Verification denied", Toast.LENGTH_SHORT).show());
        alert.create().show();
    }

    private void rejectUser() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Are you sure?");
        alert.setMessage("Do you want to reject this user?");
        alert.setPositiveButton("YES", (dialogInterface, i) -> {
            HashMap<String, Object> update = new HashMap<>();
            update.put("isRejected", "1");
            fStore.collection("Users")
                    .document(users.userId)
                    .update(update);
            Toast.makeText(this, "Rejection success", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), AdminActivity.class);
            startActivity(intent);
            finish();
        }).setNegativeButton("NO", (dialogInterface, i) ->
                Toast.makeText(this, "Rejection unsuccessful", Toast.LENGTH_SHORT).show());
        alert.create().show();
    }
}