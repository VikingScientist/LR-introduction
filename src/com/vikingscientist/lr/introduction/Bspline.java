package com.vikingscientist.lr.introduction;

import java.util.Arrays;

import android.util.Log;

public class Bspline {
	
	int knotU[];
	int knotV[];
	
	int knotU_iterator = 0;
	int knotV_iterator = 0;
	int hc = 0;
	
	Point origin;
	
	public Bspline(int p1, int p2) {
		this.knotU = new int[p1+2];
		this.knotV = new int[p1+2];
		origin = new Point(0,0);
	}
	
	public Bspline(int p1, int p2, int knotU[], int knotV[]) {
		this.knotU = new int[p1+2];
		this.knotV = new int[p1+2];
		for(int i=0; i<p1+2; i++)
			this.knotU[i] = knotU[i];
		for(int i=0; i<p2+2; i++)
			this.knotV[i] = knotV[i];
		origin = new Point(0,0);
	}
	
	public void putU(int u) {
		knotU[knotU_iterator++] = u;
	}
	
	public void putV(int v) {
		knotV[knotV_iterator++] = v;
	}
	
	public int getKnotU(int i) {
		return knotU[i];
	}
	
	public int getKnotV(int i) {
		return knotV[i];
	}
	
	public void resetIterators() {
		knotU_iterator = 0;
		knotV_iterator = 0;
	}
	
	public void doubleKnotSpan() {
		for(int i=0; i<knotU.length; i++)
			knotU[i] *= 2;
		for(int i=0; i<knotV.length; i++)
			knotV[i] *= 2;
		origin.multiply(2);
	}
	
	public void halveKnotSpan() {
		for(int i=0; i<knotU.length; i++)
			knotU[i] /= 2;
		for(int i=0; i<knotV.length; i++)
			knotV[i] /= 2;
		origin.multiply(0.5f);
	}
	
	public int umin() {
		return knotU[0];
	}
	public int vmin() {
		return knotV[0];
	}
	public int umax() {
		return knotU[knotU.length-1];
	}
	public int vmax() {
		return knotV[knotU.length-1];
	}
	
	public Point getGrevillePoint() {
		float x = 0;
		float y = 0;
		for(int i=1; i<knotU.length-1; i++)
			x += knotU[i];
		for(int i=1; i<knotV.length-1; i++)
			y += knotV[i];
		x /= (knotU.length-2);
		y /= (knotV.length-2);
		return new Point(x,y);
	}
	
	public Point getKnotMedian() {
		float x = 0;
		float y = 0;
		if(knotU.length % 2 == 1)
			x = knotU[knotU.length/2];
		else
			x = (knotU[knotU.length/2] + knotU[knotU.length/2-1]) / 2.0f;
		
		if(knotV.length % 2 == 1)
			y = knotV[knotV.length/2];
		else
			y = (knotV[knotV.length/2] + knotV[knotV.length/2-1]) / 2.0f;
		
		return new Point(x,y);
	}
	
	public int getWidth() {
		return knotU[knotU.length-1] - knotU[0];
	}
	
	public int getHeight() {
		return knotV[knotV.length-1] - knotV[0];
	}
	
	public String getKnotU() {
		String ans = "";
		for(int i=0; i<knotU.length-1; i++)
			ans += knotU[i] + " ";
		ans += knotU[knotU.length-1];
		return ans;
	}
	
	public String getKnotV() {
		String ans = "";
		for(int i=0; i<knotV.length-1; i++)
			ans += knotV[i] + " ";
		ans += knotV[knotV.length-1];
		return ans;
	}
	
	public double evaluate(double u, double v, boolean u_from_right, boolean v_from_right)  {
		int order_u_ = knotU.length-1;
		int order_v_ = knotV.length-1;
		
	    if(knotU[0] > u || u > knotU[order_u_])
	        return 0;
	    if(knotV[0] > v || v > knotV[order_v_])
	        return 0;

	    double ans_u[] = new double[order_u_];
	    double ans_v[] = new double[order_v_];

	    for(int i=0; i<order_u_; i++) {
	        if(u_from_right)
	            ans_u[i] = (knotU[i] <= u && u <  knotU[i+1]) ? 1 : 0;
	        else
	            ans_u[i] = (knotU[i] <  u && u <= knotU[i+1]) ? 1 : 0;
	    }
	    for(int n=1; n<order_u_; n++)
	        for(int j=0; j<order_u_-n; j++) {
	            ans_u[j]  = (knotU[ j+n ]==knotU[ j ]) ? 0 : (  u-knotU[j]  )/(knotU[j+n]  -knotU[ j ])*ans_u[ j ];
	            ans_u[j] += (knotU[j+n+1]==knotU[j+1]) ? 0 : (knotU[j+n+1]-u)/(knotU[j+n+1]-knotU[j+1])*ans_u[j+1];
	    }

	    for(int i=0; i<order_v_; i++) {
	        if(v_from_right)
	            ans_v[i] = (knotV[i] <= v && v <  knotV[i+1]) ? 1 : 0;
	        else
	            ans_v[i] = (knotV[i] <  v && v <= knotV[i+1]) ? 1 : 0;
	    }
	    for(int n=1; n<order_v_; n++)
	        for(int j=0; j<order_v_-n; j++) {
	            ans_v[j]  = (knotV[ j+n ]==knotV[ j ]) ? 0 : (  v-knotV[j]  )/(knotV[j+n]  -knotV[ j ])*ans_v[ j ];
	            ans_v[j] += (knotV[j+n+1]==knotV[j+1]) ? 0 : (knotV[j+n+1]-v)/(knotV[j+n+1]-knotV[j+1])*ans_v[j+1];
	    }

	    return ans_u[0]*ans_v[0];
	}
	
	public boolean hasLine(MeshLine m) {
		int hits = 0;
		if(m.span_u) {
			if(knotV[0] == m.constPar || knotV[knotV.length-1] == m.constPar) // touches edge
				return true;
			for(int i=0; i<knotV.length; i++)
				if(knotV[i] == m.constPar)
					hits++;
		} else {
			if(knotU[0] == m.constPar || knotU[knotU.length-1] == m.constPar) // touches edge
				return true;
			for(int i=0; i<knotU.length; i++)
				if(knotU[i] == m.constPar)
					hits++;
		}
		return hits == m.mult;
	}
	
	public boolean splitBy(MeshLine m) {
		if(m.span_u) {
			return  knotV[0]              < m.constPar &&
					knotV[knotV.length-1] > m.constPar &&
					knotU[0]              >= m.start &&
					knotU[knotU.length-1] <= m.stop;	
		} else {
			return  knotU[0]              < m.constPar &&
					knotU[knotU.length-1] > m.constPar &&
					knotV[0]              >= m.start &&
					knotV[knotV.length-1] <= m.stop;
		}
	}
	
	public boolean overlaps(MeshLine m) {
		if(m.span_u) {
			return  knotV[0]              < m.constPar &&
					knotV[knotV.length-1] > m.constPar &&
					knotU[0]              < m.stop &&
					knotU[knotU.length-1] > m.start;	
		} else {
			return  knotU[0]              < m.constPar &&
					knotU[knotU.length-1] > m.constPar &&
					knotV[0]              < m.stop &&
					knotV[knotV.length-1] > m.start;
		}
	}
	
	public Bspline[] split(boolean inU, int value) {
		if( (  inU && !(knotU[0] <= value && value <= knotU[knotU.length-1]) ) ||
		    ( !inU && !(knotV[0] <= value && value <= knotV[knotV.length-1]) )  ) {
		    
			Log.println(Log.ASSERT, "Bspline::split()", "Invalid " + ((inU)?"u":"v") + "-knot insertion value=" + value + " in spline " + this);
			return null;
		}
		
		Bspline b1 = this.copy();
		Bspline b2 = this.copy();
		b1.resetIterators();
		b2.resetIterators();
		
		if(inU) {
			int newKnots[] = new int[knotU.length+1];
			for(int i=0; i<knotU.length; i++)
				newKnots[i] = knotU[i];
			newKnots[knotU.length] = value;
			Arrays.sort(newKnots);
			for(int i=0; i<knotU.length; i++) {
				b1.putU(newKnots[i]);
				b2.putU(newKnots[i+1]);
			}
		} else {
			int newKnots[] = new int[knotV.length+1];
			for(int i=0; i<knotV.length; i++)
				newKnots[i] = knotV[i];
			newKnots[knotV.length] = value;
			Arrays.sort(newKnots);
			for(int i=0; i<knotV.length; i++) {
				b1.putV(newKnots[i]);
				b2.putV(newKnots[i+1]);
			}
		}
		
		Bspline ans[] = {b1, b2};
		return ans;
		
	}
	
	public int hashCode() {
		if(hc != 0) // use cached value
			return hc;
			
		int max = (knotU.length < 5) ? knotU.length : 5;
		for(int i=0; i<max; i++)
			hc = hc ^ ( (knotU[i] & 7) << (i*3) );
		max = (knotV.length < 5) ? knotV.length : 5;
		for(int i=0; i<max; i++)
			hc = hc ^ ( (knotV[i] & 7) << (i*3+15) );
		
		return hc;
	}
	
	public boolean equals(Object obj) {
		if(obj == null || ! (obj instanceof Bspline))
			return false;
		
		Bspline b = (Bspline) obj;
		
		for(int i=0; i<knotU.length; i++)	
			if(knotU[i] != b.getKnotU(i))
				return false;
		for(int i=0; i<knotV.length; i++)	
			if(knotV[i] != b.getKnotV(i))
				return false;
		
		return true;
	}
	
	public Bspline copy() {
		Bspline newSpline = new Bspline(knotU.length-2, knotV.length-2, knotU, knotV);
		newSpline.origin = origin.copy();
		return newSpline;
	}
	
	public String toString() {
		String s = "";
		s += "KnotU = [";
		for(int i=0; i<knotU.length-1; i++)
			s += knotU[i] + ", " ;
		s += knotU[knotU.length-1] + "]  knotV = [";
		for(int i=0; i<knotV.length-1; i++)
			s += knotV[i] + ", " ;
		s += knotV[knotV.length-1] + "]";
		return s; 
	}
	

}
