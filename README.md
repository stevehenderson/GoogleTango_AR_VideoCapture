# Google Tango Augmented Reality Video Capture

**PLEASE NOTE: This code if for the Tango Jacobi API**

A simple Google Tango augmented reality (AR) application that uses Point Cloud data to drop "sticky notes" into an augmented reality scene.  The point cloud is used to determine an average depth (from the camera) for the sticky note.  The sticky note is then anchored on this point.

The app also allows the user to take "snapshots" of the screen to save them for offline viewing.

When running the app, the user adds a "sticky note" and, at the same time, the app takes a picture and stored it to the SD card.

Written in Java and OpenGL 2.0.

![Tango Augmented Reality Video Capture](/screenshot.jpg?raw=true "Tango Augmented Reality Video Capture")

In the image above, the inset image is not part of the app.  It is the file that get's stored on the SD card and included for comparison.

ISSUES:  This crashes often on my Tango.  I believe I am interfereing too much with the UI & Tango Threads.  I need to spend some time perfecting the concurrency aspects of the design.  

Things I'e tried:

- Remove all but the minimal code from the OpenGL renderer
- Putting the Bitmap creation task into an asnyc task
- Minimizng the size of the captured image

For more info, follow the conversations here:

http://stackoverflow.com/questions/29189781/using-the-onframeavailable-in-jacobi-google-tango-api
