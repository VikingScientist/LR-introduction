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
	
	// core LR B-spline information
	int p1, p2;
	ArrayList<MeshLine> lines            = new ArrayList<MeshLine>();
	HashSet<Bspline>    functions        = new HashSet<Bspline>();
	ArrayList<Bspline>  animationSplines = new ArrayList<Bspline>();
	
	// nice-to-have variables
	int uMax;
	int vMax;
	boolean lastSnappedToUspan;
	
	// rendering information and buffers
	volatile FloatBuffer linePoints;
	volatile FloatBuffer circVerts;
	volatile ShortBuffer circInterior;
	volatile ShortBuffer circEdge;
	volatile FloatBuffer circVertsOrigin;
	
	volatile int         multCount[];
	volatile int         circIntCount;
	volatile int         circEdgeCount;
	
	public LRSpline(int p1, int p2, int n1, int n2) {
		this.p1 = p1;
		this.p2 = p2;
		uMax = n1-p1;
		vMax = n2-p2;
		
		// build the global knot vector
		int knotU[] = new int[p1+n1+1];
		int knotV[] = new int[p1+n1+1];
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
		
//		for(Bspline b : functions) 
//			Log.println(Log.ASSERT, "all hash values", "" + b.hashCode() + " for spline " + b);
		
	}
	
	public int getWidth() {
		return uMax;
	}
	
	public int getHeight() {
		return vMax;
	}
	
	public void buildBuffers() {
		// allocate new buffers
		int nBsplines = functions.size() + animationSplines.size();
		
		ByteBuffer bb = ByteBuffer.allocateDirect(lines.size()*4*4);
		bb.order(ByteOrder.nativeOrder());
		linePoints = bb.asFloatBuffer();
		
		bb = ByteBuffer.allocateDirect(4*(CIRCLE_POINTS+1)*2*nBsplines);
		bb.order(ByteOrder.nativeOrder());
		circVerts = bb.asFloatBuffer();
		
		bb = ByteBuffer.allocateDirect(2*(CIRCLE_POINTS)*3*nBsplines);
		bb.order(ByteOrder.nativeOrder());
		circInterior = bb.asShortBuffer();
		
		bb = ByteBuffer.allocateDirect(2*(CIRCLE_POINTS)*2*nBsplines);
		bb.order(ByteOrder.nativeOrder());
		circEdge = bb.asShortBuffer();
		
		bb = ByteBuffer.allocateDirect(4*(CIRCLE_POINTS+1)*2*nBsplines);
		bb.order(ByteOrder.nativeOrder());
		circVertsOrigin = bb.asFloatBuffer();
		
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
					linePoints.put(m.stop);
					linePoints.put(m.constPar);
				} else {
					linePoints.put(m.constPar);
					linePoints.put(m.start);
					linePoints.put(m.constPar);
					linePoints.put(m.stop);
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
		
	}
	
	public void putCenterPoint(Bspline b) {
		Point p = b.getGrevillePoint();
		circVerts.put(p.x);
		circVerts.put(p.y);
		circVertsOrigin.put(b.origin.x);
		circVertsOrigin.put(b.origin.y);
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
			circVertsOrigin.put((float) xo);
			circVertsOrigin.put((float) yo);
		}
	}
	
	public void expandKnotSpans() {
		Log.println(Log.ASSERT, "expandKnotSpan", "Expanding knot lines");
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
	
	public int getMaxLineMult() {
		return multCount.length;
	}
	
	public int getMultCount(int mult) {
		return multCount[mult-1];
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
		
		if(constPar - FloatMath.floor(constPar) != 0) {
			expandKnotSpans();
			start	 *= 2;
			stop	 *= 2;
			constPar *= 2;
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
		
		Stack<Bspline> newSpline    = new Stack<Bspline>();
		Stack<Bspline> removeSpline = new Stack<Bspline>();
		animationSplines.clear();
		boolean dimensionIncrease = false;
		for(Bspline b : functions) {
			if(b.splitBy(newLine) && !b.hasLine(newLine)) {
				Bspline newB[] = b.split(!newLine.span_u, newLine.constPar);
				removeSpline.push(b);
				newSpline.push(newB[0]);
				newSpline.push(newB[1]);
				dimensionIncrease = true;
			}
		}
		
		if(!dimensionIncrease) {
			lines.remove(newLine);
			return false;
		}
//		Log.println(Log.ASSERT, "InsertLine beforemath", "# bsplines       = " + functions.size());
//		Log.println(Log.ASSERT, "InsertLine beforemath", "# lines          = " + lines.size());
//		Log.println(Log.ASSERT, "InsertLine beforemath", "# new splines    = " + newSpline.size());
//		Log.println(Log.ASSERT, "InsertLine beforemath", "# remove splines = " + removeSpline.size());
//		Log.println(Log.ASSERT, "InsertLine beforemath", "");
		
		while(!newSpline.isEmpty()) {
			Bspline b = newSpline.pop();
			boolean isSplit = false;
			for(MeshLine m : lines) {
				if(b.splitBy(m) && !b.hasLine(m)) {
					Bspline newB[] = b.split(!m.span_u, m.constPar);
					removeSpline.push(b);
					newSpline.push(newB[0]);
					newSpline.push(newB[1]);
					isSplit = true;
					break;
				}
			}
			if(!isSplit) {
				functions.add(b);
				animationSplines.add(b);
			}
		}
		

//		Log.println(Log.ASSERT, "InsertLine midmath", "# bsplines       = " + functions.size());
//		Log.println(Log.ASSERT, "InsertLine midmath", "# lines          = " + lines.size());
//		Log.println(Log.ASSERT, "InsertLine midmath", "# new splines    = " + newSpline.size());
//		Log.println(Log.ASSERT, "InsertLine midmath", "# remove splines = " + removeSpline.size());
//		Log.println(Log.ASSERT, "InsertLine midmath", "");

		while(!removeSpline.isEmpty())
			functions.remove(removeSpline.pop());
		
//		Log.println(Log.ASSERT, "InsertLine aftermath", "# bsplines       = " + functions.size());
//		Log.println(Log.ASSERT, "InsertLine aftermath", "# lines          = " + lines.size());
//		Log.println(Log.ASSERT, "InsertLine aftermath", "# new splines    = " + newSpline.size());
//		Log.println(Log.ASSERT, "InsertLine aftermath", "# remove splines = " + removeSpline.size());
//		Log.println(Log.ASSERT, "InsertLine aftermath", "" );
//		
//		for(Bspline b : functions) 
//			Log.println(Log.ASSERT, "all hash values", "" + b.hashCode() + " for spline " + b);
		
		for(Bspline b : functions)
			Log.println(Log.DEBUG, "split review", b + " from " + b.origin + " to " + b.getGrevillePoint());
		
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
