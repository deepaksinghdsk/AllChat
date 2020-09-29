package com.example.clientserver.loginActivity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.clientserver.MainActivity;
import com.example.clientserver.R;

public class walkThrough extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkthrough);
    }

    public void goToMainActivity(View view) {
        Intent intent = new Intent(walkThrough.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK & Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
