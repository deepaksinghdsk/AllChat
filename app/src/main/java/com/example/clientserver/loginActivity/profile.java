package com.example.clientserver.loginActivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clientserver.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.example.clientserver.makeConnectionService.mid;
import static com.example.clientserver.makeConnectionService.myRef;
import static com.example.clientserver.makeConnectionService.rootPath;
import static com.example.clientserver.makeConnectionService.storageReference;

public class profile extends AppCompatActivity {

    ImageView imageView;
    ProgressBar progressBar;
    EditText et;
    ImageButton imageButton;
    File profilePicture;
    private Context c;
    private SharedPreferences myPref;
    private SharedPreferences.Editor edit;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        imageView = findViewById(R.id.profilePicture);
        progressBar = findViewById(R.id.progressBar);
        imageButton = findViewById(R.id.delete_image);
        et = findViewById(R.id.name);
        c = this.getApplicationContext();

        myPref = getApplicationContext().getSharedPreferences("myInfo", MODE_PRIVATE);
        edit = myPref.edit();

        File profilePicDirectory = new File(rootPath + "profilePictures");

        if (!profilePicDirectory.exists()) {
            profilePicDirectory.mkdir();
        }

        profilePicture = new File(profilePicDirectory, "myProfilePic.jpg");
        if (!myPref.getString("profilePicName", "null").equals("null")) {
            imageView.setImageDrawable(Drawable.createFromPath(rootPath + "profilePictures/myProfilePic.jpg"));
        } else {
            imageView.setImageResource(R.drawable.ic_person);
        }


        String myName = myPref.getString("myName", "null");
        if (!myName.equals("null")) {
            et.setText(myName);
        }
    }

    public void deleteImage(View view) {
        profilePicture.delete();
        imageView.setImageResource(R.drawable.ic_person);

        String profilePicName = myPref.getString("profilePicName", "0");
        if (!profilePicName.equals("0")) {
            storageReference.child(String.valueOf(mid)).child(profilePicName).delete();
            myRef.child(String.valueOf(mid)).child("myProfilePic").setValue("0");
        }
    }

    public void imageClicked(View view) {
        if (view.getId() == R.id.profilePicture) {

            Intent fileOpener = new Intent(Intent.ACTION_GET_CONTENT);
            fileOpener.setType("image/*");
            if (fileOpener.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(fileOpener, 11);
            }
        } else if (view.getId() == R.id.save) {
            et.setText(et.getText());
            edit.putString("myName", et.getText().toString());
            edit.commit();
            myRef.child(String.valueOf(mid)).child("name").setValue(et.getText().toString());
        }
    }

    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 11 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null && (getFileExtension(data.getData()).equals("jpg")
                    || getFileExtension(data.getData()).equals("jpeg"))) {

                try {
                    if (!profilePicture.exists()) {
                        profilePicture.createNewFile();
                    }

                    imageView.setImageURI(data.getData());

                    InputStream reader = getContentResolver().openInputStream(data.getData());
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(profilePicture));

                    byte[] buf = new byte[1024];
                    while (reader.read(buf, 0, buf.length) != -1) {
                        outputStream.write(buf, 0, buf.length);
                    }
                    reader.close();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                final String fileName = "myProfilePic" + System.currentTimeMillis() + ".jpg";
                final String fileNameForDatabase = "myProfilePic" + System.currentTimeMillis() + "_450x474.jpg";

                final String profilePicName = myPref.getString("profilePicName", "0");

                progressBar.setVisibility(View.VISIBLE);
                storageReference.child(String.valueOf(mid)).child(fileName)
                        .putFile(data.getData())
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                progressBar.setVisibility(View.GONE);
                                imageView.setImageDrawable(Drawable.createFromPath(profilePicture.getPath()));
                                edit.putString("profilePicName", fileNameForDatabase);
                                edit.commit();
                                if (!profilePicName.equals("0"))
                                    storageReference.child(String.valueOf(mid)).child(profilePicName).delete();
                                myRef.child(String.valueOf(mid)).child("myProfilePic").setValue(fileNameForDatabase);
                                Toast.makeText(c, "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        progressBar.setProgress((int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount()));
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            } else {
                Toast.makeText(this, "Images with only jpg or jpeg format are accepted for profilePic...", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
