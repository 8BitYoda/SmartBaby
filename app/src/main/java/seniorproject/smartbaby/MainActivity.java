package seniorproject.smartbaby;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.DatePicker;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity{
    Calendar calendar = Calendar.getInstance();

    BluetoothAdapter blueAdapt = null;
    BluetoothDevice mDevice;
    BluetoothSocket mSocket;

    OutputStream streamOutput;
    private InputStream streamInput;

    Thread workerThread;

    private TextView heartRate, temp, birthday, name;

    volatile boolean stopWorker;
    byte[] rBuffer;

    int rBufferPosition;
    String inData;
    String tempData;
    String bpmData;
    String bYear,bMonth,bDay,fName;
    double age;
    boolean firstRun;
    public static final String PREFS_SB = "SB_Pref_File";

    /*Baby alarm req
      //temp,hr-low,hr-high,hr-fever//
         {99.4, 80.0, 200.0, 220}, //3months
         {99.7, 70.0, 120.0, 200}, //1year
         {99.0, 60.0, 90.0, 200},  //3year
         {98.6, 60.0, 90.0, 200},  //5year
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState){ //when app is initially started up
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        SharedPreferences settings = getSharedPreferences(PREFS_SB, 0);
        firstRun = settings.getBoolean("fRun",false); //get from settings, defaults to false
        if(!firstRun) { //if never run
            Intent startUp = new Intent(this, StartUp.class);
            startActivity(startUp);
        }

        super.onCreate(savedInstanceState);
        pullData();
        setDates();

        blueAdapt = BluetoothAdapter.getDefaultAdapter();
        setContentView(R.layout.activity_main);

        heartRate = (TextView) findViewById(R.id.heartRate_text); //heat rate display
        temp = (TextView) findViewById(R.id.temp_text); //temperature display
        birthday = (TextView) findViewById(R.id.birthday_text); //birthday display
        name = (TextView) findViewById(R.id.name_text); //name display
        name.setText(fName);
        birthday.setText(bMonth + "/" + bDay + "/" + bYear);

        clearDisplays();

        isEnabled();
        try{
            find();
            open();
        }
        catch (IOException ex) { }

    }
    //date stuff//
    /*public void runDate(){ //runs date fragment
        DialogFragment newFragment = new DatePickerFragment(this);
        newFragment.show(getFragmentManager(), "datePicker");
    }*/
    //end//

    @Override
    protected void onResume(){
        super.onResume();

        if(blueAdapt==null || !blueAdapt.isEnabled()){ //checks for bluetooth enable, sends user to enable if not
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        isEnabled();
        try{
            find();
            open();
        }
        catch (IOException ex) { }


        pullData();
        name.setText(fName);
        birthday.setText(bMonth + "/" + bDay + "/" + bYear);
        clearDisplays();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_connect) { //connects to baby/arduino
            isEnabled();
            try{
                find();
                open();
            }
            catch (IOException ex) { }
            return true;
        }
        if (id == R.id.action_dial){ //opens phone
            Intent intent = new Intent(Intent.ACTION_DIAL);
            startActivity(intent);
        }
        if(id==R.id.action_camera){ //opens camera
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivity(intent);
        }
        if(id==R.id.action_settings){ //allows name and b-day edit
            Intent startUp = new Intent(this, StartUp.class);
            startActivity(startUp);
        }
        if(id==R.id.action_about){  //info about the awesome people that made this awesome app
            Toast aToast = Toast.makeText(getApplicationContext(), "Martin Hutchens & Matthew Timmons\nCharleston Southern University 2015", Toast.LENGTH_SHORT);
            LinearLayout layout = (LinearLayout) aToast.getView();
            if(layout.getChildCount()>0){
                TextView tv = (TextView) layout.getChildAt(0);
                tv.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL);
            }
            aToast.show();
        }
        if(id==R.id.action_set){//used to demonstrate notifications, delete for final build, *.xml
            pullData();
            setDates();
            birthday.setText(bMonth+"/"+bDay+"/"+bYear);
            checkVitals("101","60");
        }
        return super.onOptionsItemSelected(item);
    }


    //Adapted from Android Bluetooth example at "developer.android.com/guide/topics/connectivity/bluetooth.html"
    void isEnabled(){
        if(!blueAdapt.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, 0);
        }
    }

     void find(){
        blueAdapt = BluetoothAdapter.getDefaultAdapter();
        if(!blueAdapt.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, 0);
        }
        Set <BluetoothDevice> pairDevices = blueAdapt.getBondedDevices();
        if (pairDevices.size()>0){
            for(BluetoothDevice device : pairDevices){
                if(device.getName().equals("RNBT-397A")){
                    mDevice=device;
                    break;
                }
            }
        }
     }

    private void open() throws IOException{
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        if(mDevice!=null) {
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
            streamOutput = mSocket.getOutputStream();
            streamInput = mSocket.getInputStream();
        }
        beginListening();
    }

    private void beginListening(){
        final Handler handler = new Handler();
        final byte delimiter = 10; //ASCII code for /n
        stopWorker = false;
        rBufferPosition = 0;
        rBuffer = new byte[1024];

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()&&!stopWorker){
                    try{
                    int availBytes = streamInput.available();
                    if(availBytes>0){
                           byte[] packetBytes = new byte[availBytes];
                           streamInput.read(packetBytes);
                           for(int i=0;i<availBytes;i++){
                               byte b=packetBytes[i];
                               if(b == delimiter){
                                   byte[] eByte = new byte[rBufferPosition];
                                   System.arraycopy(rBuffer,0,eByte,0,eByte.length);
                                   inData = new String(eByte,"US-ASCII");
                                   rBufferPosition=0;
                                   //System.out.println(data); //possibly not needed anymore

                                   handler.post(new Runnable() {
                                       @Override
                                       public void run() {
                                           tempData = inData.substring(inData.lastIndexOf(" ")+1,inData.indexOf("|"));//insert pattern matcher here
                                           bpmData = inData.substring(inData.indexOf("|")+1);
                                           if (Integer.parseInt(bpmData)<200||Integer.parseInt(bpmData)>40) {
                                               temp.setText(tempData);
                                               heartRate.setText(bpmData);
                                               checkVitals(tempData, bpmData);
                                           }
                                       }
                                   });
                               }
                               else
                                   rBuffer[rBufferPosition++] = b;
                           }
                       }
                    }
                    catch(IOException ex){
                        stopWorker=true;
                    }
                }
            }
        });

        workerThread.start();
    }
    //end Android Bluetooth adaptation.

    private void setDates(){ //gets the current date and sets the age of the baby
        int cY,bY,cM,bM,monthDif,yearDif; //current Year,birth year,curr Month,birth month
        bY=Integer.parseInt(bYear);
        cY=calendar.get(Calendar.YEAR);
        cM=calendar.get(Calendar.MONTH);
        bM=Integer.parseInt(bMonth);
        monthDif=(cM+1)-bM; //difference between the current month and month baby was born
        yearDif=cY-bY; //difference between current year and baby's birth year

        if(yearDif == 0) { //if baby was born in the current year
            if(monthDif<=6) //check if the baby is less than 6months old
                age = monthDif/10; //turns age-in-months into decimal and sets to age
            else age = 1; //if older then 6months set age to 1year
        }
        else if(yearDif==1 && monthDif<0){ //if baby was born last year but isnt a year old
            if(monthDif+12<=6) //+12 to month difference, check if the baby is less than 6months old
                age = (monthDif+12)*.1; //turns age-in-months into decimal and sets to age
            else age = 1; //if older then 6months set age to 1year
        }
        else age = yearDif; //anything else use the year difference for the baby's age
    }

    private void checkVitals(String tmp, String bpm){ //checks baby's heart rate against the high
                                                      //and low values for the child's age
        Double t= Double.parseDouble(tmp);
        Double b= Double.parseDouble(bpm);
        if(b>200||b<40)
            return;
        if(age<0.7) {//baby 0-6m
            if(t>100.0||b<80.0||b>200.0){ //if temp > 100, or BPM are not between 80 and 200 send notification
                notifyMethod();
            }
        }
        else if(age>=0.7 && age<=2) {//baby 7m-2y
            if(t>100.0||b<70.0||b>120.0){ //if temp > 100, or BPM are not between 70 and 120 send notification
                notifyMethod();
            }
        }
        else {//baby 2y+
            if(t>100.0||b<60.0||b>90.0){ //if temp > 100, or BPM are not between 60 and 90 send notification
                notifyMethod();
            }
        }
    }



    private void clearDisplays(){ //clears displays
        heartRate.setText("---");
        temp.setText("---");
    }

    private void pullData(){ //retrieve name and birthday from device memory
        SharedPreferences settings = getSharedPreferences(PREFS_SB, 0);
        bMonth=settings.getString("bMonth","0"); //month of birth
        bDay=settings.getString("bDay","0"); //birth day-of-month
        bYear=settings.getString("bYear","0"); //year of birth
        fName= settings.getString("fName",""); //baby's name
    }

    public void notifyMethod(){//creates notification
        //Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); //default system alarm sound
        Uri beeps = Uri.parse("android.resource://seniorproject.smartbaby/raw/ekg"); //custom sound for notification
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notify = new Notification.Builder(this) //check baby notification
                .setContentTitle("Baby Alarm")
                .setContentText("Check on your baby!")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pIntent) //event on click
                .setVibrate(new long[]{1000, 100, 100, 100, 1000, 100, 100, 100, 1000, 100, 100, 100, 100})
                .setPriority(2) //max priority requiring user input/attention
                .setLights(Color.RED,1000,10)
                        //.setSound(sound) //default sound
                .setSound(beeps) //custom sound
                .build();
        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notify.flags|= Notification.FLAG_AUTO_CANCEL;
        nManager.notify(0,notify);
    }

    /* //custom date picker to select baby's birthday\\
    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener{
        MainActivity mActivity;
        public DatePickerFragment(MainActivity activity){
            mActivity = activity;
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(),this, year, month, day); //creates and returns instance of date picker
        }
        @Override
        public void onDateSet (DatePicker view, int year, int month, int day){
            SharedPreferences settings = mActivity.getSharedPreferences(PREFS_SB,0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("bDay",Integer.toString(day));
            editor.putString("bMonth",Integer.toString(month+1));
            editor.putString("bYear",Integer.toString(year));
            editor.apply();
        }
    }*/
}
