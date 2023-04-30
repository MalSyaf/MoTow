package com.example.motow.chats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.motow.R;
import com.example.motow.databinding.ActivityChatBinding;
import com.example.motow.databinding.ActivityTowerChatBinding;
import com.example.motow.databinding.ItemContainerSentMessageBinding;
import com.example.motow.databinding.ItemLayoutReceiveMessageBinding;

import java.util.ArrayList;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<Chats> chatsArrayList;
    private final String senderId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public ChatsAdapter(Context context, ArrayList<Chats> chatsArrayList, String senderId) {
        this.context = context;
        this.chatsArrayList = chatsArrayList;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public ChatsAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(context).inflate(R.layout.item_container_sent_message, parent, false);

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ChatsAdapter.MyViewHolder holder, int position) {
        Chats chats = chatsArrayList.get(position);

        holder.message.setText(chats.message);
        holder.dateTime.setText(chats.dateTime);
    }

    @Override
    public int getItemCount() {
        return chatsArrayList.size();
    }


    @Override
    public int getItemViewType(int position) {
        if(chatsArrayList.get(position).equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView message, dateTime;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.textMessage);
            dateTime = itemView.findViewById(R.id.textDateTime);
        }

        class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
            private final ItemLayoutReceiveMessageBinding binding;

            ReceivedMessageViewHolder(ItemLayoutReceiveMessageBinding itemLayoutReceiveMessageBinding) {
                super(itemLayoutReceiveMessageBinding.getRoot());
                binding = itemLayoutReceiveMessageBinding;
            }
            void setData(Chats chatMessage) {
                binding.textMessage.setText(chatMessage.message);
                binding.textDateTime.setText(chatMessage.dateTime);
            }
        }

        class SentMessageViewHolder extends RecyclerView.ViewHolder {
            private final ItemContainerSentMessageBinding binding;

            SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
                super(itemContainerSentMessageBinding.getRoot());
                binding = itemContainerSentMessageBinding;
            }
            void setData(Chats chatMessage) {
                binding.textMessage.setText(chatMessage.message);
                binding.textDateTime.setText(chatMessage.dateTime);
            }
        }
    }
}
