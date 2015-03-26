package com.digitalblacksmith.tango_ar_videocapture;

import java.nio.IntBuffer;

/*
 * The CameraFragmentInterface is a generic interface that provides callback mechanism 
 * to an implementing activity.
 * 
 */
interface TangoCameraScreengrabCallback {	
	public void newPhotoBuffer(int w, int h);
}
