package com.example.clientserver.loginActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clientserver.MainActivity;
import com.example.clientserver.R;
import com.example.clientserver.addNewUserInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class verifyNumber extends AppCompatActivity {

    private String number, mVerificationId;
    private EditText et;
    private Button bt;
    private ProgressBar loading;
    private FirebaseAuth mAuth;
    private Context c;

    SharedPreferences pref;
    int mid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_number);

        c = this;
        et = findViewById(R.id.otp);
        bt = findViewById(R.id.login);
        loading = findViewById(R.id.loadingOtp);
        mAuth = FirebaseAuth.getInstance();

        pref = getApplicationContext().getSharedPreferences("myInfo", MODE_PRIVATE);

        Intent intent = getIntent();
        number = "+91" + intent.getStringExtra("number");
        sendVerificationCode(number);
    }

    public void check(View view) {
        String otp = et.getText().toString();
        if (otp.length() == 6) {
            loading.setVisibility(View.VISIBLE);
            bt.setVisibility(View.GONE);
            verifyVerificationCode(otp);
        } else et.setError("Enter a valid OTP");
    }

    private void sendVerificationCode(String number) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                number,
                60,
                TimeUnit.SECONDS,
                this,
                callback
        );
        Log.i("verifyNumber.class", "inside sendVerificationCode function and number = " + number);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callback =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                    String code = phoneAuthCredential.getSmsCode();

                    if (code != null) {
                        et.setText(code);
                        loading.setVisibility(View.VISIBLE);
                        bt.setVisibility(View.GONE);
                        verifyVerificationCode(code);
                    }
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    Toast.makeText(verifyNumber.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.i("verificationFailed", "verification failed");
                }

                @Override
                public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                    super.onCodeSent(s, forceResendingToken);
                    mVerificationId = s;
                }
            };

    void verifyVerificationCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);

        //now sign in to mainActivity if authentication gets successful
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(verifyNumber.this, new OnCompleteListener<AuthResult>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.i("verification", "inside onComplete method");
                        if (task.isSuccessful()) {

                            final DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();

                            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    boolean foundInDatabase = false;
                                    //checking if this user is existing user
                                    for (DataSnapshot child : snapshot.getChildren()) {
                                        Log.i("verifyNumber", "phone number is " + child.child("phone_number").getValue());
                                        if (String.valueOf(child.child("phone_number").getValue()).equals(number.substring(3))) {
                                            mid = Integer.parseInt(child.getKey());
                                            foundInDatabase = true;
                                            break;
                                        }
                                    }

                                    //if this user is not an existing user
                                    if (!foundInDatabase) {
                                        long last_id = (long) snapshot.child("last_id").getValue();
                                        mid = ((int) last_id) + 1;

                                        Toast.makeText(c, "Your default name is 'unknown'\nyou can change it any time from settings.", Toast.LENGTH_SHORT).show();
                                        addNewUserInfo data = new addNewUserInfo(number.substring(3),"");
                                        myRef.child(String.valueOf(mid)).setValue(data);
                                        myRef.child("last_id").setValue(mid);
                                    }

                                    SharedPreferences.Editor edit = pref.edit();
                                    edit.putInt("mId", mid);
                                    edit.apply();
                                    System.out.println("new id " + mid + " is assigned to this user...");

                                    //get old groups names and add them in myInfo pref for starting reading of data of old groups again
                                    myRef.child(String.valueOf(mid)).child("oldGroups").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String oldGroups = String.valueOf(snapshot.getValue()), groupId;
                                            int lastGroupIdNumber;

                                            SharedPreferences.Editor edit = pref.edit();
                                            if (!oldGroups.equals("null")) {
                                                while (oldGroups.contains(",")) {
                                                    //getting groupIds from database and separate them with ',' in them...
                                                    groupId = oldGroups.contains(",") ? oldGroups.substring(0, oldGroups.indexOf(",")) :
                                                            oldGroups;
                                                    lastGroupIdNumber = pref.getInt("lastGroupId", 0) + 1;

                                                    Log.d("verifyNumber", groupId + " this old group is added at lastGroupIdNumber = " + lastGroupIdNumber);

                                                    edit.putString(String.valueOf(lastGroupIdNumber), groupId);
                                                    //edit.putString(String.valueOf(lastGroupIdNumber), groupId);
                                                    //edit.putInt((lastGroupIdNumber) + "'sMessageTobeRead", 1);
                                                    edit.putInt("lastGroupId", lastGroupIdNumber);
                                                    edit.apply();

                                                    oldGroups = oldGroups.substring(oldGroups.indexOf(",") + 1);
                                                    Log.i("verifyNumber", "oldGroup name = " + oldGroups);
                                                }

                                                if (oldGroups.length() > 4 && !oldGroups.contains(",")) {
                                                    groupId = oldGroups;
                                                    Log.i("verifyNumber", "inside outside of while loop in listening for new Groups values");
                                                    lastGroupIdNumber = pref.getInt("lastGroupId", 0) + 1;
                                                    Log.d("verifyNumber", groupId + " this new group is added, lastGroupIdNumber = " + lastGroupIdNumber);
                                                    edit.putString(String.valueOf(lastGroupIdNumber), groupId);
                                                    //edit.putString(String.valueOf(lastGroupIdNumber), groupId);
                                                    //edit.putInt((lastGroupIdNumber) + "'sMessageTobeRead", 1);
                                                    //edit.putString(String.valueOf(pref.getInt("lastGroupId", 0) + 1), groupId);
                                                    edit.putInt("lastGroupId", lastGroupIdNumber);
                                                    edit.apply();
                                                }

                                                myRef.child(String.valueOf(mid)).child("newGroups").setValue("");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });

                                    Intent intent = new Intent(verifyNumber.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK & Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.i("firebase in service", "unable to add " + error.getMessage());
                                }
                            });


                        } else {
                            //verification unsuccessful... display an error message
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                et.setText("Invalid code entered");
                            } else {
                                et.setError("Error! Internet problem");
                            }
                        }
                    }
                });
    }
}
