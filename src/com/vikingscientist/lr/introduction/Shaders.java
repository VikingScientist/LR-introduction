package com.vikingscientist.lr.introduction;

import android.opengl.GLES20;
import android.util.Log;

public class Shaders {
	
	private final String vertexShaderCode =
		    "attribute vec4 vPosition;" +
		    "uniform mat4 mMVP;" +
		    "void main() {" +
		    "  gl_Position = mMVP*vPosition;" +
		    "}";

	private final String fragmentShaderCode =
		    "precision mediump float;" +
		    "uniform vec4 vColor;" +
		    "void main() {" +
		    "  gl_FragColor = vColor;" +
		    "}";
	
	int mProgram;
		
	public Shaders() {
	    
	    int vertexShader   = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
	    int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

	    mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
	    GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
	    GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
	    GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
	}
		
	public static int loadShader(int type, String shaderCode){

	    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	    int shader = GLES20.glCreateShader(type);

	    // add the source code to the shader and compile it
	    GLES20.glShaderSource(shader, shaderCode);
	    GLES20.glCompileShader(shader);

	    return shader;
	}
	
	public int getProgram() {
		return mProgram;
	}
}
