package com.vikingscientist.lr.introduction;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MyGLSurfaceView extends GLSurfaceView {

	MyRenderer renderer; 
	LRSpline spline = new LRSpline(2, 2, 4, 3);
	int width;
	int height;
	
	public MyGLSurfaceView(Context context) {
		super(context);
		// Create an OpenGL ES 2.0 context
		setEGLContextClientVersion(2);

		// Set the Renderer for drawing on the GLSurfaceView
		renderer = new MyRenderer(spline);
		setRenderer(renderer);
		
		// Render the view only when there is a change in the drawing data
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	public boolean onTouchEvent(MotionEvent e) {
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
			
			if(spline.insertLine(line[0], snapEnd))
				spline.buildBuffers();
			
			renderer.terminateNewLine();
			requestRender();
		}
		
		return true;
	}
	

}
