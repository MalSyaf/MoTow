package com.example.motow.processes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.motow.databinding.UserCardViewBinding;

import java.util.List;

public class ProcessesAdapter extends RecyclerView.Adapter<ProcessesAdapter.ProcessesViewHolder>{

    private final List<Processes> processesList;
    private final ProcessListener processListener;

    public ProcessesAdapter(List<Processes> processesList, ProcessListener processListener) {
        this.processesList = processesList;
        this.processListener = processListener;
    }

    @NonNull
    @Override
    public ProcessesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        UserCardViewBinding userCardViewBinding = UserCardViewBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ProcessesAdapter.ProcessesViewHolder(userCardViewBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProcessesViewHolder holder, int position) {
        holder.setProcessesData(processesList.get(position));
    }

    @Override
    public int getItemCount() {
        return processesList.size();
    }

    class ProcessesViewHolder extends RecyclerView.ViewHolder {

        UserCardViewBinding binding;

        ProcessesViewHolder(UserCardViewBinding userCardViewBinding) {
            super(userCardViewBinding.getRoot());
            binding = userCardViewBinding;
        }

        void setProcessesData(Processes processes) {
            binding.profileImage.setVisibility(View.GONE);
            binding.userName.setText(processes.processId);
            binding.getRoot().setOnClickListener(v -> processListener.onProcessClicked(processes));
        }
    }
}
