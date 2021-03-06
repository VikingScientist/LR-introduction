package com.vikingscientist.lr.introduction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import android.util.FloatMath;
import android.util.Log;

public class LRSpline {
	
	// rendering controls
	static final int CIRCLE_POINTS = 20;
	static final int PLOT_POINTS   = 50;
	static final int MAX_SPLINES   = 1024;
	static final int MAX_LINES     = 1024;
	
	// core LR B-spline information
	int p1, p2;
	volatile ArrayList<MeshLine> lines            = new ArrayList<MeshLine>();
	volatile ArrayList<Bspline>  newSpline        = new ArrayList<Bspline>();
	volatile HashSet<Bspline>    functions        = new HashSet<Bspline>();
	volatile ArrayList<Bspline>  animationSplines = new ArrayList<Bspline>();
	
	// nice-to-have variables
	int uMax;
	int vMax;
	float zMax = 0.001f;
	boolean lastSnappedToUspan;
	
	// break-point stepping splitting
	Bspline activeB;
	MeshLine activeM;
	boolean breakStepOne = false;
	boolean breakStepTwo = false;
	boolean inStepOne    = false;
	boolean inStepTwo    = false;
	
	// rendering information and buffers
	volatile ShortBuffer selectedSpline;
	volatile FloatBuffer supportVertices;
	volatile ShortBuffer supportLines;
	volatile int         supportLinesSize;
	
	volatile FloatBuffer linePoints;
	volatile FloatBuffer circVerts;
	volatile ShortBuffer circInterior;
	volatile ShortBuffer circEdge;
	volatile FloatBuffer circVertsOrigin;
	volatile FloatBuffer perspVertices;     // for perspective view
	volatile FloatBuffer perspLineVertices; // for perspective view 
	volatile ShortBuffer perspLines;
	volatile ShortBuffer perspTriangleStrip;
	volatile ShortBuffer perspTriangles;
	
	volatile int         multCount[];
	volatile int         circIntCount;
	volatile int         circEdgeCount;
	volatile int         perspTriangleCount;
	volatile int         perspLineCount;
	
	public LRSpline(int p1, int p2, int n1, int n2) {
		this.p1 = p1;
		this.p2 = p2;
		uMax = n1-p1;
		vMax = n2-p2;
		
		// build the global knot vector
		int knotU[] = new int[p1+n1+1];
		int knotV[] = new int[p2+n2+1];
		int k=0;
		for(int i=0; i<p1+1; i++)
			knotU[k++] = 0;
		for(int i=1; i<uMax; i++)
			knotU[k++] = i;
		for(int i=0; i<p1+1; i++)
			knotU[k++] = uMax;
		k=0;
		for(int i=0; i<p2+1; i++)
			knotV[k++] = 0;
		for(int i=1; i<vMax; i++)
			knotV[k++] = i;
		for(int i=0; i<p2+1; i++)
			knotV[k++] = vMax;
		
		
		// add all mesh lines
		for(int i=1; i<n1-p1; i++)
			lines.add(new MeshLine(false, i, 0, vMax, 1));
		lines.add(new MeshLine(false, 0,    0, vMax, p1+1));
		lines.add(new MeshLine(false, uMax, 0, vMax, p1+1));
		
		for(int i=1; i<n2-p2; i++)
			lines.add(new MeshLine(true, i, 0, uMax, 1));
		lines.add(new MeshLine(true, 0,    0, uMax, p2+1));
		lines.add(new MeshLine(true, vMax, 0, uMax, p2+1));
		
		// add all B-splines
		for(int i=0; i<n1; i++) {
			for(int j=0; j<n2; j++) {
				Bspline b = new Bspline(p1,p2);
				for(k=0; k<p1+2; k++)
					b.putU(knotU[i+k]);
				for(k=0; k<p2+2; k++)
					b.putV(knotV[j+k]);
				functions.add(b);
			}
		}
		
		supportLinesSize = (p1+p2+4)*2;
		
		initBuffers();
		
	}
	
	public int getWidth() {
		return uMax;
	}
	
	public int getHeight() {
		return vMax;
	}
	
	public float getZmax() {
		return zMax;
	}
	
	public int getP(int i) {
		if(i == 0)
			return p1;
		else
			return p2;
	}
	
	public void buildFunctionBuffer(Bspline b) {
		// functionView is gridded as a triangle_strip. Double-storing the endpoint to
		// get two degenerated triangles when swapping to the next row. The exterior where
		// the B-spline is zero, is given as 8 quads surrounding the interior
		int nVerts         = (PLOT_POINTS*PLOT_POINTS + 8*4)*3;
		perspTriangleCount = 2*(PLOT_POINTS*PLOT_POINTS - 1);
		perspLineCount     = 0;
		for(MeshLine m : lines) {
			if(b.overlaps(m))
				perspLineCount += (PLOT_POINTS-1)*2;
			else 
				perspLineCount += 2;
		}
		perspLineCount *= 3;
		
		float verts[]      = new float[nVerts];
		float lineVerts[]  = new float[perspLineCount];
		short sTriStrip[]  = new short[perspTriangleCount];
		short sTri[]       = new short[8*3*2];
		
		ByteBuffer bb;
		
		bb = ByteBuffer.allocateDirect(4*nVerts); 
		bb.order(ByteOrder.nativeOrder());
		perspVertices = bb.asFloatBuffer();

		bb = ByteBuffer.allocateDirect(4*perspLineCount); 
		bb.order(ByteOrder.nativeOrder());
		perspLineVertices = bb.asFloatBuffer();
		
		bb = ByteBuffer.allocateDirect(2*perspLineCount); 
		bb.order(ByteOrder.nativeOrder());
		perspLines = bb.asShortBuffer();
		
		bb = ByteBuffer.allocateDirect(2*perspTriangleCount); 
		bb.order(ByteOrder.nativeOrder());
		perspTriangleStrip = bb.asShortBuffer();
		
		bb = ByteBuffer.allocateDirect(8*3*2 * 2); 
		bb.order(ByteOrder.nativeOrder());
		perspTriangles = bb.asShortBuffer();
		
		zMax = Float.MIN_VALUE;
		
		// build the vertex buffer (structured mesh)
		float dx = b.getWidth();
		float dy = b.getHeight();
		int k=0;
		for(int j=0; j<PLOT_POINTS; j++) {
			for(int i=0; i<PLOT_POINTS; i++) {
				float x = ((float) i) / (PLOT_POINTS-1) * dx + b.umin();
				float y = ((float) j) / (PLOT_POINTS-1) * dy + b.vmin();
				float z =  (float) b.evaluate(x,y, x==uMax, y==vMax);
				verts[k++] = x;
				verts[k++] = y;
				verts[k++] = z;
				zMax = Math.max(zMax, z);
			}
		}
		// lower left quad
		verts[k++] = 0;        verts[k++] = 0;        verts[k++] = 0;
		verts[k++] = b.umin(); verts[k++] = 0;        verts[k++] = 0;
		verts[k++] = 0;        verts[k++] = b.vmin(); verts[k++] = 0;
		verts[k++] = b.umin(); verts[k++] = b.vmin(); verts[k++] = 0;

		// under quad
		verts[k++] = b.umin(); verts[k++] = 0;        verts[k++] = 0;
		verts[k++] = b.umax(); verts[k++] = 0;        verts[k++] = 0;
		verts[k++] = b.umin(); verts[k++] = b.vmin(); verts[k++] = 0;
		verts[k++] = b.umax(); verts[k++] = b.vmin(); verts[k++] = 0;
		
		// lower right quad
		verts[k++] = b.umax(); verts[k++] = 0;        verts[k++] = 0;
		verts[k++] = uMax;     verts[k++] = 0;        verts[k++] = 0;
		verts[k++] = b.umax(); verts[k++] = b.vmin(); verts[k++] = 0;
		verts[k++] = uMax;     verts[k++] = b.vmin(); verts[k++] = 0;

		// mid left quad
		verts[k++] = 0;        verts[k++] = b.vmin(); verts[k++] = 0;
		verts[k++] = b.umin(); verts[k++] = b.vmin(); verts[k++] = 0;
		verts[k++] = 0;        verts[k++] = b.vmax(); verts[k++] = 0;
		verts[k++] = b.umin(); verts[k++] = b.vmax(); verts[k++] = 0;

		// then comes the actual function
		
		// mid right quad
		verts[k++] = b.umax(); verts[k++] = b.vmin(); verts[k++] = 0;
		verts[k++] = uMax;     verts[k++] = b.vmin(); verts[k++] = 0;
		verts[k++] = b.umax(); verts[k++] = b.vmax(); verts[k++] = 0;
		verts[k++] = uMax;     verts[k++] = b.vmax(); verts[k++] = 0;
		
		// upper left quad
		verts[k++] = 0;        verts[k++] = b.vmax(); verts[k++] = 0;
		verts[k++] = b.umin(); verts[k++] = b.vmax(); verts[k++] = 0;
		verts[k++] = 0;        verts[k++] = vMax;     verts[k++] = 0;
		verts[k++] = b.umin(); verts[k++] = vMax;     verts[k++] = 0;

		// over quad
		verts[k++] = b.umin(); verts[k++] = b.vmax(); verts[k++] = 0;
		verts[k++] = b.umax(); verts[k++] = b.vmax(); verts[k++] = 0;
		verts[k++] = b.umin(); verts[k++] = vMax;     verts[k++] = 0;
		verts[k++] = b.umax(); verts[k++] = vMax;     verts[k++] = 0;
		
		// upper right quad
		verts[k++] = b.umax(); verts[k++] = b.vmax(); verts[k++] = 0;
		verts[k++] = uMax;     verts[k++] = b.vmax(); verts[k++] = 0;
		verts[k++] = b.umax(); verts[k++] = vMax;     verts[k++] = 0;
		verts[k++] = uMax;     verts[k++] = vMax;     verts[k++] = 0;
		
		// build the line indices
		k = 0;
		for(MeshLine m : lines) {
			if(b.overlaps(m)) {
				dx = (float) ( (m.span_u) ? m.stop-m.start : 0.0 );
				dy = (float) ( (m.span_u) ? 0.0 : m.stop-m.start );
				float x1 = (m.span_u) ? m.start    : m.constPar;
				float y1 = (m.span_u) ? m.constPar : m.start;
				for(int i=1; i<PLOT_POINTS; i++) {
					float x0 = x1 + dx*(i-1)/(PLOT_POINTS-1);
					float y0 = y1 + dy*(i-1)/(PLOT_POINTS-1);
					float z0 = (float) b.evaluate(x0, y0, x0==uMax, y0==vMax);
					float x  = x1 + dx*i/(PLOT_POINTS-1);
					float y  = y1 + dy*i/(PLOT_POINTS-1);
					float z  = (float) b.evaluate(x, y, x==uMax, y==vMax);
					lineVerts[k++] = x0;
					lineVerts[k++] = y0;
					lineVerts[k++] = z0;
					lineVerts[k++] = x;
					lineVerts[k++] = y;
					lineVerts[k++] = z;
				}
			} else {
				lineVerts[k++] = (m.span_u) ? m.start    : m.constPar;
				lineVerts[k++] = (m.span_u) ? m.constPar : m.start;
				lineVerts[k++] = 0;
				lineVerts[k++] = (m.span_u) ? m.stop     : m.constPar;
				lineVerts[k++] = (m.span_u) ? m.constPar : m.stop;
				lineVerts[k++] = 0;
			}
		}
		
		// build the triangle strip indices
		k=0;
		for(int j=0; j<PLOT_POINTS-1; j++) {
			sTriStrip[k++] = (short) (  j  *PLOT_POINTS +   0  );
			sTriStrip[k++] = (short) ((j+1)*PLOT_POINTS +   0  );
			for(int i=1; i<PLOT_POINTS; i++) {
				sTriStrip[k++] = (short) (  j  *PLOT_POINTS + i );
				sTriStrip[k++] = (short) ((j+1)*PLOT_POINTS + i );
			}
			sTriStrip[k++] = (short) ((j+2)*PLOT_POINTS -  1  );
			sTriStrip[k++] = (short) ((j+1)*PLOT_POINTS +  0  );
		}
		k = 0;
		for(int s=0; s<8; s++) 
			for(int i=0; i<2; i++)
				for(int j=0; j<3; j++)
					sTri[k++] =	(short) (PLOT_POINTS*PLOT_POINTS + i + j + s*4);
		
		perspVertices.put(verts);
		perspLineVertices.put(lineVerts);
		perspTriangleStrip.put(sTriStrip);
		perspTriangles.put(sTri);
		
		perspVertices.position(0);
		perspLineVertices.position(0);
		perspTriangleStrip.position(0);
		perspTriangles.position(0);
	}
	
	public void initBuffers() {
		ByteBuffer bb = ByteBuffer.allocateDirect(MAX_LINES*4*6);
		bb.order(ByteOrder.nativeOrder());
		linePoints = bb.asFloatBuffer();
		
		bb = ByteBuffer.allocateDirect(4*(CIRCLE_POINTS+1)*3*MAX_SPLINES);
		bb.order(ByteOrder.nativeOrder());
		circVerts = bb.asFloatBuffer();
		
		bb = ByteBuffer.allocateDirect(2*(CIRCLE_POINTS)*3*MAX_SPLINES);
		bb.order(ByteOrder.nativeOrder());
		circInterior = bb.asShortBuffer();
		
		bb = ByteBuffer.allocateDirect(2*(CIRCLE_POINTS)*2*MAX_SPLINES);
		bb.order(ByteOrder.nativeOrder());
		circEdge = bb.asShortBuffer();
		
		bb = ByteBuffer.allocateDirect(4*(CIRCLE_POINTS+1)*3*MAX_SPLINES);
		bb.order(ByteOrder.nativeOrder());
		circVertsOrigin = bb.asFloatBuffer();
		
		bb = ByteBuffer.allocateDirect(2*LRSpline.CIRCLE_POINTS*3);
		bb.order(ByteOrder.nativeOrder());
		selectedSpline = bb.asShortBuffer();
		
		bb = ByteBuffer.allocateDirect((p1+2)*(p2+2)*3*4);
		bb.order(ByteOrder.nativeOrder());
		supportVertices = bb.asFloatBuffer();
		
		bb = ByteBuffer.allocateDirect(2 * supportLinesSize);
		bb.order(ByteOrder.nativeOrder());
		supportLines = bb.asShortBuffer();
	}
	
	public synchronized void buildBuffers() {
		// reset buffer pointers
		linePoints.position(0);
		circVerts.position(0);
		circVertsOrigin.position(0);
		circInterior.position(0);
		circEdge.position(0);
		selectedSpline.position(0);
		supportVertices.position(0);
		supportLines.position(0);
		
		// measure buffer sizes
		int nBsplines = functions.size() + animationSplines.size();
		circIntCount  = (CIRCLE_POINTS)*3*nBsplines;
		circEdgeCount = (CIRCLE_POINTS)*2*nBsplines;

		// evaluate the line stuff
		int maxMult = (p1 > p2) ? p1+1 : p2+1;
		multCount = new int[maxMult];
		
		for(int i=1; i<=maxMult; i++) {
			for(MeshLine m : lines) {
				if(m.mult != i) continue;
				if(m.span_u) {
					linePoints.put(m.start);
					linePoints.put(m.constPar);
					linePoints.put(0);
					linePoints.put(m.stop);
					linePoints.put(m.constPar);
					linePoints.put(0);
				} else {
					linePoints.put(m.constPar);
					linePoints.put(m.start);
					linePoints.put(0);
					linePoints.put(m.constPar);
					linePoints.put(m.stop);
					linePoints.put(0);
				}
				multCount[i-1]++;
			}
		}
		
		
		// evaluate the circle stuff
		for(Bspline b : functions) // first add in all center points
			putCenterPoint(b);
		for(Bspline b : animationSplines)
			putCenterPoint(b);
		
		for(Bspline b : functions) // then add in all circumference points
			putCircumfurencePoint(b);
		for(Bspline b : animationSplines)
			putCircumfurencePoint(b);
		
		for(int i=0; i<nBsplines; i++) {
			for(int j=0; j<CIRCLE_POINTS; j++) {
				circEdge.put((short) (nBsplines + CIRCLE_POINTS*i +   j)  );
				circEdge.put((short) (nBsplines + CIRCLE_POINTS*i + (j+1)%CIRCLE_POINTS));
				
				circInterior.put((short) i);
				circInterior.put((short) (nBsplines + CIRCLE_POINTS*i +   j)  );
				circInterior.put((short) (nBsplines + CIRCLE_POINTS*i + (j+1)%CIRCLE_POINTS));
			}
		}
		
		// reset all buffer iterators
		linePoints.position(0);
		circVerts.position(0);
		circVertsOrigin.position(0);
		circInterior.position(0);
		circEdge.position(0);
		selectedSpline.position(0);
		supportVertices.position(0);
		supportLines.position(0);
	}
	
	public void setSelectedSpline(Bspline b) {
		int globI = getIndexOf(b);
		if(globI < 0) {
			Log.println(Log.ERROR, "MyRenderer::setSelectedSpline", "Bspline not found in LRSpline object");
			return;
		}
		int nSplines = getNvisibleSplines();
		for(int i=0; i<LRSpline.CIRCLE_POINTS; i++) {
			selectedSpline.put((short) globI);
			selectedSpline.put((short) (nSplines + LRSpline.CIRCLE_POINTS*globI +   i                         ));
			selectedSpline.put((short) (nSplines + LRSpline.CIRCLE_POINTS*globI + (i+1)%LRSpline.CIRCLE_POINTS));
		}
		int p1 = getP(0);
		int p2 = getP(1);
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

		selectedSpline.position(0);
		supportVertices.position(0);
		supportLines.position(0);
	}
	
	public void putCenterPoint(Bspline b) {
		Point p = b.getGrevillePoint();
		circVerts.put(p.x);
		circVerts.put(p.y);
		circVerts.put(0);
		circVertsOrigin.put(b.origin.x);
		circVertsOrigin.put(b.origin.y);
		circVertsOrigin.put(0);
	}
	
	public void putCircumfurencePoint(Bspline b) {
		Point p = b.getGrevillePoint();
		float r = 0.025f*uMax;
		for(int i=0; i<CIRCLE_POINTS; i++) {
			double t = ((float) i)/(CIRCLE_POINTS-1) * 2 * Math.PI;
			double x = p.x + r*Math.cos(t);
			double y = p.y + r*Math.sin(t);
			double xo = b.origin.x + r*Math.cos(t);
			double yo = b.origin.y + r*Math.sin(t);
			circVerts.put((float) x);
			circVerts.put((float) y);
			circVerts.put(0);
			circVertsOrigin.put((float) xo);
			circVertsOrigin.put((float) yo);
			circVertsOrigin.put(0);
		}
	}
	
	public void expandKnotSpans() {
//		Log.println(Log.ASSERT, "expandKnotSpan", "Expanding knot lines");
		for(MeshLine m : lines) {
			m.constPar *= 2;
			m.start    *= 2;
			m.stop     *= 2;
		}
		for(Bspline b : functions)
			b.doubleKnotSpan();
		vMax *= 2;
		uMax *= 2;
	}
	
	public void contractKnotSpans() {
//		Log.println(Log.ASSERT, "contractKnotSpan", "Contracing knot lines");
		for(MeshLine m : lines) {
			m.constPar /= 2;
			m.start    /= 2;
			m.stop     /= 2;
		}
		for(Bspline b : functions)
			b.halveKnotSpan();
		vMax /= 2;
		uMax /= 2;
	}
	
	public int getMaxLineMult() {
		return multCount.length;
	}
	
	public int getMultCount(int mult) {
		return multCount[mult-1];
	}
	
	public Bspline getNearestSpline(Point p) {
		float maxDist = Float.MAX_VALUE;
		Bspline bestBet = null;
		for(Bspline b : functions) {
			float dist = p.dist2(b.getGrevillePoint()); 
			if(dist < maxDist) {
				maxDist = dist;
				bestBet = b;
			}
		}
		return bestBet;
	}
	
	public int getIndexOf(Bspline spline) {
		int i=0;
		for(Bspline b : functions) {
			if(b == spline) return i;
			else            i++;
		}
		for(Bspline b : animationSplines) {
			if(b == spline) return i;
			else            i++;
		}
		return -1;
	}
	
	public int getNvisibleSplines() {
		return functions.size() + animationSplines.size();
	}
	
	public Point snapEndToMesh(Point p, boolean spanU) {
		int i=0;
		float minDist = Float.MAX_VALUE;
		int minI = -1;
		for(MeshLine m : lines) {
			if(m.span_u == spanU) {
				i++;
				continue;
			}
			float d = p.dist(m);
			if(d < minDist) {
				minDist = d;
				minI    = i;
			}
			i++;
		}
		
		if(lines.get(minI).span_u) {
			return new Point(p.x, lines.get(minI).constPar);
		} else {
			return new Point(lines.get(minI).constPar, p.y);
		}
	}
	
	public Point snapToMesh(Point p) {
		// find closest meshline
		int i=0;
		float minDist = Float.MAX_VALUE;
		int minI = -1;
		for(MeshLine m : lines) {
			float d = p.dist(m);
			if(d < minDist) {
				minDist = d;
				minI    = i;
			}
			i++;
		}
		// figure out the closest grid point
		boolean topFree = true;
		boolean bottomFree = true;
		
		// improve snapping
		int constPar = lines.get(minI).constPar;
		float runningPar     = (lines.get(minI).span_u) ? p.x  : p.y;
		float crossingParMax = (lines.get(minI).span_u) ? vMax : uMax;
		for(MeshLine m : lines) {
			if(m.span_u == lines.get(minI).span_u) continue;
			if((m.start == 0 || m.start < constPar) && (constPar < m.stop || m.stop == crossingParMax)) {
				if(m.constPar == FloatMath.ceil(runningPar))
					topFree = false;
				else if(m.constPar == FloatMath.floor(runningPar))
					bottomFree = false;
			}
		}
		float snapVal;
		float top    = FloatMath.ceil(runningPar);
		float bottom = FloatMath.floor(runningPar);
		if(!topFree && !bottomFree) {
			snapVal = (top+bottom) / 2;
		} else if(runningPar - bottom > 0.5) { // snap up
			if(topFree)
				snapVal = top;
			else
				snapVal = bottom;
		} else { // snap down
			if(bottomFree)
				snapVal = bottom;
			else
				snapVal = top;
		}
		
		lastSnappedToUspan = lines.get(minI).span_u;
		
		if(lines.get(minI).span_u) {
			return new Point(snapVal, lines.get(minI).constPar);
		} else {
			return new Point(lines.get(minI).constPar, snapVal);
		}
	}
	
	public synchronized void terminateAnimation() {
		if(!inStepOne && ! inStepTwo)
			animationSplines.clear();
	}
	
	public boolean insertLine(Point p1, Point p2) {
//		Log.println(Log.ASSERT, "insertLine", "p1 = " + p1 + " p2 = " + p2);
		
		if(! (p1.x == p2.x || p1.y == p2.y)) // cannot insert non- vertical/horizontal lines
			return false;
		
		boolean insertSpanU = p1.y == p2.y;
		float start, stop, constPar;
		if(insertSpanU) {
			start = Math.min(p1.x, p2.x);
			stop  = Math.max(p1.x, p2.x);
			constPar = p1.y;
		} else {
			start = Math.min(p1.y, p2.y);
			stop  = Math.max(p1.y, p2.y);
			constPar = p1.x;
		}
		
		if(start == stop)
			return false;
		
		boolean didExpandKnots = false;
		if(constPar - FloatMath.floor(constPar) != 0) {
			expandKnotSpans();
			start	 *= 2;
			stop	 *= 2;
			constPar *= 2;
			didExpandKnots = true;
		}
		
		for(Bspline b : functions)
			b.origin = b.getGrevillePoint();
		
		boolean found = false;
		MeshLine newLine = null;
		for(MeshLine m : lines) {
			if(insertSpanU != m.span_u) continue;
			if(m.constPar == constPar) {
				if(m.start <= stop && start <= m.stop) {
					if(m.start <= start && stop <= m.stop) // completely covered by existing line
						return false;
					
					if(start < m.start)
						m.setStart((int) start);
					if(m.stop < stop)
						m.setStop((int) stop);
					newLine = m;
					found = true;
					break;
				}
			}
		}
		if(!found) {
			newLine = new MeshLine(insertSpanU, (int) constPar, (int) start, (int) stop, 1);
			lines.add(newLine);
		}
		
		for(Bspline b : functions) {
			if(b.splitBy(newLine) && !b.hasLine(newLine)) {
				activeM = newLine;
				activeB = b;
				inStepOne = true;
				return true;
			}
		}
		
		lines.remove(newLine);
		if(didExpandKnots)
			contractKnotSpans();
		
		return false;
		
	}
	
	public boolean setNextFocus() {
		Log.println(Log.DEBUG, "LRspline::setNextStuff", "entering");
		if(inStepOne) {
			for(Bspline b : functions) {
				if(b.splitBy(activeM) && !b.hasLine(activeM)) {
					activeB = b;
					return true;
				}
			}
		}
		inStepTwo = true;
		for(int i=newSpline.size(); i-->0 ; ) {
			Bspline b = newSpline.get(i);
			for(MeshLine m : lines) {
				if(b.splitBy(m) && !b.hasLine(m)) {
					activeM = m;
					activeB = b;
					return true;
				}
			}
		}
		
		cleanupSplitting();
		Log.println(Log.DEBUG, "LRspline::setNextStuff", "returning false");
		return false;
	}
	
	public synchronized void cleanupSplitting() {
		for(Bspline b : newSpline)
			functions.add(b);
		newSpline.clear();
		
//		for(Bspline b : functions)
//			Log.println(Log.DEBUG, "split review", b + " from " + b.origin + " to " + b.getGrevillePoint());

		inStepOne = false;
		inStepTwo = false;
	}
	
	public boolean inSplitting() {
		return inStepOne || inStepTwo;
	}
	
	public boolean isBreaking() {
		return breakStepOne || breakStepTwo;
	}
	
	public void setBreakpoints(boolean doBreak) {
		breakStepOne = doBreak;
		breakStepTwo = doBreak;
	}
	
	public synchronized boolean continueSplit() {
		Stack<Bspline> removeSpline = new Stack<Bspline>();
//		animationSplines.clear();
//		newSpline.clear();
		for(Bspline b : animationSplines)
			b.origin = b.getGrevillePoint();
		
		for(Bspline b : functions) {
			if(b.splitBy(activeM) && !b.hasLine(activeM)) {
				Bspline newB[] = b.split(!activeM.span_u, activeM.constPar);
				removeSpline.push(b);
				newSpline.add(newB[0]);
				newSpline.add(newB[1]);
				if(breakStepOne) {
					functions.remove(b);
					animationSplines.add(newB[0]);
					animationSplines.add(newB[1]);
					return false;
				}
			}
		}
		
		while(!newSpline.isEmpty()) {
			Bspline b = newSpline.remove(newSpline.size()-1);
			boolean isSplit = false;
			for(MeshLine m : lines) {
				if(b.splitBy(m) && !b.hasLine(m)) {
					Bspline newB[] = b.split(!m.span_u, m.constPar);
					removeSpline.push(b);
					newSpline.add(newB[0]);
					newSpline.add(newB[1]);
					if(breakStepTwo) {
						functions.remove(b);
						animationSplines.remove(b);
						animationSplines.add(newB[0]);
						animationSplines.add(newB[1]);
						return false;
					}
					isSplit = true;
					break;
				}
			}
			if(!isSplit) {
				functions.add(b);
				animationSplines.add(b);
			}
		}

		while(!removeSpline.isEmpty())
			functions.remove(removeSpline.pop());

		cleanupSplitting();
		
		return true;
	}
	
	public LRSpline copy() {
		LRSpline ans = new LRSpline(p1,p2,p1+1,p2+1);
		ans.lines.clear();
		ans.functions.clear();
		
		for(MeshLine m : lines)
			ans.lines.add(m.copy());
		for(Bspline b : functions)
			ans.functions.add(b.copy());
		return ans;
		
	}

}
