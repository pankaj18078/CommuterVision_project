package com.example.comvision;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class SplashScreenActivity extends AppCompatActivity {

    private int sleep_timer=2;
    private ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash_screen);

        imageView=(ImageView) findViewById(R.id.logo);
        Animation myanim = AnimationUtils.loadAnimation(this,R.anim.mytransition);
        imageView.startAnimation(myanim);

        getSupportActionBar().hide();
        LogoLauncher logoLauncher = new LogoLauncher();
        logoLauncher.start();


    }

    private class LogoLauncher extends Thread{
        public void run(){
            try{
                sleep(1000*sleep_timer);

            }
            catch(InterruptedException e){
                e.printStackTrace();

            }

            Intent intent =new Intent(SplashScreenActivity.this, MainActivity.class);
            startActivity(intent);

            SplashScreenActivity.this.finish();

        }
    }
}
