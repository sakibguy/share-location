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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import playlagom.sharelocation.auth.LoginActivity;
import playlagom.sharelocation.libs.Converter;
import playlagom.sharelocation.models.User;
import playlagom.sharelocation.models.UserAndLocation;

public class DisplayActivity extends FragmentActivity implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnInfoWindowClickListener,
        OnMapReadyCallback, GoogleMap.OnCameraMoveListener {

    private static final String TAG = DisplayActivity.class.getSimpleName();
    private static final String LOG_TAG = "DisplayActivity";
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    private GoogleMap mMap;

    private static final int PERMISSIONS_REQUEST = 1;
    static boolean taskCompleted = false;
    private boolean insideShouldShow = false;

    private ImageView ivSelectedUser;
    private ImageView ivMyCircle;
    private ImageView ivDanger;
    private boolean locationPermissionGranted = false;

    // Firebase attributes
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;

    @Override
    protected void onRestart() {
        super.onRestart();
        if (taskCompleted) {
            finish();
        }
    }

    private AdView mAdView;
    View mapView;

    // Dander sound alert
    private MediaPlayer dangerSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapView = mapFragment.getView();
        mapFragment.getMapAsync(this);
        // wire xml components with java object
        // icons: live friends + danger
        ivMyCircle = findViewById(R.id.ivMyCircle);
        ivDanger = findViewById(R.id.ivDanger);

        // Sounds: SUPPORTED by: https://stackoverflow.com/questions/18459122/play-sound-on-button-click-android
        dangerSound = MediaPlayer.create(this, R.raw.siren_alert_1);

        // selected user
        ivSelectedUser = findViewById(R.id.ivSelectedUser);
        ivSelectedUser.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.current_user)));

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

        // Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable GPS location services", Toast.LENGTH_LONG).show();
            finish();
        }

        // Init: AdMob app ID
//        MobileAds.initialize(this, "ca-app-pub-6882836186513794~2015541759");
        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");

        // ADS: https://developers.google.com/admob/android/banner
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Init firebase dependency
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        checkLocationPermission();
    }

    private void checkLocationPermission() {
        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        int permission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            startTrackerService();
            // As location permission granted then set NAME input for <= v1.6.0 users through checking with the db.
            // TODO: 4/16/2018 SET pop up window to take name for <= v1.6.0 version users not for >= 1.7.0 users. Upto v1.6.0 there was no NAME field at sign up form. To give better UX, at v1.7.0 here added NAME field at sign up form.

            // TODO: 4/15/2018      CODE is ready to CHANGE
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

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        // TODO: Before enabling the My Location layer, you must request
        // location permission from the user. This sample does not include
        // a request for location permission.

        // Change my location button position at the bottom
        // SUPPORTED by stackoverflow: https://stackoverflow.com/questions/36785542/how-to-change-the-position-of-my-location-button-in-google-maps-using-android-st/39179202
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

        // blue dot
        if (locationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);
        }

        // Supported by: https://developers.google.com/maps/documentation/android-api/infowindows
        // Set a listener for info window events.
        mMap.setOnInfoWindowClickListener(this);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "My location", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    private void loginToFirebase() {
//        String email = getString(R.string.firebase_email);
//        String password = getString(R.string.firebase_password);
//        // Authenticate with Firebase and subscribe to updates
//        FirebaseAuth.getInstance().signInWithEmailAndPassword(
//                email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//            @Override
//            public void onComplete(Task<AuthResult> task) {
//                if (task.isSuccessful()) {
//                    subscribeToUpdates();
//                    Log.d(TAG, "firebase auth success");
//                } else {
//                    Log.d(TAG, "firebase auth failed");
//                }
//            }
//        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d(TAG, "firebase auth success");
//            subscribeToUpdates();
            setInactiveMarker();
        } else {
            Log.d(TAG, "firebase auth failed");
        }
    }

    List<User> userList = new ArrayList<>();

    int countOnDataChange = 1;
    private void subscribeToUpdates() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path));

        Log.d(TAG, "------inside--- subscribeToUpdates();");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                if (controllerBitClicked) {
                    Log.d(TAG, "------subscribeToUpdates()--- onChildAdded");
                    setActiveMarker(dataSnapshot);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                if (controllerBitClicked) {
                    Log.d(TAG, "------subscribeToUpdates()--- onChildChanged");
                    setActiveMarker(dataSnapshot);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    DataSnapshot dataSnapshotGlobal;
    String keyGlobal;
    int i = 1;

    private void setActiveMarker(DataSnapshot dataSnapshot) {
        dataSnapshotGlobal = dataSnapshot;
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once

//        mMap.clear();
//        String key = dataSnapshot.getKey();
        keyGlobal = dataSnapshot.getKey();
//        Log.d(TAG, "setActiveMarker: KEY: " + i + " " + key);
        Log.d(TAG, "setActiveMarker: KEY: " + i + " " + keyGlobal);
//        String key = "";
//        try{
//            for (int j = 0; j < userList.size(); j++) {
//                Log.d(TAG, "INSIDE....setActiveMarker: ..... " + j +", " + userList.get(j).getEmail());
//                if (userList.get(j).getEmail() != null) key = userList.get(j).getEmail();
//            }
//        } catch (Exception e){
//            Log.d(TAG, "INSIDE....setActiveMarker: NULL pointer exception: ..... ");
//        }

        // HERE WHAT CORRESPONDS TO JOIN
        DatabaseReference chatGroupRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(keyGlobal);
        chatGroupRef.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot childDataSnapshot) {
                        // repeat!!
                        Log.d(TAG, "JOIN: ... " + i++ + childDataSnapshot.getValue());
                        User user = childDataSnapshot.getValue(User.class);
                        if (user != null) {
//                            userList.add(user);
                            HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshotGlobal.getValue();
                            Log.d(TAG, i + " setActiveMarker, KEY: " + keyGlobal + ", VALUE: " + value.toString());
                            i++;
//        LatLng location = new LatLng(10, 20);;
//        try{
//            double lat = Double.parseDouble(value.get("latitude").toString());
//            double lng = Double.parseDouble(value.get("longitude").toString());
//            location = new LatLng(lat, lng);
//        } catch (Exception e){
//            Log.d(TAG, "LatLang.... NULL pointer exception ..... ");
//        }

                            double lat = Double.parseDouble(value.get("latitude").toString());
                            double lng = Double.parseDouble(value.get("longitude").toString());
                            LatLng location = new LatLng(lat, lng);

                            // It is notified each time one of the device's location is updated. When this happens, it will either create a new marker at the device's location, or move the marker for a device if it exists already.
                            if (!mMarkers.containsKey(keyGlobal)) {
                                Marker marker = mMap.addMarker(new MarkerOptions().title("" + user.getName() + "").position(location).snippet("dis, time, address, cell, msg"));
                                mMarkers.put(keyGlobal, marker);
                                marker.showInfoWindow();
                            } else {
                                mMarkers.get(keyGlobal).setPosition(location);
                            }
                        }

                        // TODO: DEBUGGER 4/12/2018
//                        try{
//                            Log.d(TAG, "setActiveMarker: ..... " + user.getEmail());
//                        } catch (Exception e){
//                            Log.d(TAG, "EMAIL: NULL pointer exception: ..... ");
//                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );


//        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshot.getValue();
//        Log.d(TAG, i + " setActiveMarker, KEY: " + key + ", VALUE: " + value.toString());
//        i++;
////        LatLng location = new LatLng(10, 20);;
////        try{
////            double lat = Double.parseDouble(value.get("latitude").toString());
////            double lng = Double.parseDouble(value.get("longitude").toString());
////            location = new LatLng(lat, lng);
////        } catch (Exception e){
////            Log.d(TAG, "LatLang.... NULL pointer exception ..... ");
////        }
//
//        double lat = Double.parseDouble(value.get("latitude").toString());
//        double lng = Double.parseDouble(value.get("longitude").toString());
//        LatLng location = new LatLng(lat, lng);
//
//         // It is notified each time one of the device's location is updated. When this happens, it will either create a new marker at the device's location, or move the marker for a device if it exists already.
//        if (!mMarkers.containsKey(key)) {
//            mMarkers.put(key, mMap.addMarker(new MarkerOptions().title("" + key + "").position(location).snippet("dis, time, address, cell, msg")));
//        } else {
//            mMarkers.get(key).setPosition(location);
//        }

        // TODO: 4/13/2018      READ the comment below (mandatory)
        // Motivation from: https://codelabs.developers.google.com/codelabs/realtime-asset-tracking/index.html?index=..%2F..%2Findex#5
        // REMOVING code below brings me what exactly i want!!!
        // Understanding API + framework is highly important.

//        LatLngBounds.Builder builder = new LatLngBounds.Builder();
//        for (Marker marker : mMarkers.values()) {
////            mMap.addMarker(new MarkerOptions().title(key).position(location));
//            builder.include(marker.getPosition());
//        }
//        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
    }

    private boolean controllerBitClicked = false;
    public void onClickMyCircle(View view) {
        Log.d(TAG, "onClickMyCircle: ");
        if (controllerBitClicked) {
            // HIDE LIVE FRIENDS
            controllerBitClicked = false;
            ivMyCircle.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_people_outline_black_24dp));
            Log.d(TAG, "------controllerBitClicked = false;------ run setInactiveMarker();");
            setInactiveMarker();
        } else {
            // SHOW LIVE FRIENDS
            controllerBitClicked = true;
            Log.d(TAG, "------controllerBitClicked = true;------ run subscribeToUpdates();");
            Toast.makeText(getApplicationContext(), "Live Friends", Toast.LENGTH_LONG).show();
            ivMyCircle.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_people_black_24dp));
            subscribeToUpdates();
        }
    }

    private void setInactiveMarker() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path));
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String userId = currentUser.getUid();
        Log.d(TAG, "----------setInactiveMarker: -----------" + userId);
        // Use unique userID during registration time assigned
        ref.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!controllerBitClicked) {
                    updateDisplay(snapshot);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(LOG_TAG, "onCancelled");
            }
        });
    }

    Marker myMarker;
    private void updateDisplay(DataSnapshot dataSnapshot) {
        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshot.getValue();
        double lat = Double.parseDouble(value.get("latitude").toString());
        double lng = Double.parseDouble(value.get("longitude").toString());
        LatLng location = new LatLng(lat, lng);

        if (myMarker != null) {
            myMarker.remove();
        }
//        mMap.clear();
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

    public void onClickNotification(View view) {
        Toast.makeText(getApplicationContext(), "Notification clicked", Toast.LENGTH_SHORT).show();
    }

    /** Called when the user clicks a marker. */
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


    // anonymous method: isNameProvided
    // single event support: https://stackoverflow.com/questions/47105575/android-firebase-stop-childeventlistener
    private void isNameProvided(String currentUser) {
        databaseReference.child("users").child("" + currentUser)
        .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);

                if (dataSnapshot.getChildrenCount() == 2) {
                    // TODO: 4/16/2018      7h later eureka!!! at 2018.Apr15 12.24 pm where started at 5.10 pm
                    // Never give up! Just keep standing...!!!
                    // Now, User will see map first but to interact with the map, user must have to input name
                    // I don't need to change my LoginActivity.java code. Where i will change from DisplayActivity.java code

                    // CODE is READY to CHANGE
                    // User friendly Toast
                    Toast.makeText(getApplicationContext(),
                            "Please input your name, to get better UX. As you are <= v1.6.0 users", Toast.LENGTH_LONG).show();
                    // DEBUGGER
                    Log.d(TAG, "onDataChange: DEBUGGER-----INSIDE isNameProvided = " + false + " ------KEY: "
                            + dataSnapshot.getKey() + ", " + dataSnapshot.getChildrenCount() + ", NAME: " + user.getName());

                    // Take Input through pop up window and save to db
                    // step 1:  MAKE pop up window to take name input
                    // call method
                    popUpForName(dataSnapshot.getKey());
                } else if (dataSnapshot.getChildrenCount() == 3) {
                    Log.d(TAG, "onDataChange: DEBUGGER----- >= v1.7.0 users. So, Name already provide during sign up");
                    Log.d(TAG, "onDataChange: DEBUGGER-----INSIDE isNameProvided ------KEY: " + dataSnapshot.getKey() + ", " + dataSnapshot.getChildrenCount() + ", NAME: " + user.getName());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // SUPPORT from: https://stackoverflow.com/questions/10903754/input-text-dialog-android
    private String name = "";
    // SUPPORT from: https://stackoverflow.com/questions/4134117/edittext-on-a-popup-window
    private void popUpForName(final String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("You name?");

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
                databaseReference.child("users").child(key).child("name").setValue(name);

                // Make sure user
                databaseReference.child("users").child(key)
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
        // TODO SUPPORTED by: https://stackoverflow.com/questions/18077040/android-map-v2-get-marker-position-on-marker-click
        marker.hideInfoWindow();
        lat = marker.getPosition().latitude;
        lang =marker.getPosition().longitude;
        String name = marker.getTitle();
        Toast.makeText(getApplicationContext(), "" + name + "'s Current Location", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(DisplayActivity.this, StreetViewPanoramaBasicDemoActivity.class));
    }

    public static boolean dangerStatus = false;
    // SUPPORTED by: https://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-on-android
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

                    dangerSound.start();
                    // TODO: 4/18/2018: UPDATE db

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

                    // TODO: 4/18/2018: UPDATE db


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
}