package pupper115.pupper;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chris Yeh on 11/27/2017.
 * Unused by main program
 * Class was created as a util class for DogCard
 */

public class Utils {

    private static final String TAG = "Utils";

    // load profiles by converting from Json resources with Gson
    public static List<DogProfile> loadProfiles(Context context){
        try{
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            JSONArray array = new JSONArray(loadJSONFromAsset(context, "profiles.json"));
            List<DogProfile> profileList = new ArrayList<>();
            // convert Jsons into java objects and load into list
            for(int i=0;i<array.length();i++){
                DogProfile profile = gson.fromJson(array.getString(i), DogProfile.class);
                profileList.add(profile);
            }
            return profileList;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    // load info from Json assets
    private static String loadJSONFromAsset(Context context, String jsonFileName) {
        String json = null;
        InputStream is=null;
        try {
            // open, read and convert Json files
            AssetManager manager = context.getAssets();
            Log.d(TAG,"path "+jsonFileName);
            is = manager.open(jsonFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}