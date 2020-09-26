package com.example.clientserver;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.example.clientserver.addChats.addChats;
import com.example.clientserver.addChats.contactsInfo;
import com.example.clientserver.loginActivity.profile;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private makeConnectionService myService;
    //private Messenger messenger;
    public boolean bind = false;
    private TabLayout tl;
    public static List<contactsInfo> userInfo = new ArrayList<>();
    private TabLayout.OnTabSelectedListener listener;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tl = findViewById(R.id.tl);
        intent = new Intent(this, makeConnectionService.class);

        /*Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {*/
        start();
            /*}
        });
        thread.start();*/

        final FloatingActionButton fab = findViewById(R.id.fab);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        tl.addTab(tl.newTab().setText("Chats"));
        tl.addTab(tl.newTab().setText("Groups"));
        /*tl.addTab(tl.newTab().setText("Call"));*/
        tl.setTabTextColors(Color.WHITE, Color.GREEN);
        tl.setElevation(10);
        tl.setSelectedTabIndicatorColor(Color.BLUE);

        final ViewPager vp = findViewById(R.id.vp);

        final viewPagerAdapter pa = new viewPagerAdapter(getSupportFragmentManager(), tl.getTabCount());
        vp.setAdapter(pa);

        vp.addOnPageChangeListener(new
                TabLayout.TabLayoutOnPageChangeListener(tl));

        listener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                vp.setCurrentItem(tab.getPosition());
                if (tab.getPosition() == 0) {
                    fab.setVisibility(View.VISIBLE);
                    fab.setImageResource(R.drawable.ic_person_add);
                } else if (tab.getPosition() == 1) {
                    fab.setVisibility(View.VISIBLE);
                    fab.setImageResource(R.drawable.ic_group_add);
                } else
                    fab.setVisibility(View.GONE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        };

        tl.addOnTabSelectedListener(listener);
    }

    void readContacts() {
        Cursor phone = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);

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
            Log.i("ConnectionService", "Unable to retrieve contact numbers");
        }
    }

    protected void start() {
        System.out.println("starting service");

        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS};

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            //add this too if app is unable to send sms
            // || ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
            request(112, permissions);
        } else {
            System.out.println(!makeConnectionService.connect);

            if (!makeConnectionService.connect)
                startService();

            bind(intent);
        }
    }

    public void addChat(View view) {
        int tabPosition = tl.getSelectedTabPosition();

        //start activity to add new chats
        Intent intent = new Intent(this, addChats.class);
        intent.putExtra("tabPosition", tabPosition);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    void startService() {
      /*  if (!makeConnectionService.connect) {
            System.out.println("starting start method");*/
        startService(intent);
        //}
        //boolean bind = bindService(intent, myConnection, 0/*Context.BIND_AUTO_CREATE*/);
        //Log.i("check", "bind status = " + bind);
        readContacts();
    }

    void bind(Intent intent) {
        bindService(intent, myConnection, 0);
    }

    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            System.out.println("setting onServiceConnected");
            makeConnectionService.myLocalBinder b = (makeConnectionService.myLocalBinder) service;
            myService = b.getService();
            //messenger = new Messenger(service);
            bind = true;

            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                Log.w("MainActivity", "getInstanceId failed", task.getException());
                                return;
                            }
                            String token = "";

                            //get new instance ID token
                            if (task.getResult() != null) {
                                token = task.getResult().getToken();
                                myService.updateNotificationId(token);
                            }
                            //Log and toast
                            //String msg=getString(R.str,token);
                            Log.d("MainActivity", token);
                        }
                    });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            System.out.println("disconnecting service.");
            // messenger = null;
            bind = false;
        }
    };

    void request(int code, String[] permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permission, code);
        } else {
            ActivityCompat.requestPermissions(this, permission, code);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        /*if (item.getItemId() == R.id.settings) {
            Log.d("main", "settings pressed");
            Intent i1 = new Intent(this, profile.class);
            startActivity(i1);
        } else if (item.getItemId() == R.id.about) {
            Log.d("main", "about pressed");
            Intent i2 = new Intent(this, about.class);
            startActivity(i2);
        }*/
        switch (item.getItemId()) {
            case R.id.settings:
                Log.d("main", "settings pressed");
                Intent i1 = new Intent(this, profile.class);
                startActivity(i1);
                break;
            case R.id.about:
                Log.d("main", "about pressed");
                Intent i2 = new Intent(this, about.class);
                startActivity(i2);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 112) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission is required to run this app properly.", Toast.LENGTH_SHORT).show();

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(false);
                builder.setTitle("Exit");
                builder.setMessage("Storage Permission is required to run this app properly, would you like to close this app?");
                //set listeners for dialog buttons
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //MainActivity.this.finish();
                        Intent i = new Intent(Intent.ACTION_MAIN);
                        i.addCategory(Intent.CATEGORY_HOME);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        start();
                    }
                });

                //create the alert dialog and show it
                builder.create().show();
            } else {
                /*if (!makeConnectionService.connect) {
                    System.out.println("starting start method");*/
                if (!makeConnectionService.connect)
                    startService();

                bind(intent);
                //}
            }
        }
        /*switch (requestCode) {
            case 112:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied some functions will not work...", Toast.LENGTH_SHORT).show();
                }
            case 113:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied some functions will not work...", Toast.LENGTH_SHORT).show();
                }
            case 114:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "All permissions are required to run app", Toast.LENGTH_SHORT).show();
                }
            case 115:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied some functions will not work...", Toast.LENGTH_SHORT).show();
                }
        }*/
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        //Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.chats) {
            TabLayout.Tab tab = tl.getTabAt(0);
            assert tab != null;
            tab.select();
        } else if (id == R.id.groups) {
            TabLayout.Tab tab = tl.getTabAt(1);
            assert tab != null;
            tab.select();
        } else if (id == R.id.about) {
            Intent i = new Intent(this, about.class);
            startActivity(i);
            /*TabLayout.Tab tab = tl.getTabAt(2);
            assert tab != null;
            tab.select();*/
        } else if (id == R.id.share) {
            Intent smsIntent = new Intent(Intent.ACTION_SEND);
            smsIntent.putExtra(Intent.EXTRA_TEXT, "Check out ChatApp, I use it to message the people I care about. Get it for free at https://github.com/deepaksinghdsk/AllChat/releases/download/v1.0/AllChat.apk");
            smsIntent.setType("text/plain");
            if (smsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(smsIntent);
            }
            /*TabLayout.Tab tab = tl.getTabAt(2);
            assert tab != null;
            tab.select();*/
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getData() != null || getIntent().getType() != null || getIntent().getExtras() != null) {
            finish();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle("Exit");
            builder.setMessage("Are you sure you want to leave...");
            //set listeners for dialog buttons
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //MainActivity.this.finish();

                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_HOME);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);

                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            //create the alert dialog and show it
            builder.create().show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bind)
            unbindService(myConnection);
        tl.removeOnTabSelectedListener(listener);
        System.out.println("unbounded");
    }
}
