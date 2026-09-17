package com.example.productiondisplay.ui.slideshow.Production;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.productiondisplay.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int BOT = 0;
    private final int USER = 1;

    private List<Message> messageList = new ArrayList<>();
    private Context context;
    private OnOptionClickListener listener;

    public interface OnOptionClickListener {
        void onOptionClicked(String option);
    }

    public ChatAdapter(Context context, OnOptionClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isBot() ? BOT : USER;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == BOT) {
            view = LayoutInflater.from(context).inflate(R.layout.item_bot_message, parent, false);
            return new BotViewHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_user_message, parent, false);
            return new UserViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).bind(message);
        } else {
            ((UserViewHolder) holder).bind(message);
        }
    }

    class BotViewHolder extends RecyclerView.ViewHolder {
        TextView textBot;
        LinearLayout optionButtons;

        BotViewHolder(View itemView) {
            super(itemView);
            textBot = itemView.findViewById(R.id.textBot);
            optionButtons = itemView.findViewById(R.id.optionButtons);
        }

        void bind(Message message) {
            textBot.setText(message.getText());
            optionButtons.removeAllViews();

            if (message.getOptions() != null) {
                for (String option : message.getOptions()) {
                    Button btn = new Button(context);
                    btn.setText(option);
                    optionButtons.addView(btn);
                    btn.setOnClickListener(v -> {
                        listener.onOptionClicked(option);
                    });
                }
                optionButtons.setVisibility(View.VISIBLE);
            } else {
                optionButtons.setVisibility(View.GONE);
            }
        }
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textUser;

        UserViewHolder(View itemView) {
            super(itemView);
            textUser = itemView.findViewById(R.id.textUser);
        }

        void bind(Message message) {
            textUser.setText(message.getText());
        }
    }

}
