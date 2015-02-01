package com.digitalblacksmith.tango_ar_pointcloud;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;

/**
 * 
 * An OpenGL 1.0 renderer.  A clear background with a simple cube.
 * 
 * @author henderso
 *
 */
public class GLClearRenderer implements Renderer, DemoRenderer {


	private Cube mCube = new Cube();
	private float mCubeRotation;

	float x,y,z;
	float qx,qy,qz,qw;


	/*
	 * Set the camera position
	 */
	public void setCameraPosition(float x, float y, float z) {
		this.x=x;
		this.y=y;
		this.z=z;
	}

	/*
	 * Set the camera Euler angles
	 */
	public void setCameraAngles(float x, float y, float z, float w) {
		this.qx=x;
		this.qy=y;
		this.qz=z;
		this.qw=w;
	}


	private double[] quatMatAsArray(double w, double x, double y, double z)
	{
		double[] m = new double[16];
		double qx = 0.0f;
		double qy = 0.0f;
		double qz = 0.0f;
		double qw = 0.0f;

		qw = Math.cos(w / 2);
		qx = Math.sin(w / 2) * x;
		qy = Math.sin(w / 2) * y;
		qz = Math.sin(w / 2) * z;

		m[0] = 1.0f - 2.0f * qy * qy - 2.0f * qz * qz;
		m[1] = 0.0f + 2.0f * qx * qy + 2.0f * qw * qz;
		m[2] = 0.0f + 2.0f * qx * qz - 2.0f * qw * qy;
		m[3] = 0.0f;

		m[4] = 0.0f + 2.0f * qx * qy - 2.0f * qw * qz;
		m[5] = 1.0f - 2.0f * qx * qx - 2.0f * qz * qz;
		m[6] = 0.0f + 2.0f * qy * qz + 2.0f * qw * qx;
		m[7] = 0.0f;

		m[8] = 0.0f + 2.0f * qx * qz + 2.0f * qw * qy;
		m[9] = 0.0f + 2.0f * qy * qz - 2.0f * qw * qx;
		m[10] = 1.0f - 2.0f * qx * qx - 2.0f * qy * qy;
		m[11] = 0.0f;

		m[12] = 0.0f;
		m[13] = 0.0f;
		m[14] = 0.0f;
		m[15] = 1.0f;

		return m;
	}
	public void onDrawFrame( GL10 gl ) {
		// This method is called per frame, as the name suggests.
		// For demonstration purposes, I simply clear the screen with a random translucent gray.
		//float c = 1.0f / 256 * ( System.currentTimeMillis() % 256 );
		//gl.glClearColor( c, c, c, 0.5f );
		//gl.glClear( GL10.GL_COLOR_BUFFER_BIT );
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);        
		gl.glLoadIdentity();

		
		//Move "camera"
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		//gl.glTranslatef(x, y, z);

		Quaternion q= new Quaternion(qx,qy,qz,qw);
		Vector3f rv = q.getEulerAngles();
		gl.glRotatef(rv.x, -1.0f, 0.0f, 0.0f);
		gl.glRotatef(rv.y, 0.0f, -1.0f, 0.0f);
		gl.glRotatef(rv.z, 0.0f, 0.0f, -1.0f);
		//End camera move
		
		gl.glTranslatef(-10*x, -10*y, -10*z);
		
	
		
		//gl.glRotatef(mCubeRotation, 1.0f, 1.0f, 1.0f);

		mCube.draw(gl);
		
		
		

		gl.glLoadIdentity();                                    

	
		
		mCubeRotation -= 0.15f;                                    

	}

	public void onSurfaceChanged( GL10 gl, int width, int height ) {
		// This is called whenever the dimensions of the surface have changed.
		// We need to adapt this change for the GL viewport.
		gl.glViewport( 0, 0, width, height );
		//gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluPerspective(gl, 30.0f, (float)width / (float)height, 0.001f, 100.0f);
		gl.glViewport(0, 0, width, height);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	public void onSurfaceCreated( GL10 gl, EGLConfig config ) {
		// No need to do anything here.
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); 

		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);

		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
				GL10.GL_NICEST);
	}
}