package com.example.motow;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.motow.databinding.ActivitySignUpBinding;
import com.example.motow.rider.RiderActivity;
import com.example.motow.tower.TowerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    private ActivitySignUpBinding binding;
    private String encodedImage, icImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        setListeners();
    }

    private void setListeners() {
        binding.loginRedirectText.setOnClickListener(v -> onBackPressed());
        binding.signupButton.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickPfpImage.launch(intent);
        });
        binding.insertIc.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickIcImage.launch(intent);
        });
    }

    private void signUp() {
        loading(true);
        String email = binding.email.getText().toString();
        String password = binding.password.getText().toString();

        fAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = fAuth.getCurrentUser();
                        Toast.makeText(SignUpActivity.this, "Account created", Toast.LENGTH_SHORT).show();
                        assert user != null;
                        DocumentReference df = fStore.collection("Users").document(user.getUid());
                        HashMap<String, Object> userInfo = new HashMap<>();

                        if (binding.radioRider.isChecked()) {
                            userInfo.put("userId", fAuth.getUid());
                            userInfo.put("isRider", "1");
                            userInfo.put("idNum", binding.icNo.getText().toString());
                            userInfo.put("name", binding.fullName.getText().toString());
                            userInfo.put("email", binding.email.getText().toString());
                            userInfo.put("contact", binding.contact.getText().toString());
                            userInfo.put("image", encodedImage);
                            userInfo.put("ic", icImage);
                            userInfo.put("currentVehicle", null);
                            userInfo.put("longitude", null);
                            userInfo.put("latitude", null);
                            userInfo.put("isVerified", null);
                            userInfo.put("isRejected", null);

                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                        }
                        if (binding.radioTower.isChecked()) {
                            userInfo.put("userId", fAuth.getUid());
                            userInfo.put("isTower", "1");
                            userInfo.put("idNum", binding.icNo.getText().toString());
                            userInfo.put("name", binding.fullName.getText().toString());
                            userInfo.put("email", binding.email.getText().toString());
                            userInfo.put("contact", binding.contact.getText().toString());
                            userInfo.put("image", encodedImage);
                            userInfo.put("ic", icImage);
                            userInfo.put("providerType", null);
                            userInfo.put("companyName", null);
                            userInfo.put("companyRegNum", null);
                            userInfo.put("currentVehicle", null);
                            userInfo.put("longitude", null);
                            userInfo.put("latitude", null);
                            userInfo.put("status", "offline");
                            userInfo.put("isVerified", null);
                            userInfo.put("isRejected", null);

                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                        }

                        df.set(userInfo);
                    } else {
                        // If sign in fails, display a message to the user.
                        loading(false);
                        Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
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

    private final ActivityResultLauncher<Intent> pickPfpImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.profileImage.setImageBitmap(bitmap);
                            binding.pfpText.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<Intent> pickIcImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            icImage = encodeImage(bitmap);
                            binding.insertIc.setText("Uploaded");
                            binding.insertIc.setBackgroundColor(Color.GREEN);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private Boolean isValidSignUpDetails() {
        if (encodedImage == null) {
            showToast("Upload profile image");
            return false;
        } else if (binding.icNo.getText().toString().trim().isEmpty()) {
            showToast("Enter identification number");
            return false;
        } else if (binding.fullName.getText().toString().trim().isEmpty()) {
            showToast("Enter full name");
            return false;
        } else if (binding.email.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.email.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.password.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else if (binding.confirmPassword.getText().toString().isEmpty()) {
            showToast("Confirm your password");
            return false;
        } else if (!binding.password.getText().toString().equals(binding.confirmPassword.getText().toString())) {
            showToast("Password & confirm password must be matched");
            return false;
        } else if (binding.contact.getText().toString().trim().isEmpty()) {
            showToast("Enter phone number");
            return false;
        } else if (icImage == null) {
            showToast("Upload identification image");
            return false;
        } else if (!(binding.radioRider.isChecked() || binding.radioTower.isChecked())) {
            showToast("Choose account type");
            return false;
        } else {
            return true;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.signupButton.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.signupButton.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}