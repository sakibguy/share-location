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

import java.util.LinkedHashMap;

import playlagom.sharelocation.DisplayActivity;
import playlagom.sharelocation.R;
import playlagom.sharelocation.adapters.ReceivedFriendRequestsAdapter;
import playlagom.sharelocation.models.KeyValue;

/**
 * Created by User on 5/11/2018.
 */

public class ReceivedFriendRequestsFragment extends Fragment {

    RecyclerView rvReceivedFriendRequests;
    ReceivedFriendRequestsAdapter receivedFriendRequestsAdapter;

    public ReceivedFriendRequestsFragment() {}

    View view;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.received_friend_requests_fragment, container, false);

        // Bind fragment with recycler view
        rvReceivedFriendRequests = view.findViewById(R.id.rvReceivedFriendRequests);
        TextView tvTotalReceivedRequests = view.findViewById(R.id.tvTotalReceivedRequests);
        tvTotalReceivedRequests.setText("Received Friend Requests (" + DisplayActivity.lhmReceivedFriendRequests.size() + ")");
        receivedFriendRequestsAdapter = new ReceivedFriendRequestsAdapter(getActivity());
        rvReceivedFriendRequests.setAdapter(receivedFriendRequestsAdapter);
        rvReceivedFriendRequests.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
