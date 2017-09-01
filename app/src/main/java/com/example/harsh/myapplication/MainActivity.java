package com.example.harsh.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // Called when user clicks message sending button
    public void messageSending(View view) {
        Intent intent = new Intent(this, SendMessageActivity.class);
        startActivity(intent);
    }

    // Called when user clicks file saving button
    public void fileSaving(View view) {
        Intent intent = new Intent(this, FileSavingActivity.class);
        startActivity(intent);
    }

    // Called when user clicks file reading button
    public void fileReading(View view) {
        Intent intent = new Intent(this, FileReadingActivity.class);
        startActivity(intent);
    }

    // Called when connect is clicked
    public void connect(View view) {
        Intent intent = new Intent(this, ConnectActivity.class);
        startActivity(intent);
    }
}
