package com.sander.shield;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;

public class SplashScreen extends Activity {
    private TextView tv;
    private String[] frames = {
        "[ ] Initializing SanderCore...\n[ ] Loading Modules...",
        "[*] Initializing SanderCore...\n[ ] Loading Modules...",
        "[*] Initializing SanderCore...\n[*] Loading Modules...\n[ ] Bypassing Sandbox...",
        "[*] Initializing SanderCore...\n[*] Loading Modules...\n[*] Bypassing Sandbox...\n\n[+] SYSTEM READY."
    };
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tv = new TextView(this);
        tv.setTextColor(Color.parseColor("#00FF00"));
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextSize(18f);
        tv.setPadding(50, 50, 50, 50);
        tv.setText("root@sander:~# boot\n\n");
        setContentView(tv);

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index < frames.length) {
                    tv.append(frames[index] + "\n\n");
                    index++;
                    handler.postDelayed(this, 600);
                } else {
                    startActivity(new Intent(SplashScreen.this, MainActivity.class));
                    finish();
                }
            }
        };
        handler.postDelayed(runnable, 400);
    }
}
