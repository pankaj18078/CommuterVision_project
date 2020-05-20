package com.example.comvision;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    DatabaseReference databaseReference;

    public ArrayList<Location> trafficSignals = new ArrayList<Location>();
    // 28.565406, 77.250019

    private static final int DISTANCE_THRESHOLD = 139;
    private boolean cameraIntentFlag = false;

    private static final int MY_REQUEST_CODE=7117; // any number
    Button btn_sign_out;
    Button btn_addSignal;
    List<AuthUI.IdpConfig> providers;
    private long k = 0;
    private Toast backtoast;

    TextView latitude;
    TextView longitude;
    static MainActivity instance;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;

    public static MainActivity getInstance(){
        return instance;
    }

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 100;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted)
            Log.e("PERM", "not granted");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        databaseReference = FirebaseDatabase.getInstance().getReference("user");


        //For SIGNOUT BUTTON

        btn_sign_out = (Button)findViewById(R.id.sign_out);

        btn_sign_out.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                //Logout
                AuthUI.getInstance()
                        .signOut(MainActivity.this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                btn_sign_out.setEnabled(false);
                                showSignInOptions();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                }) ;
            }
        });


        //Init provider
        providers= Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(), //Email Builder
                new AuthUI.IdpConfig.PhoneBuilder().build(), //Phone Builder
//                new AuthUI.IdpConfig.FacebookBuilder().build(), //Facebook Builder
                new AuthUI.IdpConfig.GoogleBuilder().build()  //Google Builder
        );

        showSignInOptions();

        //For GOOGLE LOCATIONS

        instance = this;
        latitude = (TextView)findViewById(R.id.latitude);
        longitude = (TextView)findViewById(R.id.longitude);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        updateLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this,"You must accept this location",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();


        // HARD CODEING OF LATITUDE AND LOGITTUDE
        hardCodeLatLang();

        // Add Traffic Signal
        btn_addSignal = (Button)findViewById(R.id.addSignalButton);

        btn_addSignal.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                // add Signal
                double lat = Double.parseDouble(latitude.getText().toString());
                double lng = Double.parseDouble(longitude.getText().toString());
                addLocation(lat, lng);

                for (Location e: trafficSignals) {
                    Log.e("SG", Double.toString(e.getLatitude()) + ' ' + Double.toString(e.getLongitude()));
                }
                String value = latitude.getText() + "/" + longitude.getText();

                cameraIntentFlag = true;

                Intent myIntent = new Intent(MainActivity.this, AutoRecordActivity.class);
                myIntent.putExtra("key", value); //Optional parameters
                MainActivity.this.startActivity(myIntent);

            }
        });

    }

    private void updateLocation() {
        buildLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }


        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());

    }

    private PendingIntent getPendingIntent() {

        Intent intent = new Intent(this,MyLocationService.class);
        intent.setAction(MyLocationService.ACTION_PROCESS_UPDATE);

        return PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

    }


    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2500);
        locationRequest.setFastestInterval(1500);
        locationRequest.setSmallestDisplacement(10f);  //ise change krk 50 meters krdena h

    }

    public void updateTextView(final String value){
        MainActivity.this.runOnUiThread(new Runnable() {
            // MATCH GPS LATITTUDE LONGITUDE
            @Override
            public void run() {
                String[] value1=value.split("/");

                latitude.setText(value1[0]);
                longitude.setText(value1[1]);

                Location currLocation = new Location("");
                currLocation.setLatitude(Double.parseDouble(value1[0]));//your coords of course
                currLocation.setLongitude(Double.parseDouble(value1[1]));

                Location traffic_sginal_latlng = closeToTrafficSignal(currLocation);
                Log.e("FLAG", String.valueOf(cameraIntentFlag));

                if (traffic_sginal_latlng!=null && !cameraIntentFlag) {
                    cameraIntentFlag = true;
                    String value2 = String.valueOf(traffic_sginal_latlng.getLatitude()) + "/" + String.valueOf(traffic_sginal_latlng.getLongitude());

                    Intent myIntent = new Intent(MainActivity.this, AutoRecordActivity.class);
                    myIntent.putExtra("key", value2); //Optional parameters
                    MainActivity.this.startActivity(myIntent);
                }

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        k = 0;

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot userSnapshot : dataSnapshot.getChildren()){
                    User user = userSnapshot.getValue(User.class);
                    assert user != null;
                    Location newTrafficSignal =  new Location("");
                    newTrafficSignal.setLatitude(user.getLatitude());//your coords of course
                    newTrafficSignal.setLongitude(user.getLongitude());
                    trafficSignals.add(newTrafficSignal);
                    Log.e("firebase:", " Added: "+Double.toString(newTrafficSignal.getLatitude()) + ' ' + Double.toString(newTrafficSignal.getLongitude()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onBackPressed() {

        if(k + 2000 > System.currentTimeMillis()) {
            //exit app to home screen
            backtoast.cancel();
            Intent homeScreenIntent = new Intent(Intent.ACTION_MAIN);
            homeScreenIntent.addCategory(Intent.CATEGORY_HOME);
            homeScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeScreenIntent);

        } else {
            backtoast = Toast.makeText(MainActivity.this,"Press back again to exist", Toast.LENGTH_SHORT);
            backtoast.show();
        }
        k = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(MainActivity.this, "RESUMED " + cameraIntentFlag, Toast.LENGTH_SHORT).show();
        cameraIntentFlag = false;
    }


    private void showSignInOptions(){
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTheme(R.style.MyTheme)
                .build(),MY_REQUEST_CODE
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == MY_REQUEST_CODE) {

            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                //Get User
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                //show email on toast
                if (user.getEmail() == null) {
                    Toast.makeText(this, "" + user.getEmail(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Welcome", Toast.LENGTH_SHORT).show();
                }
                btn_sign_out.setEnabled(true);
            }
            else {



                Toast.makeText(this, "" + response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    }


    private void hardCodeLatLang(){
//        addLocation(28.55173,77.24743);
//        addLocation(28.56008591,77.26167845);
//        addLocation(28.56280664,77.25055959);
//        addLocation(28.56250547,77.25046990);
//        addLocation(28.55459192,77.24271641);
//        addLocation(28.56208961,77.25634597);
//        addLocation(28.55787877,77.23953106);
//        addLocation(28.56037064,77.24583664);
//        addLocation(28.56003058,77.26173106);
//        addLocation(28.56534713,77.25000309);
//        addLocation(28.54597,77.25092);
//        addLocation(28.54917,77.24863);
//        addLocation(28.54878,77.24878);
//        addLocation(28.56209832,77.25671163);
//        addLocation(28.55973475,77.26188165);
//        addLocation(28.56524087,77.25003833);
//        addLocation(28.56266137,77.25075911);
//        addLocation(28.5520652,77.2470405);
//        addLocation(28.5490793,77.2623907);
//        addLocation(28.55189536,77.24698155);
//        addLocation(28.5489839,77.2623186);

    }


    private void addLocation(double lat, double lng) {

        boolean flag1=true;
        Location newTrafficSignal = new Location("");//provider name is unnecessary
        newTrafficSignal.setLatitude(lat);//your coords of course
        newTrafficSignal.setLongitude(lng);

        for (Location e: trafficSignals) {

            double distanceToSignal = e.distanceTo(newTrafficSignal);
            if(distanceToSignal < 15) {
                flag1 = false;
                break;
            }
        }

        if(flag1){
            String id = databaseReference.push().getKey();

            User user = new User(id, lat,lng);

            databaseReference.child(id).setValue(user);

            trafficSignals.add(newTrafficSignal);
            Toast.makeText(MainActivity.this,"New Lat-Long are Addded", Toast.LENGTH_SHORT).show();
            Log.e("New", " Added: "+Double.toString(lat) + ' ' + Double.toString(lng));

        }
        else{

            Toast.makeText(MainActivity.this,"These Lat-Long are already present", Toast.LENGTH_SHORT).show();
            Log.e("Exist", " Not Added: "+Double.toString(lat) + ' ' + Double.toString(lng));
        }


    }

    private Location closeToTrafficSignal(Location currLocation) {
        for (Location e: trafficSignals) {
            double distanceToSignal = e.distanceTo(currLocation);
            if (distanceToSignal <= DISTANCE_THRESHOLD) {
                return e;
            }
        }
        return null;
    }


}
