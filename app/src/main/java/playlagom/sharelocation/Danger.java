package playlagom.sharelocation;

import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static playlagom.sharelocation.DisplayActivity.ivDanger;

public class Danger extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danger);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.sharelocation));

        final EditText etDangerReason = findViewById(R.id.etDangerReason);
        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String dangerReason = etDangerReason.getText().toString().trim();
                if (TextUtils.isEmpty(dangerReason)) {
                    Toast.makeText(Danger.this, "Please, write why are you in danger.", Toast.LENGTH_LONG).show();
                    etDangerReason.setError("Danger reason is required");
                    etDangerReason.requestFocus();
                    return;
                } else if (dangerReason.length() < 5) {
                    etDangerReason.setError("tell something more");
                    etDangerReason.requestFocus();
                    return;
                } else {
                    final ProgressDialog progressDialog =  new ProgressDialog(Danger.this);
                    progressDialog.setMessage("Updating danger reason...");
                    progressDialog.show();

                    DisplayActivity.dangerStatus = true;

                    ivDanger.setImageBitmap(BitmapFactory
                            .decodeResource(getResources(), R.drawable.baseline_directions_run_black_18dp));

                    databaseReference.child("" + firebaseAuth.getCurrentUser().getUid()).child("danger").setValue("1");
                    databaseReference
                            .child(firebaseAuth.getCurrentUser().getUid())
                            .child(getString(R.string.dangerReason))
                            .setValue(dangerReason)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Write was successful!
                                    Toast.makeText(getApplicationContext(), "Notified all, you are in danger", Toast.LENGTH_LONG).show();
                                    progressDialog.dismiss();
                                    finish();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Write failed
                                    Toast.makeText(getApplicationContext(), "Try again: Failed to update danger reason", Toast.LENGTH_LONG).show();
                                    progressDialog.dismiss();
                                }
                            });
                }
            }
        });
    }
}
