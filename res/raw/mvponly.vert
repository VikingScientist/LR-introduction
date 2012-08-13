
/**
  * mvponly - vertex shader
  *
  * Multiply by model-view-projection matrix
  *
  * Author: Kjetil A. Johannessen
  * Date: August 2012
  *
  */

attribute vec4 vPosition;
uniform mat4 mMVP;
void main() {
	gl_Position = mMVP*vPosition;
}