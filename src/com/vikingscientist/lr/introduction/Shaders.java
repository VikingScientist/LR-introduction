package com.vikingscientist.lr.introduction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

public class Shaders {


	
	int mProgram;
	int mProgramAnimate;
	
    int vertexShader    ;
    int animateShader   ;
    int fragmentShader  ;
    
//    int attPosStart = 0;
//    int attPos      = 1;
//    int uniMVP      = 0;
//    int uniTime     = 1;
//    int uniColor    = 2;
		
	public Shaders(Context ctx) {
		
		String vertexShaderCode    = "";
		String animateVertexShader = "";
		String fragmentShaderCode  = "";

		Resources res = ctx.getResources();
		
		// read all shaders from file
		try {
			vertexShaderCode    = readFile(res.openRawResource(R.raw.mvponly_vert));
			animateVertexShader = readFile(res.openRawResource(R.raw.animate_vert));
			fragmentShaderCode  = readFile(res.openRawResource(R.raw.onecolor_frag));
		} catch(IOException e) {
			if(ctx instanceof Activity) {
				Log.println(Log.ERROR, "Shaders::Shaders()", "Error reading file: " + e.getMessage());
				Log.println(Log.ERROR, "Shaders::Shaders()", "Terminating...");
				((Activity) ctx).finish();
			} else {
				Log.println(Log.ERROR, "Shaders::Shaders()", "Error reading file: " + e.getMessage());
			}
		}
		
		
	    vertexShader    = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
	    animateShader   = loadShader(GLES20.GL_VERTEX_SHADER, animateVertexShader);
	    fragmentShader  = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

	    mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
	    GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
	    GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
	    GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
	    
	    String infoLog = GLES20.glGetProgramInfoLog(mProgram);
	    Log.println(Log.DEBUG, "Program compile log", infoLog);
	    

	    mProgramAnimate = GLES20.glCreateProgram();
	    GLES20.glAttachShader(mProgramAnimate, animateShader);
	    GLES20.glAttachShader(mProgramAnimate, fragmentShader);
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
	
	private String readFile( InputStream file ) throws IOException {
	    BufferedReader reader = new BufferedReader( new InputStreamReader(file));
	    String         line = null;
	    StringBuilder  stringBuilder = new StringBuilder();
	    String         ls = System.getProperty("line.separator");

	    while( ( line = reader.readLine() ) != null ) {
	        stringBuilder.append( line );
	        stringBuilder.append( ls );
	    }

	    return stringBuilder.toString();
	}
}
