package playlagom.sharelocation.libs;

import android.graphics.Color;
import android.location.Location;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 4/20/2018.
 */

public class GoogleMapOperations {
    // TODO: 4/20/2018 STRAT IMPLEMENTING from here when DisplayActivity.java's line of code exceeds >= 1k

    // SET Margin dynamically  for any view (generic way)
    // SUPPORT: https://stackoverflow.com/questions/4472429/change-the-right-margin-of-a-view-programmatically
    public static void setMargins (View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }


    // SET animated marker at danger
    // SUPPORT: https://www.youtube.com/watch?v=hS7EFdDLjas
    // SUPPORT: https://github.com/ManiAndroid2017/Android/blob/master/MapBoundaryView/app/src/main/java/com/mapboundaryview/MapsActivity.java

    static LatLng globalLocation = new LatLng(0, 0);
    // Add a marker in Sydney and move the camera
    static CameraPosition INIT =
            new CameraPosition.Builder()
                    .target(globalLocation)
                    .zoom(21f)
                    .build();
    // SUPPORT: https://stackoverflow.com/questions/22202299/how-do-i-remove-all-radius-circles-from-google-map-android-but-keep-pegs
    public static List<Circle> circleList = new ArrayList<>();
    public static void addingCircleView(GoogleMap mMap, LatLng location) {
        globalLocation = location;
        Circle myCircle1;
        Circle myCircle2;
        Circle myCircle3;
        Circle myCircle4;
        Circle myCircle5;
        Circle myCircle6;

        CircleOptions circleOptions = new CircleOptions()
                .center(location)   // SET center
                .radius(10)         // SET radius in meter
                .fillColor(Color.parseColor("#1AFB2323"))    // Default
                .strokeColor(Color.parseColor("#1AFB2323"))
                .strokeWidth(1);
        myCircle1 = mMap.addCircle(circleOptions);
        circleList.add(myCircle1);

        circleOptions = new CircleOptions()
                .center(location)   //set center
                .radius(8)   //set radius in meters
                .fillColor(Color.parseColor("#40FB2323"))  //default
                .strokeColor(Color.parseColor("#40FB2323"))
                .strokeWidth(1);
        myCircle2 = mMap.addCircle(circleOptions);
        circleList.add(myCircle2);

        circleOptions = new CircleOptions()
                .center(location)   //set center
                .radius(6)   //set radius in meters
                .fillColor(Color.parseColor("#66FB2323"))  //default
                .strokeColor(Color.parseColor("#66FB2323"))
                .strokeWidth(1);
        myCircle3 = mMap.addCircle(circleOptions);
        circleList.add(myCircle3);

        circleOptions = new CircleOptions()
                .center(location)   //set center
                .radius(4)   //set radius in meters
                .fillColor(Color.parseColor("#8CFB2323"))  //default
                .strokeColor(Color.parseColor("#8CFB2323"))
                .strokeWidth(1);
        myCircle4 = mMap.addCircle(circleOptions);
        circleList.add(myCircle4);

        circleOptions = new CircleOptions()
                .center(location)   //set center
                .radius(2)   //set radius in meters
                .fillColor(Color.parseColor("#A6FB2323"))  //default
                .strokeColor(Color.parseColor("#A6FB2323"))
                .strokeWidth(1);
        myCircle5 = mMap.addCircle(circleOptions);
        circleList.add(myCircle5);

        circleOptions = new CircleOptions()
                .center(location)   //set center
                .radius(1)   //set radius in meters
                .fillColor(Color.parseColor("#CCFB2323"))  //default
                .strokeColor(Color.parseColor("#CCFB2323"))
                .strokeWidth(1);
        myCircle6 = mMap.addCircle(circleOptions);
        circleList.add(myCircle6);

        // use map to move camera into position
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(INIT));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12.0f));
    }
}
