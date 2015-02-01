/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalblacksmith.tango_ar_pointcloud;

import com.google.atap.tangoservice.Tango;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Application's entry point where the user gets to select a certain configuration and start the
 * next activity.
 */
public class StartActivity extends Activity implements View.OnClickListener {
	public static final String OPENGL_VERSION = 
			"com.projecttango.experiments.javamotiontracking.opengl_version";

	private Button mOpenGL1Button;
	private Button mOpenGL2Button;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startActivityForResult(
				Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING),
				Tango.TANGO_INTENT_ACTIVITYCODE);
		setContentView(R.layout.start);
		this.setTitle(R.string.app_name);
		mOpenGL1Button = (Button) findViewById(R.id.opengl1);
		mOpenGL2Button = (Button) findViewById(R.id.opengl2);

		mOpenGL1Button.setOnClickListener(this);
		mOpenGL2Button.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.opengl1:
			startMotionTracking(1.0);
			break;
		case R.id.opengl2:
			startMotionTracking(2.0);
			break;
		}
	}

	private void startMotionTracking(double d) {
		Intent startmotiontracking = new Intent(this, PointCloudActivity.class);
		startmotiontracking.putExtra(OPENGL_VERSION, d);
		startActivity(startmotiontracking);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
			// Make sure the request was successful
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, R.string.motiontrackingpermission, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}
}
