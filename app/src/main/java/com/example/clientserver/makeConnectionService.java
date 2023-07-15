package com.example.clientserver;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.example.clientserver.fragment1andItsRecyclerAdapter.updater;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class makeConnectionService extends Service {

    private Looper looper;
    private serviceHandler handler;

    public static DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
    public static StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    IBinder myBinder = new myLocalBinder();
    //Messenger messenger = null;

    static public updater fragmentUpdater1, fragmentUpdater2;
    public static Database db;
    private String tag = "service", line;

    static boolean connect = false, reading = false;
    //private int numberOfNotifications = 0;
    Context c;
    private SharedPreferences pref;

    public static String number, rootPath = android.os.Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/ChatApp/";
    public static int mid;

    final File root = new File(rootPath);
    private boolean updated = false;

    public makeConnectionService() {
    }

    // Handler that receives messages from the thread
    private final class serviceHandler extends Handler {

        serviceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
            Log.i("connectionService", "inside handleMessages method of serviceHandler");
            connect = true;

            //initialise your input and output streams
            db = new Database(getApplicationContext(), "clientDb", 1, "ridData");

            String groupId;

            if (!root.exists()) root.mkdir();

            mid = pref.getInt("mId", 100002);
            Log.i("my id = ", String.valueOf(mid));

            //Listen for any changes in firebase database
            myRef.child(String.valueOf(mid)).addValueEventListener(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull final DataSnapshot snapshot) {
                            Log.i(tag, "inside listening of firebase database");
                            //if new_messages in database is equal to 1 then go further
                            if (String.valueOf(snapshot.child("newmessages").getValue()).equals("1") &&
                                    String.valueOf(snapshot.child("writing").getValue()).equals("0")) {
                                myRef.child(String.valueOf(mid)).removeEventListener(this);
                                //myRef.child(String.valueOf(mid)).child("writing").setValue("1");
                                getMessageFromFirebase(snapshot, 0,
                                        String.valueOf(mid), 0, this);
                            } else {
                                Log.d(tag, "condition not met in reading messages of mid");
                                /*else if (msg.arg2 == 0) {
                                stopSelf(msg.arg1);
                            }*/
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.i(tag, "inside cancel of reading from database");
                            error.getMessage();
                        }
                    });

            for (int i = 1; i <= pref.getInt("lastGroupId", 0); i++) {
                groupId = pref.getString(String.valueOf(i), "");
                Log.i(tag, "group Id = " + groupId);
                int messageTobeRead = pref.getInt(i + "'sMessageTobeRead", 1);
                if (!groupId.equals("")) {
                    myRef.child(groupId).child("temp_message" + messageTobeRead).addValueEventListener(
                            listenerForGroups(groupId, messageTobeRead, i));
                    Log.i(tag, "started new event listener for " + groupId);
                }
            }

            //check for new group values
            myRef.child(String.valueOf(mid)).child("newGroups").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.i(tag, "inside checking newGroups values");
                    String newGroups = String.valueOf(snapshot.getValue()), groupId;

                    int messageTobeRead = 1, lastGroupIdNumber;

                    SharedPreferences.Editor edit = pref.edit();
                    if (!newGroups.equals("null") && !newGroups.equals("")) {

                        //add these new Groups in oldGroups value, so that when even the user logs in again after deleting this application
                        // these groups will be added again
                        final String finalNewGroups = newGroups;
                        myRef.child(String.valueOf(mid)).child("oldGroups").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String oldgroup = String.valueOf(snapshot.getValue());
                                if (oldgroup.length() > 4) {
                                    oldgroup = oldgroup + "," + finalNewGroups;
                                } else {
                                    oldgroup = finalNewGroups;
                                }
                                myRef.child(String.valueOf(mid)).child("oldGroups").setValue(oldgroup);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        while (newGroups.contains(",")) {
                            //getting groupIds from database and separate them with ',' in them...
                            groupId = newGroups.contains(",") ? newGroups.substring(0, newGroups.indexOf(",")) :
                                    newGroups;
                            lastGroupIdNumber = pref.getInt("lastGroupId", 0) + 1;

                            Log.d(tag, groupId + " this new group is added, lastGroupIdNumber = " + lastGroupIdNumber);

                            edit.putString(String.valueOf(lastGroupIdNumber), groupId);
                            //edit.putString(String.valueOf(lastGroupIdNumber), groupId);
                            //edit.putInt((lastGroupIdNumber) + "'sMessageTobeRead", 1);
                            edit.putInt("lastGroupId", lastGroupIdNumber);
                            edit.apply();

                            //this groupId is being added inside inApp database from addValueEventListener.

                            //add listener to this newly added group
                            myRef.child(groupId).child("temp_message" + messageTobeRead).addValueEventListener(
                                    listenerForGroups(groupId, messageTobeRead, lastGroupIdNumber));

                            newGroups = newGroups.substring(newGroups.indexOf(",") + 1);
                            Log.i(tag, "newGroup name = " + newGroups);
                        }

                        Log.i(tag, "after while loop newGroups value = " + newGroups);
                        if (newGroups.length() > 4 && !newGroups.contains(",")) {
                            groupId = newGroups;
                            Log.i(tag, "inside outside of while loop in listening for new Groups values");
                            lastGroupIdNumber = pref.getInt("lastGroupId", 0) + 1;
                            Log.d(tag, groupId + " this new group is added, lastGroupIdNumber = " + lastGroupIdNumber);
                            edit.putString(String.valueOf(lastGroupIdNumber), groupId);
                            //edit.putString(String.valueOf(lastGroupIdNumber), groupId);
                            //edit.putInt((lastGroupIdNumber) + "'sMessageTobeRead", 1);
                            //edit.putString(String.valueOf(pref.getInt("lastGroupId", 0) + 1), groupId);
                            edit.putInt("lastGroupId", lastGroupIdNumber);
                            edit.apply();

                            //add listener to this newly added group
                            myRef.child(groupId).child("temp_message" + messageTobeRead).addValueEventListener(
                                    listenerForGroups(groupId, messageTobeRead, lastGroupIdNumber));
                        }

                        myRef.child(String.valueOf(mid)).child("newGroups").setValue("");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.i(tag, "inside cancel of reading newGroups info from database");
                    error.getMessage();
                }
            });

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            //stopSelf(msg.arg1);
            //Log.i(tag, "service has been closed...");
        }
    }

    void getMessageFromFirebase(final DataSnapshot snapshot, final int messageTobeRead,
                                final String id, final int groupIdNumber, final ValueEventListener listener) {
        Log.i("service", "new messages found for this id");

        //code for getting and storing temp messages file to local directory will come here
        final File temp_messages = new File(root, "temp_messages");

        //now create temp_messages file at this location if does't exist and save downloaded file in this temp_messages
        try {
            if (!temp_messages.exists()) {
                temp_messages.createNewFile();
            }

            OnCompleteListener<FileDownloadTask.TaskSnapshot> storageListener = new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        try {
                            //now delete that temp_file
                            Log.d(tag, "File downloaded successfully");
                            if (id.contains("group")) {
                                String unreadIds = String.valueOf(snapshot.child("noOfUnreadIds").getValue());
                                if (!unreadIds.equals("null")) {
                                    if (Integer.parseInt(unreadIds) <= 1) {
                                        storageReference.child(id).child("temp_message" + messageTobeRead).delete();
                                        myRef.child(id).child("temp_message" + messageTobeRead).child("fileDeleted")
                                                .setValue(1);
                                    }

                                    //now reducing number of unreadId's by 1 as i have read the message
                                    myRef.child(id).child("temp_message" + messageTobeRead)
                                            .child("noOfUnreadIds")
                                            .setValue((Integer.parseInt(unreadIds) - 1));
                                }
                            } else {
                                //now update database to noNewMessages
                                storageReference.child(id).child("temp_messages").delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        myRef.child(id).child("newmessages").setValue("0");
                                        Log.i(tag, "new messages value is settled to 1");
                                        //myRef.child(id).child("writing").setValue("0");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        myRef.child(id).child("newmessages").setValue("0");
                                        Log.i(tag, "new messages value is settled to 1");
                                        //myRef.child(id).child("writing").setValue("0");
                                        e.getStackTrace();
                                    }
                                });
                            }

                            //now here read unread messages of rid's and save them in rid'stempmessages.txt file
                            //for testing purpose messages will be stored in public directory
                            //later it will be shifted to private directory.

                            //after downloading separate them by their senders
                            final BufferedReader temp_fileReader = new BufferedReader(new FileReader(temp_messages));

                            while ((line = temp_fileReader.readLine()) != null) {
                                Log.i(tag, "line = " + line);

                                final String writersId = line.length() <= 6 ? line : line.substring(0, 6);
                                final String finalWritersId = id.contains("group") ? id : writersId;

                                Log.i(tag, "writersId = " + finalWritersId);

                                //now get phone number and update application for new messages from this
                                //writers id *(it will be operated on a separate thread)
                                myRef.child(finalWritersId).addListenerForSingleValueEvent(
                                        new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                int numberOfNotifications = 0;
                                                String name = finalWritersId;
                                                try {
                                                    boolean idFound = false;

                                                    String info;
                                                    BufferedReader readerCheckUser;
                                                    if (id.contains("group")) {
                                                        readerCheckUser = new BufferedReader(
                                                                new FileReader(new File(root, "groupsInfo")));
                                                    } else {
                                                        readerCheckUser = new BufferedReader(
                                                                new FileReader(new File(root, "usersInfo")));
                                                    }

                                                    while ((info = readerCheckUser.readLine()) != null) {
                                                        Log.i(tag, "reading usersInfo info = " + info);
                                                        if (info.trim().equals(finalWritersId)) {
                                                            idFound = true;
                                                            break;
                                                        }
                                                    }

                                                    if (!idFound) {
                                                        Log.i("Connection service", "User not found in usersInfo.txt file getting its info");

                                                        if (id.contains("group"))
                                                            name = String.valueOf(snapshot.child("name").getValue());
                                                        else {
                                                            number = String.valueOf(snapshot.child("phone_number").getValue());
                                                            name = number;
                                                            Log.i(tag, "number of writer = " + number);
                                                        }

                                                        //this will check if number obtained from firebase is present in contact list if present then get its name (628) 372-0429
                                                        if (!id.contains("group")) {
                                                            String secondNumber = "+91" + number,
                                                                    thirdNumber = "(" + number.substring(0, 3) + ") " + number.substring(3, 6) + "-" + number.substring(6),
                                                                    fourthNumber = "+91" + thirdNumber;
                                                            numberOfNotifications = 1;

                                                            if (ContextCompat.checkSelfPermission
                                                                    (makeConnectionService.this, Manifest.permission.READ_CONTACTS)
                                                                    == PackageManager.PERMISSION_GRANTED) {
                                                                Log.d(tag, "checking this " + number + " in local contact list for number2 = " + secondNumber + "\nThird number = " + thirdNumber + "\nFourth number = " + fourthNumber);

                                                                Cursor phone1 = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                                                        new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                                                                        ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? or "
                                                                                + ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? or "
                                                                                + ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? or "
                                                                                + ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?",
                                                                        new String[]{number, secondNumber, thirdNumber, fourthNumber},
                                                                        null);

                                                                if (phone1 != null) {
                                                                    if (phone1.moveToFirst()) {
                                                                        name = phone1.getString(0);
                                                                    }
                                                                    phone1.close();
                                                                }
                                                            }
                                                        }
                                                        Log.i("ConnectionService", "Name or Number = " + name);

                                                        try {
                                                            //Files will written in order, first username next line then, id next line then, image if any then next line and next data
                                                            FileWriter userDataWriter;
                                                            if (id.contains("group")) {
                                                                userDataWriter = new FileWriter(
                                                                        new File(root, "groupsInfo"), true);
                                                            } else {
                                                                userDataWriter = new FileWriter(
                                                                        new File(root, "usersInfo"), true);
                                                            }

                                                            userDataWriter.write(finalWritersId + "\r\n" + name + "\r\n1\r\nperson\r\n");
                                                            userDataWriter.flush();
                                                            userDataWriter.close();
                                                            Log.i("Service", "new user entry done in file...");

                                                            final String finalName = name;
                                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Log.i(tag, "updating recyclerAdapter class");
                                                                    if (finalWritersId.contains("group"))
                                                                        fragmentUpdater2.add(finalWritersId, finalName, 1, "person", 0);
                                                                    else
                                                                        fragmentUpdater1.add(finalWritersId, finalName, 1, "person", 0);
                                                                }
                                                            });
                                                            updated = true;
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    } else { /*if (!updated)*/
                                                        //to update the number of messages field in usersInfo.txt file
                                                        //copy its data to tempUserInfo.txt then make usersInfo.txt empty then copy each line from
                                                        //tempUserInfo.txt to usersInfo.txt and when reached writersId then again copy one line
                                                        //then next instead of copy from tempUserInfo.txt get noOfMessages from database for this
                                                        //from this writersId and then write that, then copy paste other contents...
                                                        BufferedReader file;
                                                        if (id.contains("group")) {
                                                            file = new BufferedReader(
                                                                    new FileReader(rootPath + "groupsInfo"));
                                                        } else {
                                                            file = new BufferedReader(
                                                                    new FileReader(rootPath + "usersInfo"));
                                                        }

                                                        StringBuilder inputBuffer = new StringBuilder();
                                                        String usersData, rid;
                                                        int nom;
                                                        boolean foundUsersInfo = false;

                                                        Log.i(tag, "updating usersInfo file");
                                                        while ((usersData = file.readLine()) != null) {
                                                            Log.i(tag, "inside updating usersInfo file data = " + usersData);
                                                            if (usersData.equals(finalWritersId) && !foundUsersInfo) {
                                                                rid = usersData;
                                                                inputBuffer.append(rid).append("\r\n");

                                                                //this usersData contains name
                                                                usersData = file.readLine();
                                                                name = usersData;
                                                                inputBuffer.append(usersData).append("\r\n");

                                                                //this usersData contains number of messages
                                                                usersData = file.readLine();
                                                                nom = Integer.parseInt(usersData) + 1;
                                                                inputBuffer.append(nom).append("\r\n");

                                                                numberOfNotifications = nom;
                                                                Log.d(tag, "number of notifications for this rid = " + numberOfNotifications);
                                                                //this usersData contains image
                                                                usersData = file.readLine();
                                                                inputBuffer.append(usersData).append("\r\n");

                                                                final String finalRid = rid;
                                                                final int finalNom = nom;

                                                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        //now create recyclerViewDataType and notify recyclerAdapter
                                                                        Log.i(tag, "notifying recyclerAdapter class");
                                                                        if (finalWritersId.contains("group"))
                                                                            fragmentUpdater2.notifySingleDataUpdate(finalRid, finalNom);
                                                                        else
                                                                            fragmentUpdater1.notifySingleDataUpdate(finalRid, finalNom);
                                                                    }
                                                                });

                                                                foundUsersInfo = true;
                                                                continue;
                                                            }

                                                            //this usersData contains id
                                                            inputBuffer.append(usersData).append("\r\n");
                                                            //this usersData contains name
                                                            inputBuffer.append(file.readLine()).append("\r\n");
                                                            //this usersData contains number of messages
                                                            int unreadMessages = Integer.parseInt(file.readLine());
                                                            // numberOfNotifications = numberOfNotifications + unreadMessages;
                                                            inputBuffer.append(unreadMessages).append("\r\n");
                                                            //this usersData contains image
                                                            inputBuffer.append(file.readLine()).append("\r\n");
                                                        }
                                                        file.close();

                                                        FileOutputStream fileOut;
                                                        if (id.contains("group"))
                                                            fileOut = new FileOutputStream(rootPath + "groupsInfo");
                                                        else
                                                            fileOut = new FileOutputStream(rootPath + "usersInfo");
                                                        fileOut.write(inputBuffer.toString().getBytes());
                                                        fileOut.flush();
                                                        fileOut.close();
                                                        updated = false;
                                                    }

                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }

                                                //here update database showing that new messages have arrived for specific rid
                                                Log.d(tag, "writers Id = " + finalWritersId);

                                                int value = db.getData(finalWritersId);
                                                if (value != -1) {
                                                    db.updateData(finalWritersId, 1);
                                                    System.out.println("database updated");
                                                } else {
                                                    db.insertData(finalWritersId, 1);
                                                }

                                                //send notification to task bar
                                                Log.i(tag, "before sending notification, number of notifications = " + numberOfNotifications);
                                                if (numberOfNotifications > 0) {
                                                    if (finalWritersId.contains("group"))
                                                        sendNotification(1, "id", numberOfNotifications, name);
                                                    else
                                                        sendNotification(0, "id", numberOfNotifications, name);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.i(tag, "unable to obtain writers number from firebase storage");
                                                error.getMessage();
                                            }
                                        });

                                //creating reference to writersId'sTemp_messages.txt file
                                File tempMessagesFile = new File(root, finalWritersId + "'sTemp_messages.txt");
                                if (!tempMessagesFile.exists()) {
                                    tempMessagesFile.createNewFile();
                                }

                                //writing data in writersId'sTemp_messages.txt file
                                FileWriter writer = new FileWriter(tempMessagesFile, true);

                                String fileData = line + "\r\n" + temp_fileReader.readLine() + "\r\n";

                                writer.write(fileData);
                                writer.flush();
                                writer.close();
                                Log.i(tag, "data written in id'sTemp_messages.txt file data = " + fileData);
                            }

                            Log.i(tag, "now deleting internal temporary temp_messages file...");

                            //full file has been read now delete its content
                            FileWriter delete = new FileWriter(temp_messages);
                            delete.write("");
                            delete.flush();
                            delete.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Now start listening for another message of group as this reading has completed successfully
                        if (id.contains("group")) {
                            int newmessageTobeRead = messageTobeRead + 1;
                            SharedPreferences.Editor edit = pref.edit();
                            edit.putInt(groupIdNumber + "'sMessageTobeRead", newmessageTobeRead);
                            edit.apply();
                            Log.d(tag, "Starting new listener for temp_message" + newmessageTobeRead);

                            //start event listener for next message to be read
                            myRef.child(id).child("temp_message" + newmessageTobeRead).addValueEventListener(
                                    listenerForGroups(id, newmessageTobeRead, groupIdNumber));
                            reading = false;
                            //as this message has been read close event listener for this message
                           /* myRef.child(id).child("temp_message" + messageTobeRead)
                                    .removeEventListener(listener);*/
                        } else {
                            //Listen for any changes in firebase database
                            myRef.child(String.valueOf(mid)).addValueEventListener(
                                    new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull final DataSnapshot snapshot) {
                                            Log.i(tag, "inside listening of firebase database");
                                            //if new_messages in database is equal to 1 then go further
                                            if (String.valueOf(snapshot.child("newmessages").getValue()).equals("1") &&
                                                    String.valueOf(snapshot.child("writing").getValue()).equals("0")) {
                                                myRef.child(id).removeEventListener(this);
                                                //myRef.child(String.valueOf(mid)).child("writing").setValue("1");
                                                getMessageFromFirebase(snapshot, 0,
                                                        String.valueOf(mid), 0, this);
                                            } else {
                                                Log.d(tag, "condition not met in reading messages of mid");
                                /*else if (msg.arg2 == 0) {
                                stopSelf(msg.arg1);;
                            }*/
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.i(tag, "inside cancel of reading from database");
                                            error.getMessage();
                                        }
                                    });
                        }

                    } else {
                        if (!id.contains("group")) {
                            myRef.child(id).child("writing").setValue("0");
                        }
                        myRef.child(id).child("newmessages").setValue("0");
                        Log.e(tag, "download temp_messages file from firebase server is cancelled :\n" + task.getException());
                    }
                }
            };

            //download temp_message for group or normal users
            if (id.contains("group")) {
                storageReference.child(id).child("temp_message" + messageTobeRead).getFile(temp_messages)
                        .addOnCompleteListener(storageListener).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (!id.contains("group")) {
                            myRef.child(id).child("newmessages").setValue("0");
                            myRef.child(id).child("writing").setValue("0");
                        }
                        Log.e(tag, "unable to download temp_messages file from firebase server :\n" + e.getMessage());
                    }
                });
            } else {
                storageReference.child(id).child("temp_messages").getFile(temp_messages)
                        .addOnCompleteListener(storageListener).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(tag, "unable to download temp_messages file from firebase server :\n" + e.getMessage());
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(int tabPosition, String channelId, int numberOfNotifications, String name) {
        Log.d(tag, "inside message triggering block, started triggering message");
        Intent resultIntent = new Intent(c, MainActivity.class);
        if (tabPosition == 1)
            resultIntent.putExtra("tab", 1);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel notificationChannel = new
                    NotificationChannel(channelId, "messages", NotificationManager.IMPORTANCE_HIGH);

            //notificationChannel.setDescription("no sound");
            //notificationChannel.setSound(null, null);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(true);

            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(c.getApplicationContext(), "id");
        builder.setSmallIcon(R.mipmap.androidstudio);
        builder.setContentTitle("All Chat");
        builder.setGroup("allChatGroup");
        builder.setSortKey("Message received from " + name);
        //builder.setGroupSummary(true);
        if (tabPosition == 0) {
            builder.setContentText((numberOfNotifications == 1 ? "1 new message" :
                    numberOfNotifications + " new messages") + " from " + name + " received");
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText((numberOfNotifications == 1 ? "1 new message" :
                            numberOfNotifications + " new messages") +
                            " from " + name + " received\nclick here to view messages"));
        } else if (tabPosition == 1) {
            builder.setContentText((numberOfNotifications == 1 ? "1 new message" :
                    numberOfNotifications + " new messages") + " from " + name + " group received");
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText((numberOfNotifications == 1 ? "1 new message" :
                            numberOfNotifications + " new messages") +
                            " from " + name + " group received\nclick here to view messages"));
        }
        Uri path = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(path);
        //builder.setVibrate(new long[]{0, 500, 1000});
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                                                                            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                                                                builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
                                                                            else*/
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setAutoCancel(true);
        builder.setContentIntent(resultPendingIntent);


        if (notificationManager != null) {
            notificationManager.notify(0, builder.build());
            Log.i(tag, "notification sent successfully");
        }
    }

    ValueEventListener listenerForGroups(final String groupId, final int messageTobeRead, final int groupIdNumber) {

        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.i(tag, snapshot.getKey() + " file of " + groupId + " exists = " + snapshot.exists());
                if (snapshot.exists()) {
                    //as this message has been read close event listener for this message
                    myRef.child(groupId).child("temp_message" + messageTobeRead)
                            .removeEventListener(this);
                    //reading = true;

                    //Log.i(tag, "started listening for messages from database for groupIdNumber = " + groupIdNumber);
                    Log.i(tag, "new message is written by " + snapshot.child("writtenBy").getValue() + ", and my id = " + mid);
                    if (!String.valueOf(snapshot.child("writtenBy").getValue()).equals(String.valueOf(mid))
                            && !String.valueOf(snapshot.child("fileDeleted").getValue()).equals("1")) {
                        //start listening for message with below method
                        getMessageFromFirebase(snapshot, messageTobeRead, groupId, groupIdNumber, this);
                    } else {
                        //if that message is not writtenBy itself
                        Log.i(tag, "message " + messageTobeRead + " has been sent by you ");
                        SharedPreferences.Editor edit = pref.edit();
                        int newmessageTobeRead = messageTobeRead + 1;
                        edit.putInt(groupIdNumber + "'sMessageTobeRead", newmessageTobeRead);
                        edit.apply();
                        myRef.child(groupId).child("temp_message" + newmessageTobeRead).addValueEventListener(
                                listenerForGroups(groupId, newmessageTobeRead, groupIdNumber));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(tag, "error listing for group : " + error.getDetails());
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        Log.i(tag, "inside onBind");
        return myBinder;
        //return messenger.getBinder();
    }

    class myLocalBinder extends Binder {
        makeConnectionService getService() {
            return makeConnectionService.this;
        }
    }

    //update push notification id in firebase database
    void updateNotificationId(String id) {
        myRef.child(String.valueOf(mid)).child("notification_id").setValue(id);
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("startArguments");
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        looper = thread.getLooper();
        handler = new serviceHandler(looper);

        //messenger = new Messenger(handler);

        c = this.getApplicationContext();
        Log.i(tag, "inside onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(tag, "service started inside onStartCommand");
        pref = getApplicationContext().getSharedPreferences("myInfo", MODE_PRIVATE);
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = handler.obtainMessage();
        msg.arg1 = startId;
        /*try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/

        handler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    static public void setFragment1(updater c) {
        fragmentUpdater1 = c;
    }

    static public void setFragment2(updater c) {
        fragmentUpdater2 = c;
    }

    @Override
    public void onDestroy() {
        Log.i(tag, "closing service");
        connect = false;
        Message msg = handler.obtainMessage();
        msg.arg2 = 0;
        handler.sendMessage(msg);
        super.onDestroy();
       /* edit = pref.edit();
        edit.putBoolean("connected", false);
        edit.apply();*/
    }
}
