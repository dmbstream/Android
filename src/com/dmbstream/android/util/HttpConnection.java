package com.dmbstream.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Iterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import com.bugsense.trace.BugSenseHandler;
import com.dmbstream.android.helpers.ValidationHelper;
import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.UrlWithParameters;

import android.net.Uri;
import android.util.Log;
import android.util.Xml.Encoding;

public class HttpConnection {
	
	private static final String TAG = "HttpConnection";

	private static String get(final String uri, final String userToken) throws Exception {
		return get(uri, userToken, "");
	}
	
	private static String get(final String uri, final String username, final String password) throws Exception {
		BufferedReader in = null;
		String result = "";
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet();
			request.setURI(new URI(uri));
			request.addHeader("X-ApiKey", ApiConstants.instance().getApiKey());
			request.addHeader("User-Agent", ApiConstants.instance().getAppName());
			if (!ValidationHelper.isNullOrWhitespace(username))
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), Encoding.UTF_8.name(), false));
			else
				Log.w(TAG, "No usertoken was specified for the request.");
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			result = sb.toString();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return result;
	}

	private static String post(final String uri, final String userToken, HttpEntity entity) throws Exception {
		BufferedReader in = null;
		String result = "";
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			HttpPost request = new HttpPost();
			request.setURI(new URI(uri));
			request.addHeader("X-ApiKey", ApiConstants.instance().getApiKey());
			request.addHeader("User-Agent", ApiConstants.instance().getAppName());
			request.addHeader("Content-Type", "application/json");
			if (!ValidationHelper.isNullOrWhitespace(userToken))
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(userToken, ""), Encoding.UTF_8.name(), false));
			else
				Log.w(TAG, "No usertoken was specified for the request.");
			request.setEntity(entity);
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			result = sb.toString();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return result;
	}

	public static JSONObject getAsJson(final UrlWithParameters url, final String userToken) throws Exception {
		String finalUrl = url.url;
		if (url.params != null) {
			Iterator<String> keyIterator = url.params.keys();
			while (keyIterator.hasNext()) {
				String key = keyIterator.next();
				String value = url.params.getString(key);
				finalUrl += "&" + Uri.encode(key) + "=" + Uri.encode(value);
			}
		}
		return getAsJson(finalUrl, userToken);
	}
	public static JSONObject getAsJson(final String url, final String userToken) throws Exception {
		return getAsJson(url, userToken, "");
	}
	public static JSONObject getAsJson(final String url, final String username, final String password) throws Exception {
    	String result = null;
    	if (Constants.IsDebug)
    		Log.d(TAG, "getAsJson: " + url);
    	try {
    		result = HttpConnection.get(url, username, password);
    	}catch(IOException ioe) {
    		Log.d(TAG, "Error getting results from url: " + url, ioe);
			BugSenseHandler.log(TAG, ioe);
    	}catch(Exception e) {
    		Log.d(TAG, "Error getting results from url: " + url, e);    		
			BugSenseHandler.log(TAG, e);
    	}finally{
        	if (Constants.IsDebug)
        		Log.d(TAG, result);
    	}
    	
		if(result != null) {
			try {
				return new JSONObject(result);
			}catch(Exception e) {
				Log.e(TAG, "Error parsing JSON: "+result, e);
				BugSenseHandler.log(TAG, e);
			}
		}
		return null;
	}
	
	public static JSONObject postAsJson(final UrlWithParameters url, final String userToken) throws Exception {
		if (url.params == null)
			url.params = new JSONObject();
		
		return postAsJson(url.url, userToken, url.params);
	}
	public static JSONObject postAsJson(String url, final String userToken) throws Exception {
		return postAsJson(url, userToken, new JSONObject());
	}
	public static JSONObject postAsJson(final String url, final String userToken, final JSONObject content) throws Exception {
    	String result = null;
    	if (Constants.IsDebug)
    		Log.d(TAG, "postAsJson: " + url + "\n" + content);
    	try {
    		result = HttpConnection.post(url, userToken, new ByteArrayEntity(content.toString().getBytes(Encoding.UTF_8.name())));
    	}catch(IOException ioe) {
    		Log.d(TAG, "Error getting results from url: " + url, ioe);
			BugSenseHandler.log(TAG, ioe);
    	}catch(Exception e) {
    		Log.d(TAG, "Error getting results from url: " + url, e);
			BugSenseHandler.log(TAG, e);
    	}finally{
        	if (Constants.IsDebug)
        		Log.d(TAG, result);
    	}
    	
		if(result != null) {
			try {
				return new JSONObject(result);
			}catch(Exception e) {
				Log.e(TAG, "Error parsing JSON: "+result, e);
				BugSenseHandler.log(TAG, e);
			}
		}
		return null;
	}
}
