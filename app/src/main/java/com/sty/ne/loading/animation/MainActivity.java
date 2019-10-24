package com.sty.ne.loading.animation;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private LoadingView loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadingView = findViewById(R.id.loading_view);
        loadingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingView.startAnimator();
            }
        });
    }
}
