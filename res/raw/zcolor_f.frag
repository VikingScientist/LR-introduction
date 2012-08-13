
/**
  * zcolor - fragment shader
  *
  * Do a linear interpolation from start- to end-position by
  * the time-uniform (between 0.0 and 1.0)
  * Use the z-component for coloring by jet colorscheme
  *
  * Author: Kjetil A. Johannessen
  * Date: August 2012
  *
  */
  
precision mediump float;
varying vec4 color_ans;

void main(void)
{
    gl_FragColor = color_ans;
}