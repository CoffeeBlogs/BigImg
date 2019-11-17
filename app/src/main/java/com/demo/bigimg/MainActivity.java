package com.demo.bigimg;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BigImgView bigImgView = findViewById(R.id.bigImg);
        try {
            InputStream inputStream = getAssets().open("bigimg.jpg");
            bigImgView.setImage(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
