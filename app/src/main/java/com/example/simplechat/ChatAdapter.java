package com.example.simplechat;

import android.content.Context;
import android.text.Layout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<Message> mMessageList;
    private Context mContext;
    private String mUserId;

    public ChatAdapter(Context context, String userId, List<Message> messageList) {
        mContext = context;
        mUserId = userId;
        mMessageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Grab the message object from the ArrayList
        Message message = mMessageList.get(position);
        // Check to see if the message retrieved is from current user
        final boolean isMe = (message.getUserId() != null) && (message.getUserId().equals(mUserId));
        // Display the correct profile pic depending on who is the creator of the message
        if(isMe) {
            holder.mMeImage.setVisibility(View.VISIBLE);
            holder.mOtherImage.setVisibility(View.GONE);
            holder.mBodyText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        } else {
            holder.mOtherImage.setVisibility(View.VISIBLE);
            holder.mMeImage.setVisibility(View.GONE);
            holder.mBodyText.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        }

        final ImageView profileImage = isMe ? holder.mMeImage : holder.mOtherImage;
        Glide.with(mContext).load(getProfileUrl(message.getUserId())).into(profileImage);
        holder.mBodyText.setText(message.getBody());
    }

    // Creates a gravatar image based on the hash value obtained from userId
    private static String getProfileUrl(final String userId) {
        String hex = "";

        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            final byte[] hash = digest.digest(userId.getBytes());
            final BigInteger bigInt = new BigInteger(hash);
            hex = bigInt.abs().toString(16);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "https://www.gravatar.com/avatar/" + hex + "?d=identicon";
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mOtherImage;
        ImageView mMeImage;
        TextView mBodyText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mOtherImage = itemView.findViewById(R.id.ivProfileOther);
            mMeImage = itemView.findViewById(R.id.ivProfileMe);
            mBodyText = itemView.findViewById(R.id.tvBody);
        }
    }
}
