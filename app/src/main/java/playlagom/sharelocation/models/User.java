package playlagom.sharelocation.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.maps.android.clustering.ClusterItem;

import java.util.HashMap;

/**
 * Created by User on 4/12/2018.
 */

@IgnoreExtraProperties
public class User {

    private String name;
    private String email;
    private String password;
    private String phone;
    private String danger;
    private String online;
    private Position position;

    //default constructor
    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
        // SUPPORT: https://firebase.google.com/docs/database/android/read-and-write

        // empty default constructor, necessary for Firebase to be able to deserialize users
        // SUPPORT: https://stackoverflow.com/questions/39552348/firebase-databaseexception-failed-to-convert-value-of-type-java-lang-long-to-st
    }
    public User(String name, String email, String password, String phone, String danger, Position position) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.danger = danger;
        this.position = position;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getPhone() {
        return phone;
    }


    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public String getEmail() {
        return email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getPassword() {
        return password;
    }

    public void setDanger(String danger) {
        this.danger = danger;
    }
    public String getDanger() {
        return danger;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
    public Position getPosition() {
        return position;
    }

    public void setOnline(String online) {
        this.online = online;
    }
    public String getOnline() {
        return online;
    }

    // ERROR: Position is missing a constructor with no arguments
    // SOUTION:  sub class must be static or in a separate file
    // SUPPORT: https://stackoverflow.com/questions/38802269/firebase-user-is-missing-a-constructor-with-no-arguments
    public static class Position {

        private String latitude;
        private String longitude;

        //default constructor
        public Position() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
            // SUPPORT: https://firebase.google.com/docs/database/android/read-and-write

            // empty default constructor, necessary for Firebase to be able to deserialize users
            // SUPPORT: https://stackoverflow.com/questions/39552348/firebase-databaseexception-failed-to-convert-value-of-type-java-lang-long-to-st
        }
        public Position(String latitude, String longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }
        public String getLatitude() {
            return latitude;
        }

        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }
        public String getLongitude() {
            return longitude;
        }
    }
}
