package com.vikingscientist.lr.introduction;

import android.util.FloatMath;

public class Point {
	
	float x;
	float y;
	
	public Point() {
		x = 0;
		y = 0;
	}
	
	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public float dist(MeshLine m) {
		if(m.span_u) {
			if(m.start < x && x < m.stop) 
				return Math.abs(y-m.constPar);
			else
				return Float.MAX_VALUE;
		} else {
			if(m.start < y && y < m.stop) 
				return Math.abs(x-m.constPar);
			else
				return Float.MAX_VALUE;
		}
	}
	
	public void multiply(float m) {
		x *= m;
		y *= m;
	}
	
	public float dist2(Point p) {
		return (p.x-x)*(p.x-x) + (p.y-y)*(p.y-y);
	}
	
	public float dist(Point p) {
		return FloatMath.sqrt(dist2(p));
	}
	
	public Point copy() {
		return new Point(x,y);
	}
	
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

}
