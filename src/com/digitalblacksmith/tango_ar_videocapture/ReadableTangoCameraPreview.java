package com.digitalblacksmith.tango_ar_videocapture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.google.atap.tangoservice.TangoCameraPreview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class ReadableTangoCameraPreview extends TangoCameraPreview  {
	
	
	//hack--remove later
	public final static int SCREEN_GRAB_X = 760;
	public final static int SCREEN_GRAB_Y = 400;
	public final static int SCREEN_GRAB_W = 400;
	public final static int SCREEN_GRAB_H = 400;
	
	
	Activity mainActivity;
	private static final String TAG = ReadableTangoCameraPreview.class.getSimpleName();
	
	ScreenGrabRenderer screenGrabRenderer;
	
	IntBuffer intBuffer;

	private boolean takeSnapShot = false;
	
	/**
	 * Set a pointer back to the main int buffer used to grab images
	 * @param aIntBuffer
	 */
	public void setIntBuffer(IntBuffer aIntBuffer) {
		intBuffer = aIntBuffer;
	}
	
	
	@Override
	public void setRenderer(GLSurfaceView.Renderer renderer) {		
		setEGLContextClientVersion(2);
		screenGrabRenderer= new ScreenGrabRenderer(this, renderer);
		super.setRenderer(screenGrabRenderer);
		Log.i(TAG,"Intercepted the renderer!!!");		
	}
	
	/*
	 * Pass the callback down to where it will be set
	 */
	public void setScreengrabCallback(TangoCameraScreengrabCallback aTangoCameraScreengrabCallback) {		 
		 screenGrabRenderer.setTangoCameraScreengrabCallback(aTangoCameraScreengrabCallback);
	}

	/**
	 * Set a trigger for snapshot.  Call this from main activity
	 * in response to a use input
	 */
	public void takeSnapShot() {
		takeSnapShot = true;
	}	

	@Override
	public void onFrameAvailable() {
		super.onFrameAvailable();
		if(takeSnapShot) {
			//screenGrabWithRoot();				
			//screenGrabRenderer.grabNextScreen(0,0,this.getWidth(),this.getHeight(), intBuffer);
			screenGrabRenderer.grabNextScreen(SCREEN_GRAB_X,SCREEN_GRAB_Y,SCREEN_GRAB_W,SCREEN_GRAB_H, intBuffer);
			takeSnapShot = false;			
		}
	}

	public ReadableTangoCameraPreview(Activity context) {
		super(context);	
		mainActivity = context;
		//Make an OpenGL2 context
		
		// TODO Auto-generated constructor stub
	}
	
	
	
}
