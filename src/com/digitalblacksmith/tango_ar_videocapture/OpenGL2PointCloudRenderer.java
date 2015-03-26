package com.digitalblacksmith.tango_ar_videocapture;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.projecttango.tangoutils.Renderer;
import com.projecttango.tangoutils.renderables.PointCloud;


import com.digitalblacksmith.tango_ar_videocapture.Billboard;
import com.digitalblacksmith.tango_ar_videocapture.Matrix4f;
import com.digitalblacksmith.tango_ar_videocapture.Quaternion;
import com.digitalblacksmith.tango_ar_videocapture.RawResourceReader;
import com.digitalblacksmith.tango_ar_videocapture.TextureHelper;
import com.digitalblacksmith.tango_ar_videocapture.Vector3f;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class is a custom OPENGL 2 ES renderer that displays billboards sticknotes in an augmented reality environment.
 * 
 * The renderer was adapted from this example
 * https://code.google.com/p/android-tes/source/browse/trunk/opengl+example/android/AndroidOpenGLESLessons/src/com/learnopengles/android/lesson2/LessonTwoRenderer.java?r=226
 * 
 * 
 * @author henderso
 */
public class OpenGL2PointCloudRenderer extends Renderer implements GLSurfaceView.Renderer {
	
	/**
	 * The number of possible billboards.  Ensure you have a texture in res/drawable to match
	 */
	private int maxBillboards = 5;
	
	/** Used for debug logs. */
	private static final String TAG = "OpenGL2PointCloudRenderer";

	/**
	 * A pointer back to the encapsulating activity
	 */
	private final Context mActivityContext;

	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
	 * it positions things relative to our eye.
	 */
	private float[] mViewMatrix = new float[16];

	/** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
	private float[] mProjectionMatrix = new float[16];

	/** Allocate storage for the final combined matrix. This will be passed into the shader program. */
	private float[] mMVPMatrix = new float[16];

	/** 
	 * Stores a copy of the model matrix specifically for the light position.
	 */
	private float[] mLightModelMatrix = new float[16];      

	/** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
	 *  we multiply this by our transformation matrices. */
	private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

	/** Used to hold the current position of the light in world space (after transformation via model matrix). */
	private final float[] mLightPosInWorldSpace = new float[4];

	/** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
	private final float[] mLightPosInEyeSpace = new float[4];

	
	private Bitmap[] billBoardBitmaps;
	
	/** This is a handle to our per-vertex cube shading program. */
	private int mProgramHandle;

	/** This is a handle to our light point program. */
	private int mPointProgramHandle;        

	/** This is a handle to our texture data. */
	private int[] mTextureDataHandles;


	
	/**
	 * The maximum number of Points tracked by the Tango
	 */
	private int mMaxDepthPoints;

	/**
	 * Billboard scale factor
	 */
	float d = 0.05f;
	
	/**
	 * Camera (Tango) world coordinate X
	 */
	float cameraX;
	
	/**
	 * Camera (Tango) world coordinate Y
	 */
	float cameraY;
	
	/**
	 * Camera (Tango) world coordinate Z
	 */
	float cameraZ;
		
	/**
	 * Camera (Tango) rotation Quaternion X
	 */
	float qx;
	
	/**
	 * Camera (Tango) rotation Quaternion Y
	 */
	float qy;
	
	/**
	 * Camera (Tango) rotation Quaternion Z
	 */
	float qz;
	
	/**
	 * Camera (Tango) rotation Quaternion W
	 */
	float qw;
	
	/**
	 * Camera (Tango) vertical field of View
	 */
	float verticalFOV;

	/**
	 * The width of the viewport showing the Tango image
	 */
	int width;
	
	/**
	 * The height of the viewport showing the Tango image
	 */
	int height;
	
	/**
	 * The renderer near plane
	 */
	final float near = 0.01f;
	
	/**
	 * The renderer far plane
	 */
	final float far = 100.0f;

	/**
	 * An array list of Billboards
	 */
	ArrayList<Billboard> billboards;
	
	////////////////////////////////////////////////
	// GETTERS/SETTERS
	////////////////////////////////////////////////
	/**
	 * Set the camera (Tango) position
	 */
	public void setCameraPosition(float x, float y, float z) {
		this.cameraX=x;
		this.cameraY=y;
		this.cameraZ=z;
	}

	/**
	 * Set the camera (Tango) rotation Quaternion components
	 */
	public void setCameraAngles(float x, float y, float z, float w) {
		this.qx=x;
		this.qy=y;
		this.qz=z;		this.qw=w;
	}

	/**
	 * Return the vertex shader for the render
	 */
	protected String getVertexShader()
	{
		return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader);
	}

	/**
	 * Return the fragment shader for the renderer
	 */
	protected String getFragmentShader()
	{
		return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader);
	}

	
	////////////////////////////////////////////////
	// METHODS
	////////////////////////////////////////////////
	/**
	 * Create a billboard at the average point cloud depth in front of the Tango
	 * along the Tango's line of sight
	 */
	public void createBillboard(float averageDepth) {
				
		Billboard newBB= new Billboard(mTextureDataHandles[billboards.size()], d);
		
		//Project the marker out in front of camera		
		Quaternion q= new Quaternion(qx,qy,qz,qw);	
		
		Vector3f rv = q.getEulerAngles();
		Log.i(TAG, "rv: " + rv.x + " " + rv.y + " " + rv.z);
	
		float[] m = new float[16];
		float[] r = new float[16];
		float[] w = new float[16];		
		float[] b = new float[16];
		
		//BILLBOARD IN TANGO SPACE
		Matrix.setIdentityM(m, 0);
		Matrix.translateM(m, 0, 0.0f, 0.1f,0.0f);		

		//TANGO ROTATION
		Matrix.setIdentityM(r, 0);	
		Matrix.setRotateEulerM(r, 0, -1*(rv.x-90), rv.y, -1*rv.z);
		
		//WORLD MATRIX
		Matrix.setIdentityM(w, 0);
		Matrix.translateM(w, 0, cameraX,cameraY,cameraZ);		
	
		float[] o = new float[4];
		float[] p = new float[4];
		o[0]=0;
		o[1]=averageDepth;
		o[2]=0;
		o[3]=0.0f;
		
		Matrix.multiplyMV(p, 0, r, 0, o, 0);	
		
		//BILLBOARD IN TANGO SPACE
		Matrix.setIdentityM(m, 0);
		Matrix.translateM(m, 0, p[0], p[1],p[2]);		
		
		Matrix.multiplyMM(b, 0, m, 0, w, 0);
		
		Matrix4f result= new Matrix4f(b);				
		//Apply here
		Vector3f bbWorldT = result.getTranslation();
		newBB.setPosition(bbWorldT.x,  bbWorldT.y,  bbWorldT.z);
		billboards.add(newBB);
	}

	/*
	 * A helper method to get a resource ID given a string variable.
	 * Good for progamatically accessing android resources
	 */
	public int getAndroidDrawableId(String pDrawableName){
		Log.i(TAG,pDrawableName);
		Log.i(TAG,mActivityContext.getPackageName());
	    return mActivityContext.getResources().getIdentifier(pDrawableName, "drawable", mActivityContext.getPackageName());
	    
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition.khronos.opengles.GL10, javax.microedition.khronos.egl.EGLConfig)
	 */
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) 
	{
		// Set the background clear color to black.
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// Use culling to remove back faces.
		GLES20.glEnable(GLES20.GL_CULL_FACE);

		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// Position the eye in front of the origin.
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		final float eyeZ = -0.5f;

		// We are looking toward the distance
		final float lookX = 0.0f;
		final float lookY = 0.0f;
		final float lookZ = -5.0f;

		// Set our up vector. This is where our head would be pointing were we holding the camera.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);                

		final String vertexShader = getVertexShader();                  
		final String fragmentShader = getFragmentShader();                      

		final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);            
		final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);              

		mProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
				new String[] {"a_Position",  "a_Color", "a_Normal", "a_TexCoordinate"});                                                                                                                                                          

		// Load the billboard textures
		mTextureDataHandles = new int[maxBillboards];

		for(int i=0; i < maxBillboards; i++) {
			int h = getAndroidDrawableId("r" + i);
			mTextureDataHandles[i] = TextureHelper.loadTexture(mActivityContext, h);
			Log.i(TAG,"loaded mTextureDataHandle" +i);				
		}

		
	}       


	/*
	 * (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.microedition.khronos.opengles.GL10, int, int)
	 */
	@Override
	public void onSurfaceChanged(GL10 glUnused, int aWidth, int aHeight) 
	{


		this.width = aWidth;
		this.height = aHeight;

		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, width, height);

		// Create a new perspective projection matrix. The height will stay the same
		// while the width will vary as per aspect ratio.
		final float ratio = (float) width / height;
		final float near = 0.01f;
		final float far = 100.0f;

		float fov = 57.295f*verticalFOV; // degrees, try also 45, or different number if you like
		float top = (float) Math.tan(fov * 1.0*Math.PI / 360.0f) * near;
		float bottom = -top;
		float left = ratio * bottom;
		float right = ratio * top;

		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
	}       

	@Override
	public void onDrawFrame(GL10 glUnused) 
	{

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);         

		// Position the eye in front of the origin.
		float eyeX = 0.0f;
		float eyeY = -0.0f;
		float eyeZ = 0.0f;

		// We are looking toward the distance
		float lookX = 0.0f;
		float lookY = 0.0f;
		float lookZ = -1.0f;

		// Set our up vector. This is where our head would be pointing were we holding the camera.
		float upX = 0.0f;
		float upY = 1.0f;
		float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);                

		// Set our per-vertex lighting program.
		GLES20.glUseProgram(mProgramHandle);

		//CAMERA
		Quaternion q= new Quaternion(qx,qy,qz,qw);
		Vector3f rv = q.getEulerAngles();

		Matrix.rotateM(mViewMatrix, 0, rv.x, -1.0f, 0.0f, 0.0f); 
		Matrix.rotateM(mViewMatrix, 0, rv.y, 0.0f, -1.0f, 0.0f); 
		Matrix.rotateM(mViewMatrix, 0, rv.z, 0.0f, 0.0f, -1.0f); 
		Matrix.translateM(mViewMatrix, 0, -cameraX, -cameraY, -cameraZ);

		try {
			Iterator<Billboard> it = billboards.iterator();
			while(it.hasNext()) {
				Billboard bb = it.next();
				bb.setCameraPosition(cameraX,cameraY,cameraZ);
				bb.setShader(mProgramHandle);
				bb.drawBillboard( mMVPMatrix, mProjectionMatrix, mViewMatrix);
			}
		} catch(Exception e) {
			Log.e(TAG,""+e);
		}

		//TODO:  Fix rendering of point cloud
		/*
		// Create a new perspective projection matrix. The height will stay the same
		// while the width will vary as per aspect ratio.
		Matrix.setIdentityM(mViewMatrix, 0);
		//Good one
		Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 0.5f, 0f, 0f, 0f, 0f, 1f, 0f);
		mPointCloud.draw(mViewMatrix, mProjectionMatrix);
		// Draw a point to indicate the light.
		//GLES20.glUseProgram(mPointProgramHandle);        
		//drawLight();
		 * 
		 */
	}                               


	/**
	 * Draws a point representing the position of the light.
	 */
	private void drawLight()
	{
		final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
		final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

		// Pass in the position.
		GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

		// Since we are not using a buffer object, disable vertex arrays for this attribute.
		GLES20.glDisableVertexAttribArray(pointPositionHandle);  

		// Pass in the transformation matrix.
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Draw the point.
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
	}

	/** 
	 * Helper function to compile a shader.
	 * 
	 * @param shaderType The shader type.
	 * @param shaderSource The shader source code.
	 * @return An OpenGL handle to the shader.
	 */
	private int compileShader(final int shaderType, final String shaderSource) 
	{
		int shaderHandle = GLES20.glCreateShader(shaderType);

		if (shaderHandle != 0) 
		{
			// Pass in the shader source.
			GLES20.glShaderSource(shaderHandle, shaderSource);

			// Compile the shader.
			GLES20.glCompileShader(shaderHandle);

			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) 
			{
				Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
			}
		}

		if (shaderHandle == 0)
		{                       
			throw new RuntimeException("Error creating shader.");
		}

		return shaderHandle;
	}       

	/**
	 * Helper function to compile and link a program.
	 * 
	 * @param vertexShaderHandle An OpenGL handle to an already-compiled vertex shader.
	 * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
	 * @param attributes Attributes that need to be bound to the program.
	 * @return An OpenGL handle to the program.
	 */
	private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) 
	{
		int programHandle = GLES20.glCreateProgram();

		if (programHandle != 0) 
		{
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle);                       

			// Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);

			// Bind attributes
			if (attributes != null)
			{
				final int size = attributes.length;
				for (int i = 0; i < size; i++)
				{
					GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
				}                                               
			}

			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == 0) 
			{                               
				Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}

		if (programHandle == 0)
		{
			throw new RuntimeException("Error creating program.");
		}

		return programHandle;
	}
	
	////////////////////////////////////////////////
	// CONSTRUCTORS
	////////////////////////////////////////////////	
	/**
	 * Initialize the model data.
	 */
	public OpenGL2PointCloudRenderer(final Context activityContext, int maxDepthPoints, double aVerticalFOV, int theMaxBillBoards) {
		maxBillboards = theMaxBillBoards;
		mMaxDepthPoints = maxDepthPoints;	      
		billboards = new ArrayList<Billboard>();     
		mActivityContext = activityContext;
		verticalFOV = new Float(aVerticalFOV).floatValue();
		 

	}
	
	private void loadBitmaps() {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;	// No pre-scaling
		Resources res = mActivityContext.getResources();
		for(int i=0; i < maxBillboards; i++) {
			int h = getAndroidDrawableId("r" + i);
			//mTextureDataHandles[i] = TextureHelper.loadTexture(mActivityContext, h);
			billBoardBitmaps[i] = BitmapFactory.decodeResource(res, h, options);
			
			Log.i(TAG,"loaded resource:" +h);				
		}
	}
	
	private class LoadBitmapsTask extends AsyncTask {

		
		@Override
		protected Object doInBackground(Object... params) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;	// No pre-scaling
			Resources res = mActivityContext.getResources();
			for(int i=0; i < maxBillboards; i++) {
				int h = getAndroidDrawableId("r" + i);
				//mTextureDataHandles[i] = TextureHelper.loadTexture(mActivityContext, h);
				billBoardBitmaps[i] = BitmapFactory.decodeResource(res, h, options);
				
				Log.i(TAG,"loaded resource:" +h);				
			}
			return null;

		}




	}
	
}