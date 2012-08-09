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
	
	private final String animateVertexShader =
		    "attribute vec4 vPositionStart;   \n" +
		    "attribute vec4 vPosition;        \n" +
		    "uniform mat4 mMVP;               \n" + 
		    "uniform float time;              \n" +
		    "void main() {                    \n" +
//		    "  gl_Position = mMVP * ((1.0f-time)*vPositionStart + time*vPosition); \n" +
		    "  vec4 pos = vec4(0,0,0,1);" +
		    "  pos.x = vPositionStart.x*(1.0-time) + time*vPosition.x;" +
		    "  pos.y = vPositionStart.y*(1.0-time) + time*vPosition.y;" +
		    "  gl_Position = mMVP * pos; \n" +
//		    "  gl_Position.x *= time; \n" +
//		    "  gl_Position.y *= time; \n" +
		    "} \n";

	private final String fragmentShaderCode =
		    "precision mediump float;" +
		    "uniform vec4 vColor;" +
		    "void main() {" +
		    "  gl_FragColor = vColor;" +
		    "}";
	
	int mProgram;
	int mProgramAnimate;
	
    int vertexShader    ;
    int animateShader   ;
    int fragmentShader  ;
    int fragmentShader2 ;

    
//    int attPosStart = 0;
//    int attPos      = 1;
//    int uniMVP      = 0;
//    int uniTime     = 1;
//    int uniColor    = 2;
		
	public Shaders() {
	    
	    vertexShader    = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
	    animateShader   = loadShader(GLES20.GL_VERTEX_SHADER, animateVertexShader);
	    fragmentShader  = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
	    fragmentShader2 = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

	    mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
	    GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
	    GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
	    GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
	    
	    String infoLog = GLES20.glGetProgramInfoLog(mProgram);
	    Log.println(Log.DEBUG, "Program compile log", infoLog);
	    

	    mProgramAnimate = GLES20.glCreateProgram();
	    GLES20.glAttachShader(mProgramAnimate, animateShader);
	    GLES20.glAttachShader(mProgramAnimate, fragmentShader2);
	    GLES20.glLinkProgram(mProgramAnimate);
	    
	    infoLog = GLES20.glGetProgramInfoLog(mProgramAnimate);
	    Log.println(Log.DEBUG, "Program compile log", infoLog);

	}
		
	public static int loadShader(int type, String shaderCode){

	    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	    int shader = GLES20.glCreateShader(type);

	    // add the source code to the shader and compile it
	    GLES20.glShaderSource(shader, shaderCode);
	    GLES20.glCompileShader(shader);
	    
	    String infoLog = GLES20.glGetShaderInfoLog(shader);
	    Log.println(Log.DEBUG, "Shader compile log", infoLog);
	    
	    return shader;
	}
	
	public int getProgram() {
	    return mProgram;
	}
	
	public int getAnimateProgram() {
	    return mProgramAnimate;
	}
}
