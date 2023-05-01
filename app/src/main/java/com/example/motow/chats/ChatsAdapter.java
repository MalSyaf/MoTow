package com.example.motow.chats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.motow.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.MyViewHolder> {

    private final List<Chats> chatsList;
    private final Context context;

    public ChatsAdapter(Context context, List<Chats> chatsList) {
        this.chatsList = chatsList;
        this.context = context;
    }

    @NonNull
    @Override
    public ChatsAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_container_sent_message, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatsAdapter.MyViewHolder holder, int position) {
        Chats chats = chatsList.get(position);

        holder.message.setText(chats.getMessage());
    }

    @Override
    public int getItemCount() {
        return chatsList.size();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView message;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.textMessage);
        }
    }
}
