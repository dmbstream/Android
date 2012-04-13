package com.dmbstream.android.activity;

import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.dmbstream.android.R;
import com.dmbstream.android.core.LongRunningActionCallback;
import com.dmbstream.android.core.LongRunningActionDispatcher;
import com.dmbstream.android.helpers.ValidationHelper;
import com.dmbstream.android.util.AnalyticsUtil;
import com.dmbstream.android.util.Constants;
import com.dmbstream.android.util.HttpConnection;
import com.dmbstream.android.util.Util;
import com.dmbstream.api.ApiConstants;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginWithTokenActivity extends Activity implements LongRunningActionCallback<Boolean> {
    private static final String TAG = LoginWithTokenActivity.class.getSimpleName();

    private EditText apiToken;
    private LongRunningActionDispatcher<Boolean> dispatcher;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.view_login_with_token);
        super.onCreate(savedInstanceState);

        final SharedPreferences settings = getSharedPreferences(Constants.PREFERENCES_FILE_NAME, MODE_PRIVATE);

        Pattern pattern = Pattern.compile("Click here");
        
        apiToken = (EditText) findViewById(R.id.apiToken);
        apiToken.setText(settings.getString(Constants.ApiToken, ""));
        TextView description = (TextView) findViewById(R.id.description);
        Linkify.addLinks(description, pattern, null, null, new Linkify.TransformFilter() {
			
			@Override
			public String transformUrl(Matcher match, String url) {
				return ApiConstants.instance().baseUrl("account/profile");
			}
		});

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
        		Log.d(TAG, "saveButton clicked");
        		AnalyticsUtil.trackEvent(LoginWithTokenActivity.this, "LoginWithToken", "Click", "Save", 0);
        		
        		final String token = apiToken.getText().toString();

    			dispatcher = new LongRunningActionDispatcher<Boolean>(LoginWithTokenActivity.this, LoginWithTokenActivity.this);
    			dispatcher.startLongRunningAction(new Callable<Boolean>() {
    				public Boolean call() throws Exception {
        		
		        		Boolean isValid = true;
		        		if (ValidationHelper.isNullOrWhitespace(token)) {
		        			apiToken.setError("Required");
		        			isValid = false;
		        		} else {
		        			// Check to see that the token is valid
		        			try {
		        				JSONObject json = HttpConnection.getAsJson(ApiConstants.instance().baseUrl("api/users/current"), token);
		        				if (json == null) {
		        					isValid = false;
		        				}
		        			} catch (Exception e) {
		        				isValid = false;
		        				Log.d(TAG, "Error validating api token: " + e);
		        			}
		        			if (!isValid) {
		            			apiToken.setError("Your API Token appears to be invalid.");
		        			}
		        		}
		        		
		        		return isValid;
    				}
    			}, "", "Validating. Please wait...");
        		
        		
			}
        });
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	AnalyticsUtil.trackPageView(this, "/LoginWithToken");
    }
    public void onLongRunningActionFinished(Boolean result, Exception error) {
    	if (error != null) {
			Log.e(TAG, "Error validating login - " + error.toString());

			Toast toast = Toast.makeText(LoginWithTokenActivity.this, "Website is temporarily unavailable. Please try again in a few minutes.", Toast.LENGTH_LONG);
    		toast.setGravity(Gravity.CENTER, 0, 0);
    		toast.show();    		
    	} else {
    		if (result) {
    			Util.setUserToken(LoginWithTokenActivity.this, apiToken.getText().toString());
        		        		
        		Intent intent = new Intent(LoginWithTokenActivity.this, MainMenuActivity.class);
        		startActivity(intent);
    			
        		Toast toast = Toast.makeText(LoginWithTokenActivity.this, "Account information saved", Toast.LENGTH_SHORT);
        		toast.setGravity(Gravity.TOP, 0, 0);
        		toast.show();
    		} else {
        		Toast toast = Toast.makeText(LoginWithTokenActivity.this, "There were some form errors.", Toast.LENGTH_LONG);
        		toast.setGravity(Gravity.TOP, 0, 0);
        		toast.show();
    		}    		
    	}
    }
}