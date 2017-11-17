package pupper115.pupper;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.amazonaws.mobile.auth.userpools.CognitoUserPoolsSignInProvider;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.StartupAuthResultHandler;
import com.amazonaws.mobile.auth.core.StartupAuthResult;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class SplashActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash2);
        Context appContext = getApplicationContext();
        AWSConfiguration awsConfig = new AWSConfiguration(appContext);
        IdentityManager identityManager = new IdentityManager(appContext, awsConfig);
        IdentityManager.setDefaultIdentityManager(identityManager);
        identityManager.doStartupAuth(this, new StartupAuthResultHandler() {
            @Override
            public void onComplete(StartupAuthResult startupAuthResult) {
                // User identity is ready as unauthenticated user or previously signed-in user.
            }
        });



        // Go to the Login activity
        // Comment out to bypass login page
        ///*
        final Intent intent = new Intent(this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivityForResult(intent, 2);
        //*/

        //To bypass login screen, uncomment below
        /*
        final Intent intent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        this.startActivity(intent);
        this.finish();
        */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String userName = data.getStringExtra("userName");
        String password = data.getStringExtra("password");

        switch(resultCode) {
            case 2:
                // User is in the system
                final Intent intent2 = new Intent(this, SettingsActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent2.putExtra("userName", userName);
                intent2.putExtra("password", password);

                this.startActivity(intent2);
                this.finish();
                break;

            case 1:
                // New User
                final Intent intent3 = new Intent(this, NewUser.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                intent3.putExtra("userName", userName);
                intent3.putExtra("password", password);

                this.startActivityForResult(intent3, 2);

                //this.finish();
                break;
        }
    }
}
