package playlagom.sharelocation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.HashMap;

import playlagom.sharelocation.models.User;

public class TrackerService extends Service {

    private static final String TAG = TrackerService.class.getSimpleName();

    @Override
    public IBinder onBind(Intent intent) {return null;}

    DatabaseReference databaseReferenceOnDisconnect;
    FirebaseAuth firebaseAuthForOnDisconnect;
    FirebaseUser currentUserOnDisconnect;
    String userIdOnDisconnect;
    String pathOnDisconnect;

    @Override
    public void onCreate() {
        super.onCreate();
        buildNotification();

        firebaseAuthForOnDisconnect = FirebaseAuth.getInstance();
        currentUserOnDisconnect = firebaseAuthForOnDisconnect.getCurrentUser();
        userIdOnDisconnect = currentUserOnDisconnect.getUid();
        pathOnDisconnect = getString(R.string.sharelocation) + "/" + userIdOnDisconnect;
        databaseReferenceOnDisconnect = FirebaseDatabase.getInstance().getReference(pathOnDisconnect);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d(TAG, "firebase auth success");
            requestLocationUpdates();
        } else {
            Log.d(TAG, "firebase auth failed");
        }
    }

    private void buildNotification() {
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);
        // Create the persistent notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.ic_tracker);
        startForeground(1, builder.build());
    }

    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "[ OK ] ---- received stop broadcast");
            // Stop the service when the notification is tapped
            databaseReferenceOnDisconnect.child("online").onDisconnect().setValue("0");
            unregisterReceiver(stopReceiver);
            stopSelf();
        }
    };

    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();
        request.setInterval(3000);
        request.setFastestInterval(3000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        // INIT firebase dependency
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        final String userId = currentUser.getUid();
        final String path = getString(R.string.sharelocation) + "/" + userId;
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // REQUEST location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                    Location location = locationResult.getLastLocation();

                    if (location != null) {
                        // present code
                        // SUPPORT: https://firebase.google.com/docs/database/android/read-and-write
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        HashMap<String, String> dataMap = new HashMap<>();
                        dataMap.put("latitude", String.valueOf(latitude));
                        dataMap.put("longitude", String.valueOf(longitude));

                        ref.child("position").setValue(dataMap);
                        ref.child("online").setValue("1");
                        // TODO: 4/25/2018 CHECK data stored successfully or not
                        Log.d(TAG, "---- USER ---- " + userId + " ---- location ---- " + location);
                    }
                }
            }, null);
        }
    }
}