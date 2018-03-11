package playlagom.sharelocation.libs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import playlagom.sharelocation.R;

/**
 * Created by User on 12/19/2017.
 */

public class Init {

    private static final int USER_IMAGE_WIDTH = 100;
    private static final int USER_IMAGE_HEIGHT = 100;
    private static MarkerOptions marker;

    private static void resizeUserImageForMap(Context context) {
        Bitmap userImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.current_user);
        userImage = Bitmap.createScaledBitmap(userImage, USER_IMAGE_WIDTH, USER_IMAGE_HEIGHT, false);
        userImage = Converter.getCroppedBitmap(userImage);

        marker = new MarkerOptions();
        marker.icon(BitmapDescriptorFactory.fromBitmap(userImage));
    }

    public static void runMap(Context context, GoogleMap mMap, LatLng latLng, String title) {
        resizeUserImageForMap(context);

        marker.title(title).position(latLng);
        mMap.addMarker(marker);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.2f));
    }
}
