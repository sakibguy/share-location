package playlagom.sharelocation.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import playlagom.sharelocation.DisplayActivity;
import playlagom.sharelocation.R;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SignUpActivity";
    Button btnSignUp;
    EditText etSignUpEmail, etSignUpPassword, etName, etPhone;
    Button btnLogin;

    ProgressDialog progressDialog;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_sign_up);

        // check internet connection
        if (!isInternetOn()) {
            Toast.makeText(this, "Please ON your internet", Toast.LENGTH_LONG).show();
            finish();
        }
        Log.d(TAG, "----onCreate: " + isInternetOn());

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, DisplayActivity.class));
            finish();
        }

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child(getString(R.string.sharelocation));
        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);

        etName = findViewById(R.id.etName);
        etSignUpEmail = findViewById(R.id.etSignUpEmail);
        etSignUpPassword = findViewById(R.id.etSignUpPassword);
        etPhone = findViewById(R.id.etPhone);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogin = findViewById(R.id.btnLogin);

        btnSignUp.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btnSignUp) {
            registerUser();
        }
        if (v == btnLogin) {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        }
    }
    // Paste this on activity from where you need to check internet status
    public boolean isInternetOn() {
        // get Connectivity Manager object to check connection
        ConnectivityManager connec =
                (ConnectivityManager) getSystemService(getBaseContext().CONNECTIVITY_SERVICE);

        // Check for network connections
        if (connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||
                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED) {
            return true;
        } else if (
                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                        connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED) {
            return false;
        }
        return false;
    }

    private void registerUser() {
        // Register User
        final String name = etName.getText().toString().trim();
        final String email = etSignUpEmail.getText().toString().trim();
        final String password = etSignUpPassword.getText().toString().trim();
        final String phone = etPhone.getText().toString().trim();

        // START: form validation
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_LONG).show();
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_LONG).show();
            etSignUpEmail.setError("Email is required");
            etSignUpEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etSignUpEmail.setError("Please enter a valid Email");
            etSignUpEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_LONG).show();
            etSignUpPassword.setError("Password is required");
            etSignUpPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etSignUpPassword.setError("Minimum length of pass should be 6");
            etSignUpPassword.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please enter your Phone number", Toast.LENGTH_LONG).show();
            etPhone.setError("Phone is required");
            etPhone.requestFocus();
            return;
        } // END: form validation

        progressDialog.setMessage("Registering user...");
        progressDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>(){
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Registration successful and move to profile page
                    Toast.makeText(SignUpActivity.this, "Registration Successful", Toast.LENGTH_LONG).show();
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();

                    databaseReference.child(currentUser.getUid()).child("name").setValue(name);
                    databaseReference.child(currentUser.getUid()).child("email").setValue(email);
                    databaseReference.child(currentUser.getUid()).child("password").setValue(password);
                    databaseReference.child(currentUser.getUid()).child("phone").setValue(phone);
                    databaseReference.child(currentUser.getUid()).child("danger").setValue("0");
                    databaseReference.child(currentUser.getUid()).child("online").setValue("0");

                    progressDialog.dismiss();
                    finish();
                    startActivity(new Intent(SignUpActivity.this, DisplayActivity.class));
                } else {
                    progressDialog.dismiss();
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(SignUpActivity.this, "You are already registered!", Toast.LENGTH_LONG).show();
                        // TODO: tvSignIn.setText("Forget password, click to recover.");
                    } else {
                        Toast.makeText(SignUpActivity.this, "Could not register. Try again later", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }
        });
    }
}
