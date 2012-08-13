package com.vikingscientist.lr.introduction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;


public class MyRenderer implements GLSurfaceView.Renderer {

	private final float touchLineZ = 1.0f;
	
	MyGLSurfaceView parent;
	Context ctx;
	LRSpline spline;
	Shaders shader;
	
	float MVPmatrix[] = new float[16];
	float tmpmatrix[] = new float[16];
	
	FloatBuffer touchLine;
	ShortBuffer selectedSpline;
	FloatBuffer supportVertices;
	ShortBuffer supportLines;
	private int supportLinesSize;
	
	private boolean displayTouchLine      = false;
	private boolean displaySelectedSpline = false;
	private boolean inPerspectiveView     = false;
	
	private volatile float    animationLength = 0.0f;
	private volatile long     startTime       = 0;
	private volatile Animation animation      = Animation.NONE;
	
	// cellphone normal
	float nx;
	float ny;
	float nz;
	
	// camera view
	double theta = 0;
	double phi   = 0;
	
	int cLine;
	int cNewLine;
	int cBspline;
	int cBsplineEdge;
	int cBsplineSelected;
	int cSupport;

	public MyRenderer(LRSpline spline, MyGLSurfaceView parent, Context ctx) {
		this.parent = parent;
		this.spline = spline;
		this.ctx    = ctx;
	}
	
	public void setMVP(int prog, float time) {
		// fetch variables
		int sMVP		= GLES20.glGetUniformLocation(prog, "mMVP");
		
		Matrix.setIdentityM(MVPmatrix, 0);
		Matrix.scaleM(MVPmatrix, 0, 1,1,0);
		if(inPerspectiveView) {
			float dt = 0.0f;
			if(animation == Animation.NONE)
				dt = 1.0f;
			else if(animation == Animation.PERSPECTIVE)
				dt = time/animationLength;			
			else if(animation == Animation.PERSPECTIVE_REVERSE)
				dt = 1.0f - time/animationLength;

			Matrix.rotateM(MVPmatrix, 0, (float) -phi*dt,   1, 0, 0);	
			Matrix.rotateM(MVPmatrix, 0, (float) -theta*dt, 0, 0, 1);
			Log.println(Log.DEBUG, "setting MVP", "Phi = " + phi + "  theta = " + theta);
		}
		Matrix.scaleM(MVPmatrix, 0, 1.8f/spline.getWidth(), 1.8f/spline.getHeight(), 0.0f);
		Matrix.translateM(MVPmatrix, 0, -0.5f*spline.getWidth(), -0.5f*spline.getHeight(), 0);
		GLES20.glUniformMatrix4fv(sMVP, 1, false, MVPmatrix, 0);
	}
	
	public void drawMesh(int prog, float time) {
		// fetch variables
		int sCol		= GLES20.glGetUniformLocation(prog, "vColor");
		int sPos		= GLES20.glGetAttribLocation(prog, "vPosition");
		int sTime		= GLES20.glGetUniformLocation(prog, "time");
		
		// set the color
		GLES20.glEnableVertexAttribArray(sPos);
		GLES20.glVertexAttribPointer(sPos, 3, GLES20.GL_FLOAT, false, 0, spline.linePoints);
		GLES20.glUniform4f(sCol, Color.red(  cLine) /256.0f,
                                 Color.green(cLine) /256.0f,
                                 Color.blue( cLine) /256.0f,
                                 Color.alpha(cLine) /256.0f);
		GLES20.glUniform1f(sTime, 1.0f);
		
		// draw mesh 
		int linesDrawn = 0;
		for(int i=0; i<spline.getMaxLineMult(); i++) { 
			GLES20.glLineWidth(i*2 + 2);
			GLES20.glDrawArrays(GLES20.GL_LINES, linesDrawn*2, spline.getMultCount(i+1)*2);
			linesDrawn += spline.getMultCount(i+1);
		}
		if(displaySelectedSpline) {
			GLES20.glVertexAttribPointer(sPos, 3, GLES20.GL_FLOAT, false, 0, supportVertices);
			GLES20.glUniform4f(sCol, Color.red(  cSupport)/256.0f,
					                 Color.green(cSupport)/256.0f,
					                 Color.blue( cSupport)/256.0f,
					                 Color.alpha(cSupport)/256.0f); 
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
			GLES20.glUniform4f(sCol, Color.red(  cBsplineSelected)/256.0f,
                                     Color.green(cBsplineSelected)/256.0f,
                                     Color.blue( cBsplineSelected)/256.0f,
                                     Color.alpha(cBsplineSelected)/256.0f);
			GLES20.glLineWidth(3);
			GLES20.glDrawElements(GLES20.GL_LINES, supportLinesSize, GLES20.GL_UNSIGNED_SHORT, supportLines);
		}
	}
	
	public void drawFunctions(int prog, float time) {
		// fetch variables
		int sPos		= GLES20.glGetAttribLocation(prog, "vPosition");
		int sCol		= GLES20.glGetUniformLocation(prog, "vColor");
		
		// draw bubbles
		GLES20.glLineWidth(3);
		if(animation != Animation.BSPLINE_SPLIT) {
			GLES20.glVertexAttribPointer(sPos, 3, GLES20.GL_FLOAT, false, 0, spline.circVerts);
			GLES20.glUniform4f(sCol, Color.red(  cBsplineEdge)/256.0f,
                                     Color.green(cBsplineEdge)/256.0f,
                                     Color.blue( cBsplineEdge)/256.0f,
                                     Color.alpha(cBsplineEdge)/256.0f);
			GLES20.glDrawElements(GLES20.GL_LINES, spline.circEdgeCount, GLES20.GL_UNSIGNED_SHORT, spline.circEdge);
			GLES20.glUniform4f(sCol, Color.red(  cBspline)/256.0f,
					                 Color.green(cBspline)/256.0f,
					                 Color.blue( cBspline)/256.0f,
					                 Color.alpha(cBspline)/256.0f);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, spline.circIntCount, GLES20.GL_UNSIGNED_SHORT, spline.circInterior);
		}
		
		if(displaySelectedSpline) {
			GLES20.glVertexAttribPointer(sPos, 3, GLES20.GL_FLOAT, false, 0, spline.circVerts);
			GLES20.glUniform4f(sCol, Color.red(  cBsplineSelected)/256.0f,
					                 Color.green(cBsplineSelected)/256.0f,
					                 Color.blue( cBsplineSelected)/256.0f,
					                 Color.alpha(cBsplineSelected)/256.0f);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, LRSpline.CIRCLE_POINTS*3, GLES20.GL_UNSIGNED_SHORT, selectedSpline);
		}
		
	}
	
	public void drawInputLine(int prog, float time) {
		// fetch variables
		int sPos		= GLES20.glGetAttribLocation(prog, "vPosition");
		int sCol		= GLES20.glGetUniformLocation(prog, "vColor");
		
		// draw touch input line
		if(displayTouchLine) {
			if(animation == Animation.MESHLINE_FADE) {
				GLES20.glUniform4f(sCol, Color.red(  cNewLine)/256.0f,
                                         Color.green(cNewLine)/256.0f,
                                         Color.blue( cNewLine)/256.0f,
                                         Color.alpha(cNewLine) * time*(animationLength-time)*4/animationLength/animationLength/256.0f);
				GLES20.glLineWidth((int)  (time*(animationLength-time)*4/animationLength/animationLength*10));
			} else {
				GLES20.glUniform4f(sCol, Color.red(  cNewLine)/256.0f,
		                                 Color.green(cNewLine)/256.0f,
		                                 Color.blue( cNewLine)/256.0f,
		                                 Color.alpha(cNewLine)/256.0f);
				GLES20.glLineWidth(3);
			}
			GLES20.glVertexAttribPointer(sPos, 3, GLES20.GL_FLOAT, false, 0, touchLine);
			GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
		}
	}
	
	public void drawAnimation(int prog, float time) {
		setMVP(prog, time);

		// get shader variables
		int sPosOrigin	= GLES20.glGetAttribLocation(prog, "vPositionStart");
		int sPos		= GLES20.glGetAttribLocation(prog, "vPosition");
		int sTime		= GLES20.glGetUniformLocation(prog, "time");
		int sCol		= GLES20.glGetUniformLocation(prog, "vColor");
		GLES20.glEnableVertexAttribArray(sPosOrigin);
		GLES20.glEnableVertexAttribArray(sPos);
		
		// setup variables and draw edge
		GLES20.glVertexAttribPointer(sPos,       3, GLES20.GL_FLOAT, false, 0, spline.circVerts);
		GLES20.glVertexAttribPointer(sPosOrigin, 3, GLES20.GL_FLOAT, false, 0, spline.circVertsOrigin);
		GLES20.glUniform1f(sTime, time/animationLength);

		GLES20.glUniform4f(sCol, Color.red(  cBsplineEdge)/256.0f,
                                 Color.green(cBsplineEdge)/256.0f,
                                 Color.blue( cBsplineEdge)/256.0f,
                                 Color.alpha(cBsplineEdge)/256.0f);
		GLES20.glDrawElements(GLES20.GL_LINES, spline.circEdgeCount, GLES20.GL_UNSIGNED_SHORT, spline.circEdge);
		
		// draw interior
		GLES20.glUniform4f(sCol, Color.red(  cBspline)/256.0f,
                                 Color.green(cBspline)/256.0f,
                                 Color.blue( cBspline)/256.0f,
                                 Color.alpha(cBspline)/256.0f);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, spline.circIntCount, GLES20.GL_UNSIGNED_SHORT, spline.circInterior);
		
		// cleanup
		GLES20.glDisableVertexAttribArray(sPosOrigin);
		GLES20.glDisableVertexAttribArray(sPos);

	}
	
	public void onDrawFrame(GL10 arg0) {
		// get timer (for animations)
		float t = getTime();
		Log.println(Log.DEBUG, "onDraw()", "time = " + t);
		
		// clear screen
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		
		// setup the shader programs
		int prog = shader.getProgram();
		GLES20.glUseProgram(prog);
		
		// fetch all shader variables
		int sPos		= GLES20.glGetAttribLocation(prog, "vPosition");
	
		setMVP(prog, t);

		drawMesh(prog, t);
		
		drawFunctions(prog, t);

		drawInputLine(prog, t);
		
		// cleanup
		GLES20.glDisableVertexAttribArray(sPos);
		
		// draw animation
		if(animation == Animation.BSPLINE_SPLIT) {
			// choose program
			prog = shader.getAnimateProgram();
			GLES20.glUseProgram(prog);
			
			drawAnimation(prog, t);
		}
		

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
		this.shader = new Shaders(ctx);

		// build all spline object stuff
		spline.buildBuffers();
		
		// build buffers for support drawing
		ByteBuffer bb ;
		supportLinesSize =  (spline.getP(0)+spline.getP(1)+4)*2;
		bb = ByteBuffer.allocateDirect(2*3*4);
		bb.order(ByteOrder.nativeOrder());
		touchLine = bb.asFloatBuffer();
		
		bb = ByteBuffer.allocateDirect(2*LRSpline.CIRCLE_POINTS*3);
		bb.order(ByteOrder.nativeOrder());
		selectedSpline = bb.asShortBuffer();
		
		bb = ByteBuffer.allocateDirect((spline.getP(0)+2)*(spline.getP(1)+2)*3*4);
		bb.order(ByteOrder.nativeOrder());
		supportVertices = bb.asFloatBuffer();
		
		bb = ByteBuffer.allocateDirect(2 * supportLinesSize);
		bb.order(ByteOrder.nativeOrder());
		supportLines = bb.asShortBuffer();

		touchLine.position(0);
		selectedSpline.position(0);
		supportVertices.position(0);
		supportLines.position(0);
	}
	
	public void setNewLineEndPos(Point p) {
//		Log.println(Log.INFO, "setTouchLine", "p1 = (" + x1 + ", " + y1 + ")   p2 =(" + x2 + ", " + y2 + ")   " + display);
		touchLine.position(3);
		touchLine.put(p.x);
		touchLine.put(p.y);
		touchLine.put(touchLineZ);
		touchLine.position(0);
	}
	
	public void setNewLineEndX(float x) {
		float x1 = touchLine.get();
		float y1 = touchLine.get();
		float z1 = touchLine.get();
		touchLine.put(x);
		touchLine.put(y1);
		touchLine.put(touchLineZ);
		touchLine.position(0);
	}
	public void setNewLineEndY(float y) {
		float x1 = touchLine.get();
		float y1 = touchLine.get();
		float z1 = touchLine.get();
		touchLine.put(x1);
		touchLine.put(y);
		touchLine.put(1);
		touchLine.position(0);
	}
	
	public void setNewLineStartPos(Point p) {
		touchLine.put(p.x);
		touchLine.put(p.y);
		touchLine.put(1);
		touchLine.position(0);
		displayTouchLine = true;
	}
	
	public Point[] getNewLine() {
		Point ans[] = new Point[2];
		float x1 = touchLine.get();
		float y1 = touchLine.get();
		float z1 = touchLine.get();
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
	
	public void setSelectedSpline(Bspline b) {
		int globI = spline.getIndexOf(b);
		if(globI < 0) {
			Log.println(Log.ERROR, "MyRenderer::setSelectedSpline", "Bspline not found in LRSpline object");
			return;
		}
		int nSplines = spline.getNSplines();
		for(int i=0; i<LRSpline.CIRCLE_POINTS; i++) {
			selectedSpline.put((short) globI);
			selectedSpline.put((short) (nSplines + LRSpline.CIRCLE_POINTS*globI +   i                         ));
			selectedSpline.put((short) (nSplines + LRSpline.CIRCLE_POINTS*globI + (i+1)%LRSpline.CIRCLE_POINTS));
		}
		int p1 = spline.getP(0);
		int p2 = spline.getP(1);
		supportVertices.put(b.getKnotU(  0 )); supportVertices.put(b.getKnotV(  0 )); supportVertices.put(0);
		supportVertices.put(b.getKnotU(p1+1)); supportVertices.put(b.getKnotV(  0 )); supportVertices.put(0);
		supportVertices.put(b.getKnotU(  0 )); supportVertices.put(b.getKnotV(p2+1)); supportVertices.put(0);
		supportVertices.put(b.getKnotU(p1+1)); supportVertices.put(b.getKnotV(p2+1)); supportVertices.put(0);
		for(int i=1; i<p1+1; i++) {
			supportVertices.put(b.getKnotU( i )); supportVertices.put(b.getKnotV(  0 )); supportVertices.put(0);
			supportVertices.put(b.getKnotU( i )); supportVertices.put(b.getKnotV(p2+1)); supportVertices.put(0);
		}
		for(int i=1; i<p2+1; i++) {
			supportVertices.put(b.getKnotU( 0  )); supportVertices.put(b.getKnotV( i )); supportVertices.put(0);
			supportVertices.put(b.getKnotU(p1+1)); supportVertices.put(b.getKnotV( i )); supportVertices.put(0);
		}
		for(int i=0; i<supportLinesSize-4; i++)
			supportLines.put((short) i);
		supportLines.put((short) 0);
		supportLines.put((short) 2);
		supportLines.put((short) 1);
		supportLines.put((short) 3);
		
//		supportVertices.put(b.getKnotU(0));
//		supportVertices.put(b.getKnotV(0));
//		supportVertices.put(0);

		selectedSpline.position(0);
		supportVertices.position(0);
		supportLines.position(0);
		displaySelectedSpline = true;
	}
	
	public void unselectSpline() {
		displaySelectedSpline = false;
	}
	
	public void finishPerspective() {
		inPerspectiveView = false;
		phi   = 0.0f;
		theta = 0.0f;
	}
	
	
	public void startAnimation(float length, Animation animation) {
		startTime       = SystemClock.uptimeMillis();
		animationLength = length;
		this.animation  = animation;
		if(animation == Animation.PERSPECTIVE)
			inPerspectiveView = true;
	}
	
	public float getTime() {
		float timeLapsed = (SystemClock.uptimeMillis() - startTime) / 1000.0f;
//		Log.println(Log.DEBUG, "GetTime()", "startTime  = " + startTime);
//		Log.println(Log.DEBUG, "GetTime()", "timeLapsed = " + timeLapsed);
		if(timeLapsed >= animationLength) {
			if(animation == Animation.MESHLINE_FADE) {
				terminateNewLine();
			} else if(animation == Animation.BSPLINE_SPLIT) {
				spline.terminateAnimation();
				spline.buildBuffers();
			} else if(animation == Animation.PERSPECTIVE_REVERSE) {
				finishPerspective();
			}
			parent.setAnimation(Animation.NONE);
			animationLength = 0.0f;
			startTime = 0;
			animation = Animation.NONE;
			return -1;
		}
		return timeLapsed;
	}
	
	public void setPhoneNormal(float nx, float ny, float nz) {
		this.nx = nx;
		this.ny = ny;
		this.nz = nz;
	}
	
	public void rotateView(float dTheta, float dPhi) {
		phi    = Math.max(Math.min(phi+dPhi, 180.0f), 0);
		theta  = (theta + dTheta) % 360.0f;
	}
	
}
