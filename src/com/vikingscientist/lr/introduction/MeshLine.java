package com.vikingscientist.lr.introduction;

public class MeshLine {

	boolean span_u ;
	int constPar;
	int start;
	int stop;
	int mult;
	
	public MeshLine(boolean span_u, int constPar, int start, int stop, int mult) {
		this.start		= start;
		this.stop		= stop;
		this.constPar	= constPar;
		this.span_u		= span_u;
		this.mult		= mult;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public void setStop(int stop) {
		this.stop = stop;
	}
	
	public void setConstPar(int constPar) {
		this.constPar = constPar;
	}
	
}
