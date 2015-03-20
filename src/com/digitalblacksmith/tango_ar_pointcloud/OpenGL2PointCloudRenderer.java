package com.digitalblacksmith.tango_ar_pointcloud;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.projecttango.tangoutils.Renderer;
import com.projecttango.tangoutils.renderables.PointCloud;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 * 
 * from:
 * https://code.google.com/p/android-tes/source/browse/trunk/opengl+example/android/AndroidOpenGLESLessons/src/com/learnopengles/android/lesson2/LessonTwoRenderer.java?r=226
 */
public class OpenGL2PointCloudRenderer extends Renderer implements GLSurfaceView.Renderer, DemoRenderer {
	
	
	public final int MAX_BILLBOARDS = 21;
	
	
	/** Used for debug logs. */
	private static final String TAG = "OpenGL2PointCloudRenderer";

	private final Context mActivityContext;

	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
	 * it positions things relative to our eye.
	 */
	private float[] mViewMatrix = new float[16];

	/** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
	private float[] mProjectionMatrix = new float[16];


	private float[] mProjectionMatrixFishEye = new float[16];

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

	/** This is a handle to our per-vertex cube shading program. */
	private int mProgramHandle;

	/** This is a handle to our light point program. */
	private int mPointProgramHandle;        

	
	
	

	/** This is a handle to our texture data. */
	private int[] mTextureDataHandles;


	private PointCloud mPointCloud;
	private int mMaxDepthPoints;

	/**
	 * Cube scale
	 */
	float d = 0.05f;

	float x,y,z;
	float qx,qy,qz,qw;

	float verticalFOV;

	int width;
	int height;
	final float near = 0.01f;
	final float far = 100.0f;


	ArrayList<Billboard> markers;


	/**
	 * Set the camera position
	 */
	public void dropMarker() {
				
		Billboard newBB= new Billboard(mTextureDataHandles[markers.size()], d);
		float averageDepth = mPointCloud.getAverageZ();

		//Project the marker out in front of camera		
		Quaternion q= new Quaternion(qx,qy,qz,qw);	
		
		Vector3f rv = q.getEulerAngles();


		Log.i(TAG, "rv: " + rv.x + " " + rv.y + " " + rv.z);

		//Drop at Tango position
		/*		
		Matrix4f deviceT = new Matrix4f();
		deviceT.translate(this.x, this.y, this.z);
		Matrix4f world = rot.multiplyMatrix(deviceT);
		Matrix4f result = world;
		 */


		//Rotates around the drop point to the left
		/*
		Matrix4f bbLocal = new Matrix4f();		
		bbLocal.translate(0, averageDepth, 0);

		Matrix4f rot = new Matrix4f();
		rot.rotate(rv.x, -1, 0, 0);
		rot.rotate(rv.y, 0, -1, 0);
		rot.rotate(rv.z, 0, 0, -1);

		bbLocal.translate(this.x, this.y, this.z);
		bbLocal.multiplyMatrix(rot);

		Matrix4f world = bbLocal;				
		Matrix4f result = world.multiplyMatrix(bbLocal);
		 */

		//Rotates around the drop point to the front
		/*
		Matrix4f bbLocal = new Matrix4f();		
		bbLocal.translate(0, averageDepth, 0);		
		bbLocal.rotate(rv.x, -1, 0, 0);
		bbLocal.rotate(rv.y, 0, -1, 0);
		bbLocal.rotate(rv.z, 0, 0, -1);
		Matrix4f world = new Matrix4f();
		world.translate(this.x, this.y, this.z);				
		Matrix4f result = world.multiplyMatrix(bbLocal);
		 */

		float[] m = new float[16];
		float[] r = new float[16];
		float[] w = new float[16];
		float[] a = new float[16];
		float[] b = new float[16];
		float[] c = new float[16];
		//BILLBOARD IN TANGO SPACE
		Matrix.setIdentityM(m, 0);
		Matrix.translateM(m, 0, 0.0f, 0.1f,0.0f);		

		//TANGO ROTATION
		Matrix.setIdentityM(r, 0);	
		Matrix.setRotateEulerM(r, 0, -1*(rv.x-90), rv.y, -1*rv.z);
		
		//WORLD MATRIX
		Matrix.setIdentityM(w, 0);
		Matrix.translateM(w, 0, x,y,z);		

		//NO
		//Matrix.multiplyMM(a, 0, m, 0, r, 0);		
		//Matrix.multiplyMM(b, 0, w, 0, a, 0);

		//NO
		//Matrix.multiplyMM(a, 0, m, 0, r, 0);		
		//Matrix.multiplyMM(b, 0, a, 0, w, 0);

		//NO -- Breathing
		//Matrix.multiplyMM(a, 0, r, 0, m, 0);		
		//Matrix.multiplyMM(b, 0, a, 0, w, 0);

	
		//VERY CLOSE
		//Matrix.multiplyMM(a, 0, r, 0, w, 0);		
		//Matrix.multiplyMM(b, 0, m, 0, w, 0);

		float[] o = new float[4];
		float[] p = new float[4];
		o[0]=0;
		o[1]=averageDepth;
		o[2]=0;
		o[3]=0.0f;
		
		Matrix.multiplyMV(p, 0, r, 0, o, 0);	
		
		Log.i(TAG, "p: " + p[0] + " " + p[1] + " " + p[2]);
		
		//BILLBOARD IN TANGO SPACE
		Matrix.setIdentityM(m, 0);
		Matrix.translateM(m, 0, p[0], p[1],p[2]);		
		
		Matrix.multiplyMM(b, 0, m, 0, w, 0);
		
		Matrix4f result= new Matrix4f(b);				
		//Apply here
		Vector3f bbWorldT = result.getTranslation();
		newBB.setPosition(bbWorldT.x,  bbWorldT.y,  bbWorldT.z);
		markers.add(newBB);
	}

	/**
	 * Set the camera position
	 */
	public void setCameraPosition(float x, float y, float z) {
		this.x=x;
		this.y=y;
		this.z=z;
	}

	/**
	 * Set the camera Euler rotation angles
	 */
	public void setCameraAngles(float x, float y, float z, float w) {
		this.qx=x;
		this.qy=y;
		this.qz=z;
		this.qw=w;
	}

	/**
	 * Initialize the model data.
	 */
	public OpenGL2PointCloudRenderer(final Context activityContext, int maxDepthPoints, double aVerticalFOV) {
		mMaxDepthPoints = maxDepthPoints;	      
		markers = new ArrayList<Billboard>();     
		mActivityContext = activityContext;
		verticalFOV = new Float(aVerticalFOV).floatValue();

	}

	protected String getVertexShaderOLD()
	{
		// TODO: Explain why we normalize the vectors, explain some of the vector math behind it all. Explain what is eye space.
		final String vertexShader =
				"uniform mat4 u_MVPMatrix;      \n"             // A constant representing the combined model/view/projection matrix.
				+ "uniform mat4 u_MVMatrix;       \n"         // A constant representing the combined model/view matrix.      
				+ "uniform vec3 u_LightPos;       \n"     // The position of the light in eye space.

				+ "attribute vec4 a_Position;     \n"         // Per-vertex position information we will pass in.
				+ "attribute vec4 a_Color;        \n"         // Per-vertex color information we will pass in.
				+ "attribute vec3 a_Normal;       \n"         // Per-vertex normal information we will pass in.

				+ "varying vec4 v_Color;          \n"         // This will be passed into the fragment shader.

				+ "void main()                    \n"         // The entry point for our vertex shader.
				+ "{                              \n"         
				// Transform the vertex into eye space.
				+ "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);              \n"
				// Transform the normal's orientation into eye space.
				+ "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));     \n"
				// Will be used for attenuation.
				+ "   float distance = length(u_LightPos - modelViewVertex);             \n"
				// Get a lighting direction vector from the light to the vertex.
				+ "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);        \n"
				// Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
				// pointing in the same direction then it will get max illumination.
				+ "   float diffuse = max(dot(modelViewNormal, lightVector), 5.1);       \n"                                                                                                                            
				// Attenuate the light based on distance.
				+ "   diffuse = diffuse * (1.0 / (1.0 + (0.0025 * distance * distance)));  \n"
				// Multiply the color by the illumination level. It will be interpolated across the triangle.
				+ "   v_Color = a_Color * diffuse;                                       \n"   
				// gl_Position is a special variable used to store the final position.
				// Multiply the vertex by the matrix to get the final point in normalized screen coordinates.           
				+ "   gl_Position = u_MVPMatrix * a_Position;                            \n"     
				+ "}                                                                     \n"; 

		return vertexShader;
	}

	protected String getFragmentShaderOLD()
	{
		final String fragmentShader =
				"precision mediump float;       \n"             // Set the default precision to medium. We don't need as high of a 
				// precision in the fragment shader.                            
				+ "varying vec4 v_Color;          \n"         // This is the color from the vertex shader interpolated across the 
				// triangle per fragment.                         
				+ "void main()                    \n"         // The entry point for our fragment shader.
				+ "{                              \n"
				+ "   gl_FragColor = v_Color;     \n"         // Pass the color directly through the pipeline.                  
				+ "}                              \n";

		return fragmentShader;
	}

	protected String getVertexShader()
	{
		return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader);
	}

	protected String getFragmentShader()
	{
		return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader);
	}

	
	/*
	 * Get a resource ID given a string
	 */
	public int getAndroidDrawableId(String pDrawableName){
		Log.i(TAG,pDrawableName);
		Log.i(TAG,mActivityContext.getPackageName());
	    return mActivityContext.getResources().getIdentifier(pDrawableName, "drawable", mActivityContext.getPackageName());
	    
	}
	
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) 
	{
		// Set the background clear color to black.
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// Use culling to remove back faces.
		GLES20.glEnable(GLES20.GL_CULL_FACE);

		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		mPointCloud = new PointCloud(mMaxDepthPoints);

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


		// Load the textures
		
		
		
		mTextureDataHandles = new int[MAX_BILLBOARDS];
		
		for(int i=0; i < MAX_BILLBOARDS; i++) {
			int h = getAndroidDrawableId("r" + i);
			mTextureDataHandles[i] = TextureHelper.loadTexture(mActivityContext, h);
			Log.i(TAG,"loaded mTextureDataHandle" +i);				
		}

	}       


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

		// Do a complete rotation every 10 seconds.
		long time = SystemClock.uptimeMillis() % 10000L;        
		float angleInDegrees = (360.0f / 10000.0f) * ((int) time);                

		// Set our per-vertex lighting program.
		GLES20.glUseProgram(mProgramHandle);

		// Calculate position of the light. Rotate and then push into the distance.
		Matrix.setIdentityM(mLightModelMatrix, 0);
		Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);      
		Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
		Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

		Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
		Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);                        

		//CAMERA
		Quaternion q= new Quaternion(qx,qy,qz,qw);
		Vector3f rv = q.getEulerAngles();

		Matrix.rotateM(mViewMatrix, 0, rv.x, -1.0f, 0.0f, 0.0f); 
		Matrix.rotateM(mViewMatrix, 0, rv.y, 0.0f, -1.0f, 0.0f); 
		Matrix.rotateM(mViewMatrix, 0, rv.z, 0.0f, 0.0f, -1.0f); 
		Matrix.translateM(mViewMatrix, 0, -x, -y, -z);

		try {
			Iterator<Billboard> it = markers.iterator();
			while(it.hasNext()) {
				Billboard bb = it.next();
				bb.setCameraPosition(x,y,z);
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

	public PointCloud getPointCloud() {
		return mPointCloud;
	}

}