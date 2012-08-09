package com.vikingscientist.lr.introduction;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

	MyGLSurfaceView surfaceView;
	Button insertLines;
	Button selectBsplines;
	Button quit;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        GLSurfaceView v = (GLSurfaceView) findViewById(R.id.glview);
//        surfaceView = new MyGLSurfaceView(this);
//        v = surfaceView;
        insertLines    = (Button) findViewById(R.id.bLine);
        selectBsplines = (Button) findViewById(R.id.bSpline);
        quit           = (Button) findViewById(R.id.bQuit);
        surfaceView    = (MyGLSurfaceView) findViewById(R.id.glview);
        
        insertLines.setOnClickListener(surfaceView);
        selectBsplines.setOnClickListener(surfaceView);
        quit.setOnClickListener(this);
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
