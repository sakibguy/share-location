package playlagom.sharelocation.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import playlagom.sharelocation.DisplayActivity;
import playlagom.sharelocation.R;
import playlagom.sharelocation.fragments.ReceivedFriendRequestsFragment;
import playlagom.sharelocation.models.KeyValue;

import static playlagom.sharelocation.DisplayActivity.loggedInUserName;

public class ReceivedFriendRequestsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ProductAdapter";

    private Context context;
    private LayoutInflater inflater;
    DatabaseReference databaseReference;
    FirebaseAuth firebaseAuth;

    // CHECK with List<KeyValue>
    // CREATE constructor to initialize context and receivedFriendRequestsList sent from ProductListActivity
//    public ProductAdapter(Context context, List<KeyValue> receivedFriendRequestsList) {
//        this.context = context;
//        inflater = LayoutInflater.from(context);
//        this.receivedFriendRequestsList = receivedFriendRequestsList;
//
//        databaseReference = FirebaseDatabase
//                .getInstance()
//                .getReference(context.getString(R.string.sharelocation));
//        firebaseAuth = FirebaseAuth.getInstance();
//    }

    public ReceivedFriendRequestsAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);

        databaseReference = FirebaseDatabase
                .getInstance()
                .getReference(context.getString(R.string.sharelocation));
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // INFLATE the layout when ViewHolder created
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_received_friend_requests, parent, false);
        MyHolder holder = new MyHolder(view);
        return holder;
    }

    // BIND receivedFriendRequestsList
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

        // GET product position of item in RecyclerView to bind receivedFriendRequestsList and assign values from list
        final MyHolder myHolder = (MyHolder) holder;
        /* Get by position */
        final KeyValue keyValue = (new ArrayList<KeyValue>(
                DisplayActivity.lhmReceivedFriendRequests.values())).get(position);

        myHolder.tvUserName.setText(keyValue.name);
        // DEBUGGER
        // myHolder.tvMessage.setText("" + keyValue.friendRequestStatus);

        myHolder.tvMessage.setText("No message sent");
        Log.d(TAG, "[ OK ] ----- onBindViewHolder [KEY] " + keyValue.key + " " + keyValue.value  + " [VALUE]");

        // HANDLE ACCEPT & DELETE
        myHolder.tvAccept.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(context.getApplicationContext(), "You accepted friend request " +
                        "" + position, Toast.LENGTH_SHORT).show();
//                DisplayActivity.lhmReceivedFriendRequests.remove(keyValue.key);
                myHolder.tvMessage.setVisibility(View.GONE);
                myHolder.tvUserName.setVisibility(View.GONE);
                myHolder.ivUserImage.setVisibility(View.GONE);
                myHolder.tvAccept.setVisibility(View.GONE);
                myHolder.tvDelete.setVisibility(View.GONE);

                myHolder.tvClickMessage.setText("Accepted");


                // CHECK with real data (firebase)
                acceptFriendRequest(keyValue.key, keyValue.name);

                // CHECK with false data (Cache)
                // keyValue.value = "1";
//                 KeyValue tempKeyValue = new KeyValue();
//                 tempKeyValue.key = keyValue.key;
//                 tempKeyValue.value = keyValue.value;
//                 tempKeyValue.friendRequestStatus = true;
//                 DisplayActivity.lhmReceivedFriendRequests.put(keyValue.key, tempKeyValue);
                return false;
            }
        });

        myHolder.tvDelete.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(context.getApplicationContext(), "You deleted friend request " +
                        "" + position, Toast.LENGTH_SHORT).show();
//                DisplayActivity.lhmReceivedFriendRequests.remove(keyValue.key);
                myHolder.tvMessage.setVisibility(View.GONE);
                myHolder.tvUserName.setVisibility(View.GONE);
                myHolder.ivUserImage.setVisibility(View.GONE);
                myHolder.tvAccept.setVisibility(View.GONE);
                myHolder.tvDelete.setVisibility(View.GONE);

                myHolder.tvClickMessage.setText("Deleted");

                Log.d(TAG, "onTouch: KEY---- " + keyValue.key);

                // CHECK with real data (firebase)
                deleteFriendRequest(keyValue.key);

                // CHECK with false data (Cache)
                // keyValue.value = "0";
//                KeyValue tempKeyValue = new KeyValue();
//                tempKeyValue.key = keyValue.key;
//                tempKeyValue.value = keyValue.value;
//                keyValue.friendRequestStatus = false;
//                DisplayActivity.lhmReceivedFriendRequests.put(keyValue.key, tempKeyValue);
                return false;
            }
        });

//        myHolder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
    }

    // OK: AUTO PUSH NOTIFICATION
    // src: https://stackoverflow.com/questions/39068722/post-ing-json-request-to-fcm-server-isnt-working
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    Call post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .addHeader("Content-Type","application/json")
                .addHeader("Authorization","key=" + DisplayActivity.SERVER_KEY)
                .url(url)
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    // OK: ACCEPT FRIEND
    private void acceptFriendRequest(String uid, String name) {
        // PUSH NOTIFICATION: friend request accepted
        notifySenderAccepted(uid);

        Toast.makeText(context.getApplicationContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();

        // ADD uid,name,value to friends
        // requested user
        databaseReference
                .child(uid)
                .child(context.getString(R.string.friends))
                .child(firebaseAuth.getCurrentUser().getUid())
                .child("key")
                .setValue(firebaseAuth.getCurrentUser().getUid());
        databaseReference
                .child(uid)
                .child(context.getString(R.string.friends))
                .child(firebaseAuth.getCurrentUser().getUid())
                .child("name")
                .setValue(loggedInUserName);
        databaseReference
                .child(uid)
                .child(context.getString(R.string.friends))
                .child(firebaseAuth.getCurrentUser().getUid())
                .child("value")
                .setValue("1");

        // loggedin user
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.friends))
                .child(uid)
                .child("key")
                .setValue(uid);
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.friends))
                .child(uid)
                .child("name")
                .setValue(name);
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.friends))
                .child(uid)
                .child("value")
                .setValue("1");

        // requested user
        // UPDATE sentFriendRequests
        databaseReference
                .child(uid)
                .child(context.getString(R.string.sentFriendRequests))
                .child(firebaseAuth.getCurrentUser().getUid())
                .child("value")
                .setValue("1");

        // loggedin user
        // REMOVE receivedFriendRequests
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.receivedFriendRequests))
                .child(uid)
                .removeValue();
    }
    private void notifySenderAccepted(String receiverUID) {
        databaseReference
                .child(receiverUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null) {
                            if (dataSnapshot.hasChild("deviceToken")) {
                                String TARGET_DEVICE_TOKEN = dataSnapshot.child("deviceToken").getValue().toString();
                                Log.d(TAG, "deviceTokenCHECK: " + TARGET_DEVICE_TOKEN);
                                // PUSH NOTIFICATION: friend request sent
                                acceptedFriendRequestPushNotification(TARGET_DEVICE_TOKEN);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
    private void acceptedFriendRequestPushNotification(String deviceToken) {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject param = new JSONObject();
            param.put("body", loggedInUserName + " accepted you friend request");
            param.put("title", "Start chat, call, meet");
            param.put("sound", "default");
            jsonObject.put("notification", param);
            jsonObject.put("to", deviceToken);
            post("https://fcm.googleapis.com/fcm/send", jsonObject.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            //Something went wrong
                            Log.d("test", "onFailure: FAILED........");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseStr = response.body().string();
                                Log.d("Response", responseStr);
                                // Do what you want to do with the response.
                            } else {
                                // Request not successful
                            }
                        }
                    }
            );
        } catch (JSONException ex) {
            Log.d("Exception", "JSON exception", ex);
        }
    }

    // OK: DELETE FRIEND
    private void deleteFriendRequest(String uid) {
        // PUSH NOTIFICATION: friend request deleted
        notifySenderDeleted(uid);

//        Toast.makeText(context.getApplicationContext(), "Unfriend Successful", Toast.LENGTH_SHORT).show();

        // REMOVE from
        // requested user: sentFriendRequests
        databaseReference
                .child(uid)
                .child(context.getString(R.string.sentFriendRequests))
                .child(firebaseAuth.getCurrentUser().getUid())
                .removeValue();

        // loggedin user: receivedFriendRequests
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.receivedFriendRequests))
                .child(uid)
                .removeValue();
    }
    private void notifySenderDeleted(String receiverUID) {
        databaseReference
                .child(receiverUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null) {
                            if (dataSnapshot.hasChild("deviceToken")) {
                                String TARGET_DEVICE_TOKEN = dataSnapshot.child("deviceToken").getValue().toString();
                                Log.d(TAG, "deviceTokenCHECK: " + TARGET_DEVICE_TOKEN);
                                // PUSH NOTIFICATION: friend request sent
                                deletedFriendRequestPushNotification(TARGET_DEVICE_TOKEN);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
    private void deletedFriendRequestPushNotification(String deviceToken) {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject param = new JSONObject();
            param.put("body", "Deleted");
            param.put("title", loggedInUserName + " deleted your friend request");
            param.put("sound", "default");
            jsonObject.put("notification", param);
            jsonObject.put("to", deviceToken);
            post("https://fcm.googleapis.com/fcm/send", jsonObject.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            //Something went wrong
                            Log.d("test", "onFailure: FAILED........");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseStr = response.body().string();
                                Log.d("Response", responseStr);
                                // Do what you want to do with the response.
                            } else {
                                // Request not successful
                            }
                        }
                    }
            );
        } catch (JSONException ex) {
            Log.d("Exception", "JSON exception", ex);
        }
    }

    // return total item from List
    @Override
    public int getItemCount() {
        // CHECK with List<T>
        // return receivedFriendRequestsList.size();

        // CHECK with LinkedHashMap
        return DisplayActivity.lhmReceivedFriendRequests.size();
    }

    // SUPPORT: https://stackoverflow.com/questions/32771302/recyclerview-items-duplicate-and-constantly-changing
    // CLOSE repetition at recycler view
    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public ImageView ivUserImage;
        public TextView tvUserName;
        public TextView tvMessage;
        public TextView tvAccept;
        public TextView tvDelete;


        public LinearLayout rowItem;
        public TextView tvClickMessage;

        // CREATE constructor to get widget reference
        public MyHolder(final View itemView) {
            super(itemView);

            ivUserImage = itemView.findViewById(R.id.ivUserImage);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvMessage = itemView.findViewById(R.id.tvMessage);

            tvAccept = itemView.findViewById(R.id.tvAccept);
            tvDelete = itemView.findViewById(R.id.tvDelete);

            rowItem = itemView.findViewById(R.id.firstRow);
            tvClickMessage = itemView.findViewById(R.id.tvClickMessage);

            itemView.setOnClickListener(this);
        }

        // CLICK event for all items
        @Override
        public void onClick(View v) {
            // TEST
            // int position = getAdapterPosition();
            // KeyValue keyValue = (new ArrayList<KeyValue>(DisplayActivity.linkedHashMap.values())).get(position);

            Toast.makeText(context, "Coming Soon...Requested User Info", Toast.LENGTH_SHORT).show();
        }
    }
}