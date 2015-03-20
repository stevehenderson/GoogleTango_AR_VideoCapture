package com.digitalblacksmith.tango_ar_pointcloud;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * An OpenGL 2.0 ES Cube
 * 
 * @author henderso
 *
 */
public class Cube20 {

	/** Store our model data in a float buffer. */
	private final FloatBuffer mCubePositions;
	private final FloatBuffer mCubeColors;
	private final FloatBuffer mCubeNormals;

	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;

	/** This will be used to pass in the modelview matrix. */
	private int mMVMatrixHandle;

	/** This will be used to pass in the light position. */
	private int mLightPosHandle;

	/** This will be used to pass in model position information. */
	private int mPositionHandle;

	/** This will be used to pass in model color information. */
	private int mColorHandle;

	/** This will be used to pass in model normal information. */
	private int mNormalHandle;

	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;   

	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;        

	/** Size of the color data in elements. */
	private final int mColorDataSize = 4;   

	/** Size of the normal data in elements. */
	private final int mNormalDataSize = 3;

	/** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
	 *  we multiply this by our transformation matrices. */
	private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

	/** Used to hold the current position of the light in world space (after transformation via model matrix). */
	private final float[] mLightPosInWorldSpace = new float[4];

	/** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
	private final float[] mLightPosInEyeSpace = new float[4];

	/** This is a handle to our per-vertex cube shading program. */
	private int mPerVertexProgramHandle;

	/**
	 * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
	 * of being located at the center of the universe) to world space.
	 */
	private float[] mModelMatrix = new float[16];
	
	
	float x, y, z;
	
	
	
	/**
	 * @param mPerVertexProgramHandle the mPerVertexProgramHandle to set
	 */
	public void setmPerVertexProgramHandle(int mPerVertexProgramHandle) {
		this.mPerVertexProgramHandle = mPerVertexProgramHandle;
		// Set program handles for cube drawing.
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
		mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix"); 
		mLightPosHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos");
		mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
		mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color");
		mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal"); 
	}
	
	
	/**
	 * 
	 * @param px
	 * @param py
	 * @param pz
	 */
	public void setPosition(float px, float py, float pz) {
		this.x = px;
		this.y = py;
		this.z = pz;
		
	}

	/**
	 * Draws a cube.
	 */                     
	public void drawCube(float[] mMVPMatrix, float[] mProjectionMatrix, float[] mViewMatrix)
	{               
		
		Matrix.setIdentityM(mModelMatrix, 0);		
		Matrix.translateM(mModelMatrix, 0, x, y, z);
				
		// Pass in the position information
		mCubePositions.position(0);             
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
				0, mCubePositions);        

		GLES20.glEnableVertexAttribArray(mPositionHandle);        

		// Pass in the color information
		mCubeColors.position(0);
		GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
				0, mCubeColors);        

		GLES20.glEnableVertexAttribArray(mColorHandle);

		// Pass in the normal information
		mCubeNormals.position(0);
		GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 
				0, mCubeNormals);

		GLES20.glEnableVertexAttribArray(mNormalHandle);

		// This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
		// (which currently contains model * view).
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);   

		// Pass in the modelview matrix.
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);                

		// This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
		// (which now contains model * view * projection).
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

		// Pass in the combined matrix.
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Pass in the light position in eye space.        
		GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

		// Draw the cube.
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);                               
	}       

	/**
	 * Create a cube with the given unit dimension
	 * @param d
	 */
	public Cube20(float d) {
		mModelMatrix = new float[16];
		
		// Define points for a cube.            

		// X, Y, Z
		final float[] cubePositionData =
			{
				// In OpenGL counter-clockwise winding is default. This means that when we look at a triangle, 
				// if the points are counter-clockwise we are looking at the "front". If not we are looking at
				// the back. OpenGL has an optimization where all back-facing triangles are culled, since they
				// usually represent the backside of an object and aren't visible anyways.

				// Front face
				-d, d, d,                              
				-d, -d, d,
				d, d, d, 
				-d, -d, d,                             
				d, -d, d,
				d, d, d,

				// Right face
				d, d, d,                               
				d, -d, d,
				d, d, -d,
				d, -d, d,                              
				d, -d, -d,
				d, d, -d,

				// Back face
				d, d, -d,                              
				d, -d, -d,
				-d, d, -d,
				d, -d, -d,                             
				-d, -d, -d,
				-d, d, -d,

				// Left face
				-d, d, -d,                             
				-d, -d, -d,
				-d, d, d, 
				-d, -d, -d,                            
				-d, -d, d, 
				-d, d, d, 

				// Top face
				-d, d, -d,                             
				-d, d, d, 
				d, d, -d, 
				-d, d, d,                              
				d, d, d, 
				d, d, -d,

				// Bottom face
				d, -d, -d,                             
				d, -d, d, 
				-d, -d, -d,
				d, -d, d,                              
				-d, -d, d,
				-d, -d, -d,
			};      

		// R, G, B, A
		final float[] cubeColorData =
			{                               
				// Front face (red)
				1.0f, 0.0f, 0.0f, 1.0f,                         
				1.0f, 0.0f, 0.0f, 1.0f,
				1.0f, 0.0f, 0.0f, 1.0f,
				1.0f, 0.0f, 0.0f, 1.0f,                         
				1.0f, 0.0f, 0.0f, 1.0f,
				1.0f, 0.0f, 0.0f, 1.0f,

				// Right face (green)
				0.0f, 1.0f, 0.0f, 1.0f,                         
				0.0f, 1.0f, 0.0f, 1.0f,
				0.0f, 1.0f, 0.0f, 1.0f,
				0.0f, 1.0f, 0.0f, 1.0f,                         
				0.0f, 1.0f, 0.0f, 1.0f,
				0.0f, 1.0f, 0.0f, 1.0f,

				// Back face (blue)
				0.0f, 0.0f, 1.0f, 1.0f,                         
				0.0f, 0.0f, 1.0f, 1.0f,
				0.0f, 0.0f, 1.0f, 1.0f,
				0.0f, 0.0f, 1.0f, 1.0f,                         
				0.0f, 0.0f, 1.0f, 1.0f,
				0.0f, 0.0f, 1.0f, 1.0f,

				// Left face (yellow)
				1.0f, 1.0f, 0.0f, 1.0f,                         
				1.0f, 1.0f, 0.0f, 1.0f,
				1.0f, 1.0f, 0.0f, 1.0f,
				1.0f, 1.0f, 0.0f, 1.0f,                         
				1.0f, 1.0f, 0.0f, 1.0f,
				1.0f, 1.0f, 0.0f, 1.0f,

				// Top face (cyan)
				0.0f, 1.0f, 1.0f, 1.0f,                         
				0.0f, 1.0f, 1.0f, 1.0f,
				0.0f, 1.0f, 1.0f, 1.0f,
				0.0f, 1.0f, 1.0f, 1.0f,                         
				0.0f, 1.0f, 1.0f, 1.0f,
				0.0f, 1.0f, 1.0f, 1.0f,

				// Bottom face (magenta)
				1.0f, 0.0f, 1.0f, 1.0f,                         
				1.0f, 0.0f, 1.0f, 1.0f,
				1.0f, 0.0f, 1.0f, 1.0f,
				1.0f, 0.0f, 1.0f, 1.0f,                         
				1.0f, 0.0f, 1.0f, 1.0f,
				1.0f, 0.0f, 1.0f, 1.0f
			};

		// X, Y, Z
		// The normal is used in light calculations and is a vector which points
		// orthogonal to the plane of the surface. For a cube model, the normals
		// should be orthogonal to the points of each face.
		final float[] cubeNormalData =
			{                                                                                               
				// Front face
				0.0f, 0.0f, 1.0f,                               
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,                               
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,

				// Right face 
				1.0f, 0.0f, 0.0f,                               
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,                               
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,

				// Back face 
				0.0f, 0.0f, -1.0f,                              
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,                              
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,

				// Left face 
				-1.0f, 0.0f, 0.0f,                              
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,                              
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,

				// Top face 
				0.0f, 1.0f, 0.0f,                       
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,                               
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,

				// Bottom face 
				0.0f, -1.0f, 0.0f,                      
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,                              
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f
			};

		// Initialize the buffers.
		mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();                                                        
		mCubePositions.put(cubePositionData).position(0);               

		mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();                                                        
		mCubeColors.put(cubeColorData).position(0);

		mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();                                                        
		mCubeNormals.put(cubeNormalData).position(0);
	}

}
