package playlagom.sharelocation.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import playlagom.sharelocation.DisplayActivity;
import playlagom.sharelocation.R;
import playlagom.sharelocation.adapters.FriendsAdapter;
import playlagom.sharelocation.adapters.ReceivedFriendRequestsAdapter;

/**
 * Created by User on 5/11/2018.
 */

public class FriendsFragment extends Fragment {

    RecyclerView rvFriends;
    FriendsAdapter friendsAdapter;

    public FriendsFragment() {

    }

    View view;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.friends_fragment, container, false);

        // Bind fragment with recycler view
        rvFriends = view.findViewById(R.id.rvFriends);
        TextView tvTotalFriends = view.findViewById(R.id.tvTotalFriends);
        tvTotalFriends.setText("Total Friends (" + DisplayActivity.lhmFriends.size() + ")");
        friendsAdapter = new FriendsAdapter(getActivity());
        rvFriends.setAdapter(friendsAdapter);
        rvFriends.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
