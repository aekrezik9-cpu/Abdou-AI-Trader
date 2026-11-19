package com.abdou.aitrader;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(12, 18, 30));

        TextView title = new TextView(this);
        title.setText("ABDOU AI TRADER");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView status = new TextView(this);
        status.setText("\nChart analysis assistant\n\nStatus: Initial version\n\nCALL  •  PUT  •  WAIT");
        status.setTextColor(Color.rgb(150, 210, 230));
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        root.addView(status);

        TextView note = new TextView(this);
        note.setText("\nSignals are suggestions, not guarantees.\nYou remain in control of every trade.");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        root.addView(note);

        setContentView(root);
    }
}
