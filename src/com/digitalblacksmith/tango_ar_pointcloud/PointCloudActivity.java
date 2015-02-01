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

package com.digitalblacksmith.tango_ar_pointcloud;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
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

import java.io.FileInputStream;
import java.io.IOException;
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
public class PointCloudActivity extends Activity implements View.OnClickListener, SurfaceHolder.Callback  {

	private static final String TAG = PointCloudActivity.class.getSimpleName();
	private static final int SECS_TO_MILLISECS = 1000;


	private Tango mTango;
	private TangoConfig mConfig;
	private TextView mDeltaTextView;
	private TextView mPoseCountTextView;
	private TextView mPoseTextView;
	private TextView mQuatTextView;
	private TextView mPoseStatusTextView;
	private TextView mTangoServiceVersionTextView;
	private TextView mApplicationVersionTextView;
	private TextView mTangoEventTextView;
	private TextView mPointCountTextView;
	private TextView mAverageZTextView;
	private TextView mFrequencyTextView;
	private float mPreviousTimeStamp;
	private int mPreviousPoseStatus;
	private int count;
	private float mDeltaTime;
	private Button mMotionResetButton;
	private Button mDropBoxButton;
	//private boolean mIsAutoRecovery;

	private PCRenderer mOpenGL2Renderer;
	private DemoRenderer mDemoRenderer;
	private GLSurfaceView mGLView;
	private SurfaceHolder surfaceHolder;
	private SurfaceView surfaceView;

	private float mXyIjPreviousTimeStamp;
	private float mCurrentTimeStamp;

	boolean first_initialized = false;

	double mOpenGLVersion = 1.0;

	Vector3f lastPosition;
	Vector3f dropBoxPosition;


	/**
	 * Set up the activity using OpenGL 20
	 */
	@SuppressWarnings("deprecation")
	private void setUpOpenGL20() {

		///////////////////////
		//Create GLSurface
		///////////////////////
		// OpenGL view where all of the graphics are drawn
		mGLView = new GLSurfaceView(this);
		mGLView.setEGLContextClientVersion(2);
		//mGLView.setZOrderOnTop(true);
		mGLView.setEGLConfigChooser(8,8,8,8,16,0);
		surfaceHolder = mGLView.getHolder();
		surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);


		////////////////////////////////////
		// Instantiate the Tango service
		///////////////////////////////////
		mTango = new Tango(this);
		// Create a new Tango Configuration and enable the MotionTrackingActivity API
		mConfig = new TangoConfig();
		mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
		//mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
		mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);


		// Configure OpenGL renderer
		//mRenderer = new GLClearRenderer();
		int maxDepthPoints = mConfig.getInt("max_point_cloud_elements");

		mOpenGL2Renderer = new PCRenderer(maxDepthPoints);

		mDemoRenderer = mOpenGL2Renderer;
		mOpenGL2Renderer.setFirstPersonView();
		mGLView.setRenderer(mOpenGL2Renderer);
		mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		//setContentView(mGLView);


		try {
			setTangoListeners();
		} catch (TangoErrorException e) {
			Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT).show();
		} catch (SecurityException e) {
			Toast.makeText(getApplicationContext(), R.string.motiontrackingpermission,
					Toast.LENGTH_SHORT).show();
		}

		//////////////////////////
		// Create Camera Surface
		//////////////////////////
		surfaceView = new SurfaceView(this);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);


		setContentView(mGLView);
		addContentView( surfaceView, new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT ) );

		//THIS ORDER WONT WORK
		//setContentView(surfaceView);
		//addContentView( mGLView, new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT ) );

		/////////////////////////
		//Create UI Objects 
		////////////////////////
		LayoutInflater inflater = getLayoutInflater();
		View tmpView;
		tmpView = inflater.inflate(R.layout.activity_motion_tracking, null);
		getWindow().addContentView(tmpView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT)); 


		mApplicationVersionTextView = (TextView) findViewById(R.id.appversion);

		mApplicationVersionTextView.setText("OpenGL 2.0");

		// Button to reset motion tracking
		mMotionResetButton = (Button) findViewById(R.id.resetmotion);
		// Set up button click listeners
		mMotionResetButton.setOnClickListener(this);

		// Button to drop position box (breadcrumb cube)
		mDropBoxButton = (Button) findViewById(R.id.dropbox);
		// Set up button click listeners
		mDropBoxButton.setOnClickListener(this);


	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		setUpOpenGL20();

		// Text views for displaying translation and rotation data
		mPoseTextView = (TextView) findViewById(R.id.pose);
		mQuatTextView = (TextView) findViewById(R.id.quat);
		mPoseCountTextView = (TextView) findViewById(R.id.posecount);
		mDeltaTextView = (TextView) findViewById(R.id.deltatime);
		mTangoEventTextView = (TextView) findViewById(R.id.tangoevent);
		mPointCountTextView = (TextView) findViewById(R.id.pointCount);
		mAverageZTextView = (TextView) findViewById(R.id.averageZ);
		mFrequencyTextView = (TextView) findViewById(R.id.frameDelta);

		// Text views for the status of the pose data and Tango library versions
		mPoseStatusTextView = (TextView) findViewById(R.id.status);
		mTangoServiceVersionTextView = (TextView) findViewById(R.id.version);

		// Display the library version for debug purposes
		mTangoServiceVersionTextView.setText(mConfig.getString("tango_service_library_version"));

		dropBoxPosition = new Vector3f();
		lastPosition = new Vector3f();
	}

	private void motionReset() {
		mTango.resetMotionTracking();
	}

	private void dropBox() {
		dropBoxPosition.setTo(lastPosition);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "OnPause");
		try {
			mTango.disconnect();
			Log.i(TAG,"Pausing..TANGO disconnected");
		} catch (TangoErrorException e) {
			Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT).show();
		}

	}

	protected void onResume() {
		super.onResume();
		Log.i(TAG, "OnResume");

		try {
			//setTangoListeners();
		} catch (TangoErrorException e) {
			Log.e(TAG,e.toString());
		} catch (SecurityException e) {
			Log.e(TAG,e.toString());
		}
		try {           
			if(first_initialized)mTango.connect(mConfig);
		} catch (TangoOutOfDateException e) {
			Log.e(TAG,e.toString());
		} catch (TangoErrorException e) {
			Log.e(TAG,e.toString());
		}
		try {
			//setUpExtrinsics();
		} catch (TangoErrorException e) {
			Log.e(TAG,e.toString());
		} catch (SecurityException e) {
			Log.e(TAG,e.toString());
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.resetmotion:
			motionReset();
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


	/**
	 * Set up the TangoConfig and the listeners for the Tango service, then begin using the Motion
	 * Tracking API. This is called in response to the user clicking the 'Start' Button.
	 */
	private void setTangoListeners() {
		// Lock configuration and connect to Tango
		// Select coordinate frame pair
		final ArrayList<TangoCoordinateFramePair> framePairs = 
				new ArrayList<TangoCoordinateFramePair>();
		framePairs.add(new TangoCoordinateFramePair(
				TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
				TangoPoseData.COORDINATE_FRAME_DEVICE));
		// Listen for new Tango data
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
				mDeltaTime = (float) (pose.timestamp - mPreviousTimeStamp) * SECS_TO_MILLISECS;
				mPreviousTimeStamp = (float) pose.timestamp;
				// Update the OpenGL renderable objects with the new Tango Pose
				// data
				float[] translation = pose.getTranslationAsFloats();

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

						mDemoRenderer.setCameraPosition(x-dropBoxPosition.x, y-dropBoxPosition.y, z-dropBoxPosition.z);

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
						mPoseCountTextView.setText(Integer.toString(count));
						mDeltaTextView.setText(threeDec.format(mDeltaTime));
						if (pose.statusCode == TangoPoseData.POSE_VALID) {
							mPoseStatusTextView.setText(R.string.pose_valid);
						} else if (pose.statusCode == TangoPoseData.POSE_INVALID) {
							mPoseStatusTextView.setText(R.string.pose_invalid);
						} else if (pose.statusCode == TangoPoseData.POSE_INITIALIZING) {
							mPoseStatusTextView.setText(R.string.pose_initializing);
						} else if (pose.statusCode == TangoPoseData.POSE_UNKNOWN) {
							mPoseStatusTextView.setText(R.string.pose_unknown);
						}
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
					TangoPoseData pointCloudPose = mTango.getPoseAtTime(
							mCurrentTimeStamp, framePairs.get(0));

					mOpenGL2Renderer.getPointCloud().UpdatePoints(buffer,
							xyzIj.xyzCount);
					mOpenGL2Renderer.getModelMatCalculator()
					.updatePointCloudModelMatrix(
							pointCloudPose.getTranslationAsFloats(),
							pointCloudPose.getRotationAsFloats());
					mOpenGL2Renderer.getPointCloud().setModelMatrix(
							mOpenGL2Renderer.getModelMatCalculator()
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
						mPointCountTextView.setText(Integer
								.toString(xyzIj.xyzCount));
						mFrequencyTextView.setText(""
								+ threeDec.format(frameDelta));
						mAverageZTextView.setText(""
								+ threeDec.format(mOpenGL2Renderer.getPointCloud()
										.getAverageZ()));
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
		});
	}


	private void setUpExtrinsics() {
		// Get device to imu matrix.
		TangoPoseData device2IMUPose = new TangoPoseData();
		TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
		framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
		framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;
		device2IMUPose = mTango.getPoseAtTime(0.0, framePair);
		// mRenderer.getModelMatCalculator().SetDevice2IMUMatrix(
		//         device2IMUPose.getTranslationAsFloats(), device2IMUPose.getRotationAsFloats());

		// Get color camera to imu matrix.
		TangoPoseData color2IMUPose = new TangoPoseData();
		framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
		framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
		color2IMUPose = mTango.getPoseAtTime(0.0, framePair);

		// mRenderer.getModelMatCalculator().SetColorCamera2IMUMatrix(
		//        color2IMUPose.getTranslationAsFloats(), color2IMUPose.getRotationAsFloats());
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Surface surface = holder.getSurface();
		if (surface.isValid()) {
			TangoConfig config = new TangoConfig();
			config =  mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
			config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
			//mTango.connectSurface(0, surface);
			first_initialized=true;
			mTango.connect(config);

		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mTango.disconnectSurface(0);

	}

}
