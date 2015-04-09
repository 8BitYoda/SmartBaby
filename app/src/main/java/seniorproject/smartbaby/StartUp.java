package seniorproject.smartbaby;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
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
    private EditText eBDay; //birthday textbox
    private EditText eName; //name textbox
    Button sButton; //set button
    private DatePickerDialog datePicker;

    private SimpleDateFormat dateFormat;

    String date = "";
    String n; //name
    Boolean frstrun; //has app run before

    public static final String PREFS_SB = "SB_Pref_File";
    String by,bm,bd; //birth-year, birth-month, birth-day

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_start_up);

        SharedPreferences settings = getSharedPreferences(PREFS_SB, 0); //data stored on phone

        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US); //formats the date selected
        eBDay = (EditText) findViewById(R.id.editBirthday);
        eBDay.setInputType(InputType.TYPE_NULL);
        eBDay.requestFocus();

        frstrun = settings.getBoolean("fRun", false); //frstrun->first run, if never run set to false

        //gets date data from device memory and sets corresponding textbox
        bm = settings.getString("bMonth","0");
        bd = settings.getString("bDay","0");
        by = settings.getString("bYear","0");
        if(!bm.equals("0")&&!bd.equals("0")&&!by.equals("0")) {
            date = getMonth(Integer.parseInt(bm)) + " " + bd + ", " + by;
            eBDay.setText(date);
        }

        //gets name data from device memory and sets corresponding textbox
        eName = (EditText) findViewById(R.id.editName);
        n = settings.getString("fName","");
        if(!n.equals(""))
            eName.setText(n);

        sButton = (Button) findViewById(R.id.setButton);
        sButton.setOnClickListener(new OnClickListener() { //on click saves name and date data into device memory

            @Override
            public void onClick(View view) {
                SharedPreferences settings = getSharedPreferences(PREFS_SB, 0);
                SharedPreferences.Editor editor = settings.edit();
                if (!frstrun){ //if never run set date
                    date = bm + " " + bd + ", " + by;
                }
                if (date.equals(eBDay.getText().toString()) && n.equals(eName.getText().toString())){ //if a name and birthday have previsouly been set but not changed return to main screen
                        finish();
                } else if ((by.equals("0"))&& eName.getText().toString().equals("")){ //if no name or birthday was ever entered ask for both
                    Toast bToast = Toast.makeText(getApplicationContext(), "Please enter a Name and Birthday", Toast.LENGTH_SHORT);
                    bToast.setGravity(Gravity.BOTTOM,0,260);
                    bToast.show();
                } else if (eName.getText().toString().equals("")){ //if a birthday was entered but a name never was ask for a name
                    Toast nToast = Toast.makeText(getApplicationContext(), "Please enter a Name", Toast.LENGTH_SHORT);
                    nToast.setGravity(Gravity.BOTTOM,0,260);
                    nToast.show();
                } else if (Integer.parseInt(by)<=0||(!frstrun && date.equals(""))){ //if a name was entered but a birthday never was ask for it
                    Toast dToast = Toast.makeText(getApplicationContext(), "Please enter a Birthday", Toast.LENGTH_SHORT);
                    dToast.setGravity(Gravity.BOTTOM,0,260);
                    dToast.show();
                } else {
                    if (date.equals(eBDay.getText().toString())){ //if only the name was updated store and display confirmation message
                        editor.putString("fName", eName.getText().toString().trim());
                        editor.apply();
                        Toast.makeText(getBaseContext(),"Name Updated", Toast.LENGTH_SHORT).show();
                        finish();
                    } else if (n.equals(eName.getText().toString())){ //if only the birthday was updated store and display confirmation message
                        editor.putString("bDay", bd);
                        editor.putString("bMonth", Integer.toString(Integer.parseInt(bm) + 1));
                        editor.putString("bYear", by);
                        editor.apply();
                        Toast.makeText(getBaseContext(),"Birthday Updated", Toast.LENGTH_SHORT).show();
                        finish();
                    } else { //if both the name and birthday were updated store and display confirmation message
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

    private void setDateTimeField() { //sets the by, bm, and bd from what the user selects with the datepicker
        eBDay.setOnClickListener(this);

        Calendar newCalendar = Calendar.getInstance();
        datePicker = new DatePickerDialog(this, new OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance(); //gets current date
                newDate.set(year, monthOfYear, dayOfMonth); //sets current date to corresponding values
                eBDay.setText(dateFormat.format(newDate.getTime()));
                by=Integer.toString(year);
                bm=Integer.toString(monthOfYear);
                bd=Integer.toString(dayOfMonth);
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onClick(View view) { //when date text box clicked allows user to pick birthday using the datepicker
        if (view == eBDay) {
            datePicker.show();
        }
    }

    @Override
    public void onBackPressed(){ //event when devices back button is pressed
        if (date.equals(eBDay.getText().toString()) && n.equals(eName.getText().toString().trim()) && frstrun) //if its not the first run and no values have been changed do nothing go back to main screen
            finish();
        else { //if a value has been deleted ask user to fill out the form and press "Set"
            Toast backT = Toast.makeText(getBaseContext(), "Please fill out the information and press Set", Toast.LENGTH_SHORT);
            backT.setGravity(Gravity.BOTTOM, 0, 260);
            backT.show();
        }
    }

    public String getMonth(int month) { //formats month
        return new DateFormatSymbols().getShortMonths()[month-1];
    }
}
