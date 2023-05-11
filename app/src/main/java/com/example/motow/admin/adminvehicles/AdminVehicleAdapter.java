package com.example.motow.admin.adminvehicles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.motow.databinding.UserCardViewBinding;
import com.example.motow.users.UsersAdapter;
import com.example.motow.vehicles.Vehicle;
import com.example.motow.vehicles.VehicleListener;

import java.util.List;

public class AdminVehicleAdapter extends RecyclerView.Adapter<AdminVehicleAdapter.VehicleViewHolder> {

    private final List<Vehicle> vehicleList;
    private final VehicleListener vehicleListener;

    public AdminVehicleAdapter(List<Vehicle> vehicleList, VehicleListener vehicleListener) {
        this.vehicleList = vehicleList;
        this.vehicleListener = vehicleListener;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        UserCardViewBinding userCardViewBinding = UserCardViewBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new VehicleViewHolder(userCardViewBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        holder.setVehicleData(vehicleList.get(position));
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    class VehicleViewHolder extends RecyclerView.ViewHolder {
        UserCardViewBinding binding;

        VehicleViewHolder(UserCardViewBinding userCardViewBinding) {
            super(userCardViewBinding.getRoot());
            binding = userCardViewBinding;
        }

        void setVehicleData(Vehicle vehicle) {
            binding.profileImage.setVisibility(View.GONE);
            binding.userName.setText(vehicle.plateNumber);
            binding.getRoot().setOnClickListener(v -> vehicleListener.onVehicleClicked(vehicle));
        }
    }
}
