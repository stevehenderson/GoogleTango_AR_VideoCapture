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

package com.digitalblacksmith.tango_ar_videocapture;

import java.io.File;

import com.google.atap.tangoservice.Tango;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

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

	
	public final static String SCREENCAPTURE_DIRECTORY = "GoogleTangoARVideoCapture"; 
	
	private Button startButton;	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startActivityForResult(
				Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING),
				Tango.TANGO_INTENT_ACTIVITYCODE);
		setContentView(R.layout.start);
		
		
		//Check if external storage location exists		
		final String PATH = Environment.getExternalStorageDirectory() + "/" + SCREENCAPTURE_DIRECTORY + "/";
		if(!(new File(PATH)).exists()) {
			new File(PATH).mkdirs();
		}
		
		this.setTitle(R.string.app_name);
		startButton = (Button) findViewById(R.id.startButton);
		startButton.setOnClickListener(this);		

	}

	@Override
	public void onClick(View v) {
		Intent startmotiontracking = new Intent(this, TangoActivity.class);
 		startmotiontracking.putExtra(OPENGL_VERSION, 2);
 		startActivity(startmotiontracking);
     	
	}

	
	
}
