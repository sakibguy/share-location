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
import android.widget.Button;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import playlagom.sharelocation.auth.LoginActivity;
import playlagom.sharelocation.libs.Converter;
import playlagom.sharelocation.libs.GoogleMapOperations;
import playlagom.sharelocation.models.KeyValue;
import playlagom.sharelocation.models.User;

public class DisplayActivity extends FragmentActivity implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnInfoWindowClickListener,
        OnMapReadyCallback, GoogleMap.OnCameraMoveListener {

    private static final String RECEIVED_FRIEND_REQUESTS = "receivedFriendRequests";
    ImageView ivUserImage, ivMyCircle, ivDanger, ivStreetView, ivNotification;
    private static final String SENT_FRIEND_REQUESTS = "sentFriendRequests";
    private static final String TAG = DisplayActivity.class.getSimpleName();
    private static final String LOG_TAG = "DisplayActivity";
    private static final int PERMISSIONS_REQUEST = 1;
    private HashMap<String, Marker> blueMarkers;
    boolean locationPermissionGranted = false;
    private boolean insideShouldShow = false;
    static boolean taskCompleted = false;
    TextView tvPosition, tvPoint;
    GoogleMap mMap;

    // Firebase
    FirebaseAuth firebaseAuth;
    DatabaseReference allMarkerRef;
    DatabaseReference databaseReference;

    View mapView;
    AdView mAdView;

    // Danger sound
    private LatLng location = new LatLng(0, 0);
    private static boolean makeDangerSound = true;
    private int userCounter = 1;
    MediaPlayer dangerSound;
    int width, height;

    // TODO: 5/8/2018  VISUALIZE through icon
    // CACHE STORAGE: for less server call & faster data manipulation
    public static LinkedHashMap<String, KeyValue> lhmReceivedFriendRequests = new LinkedHashMap<>();
    public static LinkedHashMap<String, KeyValue> lhmFriends = new LinkedHashMap<>();
    public static LinkedHashMap<String, KeyValue> lhmSentFriendRequests = new LinkedHashMap<>();

    // IMPLEMENT logic to USE less memory through LinkedHashMap
    // NOW using HashMap for completion purpose, where more memory is used
    // FAILED:   HashMap<String, KeyValue> keyValuePosition = new HashMap<>();
    // SUPPORT: https://stackoverflow.com/questions/5237101/is-it-possible-to-get-element-from-hashmap-by-its-position
    // SUPPORT: https://www.tutorialspoint.com/compile_java_online.php
    public static LinkedHashMap<String, KeyValue> linkedHashMap = new LinkedHashMap<>();

    // Retrieve logged in username to use on add friend feature
    public static String loggedInUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        Log.d(TAG, "[ OK ] ---- onCreate: ----");

        // google map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Log.d(TAG, "[ OK ] ---- async map request done. " +
                "Waiting... for onMapReady interface/callback listener execution");
        mapView = mapFragment.getView();

        // INIT dependencies setup
        // firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.sharelocation));
        allMarkerRef = FirebaseDatabase.getInstance().getReference(getString(R.string.sharelocation));

        // TODO: 5/6/2018 REMOVE method below, at next version
        // COPY name: from sharelocation-users to SL11302018MAY6
//        copyLoggedInUserInfoToNewStructure();


        // NEVER remove: logged in username
        retrieveLoggedInUserName();

        // RETRIEVE and STORE on cache
        // friends
        retrieveFriends();
        // Received friend requests
        retrieveReceivedFriendRequests();

        // WIRE widgets: convert xml components to java object
        // ImageView
        ivDanger = findViewById(R.id.ivDanger);
        ivUserImage = findViewById(R.id.ivUserImage);
        ivMyCircle = findViewById(R.id.ivMyCircle);
        ivStreetView = findViewById(R.id.ivStreetView);
        ivNotification = findViewById(R.id.ivNotification);

        // TextView
        tvPosition = findViewById(R.id.tvPosition);
        tvPosition.setVisibility(View.INVISIBLE);
        tvPoint = findViewById(R.id.tvPoint);
        tvPoint.setVisibility(View.INVISIBLE);

        // Sound
        // SUPPORT: https://stackoverflow.com/questions/18459122/play-sound-on-button-click-android
        dangerSound = MediaPlayer.create(this, R.raw.siren_alert_1);

        // INIT setup
        // ImageView: CHECK danger status and SET icon
        showDangerIcon();

        // ImageView: MAKE round user image
        ivUserImage.setImageBitmap(Converter.getCroppedBitmap(
                BitmapFactory.decodeResource(getResources(), R.drawable.current_user)));

        // ImageView: CHECK if new friend request and SET icon NEW
        databaseReference
            .child(firebaseAuth.getCurrentUser().getUid())
                .child(RECEIVED_FRIEND_REQUESTS)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getChildrenCount() > 0) {
                                ivNotification.setImageBitmap(
                                        BitmapFactory.decodeResource(getResources(), R.drawable.ic_fiber_new_black_24dp));
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

        // HANDLE events
        // ImageView: see onClickLogout()
        // ImageView: see onClickStreetView()
        // ImageView: see onClickMyCircle()
        // ImageView: see onClickUserImage()
        // ImageView: see onClickNotification()

        // CHECK GPS is status
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please ON GPS", Toast.LENGTH_LONG).show();
            finish();
        }

        // INIT AdMob app ID
        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");

        // ADS: https://developers.google.com/admob/android/banner
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Local cache for markers
        blueMarkers = new HashMap<>();

        checkLocationPermission();
    }

    private void retrieveLoggedInUserName() {
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                    .child("name")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot != null ) {
                                    loggedInUserName = String.valueOf(dataSnapshot.getValue());
                                    Log.d(TAG, "[ OK ] -------- logged in user: " + loggedInUserName);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
    }

    private void retrieveReceivedFriendRequests() {
        databaseReference
            .child(firebaseAuth.getCurrentUser().getUid())
                .child(getString(R.string.receivedFriendRequests))
                    .addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            if (dataSnapshot != null) {
                                // DON'T USE dataSnapshot.getChildrenCount() > 0 to get rid of error
//                                if (dataSnapshot.getChildrenCount() > 0) {
                                    Log.d(TAG, "[ OK ] ------ " +
                                            "retrieveReceivedFriendRequests.onChildAdded: KEY " + dataSnapshot.getKey() + ", " +
                                            "VALUE " + dataSnapshot.getValue());

                                    // TEST with ListView<T>
                                    // KeyValue keyValue = new KeyValue();
                                    // keyValue.key = dataSnapshot.getKey();
                                    // keyValue.value = String.valueOf(dataSnapshot.getValue());

                                    // receivedFriendRequestsList.add(keyValue);

                                    KeyValue keyValue = new KeyValue();

                                    keyValue.key = dataSnapshot.getKey();
                                    if (dataSnapshot.hasChild("name")) {
                                        keyValue.name = String.valueOf(dataSnapshot.child("name").getValue());
                                        Log.d(TAG, "[ OK ] ---------  .... works... " + keyValue.name + ", " +
                                                "KEY: " + keyValue.key);
//                                        keyValue.value = String.valueOf(dataSnapshot.getValue());
                                    }

                                    // How to track which key at which position in List?
                                    // NEED LinkedHashMap
                                    // SUPPORT: https://stackoverflow.com/questions/5237101/is-it-possible-to-get-element-from-hashmap-by-its-position
//                                keyValuePosition.put(keyValue.key, keyValue);

                                    // TODO: 5/8/2018 CODING CHALLENGE
                                    // WHY LinkedHashMap: to track HashMap index/position
                                /* Populate */
//                                    linkedHashMap.put(dataSnapshot.getKey(), keyValue);

                                    lhmReceivedFriendRequests.put(keyValue.key, keyValue);
//                                }
                            }
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                            if (dataSnapshot != null) {
                                Log.d(TAG, "[ OK ] ------ " +
                                        "retrieveReceivedFriendRequests.onChildChanged: KEY " + dataSnapshot.getKey() + ", " +
                                        "VALUE " + dataSnapshot.getValue());

                                KeyValue keyValue = new KeyValue();
                                keyValue.key = dataSnapshot.getKey();
                                if (dataSnapshot.hasChild("name")) {
                                    keyValue.name = String.valueOf(dataSnapshot.child("name").getValue());
                                    Log.d(TAG, "[ OK ] --------- retrieveReceivedFriendRequests.onChildChanged: .... works... " + keyValue.name + ", " +
                                            "KEY: " + keyValue.key);
                                }
                                lhmReceivedFriendRequests.put(keyValue.key, keyValue);
                            }
                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {
                            if (dataSnapshot != null) {
                                KeyValue keyValue = new KeyValue();
                                keyValue.key = dataSnapshot.getKey();

                                lhmReceivedFriendRequests.remove(keyValue.key);
                                Log.d(TAG, "[ OK ] ------ " +
                                        "retrieveReceivedFriendRequests.onChildRemoved: KEY " + dataSnapshot.getKey() + ", " +
                                        "VALUE " + dataSnapshot.getValue());
                            }
                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                            Log.d(TAG, "[ OK ] ------ onChildMoved: retrieveReceivedFriendRequests...?? WHY");
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "[ OK ] -------- retrieveReceivedFriendRequests.onCancelled: " +
                                    "" + databaseError.getMessage());
                        }
                    });
    }

    private void retrieveFriends() {
        databaseReference
            .child(firebaseAuth.getCurrentUser().getUid())
                .child(getString(R.string.friends))
                    .addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            // DON'T USE dataSnapshot.getChildrenCount() > 0 to stay error free
                            Log.d(TAG, "[ OK ] -- -- " +
                                    "retrieveFriends.onChildAdded: KEY " + dataSnapshot.getKey() + ", " +
                                    "VALUE " + dataSnapshot.getValue());
                            if (dataSnapshot != null) {

                                KeyValue keyValue = new KeyValue();
                                keyValue.key = dataSnapshot.getKey();

                                if (dataSnapshot.hasChild("name")) {
                                    keyValue.name = String.valueOf(dataSnapshot.child("name").getValue());
                                    Log.d(TAG, "[ OK ] -- --- name: " + keyValue.name + ", " +
                                            "KEY: " + keyValue.key);
                                }
                                lhmFriends.put(keyValue.key, keyValue);
                            }
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                            Log.d(TAG, "[ OK ] -- -- " +
                                    "retrieveFriends.onChildChanged: KEY " + dataSnapshot.getKey() + ", " +
                                    "VALUE " + dataSnapshot.getValue());
                            // DON'T USE dataSnapshot.getChildrenCount() > 0 to stay error free
                            KeyValue keyValue = new KeyValue();
                            keyValue.key = dataSnapshot.getKey();

                            if (dataSnapshot.hasChild("name")) {
                                keyValue.name = String.valueOf(dataSnapshot.child("name").getValue());
                                Log.d(TAG, "[ OK ] -- --- name: " + keyValue.name + ", " +
                                        "KEY: " + keyValue.key);
                            }
                            lhmFriends.put(keyValue.key, keyValue);
                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {
                            if (dataSnapshot != null) {
                                KeyValue keyValue = new KeyValue();
                                keyValue.key = dataSnapshot.getKey();

                                lhmFriends.remove(keyValue.key);
                                Log.d(TAG, "[ OK ] ------ " +
                                        "retrieveFriends.onChildRemoved: KEY " + dataSnapshot.getKey() + ", " +
                                        "VALUE " + dataSnapshot.getValue());
                            }
                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "[ OK ] -------- retrieveFriends.onCancelled: " +
                                    "" + databaseError.getMessage());
                        }
                    });
    }

    // TODO: 5/6/2018 REMOVE method below, at next version
    // COPY name: from sharelocation-users to SL11302018MAY6
    private void copyLoggedInUserInfoToNewStructure() {
        Log.d(TAG, "[ OK ] -------- COPIED: copyLoggedInUserInfoToNewStructure: ");
        DatabaseReference tempRef = FirebaseDatabase.getInstance().getReference("sharelocation-users");
        tempRef
            .child(firebaseAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "[ test ] --- " + dataSnapshot.toString());
                        if (dataSnapshot.hasChild("name")) {
                            databaseReference
                                .child(firebaseAuth.getCurrentUser().getUid())
                                    .child("name").setValue(dataSnapshot.child("name").getValue());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "[ test ] --- " + databaseError.toString());
                    }
                });
    }

    private void showDangerIcon() {
        databaseReference.child(firebaseAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.hasChild("danger")) {
                            if (dataSnapshot.child("danger").getValue().equals("1")) {
                                // SET run danger icon
                                ivDanger.setImageBitmap(Converter.getCroppedBitmap(
                                        BitmapFactory.decodeResource(getResources(), R.drawable.danger_icon_run)));
                                Toast.makeText(getApplicationContext(), "Your status in DANGER",
                                        Toast.LENGTH_LONG).show();
                                dangerStatus = true;
                            } else {
                                // SET default danger icon
                                ivDanger.setImageBitmap(Converter.getCroppedBitmap(
                                        BitmapFactory.decodeResource(getResources(), R.drawable.danger_icon)));
                                dangerStatus = false;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    // SUPPORT: https://stackoverflow.com/questions/15368028/getting-a-map-marker-by-its-id-in-google-maps-v2
    private HashMap<String, String> hashMapMidUid = new HashMap<String, String>();
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
                            Log.d(TAG, "[ OK ] ---- LOOP: showAllRegisteredUsers() .. " + userCounter ++ +
                                    " uid: " + uniqueID + ", " + temporaryUser.getEmail() + ", " + location);

                            if (!blueMarkers.containsKey(uniqueID)) {
                                tempBlueMarker = mMap.addMarker(new MarkerOptions().title("" + temporaryUser.getName() + "")
                                        .position(location)
                                        .snippet("img, fnd req, call, msg")
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_BLUE       // SET live users marker green
                                        )));
                                blueMarkers.put(uniqueID, tempBlueMarker);
                                tempBlueMarker.showInfoWindow();
                                // STORE (MarkerID vs UID)at RAM
                                hashMapMidUid.put(tempBlueMarker.getId(), uniqueID);
                            } else {
                                blueMarkers.get(uniqueID).setPosition(location);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[ ERROR ] ---- showAllRegisteredUsers().onDataChange: " + e.getMessage());
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        // SET boundary: for auto focus area at dev time
//        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
//            @Override
//            public void onMapLoaded() {
//                LatLngBounds.Builder builder = new LatLngBounds.Builder();
//                for (Marker tempMarker : blueMarkers.values()) {
//                    builder.include(tempMarker.getPosition());
//                }
//                final LatLngBounds bounds = builder.build();
//                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 300));
//            }
//        });
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
            isNameProvided();
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

    public static double lat;
    public static double lang;
    LatLng latLng;
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "[ OK ] ---- onMapReady executing...");
        mMap = map;

        if (mMap != null) {
            // SET: blue markers for all registered users by default.
            showAllRegisteredUsers();
        } else {
            Log.e(TAG, "[ ERROR ] ---- onMapReady: null");
        }

        // TODO: Before enabling the My Location layer, you must request
        // location permission from the user. This sample does not include
        // a request for location permission.

        // Change my location button position at the bottom
        // SUPPORT: https://stackoverflow.com/questions/36785542/how-to-change-the-position-of-my-location-button-in-google-maps-using-android-st/39179202
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView
                    .findViewById(Integer.parseInt("1"))
                        .getParent()).findViewById(Integer.parseInt("2"));
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
    // subscribing to updates in Firebase and
    // when an update occurs.
    static boolean showAllLiveUser = true;

    // The subscribeToUpdates() method calls setMarker()
    // whenever it receives a new or updated location for a tracked device.
    private void subscribeToUpdates() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.sharelocation));

        // logcat color
        // SUPPORT: https://medium.com/@gun0912/android-studio-how-to-change-logcat-color-3c17a10beef8
        Log.d(TAG, "[ OK ] -- subscribeToUpdates:");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "[ OK ] -- subscribeToUpdates.onChildAdded:");
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "[ OK ] -- subscribeToUpdates.onChildChanged:");
                showMarker(dataSnapshot);
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
    private void showMarker(DataSnapshot dataSnapshot) {
        Log.d(TAG, "[ OK ] -- setActiveMarker: obj: " + dataSnapshot.toString());
        String showMarkerTempUID = null;

        if (showAllLiveUser) {          // SHOW all (live + not live)
            // CASE 1: RECEIVE data through try catch
            try {
                User showMarkerTempUser = dataSnapshot.getValue(User.class);
                Log.d(TAG, "[ OK ] -- setActiveMarker: name: " + showMarkerTempUser.getName());
                showMarkerTempUID = dataSnapshot.getKey();

                // CASE 2: CHECK data
                if (showMarkerTempUser != null) {
                    // CASE 3: CHECK and SET marker green/blue as user online/offline
                    showOnlineOfflineStatus(showMarkerTempUser, showMarkerTempUID);

                    // CASE 4: CHECK and SET animated marker as user at danger
                    showAnimatedMarkerAtDanger(showMarkerTempUser, showMarkerTempUID);
                } else {
                    Log.e(TAG, "[ ERROR ] -- NULL USER, showMarker()");
                }
            } catch (Exception e){
                Log.e(TAG, "[ ERROR ] -- showMarker(): " + e.getMessage());
            }
        } else {                            // SHOW friends (live + not live)
            if (friendsOnlyIconClicked) {
                for (Marker CurrMarker : blueMarkers.values()) {
                    // SUPPORT: https://stackoverflow.com/questions/13692398/remove-a-marker-from-a-googlemap
                    CurrMarker.remove();
                }
                blueMarkers.clear();
            }
        }
//        backup();
    }

    private void showAnimatedMarkerAtDanger(User showMarkerTempUser, String showMarkerTempUID) {
        if (showMarkerTempUser.getDanger() != null) {
            String danger = showMarkerTempUser.getDanger();
            if (danger.equals("1")) {
                try {
                    // SUPPORT: https://stackoverflow.com/questions/22202299/how-do-i-remove-all-radius-circles-from-google-map-android-but-keep-pegs
                    // CLEAR red circles
                    for (Circle myCircle : GoogleMapOperations.circleList) {
                        myCircle.remove();
                    }
                    GoogleMapOperations.circleList.clear();

                    double lat  = Double.parseDouble(showMarkerTempUser.getPosition().getLatitude());
                    double lang = Double.parseDouble(showMarkerTempUser.getPosition().getLongitude());
                    location = new LatLng(lat, lang);

                    // SUPPORT: https://www.youtube.com/watch?v=hS7EFdDLjas
                    // animation
                    GoogleMapOperations.addingCircleView(mMap, location);
                    Log.d(TAG, "[ OK ] -- showAnimatedMarkerAtDanger: latlang: " + location.toString());

                    // It is notified each time one of the device's location is updated.
                    // When this happens, it will either create a new marker at the device's location,
                    // or move the marker for a device if it exists already.
                    if (blueMarkers.containsKey(showMarkerTempUID)){
                        Log.d(TAG, "[ OK ] -- showAnimatedMarkerAtDanger: CHANGE color as user already exits at map");

                        blueMarkers.get(showMarkerTempUID).setTitle(showMarkerTempUser.getName());
                        blueMarkers.get(showMarkerTempUID).setSnippet("cell, msg, fnd req");
                        blueMarkers.get(showMarkerTempUID).setIcon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED       // SET live user marker green
                                )
                        );
                        blueMarkers.get(showMarkerTempUID).setPosition(location);
                        blueMarkers.get(showMarkerTempUID).showInfoWindow();
                    }
                }catch (Exception e) {
                    Log.d(TAG, "[ ERROR ] -- showAnimatedMarkerAtDanger: " + e.getMessage());
                }

                // START danger sound
                if (makeDangerSound) {
                    makeDangerSound = false;
                    dangerSound.start();
                    Toast.makeText(getApplicationContext(), "NEED help!\n" + showMarkerTempUser.getName()
                            + " is at DANGER now", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // TO make offline: close app -> tab notification to stop service -> clear app running history
    private void showOnlineOfflineStatus(User currentUser, String currentUID) {
        if (currentUser.getOnline() != null) {
            if (currentUser.getOnline().equals("1")) {
                Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: online = 1");
                try {
                    double lat  = Double.parseDouble(currentUser.getPosition().getLatitude());
                    double lang = Double.parseDouble(currentUser.getPosition().getLongitude());
                    location = new LatLng(lat, lang);
                    Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: latlang: " + location.toString());

                    // It is notified each time one of the device's location is updated.
                    // When this happens, it will either create a new marker at the device's location,
                    // or move the marker for a device if it exists already.
                    if (blueMarkers.containsKey(currentUID)){
                        Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: CHANGE color as user already exits at map");

                        blueMarkers.get(currentUID).setTitle(currentUser.getName());
                        blueMarkers.get(currentUID).setSnippet("img, fnd req, call, msg");
                        blueMarkers.get(currentUID).setIcon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_GREEN       // SET live user marker green
                                )
                        );
                        blueMarkers.get(currentUID).setPosition(location);
                        blueMarkers.get(currentUID).showInfoWindow();
                    } else if(!blueMarkers.containsKey(currentUID)){
                        Marker marker = mMap.addMarker(new MarkerOptions().title("" + currentUser.getName() + "")
                            .position(location)
                            .snippet("img, fnd req, call, msg")
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED       // SET live users marker green
                            )));
                        blueMarkers.put(currentUID, marker);
                        marker.showInfoWindow();
                    }
                }catch (Exception e) {
                    Log.d(TAG, "[ ERROR ] -- showOnlineOfflineStatus: " + e.getMessage());
                }
            } else if (currentUser.getOnline().equals("0")){
                Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: online = 0");
                Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: DISCONNECTING USER...");
                blueMarkers.get(currentUID).setTitle(currentUser.getName());
                blueMarkers.get(currentUID).setSnippet("cell, msg, fnd req");
                blueMarkers.get(currentUID).setIcon(
                        BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_BLUE       // SET live user marker green
                        )
                );
                blueMarkers.get(currentUID).setPosition(location);
                blueMarkers.get(currentUID).showInfoWindow();
            }
        }
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
    // SUPPORT: https://stackoverflow.com/questions/47105575/android-firebase-stop-childeventlistener
    private void isNameProvided() {
        Log.d(TAG, "[ OK ] isNameProvided: ");
        databaseReference.child(firebaseAuth.getCurrentUser().getUid())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.hasChild("name")) {
                        // MAKE pop up window to take name
                        popUpForName(dataSnapshot.getKey());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
    }

    // SUPPORT: https://stackoverflow.com/questions/10903754/input-text-dialog-android
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

        // SETUP buttons
        builder.setPositiveButton("SUBMIT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String name = input.getText().toString();
                Toast.makeText(getApplicationContext(), "Thank you!  " + name, Toast.LENGTH_SHORT).show();
                final String msg = "Congratulation! Your name saved";

                // STORE name
                databaseReference.child(key).child("name").setValue(name);

                // NOTIFY user
                databaseReference.child(key)
                    .addListenerForSingleValueEvent( new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // ERROR: user.getName() == name
                        if (dataSnapshot.child("name").getValue().equals(name)) {
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

    @Override
    public void onInfoWindowClick(final Marker marker) {
        // SUPPORT: https://stackoverflow.com/questions/18077040/android-map-v2-get-marker-position-on-marker-click
        marker.hideInfoWindow();

        // GET marker obj info
        final String userName = marker.getTitle();
        // GET (MarkerID vs UID) from RAM
        final String markerUID = hashMapMidUid.get(marker.getId());
        // GET logged in user unique key, who click onto marker?
        final String UID = firebaseAuth.getCurrentUser().getUid();

        // SUPPORT: https://www.mkyong.com/android/android-custom-dialog-example/
        // ADD custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.marker_click_dialog);

        // WIRE widgets
        TextView tvFriendRequestStatus = dialog.findViewById(R.id.tvFriendRequestStatus);
        final Button btnAddFriend = dialog.findViewById(R.id.btnAddFriend);
        ImageView ivStreetView = dialog.findViewById(R.id.ivStreetView);
        ImageView ivMessage = dialog.findViewById(R.id.ivMessage);
        ImageView ivCall = dialog.findViewById(R.id.ivCall);
        TextView tvName = dialog.findViewById(R.id.tvName);

        // INIT widgets values
        // TextView
        tvFriendRequestStatus.setVisibility(View.GONE);
        tvName.setText(userName);
        // ImageView
        ivCall.setVisibility(View.VISIBLE);

        if (markerUID.equals(UID)) {    // CHECK clicked to self or not
            btnAddFriend.setVisibility(View.GONE);
            ivCall.setVisibility(View.GONE);
        } else {                        // Button: CHECK already friend or not then req sent or not
            databaseReference
                .child(UID)
                    .child(getString(R.string.friends))
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot != null) {
                                    if (dataSnapshot.hasChild(markerUID)) {
                                        btnAddFriend.setText("Already Friend");
                                        btnAddFriend.setEnabled(false);
                                        Toast.makeText(getApplicationContext(), "Start chat/call",
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        // CHECK isExist at RECEIVED_FRIEND_REQUESTS
                                        databaseReference
                                            .child(UID).child(getString(R.string.receivedFriendRequests)).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    if (dataSnapshot != null) {
                                                        if (dataSnapshot.hasChild(markerUID)) {
                                                            btnAddFriend.setText("CHECK Friend Requests");
                                                            btnAddFriend.setEnabled(false);
                                                            Toast.makeText(getApplicationContext(), "This user want to be your friend",
                                                                    Toast.LENGTH_LONG).show();
                                                        } else {
                                                            databaseReference
                                                                .child(UID)
                                                                    .child(SENT_FRIEND_REQUESTS)
                                                                    .child(markerUID)
                                                                    .child("value")
                                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                        @Override
                                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                                            Log.d(TAG, "[ OK ] ----- fnd req sent: " + dataSnapshot.getValue());
                                                                            if (dataSnapshot.getValue() != null) {
                                                                                if (dataSnapshot.getValue().toString().equals("0")) {
                                                                                    btnAddFriend.setText("Friend Request Sent");
                                                                                    Toast.makeText(getApplicationContext(), "Please wait for acceptance",
                                                                                            Toast.LENGTH_LONG).show();
                                                                                    btnAddFriend.setEnabled(false);
                                                                                } else if (dataSnapshot.getValue().toString().equals("1")) {
                                                                                    btnAddFriend.setText("Already Friend");
                                                                                    btnAddFriend.setEnabled(false);
                                                                                    Toast.makeText(getApplicationContext(), "Start chat/call",
                                                                                            Toast.LENGTH_LONG).show();
                                                                                }
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onCancelled(DatabaseError databaseError) {
                                                                            Log.e(TAG, "[ ERROR ] ---- onCancelled: " + databaseError.getMessage());
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
        }

        // HANDLE events through listener
        // ImageView
        ivStreetView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // IMPLEMENT game style touch event
                Toast.makeText(getApplicationContext(), "" +
                        "" + userName + "'s Current Location", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DisplayActivity.this, StreetViewPanoramaBasicDemoActivity.class));
                return false;
            }
        });

        // HANDLE call event
        ivCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Coming soon... PHONE CALL", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // HANDLE chatengine event
        ivMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Coming soon... CHAT ENGINE", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // HANDLE add friend event
        btnAddFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "" +
                        "Wait for accepting friend request", Toast.LENGTH_SHORT).show();
                // TODO: 5/5/2018 SHOW text "Friend request sent"
                btnAddFriend.setText("Friend Request Sent");

                // UPDATE
                // requested user: RECEIVED_FRIEND_REQUESTS
                databaseReference
                        .child(markerUID)    // USED HashMap to get clicked marker uid & logged in user uid
                        .child(RECEIVED_FRIEND_REQUESTS)
                        .child(UID)
                        .child("key")
                        .setValue(UID);
                databaseReference
                        .child(markerUID)    // USED HashMap to get clicked marker uid & logged in user uid
                        .child(RECEIVED_FRIEND_REQUESTS)
                        .child(UID)
                        .child("value")
                        .setValue("0");
                databaseReference
                        .child(markerUID)    // USED HashMap to get clicked marker uid & logged in user uid
                        .child(RECEIVED_FRIEND_REQUESTS)
                        .child(UID)
                        .child("name")
                        .setValue(loggedInUserName);

                // loggedin user: SENT_FRIEND_REQUESTS
                databaseReference
                        .child(UID)    // USED HashMap to get clicked marker uid & logged in user uid
                        .child(SENT_FRIEND_REQUESTS)
                        .child(markerUID)
                        .child("key")
                        .setValue(markerUID);

                databaseReference
                        .child(UID)    // USED HashMap to get clicked marker uid & logged in user uid
                        .child(SENT_FRIEND_REQUESTS)
                        .child(markerUID)
                        .child("value")
                        .setValue("0");

                databaseReference
                        .child(UID)    // USED HashMap to get clicked marker uid & logged in user uid
                        .child(SENT_FRIEND_REQUESTS)
                        .child(markerUID)
                        .child("name")
                        .setValue(marker.getTitle());
            }
        });
        dialog.show();
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
                databaseReference.child("" + firebaseAuth.getCurrentUser().getUid()).child("danger").setValue("1");
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
                    databaseReference.child("" + firebaseAuth.getCurrentUser().getUid()).child("danger").setValue("0");

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
        lhmReceivedFriendRequests.clear();
        lhmFriends.clear();
        databaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("online").onDisconnect().setValue("0");
    }
    private void backup() {
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



//                    if (user != null) {
////                        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshotGlobal.getValue();
////                        Log.d(TAG, i++ + " setActiveMarker, KEY: " + uid + ", VALUE: " + value.toString() + "," +
////                                " DEBUGGER:-------- ");
//
////                        double lat = Double.parseDouble(value.get("latitude").toString());
////                        double lng = Double.parseDouble(value.get("longitude").toString());
//
//                        LatLng location = new LatLng(user.position.getLatitude(), user.position.getLongitude());

        // SHOW live all
//                        if (showAllLiveUser) {
//                            // SUPPORT: https://stackoverflow.com/questions/22202299/how-do-i-remove-all-radius-circles-from-google-map-android-but-keep-pegs
//                            for (Circle myCircle : GoogleMapOperations.circleList) {
//                                myCircle.remove();
//                            }
//                            GoogleMapOperations.circleList.clear();
//
//                            Log.d(TAG, "setActiveMarker: KEY: " + i + " " + uid + "," +
//                                    " DEBUGGER:-------- ");
//
//                            // It is notified each time one of the device's location is updated.
//                            // When this happens, it will either create a new marker at the device's location,
//                            // or move the marker for a device if it exists already.
//                            if (!blueMarkers.containsKey(uid)) {
//                                tempGreenMarker = mMap.addMarker(new MarkerOptions().title("" + greenMarkerUser.getName() + "")
//                                    .position(location)
//                                    .snippet("cell, msg, fnd req")
//                                    .icon(BitmapDescriptorFactory.defaultMarker(
//                                                BitmapDescriptorFactory.HUE_RED       // SET live users marker green
//                                    )));
//                                blueMarkers.put(uid, tempGreenMarker);
//                                tempGreenMarker.showInfoWindow();
//
//                                // READY to CHANGE CODE
//                                // SET animated marker at danger
//                                if (greenMarkerUser.getDanger() != null) {
//                                    String danger = greenMarkerUser.getDanger();
//                                    if (danger.equals("1")) {
//                                        // SUPPORT: https://www.youtube.com/watch?v=hS7EFdDLjas
//                                        GoogleMapOperations.addingCircleView(mMap, location);
//                                        tempGreenMarker.setIcon(BitmapDescriptorFactory.defaultMarker(
//                                                BitmapDescriptorFactory.HUE_RED       // SET live users marker green
//                                        ));
//                                    }
//                                }
//                            } else {
//                                // READY to CHANGE CODE
//                                // SET animated marker at danger
//                                if (greenMarkerUser.getDanger() != null) {
//                                    String danger = greenMarkerUser.getDanger();
//                                    if (danger.equals("1")) {
//                                        // SUPPORT: https://www.youtube.com/watch?v=hS7EFdDLjas
//                                        GoogleMapOperations.addingCircleView(mMap, location);
//                                        tempGreenMarker.setIcon(BitmapDescriptorFactory.defaultMarker(
//                                                BitmapDescriptorFactory.HUE_RED       // SET live users marker green
//                                        ));
//
//                                        // START danger sound
//                                        if (makeDangerSound) {
//                                            makeDangerSound = false;
//                                            dangerSound.start();
//                                            Toast.makeText(getApplicationContext(), "NEED help!\n" + greenMarkerUser.getName()
//                                                    + " is at DANGER now", Toast.LENGTH_LONG).show();
//                                        }
//                                    } else if (danger.equals("0")) {
//                                        tempGreenMarker.setIcon(BitmapDescriptorFactory.defaultMarker(
//                                                BitmapDescriptorFactory.HUE_GREEN       // SET live users marker green
//                                        ));
//                                    }
//                                }
//                                mMarkers.get(uid).setPosition(location);
//                            }
//                        }

        // SHOW live friends
//                        if (friendsOnlyIconClicked) {
//                            for (Marker CurrMarker : mMarkers.values()) {
//                                // SUPPORT: https://stackoverflow.com/questions/13692398/remove-a-marker-from-a-googlemap
//                                CurrMarker.remove();
//                            }
//                            mMarkers.clear();
//                        }
//                    }

//                    user = null;
//                }

//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                    Log.e(TAG, "[ ERROR ] -- setActiveMarker().onCancelled():");
//                }
//            }
//        );
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
    // setting the marker on the map
    // which are responsible for
    // We've also got two empty methods - subscribeToUpdates() and setMarker()
    private void backupGeoFire() {
        //        // SUPPORT: https://www.101apps.co.za/index.php/item/182-firebase-realtime-database-tutorial.html
//        // 1: https://github.com/firebase/FirebaseUI-Android/issues/1040
//        // 2: https://stackoverflow.com/questions/26700924/query-based-on-multiple-where-clauses-in-firebase
//        // todo 3: https://github.com/firebase/geofire-java

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

//                    }
//                });
    }

    boolean pictureStatus = false;
    public void onClickStreetView(View view) {
        if (!pictureStatus) {
            pictureStatus = true;
            // SUPPORT: https://stackoverflow.com/questions/5756136/how-to-hide-a-view-programmatically
            tvPoint.setVisibility(View.VISIBLE);

            Toast.makeText(getApplicationContext(), "SWIPE map to see Picture", Toast.LENGTH_SHORT).show();
            ivStreetView.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_style_black_24dp)));

        } else {
            pictureStatus = false;
            tvPoint.setVisibility(View.INVISIBLE);

            Toast.makeText(getApplicationContext(), "Picture mode disabled", Toast.LENGTH_SHORT).show();
            ivStreetView.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_crop_original_black_24dp)));
        }
    }

    static boolean friendsOnlyIconClicked = false;
    public void onClickMyCircle(View view) {
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

    public void onClickUserImage(View view) {
        Toast.makeText(getApplicationContext(), "IMPLEMENT user hide from map", Toast.LENGTH_LONG).show();
    }

    public void onClickLogout(View view) {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(getApplicationContext(), "Successfully Logout", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onTouch: DEBUGGER-----Logout icon");
        startActivity(new Intent(DisplayActivity.this, LoginActivity.class));
        finish();
    }

    public void onClickNotification(View view) {
        Toast.makeText(getApplicationContext(), "Notifications", Toast.LENGTH_SHORT).show();

//        startActivity(new Intent(DisplayActivity.this, ProductListActivity.class));
        startActivity(new Intent(DisplayActivity.this, TabNPageViewerActivity.class));
        // SUPPORT: https://www.androidcode.ninja/android-alertdialog-example/
        // SUPPORT: https://developer.android.com/guide/topics/ui/dialogs
    }
}