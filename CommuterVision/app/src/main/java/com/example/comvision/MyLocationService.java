package com.example.comvision;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.gms.location.LocationResult;

public class MyLocationService extends BroadcastReceiver {

    public static final String  ACTION_PROCESS_UPDATE="com.example.comvision.UPDATE_LOCATION";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent !=null){
            final  String action =intent.getAction();
            if(ACTION_PROCESS_UPDATE.equals(action)){

                LocationResult result= LocationResult.extractResult(intent);
                if(result != null){

                    Location location =result.getLastLocation();
                    String location_string = new StringBuilder(""+location.getLatitude())
                            .append("/")
                            .append(location.getLongitude())
                            .toString();
                    try{
                        MainActivity.getInstance().updateTextView(location_string);
                        AutoRecordActivity.getInstance().updateTextView(location_string);
                    }
                    catch (Exception e){

                        System.out.println("YHA BACKGROUND SERVICE WALA KAAM KRNA H JB APP BND HO");

//                        Toast.makeText(context, "lat/long\n"+location_string,Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}
