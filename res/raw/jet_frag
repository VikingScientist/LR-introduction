
#version 120

#-------------------------------------------------------------------------
# FragmentShader
#
# Author: Annette Stahl
# Date:   August 2012
#
#-------------------------------------------------------------------------


//uniform sampler2D qt_Texture0;
//varying vec4 qt_TexCoord0;
uniform mediump vec4 color;

in mediump vec2 maxminval;
in mediump float coli;
in mediump float black;
//attribute highp vec4 colorvalues;
//uniform mediump vec4 color1;





vec3 valToColor(float val, float maxval, float minval){

 vec3 color;
 float angle;
 float alpha;
 float scalefactor;
 float wdelta;
 wdelta = (maxval-minval)/4.0;
 scalefactor=1.0;//zwischen 0u1 oder0u255
 angle = (val - minval);///52 *180;
 float  a = 1.0;//radius brightness

 if( 0.0*wdelta <= angle && angle < 1.0*wdelta) //100->110
   {
     alpha=(angle-0.0*wdelta)/(wdelta);//wdelta;
     color.x =0.0;     // null
     color.y =alpha*a*scalefactor; // gruen faellt
     color.z =1.0*a*scalefactor; // blau bleibt
   }
 if( 1.0*wdelta <= angle && angle <2.0*wdelta) //110->010
   {
     alpha=(angle-1.0*wdelta)/(wdelta);//wdelta;
     color.x =0.0;     // null
     color.y =1.0*a*scalefactor; // gruen bleibt
     color.z =(1.0-alpha)*a*scalefactor; // blau steigt
   }
 if( 2.0*wdelta <= angle && angle < 3.0*wdelta)// 010->011
   {
     alpha=(angle-2.0*wdelta)/(wdelta);
     color.x =alpha*a*scalefactor; // rot faellt
     color.y =1.0*a*scalefactor; // gruen bleibt
     color.z =0.0; // null
   }
 if( 3.0*wdelta <= angle && angle <= 4.0*wdelta) //011->001
   {
     alpha=(angle-3.0*wdelta)/(wdelta);
     color.x=1.0*a*scalefactor; // rot bleibt
     color.y=(1.0-alpha)*a*scalefactor; //gruen steigt
     color.z=0.0; // null
   }

 //special zero field case
 if (maxval == minval){
     color.x=0.0;
     color.y=0.0;
     color.z=1.0;}

 if (maxval < minval){
     color.x=0.0;
     color.y=0.0;
     color.z=0.0;}


 return color;
}


void main(void)
{

vec3 mycolor;
//float max = 1.3;
//float min = 0.0;

mycolor = valToColor(coli, maxminval.x, maxminval.y);



    //gl_FragColor = vec4(0.0, 0.2, 1.0, 1.0);
    gl_FragColor = gl_Color;
    gl_FragColor = vec4(mycolor.x,mycolor.y,mycolor.z,1.0);
    //gl_FragColor = colorvalues;
    //gl_FrontColor = gl_Color;
    //gl_FragColor = texture2D(qt_Texture0, qt_TexCoord0.st);
}
