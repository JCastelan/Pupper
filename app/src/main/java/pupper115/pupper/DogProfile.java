package pupper115.pupper;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.squareup.picasso.Picasso;

import java.util.StringTokenizer;

import pupper115.pupper.dbmapper.repos.DogMapperRepo;
import pupper115.pupper.dbmapper.tables.TblDog;

public class DogProfile extends AppCompatActivity {
    private Context context;
    DynamoDBMapper dynamoDBMapper;
    DogMapperRepo dogMapRepo;
    TblDog dog;
    private dogTaskPull mPullTask = null;
    String dogImage = "";
    String bio = "";
    String userName = "";
    private DogRegisterTask mAuthTask = null;

    final AWSCredentialsProvider credentialsProvider = IdentityManager.getDefaultIdentityManager().getCredentialsProvider();
    AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dog_profile);
        Bundle extras = getIntent().getExtras();
        dogImage = extras.getString("dogImage");
        userName = extras.getString("userName");

        context = getApplication();
        AWSConfiguration awsConfig = null;
        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(awsConfig)
                .build();
        dogMapRepo = new DogMapperRepo(dynamoDBClient);

        mPullTask = new DogProfile.dogTaskPull();
        try {
            mPullTask.execute((Void) null).get();
        }
        catch(Exception e){
            Log.d("Exception thrown pulling db",e.getMessage());
        }

        setImage(dogImage);
        setData(dogImage);
    }

    private void setImage( String imageName){
        ImageView img = (ImageView) findViewById(R.id.imageView);
        Picasso.with(this).load("https://s3.amazonaws.com/pupper-user-info/" + imageName).noFade()
                .resize(1200, 1800).centerInside().into(img);
    }

    //Added by Josh 11/20
    private void setData(String image)
    {
        Log.d("Image", image);
        bio = "The current owner of ";
        bio = bio + dog.getDogName() + " is " + dog.getOwnerId() + ". ";
        bio = bio + dog.getDogName() + " is currently ";
        //Pull from dog table if the dog is up for adoption
        if(dog.getIsOwned() == false)
            bio = bio + "up for adoption! Contact " + dog.getOwnerId() + " for details";
        else
            bio = bio + "not up to be adopted. Sorry";

        bio = bio + ". Here is a quick bio of " + dog.getDogName() + " from " + dog.getOwnerId() + ": \r\n";
        //Pull bio about dog and add it to the string
        bio = bio + dog.getDogBio();

        String comments = dog.getComments();
        if(comments != "null")
        {
            bio = bio + "\n -------Comments------- \n";
            comments.replace("null", "");
            bio = bio + comments;
        }

        TextView name = (TextView) findViewById(R.id.textViewDogName);
        TextView info = (TextView) findViewById(R.id.textViewDogInfo);

        Button likes = (Button) findViewById(R.id.like);
        Double num = dog.getLikes();
        likes.setText("Likes: " + num.intValue());

        name.setText(dog.getDogName());
        info.setText(bio);
    }

    public void likeDog(View v)
    {
        Button likes = (Button) findViewById(R.id.like);
        Double num = dog.getLikes();
        dog.likeDog();
        ++num;
        likes.setText("Likes: " + num.intValue());
        likes.setClickable(false);
    }

    public void addComment(View v)
    {
        //Create an activity to write a comment
        Intent intent = new Intent(context, AddComment.class);
        intent.putExtra("userName", userName);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case 1:
                String comment = data.getStringExtra("comment");
                if(comment != null) {
                    dog.setComments(comment);
                    bio = bio + " \n" + comment;
                }
                mAuthTask = new DogRegisterTask(true, dog);
                mAuthTask.execute((Void) null);

                TextView info = (TextView) findViewById(R.id.textViewDogInfo);
                info.setText(bio);
                break;
        }
    }

    private class dogTaskPull extends AsyncTask<Void, Void, Boolean> {
        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(DogProfile.this);
            pDialog.setMessage("Loading... Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params){
            StringTokenizer tokens = new StringTokenizer(dogImage, ".");
            String first = tokens.nextToken(); //Dog owner ID
            Log.d("ID:", dogImage + "     " + first);

            dog = dogMapRepo.getDog(dogImage, first);
            if (dog != null){
                Log.d("Results", "WORKED!!!!");
                return true;
            }else{
                Log.d("Results", dogImage);
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            pDialog.dismiss();
        }
    }

    public class DogRegisterTask extends AsyncTask<Void, Void, Boolean> {

        private Boolean isRight = false;
        private TblDog dog = null;

        DogRegisterTask(Boolean isAllowed, TblDog t) {
            isRight = isAllowed;
            dog = t;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            if(isRight) {
                dynamoDBMapper.save(dog);
                return true;
            }
            else
                return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }

}
