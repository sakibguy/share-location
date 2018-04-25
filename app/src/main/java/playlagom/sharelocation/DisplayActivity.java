package playlagom.sharelocation;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import playlagom.sharelocation.auth.LoginActivity;
import playlagom.sharelocation.libs.Converter;
import playlagom.sharelocation.libs.GoogleMapOperations;
import playlagom.sharelocation.models.User;

public class DisplayActivity extends FragmentActivity implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnInfoWindowClickListener,
        OnMapReadyCallback, GoogleMap.OnCameraMoveListener {

    private static final String TAG = DisplayActivity.class.getSimpleName();
    private static final String LOG_TAG = "DisplayActivity";
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    private HashMap<String, Marker> blueMarkers = new HashMap<>();
    GoogleMap mMap;

    private static final int PERMISSIONS_REQUEST = 1;
    static boolean taskCompleted = false;
    private boolean insideShouldShow = false;

    ImageView ivUserImage, ivMyCircle, ivDanger, ivPicture;
    TextView tvPosition, tvPoint;
    boolean locationPermissionGranted = false;

    // Firebase attributes
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;
    DatabaseReference locationRef;
    DatabaseReference userRef;
    DatabaseReference allMarkerRef;

    AdView mAdView;
    View mapView;

    // Danger sound
    MediaPlayer dangerSound;
    int width, height;
    private static boolean makeDangerSound = true;
    private int userCounter = 1;
    private int counter = 1;
    private User tempUser = new User();
    private String markerTitle = null;
    private LatLng location = new LatLng(0, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapView = mapFragment.getView();

        // wire xml imageview components with java object
        // icons: live friends + danger
        ivMyCircle = findViewById(R.id.ivMyCircle);
        ivDanger = findViewById(R.id.ivDanger);
        ivPicture = findViewById(R.id.ivPicture);
        ivUserImage = findViewById(R.id.ivUserImage);

        // WIRE textview widgets
        tvPosition = findViewById(R.id.tvPosition);
        tvPosition.setVisibility(View.INVISIBLE);
        tvPoint = findViewById(R.id.tvPoint);
        tvPoint.setVisibility(View.INVISIBLE);

        // INIT danger sound
        // SUPPORT: https://stackoverflow.com/questions/18459122/play-sound-on-button-click-android
        dangerSound = MediaPlayer.create(this, R.raw.siren_alert_1);

        // MAKE round user image
        ivUserImage.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.current_user)));

        ImageView ivLogout = findViewById(R.id.ivLogout);
        ivLogout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getApplicationContext(), "Successfully Logout", Toast.LENGTH_LONG).show();
                Log.d(TAG, "onTouch: DEBUGGER-----Logout icon");
                startActivity(new Intent(DisplayActivity.this, LoginActivity.class));
                finish();
                return false;
            }
        });

        // CHECK GPS is status
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable GPS location services", Toast.LENGTH_LONG).show();
            finish();
        }

        // INIT AdMob app ID
        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");

        // ADS: https://developers.google.com/admob/android/banner
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // INIT: firebase dependency
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.sharelocation_users));
        allMarkerRef = FirebaseDatabase.getInstance().getReference(getString(R.string.sharelocation_users));

        // SET: danger icon, set value = 0 or read danger status and select icon
        showDangerIcon();

        // SET: blue markers for all registered users by default.
        showAllRegisteredUsers();

        // COPY v1.11.0 pointed db data to new structure db
        copyLoggedInUserInfoToNewStructure();

//        // SUPPORT: https://www.101apps.co.za/index.php/item/182-firebase-realtime-database-tutorial.html
//        // 1: https://github.com/firebase/FirebaseUI-Android/issues/1040
//        // 2: https://stackoverflow.com/questions/26700924/query-based-on-multiple-where-clauses-in-firebase
//        // todo 3: https://github.com/firebase/geofire-java
//
//        // look for the matching item
//        locationRef.orderByChild("locations").equalTo(uniqueID)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshotInner) {
//                        try{
////                                        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshotInner.getValue();
//
//                            // SUPPORT handle idea raised by myself first then suggested
//                            // from: https://www.youtube.com/watch?v=Idu9EJPSxiY
//                            // AS unique id is just the last value of the dataSnapshot.getChildren() then
//                            // store keys + value on a hasmap/arraylist to handle/loop through later.
//                            Log.d(TAG, counter ++ +" DEBUGGER: --- id: " + tempSnapShot.getKey());
//                        } catch (Exception e){
//                            Log.d(TAG, "NULL pointer exception: DEBUGGER: ---");
//                        }
//
////                                    if (tempSnapShot != null) {
////                                        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshotInner.getValue();
//
////                                        double lat = Double.parseDouble(value.get("latitude").toString());
////                                        double lng = Double.parseDouble(value.get("longitude").toString());
////                                        location = new LatLng(lat, lng);
//
////                                        // SHOW all users by default through black marker
////
////                                        // create a new marker at the device's location
////                                        // move the marker for a device if it exists already.
////
////                                        if (dataSnapshotInner != null) {
////                                            Log.d(TAG, "--- --- DEBUGGER: --- " + tempUser.getEmail());
////                                            if (!mMarkers.containsKey(dataSnapshotInner.getKey())) {
////                                                if (tempSnapShot.getChildrenCount() == 2) {
////                                                    markerTitle = tempUser.getEmail();
////                                                } else if (tempSnapShot.getChildrenCount() == 3) {
////                                                    markerTitle = tempUser.getName();
////                                                } else {
////                                                    markerTitle = tempUser.getName();
////                                                }
////                                                marker = mMap.addMarker(new MarkerOptions().title("" + markerTitle + "")
////                                                        .position(location)
////                                                        .snippet("cell, msg, fnd req")
////                                                        .icon(BitmapDescriptorFactory.defaultMarker(
////                                                                BitmapDescriptorFactory.HUE_BLUE       // SET live users marker green
////                                                        )));
////                                                mMarkers.put(uid, marker);
////                                                marker.showInfoWindow();
////
////                                                tempSnapShot = null;
////                                                tempUser = null;
////                                            }
////                                        }
////                                    }
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//
//                    }
//                });

        checkLocationPermission();
        Log.d(TAG, "[ OK ] ---- onCreate: ----");
    }

    // COPY v1.11.0 pointed db data to new structure db
    private void copyLoggedInUserInfoToNewStructure() {
        Log.d(TAG, "[ OK ] -------- copyLoggedInUserInfoToNewStructure: ");
        DatabaseReference copyFromRef = FirebaseDatabase.getInstance().getReference("users");
        copyFromRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "[ OK ] -------- copyLoggedInUserInfoToNewStructure(): LOGGED IN USER INFO: " + dataSnapshot.toString());
                    User copyUser;
                    try{
                        copyUser = dataSnapshot.getValue(User.class);
                        Log.d(TAG, "[ OK ] -------- copyLoggedInUserInfoToNewStructure(): " +
                                "name: " + copyUser.getName() + ", " +
                                "email: " + copyUser.getEmail() + ", " +
                                "pass: " + copyUser.getPassword() + ", " +
                                "danger: " + copyUser.getDanger());
                        if (dataSnapshot.getChildrenCount() == 2) {
                            databaseReference.child(dataSnapshot.getKey()).setValue(copyUser);
                            databaseReference.child(dataSnapshot.getKey()).child("danger").setValue("0");
                        } else if (dataSnapshot.getChildrenCount() == 3) {
                            databaseReference.child(dataSnapshot.getKey()).setValue(copyUser);
                            databaseReference.child(dataSnapshot.getKey()).child("danger").setValue("0");
                        } else if (dataSnapshot.getChildrenCount() == 4) {
                            databaseReference.child(dataSnapshot.getKey()).setValue(copyUser);
                        }
                    } catch (Exception e){
                        Log.e(TAG, "[ ERROR ] -------- copyLoggedInUserInfoToNewStructure: " + e.getMessage());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
    }
    private void showDangerIcon() {
        databaseReference.child(firebaseAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getChildrenCount() == 3 || dataSnapshot.getChildrenCount() == 4) {
                            // SET danger = 0
                            databaseReference.child(firebaseAuth.getCurrentUser().getUid())
                                    .child("danger").setValue("0");
                            // SET default danger icon
                            ivDanger.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.danger_icon)));
                            dangerStatus = false;
                        } else if (dataSnapshot.getChildrenCount() == 5) {
                            User dangerUser = dataSnapshot.getValue(User.class);
                            if (dangerUser.getDanger().equals("0")){
                                // SET default danger icon
                                ivDanger.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.danger_icon)));
                                dangerStatus = false;
                            }
                            if (dangerUser.getDanger().equals("1")){
                                // SET run danger icon
                                ivDanger.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.danger_icon_run)));
                                Toast.makeText(getApplicationContext(), "Your status in DANGER",
                                        Toast.LENGTH_LONG).show();
                                dangerStatus = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
    Marker tempBlueMarker = null;
    private void showAllRegisteredUsers() {
        // SHOW all users black/blue marker icon
        allMarkerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "[ OK ] ---- TOTAL REGISTERED USER: " +
                        "" + dataSnapshot.getChildrenCount() +
                        ", showAllRegisteredUsers().onDataChange: " +
                        "" + dataSnapshot.toString());
                User temporaryUser;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    temporaryUser = snapshot.getValue(User.class);
                    String uniqueID = snapshot.getKey();

                    try {
                        if (temporaryUser != null) {
                            double lat  = Double.parseDouble(temporaryUser.getPosition().getLatitude());
                            double lang = Double.parseDouble(temporaryUser.getPosition().getLongitude());
                            location = new LatLng(lat, lang);
                            Log.d(TAG, "[ OK ] ---- showAllRegisteredUsers() .. " + userCounter ++ +
                                    " uid: " + uniqueID + ", " + temporaryUser.getEmail() + ", " + location);

                            if (!blueMarkers.containsKey(uniqueID)) {
                                tempBlueMarker = mMap.addMarker(new MarkerOptions().title("" + temporaryUser.getName() + "")
                                        .position(location)
                                        .snippet("cell, msg, fnd req")
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_BLUE       // SET live users marker green
                                        )));
                                blueMarkers.put(uniqueID, tempBlueMarker);
                                tempBlueMarker.showInfoWindow();
                            } else {
                                blueMarkers.get(uniqueID).setPosition(location);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[ ERROR ] ---- showAllRegisteredUsers().onDataChange: " + e.getMessage());
                    }
                }
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker tempMarker : blueMarkers.values()) {
                    builder.include(tempMarker.getPosition());
                }
                final LatLngBounds bounds = builder.build();
                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
//                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 300));
                    }
                });
//                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void checkLocationPermission() {
        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        int permission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            startTrackerService();
            subscribeToUpdates();
            // As location permission granted then set NAME input for <= v1.6.0 users through checking with the db.
            // SET pop up window to take name for <= v1.6.0 version users not for >= 1.7.0 users. Upto v1.6.0 there was no NAME field at sign up form. To give better UX, at v1.7.0 here added NAME field at sign up form.

            // CODE is ready to CHANGE
            // CHECK isNameProvided. Basically popup will not show 1st time where will show during running app 2nd time.
            isNameProvided(firebaseAuth.getCurrentUser().getUid());
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST);
                insideShouldShow = true;
            } else {
                // when user check "Don't show me again then this part"
                insideShouldShow = false;
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST);
            }
        }
    }
    private void startTrackerService() {
        startService(new Intent(this, TrackerService.class));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Start the service when the permission is granted
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);
            startTrackerService();
            subscribeToUpdates();
            // when ALLOWED then no problem, but when DENY then there two cases for deny 1. Don't ask again 2. Just Deny
            // We will detect that using below code
        } else {
            // Now be sure which one user selected!
            // Don't ask again | DENY
            // simple logic below
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Build the alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Location Services Not Active");
                builder.setMessage("Please enable Location permission for this app to go ahead");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {

                        taskCompleted = true;
                        // this code for launch app permission setting page
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName()));
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
                Dialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            } else {
                // Don't show me again selected for first time
                if (insideShouldShow) {
                    finish();
                } else {
                    String msg = "This dialog as 'Don't ask again' was selected at previous time";
                    // Build the alert dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Location Services Not Active");
                    builder.setMessage(msg + "\n\nPlease enable Location permission for this app to go ahead");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {

                            // this code for launch app permission setting page
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + getPackageName()));
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            taskCompleted = true;
                        }
                    });
                    Dialog alertDialog = builder.create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();
                }
            }
        }
    }

    LatLng latLng;
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        // TODO: Before enabling the My Location layer, you must request
        // location permission from the user. This sample does not include
        // a request for location permission.

        // Change my location button position at the bottom
        // SUPPORT: https://stackoverflow.com/questions/36785542/how-to-change-the-position-of-my-location-button-in-google-maps-using-android-st/39179202
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 30);
        }

        // SET blue dot
        if (locationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);
            subscribeToUpdates();
        }

        // SET a listener for info window events.
        // SUPPORT: https://developers.google.com/maps/documentation/android-api/infowindows
        mMap.setOnInfoWindowClickListener(this);
        // ADD a camera idle listener.
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                latLng = mMap.getCameraPosition().target;

                // RENDER: You cannot use the width/height/getMeasuredWidth/getMeasuredHeight on a View before the system renders it (typically from onCreate/onResume).
                // SUPPORT 1: https://stackoverflow.com/questions/42257090/android-google-maps-api-calculate-width-and-height-in-pixel-of-the-full-map
                // SUPPORT 2: https://stackoverflow.com/questions/6939002/if-i-call-getmeasuredwidth-or-getwidth-for-layout-in-onresume-they-return-0
                mapView.post(new Runnable() {
                    @Override
                    public void run() {
                        width = mapView.getMeasuredWidth();
                        height = mapView.getMeasuredHeight();
                    }
                });

                GoogleMapOperations.setMargins(tvPoint, width/2, height/2, 0, 0);
                // DEBUGGER: tvPosition.setText(" " + height + ", " + width);
                // tvPosition.setText("" + latLng.latitude + ", " + latLng.longitude);

                lat = latLng.latitude;
                lang = latLng.longitude;

                if (pictureStatus) {
                    startActivity(new Intent(DisplayActivity.this, StreetViewPanoramaBasicDemoActivity.class));
                }
            }
        });

        // SUPPORT: https://www.youtube.com/watch?v=hS7EFdDLjas
        // zoom control: plus | minus button by default android sdk
//        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "My Location", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    // SUPPORT: https://codelabs.developers.google.com/codelabs/realtime-asset-tracking/index.html?index=..%2F..%2Findex#5
    // We've also got two empty methods - subscribeToUpdates() and setMarker()
    // which are responsible for
    
    // subscribing to updates in Firebase and
    // setting the marker on the map

    // when an update occurs.
    static boolean showAllLiveUser = true;
    // The subscribeToUpdates() method calls setMarker()
    // whenever it receives a new or updated location for a tracked device.
    private void subscribeToUpdates() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.sharelocation_users));

        // logcat color SUPPORT: https://medium.com/@gun0912/android-studio-how-to-change-logcat-color-3c17a10beef8
        Log.d(TAG, "[ OK ] -- subscribeToUpdates:");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "[ OK ] -- subscribeToUpdates.onChildAdded:");
                setActiveMarker(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "[ OK ] -- subscribeToUpdates.onChildChanged:");
                setActiveMarker(dataSnapshot);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "[ OK ] -- subscribeToUpdates.onChildMoved:");
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "[ OK ] -- subscribeToUpdates.onChildRemoved:");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "[ ERROR ] -- Failed to read value. subscribeToUpdates():", error.toException());
            }
        });
    }

    // SUPPORT: https://codelabs.developers.google.com/codelabs/realtime-asset-tracking/index.html?index=..%2F..%2Findex#5
    DataSnapshot dataSnapshotGlobal;
    String uid;
    int i = 1;
    User greenMarkerUser = new User();
    private Marker tempGreenMarker;
    // setMarker() accepts the location data from Firebase,
    // which contains the latitude and longitude, as well as the key for the device.
    private void setActiveMarker(DataSnapshot dataSnapshot) {
        Log.d(TAG, "[ OK ] -- setActiveMarker: obj: " + dataSnapshot.toString());

        try{
            greenMarkerUser = dataSnapshot.getValue(User.class);
            Log.d(TAG, "[ OK ] -- setActiveMarker: name: " + greenMarkerUser.getName());
        } catch (Exception e){
            Log.e(TAG, "[ ERROR ] -- setActiveMarker(): " + e.getMessage());
        }
        uid = dataSnapshot.getKey();

        try {
            if (greenMarkerUser != null) {
                Log.d(TAG, "[ OK ] -- setActiveMarker: name: " + greenMarkerUser.getName());
                double lat  = Double.parseDouble(greenMarkerUser.getPosition().getLatitude());
                double lang = Double.parseDouble(greenMarkerUser.getPosition().getLongitude());
                LatLng location = new LatLng(lat, lang);
                Log.d(TAG, "[ OK ] -- setActiveMarker: latlang: " + location.toString());

                // It is notified each time one of the device's location is updated.
                // When this happens, it will either create a new marker at the device's location,
                // or move the marker for a device if it exists already.
                if (!mMarkers.containsKey(uid)) {
                    tempGreenMarker = mMap.addMarker(new MarkerOptions().title("" + greenMarkerUser.getName() + "")
                            .position(location)
                            .snippet("cell, msg, fnd req")
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_GREEN       // SET live users marker green
                            )));
                    mMarkers.put(uid, tempGreenMarker);
                    tempGreenMarker.showInfoWindow();
                } else {
                    mMarkers.get(uid).setPosition(location);
                }
            }
        }catch (Exception e) {
            Log.d(TAG, "[ ERROR ] setActiveMarker: " + e.getMessage());
        }


//        // HERE WHAT CORRESPONDS TO JOIN
//        databaseReference.child(uid)
//        .addValueEventListener(
//            new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot childDataSnapshot) {
//                    try{
//                        value = (HashMap<String, Object>) childDataSnapshot.getValue();
////                        user = childDataSnapshot.getValue(User.class);
//                        Log.d(TAG, "[ OK ] -- setActiveMarker.onDataChange: " + value.get("latitude").toString() +", " + i++ + childDataSnapshot.getValue());
//                    } catch (Exception e){
//                        Log.e(TAG, "[ ERROR ] -- setActiveMarker().onDataChange(): " + e.getMessage());
//                    }

                    /*

                    if (user != null) {
//                        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshotGlobal.getValue();
//                        Log.d(TAG, i++ + " setActiveMarker, KEY: " + uid + ", VALUE: " + value.toString() + "," +
//                                " DEBUGGER:-------- ");
//
//                        double lat = Double.parseDouble(value.get("latitude").toString());
//                        double lng = Double.parseDouble(value.get("longitude").toString());
//
                        LatLng location = new LatLng(user.position.getLatitude(), user.position.getLongitude());

                        // SHOW live all
                        if (showAllLiveUser) {
                            // SUPPORT: https://stackoverflow.com/questions/22202299/how-do-i-remove-all-radius-circles-from-google-map-android-but-keep-pegs
                            for (Circle myCircle : GoogleMapOperations.circleList) {
                                myCircle.remove();
                            }
                            GoogleMapOperations.circleList.clear();

                            Log.d(TAG, "setActiveMarker: KEY: " + i + " " + uid + "," +
                                    " DEBUGGER:-------- ");
                            // It is notified each time one of the device's location is updated.
                            // When this happens, it will either create a new marker at the device's location,
                            // or move the marker for a device if it exists already.
                            if (!mMarkers.containsKey(uid)) {
                                marker = mMap.addMarker(new MarkerOptions().title("" + user.getName() + "")
                                    .position(location)
                                    .snippet("cell, msg, fnd req")
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_GREEN       // SET live users marker green
                                    )));
                                mMarkers.put(uid, marker);
                                marker.showInfoWindow();

                                // READY to CHANGE CODE
                                // SET animated marker at danger
                                if (user.getDanger() != null) {
                                    String danger = user.getDanger();
                                    if (danger.equals("1")) {
                                        // SUPPORT: https://www.youtube.com/watch?v=hS7EFdDLjas
                                        GoogleMapOperations.addingCircleView(mMap, location);
                                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED       // SET live users marker green
                                        ));
                                    }
                                }
                            } else {
                                // READY to CHANGE CODE
                                // SET animated marker at danger
                                if (user.getDanger() != null) {
                                    String danger = user.getDanger();
                                    if (danger.equals("1")) {
                                        // SUPPORT: https://www.youtube.com/watch?v=hS7EFdDLjas
                                        GoogleMapOperations.addingCircleView(mMap, location);
                                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED       // SET live users marker green
                                        ));

                                        // START danger sound
                                        if (makeDangerSound) {
                                            makeDangerSound = false;
                                            dangerSound.start();
                                            Toast.makeText(getApplicationContext(), "NEED help!\n" + user.getName()
                                                    + " is at DANGER now", Toast.LENGTH_LONG).show();
                                        }
                                    } else if (danger.equals("0")) {
                                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_GREEN       // SET live users marker green
                                        ));
                                    }
                                }
                                mMarkers.get(uid).setPosition(location);
                            }
                        }

                        // SHOW live friends
                        if (friendsOnlyIconClicked) {
                            for (Marker CurrMarker : mMarkers.values()) {
                                // SUPPORT: https://stackoverflow.com/questions/13692398/remove-a-marker-from-a-googlemap
                                CurrMarker.remove();
                            }
                            mMarkers.clear();
                        }
                    }
                    */
//                    user = null;
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                    Log.e(TAG, "[ ERROR ] -- setActiveMarker().onCancelled():");
//                }
//            }
//        );
    }

    static boolean friendsOnlyIconClicked = false;
    public void onClickFriendsOnly(View view) {
        Log.d(TAG, "onClickLiveUsers: DEBUGGER:--------");
        // RE-DESIGN & WRITE code for live feature

        // TODO: 4/21/2018 HANDLE Showing friends only
        if (!friendsOnlyIconClicked){
            showAllLiveUser = false;
            friendsOnlyIconClicked = true;
            ivMyCircle.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_people_black_24dp));
            Toast.makeText(getApplicationContext(), "IMPLEMENT live friends functionality",
                    Toast.LENGTH_SHORT).show();
        } else {
            showAllLiveUser = true;
            friendsOnlyIconClicked = false;
            ivMyCircle.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_people_outline_black_24dp));
            Toast.makeText(getApplicationContext(), "Live all", Toast.LENGTH_LONG).show();
        }
    }

    // TODO: 4/23/2018 ANALYZE the below code for SHOWING LIVE FRIENDS
    Marker myMarker;
    private void updateDisplay(DataSnapshot dataSnapshot) {
        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshot.getValue();
        double lat = Double.parseDouble(value.get("latitude").toString());
        double lng = Double.parseDouble(value.get("longitude").toString());
        LatLng location = new LatLng(lat, lng);

        if (myMarker != null) {
            myMarker.remove();
        }

        myMarker = mMap.addMarker(new MarkerOptions().position(location).title("My Location").snippet("address, cell, msg"));
        myMarker.showInfoWindow();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomValue));
    }

    // shared variable
    private float zoomValue = 0;
    @Override
    public void onCameraMove() {
        zoomValue = mMap.getCameraPosition().zoom;
    }

    /** Called when the user clicks on a marker. */
    @Override
    public boolean onMarkerClick(Marker marker) {
        // Retrieve the data from the marker.
        Integer clickCount = (Integer) marker.getTag();
        Log.d(TAG, "onMarkerClick: ");

        // Check if a click count was set, then display the click count.
        if (clickCount != null) {
            clickCount = clickCount + 1;
            marker.setTag(clickCount);
            Toast.makeText(this,
                    marker.getTitle() +
                            " has been clicked " + clickCount + " times.",
                    Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    // Auto pop up feature for <= v1.6.0 users of Share Location
    DatabaseReference refUsers;
    // SUPPORT: https://stackoverflow.com/questions/47105575/android-firebase-stop-childeventlistener
    private void isNameProvided(String currentUser) {
        Log.d(TAG, "[ OK ] isNameProvided: ");
        refUsers = FirebaseDatabase.getInstance().getReference();
        refUsers.child("users").child("" + currentUser)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User isNameUser = null;
                    try {
                        isNameUser = dataSnapshot.getValue(User.class);
                    } catch (Exception e) {
                        Log.d(TAG, "[ ERROR ] isNameProvided().onDataChange: " + e.getMessage());
                    }

                    if (dataSnapshot.getChildrenCount() == 2) {
                        // TODO: 4/16/2018      7h later eureka!!! at 2018.Apr15 12.24 pm where started at 5.10 pm
                        // Never give up! Just keep standing...!!!
                        // Now, User will see map first but to interact with the map, user must have to input name
                        // I don't need to change my LoginActivity.java code. Where i will change from DisplayActivity.java code

                        // CODE is READY to CHANGE
                        // User friendly Toast
                        Toast.makeText(getApplicationContext(),
                                "Please input your name, to get better UX. As you are <= v1.6.0 users of Share Location", Toast.LENGTH_LONG).show();
                        // DEBUGGER
                        try {
                            Log.d(TAG, "[ OK ] isNameProvided().onDataChange: isNameProvided = " + false + " ------KEY: "
                                    + dataSnapshot.getKey() + ", " + dataSnapshot.getChildrenCount() + ", NAME: " + isNameUser.getName());
                        } catch (Exception e){
                            Log.d(TAG, "[ ERROR ] isNameProvided().onDataChange: " + e.getMessage());
                        }

                        // Take Input through pop up window and save to db
                        // step 1:  MAKE pop up window to take name input
                        // call method
                        popUpForName(dataSnapshot.getKey());
                    } else if (dataSnapshot.getChildrenCount() == 3) {
                        Log.d(TAG, "[ OK ] onDataChange: DEBUGGER----- >= v1.7.0 users of Share Location. So, Name already provide during sign up");
                        Log.d(TAG, "[ OK ] onDataChange: DEBUGGER-----INSIDE isNameProvided ------KEY: " + dataSnapshot.getKey() + ", " + dataSnapshot.getChildrenCount() + ", NAME: " + isNameUser.getName());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
    }
    // SUPPORT: https://stackoverflow.com/questions/10903754/input-text-dialog-android
    private String name = "";
    // SUPPORT: https://stackoverflow.com/questions/4134117/edittext-on-a-popup-window
    private void popUpForName(final String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Give your name");

        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("SUBMIT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do something here on SUBMIT
                name = input.getText().toString();
                Toast.makeText(getApplicationContext(), "Thank you!  " + name, Toast.LENGTH_SHORT).show();
                final String msg = "Congratulation! Your name saved";

                // setp 2:  Store name into db
                refUsers.child("users").child(key).child("name").setValue(name);

                // Make sure user
                refUsers.child("users").child(key)
                .addListenerForSingleValueEvent( new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        Log.d(TAG, "onDataChange: DEBUGGER----- name: " + user.getName());

                        // ERROR: user.getName() == name
                        if (user.getName().equals(name)) {
                            Toast.makeText(getApplicationContext(), "" + msg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public static double lat;
    public static double lang;
    @Override
    public void onInfoWindowClick(Marker marker) {
        // SUPPORT: https://stackoverflow.com/questions/18077040/android-map-v2-get-marker-position-on-marker-click
        marker.hideInfoWindow();
        lat = marker.getPosition().latitude;
        lang =marker.getPosition().longitude;
        String name = marker.getTitle();
        Toast.makeText(getApplicationContext(), "" + name + "'s Current Location", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(DisplayActivity.this, StreetViewPanoramaBasicDemoActivity.class));
    }

    // SUPPORT: https://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-on-android
    public static boolean dangerStatus = false;
    public void onClickDanger(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("DANGER !!!");

        // SET danger control: Logic
        if (dangerStatus == false) {
            builder.setMessage("Are you in Danger?");

            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                dangerStatus = true;
                Toast.makeText(getApplicationContext(), "Live users seeing, you're in DANGER!", Toast.LENGTH_LONG).show();
                ivDanger.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.danger_icon_run));

                // TODO: 4/18/2018: UPDATE db: danger = 1
                databaseReference.child("users").child("" + firebaseAuth.getCurrentUser().getUid()).child("danger").setValue("1");
                // TODO: 4/23/2018 MAKE animated marker
                dialog.dismiss();
                }
            });

            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(), "Peaceful, u're not in DANGER!", Toast.LENGTH_LONG).show();
                    // Do nothing
                    dialog.dismiss();
                }
            });
        } else if (dangerStatus){
            builder.setMessage("Are you OUT OF Danger?");

            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dangerStatus = false;
                    Toast.makeText(getApplicationContext(), "Peaceful, u're not in DANGER!", Toast.LENGTH_LONG).show();
                    ivDanger.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.danger_icon));

                    // TODO: 4/18/2018: UPDATE db: danger = 0
                    databaseReference.child("users").child("" + firebaseAuth.getCurrentUser().getUid()).child("danger").setValue("0");

                    dialog.dismiss();
                }
            });

            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(), "Live users seeing, you're in DANGER!", Toast.LENGTH_LONG).show();
                    // Do nothing
                    dialog.dismiss();
                }
            });
        }

        AlertDialog alert = builder.create();
        alert.show();
    }

    boolean pictureStatus = false;
    public void onClickLocationPicture(View view) {

        if (!pictureStatus) {
            pictureStatus = true;
            // SUPPORT: https://stackoverflow.com/questions/5756136/how-to-hide-a-view-programmatically
            tvPoint.setVisibility(View.VISIBLE);

            Toast.makeText(getApplicationContext(), "Picture enable", Toast.LENGTH_SHORT).show();
            ivPicture.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_photo_black_24dp)));

        } else {
            pictureStatus = false;
            tvPoint.setVisibility(View.INVISIBLE);

            Toast.makeText(getApplicationContext(), "Picture disable", Toast.LENGTH_SHORT).show();
            ivPicture.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image_2)));
        }


    }

    // Android SDK Lifecycle
    // SUPPORT: https://stackoverflow.com/questions/19484493/activity-life-cycle-android
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "[ OK ] ---- onStart: ----");
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "[ OK ] ---- onResume: ----");
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "[ OK ] ---- onPause: ----");
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "[ OK ] ---- onStop: ----");
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        if (taskCompleted) {
            finish();
        }
        Log.d(TAG, "[ OK ] ---- onRestart: ----");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "[ OK ] ---- onDestroy: ----");
    }
}