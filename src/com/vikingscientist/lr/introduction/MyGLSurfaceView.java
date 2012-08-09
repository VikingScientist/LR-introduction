package com.vikingscientist.lr.introduction;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MyGLSurfaceView extends GLSurfaceView implements OnClickListener {

	MyRenderer renderer; 
	LRSpline spline = new LRSpline(2, 2, 4, 3);
	
	int width;
	int height;
	
	volatile boolean inAnimation;
	
	public MyGLSurfaceView(Context context) {
		super(context);
		init();
	}
	
	public MyGLSurfaceView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}
	
	public void init() {
		// Create an OpenGL ES 2.0 context
		setEGLContextClientVersion(2);

		// Set the Renderer for drawing on the GLSurfaceView
		renderer = new MyRenderer(spline, this);
		setRenderer(renderer);
		
		// Render the view only when there is a change in the drawing data
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	public void setAnimation(Animation animate) {
		if(animate == Animation.BSPLINE_SPLIT) {
			renderer.startAnimation(2.0f, animate);
			inAnimation = true;
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		} else if(animate == Animation.MESHLINE_FADE) {
			renderer.startAnimation(1.0f, animate);
			inAnimation = true;
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		} else if(animate == Animation.NONE) {
			inAnimation = false;
			setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		}
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
			
//			requestRender();
		}
		
		return true;
	}

	public void onClick(View v) {
		if(v == findViewById(R.id.bLine)) {
			Log.println(Log.DEBUG, "onClick()", "Bspline button clicked");
		} else if(v == findViewById(R.id.bLine)) {
			Log.println(Log.DEBUG, "onClick()", "Meshline button clicked");
		} else {
			Log.println(Log.DEBUG, "onClick()", "Some button clicked");
		}
		
	}
	

}
