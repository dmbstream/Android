package com.dmbstream.android.activity;

import java.util.Calendar;
import java.util.concurrent.Callable;

import javax.security.auth.login.LoginException;

import org.json.JSONObject;

import com.dmbstream.android.R;
import com.dmbstream.android.core.LongRunningActionCallback;
import com.dmbstream.android.core.LongRunningActionDispatcher;
import com.dmbstream.android.helpers.StringHelper;
import com.dmbstream.android.helpers.ValidationHelper;
import com.dmbstream.android.util.AnalyticsUtil;
import com.dmbstream.android.util.HttpConnection;
import com.dmbstream.android.util.Util;
import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.User;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity implements LongRunningActionCallback<User> {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private EditText username;
    private EditText password;
    private LongRunningActionDispatcher<User> dispatcher;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.view_login);
        super.onCreate(savedInstanceState);

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        TextView description = (TextView) findViewById(R.id.description);
        StringHelper.makeClickable(description, "Click here", new ClickableSpan() {
        	@Override
        	public void onClick(View widget) {
        		Log.d(TAG, "description::Click here::onClick");
	    		Intent intent = new Intent(LoginActivity.this, LoginWithTokenActivity.class);
	    		startActivity(intent);
        	}
        });

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
        		Log.d(TAG, "saveButton clicked");
        		
        		final String u = username.getText().toString();
        		final String p = password.getText().toString();
        		boolean isValid = true;
        		if (ValidationHelper.isNullOrWhitespace(u)) {
        			username.setError(StringHelper.getFixedErrorString("Required"));
        			isValid = false;
        		}
        		if (ValidationHelper.isNullOrWhitespace(p)) {
        			password.setError(StringHelper.getFixedErrorString("Required"));
        			isValid = false;
        		}

        		if (isValid) {
            		AnalyticsUtil.trackEvent(LoginActivity.this, "LoginWithToken", "Click", "Save", 0);
	    			dispatcher = new LongRunningActionDispatcher<User>(LoginActivity.this, LoginActivity.this);
	    			dispatcher.startLongRunningAction(new Callable<User>() {
						public User call() throws Exception {

							// Check to see that the token is valid
							JSONObject result = HttpConnection.getAsJson(ApiConstants.instance().baseUrl("api/users/GetAuthenticationToken"), u, p);
							
							if (result.has("message")) {
								throw new LoginException("Invalid username or password");
							}
							
							User user = new User();
							user.loadFromJson(result);
														
							return user;

	    				}
	    			}, "", "Validating. Please wait...");
        		} else {
            		Toast toast = Toast.makeText(LoginActivity.this, "There were some form errors.", Toast.LENGTH_SHORT);
            		toast.setGravity(Gravity.CENTER, 0, 0);
            		toast.show();
        		}
			}
        });
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	AnalyticsUtil.trackPageView(this, "/Login");
    }

	public void onLongRunningActionFinished(User user, Exception error) {
    	if (error != null || user == null) {
			Log.e(TAG, "Error validating login - " + error.toString());

			if (error instanceof LoginException) {
				password.setError(StringHelper.getFixedErrorString("Incorrect username or password"));
				Toast toast = Toast.makeText(LoginActivity.this, "Your username or password were incorrect.", Toast.LENGTH_LONG);
	    		toast.setGravity(Gravity.CENTER, 0, 0);
	    		toast.show();
			} else {
				Toast toast = Toast.makeText(LoginActivity.this, "Website is temporarily unavailable. Please try again in a few minutes.", Toast.LENGTH_LONG);
	    		toast.setGravity(Gravity.CENTER, 0, 0);
	    		toast.show();
			}
    	} else {

    		Util.setLastAccessed(LoginActivity.this, Calendar.getInstance().getTimeInMillis());
    		Util.setUserToken(LoginActivity.this, user.token);
    		Util.setUsername(LoginActivity.this, user.name);
    		Util.setUserId(LoginActivity.this, user.id);
    		Util.setIsDonor(LoginActivity.this, user.isDonor);
			if (!user.isDonor)
				Util.setPreloadCount(LoginActivity.this, 1);

			Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
			startActivity(intent);

			Toast toast = Toast.makeText(LoginActivity.this, "Account information saved", Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.TOP, 0, 0);
			toast.show();  		
    	}
    }
}