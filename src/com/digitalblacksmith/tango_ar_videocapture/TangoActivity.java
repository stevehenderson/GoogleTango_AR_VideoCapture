/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalblacksmith.tango_ar_videocapture;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCameraPreview;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.tangoutils.renderables.PointCloud;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PixelFormat;
import android.media.ThumbnailUtils;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * 
 * Modified Main Activity class from the Original Google Tango SDK  Motion Tracking API Sample. 
 * 
 * Creates a GLSurfaceView for the OpenGL scene, which displays a cube
 * Then adds a SurfaceView for the camera image.  The surface is connected 
 * to the Tango camera.  This is necessary if one wants to get point cloud
 * data from the Tango AND use the camera for video-see through Augmented Reality.
 * 
 * Lessons learned:  Ensure your onPause and onResume actions are handled correctly
 * in terms of disconnecting and reconnecting the Tango!!  If the Tango is not
 * disconnected and reconnected properly, you will get a black background and
 * may think the issue is something else.
 * 
 * @author  Steve Henderson @stevehenderson 
 * 
 */
public class TangoActivity extends Activity implements TangoCameraScreengrabCallback, View.OnClickListener {

	private ReadableTangoCameraPreview tangoCameraPreview;
	private Tango mTango;

	private static final String TAG = TangoActivity.class.getSimpleName();
	private static final int SECS_TO_MILLISECS = 1000;

	private TangoConfig mConfig;
	private TextView mPoseTextView;
	private TextView mQuatTextView;
	private TextView mPoseStatusTextView;
	private TextView mTangoServiceVersionTextView;	
	private TextView mTangoEventTextView;
	private TextView mPointCountTextView;
	private TextView mAverageZTextView;
	private TextView mFrequencyTextView;
	private float mPreviousTimeStamp;
	private int mPreviousPoseStatus;
	private int count;	
	private double FOV = 0.665313;
	int maxDepthPoints = 20000;
	int markerCount=0;
	int MAX_BILLBOARDS = 5;
	PointCloud mPointCloud;

	private Button mMotionResetButton;
	private Button mDropBoxButton;
	//private boolean mIsAutoRecovery;

	//private PCRenderer mOpenGL2Renderer;
	private OpenGL2PointCloudRenderer mOpenGL2Renderer;
	private OpenGL2PointCloudRenderer mDemoRenderer;
	private GLSurfaceView mGLView;



	private float mXyIjPreviousTimeStamp;
	private float mCurrentTimeStamp;

	boolean first_initialized = false;

	Surface tangoSurface;

	Vector3f lastPosition;
	Vector3f dropBoxPosition;

	int bitmapBuffer[];
	int bitmapSource[];
	IntBuffer intBuffer;

	private void setupUI() {
		/////////////////////////
		//Create UI Objects 
		////////////////////////
		LayoutInflater inflater = getLayoutInflater();
		View tmpView;
		tmpView = inflater.inflate(R.layout.activity_motion_tracking, null);
		getWindow().addContentView(tmpView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT)); 


		// Button to reset motion tracking
		mMotionResetButton = (Button) findViewById(R.id.resetmotion);
		// Set up button click listeners
		mMotionResetButton.setOnClickListener(this);

		// Button to drop position box (breadcrumb cube)
		mDropBoxButton = (Button) findViewById(R.id.dropbox);
		// Set up button click listeners
		mDropBoxButton.setOnClickListener(this);

		// Text views for displaying translation and rotation data
		mPoseTextView = (TextView) findViewById(R.id.pose);
		mQuatTextView = (TextView) findViewById(R.id.quat);

		mTangoEventTextView = (TextView) findViewById(R.id.tangoevent);
		mPointCountTextView = (TextView) findViewById(R.id.pointCount);
		mAverageZTextView = (TextView) findViewById(R.id.averageZ);
		mFrequencyTextView = (TextView) findViewById(R.id.frameDelta);

		// Text views for the status of the pose data and Tango library versions		
		mTangoServiceVersionTextView = (TextView) findViewById(R.id.version);

		dropBoxPosition = new Vector3f();
		lastPosition = new Vector3f();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tangoCameraPreview = new ReadableTangoCameraPreview(this);
		mTango = new Tango(this);
		startActivityForResult(
				Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING),
				Tango.TANGO_INTENT_ACTIVITYCODE);

		
		tangoCameraPreview.setScreengrabCallback(this);
		
		///////////////////////
		//Create GLSurface
		///////////////////////
		// OpenGL view where all of the graphics are drawn
		mGLView = new GLSurfaceView(this);
		mGLView.setEGLContextClientVersion(2);
		mGLView.setEGLConfigChooser(8,8,8,8,16,0);
		SurfaceHolder glSurfaceHolder = mGLView.getHolder();
		glSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
		mOpenGL2Renderer = new OpenGL2PointCloudRenderer(this, maxDepthPoints, FOV, MAX_BILLBOARDS);

		mDemoRenderer = mOpenGL2Renderer;
		mGLView.setRenderer(mOpenGL2Renderer);
		mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		setContentView(mGLView);
		addContentView(tangoCameraPreview, new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT ) );
		setupUI();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
			// Make sure the request was successful
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Motion Tracking Permissions Required!",
						Toast.LENGTH_SHORT).show();
				finish();
			} else {
				startCameraPreview();
			}
		}
	}

	// Camera Preview
	private void startCameraPreview() {
		tangoCameraPreview.connectToTangoCamera(mTango,TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

		// Must run UI changes on the UI thread. Running in the Tango
		// service thread will result in an error.
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				int w = ReadableTangoCameraPreview.SCREEN_GRAB_W;
				int h = ReadableTangoCameraPreview.SCREEN_GRAB_H;
				//Create the exploited object here!		
				//Create an image buffer to grab images
				//int w = tangoCameraPreview.getWidth();
				//int h = tangoCameraPreview.getHeight();		
				Log.i(TAG,"TangoPreview w and h :" +w + "  " + h);
				bitmapBuffer = new int[w * h];		
				bitmapSource = new int[w * h];
				intBuffer = IntBuffer.wrap(bitmapBuffer);
				tangoCameraPreview.setIntBuffer(intBuffer);
			}
		});

		TangoConfig config = mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
		config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
		config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);

		maxDepthPoints = config.getInt(TangoConfig.KEY_INT_MAXPOINTCLOUDELEMENTS);

		Log.i(TAG, "maxpoints =" + maxDepthPoints);
		mPointCloud = new PointCloud(maxDepthPoints);	

		mTango.connect(config);
		final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
		framePairs.add(new TangoCoordinateFramePair(
				TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
				TangoPoseData.COORDINATE_FRAME_DEVICE));

		mTango.connectListener(framePairs, new OnTangoUpdateListener() {
			@Override
			public void onPoseAvailable(final TangoPoseData pose) {
				// Log whenever Motion Tracking enters a n invalid state
				if (pose.statusCode == TangoPoseData.POSE_INVALID) {
					Log.w(TAG, "Invalid State");
				}
				if (mPreviousPoseStatus != pose.statusCode) {
					count = 0;
				}
				count++;
				mPreviousPoseStatus = pose.statusCode;				
				mPreviousTimeStamp = (float) pose.timestamp;
				// Update the OpenGL renderable objects with the new Tango Pose
				// data
				mOpenGL2Renderer.getModelMatCalculator().updateModelMatrix(
						pose.getTranslationAsFloats(),
						pose.getRotationAsFloats());

				mGLView.requestRender();

				// Update the UI with TangoPose information
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						DecimalFormat threeDec = new DecimalFormat("0.000");
						String translationString = "[" + threeDec.format(pose.translation[0])
								+ ", " + threeDec.format(pose.translation[1]) + ", "
								+ threeDec.format(pose.translation[2]) + "] ";
						String quaternionString = "[" + threeDec.format(pose.rotation[0]) + ", "
								+ threeDec.format(pose.rotation[1]) + ", "
								+ threeDec.format(pose.rotation[2]) + ", "
								+ threeDec.format(pose.rotation[3]) + "] ";

						float x = (float) pose.translation[0];
						float y = (float) pose.translation[1];
						float z = (float) pose.translation[2];

						mDemoRenderer.setCameraPosition(x, y,z );

						lastPosition.setTo(x, y, z);

						float qx = (float) pose.rotation[0];
						float qy = (float) pose.rotation[1];
						float qz = (float) pose.rotation[2];
						float qw = (float) pose.rotation[3];

						mDemoRenderer.setCameraAngles(qx, qy, qz, qw);

						// Display pose data on screen in TextViews
						//Log.i(TAG,translationString);
						mPoseTextView.setText(translationString);
						mQuatTextView.setText(quaternionString);

					}
				});
			}

			@Override
			public void onXyzIjAvailable(final TangoXyzIjData xyzIj) {
				//Log.i(TAG,"xyzijAvailable!!!!!!!!");
				mCurrentTimeStamp = (float) xyzIj.timestamp;
				final float frameDelta = (mCurrentTimeStamp - mXyIjPreviousTimeStamp)
						* SECS_TO_MILLISECS;
				mXyIjPreviousTimeStamp = mCurrentTimeStamp;
				byte[] buffer = new byte[xyzIj.xyzCount * 3 * 4];
				//////mGLView.requestRender();
				FileInputStream fileStream = new FileInputStream(
						xyzIj.xyzParcelFileDescriptor.getFileDescriptor());
				try {
					fileStream.read(buffer,
							xyzIj.xyzParcelFileDescriptorOffset, buffer.length);
					fileStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					TangoPoseData pointCloudPose = mTango.getPoseAtTime(mCurrentTimeStamp, framePairs.get(0));

					mPointCloud.UpdatePoints(buffer,xyzIj.xyzCount);
					mOpenGL2Renderer.getModelMatCalculator()
					.updatePointCloudModelMatrix(
							pointCloudPose.getTranslationAsFloats(),
							pointCloudPose.getRotationAsFloats());
					mPointCloud.setModelMatrix(mOpenGL2Renderer.getModelMatCalculator()
							.getPointCloudModelMatrixCopy());
				} catch (TangoErrorException e) {
					Toast.makeText(getApplicationContext(),
							R.string.TangoError, Toast.LENGTH_SHORT).show();
				} catch (TangoInvalidException e) {
					Toast.makeText(getApplicationContext(),
							R.string.TangoError, Toast.LENGTH_SHORT).show();
				}

				// Must run UI changes on the UI thread. Running in the Tango
				// service thread
				// will result in an error.
				runOnUiThread(new Runnable() {
					DecimalFormat threeDec = new DecimalFormat("0.000");

					@Override
					public void run() {
						// Display number of points in the point cloud
						mPointCountTextView.setText(Integer.toString(xyzIj.xyzCount));
						mFrequencyTextView.setText(""+ threeDec.format(frameDelta));
						mAverageZTextView.setText(""+ threeDec.format(mPointCloud.getAverageZ()));
					}
				});
			}

			@Override
			public void onTangoEvent(final TangoEvent event) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mTangoEventTextView.setText(event.eventKey + ": " + event.eventValue);
					}
				});
			}

			@Override
			public void onFrameAvailable(int arg0) {				
				tangoCameraPreview.onFrameAvailable();
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		mTango.disconnect();
	}

	private void dropBox() {
		float averageDepth = mPointCloud.getAverageZ();		
		mDemoRenderer.createBillboard(averageDepth);
		tangoCameraPreview.takeSnapShot();
		markerCount++;
		if(markerCount >= MAX_BILLBOARDS) {
			mDropBoxButton.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.resetmotion:
			//motionReset();
			break;
		case R.id.dropbox:
			dropBox();
			break;
		default:
			Log.w(TAG, "Unknown button click");
			return;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		return false; 
	}

	public void newPhotoBuffer(final int w, final int h) {
		SaveImageTask saveImageTask = new SaveImageTask();
		saveImageTask.setHeight(h);
		saveImageTask.setWidth(w);
		saveImageTask.execute(intBuffer);
	}

	/***
	 * An inner class to run an asynchronous task for the image creation
	 * 
	 * @author henderso
	 *
	 */
	private class SaveImageTask extends AsyncTask<IntBuffer, Void, Event> {

		int width;
		int height;

		public void setHeight(int h) { height =h; }
		public void setWidth(int w) { width =w; }

				/** The system calls this to perform work in a worker thread and
		 * delivers it the parameters given to AsyncTask.execute() */
		protected Event doInBackground(IntBuffer... aIntBuffer) {

			int h = height;
			int w = width;
			Event eo = null;

			String result = "foo";
			//Create the exploited object here!		

			long fileprefix = System.currentTimeMillis();
			String targetPath =Environment.getExternalStorageDirectory()  + "/" + StartActivity.SCREENCAPTURE_DIRECTORY + "/";
			//String imageFileName = fileprefix + ".png";
			String imageFileName = "r" + markerCount + ".png";
			String fullPath = "error";
			fullPath =targetPath + imageFileName;
			Log.i(TAG, "Grabbed an image in target path:" + fullPath);	


			int offset1, offset2;
			for (int i = 0; i <  h; i++) {
				offset1 = i * w;
				offset2 = (h - i - 1) * w;
				for (int j = 0; j < w; j++) {
					int texturePixel = bitmapBuffer[offset1 + j];
					int blue = (texturePixel >> 16) & 0xff;
					int red = (texturePixel << 16) & 0x00ff0000;
					int pixel = (texturePixel & 0xff00ff00) | red | blue;
					bitmapSource[offset2 + j] = pixel;
				}
			}

			Bitmap image = Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
			//Bitmap image = ThumbnailUtils.extractThumbnail(bitmap, 100, 100);

			fullPath =targetPath + imageFileName;
			//mTangoCameraScreengrabCallback.newPhoto(fullPath);

			if(!(new File(targetPath)).exists()) {
				new File(targetPath).mkdirs();
			}
			try {           
				File targetDirectory = new File(targetPath);
				File photo=new File(targetDirectory, imageFileName);
				FileOutputStream fos=new FileOutputStream(photo.getPath());
				image.compress(CompressFormat.PNG, 10, fos);          
				fos.flush();
				fos.close();

				Log.i(TAG, "Grabbed an image in target path:" + fullPath);		


			} catch (FileNotFoundException e) {
				Log.e(TAG,"Exception " + e);
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG,"Exception " + e);
				e.printStackTrace();

			}

			return eo;


		}

		/** The system calls this to perform work in the UI thread and delivers
		 * the result from doInBackground() */
		protected void onPostExecute(Event eo) {
			//Unused
		}
	}



}
