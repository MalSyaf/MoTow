package com.example.motow.chats;

import android.content.ClipData;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.motow.R;
import com.example.motow.databinding.ItemContainerSentMessageBinding;
import com.example.motow.databinding.ItemLayoutReceiveMessageBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ChatsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Chats> chatMessages;
    private final String senderId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public ChatsAdapter(List<Chats> chatsMessages, String senderId) {
        this.chatMessages = chatsMessages;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                  ItemContainerSentMessageBinding.inflate(
                                    LayoutInflater.from(parent.getContext()),
                                    parent,
                                    false
                          )
                  );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemLayoutReceiveMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).sender.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;
        public SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(Chats chats) {
            binding.textMessage.setText(chats.message);
            binding.textDateTime.setText(chats.dateTime);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemLayoutReceiveMessageBinding binding;
        ReceivedMessageViewHolder(ItemLayoutReceiveMessageBinding itemLayoutReceiveMessageBinding) {
            super(itemLayoutReceiveMessageBinding.getRoot());
            binding = itemLayoutReceiveMessageBinding;
        }
        void setData(Chats chats) {
            binding.textMessage.setText(chats.message);
            binding.textDateTime.setText(chats.dateTime);
        }
    }
}
