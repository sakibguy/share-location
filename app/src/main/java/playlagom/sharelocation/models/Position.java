package playlagom.sharelocation.models;

/**
 * Created by Sakibur Rahman on 4/22/2018.
 */

// SUPPORT: https://firebase.google.com/docs/database/android/read-and-write
public class Position {
    private double latitude;
    private double longitude;

    public Position() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Position(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
