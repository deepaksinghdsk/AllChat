package com.example.clientserver.fragment2;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clientserver.FullscreenImageopener;
import com.example.clientserver.R;
import com.example.clientserver.addChats.addChats;
import com.example.clientserver.chatActivity;
import com.example.clientserver.fragment1andItsRecyclerAdapter.recyclerViewDataType;
import com.example.clientserver.fragment1andItsRecyclerAdapter.updater;
import com.example.clientserver.makeConnectionService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.clientserver.makeConnectionService.myRef;
import static com.example.clientserver.makeConnectionService.rootPath;
import static com.example.clientserver.makeConnectionService.storageReference;

class RecyclerAdapter extends
        RecyclerView.Adapter<RecyclerAdapter.ViewHolder> implements updater {

    private Context c;
    private String tag = "RecyclerAdapter";
    private List<recyclerViewDataType> userInfo = new ArrayList<>();

    RecyclerAdapter(Context c) {
        this.c = c;

        makeConnectionService.setFragment2(this); //this will initialise updater of makeConnectionService.java class
        addChats.setFragment2(this); //this will initialise updater of addChats.java class

        String line;
        // boolean fileCreated = false;
        int iteration = 1;
        //boolean inside = false;
        //Log.i("RecyclerAdapter.java", "image id of person is " + R.drawable.ic_person_black_24dp);

        File root = new File(android.os.Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/ChatApp/");
        if (!root.exists()) {
            boolean created = root.mkdir();
            System.out.println("created = " + created);
        }
        File file = new File(root, "groupsInfo");

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            /*if (!file.exists()) {
                boolean created = file.createNewFile();

                if (created) {
                    //Files will written in order, first username next line then, id next line then, image if any then next line and next data
                    System.out.println("writing userData(inside if)");
                    FileWriter writer = new FileWriter(file, true);
                    //inside = true;
                    //this condition is triggered if application is running for first time
                    writer.write("100001\r\nBoat1\r\n0\r\nperson\r\n");
                    writer.write("100002\r\nBoat2\r\n0\r\nperson\r\n");
                    writer.flush();
                }
            }*/

            //if userInfo variable is empty at start then fill it with info
            if (userInfo.isEmpty()) {
                int nom = 0;
                String name = null, img, id = "";
                //deleting previous record and entering new from userData File
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (iteration == 1) id = line;
                    else if (iteration == 2) name = line;
                    else if (iteration == 3) nom = Integer.parseInt(line);
                    else if (iteration == 4) {
                        img = line;
                        userInfo.add(new recyclerViewDataType(name, id, img, nom));
                        iteration = 0;
                    }
                    iteration++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final recyclerViewDataType info = userInfo.get(position);
        Log.i(tag, "noOfMessages = " + info.getNoOfMessages());
        holder.itemName.setText(info.getName());

        File picDir = new File(rootPath + "profilePictures");
        if (!picDir.exists()) {
            picDir.mkdir();
        }

        final File profilePic = new File(picDir, info.getRid() + ".jpg");
        if (info.getImage().equals("person")){
            holder.itemImage.setImageResource(R.drawable.ic_group);
        }else if (profilePic.exists()){
            holder.itemImage.setImageDrawable(Drawable.createFromPath(rootPath + "profilePictures/" + info.getRid() + ".jpg"));
        }else holder.itemImage.setImageResource(R.drawable.ic_group);

        /*if (profilePic.exists()) {
            //now check if info.getImage equals person, because if profile picture is deleted somehow than its size will be zero
            //and it will not be removed from directory, so always check if info.getImage equals person if yes than put ic_group as DP
            if (info.getImage().equals("person")) {
                Log.i("RecyclerAdapter", "profilePic file is empty...");
                holder.itemImage.setImageResource(R.drawable.ic_group);
            } else {
                holder.itemImage.setImageDrawable(Drawable.createFromPath(rootPath + "profilePictures/" + info.getRid() + ".jpg"));
            }
        }
        else {
            try {
                profilePic.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            holder.itemImage.setImageResource(R.drawable.ic_group);
        }*/

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot snapshot) {
                Log.i("RecyclerAdapter", "inside checking for profile pic");

                if (String.valueOf(snapshot.getValue()).equals("0")) {
                    holder.itemImage.setImageResource(R.drawable.ic_group);

                    if (profilePic.exists())
                        profilePic.delete();

                    updateUserInfoFile(info, "person");
                } else if (!String.valueOf(snapshot.getValue()).equals(info.getImage())) {

                    String rid = String.valueOf(info.getRid()), imageName = String.valueOf(snapshot.getValue());
                    Log.i("RecyclerAdapter", "profile pic exists for this rid = "
                            + rid + "\nimage name = " + imageName);

                    if (!profilePic.exists()) {
                        try {
                            profilePic.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    storageReference.child(rid).child(imageName)
                            .getFile(profilePic).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.i(tag, "image downloaded successfully now updating usersInfo data");
                                holder.itemImage.setImageDrawable(Drawable.createFromPath(rootPath + "profilePictures/" + info.getRid() + ".jpg"));
                                //now update profile pic name in userInfo file
                                updateUserInfoFile(info, String.valueOf(snapshot.getValue()));
                            } else {
                                Log.e(tag, "unable to download profile picture");
                                holder.itemImage.setImageResource(R.drawable.ic_group);
                                //now update profile pic name in userInfo file
                                updateUserInfoFile(info, "person");
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            holder.itemImage.setImageResource(R.drawable.ic_group);
                            e.printStackTrace();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i("RecyclerAdapter", "profilePic check in data base has been failed");
                error.getMessage();
            }
        };

        myRef.child(String.valueOf(info.getRid())).child("myProfilePic").addValueEventListener(valueEventListener);

        if (info.getNoOfMessages() > 0) {
            holder.noOfMessages.setText(String.valueOf(info.getNoOfMessages()));
            holder.noOfMessages.setVisibility(View.VISIBLE);
        } else {
            holder.noOfMessages.setVisibility(View.GONE);
        }

        holder.itemImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(tag, "image name = " + info.getImage());
                //open an activity here which will open this image in large view
                if (info.getImage().trim().toLowerCase().equals("person")) {
                    Toast.makeText(c, "No image is set by this user", Toast.LENGTH_SHORT).show();
                } else {
                    Intent i = new Intent(c, FullscreenImageopener.class);
                    i.putExtra("type", "path");
                    i.putExtra("path", rootPath + "profilePictures/" + info.getRid() + ".jpg");
                    c.startActivity(i);
                }
            }
        });

        holder.itemName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Position = " + position + " and Recipients id is :" + info.getRid());

                if (info.getImage().trim().toLowerCase().equals("person")) {
                    Intent chat = new Intent(c, chatActivity.class);
                    chat.putExtra("Rid", info.getRid());
                    chat.putExtra("name", info.getName());
                    chat.putExtra("image","0");
                    c.startActivity(chat);
                } else {
                    Intent chat = new Intent(c, chatActivity.class);
                    chat.putExtra("Rid", info.getRid());
                    chat.putExtra("name", info.getName());
                    chat.putExtra("image", rootPath + "profilePictures/" + info.getRid() + ".jpg");
                    c.startActivity(chat);
                }

                /*Intent chat = new Intent(c, chatActivity.class);
                chat.putExtra("Rid", info.getRid());
                chat.putExtra("name", info.getName());
                c.startActivity(chat);*/
            }
        });
    }

    private void updateUserInfoFile(recyclerViewDataType info, String imageName) {

        //update image data in current info object
        info.setImage(imageName);

        try {
            //now update image data in usersInfo file
            BufferedReader file = new BufferedReader(new FileReader(rootPath + "usersInfo"));
            StringBuilder inputBuffer = new StringBuilder();
            String usersData;
            boolean foundUsersInfo = false;

            while ((usersData = file.readLine()) != null) {
                if (usersData.equals(String.valueOf(info.getRid())) && !foundUsersInfo) {
                    inputBuffer.append(usersData).append("\r\n");

                    //this usersData contains name
                    usersData = file.readLine();
                    inputBuffer.append(usersData).append("\r\n");

                    //this usersData contains number of messages
                    usersData = file.readLine();
                    inputBuffer.append(usersData).append("\r\n");

                    //this usersData contains image
                    file.readLine();
                    inputBuffer.append(imageName).append("\r\n");

                    foundUsersInfo = true;
                    continue;
                }
                inputBuffer.append(usersData).append("\r\n");
            }
            file.close();

            FileOutputStream fileOut = new FileOutputStream(rootPath + "usersInfo");
            fileOut.write(inputBuffer.toString().getBytes());
            fileOut.flush();
            fileOut.close();
        } catch (IOException e) {
            e.getMessage();
        }
    }

    @Override
    public int getItemCount() {
        return userInfo.size();
    }

    @Override
    public void add(String id, String name, int noOfMessages, String image, int index) {
        userInfo.add(index, new recyclerViewDataType(name, id, image, noOfMessages));
        notifyItemInserted(index);
    }

    @Override
    public void notifySingleDataUpdate(String rid, int nom) {
        int index = 0;
        boolean found = false;

        for (recyclerViewDataType data : userInfo) {
            if (data.getRid().equals(rid)) {
                index = userInfo.indexOf(data);
                data.setNoOfMessages(nom);
                found = true;
            }
        }

        if (found)
            notifyItemChanged(index);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView itemImage;
        TextView itemName;
        TextView noOfMessages;
        //CardView cardView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.iv);
            itemName = itemView.findViewById(R.id.tv);
            noOfMessages = itemView.findViewById(R.id.nom);
            //cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
