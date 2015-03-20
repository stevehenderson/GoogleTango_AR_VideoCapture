# Google Tango Augmented Reality Sticky Notes 

A simple Google Tango augmented reality (AR) application that uses Point Cloud data to drop "sticky notes" into an augmented reality scene.  The point cloud is used to determine an average depth (from the camera) for the sticky note.  The sticky note is then anchored on this point.

Written in Java and OpenGL 2.0.


![Tango Augmented Reality Sticky Notes](/demo.png?raw=true "Tango Augmented Reality Sticky Notes")


Video pending.

**Lessons Learned**

Lighting is very important when trying to do AR on the Tango and use the Tango depth info:

See my post here:

http://stackoverflow.com/questions/28402718/dark-google-tango-camera-surface-when-using-depth-information/28415697#28415697

Thanks to mengu@cgui for pointing out the underexposure issue
