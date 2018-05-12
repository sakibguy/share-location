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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import playlagom.sharelocation.DisplayActivity;
import playlagom.sharelocation.R;
import playlagom.sharelocation.fragments.ReceivedFriendRequestsFragment;
import playlagom.sharelocation.models.KeyValue;

public class FriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "FriendsAdapter";

    private Context context;
    private LayoutInflater inflater;
    DatabaseReference databaseReference;
    FirebaseAuth firebaseAuth;

    public FriendsAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);

        databaseReference = FirebaseDatabase.getInstance().getReference(context.getString(R.string.sharelocation));
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // INFLATE the layout when ViewHolder created
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_friends, parent, false);
        MyHolder holder = new MyHolder(view);
        return holder;
    }

    // BIND Friend List
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

        // GET product position of item in RecyclerView to bind receivedFriendRequestsList and assign values from list
        final MyHolder myHolder = (MyHolder) holder;
        // Get by position
        final KeyValue keyValue = (new ArrayList<KeyValue>(
                DisplayActivity.lhmFriends.values())).get(position);
        myHolder.tvUserName.setText(keyValue.name);

        // HANDLE Delete
        myHolder.tvDelete.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                myHolder.tvUserName.setVisibility(View.GONE);
                myHolder.ivUserImage.setVisibility(View.GONE);
                myHolder.tvDelete.setVisibility(View.GONE);
                myHolder.tvClickMessage.setText("Deleted");

                // CHECK with real data (firebase)
                deleteFriend(keyValue.key);

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
    }

    private void acceptFriendRequest(String uid, String name) {
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
                .setValue(DisplayActivity.loggedInUserName);
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

    // OK: deleteFriend
    private void deleteFriend(String uid) {
        Toast.makeText(context.getApplicationContext(), "Unfriend Successful", Toast.LENGTH_SHORT).show();

        // REMOVE from requested user
        // friends
        databaseReference
                .child(uid)
                .child(context.getString(R.string.friends))
                .child(firebaseAuth.getCurrentUser().getUid())
                .removeValue();
        // sentFriendRequests
        databaseReference
                .child(uid)
                .child(context.getString(R.string.sentFriendRequests))
                .child(firebaseAuth.getCurrentUser().getUid())
                .removeValue();

        // REMOVE from loggedin user
        // friends
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.friends))
                .child(uid)
                .removeValue();
        // sentFriendRequests
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(context.getString(R.string.sentFriendRequests))
                .child(uid)
                .removeValue();
    }

    // return total item from List
    @Override
    public int getItemCount() {
        // CHECK with List<T>
        // return receivedFriendRequestsList.size();

        // CHECK with LinkedHashMap
        return DisplayActivity.lhmFriends.size();
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
        public TextView tvDelete;


        public LinearLayout rowItem;
        public TextView tvClickMessage;

        // CREATE constructor to get widget reference
        public MyHolder(final View itemView) {
            super(itemView);

            ivUserImage = itemView.findViewById(R.id.ivUserImage);
            tvUserName = itemView.findViewById(R.id.tvUserName);

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