package com.example.harsh.myapplication;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FileReadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_reading);
    }

    // Called when read button is clicked
    public void readFile(View view) {
        File directory = new File(Environment.getExternalStorageDirectory(), "Test");
        String filename = getFileName();
        File file = new File(directory, filename);

        String fileString = "";

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            fileString = bufferedReader.readLine();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextView textView = (TextView) findViewById(R.id.










                fileText);
        textView.setText(fileString);
    }

    private String getFileName() {
        EditText editText = (EditText) findViewById(R.id.fileName);
        return editText.getText().toString();
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
