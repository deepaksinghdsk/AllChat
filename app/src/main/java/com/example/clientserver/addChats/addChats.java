package com.example.clientserver.addChats;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clientserver.MainActivity;
import com.example.clientserver.R;
import com.example.clientserver.addNewUserInfo;
import com.example.clientserver.fragment1andItsRecyclerAdapter.updater;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.example.clientserver.makeConnectionService.db;
import static com.example.clientserver.makeConnectionService.rootPath;

public class addChats extends AppCompatActivity {

    private List<contactsInfo> userInfo = new ArrayList<>();
    private List<contactsInfo> usersToBeAddedInGroup = new ArrayList<>();
    private recyclerAdapter recyclerAdapter;
    private SearchView sv;
    private BottomSheetBehavior sheetBehavior;
    private FloatingActionButton floatingActionButton;

    //Database db;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    private static updater fragmentUpdater1, fragmentUpdater2;
    static boolean userAdded = false;
    String tag = "addChats";

    //this class is used for adding phone contacts to start chatting with them

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_chats);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

       /* ImageView icon = new ImageView(this);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        icon.setImageResource(R.drawable.icon);
        getSupportActionBar().setCustomView(icon, new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));*/

        RecyclerView rv = findViewById(R.id.list);

        //db = new Database(this, "clientDb", 1, "ridData");
        ConstraintLayout bottomSheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setPeekHeight(0, true);
        if (sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        floatingActionButton = findViewById(R.id.floatingActionButton2);

        if (MainActivity.userInfo.isEmpty()) {
            //if phone details information is empty in service then retrieve them
            Cursor phone = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

            if (phone != null) {
                String name, preNumber, number;
                while (phone.moveToNext()) {
                    name = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    preNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    number = !preNumber.trim().contains("+91") ?
                            preNumber.trim().replaceAll("\\(", "").replaceAll("\\)", "")
                                    .replaceAll("-", "").replaceAll("\\s+", "")
                            : preNumber.trim().replaceAll("\\s+", "").substring(3)
                            .replaceAll("\\(", "").replaceAll("\\)", "")
                            .replaceAll("-", "");
                    userInfo.add(new contactsInfo(name, number));
                }
                phone.close();
            } else {
                Log.i("AddChats", "Unable to retrieve contact numbers");
                Toast.makeText(this, "Permission not granted to read contacts", Toast.LENGTH_SHORT).show();
                back();
                return;
            }
        } else {
            userInfo.addAll(MainActivity.userInfo);
        }

        recyclerAdapter = new recyclerAdapter(userInfo, getApplicationContext());
        RecyclerView.LayoutManager manager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(manager);

        RecyclerView.Adapter adapter = recyclerAdapter;
        rv.setAdapter(adapter);

        RecyclerView.ItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(1000);
        animator.setRemoveDuration(1000);
        rv.setItemAnimator(animator);
        // sv.setOnQueryTextListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.addchatmenu, menu);

        MenuItem item = menu.findItem(R.id.search);
        sv = (SearchView) item.getActionView();
        sv.setQueryHint("Search...");
        sv.setImeOptions(EditorInfo.IME_ACTION_DONE);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                recyclerAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.i("addChats.onItemSelected", "back button is clicked by user....");
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.search:
                //sv.setBackgroundColor(getResources().getColor(android.R.color.white));
                sv.requestFocus();
                Log.i("addChats.onItemSelected", "search clicked...");
        }
        return super.onOptionsItemSelected(item);
    }

    static public void setFragment1(updater c) {
        fragmentUpdater1 = c;
    }

    static public void setFragment2(updater c) {
        fragmentUpdater2 = c;
    }

   /* public static void setUpdater(updater c) {
        //this is called by RecyclerAdapter.java in its constructor for future references
        updater = c;
    }*/

    public void addGroup(View view) {
        //create or add group of people added in userToBeAddedInGroup in firebase and in local groupsInfo file

        Button createGroupButton = findViewById(R.id.bottom_sheet_button);
        final EditText et = findViewById(R.id.group_name);

        sheetBehavior.setPeekHeight(140, true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        floatingActionButton.setVisibility(View.GONE);

        createGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et.length() > 1) {
                    Log.i(tag, "starting database listening");

                    myRef.child("last_group_id").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.i(tag, "inside listener");

                            //obtain group id from firebase then initialise groupId with it
                            String groupId = String.valueOf(snapshot.getValue()), allGroups, allOldGroups, groupName = et.getText().toString();
                            final String name = String.valueOf(et.getText());

                            if (groupId.equals("null")) {
                                groupId = "group100001";
                            } else groupId = "group" + (Integer.parseInt(groupId.substring(5)) + 1);
                            Log.d(tag, "groupId = " + groupId);

                            if (!usersToBeAddedInGroup.isEmpty()) {
                                myRef.child("last_group_id").setValue(groupId);

                                //using previousGroups info from usersToBeAddedInGroup, adding this group in previousGroups too
                                for (contactsInfo user : usersToBeAddedInGroup) {
                                    Log.d(tag, "previous groups = " + user.getPreviousGroups());
                                    allGroups = user.getPreviousGroups().isEmpty() ? user.getPreviousGroups().concat(groupId) :
                                            user.getPreviousGroups().concat("," + groupId);
                                    allOldGroups = user.getPreviousOldGroups().isEmpty() ? user.getPreviousOldGroups().concat(groupId) :
                                            user.getPreviousOldGroups().concat("," + groupId);
                                    myRef.child(user.getId()).child("newGroups").setValue(allGroups);
                                    myRef.child(user.getId()).child("oldGroups").setValue(allOldGroups);
                                }

                                //add this groups id in myInfo sharedPreferences so that it can be used to listen from firebase database
                                final String finalGroupId = groupId;
                                SharedPreferences pref = getApplicationContext().getSharedPreferences("myInfo", MODE_PRIVATE);
                                SharedPreferences.Editor edit = pref.edit();

                                //save this group id in shared preferences for reading purpose in makeConnectionService class
                                int groupNumber = pref.getInt("lastGroupId", 0) + 1;
                                edit.putString(String.valueOf(groupNumber), finalGroupId);
                                edit.putInt("lastGroupId", groupNumber);

                                //now add last message read of this group in myInfo file
                                edit.putInt(groupNumber + "'sMessageTobeRead", 1);
                                edit.apply();

                                //add this group inside inApp database
                                try {
                                    if (db.getData(finalGroupId) == -1) {
                                        boolean insertedInDatabase = db.insertData(finalGroupId, 0);
                                        Log.d(tag, "inserted in database = " + insertedInDatabase);
                                    }
                                } catch (Exception e) {
                                    e.getStackTrace();
                                }

                                //add this new group info in group tab and in file
                                addNewUser(name, finalGroupId, "group");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //calling update method of recycler adapter so that it updates its cardViews
                                        fragmentUpdater2.add(finalGroupId, name, 0, "person", 0);
                                    }
                                });

                                //create an entry in database with name of this groupId and put values in it,
                                myRef.child(groupId).setValue(new addNewUserInfo(groupName));
                                myRef.child(groupId).child("noOfGroupMembers").setValue(usersToBeAddedInGroup.size());
                                myRef.child(groupId).child("lastMessageNumber").setValue(0);

                                back();
                            } else {
                                Toast.makeText(new addChats(), "No user is selected! select at least 1 user.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(tag, "error in getting value from db error = " + error.getMessage());
                        }
                    });
                } else {
                    et.setError("Group name must be greater than 1! try again.");
                }
            }
        });
    }

    private class recyclerAdapter extends
            RecyclerView.Adapter<recyclerAdapter.Holder> implements Filterable {

        List<contactsInfo> userInfo;
        List<contactsInfo> allUserInfo;
        Context c;
        int tabPosition;

        recyclerAdapter(List<contactsInfo> userInfo, Context c) {
            this.c = c;
            this.userInfo = new LinkedList<>(userInfo);
            allUserInfo = new ArrayList<>(userInfo);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.contactslist, parent, false);
            tabPosition = getIntent().getIntExtra("tabPosition", 0);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final Holder holder, final int position) {
            final contactsInfo info = userInfo.get(position);

            holder.name.setText(info.getName());
            holder.number.setText(info.getNumber());

            if (tabPosition == 1) holder.tick.setVisibility(View.VISIBLE);

            if (info.getChecked()) info.setChecked(false);

            holder.tick.setChecked(info.getChecked());

            holder.ll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("addChats.Holder", "Inside setOnClickListener");

                    final String number = info.getNumber().trim();
                    Log.i(tag, "phone number from contact list = " + number);

                    if (!info.getUserExists()) {
                        Log.i(tag, "user does't exists, now checking it in database.");

                        //now check in firebase database if user with this phone number exists
                        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Log.i(tag, "inside onDataChange in single event listener");

                                //iterating through all user information in database
                                for (DataSnapshot data : snapshot.getChildren()) {
                                    if (String.valueOf(data.child("phone_number").getValue()).equals(number)) {
                                        Log.i(tag, "id : " + data.getKey() + "\nnumber in database = " + data.child("phone_number").getValue());
                                        final String idOfNumber = data.getKey();

                                        Log.i(tag, number + " found in database");
                                        if (tabPosition == 0) {

                                            //first check if user already exists in usersInfo file
                                               /* try {
                                                    File reader = new File(rootPath, "usersInfo");
                                                    if (!reader.exists()) {
                                                        reader.createNewFile();
                                                    }

                                                    BufferedReader usersInfoReader = new BufferedReader(new FileReader(reader));
                                                    String text;

                                                    while ((text = usersInfoReader.readLine()) != null) {
                                                        Log.d(tag, "obtained data from usersInfo file is : " + text);
                                                        if (text.equals(number)) {
                                                            alreadyExists = true;
                                                            break;
                                                        }
                                                    }
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }*/

                                            try {
                                                if (db.getData(idOfNumber) == -1) {
                                                    boolean insertedInDatabase = db.insertData(idOfNumber, 0);
                                                    Log.d(tag, "inserted in database = " + insertedInDatabase);
                                                } else {
                                                    Toast.makeText(c, "This user is already added", Toast.LENGTH_SHORT).show();
                                                    userAdded = true;
                                                    //get out of this activity
                                                    back();
                                                    //break out of for loop
                                                    break;
                                                }
                                            } catch (Exception e) {
                                                e.getStackTrace();
                                            }

                                            addNewUser(info.getName(), idOfNumber, "chat");
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //calling update method of recycler adapter so that it updates its cardViews
                                                    fragmentUpdater1.add(idOfNumber, info.getName(), 0, "person", 0);
                                                }
                                            });

                                            //set userAdded to true so that sms will not be sent to current number as
                                            //it already exists in firebase database.
                                            userAdded = true;

                                            //get out of this activity
                                            back();

                                            //break out of for loop
                                            break;
                                        } else if (tabPosition == 1 && usersToBeAddedInGroup.size() < 100) {
                                            //if this user is already added in group then remove it else add it
                                            if (info.getChecked()) {
                                                Log.d(tag, "inside tick is already checked");
                                                holder.tick.setChecked(false);
                                                info.setChecked(false);

                                                for (contactsInfo info : usersToBeAddedInGroup) {
                                                    if (info.getId().equals(idOfNumber))
                                                        usersToBeAddedInGroup.remove(info);
                                                }

                                                if (usersToBeAddedInGroup.size() < 3) {
                                                    floatingActionButton.setVisibility(View.GONE);
                                                }
                                            } else {
                                                boolean exists = false;
                                                Log.d(tag, "inside tick is not checked");
                                                for (contactsInfo info : usersToBeAddedInGroup) {
                                                    if (info.getNumber().equals(String.valueOf(holder.number.getText()))) {
                                                        exists = true;
                                                        break;
                                                    }
                                                }

                                                if (!exists) {
                                                    holder.tick.setChecked(true);
                                                    info.setChecked(true);
                                                    usersToBeAddedInGroup.add(new contactsInfo(String.valueOf(holder.name.getText()),
                                                            String.valueOf(holder.number.getText()),
                                                            String.valueOf(data.child("newGroups").getValue()),
                                                            String.valueOf(data.child("oldGroups").getValue()),
                                                            idOfNumber));
                                                    Log.d(tag, "previous groups value = " + data.child("newGroups").getValue());
                                                    if (floatingActionButton.getVisibility() == View.GONE)
                                                        floatingActionButton.setVisibility(View.VISIBLE);
                                                } else
                                                    Toast.makeText(c, "User is already added", Toast.LENGTH_SHORT).show();
                                            }

                                            //set userAdded to true so that sms will not be sent to current number as
                                            //it already exists in firebase database.
                                            userAdded = true;

                                            //break out of the main for loop
                                            break;
                                        } else {
                                            Toast.makeText(c, "A group can't contain more than 100 participants", Toast.LENGTH_SHORT).show();

                                            //set userAdded to true so that sms will not be sent to current number as
                                            //it already exists in firebase database.
                                            userAdded = true;

                                            //break out of the main for loop
                                            break;
                                        }
                                    }
                                }

                                //if user not exists in firebase database than send that contact a sms
                                if (!userAdded) {
                                    Toast.makeText(c, "No such user found", Toast.LENGTH_SHORT).show();
                                    Log.d(tag, "No such user found, user info = " + info.getName());

                                    AlertDialog.Builder builder = new AlertDialog.Builder(addChats.this);
                                    builder.setCancelable(true);
                                    builder.setTitle("Send this contact SMS to start using this application?");
                                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                                            //smsIntent.putExtra(Intent.EXTRA_TEXT, "Check out ChatApp, I use it to message the people I care about. Get it for free at Google.com");
                                            //smsIntent.setType("text/plain");
                                            smsIntent.setData(Uri.parse("smsto:" + number));
                                            smsIntent.putExtra("sms_body", "Check out AllChat, I use it to message the people I care about. Get it for free at https://github.com/deepaksinghdsk/AllChat/releases/download/v1.0/AllChat.apk");
                                            if (smsIntent.resolveActivity(getPackageManager()) != null) {
                                                Log.e(tag, "Action_sendTo Intent is resolved");
                                                startActivity(smsIntent);
                                            } else {
                                                Log.e(tag, "Can't resolve app for Action_SendTo Intent");
                                            }
                                        }
                                    });
                                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            ;
                                        }
                                    });
                                    builder.create().show();
                                }

                                //set this userAdded to false so that next time we can check if that user exists or not
                                userAdded = false;
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("tag", "unable to read data from firebase database, check below logs");
                                error.getDetails();
                            }
                        });
                    } else {
                        Toast.makeText(c, "This number already exists in chatActivity.", Toast.LENGTH_SHORT).show();
                    }

                    //Toast.makeText(c, "Internet is slow, this number is already being checked in database.", Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return userInfo.size();
        }

        @Override
        public Filter getFilter() {
            return dataFilter;
        }

        private Filter dataFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<contactsInfo> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(allUserInfo);
                } else {
                    String filterString = constraint.toString().toLowerCase().trim();
                    for (contactsInfo item : allUserInfo) {
                        if (item.getName().toLowerCase().contains(filterString)) {
                            filteredList.add(item);
                        } else if (item.getNumber().contains(filterString)) {
                            filteredList.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                userInfo.clear();
                userInfo.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };

        class Holder extends RecyclerView.ViewHolder {

            TextView name;
            TextView number;
            LinearLayout ll;
            CheckBox tick;

            Holder(@NonNull View itemView) {
                super(itemView);
                ll = itemView.findViewById(R.id.fullContainer);
                name = itemView.findViewById(R.id.name);
                number = itemView.findViewById(R.id.phoneNumber);
                tick = itemView.findViewById(R.id.checkbox);
            }
        }
    }

    private void back() {
        NavUtils.navigateUpFromSameTask(this);
    }

    public void addNewUser(String name, String idOfNumber, String type) {
        try {
            //Files will written in order, first username next line then, id next line then, image if any then next line and next data
            File root = new File(rootPath);
            File file;
            if (type.equals("chat"))
                file = new File(root, "usersInfo");
            else file = new File(root, "groupsInfo");

            FileWriter userDataWriter = new FileWriter(file, true);

            userDataWriter.write(idOfNumber + "\r\n" + name + "\r\n0\r\nperson\r\n");
            userDataWriter.flush();
            userDataWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

