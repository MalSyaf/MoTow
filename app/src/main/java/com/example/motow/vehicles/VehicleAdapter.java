package com.example.motow.vehicles;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.motow.R;
import com.example.motow.databinding.VehicleCardViewBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private final List<Vehicle> vehicles;
    private final VehicleListener vehicleListener;

    public VehicleAdapter(List<Vehicle> vehicles, VehicleListener vehicleListener) {
        this.vehicles = vehicles;
        this.vehicleListener = vehicleListener;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VehicleCardViewBinding vehicleCardViewBinding = VehicleCardViewBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new VehicleViewHolder(vehicleCardViewBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        holder.setVehicleData(vehicles.get(position));
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    class VehicleViewHolder extends RecyclerView.ViewHolder {

        VehicleCardViewBinding binding;

        public VehicleViewHolder(VehicleCardViewBinding vehicleCardViewBinding) {
            super(vehicleCardViewBinding.getRoot());
            binding = vehicleCardViewBinding;
        }

        void setVehicleData(Vehicle vehicle) {
            FirebaseAuth fAuth = FirebaseAuth.getInstance();
            FirebaseFirestore fStore = FirebaseFirestore.getInstance();
            String userId = fAuth.getUid();

            binding.displayPlate.setText(vehicle.plateNumber);
            binding.displayBrand.setText(vehicle.brand);
            binding.displayModel.setText(vehicle.model);
            binding.displayColor.setText(vehicle.color);
            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.getString("isRider") != null) {
                            binding.vehicleImage.setImageResource(R.drawable.sportbike);
                        }
                        if (documentSnapshot.getString("isTower") != null) {
                            binding.vehicleImage.setImageResource(R.drawable.main);
                        }
                    });
            binding.getRoot().setOnClickListener(v -> {
                    vehicleListener.onVehicleClicked(vehicle);
                    binding.vehicleContainer.setCardBackgroundColor(Color.parseColor("#89CFF0"));
            });
        }
    }
}
