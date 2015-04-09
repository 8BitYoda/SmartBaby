package seniorproject.smartbaby;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class StartUp extends Activity implements OnClickListener {
    private EditText eBDay;
    private EditText eName;
    Button sButton;
    private DatePickerDialog datePicker;

    private SimpleDateFormat dateFormat;

    String date = "";
    String n;
    Boolean frstrun;

    public static final String PREFS_SB = "SB_Pref_File";
    String by,bm,bd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_start_up);

        SharedPreferences settings = getSharedPreferences(PREFS_SB, 0);

        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        eBDay = (EditText) findViewById(R.id.editBirthday);
        eBDay.setInputType(InputType.TYPE_NULL);
        eBDay.requestFocus();

        frstrun = settings.getBoolean("fRun", false); //frstrun->first run

        bm = settings.getString("bMonth","0");
        bd = settings.getString("bDay","0");
        by = settings.getString("bYear","0");
        if(!bm.equals("0")&&!bd.equals("0")&&!by.equals("0")) {
            date = getMonth(Integer.parseInt(bm)) + " " + bd + ", " + by;
            eBDay.setText(date);
        }

        eName = (EditText) findViewById(R.id.editName);
        n = settings.getString("fName","");
        if(!n.equals(""))
            eName.setText(n);

        sButton = (Button) findViewById(R.id.setButton);
        sButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences settings = getSharedPreferences(PREFS_SB, 0);
                SharedPreferences.Editor editor = settings.edit();
                if (!frstrun){
                    date = bm + " " + bd + ", " + by;
                }
                if (date.equals(eBDay.getText().toString()) && n.equals(eName.getText().toString())){
                        finish();
                } else if ((by.equals("0"))&& eName.getText().toString().equals("")){
                    Toast bToast = Toast.makeText(getApplicationContext(), "Please enter a Name and Birthday", Toast.LENGTH_SHORT);
                    bToast.setGravity(Gravity.BOTTOM,0,260);
                    bToast.show();
                } else if (eName.getText().toString().equals("")){
                    Toast nToast = Toast.makeText(getApplicationContext(), "Please enter a Name", Toast.LENGTH_SHORT);
                    nToast.setGravity(Gravity.BOTTOM,0,260);
                    nToast.show();
                } else if (Integer.parseInt(by)<=0||(!frstrun && date.equals(""))){//date.equals("0 0, 0"))) {
                    Toast dToast = Toast.makeText(getApplicationContext(), "Please enter a Birthday", Toast.LENGTH_SHORT);
                    dToast.setGravity(Gravity.BOTTOM,0,260);
                    dToast.show();
                } else {
                    if (date.equals(eBDay.getText().toString())){
                        editor.putString("fName", eName.getText().toString().trim());
                        editor.apply();
                        Toast.makeText(getBaseContext(),"Name Updated", Toast.LENGTH_SHORT).show();
                        finish();
                    } else if (n.equals(eName.getText().toString())){
                        editor.putString("bDay", bd);
                        editor.putString("bMonth", Integer.toString(Integer.parseInt(bm) + 1));
                        editor.putString("bYear", by);
                        editor.apply();
                        Toast.makeText(getBaseContext(),"Birthday Updated", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        editor.putString("bDay", bd);
                        editor.putString("bMonth", Integer.toString(Integer.parseInt(bm) + 1));
                        editor.putString("bYear", by);
                        editor.putString("fName", eName.getText().toString());
                        editor.putBoolean("fRun", true);
                        editor.apply();
                        Toast.makeText(getBaseContext(), "Updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }
        });
        setDateTimeField();
    }
    private void setDateTimeField() {
        eBDay.setOnClickListener(this);

        Calendar newCalendar = Calendar.getInstance();
        datePicker = new DatePickerDialog(this, new OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                eBDay.setText(dateFormat.format(newDate.getTime()));
                by=Integer.toString(year);
                bm=Integer.toString(monthOfYear);
                bd=Integer.toString(dayOfMonth);
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onClick(View view) {
        if (view == eBDay) {
            datePicker.show();
        }
    }

    @Override
    public void onBackPressed(){
        if (date.equals(eBDay.getText().toString()) && n.equals(eName.getText().toString().trim()) && frstrun)
            finish();
        else {
            Toast backT = Toast.makeText(getBaseContext(), "Please fill out the information and press Set", Toast.LENGTH_SHORT);
            backT.setGravity(Gravity.BOTTOM, 0, 260);
            backT.show();
        }
    }

    public String getMonth(int month) {
        return new DateFormatSymbols().getShortMonths()[month-1];
    }
}
