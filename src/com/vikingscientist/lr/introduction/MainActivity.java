package com.vikingscientist.lr.introduction;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	MyGLSurfaceView surfaceView;
	CheckBox breakpoints;
	Button quit;
	Button restart;
	Button newSpline;
	Button cancelNewSpline;
	SensorManager sensors;
	EditText textP1, textP2;
	EditText textN1, textN2;
	
	int mStackLevel;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        RadioButton insertLines    = (RadioButton) findViewById(R.id.bLine);
        RadioButton selectBsplines = (RadioButton) findViewById(R.id.bSpline);
        TextView    outKnotU       = (TextView) findViewById(R.id.outKnotU);
        TextView    outKnotV       = (TextView) findViewById(R.id.outKnotV);
        restart        = (Button) findViewById(R.id.bRestart);
        quit           = (Button) findViewById(R.id.bQuit);
        breakpoints    = (CheckBox) findViewById(R.id.breakpoints);
        surfaceView    = (MyGLSurfaceView) findViewById(R.id.glview);
        
        insertLines.setOnClickListener(surfaceView);
        selectBsplines.setOnClickListener(surfaceView);
        quit.setOnClickListener(this);
        restart.setOnClickListener(this);
        breakpoints.setOnClickListener(surfaceView);
        
        surfaceView.setSplineButton(selectBsplines);
        surfaceView.setLineButton(insertLines);
        surfaceView.setBreakpointButton(breakpoints);
        surfaceView.setOutKnot(outKnotU, outKnotV);
        
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void onResume() {
    	super.onResume();
    }
    
    public void onPause() {
    	super.onPause();
    	surfaceView.finishAnimation();
    }
    
    public void onStop() {
    	super.onStop();
    	Log.println(Log.DEBUG, "onStop", "LOOK AT MEEEE!!!");
    }
    
    public void onDestroy() {
    	super.onDestroy();
    	Log.println(Log.DEBUG, "onDestroy", "LOOK AT MEEEE!!!");
    }

	public void onClick(View v) {
		if(v == quit) {
			finish();
		} else if(v == restart) {
			showDialog(0);
		} else if(v == cancelNewSpline) {
			dismissDialog(0);
		} else if(v == newSpline) {
			Log.println(Log.DEBUG, "quacvk", "adsf");
			int p1=0,p2=0,n1=0,n2=0;
			try {
				p1 = Integer.parseInt(textP1.getText().toString());
				p2 = Integer.parseInt(textP2.getText().toString());
				n1 = Integer.parseInt(textN1.getText().toString());
				n2 = Integer.parseInt(textN2.getText().toString());
			} catch (NumberFormatException e) {

				Log.println(Log.ERROR, "quacvk", "NUMBER FORMAT");
				return;
			}
			if(p1 < 1 || p1 > 8 ||
			   p2 < 1 || p2 > 8 ||
			   n1 < p1+1 || n1 > 20 ||
			   n2 < p2+1 || n2 > 20) {
				Log.println(Log.ERROR, "quacvk", "WRONG NUMBERS!!!!");
				return;
			}
			
			surfaceView.resetLRSpline(p1, p2, n1, n2);
			dismissDialog(0);
			surfaceView.requestRender();
		}
	}
	
	public void onBackPressed() {
		if(!surfaceView.onBackPressed()) 
			super.onBackPressed();
	}
	
	protected Dialog onCreateDialog(int id) {
		Context mContext = this;
		Dialog dialog = new Dialog(mContext);

		dialog.setContentView(R.layout.new_spline_dialog);
		dialog.setTitle("New tensor spline");
		
		newSpline       = (Button) dialog.findViewById(R.id.bDialogOK);
		newSpline.setOnClickListener(this);
		cancelNewSpline = (Button) dialog.findViewById(R.id.bDialogCancel);
		cancelNewSpline.setOnClickListener(this);
		
		textP1 = (EditText) dialog.findViewById(R.id.textInP1);
		textP2 = (EditText) dialog.findViewById(R.id.textInP2);
		textN1 = (EditText) dialog.findViewById(R.id.textInN1);
		textN2 = (EditText) dialog.findViewById(R.id.textInN2);
		
		return dialog;
		

	}


    
}
