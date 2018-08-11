package playlagom.sharelocation;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import playlagom.sharelocation.auth.LoginActivity;
import playlagom.sharelocation.auth.SignUpActivity;
import playlagom.sharelocation.libs.Converter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        //        ImageView ivDedicatedTo = findViewById(R.id.ivDedicatedTo);
        //        // ImageView: MAKE round user image
        //        ivDedicatedTo.setImageBitmap(Converter.getCroppedBitmap(
        //                BitmapFactory.decodeResource(getResources(), R.drawable.jubayer)));

        Toast.makeText(getApplicationContext(), "Bring the world in your hand", Toast.LENGTH_SHORT).show();
        Thread thread = new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("MainActivity", "=== try ====");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Log.d("MainActivity", "=== finally ====");
                    Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
        thread.start();
    }
}
