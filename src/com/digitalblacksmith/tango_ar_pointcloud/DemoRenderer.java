package com.digitalblacksmith.tango_ar_pointcloud;

public interface DemoRenderer {
	public void setCameraPosition(float x, float y, float z);
	public void setCameraAngles(float qx, float qy, float qz, float qw);
}
