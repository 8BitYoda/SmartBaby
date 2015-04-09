//Adapted from Ravi Tamada's Android Splash Screen Example http://www.androidhive.info/2013/07/how-to-implement-android-splash-screen-2/

package seniorproject.smartbaby;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


public class SplashActivity extends Activity {

    private final int SPLASH_LENGTH = 700; //length that the splash is on the screen

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() { //after delay go to main page
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }, SPLASH_LENGTH);
    }
}