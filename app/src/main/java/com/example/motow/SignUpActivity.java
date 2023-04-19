package com.example.motow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    EditText userName, signEmail, signPassword, signContact;
    CheckBox isRiderBox, isTowerBox;
    TextView loginRedirect;
    Button buttonSign;
    FirebaseAuth mAuth;
    FirebaseFirestore fStore;

    /*@Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            startActivity(intent);
            finish();
        }
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        userName = findViewById(R.id.username);
        signEmail = findViewById(R.id.signup_email);
        signPassword = findViewById(R.id.signup_password);
        signContact = findViewById(R.id.signup_contact);

        isRiderBox = findViewById(R.id.rider_cbox);
        isTowerBox = findViewById(R.id.tower_cbox);

        buttonSign = findViewById(R.id.signup_button);
        loginRedirect = findViewById(R.id.loginRedirectText);

        // check boxes logics
        isRiderBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    isTowerBox.setChecked(false);
                }
            }
        });

        isTowerBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    isRiderBox.setChecked(false);
                }
            }
        });

        loginRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username, email, password;

                username = String.valueOf(userName.getText());
                email = String.valueOf(signEmail.getText());
                password = String.valueOf(signPassword.getText());

                if (TextUtils.isEmpty(username)) {
                    Toast.makeText(SignUpActivity.this, "Enter full name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(SignUpActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(SignUpActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
                    return;
                }
                // checkbox validation
                if(!(isRiderBox.isChecked() || isTowerBox.isChecked())) {
                    Toast.makeText(SignUpActivity.this, "Select account type", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(SignUpActivity.this, "Account created.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                    DocumentReference df = fStore.collection("Users").document(user.getUid());
                                    Map<String,Object> userInfo = new HashMap<>();

                                    if(isRiderBox.isChecked()){
                                        userInfo.put("userId", mAuth.getCurrentUser().getUid());
                                        userInfo.put("isRider","1");
                                        userInfo.put("fullName", userName.getText().toString());
                                        userInfo.put("email", signEmail.getText().toString());
                                        userInfo.put("phoneNumber", signContact.getText().toString());
                                        userInfo.put("longitude", null);
                                        userInfo.put("latitude", null);
                                    }
                                    if(isTowerBox.isChecked()){
                                        userInfo.put("userId", mAuth.getCurrentUser().getUid());
                                        userInfo.put("isTower", "1");
                                        userInfo.put("fullName", userName.getText().toString());
                                        userInfo.put("email", signEmail.getText().toString());
                                        userInfo.put("phoneNumber", signContact.getText().toString());
                                        userInfo.put("providerType", null);
                                        userInfo.put("companyName", null);
                                        userInfo.put("companyRegNum", null);
                                        userInfo.put("currentVehicle", null);
                                        userInfo.put("longitude", null);
                                        userInfo.put("latitude", null);
                                    }

                                    df.set(userInfo);
                                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}