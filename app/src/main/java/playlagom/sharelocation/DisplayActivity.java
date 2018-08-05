package playlagom.sharelocation;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import playlagom.sharelocation.auth.LoginActivity;
import playlagom.sharelocation.auth.SignUpActivity;
import playlagom.sharelocation.libs.Converter;
import playlagom.sharelocation.libs.GoogleMapOperations;
import playlagom.sharelocation.models.KeyValue;
import playlagom.sharelocation.models.User;
import playlagom.sharelocation.notification.MyFirebaseInstanceIdService;
import playlagom.sharelocation.notification.SharedPrefManager;

public class DisplayActivity extends FragmentActivity implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnInfoWindowClickListener,
        OnMapReadyCallback, GoogleMap.OnCameraMoveListener {

    public static final String SERVER_KEY = "AAAAzSZNbUY:APA91bE-g_vgALMF4u9mqC2rVbPVi_FkiVtXFi3SiK7ya802mWMLUkIxeatHaxTcZfBnQPacCwJUYQoRXSqA6fBF2vJ_zEsfKruxXdxnTYyuKDgB6uVteHJOumJm5-NLYUqRuyZXq4R7";

    ImageView ivUserImage, ivMyCircle, ivSmallStreetView, ivStreetView, ivNotification;
    public static ImageView ivDanger;
    ImageView ivWave, ivChatEngine, ivCall;
    private static final String RECEIVED_FRIEND_REQUESTS = "receivedFriendRequests";
    private static final String SENT_FRIEND_REQUESTS = "sentFriendRequests";
    private static final String TAG = DisplayActivity.class.getSimpleName();
    public static double myLocationLatitude = 0, myLocationLongitude = 0;
    TextView tvPosition, tvPoint, tvTraffic, tvUber, tvPathao, tvInstruction;
    private static final String LOG_TAG = "DisplayActivity";
    private static final int CALL_PERMISSIONS_REQUEST = 2;
    private static final int PERMISSIONS_REQUEST = 1;
    private HashMap<String, Marker> blueMarkers;
    boolean locationPermissionGranted = false;
    private boolean insideShouldShow = false;
    static boolean taskCompleted = false;
    GoogleMap mMap;

    // Firebase
    FirebaseAuth firebaseAuth;
    DatabaseReference allMarkerRef;
    DatabaseReference databaseReference;
    DatabaseReference dbRefLatestVersion;

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
    // TODO: 5/12/2018 COMPLETE view of SentFriendRequests
    public static LinkedHashMap<String, KeyValue> lhmSentFriendRequests = new LinkedHashMap<>();

    // IMPLEMENT logic to USE less memory through LinkedHashMap
    // NOW using HashMap for completion purpose, where more memory is used
    // FAILED:   HashMap<String, KeyValue> keyValuePosition = new HashMap<>();
    // SUPPORT: https://stackoverflow.com/questions/5237101/is-it-possible-to-get-element-from-hashmap-by-its-position
    // SUPPORT: https://www.tutorialspoint.com/compile_java_online.php
    public static LinkedHashMap<String, KeyValue> linkedHashMap = new LinkedHashMap<>();

    // Retrieve logged in username to use on add friend feature
    public static String loggedInUserName;
    private static double destinationLatitude = 0;
    private static double destinationLongitude = 0;

    // RECEIVE broadcast_token
    private BroadcastReceiver broadcastReceiver;

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

        // RETRIEVE latestVersion from firebase
        dbRefLatestVersion = FirebaseDatabase.getInstance().getReference("latestVersion");

        // CHECK & GET device-token
        if (SharedPrefManager.getInstance(this).getToken() != null) {
            String CURRENT_DEVICE_TOKEN = SharedPrefManager.getInstance(this).getToken();
            Log.d(TAG, "onCreate: device-token: " + CURRENT_DEVICE_TOKEN);
            // UPDATE device-token
            databaseReference
                    .child(firebaseAuth.getCurrentUser().getUid())      // USED HashMap to get clicked marker uid & logged in user uid
                    .child("deviceToken")
                    .setValue(CURRENT_DEVICE_TOKEN);
        }

        // TODO: 5/6/2018 REMOVE method below, at next version
        // COPY name: from sharelocation-users to SL11302018MAY6
        // copyLoggedInUserInfoToNewStructure();

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
        ivDanger.setVisibility(View.GONE);
        ivUserImage = findViewById(R.id.ivUserImage);
        ivMyCircle = findViewById(R.id.ivMyCircle);
        ivSmallStreetView = findViewById(R.id.ivSmallStreetView);
        ivSmallStreetView.setVisibility(View.GONE);
        ivStreetView = findViewById(R.id.ivStreetView);
        ivNotification = findViewById(R.id.ivNotification);
        ivWave = findViewById(R.id.ivWave);
        ivWave.setVisibility(View.GONE);
        ivChatEngine = findViewById(R.id.ivChatEngine);
        ivChatEngine.setVisibility(View.GONE);
        ivCall = findViewById(R.id.ivCall);
        ivCall.setVisibility(View.GONE);

        // TextView
        tvPosition = findViewById(R.id.tvPosition);
        tvPosition.setVisibility(View.GONE);
        tvPoint = findViewById(R.id.tvPoint);
        tvPoint.setVisibility(View.GONE);
        tvTraffic = findViewById(R.id.tvTraffic);
        tvTraffic.setVisibility(View.GONE);
        tvUber = findViewById(R.id.tvUber);
        tvUber.setVisibility(View.GONE);
        tvPathao = findViewById(R.id.tvPathao);
        tvPathao.setVisibility(View.GONE);
        tvInstruction = findViewById(R.id.tvInstruction);

        // Sound
        // SUPPORT: https://stackoverflow.com/questions/18459122/play-sound-on-button-click-android
        dangerSound = MediaPlayer.create(this, R.raw.siren_alert_1);

        // INIT setup
        // ImageView: CHECK danger status and SET icon
        showDangerIcon();

        // ImageView: MAKE round user image
//        ivUserImage.setImageBitmap(Converter.getCroppedBitmap(
//                BitmapFactory.decodeResource(getResources(), R.drawable.current_user)));

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
        MobileAds.initialize(this, "ca-app-pub-6882836186513794~2015541759");

        // ADS: https://developers.google.com/admob/android/banner
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Local cache for markers
        blueMarkers = new HashMap<>();

        checkLocationPermission();
        // Notify friends online status
        notifyFriendsOnline();

        //  Check to install latest app
        checkUpdate();
    }

    private void checkUpdate() {
        dbRefLatestVersion
                .child("sharelocation")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null) {
                            // [ OK ] data reached
                            String serverVersion = "v" + dataSnapshot.getValue().toString();
                            String apkVersion = getString(R.string.version);

                            // Forcefully update latest version
                            if (serverVersion.equals(apkVersion)) {
                                Log.d("MainActivity", "[ OK ] " +
                                        "" + true +
                                        "  latestVersion = " + serverVersion + ", appBuild = " + apkVersion);

                                Toast.makeText(getApplicationContext(), "Everything is Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("Display", "[ OK ] " +
                                        "" + false +
                                        "  latestVersion = " + serverVersion + ", appBuild = " + apkVersion);

                                Toast.makeText(getApplicationContext(), "Please Uninstall then Install latest app", Toast.LENGTH_LONG).show();
                                Thread thread = new Thread( new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Log.d("DisplayActivity", "=== try ====");
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        } finally {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=playlagom.sharelocation")));
                                            finish();
                                        }
                                    }
                                });
                                thread.start();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    public static DataSnapshot tempDataSnapshot;
    private void notifyFriendsOnline() {
        // SEND push notification to friends that you are online
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child(getString(R.string.friends))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null) {
                            int counter = 1;
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                tempDataSnapshot = snapshot;
                                // [ OK ] data reached
                                Log.d(TAG, "[ OK ] ==== onDataChange: " +
                                        "" +counter++ +
                                        ", " + snapshot.toString());

                                // retrieve friend deviceToken
                                databaseReference
                                        .child(snapshot.getKey())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                // [ OK ] data reached
                                                if (dataSnapshot != null) {
                                                    if (dataSnapshot.hasChild(getString(R.string.deviceToken))) {
                                                        // [ OK ] data reached
                                                        Log.d(TAG, "[ OK ] =====token: " + loggedInUserName +
                                                                ", friendKey: " + tempDataSnapshot.getKey() + "" +
                                                                        ", " +
                                                                "deviceToken: " + dataSnapshot.child(getString(R.string.deviceToken)).getValue().toString());
                                                        String deviceToken = dataSnapshot.child(getString(R.string.deviceToken)).getValue().toString();

                                                        sendOnlinePushNotification(deviceToken);
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                            }
                            counter = 0;
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "[ ERROR ] -------- notifyFriendsOnline.onCancelled: " +
                                "" + databaseError.getMessage());
                    }
                });
    }

    private void retrieveLoggedInUserName() {
        databaseReference
                .child(firebaseAuth.getCurrentUser().getUid())
                .child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null) {
                            loggedInUserName = String.valueOf(dataSnapshot.getValue());
                            Log.d(TAG, "[ OK ] -------- logged in user: " + loggedInUserName);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "[ OK ] -------- retrieveLoggedInUserName.onCancelled: " +
                                "" + databaseError.getMessage());
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
                        if (dataSnapshot.hasChild(getString(R.string.danger))) {
                            ivDanger.setVisibility(View.VISIBLE);
                            if (dataSnapshot.child(getString(R.string.danger)).getValue().equals("1")) {
                                // SET run danger icon
                                ivDanger.setImageBitmap(Converter.getCroppedBitmap(
                                        BitmapFactory.decodeResource(getResources(), R.drawable.baseline_directions_run_black_18dp)));
                                Toast.makeText(getApplicationContext(), "Your status in DANGER",
                                        Toast.LENGTH_LONG).show();
                                dangerStatus = true;
                            } else {
                                // SET default danger icon
                                ivDanger.setImageBitmap(Converter.getCroppedBitmap(
                                        BitmapFactory.decodeResource(getResources(), R.drawable.baseline_local_parking_black_18dp)));
                                dangerStatus = false;
                            }
                        } else {
                            databaseReference
                                    .child(firebaseAuth.getCurrentUser().getUid())
                                    .child(getString(R.string.danger))
                                    .setValue("0");
                            // SET default danger icon
                            ivDanger.setImageBitmap(Converter.getCroppedBitmap(
                                    BitmapFactory.decodeResource(getResources(), R.drawable.baseline_local_parking_black_18dp)));
                            dangerStatus = false;
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    // SUPPORT: https://stackoverflow.com/questions/15368028/getting-a-map-marker-by-its-id-in-google-maps-v2
    private HashMap<String, KeyValue> hashMapMidUid = new HashMap<String, KeyValue>();
    Marker tempBlueMarker = null;

    private void renderAll() {
        // SHOW all users black/blue marker icon
        allMarkerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "[ OK ] ---- TOTAL REGISTERED USER: " +
                        "" + dataSnapshot.getChildrenCount() +
                        ", renderAll().onDataChange: " +
                        "" + dataSnapshot.toString());

                User temporaryUser;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    temporaryUser = snapshot.getValue(User.class);
                    String uniqueID = snapshot.getKey();

                    // STORE at object to phone call as well as to retrieve markerId
                    KeyValue keyValue = new KeyValue();
                    keyValue.key = uniqueID;
                    keyValue.value = temporaryUser.getPhone();

                    try {
                        if (temporaryUser != null) {
                            double lat = Double.parseDouble(temporaryUser.getPosition().getLatitude());
                            double lang = Double.parseDouble(temporaryUser.getPosition().getLongitude());
                            location = new LatLng(lat, lang);
                            Log.d(TAG, "[ OK ] ---- LOOP: renderAll() .. " + userCounter++ +
                                    " uid: " + uniqueID + ", phone: " + temporaryUser.getPhone() + ", " + location);

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
                                hashMapMidUid.put(tempBlueMarker.getId(), keyValue);
                            } else {
                                blueMarkers.get(uniqueID).setPosition(location);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[ ERROR ] ---- renderAll().onDataChange: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
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

    private void renderFriends() {
        // RETRIEVE and RENDER friends
        databaseReference.child(firebaseAuth.getCurrentUser().getUid())
                .child(getString(R.string.friends))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Log.d(TAG, "[ OK ] - ----- TOTAL FRIENDS: " +
                                "" + dataSnapshot.getChildrenCount() +
                                ", renderFriends().onDataChange: KEY: " +
                                "" + dataSnapshot.getKey());

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Log.d(TAG, "[ OK ] - ----- KEY: " +
                                    "" + snapshot.getKey());

                            databaseReference
                                    .child(snapshot.getKey())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Log.d(TAG, "[ OK ] -----------debug onDataChange: KEY: " + dataSnapshot.getKey() + "" +
                                                    ", name: " + dataSnapshot.child("name").getValue());
                                            try {
                                                User temporaryUser = dataSnapshot.getValue(User.class);
                                                String uniqueID = dataSnapshot.getKey();

                                                if (temporaryUser != null) {
                                                    double lat = Double.parseDouble(temporaryUser.getPosition().getLatitude());
                                                    double lang = Double.parseDouble(temporaryUser.getPosition().getLongitude());
                                                    KeyValue keyValue = new KeyValue();
                                                    keyValue.key = uniqueID;
                                                    keyValue.value = temporaryUser.getPhone();
                                                    location = new LatLng(lat, lang);
                                                    Log.d(TAG, "[ OK ] --- - LOOP: renderFriends() .. " + userCounter++ +
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
                                                        hashMapMidUid.put(tempBlueMarker.getId(), keyValue);
                                                    } else {
                                                        blueMarkers.get(uniqueID).setPosition(location);
                                                    }
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "[ ERROR ] ---- renderFriends().onDataChange: " + e.getMessage());
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
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
            isNameProvided();
            isPhoneProvided();
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

        // SUPPORT: https://stackoverflow.com/questions/40125931/how-to-ask-permission-to-make-phone-call-from-android-from-android-version-marsh
        // switch case support
        switch (requestCode) {
            case CALL_PERMISSIONS_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + tempPhoneNumber));
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Please, Enable phone call permission", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case PERMISSIONS_REQUEST: {
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
            renderAll();
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

        // SUPPORT: https://developers.google.com/maps/documentation/android-api/infowindows
        // SET a listener for info window events.
        mMap.setOnInfoWindowClickListener(this);
        // SET marker click event lister
        mMap.setOnMarkerClickListener(this);
        // SET camera change listener
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                // REMOVE all widget from memory
                ivWave.setVisibility(View.GONE);
                ivChatEngine.setVisibility(View.GONE);
                ivCall.setVisibility(View.GONE);
                ivSmallStreetView.setVisibility(View.GONE);

                tvUber.setVisibility(View.GONE);
                tvPathao.setVisibility(View.GONE);
                tvTraffic.setVisibility(View.GONE);
                tvInstruction.setVisibility(View.GONE);
            }
        });
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

                GoogleMapOperations.setMargins(tvPoint, width / 2, height / 2, 0, 0);
                // DEBUGGER: tvPosition.setText(" " + height + ", " + width);
                // tvPosition.setText("" + latLng.latitude + ", " + latLng.longitude);

                lat = latLng.latitude;
                lang = latLng.longitude;

                if (onMarkerClicked) {
                    Toast.makeText(getApplicationContext(),
                            "SEE traffic, distance, time, cost", Toast.LENGTH_LONG).show();

                    String imageURL = "https://maps.googleapis.com/maps/api/streetview?size=600x400&location=" +
                            "" + destinationLatitude +
                            "," + destinationLongitude +
                            "&fov=90&heading=235&pitch=10";

                    ivSmallStreetView.setVisibility(View.VISIBLE);
                    // SUPPORT 1: https://stackoverflow.com/questions/27024965/how-to-display-a-streetview-preview
                    // SUPPORT 2: http://square.github.io/picasso/
                    // Picasso: A powerful image downloading and caching library for Android
                    // TODO: 5/14/2018 MAKE resizable, movable round/square image like messenger
                    Picasso.get().load(imageURL).into(ivSmallStreetView);
                    // MAKE visible
                    ivWave.setVisibility(View.VISIBLE);
                    ivChatEngine.setVisibility(View.VISIBLE);
                    ivCall.setVisibility(View.VISIBLE);

                    tvUber.setVisibility(View.VISIBLE);
                    tvPathao.setVisibility(View.VISIBLE);
                    tvTraffic.setVisibility(View.VISIBLE);
                    tvInstruction.setVisibility(View.VISIBLE);
                    onMarkerClicked = false;
                } else if (blackBoxClicked) {
                    String imageURL = "https://maps.googleapis.com/maps/api/streetview?size=600x400&location=" +
                            "" + lat +
                            "," + lang +
                            "&fov=90&heading=235&pitch=10";

                    ivSmallStreetView.setVisibility(View.VISIBLE);
                    // SUPPORT 1: https://stackoverflow.com/questions/27024965/how-to-display-a-streetview-preview
                    // SUPPORT 2: http://square.github.io/picasso/
                    // Picasso: A powerful image downloading and caching library for Android
                    // TODO: 5/14/2018 MAKE resizable, movable round/square image like messenger
                    Picasso.get().load(imageURL).into(ivSmallStreetView);

                    destinationLatitude = latLng.latitude;
                    destinationLongitude = latLng.longitude;

                    tvUber.setVisibility(View.VISIBLE);
                    tvPathao.setVisibility(View.VISIBLE);
                    tvTraffic.setVisibility(View.VISIBLE);
                    tvInstruction.setVisibility(View.VISIBLE);
                    tvInstruction.setText("Go There");
                }
            }
        });

        // SUPPORT: https://www.youtube.com/watch?v=hS7EFdDLjas
        // zoom control: plus | minus button by default android sdk
        // mMap.getUiSettings().setZoomControlsEnabled(true);

        // SUPPORT: https://stackoverflow.com/questions/30430664/how-to-hide-navigation-and-gps-pointer-buttons-when-i-click-the-marker-on-th/30431024
        // Disable Map Toolbar
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "My Location", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
//        myLocationLatitude = mMap.getMyLocation().getLatitude();
//        myLocationLangitude = mMap.getMyLocation().getLongitude();
        return false;
    }

    // SUPPORT: https://codelabs.developers.google.com/codelabs/realtime-asset-tracking/index.html?index=..%2F..%2Findex#5
    // subscribing to updates in Firebase and
    // when an update occurs.
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

    // UPDATE marker position & color
    // SUPPORT: https://codelabs.developers.google.com/codelabs/realtime-asset-tracking/index.html?index=..%2F..%2Findex#5
    private void showMarker(DataSnapshot dataSnapshot) {
        try {
            // CASE 1: HANDLE null value(as real-time system) through try catch
            User showMarkerTempUser = dataSnapshot.getValue(User.class);
            Log.d(TAG, "[ OK ] -- showMarker: name: " + showMarkerTempUser.getName());
            String showMarkerTempUID = dataSnapshot.getKey();

            // CASE 2: CHECK value
            if (showMarkerTempUser != null) {
                // CASE 3: CHECK and SET marker green/blue as user online/offline
                showOnlineOfflineStatus(showMarkerTempUser, showMarkerTempUID, dataSnapshot);

                // CASE 4: CHECK and SET animated marker as user at danger
                showAnimatedMarkerAtDanger(showMarkerTempUser, showMarkerTempUID, dataSnapshot);
            } else {
                Log.e(TAG, "[ ERROR ] -- NULL USER, showMarker()");
            }
        } catch (Exception e) {
            Log.e(TAG, "[ ERROR ] -- showMarker(): " + e.getMessage());
        }
    }

    String dangerReason;
    private void showAnimatedMarkerAtDanger(User showMarkerTempUser, String showMarkerTempUID, DataSnapshot dataSnapshot) {
        if (showMarkerTempUser.getDanger() != null) {
            String danger = showMarkerTempUser.getDanger();
            if (danger.equals("1")) {
                if (dataSnapshot.hasChild(getString(R.string.dangerReason))) {
                     dangerReason = dataSnapshot.child(getString(R.string.dangerReason)).getValue().toString();
                }
                try {
                    // REMOVED frequently focused danger markers
//                    // SUPPORT: https://stackoverflow.com/questions/22202299/how-do-i-remove-all-radius-circles-from-google-map-android-but-keep-pegs
//                    // CLEAR red circles
//                    for (Circle myCircle : GoogleMapOperations.circleList) {
//                        myCircle.remove();
//                    }
//                    GoogleMapOperations.circleList.clear();

//                    double lat = Double.parseDouble(showMarkerTempUser.getPosition().getLatitude());
//                    double lang = Double.parseDouble(showMarkerTempUser.getPosition().getLongitude());
//                    location = new LatLng(lat, lang);

//                    // SUPPORT: https://www.youtube.com/watch?v=hS7EFdDLjas
//                    // animation
//                    GoogleMapOperations.addingCircleView(mMap, location);
//                    Log.d(TAG, "[ OK ] -- showAnimatedMarkerAtDanger: latlang: " + location.toString());

                    // It is notified each time one of the device's location is updated.
                    // When this happens, it will either create a new marker at the device's location,
                    // or move the marker for a device if it exists already.
                    if (blueMarkers.containsKey(showMarkerTempUID)) {
                        Log.d(TAG, "[ OK ] -- showAnimatedMarkerAtDanger: CHANGE color as user already exits at map");
                        blueMarkers.get(showMarkerTempUID).setTitle(showMarkerTempUser.getName());
                        if (dangerReason != null) {
                            blueMarkers.get(showMarkerTempUID).setSnippet(dangerReason);
                        }
                        blueMarkers.get(showMarkerTempUID).setIcon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED       // SET live user marker green
                                )
                        );
                        blueMarkers.get(showMarkerTempUID).setPosition(location);
                        blueMarkers.get(showMarkerTempUID).showInfoWindow();
                    }
                } catch (Exception e) {
                    Log.d(TAG, "[ ERROR ] -- showAnimatedMarkerAtDanger: " + e.getMessage());
                }

                // START danger sound
                if (makeDangerSound) {
                    makeDangerSound = false;
                    dangerSound.start();
                    Toast.makeText(getApplicationContext(), "NEED help!\n" + showMarkerTempUser.getName()
                            + " is in DANGER", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // TO make offline: close app -> tab notification to stop service -> clear app running history
    private void showOnlineOfflineStatus(User currentUser, String currentUID, DataSnapshot dataSnapshot) {
        if (currentUser.getOnline() != null) {
            if (currentUser.getOnline().equals("1")) {
                Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: online = 1");
                try {
                    double lat = Double.parseDouble(currentUser.getPosition().getLatitude());
                    double lang = Double.parseDouble(currentUser.getPosition().getLongitude());
                    location = new LatLng(lat, lang);
                    Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: latlang: " + location.toString());

                    // It is notified each time one of the device's location is updated.
                    // When this happens, it will either create a new marker at the device's location,
                    // or move the marker for a device if it exists already.
                    if (blueMarkers.containsKey(currentUID)) {
                        Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: CHANGE color as user already exits at map");

                        blueMarkers.get(currentUID).setTitle(currentUser.getName());
                        blueMarkers.get(currentUID).setSnippet("track & call");
                        blueMarkers.get(currentUID).setIcon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_GREEN       // SET live user marker green
                                )
                        );
                        blueMarkers.get(currentUID).setPosition(location);
                        blueMarkers.get(currentUID).showInfoWindow();
                    } else if (!blueMarkers.containsKey(currentUID)) {
                        Marker marker = mMap.addMarker(new MarkerOptions().title("" + currentUser.getName() + "")
                                .position(location)
                                .snippet("track & call")
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED       // SET live users marker green
                                )));
                        blueMarkers.put(currentUID, marker);
                        marker.showInfoWindow();
                    }
                } catch (Exception e) {
                    Log.d(TAG, "[ ERROR ] -- showOnlineOfflineStatus: " + e.getMessage());
                }
            } else if (currentUser.getOnline().equals("0")) {
                Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: online = 0");
                Log.d(TAG, "[ OK ] -- showOnlineOfflineStatus: DISCONNECTING USER...");
                blueMarkers.get(currentUID).setTitle(currentUser.getName());
                blueMarkers.get(currentUID).setSnippet("track & call");
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

    // DECLARE shared marker: when click event occurred at marker
    Marker clickedMarker;
    // logic var for user friendly presentation
    static boolean onMarkerClicked = false;
    // shared variable
    private float zoomValue = 0;

    @Override
    public void onCameraMove() {
        zoomValue = mMap.getCameraPosition().zoom;
    }

    /** Called when the user clicks on a marker. */
    @Override
    public boolean onMarkerClick(Marker marker) {
        clickedMarker = marker;
        onMarkerClicked = true;

        blackBoxClicked = false;
        ivStreetView.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_crop_original_black_24dp)));

        // Update destination position for uber + traffic
        destinationLatitude = marker.getPosition().latitude;
        destinationLongitude = marker.getPosition().longitude;
        tvInstruction.setText(marker.getTitle());

        tvPoint.setVisibility(View.GONE);
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
                        .addListenerForSingleValueEvent(new ValueEventListener() {
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

    private void isPhoneProvided() {
        Log.d(TAG, "[ OK ] isPhoneProvided: ");
        databaseReference.child(firebaseAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.hasChild("phone")) {
                            // MAKE pop up window to take name
                            popUpForPhone(dataSnapshot.getKey());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    // SUPPORT: https://stackoverflow.com/questions/10903754/input-text-dialog-android
    // SUPPORT: https://stackoverflow.com/questions/4134117/edittext-on-a-popup-window
    private void popUpForPhone(final String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add your phone number");

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
                final String phone = input.getText().toString();
                Toast.makeText(getApplicationContext(), "Thank you! ", Toast.LENGTH_SHORT).show();
                final String msg = "Congratulation! Your phone no. added";

                // STORE name
                databaseReference.child(key).child("phone").setValue(phone)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Write was successful!
                                // NOTIFY user
                                Toast.makeText(getApplicationContext(), "" + msg, Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Write failed
                                Toast.makeText(getApplicationContext(), "Failed adding phone..try again", Toast.LENGTH_LONG).show();
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

    public String tempPhoneNumber;
    @Override
    public void onInfoWindowClick(final Marker marker) {
        // SUPPORT: https://stackoverflow.com/questions/18077040/android-map-v2-get-marker-position-on-marker-click
        marker.hideInfoWindow();

        // GET marker obj info
        KeyValue keyValue = hashMapMidUid.get(marker.getId());
        // GET name
        final String userName = marker.getTitle();
        // GET (MarkerID vs UID) from RAM
        final String markerUID = keyValue.key;
        // GET loggedIn UID
        final String UID = firebaseAuth.getCurrentUser().getUid();

        // SUPPORT: https://www.mkyong.com/android/android-custom-dialog-example/
        // ADD custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.marker_click_dialog);

        // WIRE widgets
        TextView tvFriendRequestStatus = dialog.findViewById(R.id.tvFriendRequestStatus);
        final Button btnAddFriend = dialog.findViewById(R.id.btnAddFriend);
        TextView tvName = dialog.findViewById(R.id.tvName);

        // INIT widgets values
        // TextView
        tvFriendRequestStatus.setVisibility(View.GONE);
        tvName.setText(userName);

        if (markerUID.equals(UID)) {    // CHECK clicked to self or not
            tvFriendRequestStatus.setVisibility(View.VISIBLE);
            tvFriendRequestStatus.setText("Cool! It's You.");
            btnAddFriend.setVisibility(View.GONE);
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
                                    // SUPPORT: https://stackoverflow.com/questions/4384890/how-to-disable-an-android-button
                                    btnAddFriend.setEnabled(false);
                                    Toast.makeText(getApplicationContext(), "Start call/chat",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    // CHECK isExist at RECEIVED_FRIEND_REQUESTS
                                    databaseReference
                                            .child(UID)
                                                .child(getString(R.string.receivedFriendRequests))
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if (dataSnapshot != null) {
                                                if (dataSnapshot.hasChild(markerUID)) {
                                                    btnAddFriend.setText("CHECK Friend Requests");
                                                    btnAddFriend.setEnabled(false);
                                                    Toast.makeText(getApplicationContext(),
                                                            "This user want to be your friend",
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
                                                                            Toast.makeText(getApplicationContext(),
                                                                                    "Please wait for acceptance",
                                                                                    Toast.LENGTH_LONG).show();
                                                                            btnAddFriend.setEnabled(false);
                                                                        } else if (dataSnapshot.getValue().toString().equals("1")) {
                                                                            btnAddFriend.setText("Already Friend");
                                                                            btnAddFriend.setEnabled(false);
                                                                            Toast.makeText(
                                                                                    getApplicationContext(),
                                                                                    "Start chat/call",
                                                                                    Toast.LENGTH_LONG).show();
                                                                        }
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(DatabaseError databaseError) {
                                                                    Log.e(TAG, "[ ERROR ] ---- onCancelled: "
                                                                            + databaseError.getMessage());
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

        // HANDLE add friend event
        btnAddFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                databaseReference
                        .child(markerUID)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot != null) {
                                        if (dataSnapshot.hasChild("deviceToken")) {
                                            String TARGET_DEVICE_TOKEN = dataSnapshot.child("deviceToken").getValue().toString();
                                            Log.d(TAG, "deviceTokenCHECK: " + TARGET_DEVICE_TOKEN);
                                            // PUSH NOTIFICATION: friend request sent
                                            sendFriendRequestPushNotification(TARGET_DEVICE_TOKEN);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
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

    private boolean checkPhoneCallPermission() {

        int permission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CALL_PHONE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.CALL_PHONE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CALL_PHONE},
                        CALL_PERMISSIONS_REQUEST);
                insideShouldShow = true;
            } else {
                // when user check "Don't show me again then this part"
                insideShouldShow = false;
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CALL_PHONE},
                        CALL_PERMISSIONS_REQUEST);
            }
            return false;
        }
    }

    // SUPPORT: https://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-on-android
    public static boolean dangerStatus = false;
    public void onClickDanger(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // SET danger control: Logic
        if (!dangerStatus) {
            startActivity(new Intent(DisplayActivity.this, Danger.class));
        } else if (dangerStatus){
            builder.setMessage("Are you safe?");
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dangerStatus = false;
                    Toast.makeText(getApplicationContext(), "Nice, you are safe now.", Toast.LENGTH_LONG).show();
                    ivDanger.setImageBitmap(BitmapFactory
                            .decodeResource(getResources(), R.drawable.baseline_local_parking_black_18dp));
                    databaseReference.child("" + firebaseAuth.getCurrentUser().getUid()).child("danger").setValue("0");
                    dialog.dismiss();
                }
            });

            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(),
                            "All watching, you're in DANGER!", Toast.LENGTH_LONG).show();
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
        if (!isLogoutSuccessful) {
            lhmReceivedFriendRequests.clear();
            lhmFriends.clear();
            databaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("online").onDisconnect().setValue("0");
        }
    }

    boolean blackBoxClicked = false;
    public void onClickStreetView(View view) {
        // REMOVE from memory
        // preview, wave, chatengine, call
        ivSmallStreetView.setVisibility(View.GONE);
        ivWave.setVisibility(View.GONE);
        ivChatEngine.setVisibility(View.GONE);
        ivCall.setVisibility(View.GONE);
        // uber, traffic, instruction
        tvTraffic.setVisibility(View.GONE);
        tvUber.setVisibility(View.GONE);
        tvPathao.setVisibility(View.GONE);
        tvInstruction.setVisibility(View.GONE);

        onMarkerClicked = false;

        if (!blackBoxClicked) {
            blackBoxClicked = true;
            // SUPPORT: https://stackoverflow.com/questions/5756136/how-to-hide-a-view-programmatically
            tvPoint.setVisibility(View.VISIBLE);

            Toast.makeText(getApplicationContext(), "Swipe map to see 360 deg image", Toast.LENGTH_SHORT).show();
            ivStreetView.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_style_black_24dp)));
        } else {
            blackBoxClicked = false;
            tvPoint.setVisibility(View.GONE);

            Toast.makeText(getApplicationContext(), "Picture mode disabled", Toast.LENGTH_SHORT).show();
            ivStreetView.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_crop_original_black_24dp)));
        }
    }

    static boolean friendsOnlyIconClicked = false;
    public void onClickMyCircle(View view) {
        Log.d(TAG, "[ OK ] --- onClickMyCircle()");

        // CLEAR markers from cache & map
        for (Marker CurrMarker : blueMarkers.values()) {
            // SUPPORT: https://stackoverflow.com/questions/13692398/remove-a-marker-from-a-googlemap
            CurrMarker.remove();
        }
        blueMarkers.clear();

        // DRAW markers
        if (!friendsOnlyIconClicked){
            renderFriends();            // RENDER friends
            friendsOnlyIconClicked = true;
            ivMyCircle.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_people_black_24dp));
            Toast.makeText(getApplicationContext(), "My Friends",
                    Toast.LENGTH_SHORT).show();
        } else {
            renderAll();                // RENDER all
            friendsOnlyIconClicked = false;
            ivMyCircle.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_people_outline_black_24dp));
            Toast.makeText(getApplicationContext(), "All", Toast.LENGTH_LONG).show();
        }
    }

    public static boolean hideMeClicked = true;
    @SuppressLint("Range")
    public void onClickUserImage(View view) {
        if (hideMeClicked) {
            ivUserImage.setAlpha((float) 0.7);
            hideMeClicked = false;
            Toast.makeText(getApplicationContext(), "HIDE ME (under construction)", Toast.LENGTH_SHORT).show();
        } else {
            ivUserImage.setAlpha((float) 200);
            ivUserImage.setImageBitmap(BitmapFactory
                    .decodeResource(getResources(), R.drawable.baseline_person_pin_black_18dp));
            hideMeClicked = true;
            Toast.makeText(getApplicationContext(), "By default public", Toast.LENGTH_SHORT).show();
        }
    }
    static boolean isLogoutSuccessful = false;
    public void onClickLogout(View view) {

        // REQUESTED FEATURE: ADD dialog for logout confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure to logout?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                isLogoutSuccessful = true;
                lhmReceivedFriendRequests.clear();
                lhmFriends.clear();
                databaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("online").onDisconnect().setValue("0");

                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getApplicationContext(), "Successfully Logout", Toast.LENGTH_LONG).show();
                dialog.dismiss();
                startActivity(new Intent(DisplayActivity.this, LoginActivity.class));
                finish();
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Whoops! It's OK", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
    public void onClickNotification(View view) {
        Toast.makeText(getApplicationContext(), "Notifications", Toast.LENGTH_SHORT).show();

//        startActivity(new Intent(DisplayActivity.this, ProductListActivity.class));
        startActivity(new Intent(DisplayActivity.this, TabNPageViewerActivity.class));
        // SUPPORT: https://www.androidcode.ninja/android-alertdialog-example/
        // SUPPORT: https://developer.android.com/guide/topics/ui/dialogs
    }

    public void onClickSmallStreetView(View view) {
        // IMPLEMENT game style touch event
        if (onMarkerClicked) {
            Toast.makeText(getApplicationContext(), "" +
                    "" + clickedMarker.getTitle() + "'s Current Location", Toast.LENGTH_SHORT).show();
        }
        startActivity(new Intent(DisplayActivity.this, StreetViewPanoramaBasicDemoActivity.class));
    }

    // HANDLE event by listener
    public void onClickUber(View view) {
        // SUPPORT: https://stackoverflow.com/questions/6205827/how-to-open-standard-google-map-application-from-my-application
        // SUPPORT: https://developers.google.com/maps/documentation/urls/android-intents

        // SUPPORT: https://stackoverflow.com/questions/35913628/open-uber-app-from-my-app-android
        // Opens Source UBER: https://github.com/uber
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES);

            String uri = String.format(Locale.ENGLISH, "geo:%f,%f", destinationLatitude,
                destinationLongitude);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.ubercab");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        } catch (PackageManager.NameNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ubercab")));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.ubercab")));
            }
        }
    }

    // HANDLE event by listener
    public void onClickTraffic(View view) {
        // SUPPORT: https://stackoverflow.com/questions/6205827/how-to-open-standard-google-map-application-from-my-application
        // SUPPORT: https://developers.google.com/maps/documentation/urls/android-intents

        String uri = "http://maps.google.com/maps?saddr=" + myLocationLatitude + "," + myLocationLongitude + "&daddr=" + destinationLatitude + "," + destinationLongitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
    public void onClickWave(View view) {
        // debugger
        // Toast.makeText(getApplicationContext(), "Wave clicked", Toast.LENGTH_SHORT).show();

        // GET marker obj info
        String userName = clickedMarker.getTitle();
        // GET markerID (MarkerID vs UID) from RAM
        KeyValue keyValue = hashMapMidUid.get(clickedMarker.getId());
        final String markerUID = keyValue.key;
        // GET logged in user unique key, who click onto marker
        final String UID = firebaseAuth.getCurrentUser().getUid();

        // debugger
        // Toast.makeText(getApplicationContext(), "" + markerUID, Toast.LENGTH_LONG).show();

        // CHECK and SET wave to receiver
        databaseReference
            .child(markerUID)
                .child(getString(R.string.friends))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // CHECK senderIsExistAtFriends
                        if (isFriend(dataSnapshot, UID)) {
                            // CHECK device-token exist or not
                            databaseReference
                                    .child(markerUID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if (dataSnapshot != null) {
                                                if (dataSnapshot.hasChild("deviceToken")) {
                                                    String TARGET_DEVICE_TOKEN = dataSnapshot.child("deviceToken").getValue().toString();
                                                    Log.d(TAG, "deviceTokenCHECK: " + TARGET_DEVICE_TOKEN);
                                                    // PUSH NOTIFICATION: wave push notification
                                                    sendWavePushNotification(TARGET_DEVICE_TOKEN);

                                                    // debugger
                                                    // Toast.makeText(getApplicationContext(), "Yes friends", Toast.LENGTH_LONG).show();

                                                    // SET sender wave with a completion listener: To know when a write operation has completed
                                                    // SUPPORT: https://stackoverflow.com/questions/41403085/how-to-check-if-writing-task-was-successful-in-firebase

                                                    // Add a Completion Callback
                                                    // SUPPORT: https://firebase.google.com/docs/database/android/read-and-write#updating_or_deleting_data
                                                    databaseReference
                                                            .child(markerUID)
                                                            .child(getString(R.string.friends))
                                                            .child(UID)
                                                            .child("wave")
                                                            .setValue("1")
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    // Write was successful!
                                                                    Toast.makeText(getApplicationContext(),
                                                                            "Wave Sent Successful", Toast.LENGTH_LONG).show();
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    // Write failed
                                                                    Toast.makeText(getApplicationContext(),
                                                                            "Failed sending..try again", Toast.LENGTH_LONG).show();
                                                                    Log.e(TAG, "[ ERROR ] ---write failed--- onClickWave: " + e.getMessage());
                                                                }
                                                            });
                                                } else {
                                                    // Write failed
                                                    Toast.makeText(getApplicationContext(),
                                                            "Failed sending... To get this service, call user to install latest version of the app", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                        } else {
                            Toast.makeText(getApplicationContext(), "Be Friend First", Toast.LENGTH_LONG).show();
                            // CHECK device-token exist or not
                            databaseReference
                                    .child(markerUID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if (dataSnapshot != null) {
                                                if (dataSnapshot.hasChild("deviceToken")) {
                                                    String TARGET_DEVICE_TOKEN = dataSnapshot.child("deviceToken").getValue().toString();
                                                    Log.d(TAG, "deviceTokenCHECK: " + TARGET_DEVICE_TOKEN);
                                                    // PUSH NOTIFICATION: wave push notification
                                                    wantToBeFriendPushNotification(TARGET_DEVICE_TOKEN);
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private boolean isFriend(DataSnapshot dataSnapshot, String UID) {
        if (dataSnapshot.hasChild(UID)) {
            return true;
        } else {
            return false;
        }
    }

    // HANDLE call event
    @SuppressLint("MissingPermission")
    public void onClickCall(View view) {
        KeyValue keyValue = hashMapMidUid.get(clickedMarker.getId());

        // GET (MarkerID vs UID) from RAM
        final String markerUID = keyValue.key;
        // GET phoneNumber
        final String phone = keyValue.value;
        tempPhoneNumber = phone;
        // GET loggedin UID
        final String UID = firebaseAuth.getCurrentUser().getUid();

        if (UID.equals(markerUID)) {
            Toast.makeText(getApplicationContext(), "It's You! Try another", Toast.LENGTH_SHORT).show();
        } else {
            // Be friend first
            databaseReference
                .child(markerUID)
                    .child(getString(R.string.friends))
                        .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (isFriend(dataSnapshot, UID) || !isFriend(dataSnapshot, UID)) {
                                    // CHECK permission
                                    if (checkPhoneCallPermission()) {
                                        if (phone == null) {
                                            Toast.makeText(getApplicationContext(),
                                                    "Tell " + clickedMarker.getTitle() + " to input cell no.",
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(),
                                                    "Calling..." + clickedMarker.getTitle(), Toast.LENGTH_SHORT).show();
                                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                                            callIntent.setData(Uri.parse("tel:" + phone));
                                            startActivity(callIntent);
                                        }
                                    } else {
                                        Toast.makeText(getApplicationContext(),
                                                "Enable phone call permission", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "Be Friend First", Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
        }
    }

    // HANDLE chatengine event
    public void onClickChatEngine(View view) {
        Toast.makeText(getApplicationContext(), "Developers working for ChatEngine", Toast.LENGTH_SHORT).show();
    }
    public void onClickPathao(View view) {
        // SUPPORT: https://stackoverflow.com/questions/6205827/how-to-open-standard-google-map-application-from-my-application
        // SUPPORT: https://developers.google.com/maps/documentation/urls/android-intents

        // SUPPORT: https://stackoverflow.com/questions/35913628/open-uber-app-from-my-app-android
        // Opens Source UBER: https://github.com/uber
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.pathao.user", PackageManager.GET_ACTIVITIES);

            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.pathao.user");
            if (launchIntent != null) {
                startActivity(launchIntent);//null pointer check in case package name was not found
            }
        } catch (PackageManager.NameNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.pathao.user")));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.pathao.user")));
            }
        }
    }

    // src: https://stackoverflow.com/questions/39068722/post-ing-json-request-to-fcm-server-isnt-working
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();
    Call post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .addHeader("Content-Type","application/json")
                .addHeader("Authorization","key=" + SERVER_KEY)
                .url(url)
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    private void sendOnlinePushNotification(String deviceToken) {
        Log.d(TAG, "sendOnlinePushNotification: data: " + deviceToken);

        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject param = new JSONObject();
            param.put("body", loggedInUserName + " is active now");
            param.put("title", "Tab to see live location");
            param.put("sound", "default");
            jsonObject.put("notification", param);
            jsonObject.put("to", deviceToken);
            post("https://fcm.googleapis.com/fcm/send", jsonObject.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            //Something went wrong
                            Log.d("test", "onFailure: FAILED........");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseStr = response.body().string();
                                Log.d("Response", responseStr);
                                // Do what you want to do with the response.
                            } else {
                                // Request not successful
                            }
                        }
                    }
            );
        } catch (JSONException ex) {
            Log.d("Exception", "JSON exception", ex);
        }
    }

    private void sendFriendRequestPushNotification(String deviceToken) {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject param = new JSONObject();
            param.put("body", loggedInUserName + " sent you a friend request");
            param.put("title", "Connect people, reach people");
            param.put("sound", "default");
            jsonObject.put("notification", param);
            jsonObject.put("to", deviceToken);
            post("https://fcm.googleapis.com/fcm/send", jsonObject.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            //Something went wrong
                            Log.d("test", "onFailure: FAILED........");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseStr = response.body().string();
                                Log.d("Response", responseStr);
                                // Do what you want to do with the response.
                            } else {
                                // Request not successful
                            }
                        }
                    }
            );
        } catch (JSONException ex) {
            Log.d("Exception", "JSON exception", ex);
        }
    }
    private void sendWavePushNotification(String deviceToken) {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject param = new JSONObject();
            param.put("body", loggedInUserName + " missing you!");
            param.put("title", "Tap to see live location");
            param.put("sound", "default");
            jsonObject.put("notification", param);
            jsonObject.put("to", deviceToken);
            post("https://fcm.googleapis.com/fcm/send", jsonObject.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            //Something went wrong
                            Log.d("test", "onFailure: FAILED........");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseStr = response.body().string();
                                Log.d("Response", responseStr);
                                // Do what you want to do with the response.
                            } else {
                                // Request not successful
                            }
                        }
                    }
            );
        } catch (JSONException ex) {
            Log.d("Exception", "JSON exception", ex);
        }
    }
    private void wantToBeFriendPushNotification(String deviceToken) {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject param = new JSONObject();
            param.put("body", loggedInUserName + " wants to be your friend");
            param.put("title", "Tap to find on the map");
            param.put("sound", "default");
            jsonObject.put("notification", param);
            jsonObject.put("to", deviceToken);
            post("https://fcm.googleapis.com/fcm/send", jsonObject.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            //Something went wrong
                            Log.d("test", "onFailure: FAILED........");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseStr = response.body().string();
                                Log.d("Response", responseStr);
                                // Do what you want to do with the response.
                            } else {
                                // Request not successful
                            }
                        }
                    }
            );
        } catch (JSONException ex) {
            Log.d("Exception", "JSON exception", ex);
        }
    }

    // event handle: fb icon click
    public void onClickFB(View view) {
        Toast.makeText(getApplicationContext(), "Like page to get update", Toast.LENGTH_LONG).show();
        startActivity(getOpenFacebookIntent(DisplayActivity.this));
    }

    // SUPPORT: https://stackoverflow.com/questions/4810803/open-facebook-page-from-android-app
    public static Intent getOpenFacebookIntent(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            // SUPPORT: https://support.wix.com/en/article/accessing-your-facebook-business-page-id
            return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/111135596434840"));
        } catch (Exception e) {
            return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/playlagom"));
        }
    }

}