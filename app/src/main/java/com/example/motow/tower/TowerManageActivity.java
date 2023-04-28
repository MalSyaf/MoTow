package com.example.motow.tower;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.motow.databinding.ActivityTowerBinding;
import com.example.motow.databinding.ActivityTowerManageBinding;
import com.example.motow.rider.ChatActivity;
import com.example.motow.LoginActivity;
import com.example.motow.utilities.Constants;
import com.example.motow.vehicles.ManageVehicleActivity;
import com.example.motow.NotifyActivity;
import com.example.motow.R;
import com.example.motow.UserInfoActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class TowerManageActivity extends AppCompatActivity {

    private ActivityTowerManageBinding binding;

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId, encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTowerManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();

        loadUserDetails();
        setListeners();
    }

    private void loadUserDetails() {
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        binding.towerName.setText(documentSnapshot.getString("name"));
                        byte[] bytes = Base64.decode(documentSnapshot.getString("image"), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        binding.pfp.setImageBitmap(bitmap);
                    }
                });
    }

    private void setListeners() {
        binding.logoutBtn.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Navbar
        binding.homeBtn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), TowerActivity.class));
            finish();
        });
        binding.notifyBtn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), NotifyActivity.class));
            finish();
        });

        // Manage interface
        binding.changePfpBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        binding.personalInfo.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), UserInfoActivity.class));
            finish();
        });
        binding.manageVehicles.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ManageVehicleActivity.class));
            finish();
        });
        binding.deleteAccount.setOnClickListener(view -> {
            requestDeletion();
        });
        binding.cancelDelete.setOnClickListener(view -> {
            cancelDeletion();
        });
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.pfp.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                            HashMap<String, Object> userInfo = new HashMap<>();
                            userInfo.put(Constants.KEY_IMAGE, encodedImage);
                            fStore.collection("Users")
                                    .document(userId)
                                    .update(userInfo);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

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
                                Toast.makeText(TowerManageActivity.this, "Request has been sent", Toast.LENGTH_SHORT).show();
                            }
                        });

                binding.deleteAccount.setVisibility(View.GONE);
                binding.cancelDelete.setVisibility(View.VISIBLE);
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
                                Toast.makeText(TowerManageActivity.this, "Account deletion has been canceled", Toast.LENGTH_SHORT).show();
                            }
                        });

                binding.deleteAccount.setVisibility(View.VISIBLE);
                binding.cancelDelete.setVisibility(View.GONE);
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