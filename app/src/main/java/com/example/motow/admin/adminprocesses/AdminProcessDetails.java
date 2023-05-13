package com.example.motow.admin.adminprocesses;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.motow.databinding.ActivityAdminProcessDetailsBinding;
import com.example.motow.processes.Processes;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminProcessDetails extends AppCompatActivity {

    private ActivityAdminProcessDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminProcessDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Processes processes = (Processes) getIntent().getSerializableExtra("processId");

        loadAssistanceDetails(processes);
        setListeners();
    }

    private void setListeners() {
        binding.backBtn.setOnClickListener(v -> startActivity(new Intent(this, AdminProcessActivity.class)));
    }

    private void loadAssistanceDetails(Processes processes) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Processes")
                .document(processes.processId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                   binding.processStatus.setText(documentSnapshot.getString("processStatus"));
                   binding.processDateTime.setText(getReadableDateTime(documentSnapshot.getDate("timestamp")));
                   db.collection("Users")
                           .document(documentSnapshot.getString("riderId"))
                           .get()
                           .addOnSuccessListener(riderSnapshot -> {
                               binding.riderIc.setText(riderSnapshot.getString("idNum"));
                               binding.riderName.setText(riderSnapshot.getString("name"));
                               binding.riderContact.setText(riderSnapshot.getString("contact"));
                           });
                    db.collection("Users")
                            .document(documentSnapshot.getString("operatorId"))
                            .get()
                            .addOnSuccessListener(operatorSnapshot -> {
                                binding.operatorIc.setText(operatorSnapshot.getString("idNum"));
                                binding.operatorName.setText(operatorSnapshot.getString("name"));
                                binding.operatorContact.setText(operatorSnapshot.getString("contact"));
                            });
                    db.collection("Vehicles")
                            .document(documentSnapshot.getString("riderVehicle"))
                            .get()
                            .addOnSuccessListener(riderVehicle -> binding.riderPlateNo.setText(riderVehicle.getString("plateNumber")));
                    db.collection("Vehicles")
                            .document(documentSnapshot.getString("operatorVehicle"))
                            .get()
                            .addOnSuccessListener(operatorSnapshot -> binding.operatorPlateNo.setText(operatorSnapshot.getString("plateNumber")));
                });
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }
}