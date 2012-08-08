package com.vikingscientist.lr.introduction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;


public class MyRenderer implements GLSurfaceView.Renderer {

	MyGLSurfaceView parent;
	LRSpline spline;
	Shaders shader;
	
	float MVPmatrix[] = new float[16];
	float tmpmatrix[] = new float[16];
	
	FloatBuffer unitSquare;
	FloatBuffer touchLine;
	
	private boolean displayTouchLine = false;
	
	private volatile float animationLength = 0.0f;
	private volatile long  startTime       = 0;
	
	public MyRenderer(LRSpline spline, MyGLSurfaceView parent) {
		this.parent = parent;
		this.spline = spline;
	}
	
	public void onDrawFrame(GL10 arg0) {
		// get timer (for animations)
		float t = getTime();
		Log.println(Log.DEBUG, "onDraw()", "time = " + t);
		
		// clear screen
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		int error = GLES20.glGetError();
		Log.println(Log.DEBUG, "onDraw checkpoint", "error = " + error);
		
		// setup the shader programs
		int prog = shader.getProgram();
		GLES20.glUseProgram(prog);
		
		error = GLES20.glGetError();
		Log.println(Log.DEBUG, "onDraw checkpoint", "error = " + error);
		
		// fetch all shader variables
		int sPos		= GLES20.glGetAttribLocation(prog, "vPosition");
		int sCol		= GLES20.glGetUniformLocation(prog, "vColor");
		int sMVP		= GLES20.glGetUniformLocation(prog, "mMVP");
				
		error = GLES20.glGetError();
		Log.println(Log.DEBUG, "onDraw checkpoint", "error = " + error);
		
		// setup the model-view projection matrix
		Matrix.setIdentityM(MVPmatrix, 0);
		Matrix.translateM(MVPmatrix, 0, -0.9f, -0.9f, 0);
		Matrix.scaleM(MVPmatrix, 0, 1.8f/spline.getWidth(), 1.8f/spline.getHeight(), 0);
		GLES20.glUniformMatrix4fv(sMVP, 1, false, MVPmatrix, 0);

		// setup the color
		GLES20.glEnableVertexAttribArray(sPos);
		GLES20.glVertexAttribPointer(sPos, 2, GLES20.GL_FLOAT, false, 0, spline.linePoints);
		GLES20.glUniform4f(sCol, 0.7f, 0.7f, 0.1f, 1.0f);
		
		// draw mesh 
		int linesDrawn = 0;
		for(int i=0; i<spline.getMaxLineMult(); i++) { 
			GLES20.glLineWidth(i*2 + 2);
			GLES20.glDrawArrays(GLES20.GL_LINES, linesDrawn*2, spline.getMultCount(i+1)*2);
			linesDrawn += spline.getMultCount(i+1);
		}
		
		// draw bubbles
		GLES20.glLineWidth(3);
		if(t>0) {

		} else {
			GLES20.glVertexAttribPointer(sPos, 2, GLES20.GL_FLOAT, false, 0, spline.circVerts);
			GLES20.glUniform4f(sCol, 0.70f, 0.65f, 1.0f, 1); // white with a hint of purple
			GLES20.glDrawElements(GLES20.GL_LINES, spline.circEdgeCount, GLES20.GL_UNSIGNED_SHORT, spline.circEdge);
			GLES20.glUniform4f(sCol, 0.5686275f, 0.49411765f, 1.0f, 1.0f); // blue purple color
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, spline.circIntCount, GLES20.GL_UNSIGNED_SHORT, spline.circInterior);
		}

		error = GLES20.glGetError();
		Log.println(Log.DEBUG, "onDraw checkpoint", "error = " + error);
		
		// draw touch input line
		if(displayTouchLine) {
//			Log.println(Log.INFO, "draw", "coords = " + )
			GLES20.glUniform4f(sCol, 1, 0, 0, 1);
			GLES20.glVertexAttribPointer(sPos, 2, GLES20.GL_FLOAT, false, 0, touchLine);
			GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
		}

		error = GLES20.glGetError();
		Log.println(Log.DEBUG, "onDraw checkpoint", "error = " + error);
		
		// cleanup
		GLES20.glDisableVertexAttribArray(sPos);
		GLES20.glDeleteProgram(prog);
		
		// draw animation
		if(t>0) {
			// choose program
			prog = shader.getAnimateProgram();
			GLES20.glUseProgram(prog);

			error = GLES20.glGetError();
			Log.println(Log.DEBUG, "onDraw checkpoint", "error = " + error);

			// get shader variables
			int sTime		= GLES20.glGetUniformLocation(prog, "time");
			int sPosOrigin	= GLES20.glGetAttribLocation(prog, "vPositionStart");
			sPos		= GLES20.glGetAttribLocation(prog, "vPosition");
			sCol		= GLES20.glGetUniformLocation(prog, "vColor");
			sMVP		= GLES20.glGetUniformLocation(prog, "mMVP");
			GLES20.glEnableVertexAttribArray(sPosOrigin);
			GLES20.glEnableVertexAttribArray(sPos);
			
			Log.println(Log.DEBUG, "onDraw", "sTime = " + sTime);
			Log.println(Log.DEBUG, "onDraw", "sPosOrigin = " + sPosOrigin);
			Log.println(Log.DEBUG, "onDraw", "sPos = " + sPos);
			Log.println(Log.DEBUG, "onDraw", "sCol = " + sCol);
			Log.println(Log.DEBUG, "onDraw", "sMVP = " + sMVP);

			error = GLES20.glGetError();
			Log.println(Log.DEBUG, "onDraw checkpoint", "error = " + error);
			
			// setup variables and draw edge
			GLES20.glUniformMatrix4fv(sMVP, 1, false, MVPmatrix, 0);
			GLES20.glVertexAttribPointer(sPos,       2, GLES20.GL_FLOAT, false, 0, spline.circVerts);
			GLES20.glVertexAttribPointer(sPosOrigin, 2, GLES20.GL_FLOAT, false, 0, spline.circVertsOrigin);
			GLES20.glUniform1f(sTime, t/animationLength);
			GLES20.glUniform4f(sCol, 0.70f, 0.65f, 1.0f, 1); // white with a hint of purple
			GLES20.glDrawElements(GLES20.GL_LINES, spline.circEdgeCount, GLES20.GL_UNSIGNED_SHORT, spline.circEdge);

			error = GLES20.glGetError();
			Log.println(Log.DEBUG, "onDraw checkpoint", "error = " + error);
			
			// draw interior
			GLES20.glUniform4f(sCol, 0.5686275f, 0.49411765f, 1.0f, 1.0f); // blue purple color
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, spline.circIntCount, GLES20.GL_UNSIGNED_SHORT, spline.circInterior);
			
			// cleanup
			GLES20.glDisableVertexAttribArray(sPosOrigin);
			GLES20.glDisableVertexAttribArray(sPos);
			GLES20.glDeleteProgram(prog);
		}

		error = GLES20.glGetError();
		Log.println(Log.DEBUG, "onDraw checkpoint", "error = " + error);
	}

	public void onSurfaceChanged(GL10 arg0, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
	}

	public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
		// setup default rendering behavior
		GLES20.glClearColor(0,0,0,1);
		
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		
		// make, compile and link all programs
		this.shader = new Shaders();
		
		// build all spline object stuff
		spline.buildBuffers();
		
		// debug drawing unit square
		ByteBuffer bb = ByteBuffer.allocateDirect(2*4*4);
		bb.order(ByteOrder.nativeOrder());
		unitSquare = bb.asFloatBuffer();
		unitSquare.put(0); unitSquare.put(0);
		unitSquare.put(1); unitSquare.put(0);
		unitSquare.put(0); unitSquare.put(1);
		unitSquare.put(1); unitSquare.put(1);
		unitSquare.position(0);
		
		// debug drawing unit square
		bb = ByteBuffer.allocateDirect(2*2*4);
		bb.order(ByteOrder.nativeOrder());
		touchLine = bb.asFloatBuffer();
		touchLine.position(0);
	}
	
	public void setNewLineEndPos(Point p) {
//		Log.println(Log.INFO, "setTouchLine", "p1 = (" + x1 + ", " + y1 + ")   p2 =(" + x2 + ", " + y2 + ")   " + display);
		touchLine.position(2);
		touchLine.put(p.x);
		touchLine.put(p.y);
		touchLine.position(0);
	}
	
	public void setNewLineEndX(float x) {
		float x1 = touchLine.get();
		float y1 = touchLine.get();
		touchLine.put(x);
		touchLine.put(y1);
		touchLine.position(0);
	}
	public void setNewLineEndY(float y) {
		float x1 = touchLine.get();
		float y1 = touchLine.get();
		touchLine.put(x1);
		touchLine.put(y);
		touchLine.position(0);
	}
	
	public void setNewLineStartPos(Point p) {
		touchLine.put(p.x);
		touchLine.put(p.y);
		touchLine.position(0);
		displayTouchLine = true;
	}
	
	public Point[] getNewLine() {
		Point ans[] = new Point[2];
		float x1 = touchLine.get();
		float y1 = touchLine.get();
		float x2 = touchLine.get();
		float y2 = touchLine.get();
		touchLine.position(0);
		
		ans[0] = new Point(x1,y1);
		ans[1] = new Point(x2,y2);
		
		return ans;
	}
	
	public void terminateNewLine() {
		displayTouchLine = false;
	}
	
	public void startAnimation(float length) {
		startTime       = SystemClock.uptimeMillis();
		animationLength = length;
	}
	
	public float getTime() {
		float timeLapsed = (SystemClock.uptimeMillis() - startTime) / 1000.0f;
//		Log.println(Log.DEBUG, "GetTime()", "startTime  = " + startTime);
//		Log.println(Log.DEBUG, "GetTime()", "timeLapsed = " + timeLapsed);
		if(timeLapsed >= animationLength) {
			parent.setAnimation(false);
			animationLength = 0.0f;
			startTime = 0;
			return -1;
		}
		return timeLapsed;
	}

	
}
