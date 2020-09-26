package com.example.clientserver;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static android.os.Looper.prepare;
import static com.example.clientserver.makeConnectionService.fragmentUpdater1;
import static com.example.clientserver.makeConnectionService.fragmentUpdater2;
import static com.example.clientserver.makeConnectionService.mid;
import static com.example.clientserver.makeConnectionService.storageReference;

public class chatActivity extends AppCompatActivity {

    private EditText et;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = database.getReference();
    private LinearLayout ll;
    private String rid, line;
    private Database db;
    private String rName, tag = "chatActivity";
    private Context c = this;
    private int iteration = 0;
    private boolean closed = false;
    private ProgressBar progressBar;
    private StorageReference temp_messages;
    private String rootPath = android.os.Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/ChatApp/";
    private File tempFile = new File(rootPath + "temp_messages");

    //this is handler to communicate with threads in this class if required
   /* @SuppressLint("HandlerLeak")
    private
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NotNull Message msg) {
            super.handleMessage(msg);
            et.setText("");
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar bar = findViewById(R.id.chatToolbar);
        setSupportActionBar(bar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ImageView toolbarImage = findViewById(R.id.dp);
        TextView name = findViewById(R.id.name);

        ll = findViewById(R.id.ll);
        et = findViewById(R.id.et);
        db = new Database(getApplicationContext(), "clientDb", 1, "ridData");

        //Button bt = findViewById(R.id.bt);
        ImageButton ibt = findViewById(R.id.ibt);
        final String toolbarImageLoc = String.valueOf(getIntent().getStringExtra("image"));
        rid = String.valueOf(getIntent().getStringExtra("Rid"));

        if (toolbarImageLoc.equals("0")) {

            //toolbarImage.setImageDrawable(Drawable.createFromPath(toolbarImageLoc));
            if (rid.contains("group")) {
                toolbarImage.setImageResource(R.drawable.ic_group);
            } else {
                toolbarImage.setImageResource(R.drawable.ic_person);
            }
        } else toolbarImage.setImageDrawable(Drawable.createFromPath(toolbarImageLoc));

        System.out.println("rid = " + rid);
        rName = getIntent().getStringExtra("name");
        name.setText(rName);
        //temp_messages = storageReference.child(rid).child("temp_messages");

        //now start reading old,temp messages from this rid, temp messages should be saved by service in rid'stempmessages file
        readerThread();

        //handler.sendEmptyMessage(0);

        toolbarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent imageOpenerIntent = new Intent(chatActivity.this, FullscreenImageopener.class);
                if (toolbarImageLoc.equals("0")) {

                    imageOpenerIntent.putExtra("type", "resource");
                    if (rid.contains("group")) {
                        imageOpenerIntent.putExtra("path", R.drawable.ic_group);
                    }
                } else {
                    imageOpenerIntent.putExtra("type", "path");
                    imageOpenerIntent.putExtra("path", toolbarImageLoc);
                }
                startActivity(imageOpenerIntent);

            }
        });

        ibt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //opening of file selector/opener
                Intent fileOpener;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    fileOpener = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                } else {
                    fileOpener = new Intent(Intent.ACTION_GET_CONTENT);
                }
                fileOpener.setType("*/*");
                startActivityForResult(fileOpener, 11);
            }
        });
    }

    private boolean isConnectingToInternet(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    private int dp(int sizeInPx) {
        Resources r = getResources();
        int dp = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, sizeInPx,
                r.getDisplayMetrics());
        System.out.println(sizeInPx + "px = " + dp + "dp");
        return dp;
    }

    private TextView textView(String from, String message, String fromColor, String textColor, boolean focus, boolean needReferenceOfFromTv) {

        if (fromColor == null) fromColor = "#3700B3";

        TextView fromTv = new TextView(this);
        TextView messTv = new TextView(this);

        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        if (from.contains("tab")) {
            from = "";
            llParam.setMargins(200, 5, 5, 5);
        } else
            llParam.setMargins(5, 5, 5, 5);

        final LinearLayout parent = new LinearLayout(this);
        parent.setLayoutParams(llParam);
        parent.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);

        fromTv.setLayoutParams(params);
        fromTv.setText(from);
        params.weight = 0;
        fromTv.setTextColor(Color.parseColor(fromColor));
        fromTv.setTextSize(dp(10));
        fromTv.setPadding(5, 1, 0, 0);

        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams
                (0, LinearLayout.LayoutParams.MATCH_PARENT);

        messTv.setText(message);
        params1.weight = 1;
        if (textColor != null) messTv.setTextColor(Color.parseColor(textColor));
        messTv.setLayoutParams(params1);
        messTv.setTextSize(dp(9));
        messTv.setPadding(5, 1, 2, 0);

        parent.addView(fromTv);
        parent.addView(messTv);

        if (focus) {
            parent.setFocusable(true);
            parent.setFocusableInTouchMode(true);
            parent.requestFocus();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ll.addView(parent);
            }
        });

        return needReferenceOfFromTv ? fromTv : null;
    }

    private ProgressBar imageView(final String img, final Uri uri, boolean needProgressBar, boolean focus) {
        Log.i(tag, "imageView is called and progressBar wanted = " + needProgressBar);
        final CardView imageCv = new CardView(this);
        final ImageView image = new ImageView(this);
        final ProgressBar imgProgressBar = new ProgressBar(getApplicationContext(), null, android.R.attr.progressBarStyleHorizontal);

        //params for cardView
        LinearLayout.LayoutParams paramForCv = new LinearLayout.LayoutParams(400, 400);
        paramForCv.gravity = Gravity.CENTER;
        paramForCv.setMargins(0, 5, 5, 0);
        imageCv.setLayoutParams(paramForCv);
        imageCv.setRadius(30);
        imageCv.setCardBackgroundColor(Color.GRAY);

        CardView.LayoutParams params = new CardView.LayoutParams
                (CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        params.setMargins(4, 7, 4, 7);
        image.setLayoutParams(params);
        if (img != null)
            image.setImageDrawable(Drawable.createFromPath(img));
        else image.setImageURI(uri);
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            image.setElevation(10);*/

        CardView.LayoutParams progressParams = new CardView.LayoutParams
                (CardView.LayoutParams.WRAP_CONTENT, 130);
        progressParams.gravity = Gravity.CENTER;
        imgProgressBar.setLayoutParams(progressParams);
        imgProgressBar.setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.circular, null));
        imgProgressBar.setIndeterminate(false);
        //by default max progress is 100
        imgProgressBar.setMax(100);
        imgProgressBar.setProgress(10);
        imgProgressBar.setVisibility(View.GONE);

        imageCv.addView(image);
        imageCv.addView(imgProgressBar);

        if (focus) {
            imageCv.setFocusable(true);
            imageCv.setFocusableInTouchMode(true);
            imageCv.requestFocus();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ll.addView(imageCv);
            }
        });

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(chatActivity.this, FullscreenImageopener.class);
                i.putExtra("type", img == null ? "uri" : "path");
                i.putExtra("path", img == null ? uri.toString() : img);
                startActivity(i);
            }
        });

        if (needProgressBar) return imgProgressBar;

        return null;
    }

    private void emptyMedia(final String fileName, final String mediaType, final boolean focus) {
        Log.i(tag, "emptyMedia started...");

        final CardView cv = new CardView(this);
        final ImageView imageView = new ImageView(this);
        final VideoView videoView = new VideoView(this);
        final ImageButton imageButton = new ImageButton(this);
        final ProgressBar progressBar = new ProgressBar(getApplicationContext(), null, android.R.attr.progressBarStyleHorizontal);

        //params for cardView
        LinearLayout.LayoutParams paramForCv = new LinearLayout.LayoutParams(400, 400);
        paramForCv.gravity = Gravity.CENTER;
        paramForCv.setMargins(0, 5, 0, 5);
        cv.setLayoutParams(paramForCv);
        cv.setRadius(30);
        cv.setCardBackgroundColor(Color.GRAY);

        LinearLayout.LayoutParams paramsForImageAndVideo = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        paramsForImageAndVideo.gravity = Gravity.CENTER;
        paramsForImageAndVideo.setMargins(0, 10, 0, 10);

        imageView.setLayoutParams(paramsForImageAndVideo);
        imageView.setVisibility(View.GONE);
        videoView.setLayoutParams(paramsForImageAndVideo);
        videoView.setVisibility(View.GONE);

        //params for imageButton and progressBar
        CardView.LayoutParams params = new CardView.LayoutParams
                (CardView.LayoutParams.WRAP_CONTENT, 130, Gravity.CENTER);
        imageButton.setLayoutParams(params);
        imageButton.setImageResource(R.drawable.ic_file_download);
        imageButton.setBackgroundColor(Color.GRAY);
        imageButton.setVisibility(View.GONE);

        progressBar.setLayoutParams(params);
        progressBar.setIndeterminate(false);
        progressBar.setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.circular, null));
        //by default max progress is 100
        progressBar.setMax(100);
        progressBar.setProgress(10);
        progressBar.setVisibility(View.GONE);

        //image.setVerticalFadingEdgeEnabled(true);
        cv.addView(imageView);
        cv.addView(videoView);
        cv.addView(imageButton);
        cv.addView(progressBar);

        if (focus) {
            cv.setFocusable(true);
            cv.setFocusableInTouchMode(true);
            cv.requestFocus();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ll.addView(cv);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(chatActivity.this, FullscreenImageopener.class);
                i.putExtra("type", "path");
                i.putExtra("path", rootPath + fileName);
                startActivity(i);
            }
        });

        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(chatActivity.this, FullscreenActivityVideoView.class);
                i.putExtra("type", "path");
                i.putExtra("path", rootPath + fileName);
                startActivity(i);
            }
        });

        final File contentCheck = new File(rootPath + fileName);
        if (!contentCheck.exists()) {
            imageButton.setVisibility(View.VISIBLE);

            Log.i("chatActivity.java", "FNF in root directory, download from server...");
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imageButton.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    Log.i(tag, "data to be downloaded = " + fileName + ", and rid = " + rid);

                    try {
                        contentCheck.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    storageReference.child(String.valueOf(mid)).child(fileName).getFile(contentCheck)
                            .addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                                    storageReference.child(String.valueOf(mid)).child(fileName).delete();
                                    progressBar.setVisibility(View.GONE);
                                    iteration = 0;
                                    Log.i("chatActivity.java", "media downloaded successfully");

                                    //notify image viewers(i.e gallery apps) about this new image download
                                    MediaScannerConnection.scanFile(c, new String[]{rootPath + fileName}, null, new MediaScannerConnection.OnScanCompletedListener() {
                                        @Override
                                        public void onScanCompleted(String path, Uri uri) {
                                            Log.d(tag, "Media scan is complete and path = " + path);
                                        }
                                    });

                                    if (mediaType.equals("image")) {
                                        imageView.setVisibility(View.VISIBLE);
                                        imageView.setCropToPadding(true);
                                        imageView.setImageURI(Uri.fromFile(contentCheck));

                                    } else if (mediaType.equals("video")) {
                                        videoView.setVisibility(View.VISIBLE);
                                        videoView.setVideoURI(Uri.fromFile(contentCheck));
                                    }
                                }
                            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull final FileDownloadTask.TaskSnapshot taskSnapshot) {
                            iteration++;
                            int progress = (int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                            if (progress < 20) {
                                progress = 20 * iteration + progress;
                                progressBar.setProgress(Math.min(progress, 70));
                            } else
                                progressBar.setProgress(progress);
                            Log.i(tag, "progress = " + progress);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            iteration = 0;
                            progressBar.setVisibility(View.GONE);
                            contentCheck.delete();
                            Toast.makeText(c, "File not found at server", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } else {
            Log.i(tag, "image's size = " + contentCheck.getTotalSpace());

            cv.setCardBackgroundColor(Color.parseColor("#7C011049"));
            try {
                if (mediaType.equals("image")) {
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageURI(Uri.fromFile(contentCheck));
                } else if (mediaType.equals("video")) {
                    videoView.setVisibility(View.VISIBLE);
                    videoView.setVideoURI(Uri.fromFile(contentCheck));
                }
            } catch (Exception e) {
                Log.e(tag, "Enable to set content in its view");
            }
        }

    }

    private ProgressBar videoView(final String s, final Uri uri, boolean needProgressBar, final boolean focus) {
        VideoView video = new VideoView(this);
        final CardView videoCv = new CardView(this);
        ProgressBar videoProgressbar = new ProgressBar(this);

        LinearLayout.LayoutParams paramForCv = new LinearLayout.LayoutParams
                (300, 300);
        paramForCv.gravity = Gravity.CENTER;
        videoCv.setLayoutParams(paramForCv);
        videoCv.setRadius(dp(30));
        videoCv.setCardBackgroundColor(Color.GRAY);

        CardView.LayoutParams progressParams = new CardView.LayoutParams
                (CardView.LayoutParams.WRAP_CONTENT, CardView.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = Gravity.CENTER;
        progressParams.setMargins(dp(5), dp(5), dp(5), dp(5));
        videoProgressbar.setLayoutParams(progressParams);
        videoProgressbar.setIndeterminate(false);
        videoProgressbar.setMax(100);
        videoProgressbar.setProgress(0);
        videoProgressbar.setVisibility(View.GONE);

        CardView.LayoutParams params = new CardView.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;

        video.setLayoutParams(params);
        if (s != null)
            video.setVideoPath(s);
        else video.setVideoURI(uri);
        videoCv.addView(video);

        if (focus) {
            videoCv.setFocusable(true);
            videoCv.setFocusableInTouchMode(true);
            videoCv.requestFocus();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ll.addView(videoCv);
            }
        });

        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(chatActivity.this, FullscreenActivityVideoView.class);
                i.putExtra("type", s == null ? "uri" : "path");
                i.putExtra("path", s == null ? uri.toString() : s);
                startActivity(i);
            }
        });

        if (needProgressBar) return videoProgressbar;

        return null;
    }

    public void sendClicked(View view) {
        System.out.println("inside send button");
        final String outData = et.getText().toString();

        if (outData.length() > 0) {
            et.setText("");

            if (isConnectingToInternet(this)) {
                Log.i("send button", "accessing firebase rid = " + rid);

                //sendText(temp_messages,tempFile,outData);
                myRef.child(rid).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot snapshot) {
                        System.out.println("before condition");
                        if (String.valueOf(snapshot.child("writing").getValue()).equals("0")) {

                            myRef.child(rid).removeEventListener(this);
                            myRef.child(rid).child("writing").setValue("1");

                            Log.i("chatActivity", "condition met");

                            //set value of writing to 1, so that no other client can download and write in tempmessages file
                            // and messages of any user will not be lost

                            if (!tempFile.exists()) {
                                try {
                                    //create an empty file at this location
                                    tempFile.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (rid.contains("group")) {
                                final int lastMessageNumber = Integer.parseInt(String.valueOf
                                        (snapshot.child("lastMessageNumber").getValue())) + 1;
                                //final int lastMessageNumber = Integer.parseInt(MessageNumber) + 1;
                                temp_messages = storageReference.child(rid)
                                        .child("temp_message" + lastMessageNumber);

                                try {
                                    Log.i(tag, "Data to be written :\r\n" + mid + "\r\n" + outData);
                                    FileWriter tempWriter = new FileWriter(tempFile);
                                    tempWriter.write(mid + "\r\n" + outData + "\r\n");
                                    tempWriter.flush();
                                    tempWriter.close();

                                    temp_messages.putFile(Uri.fromFile(tempFile)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                            deleteFile(tempFile);
                                            myRef.child(rid).child("writing").setValue("0");

                                            //put writtenBy and noOfUnreadIds in new temp_message entry in database
                                            myRef.child(rid).child("temp_message" + lastMessageNumber)
                                                    .child("writtenBy").setValue(mid);
                                            myRef.child(rid).child("temp_message" + lastMessageNumber)
                                                    .child("noOfUnreadIds").setValue(snapshot.child("noOfGroupMembers").getValue());

                                            //update last message number in database
                                            myRef.child(rid).child("lastMessageNumber").setValue(lastMessageNumber);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            deleteFile(tempFile);
                                            myRef.child(rid).child("writing").setValue("0");
                                            Log.e(tag, "unable to upload text messages data in tempFile in database, error :\n" + e.getMessage());
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                temp_messages = storageReference.child(rid).child("temp_messages");

                                temp_messages.getFile(tempFile).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                                        try {
                                            Log.i(tag, "Data to be written :\r\n" + mid + "text\r\n" + outData);
                                            FileWriter tempWriter = new FileWriter(tempFile, true);
                                            tempWriter.write(mid + "\r\n" + outData + "\r\n");
                                            tempWriter.flush();
                                            tempWriter.close();

                                            temp_messages.putFile(Uri.fromFile(tempFile)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                    deleteFile(tempFile);
                                                    myRef.child(rid).child("writing").setValue("0");
                                                    myRef.child(rid).child("newmessages").setValue("1");
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    deleteFile(tempFile);
                                                    myRef.child(rid).child("writing").setValue("0");
                                                    Log.e(tag, "unable to upload text messages data in tempFile in database, error :\n" + e.getMessage());
                                                }
                                            });
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        //Toast.makeText(c, "unable to download file from server", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        myRef.child(rid).child("writing").setValue("0");
                                        Log.e(tag, "unable to download text messages data in tempFile in database, error :\n" + e.getMessage());
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.i(tag, "firebase onCancelled : " + error.getMessage());
                    }
                });

                //write data in rid's_messages.txt file
                try {
                    FileWriter fileWriter = new FileWriter(rootPath + rid + "'s_messages.txt", true);
                    fileWriter.write("me:\r\n" + outData + "\r\n");
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Log.i("FirebaseStorage", "Messages sent to Firebase server");

                textView("Me : ", outData, "#3700B3", null, true, false);

            } else {
                //Toast.makeText(b, "Check your internet connection!", Toast.LENGTH_SHORT).show();
                textView("\t\tError : ", "No internet!", "#FF6347", null, true, false);
            }
            //et.focusSearch(View.FOCUS_UP);

        /*
        //initialising keyboard manager if necessary
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Service.INPUT_METHOD_SERVICE);
        if (imm != null) {
        //for opening keyboard
            imm.showSoftInput(et, 0);
        }
        //for closing keyboard
          imm.hideSoftInputFromWindow(your edittext.getWindowToken(), 0);
        */
        }

        et.setFocusable(true);
        //et.setFocusableInTouchMode(true);
        et.requestFocus();
    }

    void deleteFile(File tempFile) {
        try {
            //delete data in tempFile
            FileWriter deleteTemp = new FileWriter(tempFile);
            deleteTemp.write("");
            deleteTemp.flush();
            deleteTemp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void modifiedEventListener(final String writersId, final String child, final String message, final boolean focus) {
        final TextView fromTv = textView("Loading", message == null ? "" : message, null, null, focus, true);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(tag, "creating textView");
                String fromName = snapshot.getValue() + " : ";
                if (fromTv != null) {
                    fromTv.setText(fromName);
                } else {
                    Log.d(tag, "empty fromTv textView!!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(tag, "unable to start listener, error : " + error.getDetails());
            }
        };

        myRef.child(writersId).child(child).addListenerForSingleValueEvent(listener);
    }

    void readerThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                prepare();
                Log.i(tag, "listening for data from firebase is started for rids names");
                try {
                    //this inner class will read all previous messages from rid'smessages file if any
                    File file = new File(rootPath + rid + "'s_messages.txt");

                    if (file.exists()) {
                        //BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                        final BufferedReader reader = new BufferedReader(new FileReader(file));

                        //print all earlier messages
                        while ((line = reader.readLine()) != null) {
                            System.out.println("data = " + line);

                            if (line.toLowerCase().contains("jpg") || line.toLowerCase().contains("jpeg")
                                    || line.toLowerCase().contains("png") || line.toLowerCase().contains("gif")) {

                                //do substring(13) to get the file name as line contains !@#extension:thenFileName.jpeg

                                final String filename = reader.readLine();

                                Log.i(tag, "file name = " + filename + " and file length = " + filename.length());

                           /* if (filename.contains("/")) {
                                Log.i(tag, "file is uri");
                            } else {
                                Log.i(tag, "file is image in local directory");
                            }*/

                                if (line.contains("me:")) {
                                    textView("Me : ", "", null, null, false, false);
                                } else {
                                    if (rid.contains("group")) {
                                        modifiedEventListener(line.substring(0, 6), "name", "", false);
                                    } else {
                                        textView(rName + " : ", "", null, null, false, false);
                                    }
                                }

                                if (!filename.contains("/")) {
                                    emptyMedia(filename, "image", true);
                                } else {
                                    final Uri fileUri = Uri.parse(filename);
                                    imageView(null, fileUri, false, true);
                                }
                            } else if (line.contains("mp4") || line.contains("avi")) {

                                //do substring(13) to get the file name as line contains !@#extension:thenFileName.mp4
                                final String filename = reader.readLine();

                                Log.i("chatActivityReading", "filename = " + filename);

                                //Log.i(tag, "file name = " + filename + " and file length = " + filename.length());
                                if (line.contains("me:")) {
                                    textView("Me : ", "", null, null, false, false);
                                } else {
                                    if (rid.contains("group")) {
                                        modifiedEventListener(line.substring(0, 6), "name", "", false);
                                    } else {
                                        textView(rName + " : ", "", null, null, false, false);
                                    }
                                }

                                if (!filename.contains("/")) {
                                    emptyMedia(filename, "video", true);
                                } else {
                                    final Uri fileUri = Uri.parse(filename);
                                    videoView(null, fileUri, false, true);
                                }
                            } else {
                                if (line.contains("me:")) {
                                    textView("Me : ", reader.readLine(), null, null, true, false);
                                } else {
                                    if (rid.contains("group")) {
                                        modifiedEventListener(line.substring(0, 6), "name", reader.readLine(), true);
                                    } else {
                                       /* if (line.contains("me")) {
                                            textView("Me : ", reader.readLine(), null, null, true, false);
                                        } else {*/
                                        textView(rName + " : ", reader.readLine(), null, null, true, false);
                                        //}
                                    }
                                }
                            }
                        }
                    } else {
                        file.createNewFile();
                    }

                    //now print unread messages
                    //unread messages are to be saved by service at rid'stempmessages file
                    //before making rid'sTemp_messages file empty copy its messages to rid's_messages fil and
                    //and this file should be not appendable and at its writing time,
                    // first read old message and than write those messages with new message
                    while (!closed) {
                        int value = db.getData(rid);
                        if (value == 1) {
                            //for testing purposes messages will be stored in public directory
                            //later it will be shifted to private directory.

                            String filename = rid + "'sTemp_messages.txt";
                            //System.out.println(filename);
                            File file2 = new File(rootPath, filename);

                            if (file2.exists()) {
                                textView("tab", "New messages", null, "#136116", true, false);

                                //BufferedReader ridsTempMessagesReader = new BufferedReader(new InputStreamReader(new FileInputStream(file2)));
                                final BufferedReader ridsTempMessagesReader = new BufferedReader(new FileReader(file2));
                                FileWriter ridsMessagesWriter = new FileWriter(file, true);

                                //print all rid'stempmessages and cut paste them to rid'smessages
                                while ((line = ridsTempMessagesReader.readLine()) != null) {

                                    //write messages to rids permanent messages file
                                    ridsMessagesWriter.write(line + "\r\n");
                                    ridsMessagesWriter.flush();

                                    if (line.toLowerCase().contains("jpg") || line.toLowerCase().contains("jpeg")
                                            || line.toLowerCase().contains("png") || line.toLowerCase().contains("gif")) {

                                        //do substring(13) to get the file name as line contains !@#extension:thenFileName.jpeg

                                        final String name = ridsTempMessagesReader.readLine();

                                        ridsMessagesWriter.write(name + "\r\n");
                                        ridsMessagesWriter.flush();

                                        Log.i("chatActivityReading", "filename = " + name);

                                        if (line.contains("me:")) {
                                            textView("Me : ", "", null, null, false, false);
                                        } else {
                                            if (rid.contains("group")) {
                                                modifiedEventListener(line.substring(0, 6), "name", "", false);
                                                /*textView(snapshot.child(line.substring(0, 6)).child("name").getValue() + " : ",
                                                        "", null, null, false);*/

                                            } else {
                                                textView(rName + " : ", "", null, null, false, false);
                                            }
                                        }

                                        if (!filename.contains("/")) {
                                            //Log.i("chatActivity.java", "image to be downloaded from server...");

                                            emptyMedia(name, "image", false);

                                        } else {
                                            final Uri fileUri = Uri.parse(name);
                                            Log.i("chatActivity.java", "setting image with uri = " + fileUri);

                                            imageView(null, fileUri, false, false);

                                        }

                                    } else if (line.contains("mp4") || line.contains("avi")) {

                                        //do substring(13) to get the file name as line contains !@#extension:thenFileName.mp4

                                        final String name = ridsTempMessagesReader.readLine();
                                        //File checkUr = new File(URI.create(filename));

                                        ridsMessagesWriter.write(name + "\r\n");
                                        ridsMessagesWriter.flush();

                                        Log.i("chatActivityReading", "filename = " + name);

                                        if (line.contains("me:")) {

                                            textView("Me : ", "", null, null, false, false);
                                        } else {
                                            if (rid.contains("group")) {
                                                modifiedEventListener(line.substring(0, 6), "name", "", false);
                                                /*textView(snapshot.child(line.substring(0, 6)).child("name").getValue() + " : ",
                                                        "", null, null, false);*/
                                            } else {
                                                textView(rName + " : ", "", null, null, false, false);
                                            }
                                        }

                                        if (!filename.contains("/")) {
                                            //Log.i("chatActivity.java", "image to be downloaded from server...");

                                            emptyMedia(name, "video", false);
                                        } else {
                                            final Uri fileUri = Uri.parse(name);
                                            Log.i("chatActivity.java", "setting image with uri = " + fileUri);

                                            videoView(null, fileUri, false, false);
                                        }

                                    } else {
                                        String message = ridsTempMessagesReader.readLine();

                                        //write messages to rids permanent messages file
                                        ridsMessagesWriter.write(message + "\r\n");
                                        ridsMessagesWriter.flush();

                                        if (line.contains("me:")) {
                                            textView("Me : ", message, null, null, false, false);
                                        } else {
                                            if (rid.contains("group")) {
                                                modifiedEventListener(line.substring(0, 6), "name", message, false);
                                            } else {
                                                /*if (line.contains("me")) {
                                                    textView("Me : ", message, null, null, true, false);
                                                } else {*/
                                                textView(rName + " : ", message, null, null, false, false);
                                                //}
                                            }
                                        }
                                    }
                                }

                                //now before closing file empty this tempMessages file
                                FileWriter write2 = new FileWriter(file2);
                                write2.write("");
                                write2.flush();
                                write2.close();
                                ridsMessagesWriter.close();
                                ridsTempMessagesReader.close();

                                //here set noOfMessages to 0 and notify recyclerAdapter as all messages have been read successfully
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            BufferedReader userInfoFile;

                                            if (rid.contains("group"))
                                                userInfoFile = new BufferedReader(new FileReader(rootPath + "groupsInfo"));
                                            else
                                                userInfoFile = new BufferedReader(new FileReader(rootPath + "usersInfo"));

                                            StringBuilder inputBuffer = new StringBuilder();
                                            String usersData;
                                            boolean incremented = false;

                                            while ((usersData = userInfoFile.readLine()) != null) {
                                                if (usersData.equals(rid) && !incremented) {
                                                    inputBuffer.append(usersData).append("\r\n");

                                                    usersData = userInfoFile.readLine();
                                                    inputBuffer.append(usersData).append("\r\n");

                                                    userInfoFile.readLine();
                                                    inputBuffer.append(0).append("\r\n");

                                                    usersData = userInfoFile.readLine();
                                                    inputBuffer.append(usersData).append("\r\n");

                                                    //now create recyclerViewDataType and notify recyclerAdapter
                                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (rid.contains("group"))
                                                                fragmentUpdater2.notifySingleDataUpdate(rid, 0);
                                                            else
                                                                fragmentUpdater1.notifySingleDataUpdate(rid, 0);
                                                        }
                                                    });
                                                    incremented = true;
                                                    continue;
                                                }
                                                inputBuffer.append(usersData).append("\r\n");
                                            }
                                            userInfoFile.close();
                                            FileOutputStream fileOut;
                                            if (rid.contains("group"))
                                                fileOut = new FileOutputStream(rootPath + "groupsInfo");
                                            else
                                                fileOut = new FileOutputStream(rootPath + "usersInfo");
                                            fileOut.write(inputBuffer.toString().getBytes());
                                            fileOut.flush();
                                            fileOut.close();
                                        } catch (IOException e) {
                                            e.getMessage();
                                        }
                                    }
                                }).start();

                            } else
                                System.out.println("rid'sTemp_messages.txt file not exists");

                            //and update database
                            System.out.println("updating database to no new messages from this rid.");
                            db.updateData(rid, 0);
                        }
                    }

                    Log.i("New messages", "closing rid'sTemp_messages thread");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 11) {
            if (resultCode == RESULT_OK) {
                try {

                    //ask persistable uri grant permission so that this uri will be accessible after app restarts too
                    Uri sourceTreeUri = data.getData();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getApplicationContext().getContentResolver().takePersistableUriPermission(
                                sourceTreeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }
                    Log.i("chatActivity.java", "Data as String = " + data.getDataString() + "\nData as Uri= " + data.getData());
                    final Uri path = data.getData();
                    final String extension;
                    int size, index_mime, index_size;

                    //getting file format
                    Cursor c = getContentResolver().query(data.getData(), null, null, null, null);
                    c.moveToFirst();

                    index_mime = c.getColumnIndex("mime_type");
                    index_size = c.getColumnIndex("_size");

                    extension = c.getString(index_mime);
                    size = c.getInt(index_size);
                    Log.i("chatActivity.java", "name of file = " + extension + ", Size = " + size);

                    /*String[] columns = c.getColumnNames();
                    for (String columnName : columns) {
                        Log.i("chatActivityColNames", columnName);
                    }*/

                    c.close();

                    Log.i("chatActivityResult", "file extension = " + extension +
                            " \nfile path = " + path + "\nfile size = " + size);
                    if (size / 1024 < 20 * 1024) {
                        Log.i("chatActivity", "file size is ok");
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setCancelable(true);
                        builder.setTitle("Send " + path + "\n to " + rName);
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendFile(path, extension);
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    } else {
                        Log.i("chatActivity", "file size is not ok");
                        Toast.makeText(this, "File size is big", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.i("chatActivity.java", "error in getting path : ");
                    e.getStackTrace();
                }
            }
        }
    }

    private void sendFile(final Uri path, final String extension) {
        if (isConnectingToInternet(this)) {
            Log.i("sending image", "internet is ok");
            textView("Me : ", "", null, null, false, false);
            if (extension.contains("mp4") || extension.contains("avi")) {
                progressBar = videoView(null, path, true, true);
            } else {
                progressBar = imageView(null, path, true, true);
            }

            //download tempMessages file from firebase server
            // if (isConnectingToInternet(this)) {
            final String filename = System.currentTimeMillis() + "." + getFileExtension(path);
            final String filenameForDatabase = filename.substring(0, filename.lastIndexOf(".")) + "_450x474." + getFileExtension(path);

            progressBar.setVisibility(View.VISIBLE);

            //update temp_messages file of recipients
            final ValueEventListener valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull final DataSnapshot snapshot) {
                    Log.i(tag, "sending video before condition");

                    if (String.valueOf(snapshot.child("writing").getValue()).equals("0")) {
                        myRef.child(rid).removeEventListener(this);

                        myRef.child(rid).child("writing").setValue("1");
                        Log.i(tag, "sending video condition met");

                        //download messages file from firebase storage and add this URL in it
                        if (rid.contains("group")) {
                            final int lastMessageNumber = Integer.parseInt(
                                    String.valueOf(snapshot.child("lastMessageNumber").getValue())) + 1;
                            temp_messages = storageReference.child(rid)
                                    .child("temp_message" + lastMessageNumber);

                            try {
                                FileWriter tempWriter = new FileWriter(tempFile, true);
                                tempWriter.write(mid + "@!#extension:" + extension + "\r\n" + filenameForDatabase + "\r\n");
                                tempWriter.flush();
                                tempWriter.close();

                                temp_messages.putFile(Uri.fromFile(tempFile))
                                        .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                Log.i(tag, "temp_messages file uploaded successfully");
                                                deleteFile(tempFile);
                                                myRef.child(rid).child("writing").setValue("0");

                                                //put writtenBy and noOfUnreadIds in new temp_message entry in database
                                                myRef.child(rid).child("temp_message" + lastMessageNumber)
                                                        .child("writtenBy").setValue(mid);
                                                myRef.child(rid).child("temp_message" + lastMessageNumber)
                                                        .child("noOfUnreadIds").setValue(snapshot.child("noOfGroupMembers").getValue());

                                                //update last message number in database
                                                myRef.child(rid).child("lastMessageNumber").setValue(lastMessageNumber);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        deleteFile(tempFile);
                                        myRef.child(rid).child("writing").setValue("0");
                                        Log.e(tag, "unable to upload text messages data in tempFile in database, error :\n" + e.getMessage());
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else {
                            temp_messages = storageReference.child(rid).child("temp_messages");

                            temp_messages.getFile(tempFile).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                                    try {
                                        FileWriter tempWriter = new FileWriter(tempFile, true);
                                        tempWriter.write(mid + "@!#extension:" + extension + "\r\n" + filenameForDatabase + "\r\n");
                                        tempWriter.flush();
                                        tempWriter.close();

                                        temp_messages.putFile(Uri.fromFile(tempFile))
                                                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                        Log.i(tag, "temp_messages file uploaded successfully");
                                                        //delete tempFile which is in phone used to send message
                                                        deleteFile(tempFile);
                                                        myRef.child(rid).child("writing").setValue("0");
                                                        myRef.child(rid).child("newmessages").setValue("1");
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                deleteFile(tempFile);
                                                myRef.child(rid).child("writing").setValue("0");
                                                Log.e(tag, "unable to uload text messages data in tempFile in database, error :\n" + e.getMessage());
                                            }
                                        });
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    //Toast.makeText(c, "unable to download file from server", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    myRef.child(rid).child("writing").setValue("0");
                                    Log.e(tag, "unable to download text messages data in tempFile in database, error :\n" + e.getMessage());
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.i(tag, "media uploading failed");
                    error.getDetails();
                }
            };

            myRef.child(rid).addValueEventListener(valueEventListener);

            //uploading image to firebase storage
            storageReference.child(rid).child(filename).putFile(path)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            progressBar.setVisibility(View.GONE);

                            Log.i("chatActivity", "content uploaded to firebase storage");
                            if (!tempFile.exists()) {
                                try {
                                    //create an empty file at this location
                                    boolean created = tempFile.createNewFile();
                                    Log.i(tag, "In upload image, tempFile created = " + created);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            /* temp_messages.getFile(tempFile).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                                                Log.i(tag, "temp_messages file downloaded successfully");
                                                try {
                                                    FileWriter writeUrl = new FileWriter(tempFile, true);
                                                    writeUrl.write(mid + "@!#extension:" + extension + "\r\n" + filenameForDatabase + "\r\n");
                                                    writeUrl.flush();
                                                    writeUrl.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }

                                                temp_messages.putFile(Uri.fromFile(tempFile))
                                                        .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                                Log.i(tag, "temp_messages file uploaded successfully");
                                                                deleteFile(tempFile);
                                                                myRef.child(rid).child("writing").setValue("0");
                                                                myRef.child(rid).child("newmessages").setValue("1");
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        deleteFile(tempFile);
                                                        //storageReference.child(String.valueOf(rid)).child(filenameForDatabase).delete();
                                                        myRef.child(rid).child("writing").setValue("0");
                                                        Log.e(tag, "unable to upload text messages data in tempFile from database, error :\n" + e.getMessage());
                                                    }
                                                });
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                storageReference.child(rid).child(filenameForDatabase).delete();
                                                myRef.child(rid).child("writing").setValue("0");
                                                Log.e(tag, "unable to download text messages data in tempFile from database, error :\n" + e.getMessage());
                                            }
                                        });*/

                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    iteration++;
                    int progress = (int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                    if (progress < 20) {
                        progress = 20 * iteration + progress;
                        progressBar.setProgress(Math.min(progress, 70));
                    } else
                        progressBar.setProgress(progress);
                    Log.i(tag, "upload progress = " + progress + "%");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressBar.setVisibility(View.GONE);
                }
            });

            try {
                File loc = new File(rootPath + rid + "'s_messages.txt");
                if (!loc.exists()) {
                    loc.createNewFile();
                }
                FileWriter writer = new FileWriter(rootPath + rid + "'s_messages.txt", true);
                writer.write("me:@!#extension:" + extension + "\r\n" + path.toString() + "\r\n");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Log.i("unable to send image", "no internet");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView("\t\t\tError : ", "No internet", "#FF6347", null, true, false);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(tag, "back pressed");
        closed = true;
        //NavUtils.navigateUpFromSameTask(this);
        //the code below does the same thing as above does
        //finish();
        /*Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("ChatActivity", "getting out chatActivity.java");
    }
}
