package playlagom.sharelocation;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import playlagom.sharelocation.adapters.ViewPagerAdapter;
import playlagom.sharelocation.fragments.FriendsFragment;
import playlagom.sharelocation.fragments.ReceivedFriendRequestsFragment;
import playlagom.sharelocation.fragments.SentFriendRequestsFragment;

public class TabNPageViewerActivity extends AppCompatActivity {

    // google SUPPORT: https://www.youtube.com/watch?v=zQekzaAgIlQ

    // SUPPORT: https://www.youtube.com/watch?v=oBhgyiBVd3k
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_npage_viewer);

        tabLayout = findViewById(R.id.tabLayoutId);
        viewPager = findViewById(R.id.viewPagerId);

        // ADD fragment
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
//        adapter.addFragment(new ReceivedFriendRequestsFragment(), "Received");
        adapter.addFragment(new ReceivedFriendRequestsFragment(), "");
//        adapter.addFragment(new FriendsFragment(), "Friends");
        adapter.addFragment(new FriendsFragment(), "");
//        adapter.addFragment(new SentFriendRequestsFragment(), "Sent");
        adapter.addFragment(new SentFriendRequestsFragment(), "");

        adapter.notifyDataSetChanged();

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_fiber_new_black_24dp);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_people_black_24dp);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_send_black_24dp);
    }
}
