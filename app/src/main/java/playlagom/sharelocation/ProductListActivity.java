package playlagom.sharelocation;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import playlagom.sharelocation.models.KeyValue;

public class ProductListActivity extends Activity {

    private static final String TAG = "ProductListActivity";
    private RecyclerView rvProduct;
    private ProductAdapter productAdpater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        for (int i = 0; i < DisplayActivity.linkedHashMap.size(); i++) {
            KeyValue value = (new ArrayList<KeyValue>(DisplayActivity.linkedHashMap.values())).get(i);

            Log.d(TAG, "[ OK ] ----- onCreate: [KEY] " + value.key + " " + value.value  + " [VALUE] " +
                    ", cachestatus = " +value.friendRequestStatus );
        }

//        for (int i = 0; i < DisplayActivity.receivedFriendRequestsList.size(); i++) {
//            String key = DisplayActivity.receivedFriendRequestsList.get(i).key;
//            String value = DisplayActivity.receivedFriendRequestsList.get(i).value;

//            Log.d(TAG, "[ OK ] ----- onCreate: [KEY] " + key + " " + value + " [VALUE]");
//        }

        // SETUP and Handover product to recyclerview
        rvProduct = findViewById(R.id.rvProductList);
//        productAdpater = new ProductAdapter(ProductListActivity.this, DisplayActivity.receivedFriendRequestsList);

        productAdpater = new ProductAdapter(ProductListActivity.this, DisplayActivity.linkedHashMap);
        rvProduct.setAdapter(productAdpater);
        rvProduct.setLayoutManager(new LinearLayoutManager(ProductListActivity.this));
    }
}