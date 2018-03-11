package playlagom.sharelocation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

import playlagom.sharelocation.auth.LoginActivity;
import playlagom.sharelocation.libs.Converter;

public class DisplayActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener {

    private static final String TAG = DisplayActivity.class.getSimpleName();
    private static final String LOG_TAG = "DisplayActivity";
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    private GoogleMap mMap;

    private static final int PERMISSIONS_REQUEST = 1;
    static boolean taskCompleted = false;
    private boolean insideShouldShow = false;

    private ImageView ivSelectedUser;

    @Override
    protected void onRestart() {
        super.onRestart();
        if (taskCompleted) {
            finish();
        }
    }

    private ImageView ivMyCircle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // wire xml components with java object
        // My Circle
        ivMyCircle = findViewById(R.id.ivMyCircle);
        // selected user
        ivSelectedUser = findViewById(R.id.ivSelectedUser);
        ivSelectedUser.setImageBitmap(Converter.getCroppedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.current_user)));

        TextView tvLogout = findViewById(R.id.tvLogout);
        tvLogout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getApplicationContext(), "Successfully Logout", Toast.LENGTH_LONG).show();
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
            // Build the alert dialog
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("Location Services Not Active");
//            builder.setMessage("Please enable Location Services and GPS");
//            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    // Show location settings when the user acknowledges the alert dialog
//                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                    startActivity(intent);
//                }
//            });
//            Dialog alertDialog = builder.create();
//            alertDialog.setCanceledOnTouchOutside(false);
//            alertDialog.show();
        }

        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        int permission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startTrackerService();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {

        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Start the service when the permission is granted
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Authenticate with Firebase when the Google map is loaded
        mMap = googleMap;
        mMap.setOnCameraMoveListener(this);
//        mMap.setMaxZoomPreference(16);
        loginToFirebase();
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

//    private void subscribeToUpdates() {
//        // Functionality coming next step
//    }
//
//    private void setMarker(DataSnapshot dataSnapshot) {
//        // Functionality coming next step
//    }

    private void subscribeToUpdates() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path));
//        final String path = getString(R.string.firebase_path) + "/" + DisplayActivity.currentUser.getUid() + "/location";

        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                if (controllerBitClicked) {
                    setActiveMarker(dataSnapshot);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                if (controllerBitClicked) {
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

    private void setActiveMarker(DataSnapshot dataSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once

//        mMap.clear();
        String key = dataSnapshot.getKey();
        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshot.getValue();
        double lat = Double.parseDouble(value.get("latitude").toString());
        double lng = Double.parseDouble(value.get("longitude").toString());
        LatLng location = new LatLng(lat, lng);


        if (!mMarkers.containsKey(key)) {
            mMarkers.put(key, mMap.addMarker(new MarkerOptions().title("" + key + ": FriendName").position(location).snippet("dis, time, address, cell, msg")));
        } else {
            mMarkers.get(key).setPosition(location);
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (Marker marker : mMarkers.values()) {
//            mMap.addMarker(new MarkerOptions().title(key).position(location));

            builder.include(marker.getPosition());
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
    }

    private boolean controllerBitClicked = false;
    public void onClickMyCircle(View view) {
        if (controllerBitClicked) {
            // HIDE LIVE FRIENDS
            controllerBitClicked = false;
            ivMyCircle.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_people_outline_black_24dp));
            setInactiveMarker();
        } else {
            // SHOW LIVE FRIENDS
            controllerBitClicked = true;
            Toast.makeText(getApplicationContext(), "Live Friends", Toast.LENGTH_LONG).show();
            ivMyCircle.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_people_black_24dp));
            subscribeToUpdates();
        }
    }

    private void setInactiveMarker() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path));
        ref.child("1000").addValueEventListener(new ValueEventListener() {
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
}