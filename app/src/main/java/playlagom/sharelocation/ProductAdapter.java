package playlagom.sharelocation;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import playlagom.sharelocation.models.KeyValue;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ProductAdapter";

    private Context context;
    private LayoutInflater inflater;
    List<KeyValue> receivedFriendRequestsList = Collections.emptyList();
    DatabaseReference databaseReference;
    FirebaseAuth firebaseAuth;
    LinkedHashMap<String, KeyValue> linkedHashMap;

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

    public ProductAdapter(Context context, LinkedHashMap<String, KeyValue> linkedHashMap) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.linkedHashMap = linkedHashMap;

        databaseReference = FirebaseDatabase
                .getInstance()
                .getReference(context.getString(R.string.sharelocation));
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // INFLATE the layout when ViewHolder created
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.product_list_item, parent, false);
        MyHolder holder = new MyHolder(view);
        return holder;
    }

    // BIND receivedFriendRequestsList
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

        // GET product position of item in RecyclerView to bind receivedFriendRequestsList and assign values from list
        final MyHolder myHolder = (MyHolder) holder;
        /* Get by position */
        final KeyValue keyValue = (new ArrayList<KeyValue>(DisplayActivity.linkedHashMap.values())).get(position);

        Log.d(TAG, "[ OK ] ----- onBindViewHolder [KEY] " + keyValue.key + " " + keyValue.value  + " [VALUE]");

        // CHECK with false/real data (cache/firebase)
        if (keyValue.value.equals("1")) {
            myHolder.switchFriend.setChecked(true);
        } else if(keyValue.value.equals("0")){
            myHolder.switchFriend.setChecked(false);
        }

        // CHECK with false data (Cache)
//        if (keyValue.friendRequestStatus) {
//            myHolder.switchFriend.setChecked(true);
//        } else if(!keyValue.friendRequestStatus){
//            myHolder.switchFriend.setChecked(false);
//        }

        // RETRIEVE username & message through this key and SHOW at recycler view
        databaseReference
                .child(keyValue.key)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Log.d(TAG, "[ OK ] - onDataChange: " + dataSnapshot.toString());
                            myHolder.tvUserName.setText(dataSnapshot.child("name").getValue().toString());
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

        // HANDLE switch
        myHolder.switchFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean status = ((Switch) v).isChecked();
                if (status) {
                    // CHECK with real data (firebase)
                    acceptFriendRequest(keyValue.key);

                    // CHECK with false data (Cache)
                    // keyValue.value = "1";
                    // keyValue.friendRequestStatus = true;
                    // DisplayActivity.linkedHashMap.put(keyValue.key, keyValue);
                } else {
                    // CHECK with real data (firebase)
                    deleteFriend(keyValue.key);

                    // CHECK with false data (Cache)
                    // keyValue.value = "0";
                    // keyValue.friendRequestStatus = false;
                    // DisplayActivity.linkedHashMap.put(keyValue.key, keyValue);
                }
            }
        });
    }

    private void acceptFriendRequest(String uid) {
        Toast.makeText(context.getApplicationContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();

        // ADD uid to friends: at requested user
        databaseReference
                .child(uid)
                .child(context.getString(R.string.friends))
                .child(firebaseAuth.getCurrentUser().getUid())
                .setValue("1");
        // ADD uid to friends: at logged in user
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.friends))
                .child(uid)
                .setValue("1");

        // UPDATE acceptance
        // requested user: sentFriendRequests
        databaseReference
                .child(uid)
                .child(context.getString(R.string.sentFriendRequests))
                .child(firebaseAuth.getCurrentUser().getUid())
                .setValue("1");

        // logged in user: receivedFriendRequests
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.receivedFriendRequests))
                .child(uid)
                .setValue("1");
    }

    private void deleteFriend(String uid) {
        Toast.makeText(context.getApplicationContext(), "Unfriend Successful", Toast.LENGTH_SHORT).show();

        // REMOVE uid from friends: at requested user
        databaseReference
                .child(uid)
                .child(context.getString(R.string.friends))
                .child(firebaseAuth.getCurrentUser().getUid())
                .removeValue();

        // REMOVE uid from friends: at logged in user
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.friends))
                .child(uid)
                .removeValue();

        // REMOVE from
        // requested user: sentFriendRequests
        databaseReference
                .child(uid)
                .child(context.getString(R.string.sentFriendRequests))
                .child(firebaseAuth.getCurrentUser().getUid())
                .removeValue();

        // logged in user: receivedFriendRequests
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.receivedFriendRequests))
                .child(uid)
                .removeValue();
    }

    // return total item from List
    @Override
    public int getItemCount() {
        // CHECK with List<T>
        // return receivedFriendRequestsList.size();

        // CHECK with LinkedHashMap
        return linkedHashMap.size();
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

        public TextView tvUserName;
        public TextView tvMessage;
        public Switch switchFriend;

        // CREATE constructor to get widget reference
        public MyHolder(final View itemView) {
            super(itemView);

            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            switchFriend = itemView.findViewById(R.id.switchFriend);

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