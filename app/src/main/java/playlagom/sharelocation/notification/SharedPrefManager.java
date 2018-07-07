package playlagom.sharelocation.notification;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Sakib on 7/7/2018.
 */

public class SharedPrefManager {

    private static final String SHARED_PREF_NAME = "slfcmsp";
    private static final String KEY_ACCESS_TOKEN = "token";

    private static Context ctx;
    private static SharedPrefManager instance;

    private SharedPrefManager (Context context) {
        ctx = context;
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null)
            instance = new SharedPrefManager(context);
        return instance;
    }

    public boolean storeToken(String token) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ACCESS_TOKEN, token);
        editor.apply();
        return true;
    }

    public String getToken() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }
}
