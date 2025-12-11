
package com.example.eiapp;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    static { System.loadLibrary("ei"); }
    public native String runInference();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        TextView tv=new TextView(this);
        tv.setText(runInference());
        setContentView(tv);
    }
}
