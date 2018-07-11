package playlagom.sharelocation.notification;

import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import playlagom.sharelocation.DisplayActivity;
import playlagom.sharelocation.auth.SignUpActivity;

/**
 * Created by Sakib on 7/7/2018.
 */

// Handle messages
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessaginService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
//                scheduleJob();
            } else {
                // Handle message within 10 seconds
//                handleNow();
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody() + ", title: "
            + remoteMessage.getNotification().getTitle());
//            Toast.makeText(getApplicationContext(), "" + remoteMessage.getNotification().getBody(), Toast.LENGTH_LONG).show();
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        notifyUser(remoteMessage.getNotification().getBody(), remoteMessage.getNotification().getTitle());
    }

    private NotificationHelper notificationHelper;
    public void notifyUser(String body, String title) {
        MyNotificationManager myNotificationManager = new MyNotificationManager(getApplicationContext());
        myNotificationManager.showNotification(body, title, new Intent(getApplicationContext(), DisplayActivity.class));


//        NotificationCompat.Builder nb = notificationHelper.getChannel1Notification(body, title);
//        notificationHelper.getNotificationManager().notify(1, nb.build());
    }
}
