package com.example.motow;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.motow.databinding.ActivitySignUpBinding;
import com.example.motow.rider.RiderActivity;
import com.example.motow.tower.TowerActivity;
import com.example.motow.utilities.Constants;
import com.example.motow.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
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
    private PreferenceManager preferenceManager;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        preferenceManager = new PreferenceManager(getApplicationContext());
        
        setListeners();
    }

    private void setListeners() {
        binding.loginRedirectText.setOnClickListener(view -> onBackPressed());
        binding.signupButton.setOnClickListener(v -> {
            if(isValidSignUpDetails()) {
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });

        // check boxes logics
        binding.riderCbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    binding.towerCbox.setChecked(false);
                }
            }
        });
        binding.towerCbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    binding.riderCbox.setChecked(false);
                }
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        loading(true);
        String email = binding.signupEmail.getText().toString();
        String password = binding.signupPassword.getText().toString();

        fAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = fAuth.getCurrentUser();
                            Toast.makeText(SignUpActivity.this, "Account created.", Toast.LENGTH_SHORT).show();
                            DocumentReference df = fStore.collection("Users").document(user.getUid());
                            HashMap<String, Object> userInfo = new HashMap<>();

                            if(binding.riderCbox.isChecked()){
                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                preferenceManager.putString(Constants.KEY_USER_ID, fAuth.getCurrentUser().getUid());
                                preferenceManager.putString(Constants.KEY_NAME, binding.userName.getText().toString());
                                preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                                userInfo.put("userId", fAuth.getCurrentUser().getUid());
                                userInfo.put("isRider","1");
                                userInfo.put(Constants.KEY_IC, binding.icNum.getText().toString());
                                userInfo.put(Constants.KEY_NAME, binding.userName.getText().toString());
                                userInfo.put(Constants.KEY_EMAIL, binding.signupEmail.getText().toString());
                                userInfo.put(Constants.KEY_PASSWORD, binding.signupPassword.getText().toString());
                                userInfo.put(Constants.KEY_CONTACT, binding.signupContact.getText().toString());
                                userInfo.put(Constants.KEY_IMAGE, encodedImage);
                                userInfo.put("currentVehicle", null);
                                userInfo.put("longitude", null);
                                userInfo.put("latitude", null);

                                Intent intent = new Intent(getApplicationContext(), RiderActivity.class);
                                startActivity(intent);
                            }
                            if(binding.towerCbox.isChecked()){
                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                preferenceManager.putString(Constants.KEY_USER_ID, fAuth.getCurrentUser().getUid());
                                preferenceManager.putString(Constants.KEY_NAME, binding.userName.getText().toString());
                                preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                                userInfo.put("userId", fAuth.getCurrentUser().getUid());
                                userInfo.put("isTower", "1");
                                userInfo.put(Constants.KEY_IC, binding.icNum.getText().toString());
                                userInfo.put(Constants.KEY_NAME, binding.userName.getText().toString());
                                userInfo.put(Constants.KEY_EMAIL, binding.signupEmail.getText().toString());
                                userInfo.put(Constants.KEY_PASSWORD, binding.signupPassword.getText().toString());
                                userInfo.put(Constants.KEY_CONTACT, binding.signupContact.getText().toString());
                                userInfo.put(Constants.KEY_IMAGE, encodedImage);
                                userInfo.put("providerType", null);
                                userInfo.put("companyName", null);
                                userInfo.put("companyRegNum", null);
                                userInfo.put("currentVehicle", null);
                                userInfo.put("longitude", null);
                                userInfo.put("latitude", null);

                                Intent intent = new Intent(getApplicationContext(), TowerActivity.class);
                                startActivity(intent);
                            }

                            df.set(userInfo);
                        } else {
                            // If sign in fails, display a message to the user.
                            loading(false);
                            Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
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

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.addPfp.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private Boolean isValidSignUpDetails() {
        if(encodedImage == null) {
            showToast("Select profile image");
            return false;
        } else if (binding.icNum.getText().toString().trim().isEmpty()) {
            showToast("Enter identification number");
            return false;
        } else if (binding.userName.getText().toString().trim().isEmpty()) {
            showToast("Enter full name");
            return false;
        } else if (binding.signupEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.signupEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.signupPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else if (binding.signupPassword2.getText().toString().isEmpty()) {
            showToast("Confirm your password");
            return false;
        } else if (!binding.signupPassword.getText().toString().equals(binding.signupPassword2.getText().toString())) {
            showToast("Password & confirm password must be matched");
            return false;
        } else if (binding.signupContact.getText().toString().trim().isEmpty()) {
            showToast("Enter phone number");
            return false;
        } else if (!(binding.riderCbox.isChecked() || binding.towerCbox.isChecked())) {
            showToast("Choose account type");
            return false;
        } else {
            return true;
        }
    }

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.signupButton.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.signupButton.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);        }
    }
}