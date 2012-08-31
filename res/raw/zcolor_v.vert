
/**
  * zcolor - vertex shader
  *
  * Do a linear interpolation from start- to end-position by
  * the time-uniform (between 0.0 and 1.0)
  * Use the z-component for coloring by jet colorscheme
  *
  * Author: Kjetil A. Johannessen
  * Date: August 2012
  *
  */

attribute vec4 vPosition;
attribute vec4 vPositionStart;
varying vec4 color_ans;
uniform float maximumVal;
uniform float minimumVal;
uniform float time;
uniform bool justBlack;
uniform mat4 mMVP;


vec4 valToColor(float val, float maxval, float minval) {

	 vec4 color = vec4(1,1,1,1);
	 float angle = (val - minval);                  // 52 *180;
	 float alpha;
	 float scalefactor = 1.0;
	 float a = 1.0;                                 // radius brightness
	 float wdelta = (maxval-minval)/4.0; 
	
	 if( 0.0*wdelta <= angle && angle < 1.0*wdelta) //100->110
	   {
	     alpha=(angle-0.0*wdelta)/(wdelta);         // wdelta;
	     color.x =0.0;                              // null
	     color.y =alpha*a*scalefactor;              // gruen faellt
	     color.z =1.0*a*scalefactor;                // blau bleibt
	   }
	 if( 1.0*wdelta <= angle && angle <2.0*wdelta)  // 110->010
	   {
	     alpha=(angle-1.0*wdelta)/(wdelta);         // wdelta;
	     color.x =0.0;                              // null
	     color.y =1.0*a*scalefactor;                // gruen bleibt
	     color.z =(1.0-alpha)*a*scalefactor;        // blau steigt
	   }
	 if( 2.0*wdelta <= angle && angle < 3.0*wdelta) // 010->011
	   {
	     alpha=(angle-2.0*wdelta)/(wdelta);
	     color.x =alpha*a*scalefactor;              // rot faellt
	     color.y =1.0*a*scalefactor;                // gruen bleibt
	     color.z =0.0;                              // null
	   }
	 if( 3.0*wdelta <= angle && angle <= 4.0*wdelta) // 011->001
	   {
	     alpha=(angle-3.0*wdelta)/(wdelta);
	     color.x=1.0*a*scalefactor;                  // rot bleibt
	     color.y=(1.0-alpha)*a*scalefactor;          // gruen steigt
	     color.z=0.0;                                // null
	   }
	
	 //special zero field case
	 if (maxval == minval){
	     color.x=0.0;
	     color.y=0.0;
	     color.z=1.0;
	 }
	
	 if (maxval < minval) {
	     color.x=0.0;
	     color.y=0.0;
	     color.z=0.0;
	 }
	 
	 return color;
}


void main(void)
{
	vec4 pos = vec4(0,0,0,1);
//	pos.x = vPositionStart.x*(1.0-time) + time*vPosition.x;
//	pos.y = vPositionStart.y*(1.0-time) + time*vPosition.y;
//	pos.z = vPositionStart.z*(1.0-time) + time*vPosition.z;
	
	if(justBlack) {
		color_ans = vec4(0,0,0,1);
	} else {
		color_ans = valToColor(vPosition.z, maximumVal, minimumVal);
	}
	// color_ans = valToColor(pos.z, maximumVal, minimumVal);
	// color_ans = vec4(1,0,0,0); 

	pos = mMVP * vPosition;
	
	if(justBlack) {
		pos.z = pos.z - 1e-5;
	}
	gl_Position = pos;
	
	// gl_Position = mMVP * pos;
	// gl_Position = mMVP * vPosition;
    
}
