package playlagom.sharelocation;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class CompanyLogo extends AppCompatActivity {

    private static final String TAG = "CompanyLogo";
    static int timeCount = 1;
    boolean threadFlag = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // requesting to turn the title OFF
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // dont sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // only portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.company_logo);

        final TextView tvTimeCount = findViewById(R.id.tvTimeCount);
        threadFlag = true;

        final Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "run: active");
                try {
                    while (threadFlag) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "run: UiThread");
                                if (timeCount == 5) {
                                    Log.d(TAG, "run: UiThread: timeCount==4");
                                    stopLoop();
                                    startActivity(new Intent(CompanyLogo.this, MainActivity.class));
                                    finish();
                                } else {
                                    Log.d(TAG, "run: UiThread: timeCount");
                                    int tempTimeValue = 4 - timeCount;
                                    tvTimeCount.setText("" + tempTimeValue);
                                    timeCount++;
                                }
                            }
                        });
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void stopLoop() {
        threadFlag = false;
    }
}
