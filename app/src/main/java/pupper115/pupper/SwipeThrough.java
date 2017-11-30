package pupper115.pupper;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import pupper115.pupper.s3bucket.Constants;
import pupper115.pupper.s3bucket.Util;

/**
 * This page was first created by Sri, then the picture viewing and buttons for viewing next dog,
 * more info and the code behind both were added by Josh. This page was reviewed by Joseph.
 *
 * This is where the user is able to view the dogs, more info, add a dog, and go to settings. Very
 * straightforward. If the user press back, a message is displayed making sure the user meant to do
 * that.
 */

public class SwipeThrough extends AppCompatActivity {

    private Context context;

    //Added by Josh for use in the dog displaying
    // The S3 client used for getting the list of objects in the bucket
    private AmazonS3Client s3;
    private SimpleAdapter simpleAdapter;
    private ArrayList<HashMap<String, Object>> transferRecordMaps;
    private TransferUtility transferUtility;
    private String userName = "";
    private String password = "";
    //private int counter = 0;
    private boolean isNotPlaceholderDog = false;
    private String lastPicture = "init";
    private String penultimatePicture = "init";

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //Should be the main page
                    break;
                case R.id.navigation_createDog:
                    //Should be the upload page
                    transitionToNavActivity(CreateDogProfile.class);

                    break;
                case R.id.navigation_settings:
                    //Should be the settings page
                    transitionToNavActivity(SettingsActivity.class);

                    break;
            }
            return true;
        }

    };

    private void transitionToNavActivity(Class targetActivity){
        Intent intent = new Intent(context, targetActivity)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("userName", userName);
        intent.putExtra("password", password);

        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe_through);

        Intent data = getIntent();
        userName = data.getStringExtra("userName");
        password = data.getStringExtra("password");

        context = getApplication();
        transferUtility = Util.getTransferUtility(this);
        initData();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    //ADDED by Josh until bottom
    public void getMoreInfo(View v){
        if(isNotPlaceholderDog) {
            Intent intent = new Intent(context, DogProfile.class);
            intent.putExtra("dogImage", lastPicture);
            intent.putExtra("userName", userName);
            startActivity(intent);
        }
        else{
            CharSequence text = "This is the placeholder dog!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    public void getNextDog(View v) {
        //Context context = getApplicationContext();
        /*CharSequence text = "Loading the good doggo...";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();*/

        ImageView img = (ImageView) findViewById(R.id.doggo1);

        int range  = transferRecordMaps.size();
        Object [] array = transferRecordMaps.toArray();

        Random rand = new Random();

        int randomIndex = rand.nextInt(range);

        Object nextPicture = array[randomIndex];

        String pictureName = nextPicture.toString();
        pictureName = pictureName.substring(5, pictureName.length() - 1);

        while(lastPicture.equals(pictureName) || penultimatePicture.equals(pictureName))
        {
            randomIndex++;
            randomIndex = randomIndex % range;
            nextPicture = array[randomIndex];

            pictureName = nextPicture.toString();
            pictureName = pictureName.substring(5, pictureName.length() - 1);
        }
        penultimatePicture = lastPicture;
        lastPicture = pictureName;

        Picasso.with(this).load("https://s3.amazonaws.com/pupper-user-info/" + pictureName).noFade()
                .resize(1200, 1800).centerInside().into(img);

        //++counter;
        isNotPlaceholderDog = true;
    }

    private void initData() {
        // Gets the default S3 client.
        s3 = Util.getS3Client(SwipeThrough.this);
        transferRecordMaps = new ArrayList<HashMap<String, Object>>();
        TransferListener listener = new DownloadListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the file list.
        new GetFileListTask().execute();
    }

    //To get the dog pictures at the first instance of going to the page
    private class GetFileListTask extends AsyncTask<Void, Void, Void> {
        // The list of objects we find in the S3 bucket
        private List<S3ObjectSummary> s3ObjList;
        // A dialog to let the user know we are retrieving the files
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(SwipeThrough.this,
                    "Refreshing the doggos",
                    "Please Wait");
        }

        @Override
        protected Void doInBackground(Void... inputs) {
            // Queries files in the bucket from S3.
            s3ObjList = s3.listObjects(Constants.BUCKET_NAME).getObjectSummaries();
            transferRecordMaps.clear();
            for (S3ObjectSummary summary : s3ObjList) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("key", summary.getKey());
                transferRecordMaps.add(map);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
        }
    }

    private class DownloadListener implements TransferListener {
        // Simply updates the list when notified.
        @Override
        public void onError(int id, Exception e) {
            Log.e("DownloadListener", "onError: " + id, e);
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d("DownloadListener", String.format("onProgressChanged: %d, total: %d, current: %d",
                    id, bytesTotal, bytesCurrent));
        }

        @Override
        public void onStateChanged(int id, TransferState state) {
            Log.d("DownloadListener", "onStateChanged: " + id + ", " + state);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Leaving Pupper!")
                .setMessage("Are you sure you want to leave these dogs?!?!?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
