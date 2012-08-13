package com.vikingscientist.lr.introduction;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.TextView;

public class MyGLSurfaceView extends GLSurfaceView implements OnClickListener, SensorEventListener {

	MyRenderer renderer; 
	Activity owner;
	LRSpline spline = new LRSpline(2, 2, 4, 3);
	
	int width;
	int height;
	
	RadioButton splineButton;
	RadioButton lineButton;
	TextView outKnot[] = new TextView[2];
	
	volatile boolean inAnimation;
	
	private boolean inPerspectiveView  = false;
	
	volatile long startTime = -1;
	float lastX;
	float lastY;
	final float phiScale   = 0.20f;
	final float thetaScale = 0.28f;
	Bspline nearestSpline ;
	
	public MyGLSurfaceView(Context context) {
		super(context);
		init(context);
	}
	
	public MyGLSurfaceView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}
	
	public void init(Context context) {
		// Create an OpenGL ES 2.0 context
		setEGLContextClientVersion(2);

		// Set the Renderer for drawing on the GLSurfaceView
		renderer = new MyRenderer(spline, this, (Activity) context);
		setRenderer(renderer);
		
		// Render the view only when there is a change in the drawing data
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		
		// set the right colors
        Resources res = getResources();
        renderer.cLine            = res.getColor(R.color.line);
    	renderer.cNewLine         = res.getColor(R.color.newLine);
    	renderer.cBspline         = res.getColor(R.color.bspline);
    	renderer.cBsplineEdge     = res.getColor(R.color.bsplineEdge);
    	renderer.cBsplineSelected = res.getColor(R.color.bsplineSelected);
    	renderer.cSupport         = res.getColor(R.color.support);
	}
	
	public void setAnimation(Animation animate) {
		
		if(animate == Animation.BSPLINE_SPLIT) {
			renderer.startAnimation(2.0f, animate);
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
			
		} else if(animate == Animation.MESHLINE_FADE) {
			renderer.startAnimation(1.0f, animate);
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
			
		} else if(animate == Animation.PERSPECTIVE) {
			renderer.startAnimation(2.5f, animate);
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

		} else if(animate == Animation.PERSPECTIVE) {
			renderer.startAnimation(2.5f, animate);
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
			
		} else if(animate == Animation.PERSPECTIVE_REVERSE) {
			renderer.startAnimation(2.5f, animate);
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
			
		} else if(animate == Animation.NONE) {
			inAnimation = false;
			setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			return; // to not set inAnimation=true
		}
		inAnimation = true;
	}
	
	public boolean onTouchEvent(MotionEvent e) {
		if(inAnimation)
			return true;
		
		float x = e.getX();
		float y = e.getY();
		width  = getWidth();
		height = getHeight();
		y = height-y; // define origin as lower left corner (same as GLES does)
		
		float mX = 1.0f/0.9f/width*spline.getWidth();
		float mY = 1.0f/0.9f/height*spline.getHeight();
		float aX = 0.05f*spline.getWidth()/0.9f;
		float aY = 0.05f*spline.getHeight()/0.9f;
		
		if(inPerspectiveView) {
			// coordinates using only screen coordinates
			if(e.getAction() == MotionEvent.ACTION_DOWN) {
				lastX = e.getX();
				lastY = e.getY();
			} else if(e.getAction() == MotionEvent.ACTION_MOVE) {
				float dx = (e.getX() - lastX) * thetaScale;
				float dy = (e.getY() - lastY) * phiScale;
				lastX = e.getX();
				lastY = e.getY();
				renderer.rotateView(-dx, -dy);
				requestRender();
			}
			return true;
		}

		if(lineButton.isChecked() ) {
			// insert new-line input events
			
			if(e.getAction() == MotionEvent.ACTION_DOWN) {
	//			Log.println(Log.INFO, "onTouch DOWN", "x = " + x + "  y = " + y);
				renderer.setNewLineStartPos(spline.snapToMesh(new Point(x*mX-aX, y*mY-aY)));
			} else if(e.getAction() == MotionEvent.ACTION_MOVE) {
	//			Log.println(Log.INFO, "onTouch MOVE", "x = " + x + "  y = " + y);
	
				if(spline.lastSnappedToUspan)
					renderer.setNewLineEndY(y*mY-aY);
				else
					renderer.setNewLineEndX(x*mX-aX);
				requestRender();
			} else if(e.getAction() == MotionEvent.ACTION_UP) {
				Point line[] = renderer.getNewLine();
				Point snapEnd = spline.snapEndToMesh(line[1], !spline.lastSnappedToUspan);
				renderer.setNewLineEndPos(snapEnd);
				if(line[0].dist2(snapEnd) == 0.0f) {
					renderer.terminateNewLine();
					requestRender();
				} else if(spline.insertLine(line[0], snapEnd)) {
					renderer.terminateNewLine();
					spline.buildBuffers();
					setAnimation(Animation.BSPLINE_SPLIT);
				} else {
					setAnimation(Animation.MESHLINE_FADE);
				}
				
			}
		} else if(splineButton.isChecked()) {
			// view B-spline input events
			if(e.getAction() == MotionEvent.ACTION_DOWN) {
				nearestSpline = spline.getNearestSpline(new Point(x*mX-aX, y*mY-aY));
				outKnot[0].setText(nearestSpline.getKnotU());
				outKnot[1].setText(nearestSpline.getKnotV());
				renderer.setSelectedSpline(nearestSpline);
				requestRender();
				startTime = SystemClock.uptimeMillis();
			} else if(e.getAction() == MotionEvent.ACTION_MOVE) {
				long timeLapsed = (SystemClock.uptimeMillis() - startTime);
				if(timeLapsed > 1500) {
					Log.println(Log.DEBUG, "MyGLSurface::onTouchEvent", "holding for more than 1.5 sec");
					spline.buildFunctionBuffer(nearestSpline);
					outKnot[0].setText("");
					outKnot[1].setText("");
					renderer.unselectSpline();
					setAnimation(Animation.PERSPECTIVE);
					renderer.rotateView(45.0f, 75.0f);
					inPerspectiveView = true;
				}
			} else if(e.getAction() == MotionEvent.ACTION_UP) {
				startTime = -1;
			}
		}
		
		return true;
	}
	
	public void setSplineButton(RadioButton b) {
		splineButton = b;
	}
	
	public void setLineButton(RadioButton b) {
		lineButton = b;
	}
	
	public void setOutKnot(TextView u, TextView v) {
		outKnot[0] = u;
		outKnot[1] = v;
	}

	public void onClick(View v) {
		if(v == splineButton) {
			Log.println(Log.DEBUG, "onClick()", "Bspline button clicked");
			renderer.terminateNewLine();
			requestRender();
		} else if(v == lineButton) {
			Log.println(Log.DEBUG, "onClick()", "Meshline button clicked");
			outKnot[0].setText("");
			outKnot[1].setText("");
			renderer.unselectSpline();
			requestRender();
		} else {
			Log.println(Log.DEBUG, "onClick()", "Some button clicked");
		}
	}

	public boolean onBackPressed() {
	   Log.println(Log.DEBUG, "CDA", "onBackPressed Called");
	   if(inPerspectiveView) {
		   setAnimation(Animation.PERSPECTIVE_REVERSE);
		   inPerspectiveView = false;
		   return true;
	   }
	   return false;
	}
	

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent event) {
		if(!inPerspectiveView)
			return;
		if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
			float nx=event.values[0];
            float ny=event.values[1];
            float nz=event.values[2];
//            Log.println(Log.DEBUG, "sensor ACCELEROMETER event", String.format("normal = [%.3f, %.3f, %.3f]", nx,ny,nz));
            renderer.setPhoneNormal(nx, ny, nz);
		} 
	}
	

}
