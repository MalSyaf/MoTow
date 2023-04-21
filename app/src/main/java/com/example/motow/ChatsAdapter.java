package com.example.motow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ChatsAdapter extends FirestoreRecyclerAdapter<Chats, ChatsAdapter.ChatHolder> {

    public ChatsAdapter(@NonNull FirestoreRecyclerOptions<Chats> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatHolder holder, int position, @NonNull Chats model) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore fStore = FirebaseFirestore.getInstance();

        fStore.collection("Users")
                        .document(userId)
                                .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                String name = documentSnapshot.getString("fullName");
                                                holder.textViewName.setText(name);
                                            }
                                        });

        Date dateAndTime = Calendar.getInstance().getTime();
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
        String time = timeFormat.format(dateAndTime);

        holder.textViewMessage.setText(model.message);
        holder.textViewTime.setText(time);
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_sent_message, parent, false);
        return new ChatHolder(v) ;
    }

    class ChatHolder extends RecyclerView.ViewHolder {

        TextView textViewName;
        TextView textViewMessage;
        TextView textViewTime;

        public ChatHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.name);
            textViewMessage = itemView.findViewById(R.id.text_message);
            textViewTime = itemView.findViewById(R.id.time);
        }
    }
}
