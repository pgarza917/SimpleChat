package com.example.simplechat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.SubscriptionHandling;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String TAG = ChatActivity.class.getSimpleName();
    static final String USER_ID_KEY = "userId";
    static final String BODY_KEY = "body";
    static final int MAX_CHAT_MESSAGES_TO_SHOW = 50;

    Button mButtontSend;
    EditText mEditTextMessage;
    RecyclerView mRecyclerViewChat;
    ArrayList<Message> mMessageList;
    ChatAdapter mAdapter;
    // Keep track of initial load to scroll to bottom of the ListView
    boolean mFirstLoad;

    // Create handler which can run code periodically
    static final int POLL_INTERVAL = 1000; // in milliseconds
    Handler myHandler = new android.os.Handler();
    Runnable mRefreshMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            refreshMessages();
            myHandler.postDelayed(this, POLL_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // User login
        // Start with existing user if it exists
        if (ParseUser.getCurrentUser() != null) {
            startWithCurrentUser();
        } else {    // If not logged in, login as a new anonymous user
            login();
        }
        //myHandler.postDelayed(mRefreshMessagesRunnable, POLL_INTERVAL);

        // Load existing messages to begin with
        refreshMessages();

        ParseLiveQueryClient parseLiveQueryClient = ParseLiveQueryClient.Factory.getClient();

        ParseQuery<Message> parseQuery = ParseQuery.getQuery(Message.class);

        // Connect to Parse server
        SubscriptionHandling<Message> subscriptionHandling = parseLiveQueryClient.subscribe(parseQuery);

        // Listen for Create events
        subscriptionHandling.handleEvent(SubscriptionHandling.Event.CREATE, new
                SubscriptionHandling.HandleEventCallback<Message>() {
            @Override
            public void onEvent(ParseQuery<Message> query, Message object) {
                mMessageList.add(0, object);

                // Recycler View updates need to be run on the UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                        mRecyclerViewChat.scrollToPosition(0);
                    }
                });
            }
        });

    }

    // Create an anonymous user using the ParseAnonymousUtils and set UserId
    void login() {
        ParseAnonymousUtils.logIn(new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if(e != null) {
                    Log.e(TAG, "Anonymous login failed: ", e);
                } else {
                    startWithCurrentUser();
                }
            }
        });
    }

    // Get the userId from the cached currentUser object
    void startWithCurrentUser() {
        setupMessagePosting();
    }

    // Setup the button event handler that will post the entered messages to Parse
    void setupMessagePosting() {
        // Get references to button and edit text in activity chat
        mButtontSend = findViewById(R.id.btSend);
        mEditTextMessage = findViewById(R.id.etMessage);
        mRecyclerViewChat = findViewById(R.id.rvChat);
        mMessageList = new ArrayList<>();
        mFirstLoad = true;
        final String userId = ParseUser.getCurrentUser().getObjectId();
        mAdapter = new ChatAdapter(ChatActivity.this, userId, mMessageList);
        mRecyclerViewChat.setAdapter(mAdapter);

        // Set a layout manager on the Recycler View
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        linearLayoutManager.setReverseLayout(true);
        mRecyclerViewChat.setLayoutManager(linearLayoutManager);

        // When the button is clicked, create message object on Parse
        mButtontSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = mEditTextMessage.getText().toString();
                Message message = new Message();
                message.setUserId(ParseUser.getCurrentUser().getObjectId());
                message.setBody(data);
                message.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e == null) {
                            Toast.makeText(ChatActivity.this, "Successfully created message on Parse", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Failed to save message", e);
                        }
                    }
                });
                mEditTextMessage.setText(null);
            }
        });
    }

    // Query messages from Parse so we can load them into the chat adapter
    void refreshMessages() {
        // Construct query to execute
        ParseQuery<Message> query = ParseQuery.getQuery(Message.class);
        // Configure limit and set order as newest to oldest
        query.setLimit(MAX_CHAT_MESSAGES_TO_SHOW);
        query.orderByDescending("createdAt");

        // Execute query to fetch all messages from Parse asynchronously
        // Pretty much the same as SELECT in SQL
        query.findInBackground(new FindCallback<Message>() {
            @Override
            public void done(List<Message> messages, ParseException e) {
                if(e == null) {
                    mMessageList.clear();
                    mMessageList.addAll(messages);
                    mAdapter.notifyDataSetChanged(); // Update adapter
                    // Scroll to the bottom on an initial load
                    if(mFirstLoad) {
                        mRecyclerViewChat.scrollToPosition(0);
                        mFirstLoad = false;
                    }
                } else {
                    Log.e(TAG, "Error loading messages ", e);
                }
            }
        });
    }
}