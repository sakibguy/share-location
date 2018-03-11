package playlagom.sharelocation.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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

    Button btnSignUp;
    EditText etSignUpEmail, etSignUpPassword;
    TextView tvSignIn;

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

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("users");
        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);

        etSignUpEmail = findViewById(R.id.etSignUpEmail);
        etSignUpPassword = findViewById(R.id.etSignUpPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);

        btnSignUp.setOnClickListener(this);
        tvSignIn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btnSignUp) {
            registerUser();
        }
        if (v == tvSignIn) {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void registerUser() {
        // Register User
        final String email = etSignUpEmail.getText().toString().trim();
        final String password = etSignUpPassword.getText().toString().trim();

        // Form validation part: Start
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
        } // Form validation part: End

        progressDialog.setMessage("Registering user...");
        progressDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Registration successful and move to profile page
                    Toast.makeText(SignUpActivity.this, "Registration Successful", Toast.LENGTH_LONG).show();
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();

                    databaseReference.child(currentUser.getUid()).child("email").setValue(email);
                    databaseReference.child(currentUser.getUid()).child("password").setValue(password);

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
