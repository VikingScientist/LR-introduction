package com.vikingscientist.lr.introduction;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	MyGLSurfaceView surfaceView;
	Button quit;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        RadioButton insertLines    = (RadioButton) findViewById(R.id.bLine);
        RadioButton selectBsplines = (RadioButton) findViewById(R.id.bSpline);
        TextView    outKnotU       = (TextView) findViewById(R.id.outKnotU);
        TextView    outKnotV       = (TextView) findViewById(R.id.outKnotV);
        quit           = (Button) findViewById(R.id.bQuit);
        surfaceView    = (MyGLSurfaceView) findViewById(R.id.glview);
        
        insertLines.setOnClickListener(surfaceView);
        selectBsplines.setOnClickListener(surfaceView);
        quit.setOnClickListener(this);
        
        surfaceView.setSplineButton(selectBsplines);
        surfaceView.setLineButton(insertLines);
        surfaceView.setOutKnot(outKnotU, outKnotV);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	public void onClick(View v) {
		if(v == quit) {
			finish();
		}
	}

    
}
