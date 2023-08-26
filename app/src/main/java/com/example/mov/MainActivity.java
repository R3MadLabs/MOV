package com.example.mov;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_FILE = 123;
    private static final int STORAGE_PERMISSION_CODE = 1;

    @SuppressLint("IntentReset")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(v ->{
            pickAnyFile();
        });
        requestStoragePermission();
    }
    private boolean isStoragePermissionGranted() {
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
    }
    private void requestStoragePermission() {
        if (!isStoragePermissionGranted()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE
            );
        }else {
            return;
        }
    }
    private void pickAnyFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent = Intent.createChooser(intent, "Choose a file");
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (selectedUri != null) {
                String base_name = getFileNameFromUri(selectedUri);
                String tempdir = temp_dir(selectedUri, base_name);
                do_video_part(tempdir, base_name);
            }
        }
    }
    private String temp_dir(Uri uri, String base_name){
        File tempFile = new File(this.getCacheDir(), base_name);
        if(tempFile.exists()){
            tempFile.delete();
        }
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while (true) {
                assert inputStream != null;
                if ((length = inputStream.read(buffer)) == -1) break;
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile.getAbsolutePath();
    }
    private String getFileNameFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        String displayName = "";
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return displayName;
    }
    private void del_temp_dir(String tempdir){
        File tempFile = new File(tempdir);
        if(tempFile.exists()){
            tempFile.delete();
        }
    }
    @SuppressLint("RestrictedApi")
    void do_video_part(final String videoPath, final String base_name) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int lastDotIndex = videoPath.lastIndexOf(".");
                String fileExtension = "";
                if (lastDotIndex > 0 && lastDotIndex < videoPath.length() - 1) {
                    fileExtension = videoPath.substring(lastDotIndex + 1);
                }
                // Modify the output file name to replace the original extension with ".mp4"
                String outputFileName = base_name.replaceAll(fileExtension, "mp4");
                String[] command = {
                        "-y", // Overwrite output file if it already exists
                        "-i", videoPath,
                        "-c:v", "h264", // Change the video codec to h264
                        "-c:a", "aac",
                        "-strict", "experimental",
                        Environment.getExternalStorageDirectory() + "/" + outputFileName
                };
                FFmpegKit.execute(command);
                // Post the result back to the main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onVideoProcessingCompleted(videoPath);
                    }
                });
            }
        });
        thread.start();
    }


    @SuppressLint("RestrictedApi")
    private void onVideoProcessingCompleted(String videoPath) {
        del_temp_dir(videoPath);
        Log.i(LOG_TAG, "Async command execution completed successfully.");
        Toast.makeText(this, "Video Saved", Toast.LENGTH_SHORT).show();
    }
}