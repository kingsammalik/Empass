package com.samapps.entire.empass;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.github.florent37.singledateandtimepicker.SingleDateAndTimePicker;
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.PermissionListener;

import org.joda.time.Duration;
import org.joda.time.LocalTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.start)
    TextView start;

    @BindView(R.id.end)
    TextView end;

    @BindView(R.id.calendar)
    TextView calendar;

    @BindView(R.id.message)
    EditText message;

    @BindView(R.id.title)
    EditText title;

    @BindView(R.id.root_view)
    ConstraintLayout root_view;

    Date stdate,endate;
    private MyCalendar m_calendars[];

    int SelectedCalID=-1;

    class MyCalendar {
        public String name;
        public String id;
        private MyCalendar(String _name, String _id) {
            name = _name;
            id = _id;
        }
        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        requestPermission1();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.save:
                if (title.getText().toString().trim().length()>0 && stdate!=null && endate!=null && SelectedCalID>1){
                    if (stdate.after(endate)){
                        Snackbar.make(root_view,"Starting Time should be earlier than End Time.",Snackbar.LENGTH_SHORT).show();
                    }
                    else{
                        requestPermission();
                    }

                }
                else {
                    Snackbar.make(root_view,"Please fill the Mandatory details.",Snackbar.LENGTH_SHORT).show();
                }

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestPermission1() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_CALENDAR)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        System.out.println("permission is granted");
                        // permission is granted
                        getCalendars();
                        for (MyCalendar myCalendar:m_calendars){
                            Log.e("tag","calender "+myCalendar.name+" "+myCalendar.id);
                        }
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        // check for permanent denial of permission
                        if (response.isPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(com.karumi.dexter.listener.PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }

                }).check();
    }

    @OnClick(R.id.start_card) void clickstart(){
        new SingleDateAndTimePickerDialog.Builder(MainActivity.this)
                .bottomSheet()
                .curved()
                .mainColor(getResources().getColor(R.color.colorPrimary))
                .displayListener(new SingleDateAndTimePickerDialog.DisplayListener() {
                    @Override
                    public void onDisplayed(SingleDateAndTimePicker picker) {
                        //retrieve the SingleDateAndTimePicker
                    }
                })

                .title("  START TIME")
                .listener(new SingleDateAndTimePickerDialog.Listener() {
                    @Override
                    public void onDateSelected(Date date) {
                        stdate=date;
                        start.setText(new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(date));
                    }
                }).display();
    }

    @OnClick(R.id.end_card) void clickend(){
        new SingleDateAndTimePickerDialog.Builder(MainActivity.this)
                .bottomSheet()
                .curved()
                .mainColor(getResources().getColor(R.color.colorPrimary))
                .displayListener(new SingleDateAndTimePickerDialog.DisplayListener() {
                    @Override
                    public void onDisplayed(SingleDateAndTimePicker picker) {
                        //retrieve the SingleDateAndTimePicker
                    }
                })

                .title("  END TIME")
                .listener(new SingleDateAndTimePickerDialog.Listener() {
                    @Override
                    public void onDateSelected(Date date) {
                        endate=date;
                        Log.e("tag","etdate "+new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(endate));
                        end.setText(new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(date));
                    }
                }).display();
    }

    @OnClick(R.id.calendar_card) void clickcal(){
        if (m_calendars!=null)
            showdialog("Calendars");
        else requestPermission1();
    }

    private void getCalendars() {
        String[] l_projection = new String[]{"_id", "calendar_displayName"};
        Uri l_calendars;
        if (Build.VERSION.SDK_INT >= 8) {
            l_calendars = Uri.parse("content://com.android.calendar/calendars");
        } else {
            l_calendars = Uri.parse("content://calendar/calendars");
        }
        Cursor l_managedCursor = this.managedQuery(l_calendars, l_projection, null, null, null);	//all calendars
        //Cursor l_managedCursor = this.managedQuery(l_calendars, l_projection, "selected=1", null, null);   //active calendars
        if (l_managedCursor.moveToFirst()) {
            m_calendars = new MyCalendar[l_managedCursor.getCount()];
            String l_calName;
            String l_calId;
            int l_cnt = 0;
            int l_nameCol = l_managedCursor.getColumnIndex(l_projection[1]);
            int l_idCol = l_managedCursor.getColumnIndex(l_projection[0]);
            do {
                l_calName = l_managedCursor.getString(l_nameCol);
                l_calId = l_managedCursor.getString(l_idCol);
                m_calendars[l_cnt] = new MyCalendar(l_calName, l_calId);
                ++l_cnt;
            } while (l_managedCursor.moveToNext());
        }
    }

    private void showdialog(final String title) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Your dialog code.
                android.app.AlertDialog.Builder builder1 = new android.app.AlertDialog.Builder(MainActivity.this,R.style.AppTheme);
                builder1.setCancelable(true);
                String[] checkarray=null;
                checkarray=getArrayfromList(m_calendars);
                //int checkedItem = null; // cow
                builder1.setItems(checkarray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectedCalID=Integer.parseInt(m_calendars[which].id);
                        calendar.setText(m_calendars[which].name);
                    }
                });


                android.app.AlertDialog alert11 = builder1.create();

                alert11.setTitle(title);
                //alert11.getWindow().getAttributes().windowAnimations= R.style.PauseDialogAnimation;
                alert11.show();
            }
        });

    }

    private String[] getArrayfromList(MyCalendar[] myCalendar) {
        String[] temp = new String[myCalendar.length];
        int index=0;
        for(MyCalendar myCalendar1:myCalendar){
            temp[index]=myCalendar1.name;
            index++;
        }
        return temp;
    }

    private void addtoCalendar() {
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        Calendar beginTime = Calendar.getInstance();
        beginTime.setTime(stdate);
        Calendar endTime = Calendar.getInstance();
        endTime.setTime(endate);
        values.put(CalendarContract.Events.DTSTART, beginTime.getTimeInMillis());
        values.put(CalendarContract.Events.DTEND, endTime.getTimeInMillis());
        values.put(CalendarContract.Events.TITLE, title.getText().toString());
        values.put(CalendarContract.Events.DESCRIPTION, message.getText().toString());

        TimeZone timeZone = TimeZone.getDefault();
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());

// Default calendar
        values.put(CalendarContract.Events.CALENDAR_ID, SelectedCalID);

        values.put(CalendarContract.Events.HAS_ALARM, 1);

// Insert event to calendar
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
        LocalTime start1 = new LocalTime(stdate);
                LocalTime now = new LocalTime();
                Duration p = new Duration(now.getMillisOfDay(), start1.getMillisOfDay());
                scheduleNotification(p.getStandardMinutes(),title.getText().toString(),message.getText().toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(root_view,"Event added Successfully.",Snackbar.LENGTH_SHORT).show();
                title.setText("");
                message.setText("");
                stdate=null;
                endate=null;
                start.setText("");
                end.setText("");
                SelectedCalID=-1;
                calendar.setText("");
            }
        });
    }


    private void scheduleNotification(long delay, String title, String message) {
        Log.e("dash","delay "+delay);
        Intent notificationIntent = new Intent(this, AlarmReceiver.class);
        notificationIntent.putExtra("title",title);
        notificationIntent.putExtra("message",message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 3, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        long futureInMillis = SystemClock.elapsedRealtime() + (delay*60000);
        Log.e("dash","mins to notify "+(futureInMillis/(1000*60)));
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }


    private void requestPermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.WRITE_CALENDAR)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        System.out.println("permission is granted");
                        // permission is granted
                        addtoCalendar();

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        // check for permanent denial of permission
                        if (response.isPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(com.karumi.dexter.listener.PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }

                }).check();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant this in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }
}
