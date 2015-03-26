# Google Tango Augmented Reality Video Capture

**PLEASE NOTE: This code if for the Tango Jacobi API**

A simple Google Tango augmented reality (AR) application that uses Point Cloud data to drop "sticky notes" into an augmented reality scene.  The point cloud is used to determine an average depth (from the camera) for the sticky note.  The sticky note is then anchored on this point.

The app also allows the user to take "snapshots" of the screen to save them for offline viewing.

When running the app, the user adds a "sticky note" and, at the same time, the app takes a picture and stored it to the SD card.

Written in Java and OpenGL 2.0.

![Tango Augmented Reality Video Capture](/screenshot.jpg?raw=true "Tango Augmented Reality Video Capture")

<i>In the image above, a user tags a keyboard with a sticky note.  The  inset image (not rendered as part of the app's current view) represents  the corresponding file that get's stored on the SD card and included for comparison.</i>

I essentially set up a "man (renderer) in the middle" attack on the rendering pipeline. This approach intercepts the SetRenderer call from the TangoCameraPreview base class, and allows one to get access to the base renderer's OnDraw() method and the GL context. I then add additional methods to this extended renderer that allow reading of the GL buffer.

General approach

1) Extend the TangoCameraPreview class (e.g. in my example ReadableTangoCameraPreview). Override the setRenderer(GLSurfaceView.Renderer renderer), keeping a reference to the base renderer, and replacing the renderer with your own "wrapped" GLSUrface.Renderer renderer that will add methods to render the backbuffer to an image on the device.

2) Create your own GLSurfaceView.Renderer Interface (e.g. my ScreenGrabRenderer class ) that implements all the GLSurfaceView.Renderer methods, passing them on to the base renderer captured in Step 1. Also, add a few new methods to "cue" when you want to grab the image.

3) Implement the ScreenGrabRenderer described in step 2 above.

4) Use a callback interface (my TangoCameraScreengrabCallback) to communicate when an image has been copied

ISSUES:  This crashes often on my Tango.  I believe I am interfereing too much with the UI & Tango Threads.  I need to spend some time perfecting the concurrency aspects of the design.  

Things I've tried:

- Remove all but the minimal code from the OpenGL renderer
- Putting the Bitmap creation task into an asnyc task
- Minimizng the size of the captured image

For more info, follow the conversations here:

http://stackoverflow.com/questions/29189781/using-the-onframeavailable-in-jacobi-google-tango-api
