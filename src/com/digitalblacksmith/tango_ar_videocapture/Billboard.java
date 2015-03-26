package com.digitalblacksmith.tango_ar_videocapture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * A billboard class for OpenGL
 * 
 * Lot's of help from here
 * http://stackoverflow.com/questions/7767367/how-to-fill-each-side-of-a-cube-with-different-textures-on-opengl-es-1-1
 * 
 * @author henderso
 *
 */
public class Billboard {

	/** Store our model data in a float buffer. */
	private final FloatBuffer mCubePositions;
	private final FloatBuffer mCubeColors;
	private final FloatBuffer mCubeNormals;
	private final FloatBuffer mCubeTextureCoordinates;

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


	private int mTextureCoordinateHandle;

	/** This will be used to pass in the texture. */
	private int mTextureUniformHandle;

	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;   

	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;        

	/** Size of the color data in elements. */
	private final int mColorDataSize = 4;   

	/** Size of the normal data in elements. */
	private final int mNormalDataSize = 3;

	/** Size of the texture coordinate data in elements. */
	private final int mTextureCoordinateDataSize = 2;

	/** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
	private final float[] mLightPosInEyeSpace = new float[4];

	/** This is a handle to our per-vertex cube shading program. */
	private int mPerVertexProgramHandle;


	/**
	 * The Billboards texture handle.  Set and loaded in renderer
	 */
	private int mTextureDataHandle;

	/**
	 * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
	 * of being located at the center of the universe) to world space.
	 */
	private float[] mModelMatrix = new float[16];

	/**
	 *	The Billboard's location in world space (X)
	 */
	float x;

	/**
	 *	The Billboard's location in world space (Y)
	 */
	float y;

	/**
	 *	The Billboard's location in world space (Z)
	 */
	float z;

	/**
	 * The rendering camera location in world space (X)
	 */
	float camx;

	/**
	 * The rendering camera location in world space (Y)
	 */
	float camy;

	/**
	 * The rendering camera location in world space (Z)
	 */
	float camz;

	/**
	 * Sets the rendering camera's location
	 * @param cx
	 * @param cy
	 * @param cz
	 */
	public void setCameraPosition(float cx, float cy, float cz) {

		camx = cx;
		camy = cy;
		camz = cz;

	}


	/**
	 * @param mPerVertexProgramHandle the mPerVertexProgramHandle to set
	 */
	public void setShader(int mProgramHandle) {
		this.mPerVertexProgramHandle = mProgramHandle;
		// Set program handles for cube drawing.
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
		mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix"); 
		mLightPosHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos");
		mTextureUniformHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_Texture");
		mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
		mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color");
		mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal"); 
		mTextureCoordinateHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_TexCoordinate");

	}


	/**
	 * 
	 * Sets the position of the billboard in space (same coordinates as tango)
	 * 
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
	 * Draws the billboard
	 */                     
	public void drawBillboard(float[] mMVPMatrix, float[] mProjectionMatrix, float[] mViewMatrix)
	{               

		// Set the active texture unit to texture unit 0.
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		// Bind the texture to this unit.
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

		// Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
		GLES20.glUniform1i(mTextureUniformHandle, 0);    

		Matrix.setIdentityM(mModelMatrix, 0);	

		float[] mCameraRot = new float[16];  
		float[] mCameraTrans = new float[16];

		Matrix.setIdentityM(mCameraTrans, 0);
		Matrix.setIdentityM(mCameraRot, 0);

		Matrix.translateM(mCameraTrans, 0, x, y, z);
		Matrix.setLookAtM(mModelMatrix, 0, x,y,z,camx,camy,camz,0,0,1);		

		Matrix.invertM(mModelMatrix, 0, mModelMatrix, 0);
		Matrix.rotateM(mModelMatrix, 0, -90, 1, 0, 0);

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

		// Pass in the texture coordinate information
		mCubeTextureCoordinates.position(0);
		GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 
				0, mCubeTextureCoordinates);

		GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);


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
	 * Create the Billboard
	 * @param d
	 */
	public Billboard(int aTexture, float d) {
		mModelMatrix = new float[16];

		// Load the texture
		mTextureDataHandle = aTexture;

		// Define points for a cube.            
		float i = 1.0f* d;
		float j = 0.1f* d;
		float k = 1.0f* d;

		// X, Y, Z
		final float[] cubePositionData =
		{	

			//NOTE, FOR TANGO the TOP and BOTTOM faces are facing the users line of site

			// Front face
			-i, j, k,                              
			-i, -j, k,
			i, j, k, 
			-i, -j, k,                             
			i, -j, k,
			i, j, k,

			// Right face
			i, j, k,                               
			i, -j, k,
			i, j, -k,
			i, -j, k,                              
			i, -j, -k,
			i, j, -k,

			// Back face
			i, j, -k,                              
			i, -j, -k,
			-i, j, -k,
			i, -j, -k,                             
			-i, -j, -k,
			-i, j, -k,

			// Left face
			-i, j, -k,                             
			-i, -j, -k,
			-i, j, k, 
			-i, -j, -k,                            
			-i, -j, k, 
			-i, j, k, 

			// Top face
			-i, j, -k,                             
			-i, j, k, 
			i, j, -k, 
			-i, j, k,                              
			i, j, k, 
			i, j, -k,

			// Bottom face
			i, -j, -k,                             
			i, -j, k, 
			-i, -j, -k,
			i, -j, k,                              
			-i, -j, k,
			-i, -j, -k,
		};      

		// R, G, B, A
		final float[] cubeColorData =
		{                               
			// Front face (red)
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,

			// Right face (green)
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,

			// Back face (blue)
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,

			// Left face (yellow)
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,

			// Top face (cyan)
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,

			// Bottom face (magenta)
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,                         
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
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

		// S, T (or X, Y)
		// Texture coordinate data.
		// Because images have a Y axis pointing downward (values increase as you move down the image) while
		// OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
		// What's more is that the texture coordinates are the same for every face.
		final float[] cubeTextureCoordinateData =
		{												
			// Front face
			/*0.0f, 0.0f, 				
					0.0f, 1.0f,
					1.0f, 0.0f,
					0.0f, 1.0f,
					1.0f, 1.0f,
					1.0f, 0.0f,*/				

			0.0f, 0.0f, 				
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,	

			// Right face 
			0.0f, 0.0f, 				
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,	

			// Back face 
			0.0f, 0.0f, 				
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,	

			// Left face 
			0.0f, 0.0f, 				
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 0.0f,	

			// Top face 
			1.0f, 1.0f, 				
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 1.0f,

			// Bottom face 
			1.0f, 1.0f, 				
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 0.0f,
			0.0f, 1.0f,
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

		mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);
	}


}