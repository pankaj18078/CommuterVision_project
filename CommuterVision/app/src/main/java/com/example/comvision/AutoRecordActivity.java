package com.example.comvision;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.comvision.media.CameraHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AutoRecordActivity extends AppCompatActivity {

    private Camera mCamera;
    private TextureView mPreview;
    private MediaRecorder mMediaRecorder;
    private File mOutputFile;

    private boolean violation_flag = false;
    private boolean isRecording = false;
    private static final String TAG = "Recorder";
    private Button captureButton;
    private Location oldLocation = new Location("");

    static AutoRecordActivity instance;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int DISTANCE_THRESHOLD = 158; //CHANGE THE THRESHOLD WHEN DONE WITH TESTING


    public static AutoRecordActivity getInstance() {
        return instance;
    }

    // Requesting permission to RECORD_AUDIO GPS
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 100;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION};

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
        setContentView(R.layout.activity_auto_record);

        // Accessing the value
        Intent myintent = getIntent();
        String value = myintent.getStringExtra("key");
        String[] value1 = value.split("/");
        oldLocation.setLatitude(Double.parseDouble(value1[0]));//your OLD coordinatess
        oldLocation.setLongitude(Double.parseDouble(value1[1]));

        //For GOOGLE LOCATIONS

        instance = this;

//        Dexter.withActivity(this)
//                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
//                .withListener(new PermissionListener() {
//                    @Override
//                    public void onPermissionGranted(PermissionGrantedResponse response) {
//                        updateLocation();
//                    }
//
//                    @Override
//                    public void onPermissionDenied(PermissionDeniedResponse response) {
//                        Toast.makeText(AutoRecordActivity.this,"You must accept this location",Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
//
//                    }
//                }).check();


        //camra recording

        mPreview = (TextureView) findViewById(R.id.surface_view);
        captureButton = (Button) findViewById(R.id.button_capture);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
//        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_VIDEO_PERMISSION);

//        captureButton.setOnClickListener(new View.OnClickListener(){
//
//            @Override
//            public void onClick(View v) {
//                Log.e("CLICK", "record button click");
//                if (isRecording) {
//                    // BEGIN_INCLUDE(stop_release_media_recorder)
//
//                    // stop recording and release camera
//                    mMediaRecorder.stop();  // stop the recording
//                    releaseMediaRecorder(); // release the MediaRecorder object
//                    mCamera.lock();         // take camera access back from MediaRecorder
//
//                    // inform the user that recording has stopped
//                    setCaptureButtonText("Capture");
//                    isRecording = false;
//                    releaseCamera();
//                    Log.e("Video file path", CameraHelper.getOutputMediaFile(
//                            CameraHelper.MEDIA_TYPE_VIDEO).toString());
//                    finish();
//                    // END_INCLUDE(stop_release_media_recorder)
//
//                } else {
//
//                    // BEGIN_INCLUDE(prepare_start_media_recorder)
//
//                    new MediaPrepareTask().execute(null, null, null);
//
//                    // END_INCLUDE(prepare_start_media_recorder)
//
//                }
//            }
//        });
        captureButton.performClick();
    }


    public void onCaptureClick(View view) {
        Log.e("CLICK", "record button click");
        if (isRecording) {
            // BEGIN_INCLUDE(stop_release_media_recorder)

            // stop recording and release camera
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            // inform the user that recording has stopped
            setCaptureButtonText("Capture");
            isRecording = false;
            releaseCamera();
            Log.e("Video file path", mOutputFile.getName());

            AlertDialog.Builder builder1 = new AlertDialog.Builder(view.getContext());

            // alert dialog box
            String[] listItems = new String[] {"Violation Detected", "Violation NOT detected"};

            builder1.setTitle("Want to Upload this Video?");
            builder1.setCancelable(false);


            builder1.setSingleChoiceItems(listItems, 1,
                    new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.e("NAME", String.valueOf(which));
                            if (which == 0)
                                violation_flag = true;
                            else
                                violation_flag = false;
                            // If the user checked the item, add it to the selected items

                        }
                    });


            builder1.setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            if (violation_flag) {
                                String fileName = mOutputFile.getName();
                                int extensionIndex = fileName.lastIndexOf(".");
                                String newFileName = fileName.substring(0, extensionIndex) + "_violation" + fileName.substring(extensionIndex);
                                File currentFile = new File(mOutputFile.getParent(), fileName);


                                File flaggedFile = new File(mOutputFile.getParent(), newFileName);

                                boolean success = false;
                                if(currentFile.exists()) {
                                    Log.e("NAME", "file exists");
                                    success = currentFile.renameTo(flaggedFile);
                                }
                                else {
                                    Log.e("NAME", "file not exist");
                                }
//                            try {
//                                Files.move(Paths.get(currentFile.getAbsolutePath()), Paths.get(flaggedFile.getAbsolutePath()),
//                                        StandardCopyOption.REPLACE_EXISTING);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
                                mOutputFile = flaggedFile;

                                Log.e("NAME", fileName);
                                Log.e("NAME", newFileName);
                                Log.e("NAME", flaggedFile.getAbsolutePath());
                                Log.e("NAME", mOutputFile.getAbsolutePath());
                                Log.e("NAME", mOutputFile.getParent());
                                Log.e("NAME", String.valueOf(success));

//                            Log.e("NAME", String.valueOf(success));
                            }

                            dialog.cancel();

                            Intent mServiceIntent = new Intent(AutoRecordActivity.this, VideoUploadService.class);
                            mServiceIntent.putExtra("path", mOutputFile.getAbsolutePath());
                            mServiceIntent.putExtra("name", mOutputFile.getName());
                            mServiceIntent.putExtra("violation", String.valueOf(violation_flag));
//                            mServiceIntent.putExtra("data", byteArray);
                            startService(mServiceIntent);

                            Log.e("FIN", "going to end activity.");
                            finish();
                        }
                    });

            builder1.setNegativeButton(
                    "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Log.e("FIN", "going to end activity.");
                            finish();
                        }
                    });



            final AlertDialog alert11 = builder1.create();
            alert11.show();

            final Timer t = new Timer();
            t.schedule(new TimerTask() {
                public void run() {
                    alert11.dismiss(); // when the task active then close the dialog
                    t.cancel(); // also just top the timer thread, otherwise, you may receive a crash report
                    finish();
                }
            }, 10000);


            // END_INCLUDE(stop_release_media_recorder)

        } else {

            // BEGIN_INCLUDE(prepare_start_media_recorder)
            new MediaPrepareTask().execute(null, null, null);
//

            // END_INCLUDE(prepare_start_media_recorder)

        }
    }


//    private void updateLocation() {
//        buildLocationRequest();
//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
//            return;
//        }
//
//
//        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
//
//    }
//
//    private PendingIntent getPendingIntent() {
//
//        Intent intent = new Intent(this,MyLocationService.class);
//        intent.setAction(MyLocationService.ACTION_PROCESS_UPDATE);
//
//        return PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
//
//    }


//    private void buildLocationRequest() {
//        locationRequest = new LocationRequest();
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        locationRequest.setInterval(5000);
//        locationRequest.setFastestInterval(3000);
//        locationRequest.setSmallestDisplacement(10f);  //ise change krk 50 meters krdena h
//
//    }

    public void updateTextView(final String value) {
        AutoRecordActivity.this.runOnUiThread(new Runnable() {
            // MATCH GPS LATITTUDE LONGITUDE
            @Override
            public void run() {
                String[] value1 = value.split("/");


                Location currLocation = new Location("");
                currLocation.setLatitude(Double.parseDouble(value1[0]));//your coords of course
                currLocation.setLongitude(Double.parseDouble(value1[1]));

                if (isRecording) {
                    double distance = oldLocation.distanceTo(currLocation);
                    if (distance >= DISTANCE_THRESHOLD) {
                        captureButton.performClick();
                    }
                }

            }
        });
    }


    private void setCaptureButtonText(String title) {
        captureButton.setText(title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // if we are using MediaRecorder, release it first
        releaseMediaRecorder();
        // release the camera immediately on pause event
        releaseCamera();
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }

//    private boolean prepareVideoRecorder(){
//
//        // BEGIN_INCLUDE (configure_preview)
//        mCamera = CameraHelper.getDefaultCameraInstance();
//        mCamera.setDisplayOrientation(90);
//
//        // We need to make sure that our preview and recording video size are supported by the
//        // camera. Query camera to find all the sizes and choose the optimal size given the
//        // dimensions of our preview surface.
//        Camera.Parameters parameters = mCamera.getParameters();
//        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
//        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
//        Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
//                mSupportedPreviewSizes, mPreview.getWidth(), mPreview.getHeight());
//
//        // AUTOFOCUS
//        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//        parameters.set("cam_mode", 1 ); //not sure why this arcane setting is required. found this in another post on Stackoverlflow
//
//
//        // Use the same size for recording profile.
//        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
//        profile.videoFrameWidth = optimalSize.width;
//        profile.videoFrameHeight = optimalSize.height;
//
//        // likewise for the camera object itself.
//        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
//        mCamera.setParameters(parameters);
//        try {
//            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
//            // with {@link SurfaceView}
//            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
//        } catch (IOException e) {
//            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
//            return false;
//        }
//        // END_INCLUDE (configure_preview)
//
//
//        // BEGIN_INCLUDE (configure_media_recorder)
//        mMediaRecorder = new MediaRecorder();
//
//        // Step 1: Unlock and set camera to MediaRecorder
//        mCamera.stopPreview();
//        mCamera.unlock();
//        mMediaRecorder.setCamera(mCamera);
//
//        // Step 2: Set sources
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
//
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//
//        mMediaRecorder.setOrientationHint(90);
//
//        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
////        Log.e("oooooo", CameraHelper.getOutputMediaFile(
////                CameraHelper.MEDIA_TYPE_VIDEO).toString());
////        mMediaRecorder.setOutputFile(CameraHelper.getOutputMediaFile(
////                CameraHelper.MEDIA_TYPE_VIDEO).toString());
////        mMediaRecorder.setProfile(profile);
//
//        // Step 4: Set output file
//        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
//        if (mOutputFile == null) {
//            return false;
//        }
//        mMediaRecorder.setOutputFile(mOutputFile.getPath());
//        // END_INCLUDE (configure_media_recorder)
//
//        // Step 5: Prepare configured MediaRecorder
//        try {
//            mMediaRecorder.prepare();
//        } catch (IllegalStateException e) {
//            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
//            releaseMediaRecorder();
//            return false;
//        } catch (IOException e) {
//            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
//            releaseMediaRecorder();
//            return false;
//        }
//        return true;
//    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private boolean prepareVideoRecorder() {
        // BEGIN_INCLUDE (configure_preview)
        mCamera = CameraHelper.getDefaultCameraInstance();
        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();


        Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes, mSupportedPreviewSizes, mPreview.getWidth(), mPreview.getHeight());
//        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
//                mPreview.getWidth(), mPreview.getHeight());

        // AUTOFOCUS
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        parameters.set("cam_mode", 1); //not sure why this arcane setting is required. found this in another post on Stackoverlflow


        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;

        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(parameters);
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }
        // END_INCLUDE (configure_preview)
        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();

        mCamera.setDisplayOrientation(90);
        mMediaRecorder.setOrientationHint(90);
        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        // Step 2: Set sources
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)

        mMediaRecorder.setProfile(profile);
        // Step 4: Set output file
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);

        if (mOutputFile != null) {
            mMediaRecorder.setOutputFile(mOutputFile.toString());
        } else {
            Log.e("outputFile", "output file is null");
        }

        // END_INCLUDE (configure_media_recorder)
        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            try {
                boolean videoRecorder = prepareVideoRecorder();
                Log.e("VIDRECORDER", String.valueOf(videoRecorder));
                if (videoRecorder) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording

                    mMediaRecorder.start();
//                    Thread.sleep(1000);

                    isRecording = true;
                } else {
                    // prepare didn't work, release the camera
                    releaseMediaRecorder();
                    return false;
                }
            } catch (RuntimeException e) {
//                Toast.makeText(AutoRecordActivity.getInstance(), e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ERR", e.getMessage());
            }
//            catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            try {
//                Toast.makeText(AutoRecordActivity.this,"Wait A second", Toast.LENGTH_SHORT).show();

                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!result) {
                AutoRecordActivity.this.finish();
            }
            // inform the user that recording has started
            if (!isRecording) {
                captureButton.performClick();
            }
            setCaptureButtonText("Stop");

        }
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(AutoRecordActivity.this, "Wait!! 1sec", Toast.LENGTH_SHORT).show();
        captureButton.performClick();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
