package com.example.motow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.motow.databinding.ActivityUserInfoBinding;
import com.example.motow.operator.OperatorManageActivity;
import com.example.motow.rider.RiderManageActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {

    private ActivityUserInfoBinding binding;

    // Firebase
    private FirebaseFirestore fStore;
    private String userId, licenseImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        FirebaseAuth fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getUid();

        setListeners();
        loadLayout();
        displayUserInfo();
    }

    @SuppressLint("SetTextI18n")
    private void setListeners() {
        binding.backBtn.setOnClickListener(v ->
                fStore.collection("Users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            // Identify the user access level
                            if (documentSnapshot.getString("isRider") != null) {
                                // User is a rider
                                Intent intent = new Intent(getApplicationContext(), RiderManageActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            if (documentSnapshot.getString("isOperator") != null) {
                                // User is a rider
                                Intent intent = new Intent(getApplicationContext(), OperatorManageActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }));

        binding.manageInfoBtn.setOnClickListener(v -> {
            interfaceSetup();

            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        binding.editName.setText(documentSnapshot.getString("name"));
                        binding.editEmail.setHint(documentSnapshot.getString("email"));
                        binding.editPhone.setText(documentSnapshot.getString("contact"));
                        binding.editCompany.setText(documentSnapshot.getString("companyName"));
                        binding.editRegnum.setText(documentSnapshot.getString("companyRegNum"));
                    });
        });

        binding.cancelBtn.setOnClickListener(v ->
                showAlertDialog());

        binding.insertLicense.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            picklicenseImage.launch(intent);
        });

        binding.saveBtn.setOnClickListener(v -> {
            String nameFilled = binding.editName.getText().toString();
            String phoneFilled = binding.editPhone.getText().toString();
            String companyNameFilled = binding.editCompany.getText().toString();
            String regNumFilled = binding.editRegnum.getText().toString();

            // Update user info
            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.getString("isRider") != null) {
                            // User is a rider
                            if (TextUtils.isEmpty(nameFilled)) {
                                Toast.makeText(UserInfoActivity.this, "Enter full name", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (TextUtils.isEmpty(phoneFilled)) {
                                Toast.makeText(UserInfoActivity.this, "Enter contact number", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            binding.backBtn.setVisibility(View.VISIBLE);
                            binding.manageInfoBtn.setVisibility(View.VISIBLE);
                            binding.nameLayout.setVisibility(View.VISIBLE);
                            binding.emailLayout.setVisibility(View.VISIBLE);
                            binding.phoneLayout.setVisibility(View.VISIBLE);

                            binding.saveBtn.setVisibility(View.GONE);
                            binding.cancelBtn.setVisibility(View.GONE);
                            binding.editName.setVisibility(View.GONE);
                            binding.editEmail.setVisibility(View.GONE);
                            binding.editPhone.setVisibility(View.GONE);
                            binding.licenseLayout.setVisibility(View.GONE);

                            Map<String, Object> updateUser = new HashMap<>();
                            updateUser.put("name", nameFilled);
                            updateUser.put("contact", phoneFilled);

                            fStore.collection("Users")
                                    .document(userId)
                                    .update(updateUser)
                                    .addOnSuccessListener(unused -> {
                                        binding.infoTextview.setText("Personal Information");
                                        Toast.makeText(UserInfoActivity.this, "Information has been updated", Toast.LENGTH_SHORT).show();
                                    });
                        }
                        if (documentSnapshot.getString("isOperator") != null) {
                            // User is a operator
                            if (TextUtils.isEmpty(nameFilled)) {
                                Toast.makeText(UserInfoActivity.this, "Enter full name", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (TextUtils.isEmpty(phoneFilled)) {
                                Toast.makeText(UserInfoActivity.this, "Enter contact number", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (TextUtils.isEmpty(companyNameFilled)) {
                                Toast.makeText(UserInfoActivity.this, "Enter company name", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (TextUtils.isEmpty(regNumFilled)) {
                                Toast.makeText(UserInfoActivity.this, "Enter registration number", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            binding.backBtn.setVisibility(View.VISIBLE);
                            binding.manageInfoBtn.setVisibility(View.VISIBLE);
                            binding.nameLayout.setVisibility(View.VISIBLE);
                            binding.emailLayout.setVisibility(View.VISIBLE);
                            binding.phoneLayout.setVisibility(View.VISIBLE);
                            binding.companyLayout.setVisibility(View.VISIBLE);

                            binding.saveBtn.setVisibility(View.GONE);
                            binding.cancelBtn.setVisibility(View.GONE);
                            binding.editName.setVisibility(View.GONE);
                            binding.editEmail.setVisibility(View.GONE);
                            binding.editPhone.setVisibility(View.GONE);
                            binding.editCompany.setVisibility(View.GONE);
                            binding.editRegnum.setVisibility(View.GONE);
                            binding.licenseLayout.setVisibility(View.GONE);

                            Map<String, Object> updateUser = new HashMap<>();
                            updateUser.put("name", nameFilled);
                            updateUser.put("contact", phoneFilled);
                            updateUser.put("companyName", companyNameFilled);
                            updateUser.put("companyRegNum", regNumFilled);
                            updateUser.put("license", licenseImage);

                            fStore.collection("Users")
                                    .document(userId)
                                    .update(updateUser)
                                    .addOnSuccessListener(unused -> {
                                        binding.infoTextview.setText("Personal Information");
                                        Toast.makeText(UserInfoActivity.this, "Information has been updated", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });
            closeKeyboard();
        });
    }

    private void loadLayout() {
        // Extract the data from the document
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Identify the user access level
                    if (documentSnapshot.getString("isRider") != null) {
                        // User is a rider
                        binding.companyLayout.setVisibility(View.GONE);
                    }
                    if (documentSnapshot.getString("isOperator") != null) {
                        // User is a operator
                        binding.companyLayout.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void displayUserInfo() {
        DocumentReference documentReference = fStore.collection("Users").document(userId);
        documentReference.addSnapshotListener(this, (value, error) -> {
            assert value != null;
            binding.name.setText(value.getString("name"));
            binding.email.setText(value.getString("email"));
            binding.phoneNo.setText(value.getString("contact"));
            binding.company.setText(value.getString("companyName"));
            binding.company.setTextColor(getResources().getColor(R.color.grey_btn));
        });
    }

    @SuppressLint("SetTextI18n")
    private void showAlertDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Are you sure?");
        alert.setMessage("Do you want to continue without saving?");
        alert.setPositiveButton("YES", (dialogInterface, i) -> {
            binding.saveBtn.setVisibility(View.GONE);
            binding.cancelBtn.setVisibility(View.GONE);
            binding.editName.setVisibility(View.GONE);
            binding.editEmail.setVisibility(View.GONE);
            binding.editPhone.setVisibility(View.GONE);
            binding.editCompany.setVisibility(View.GONE);
            binding.editRegnum.setVisibility(View.GONE);
            binding.licenseLayout.setVisibility(View.GONE);

            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.getString("isRider") != null) {
                            // User is a rider
                            binding.backBtn.setVisibility(View.VISIBLE);
                            binding.manageInfoBtn.setVisibility(View.VISIBLE);
                            binding.nameLayout.setVisibility(View.VISIBLE);
                            binding.emailLayout.setVisibility(View.VISIBLE);
                            binding.phoneLayout.setVisibility(View.VISIBLE);
                        }
                        if (documentSnapshot.getString("isOperator") != null) {
                            // User is a operator
                            binding.backBtn.setVisibility(View.VISIBLE);
                            binding.manageInfoBtn.setVisibility(View.VISIBLE);
                            binding.nameLayout.setVisibility(View.VISIBLE);
                            binding.emailLayout.setVisibility(View.VISIBLE);
                            binding.phoneLayout.setVisibility(View.VISIBLE);
                            binding.companyLayout.setVisibility(View.VISIBLE);
                        }
                    });
            binding.infoTextview.setText("Personal Information");
        });
        alert.setNegativeButton("NO", (dialogInterface, i) -> {
            //
        });
        alert.create().show();
        closeKeyboard();
    }

    @SuppressLint("SetTextI18n")
    private void interfaceSetup() {
        binding.infoTextview.setText("Edit Information");
        binding.backBtn.setVisibility(View.GONE);
        binding.manageInfoBtn.setVisibility(View.GONE);
        binding.nameLayout.setVisibility(View.GONE);
        binding.emailLayout.setVisibility(View.GONE);
        binding.phoneLayout.setVisibility(View.GONE);
        binding.companyLayout.setVisibility(View.GONE);

        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.getString("isRider") != null) {
                        // User is a rider
                        binding.saveBtn.setVisibility(View.VISIBLE);
                        binding.cancelBtn.setVisibility(View.VISIBLE);
                        binding.editName.setVisibility(View.VISIBLE);
                        binding.editEmail.setVisibility(View.VISIBLE);
                        binding.editPhone.setVisibility(View.VISIBLE);
                        binding.licenseLayout.setVisibility(View.VISIBLE);
                    }
                    if (documentSnapshot.getString("isOperator") != null) {
                        // User is a operator
                        binding.saveBtn.setVisibility(View.VISIBLE);
                        binding.cancelBtn.setVisibility(View.VISIBLE);
                        binding.editName.setVisibility(View.VISIBLE);
                        binding.editEmail.setVisibility(View.VISIBLE);
                        binding.editPhone.setVisibility(View.VISIBLE);
                        binding.editCompany.setVisibility(View.VISIBLE);
                        binding.editRegnum.setVisibility(View.VISIBLE);
                        binding.licenseLayout.setVisibility(View.VISIBLE);
                    }
                    fStore.collection("Users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener(documentSnapshot1 -> {
                                if (documentSnapshot1.getString("license") != null) {
                                    binding.insertLicense.setText("Uploaded");
                                    binding.insertLicense.setBackgroundColor(Color.GREEN);
                                } else {
                                    binding.insertLicense.setText("Upload Image");
                                    binding.insertLicense.setBackgroundColor(Color.GRAY);
                                }
                            });
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

    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<Intent> picklicenseImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            licenseImage = encodeImage(bitmap);
                            binding.insertLicense.setText("Uploaded");
                            binding.insertLicense.setBackgroundColor(Color.GREEN);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}