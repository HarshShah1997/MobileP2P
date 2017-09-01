package com.example.harsh.myapplication;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileSavingActivity extends AppCompatActivity {

    private String extension = ".txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_saving);
    }

    // Called when save button is clicked
    public void saveFile(View view) throws IOException {
        if (!isExternalStorageWritable()) {
            // Display error message
            return;
        }
        String filename = getFilename();
        File directory = new File(Environment.getExternalStorageDirectory(), "Test");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, filename);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.append("No string");
        fileWriter.close();
        Toast.makeText(getApplicationContext(), "File created successfully", Toast.LENGTH_SHORT).show();
    }

    private String getFilename() {
        EditText editText = (EditText) findViewById(R.id.fileName);
        String filename = editText.getText().toString();
        return filename + extension;
    }

    // Checks if the external storage is writable or not
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
