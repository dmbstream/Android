/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package com.dmbstream.android.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.util.Log;
import android.util.Xml.Encoding;

import com.dmbstream.android.R;
import com.dmbstream.android.helpers.ValidationHelper;
import com.dmbstream.android.util.CancellableTask;
import com.dmbstream.android.util.ProgressListener;
import com.dmbstream.android.util.Util;
import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.Track;

/**
 * @author Sindre Mehus
 */
public class RESTMusicService implements IMusicService {

    private static final String TAG = RESTMusicService.class.getSimpleName();

    private static final int SOCKET_CONNECT_TIMEOUT = 10 * 1000;
    private static final int SOCKET_READ_TIMEOUT_DEFAULT = 30 * 1000;

    private static final int HTTP_REQUEST_MAX_ATTEMPTS = 5;

    private final DefaultHttpClient httpClient;
    private final ThreadSafeClientConnManager connManager;

    public RESTMusicService() {

        // Create and initialize default HTTP parameters
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 20);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(20));
        HttpConnectionParams.setConnectionTimeout(params, SOCKET_CONNECT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_DEFAULT);

        // Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // Create and initialize scheme registry
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

        // Create an HttpClient with the ThreadSafeClientConnManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        connManager = new ThreadSafeClientConnManager(params, schemeRegistry);
        httpClient = new DefaultHttpClient(connManager, params);
    }

    @Override
    public HttpResponse getDownloadInputStream(Context context, Track song, boolean isHighQuality, CancellableTask task) throws Exception {
    	Log.v(TAG, "getDownloadInputStream");
    			
        String url = song.getPlayUrl(isHighQuality);

        HttpResponse response = getResponseForURL(context, url, null, null, null, null, null, task);

        // If content type is XML, an error occurred.  Get it.
        String contentType = Util.getContentType(response.getEntity());
        if (contentType != null && contentType.startsWith("text/json")) {
            InputStream in = response.getEntity().getContent();
            try {
            	// TODO: Urgent: Property handle this error!
            	Log.e(TAG, "There was an error downloading the file. // TODO: Handle this!");
//                new ErrorParser(context).parse(new InputStreamReader(in, Encoding.UTF_8.name()));
            } finally {
                Util.close(in);
            }
        }

        return response;
    }

    private HttpResponse getResponseForURL(Context context, String url, HttpParams requestParams,
                                           List<String> parameterNames, List<Object> parameterValues,
                                           List<Header> headers, ProgressListener progressListener, CancellableTask task) throws Exception {
        Log.d(TAG, "Connections in pool: " + connManager.getConnectionsInPool());

        // If not too many parameters, extract them to the URL rather than relying on the HTTP POST request being
        // received intact. Remember, HTTP POST requests are converted to GET requests during HTTP redirects, thus
        // loosing its entity.
        if (parameterNames != null && parameterNames.size() < 10) {
            StringBuilder builder = new StringBuilder(url);
            for (int i = 0; i < parameterNames.size(); i++) {
                builder.append("&").append(parameterNames.get(i)).append("=");
                builder.append(URLEncoder.encode(String.valueOf(parameterValues.get(i)), "UTF-8"));
            }
            url = builder.toString();
            parameterNames = null;
            parameterValues = null;
        }

        return executeWithRetry(context, url, requestParams, parameterNames, parameterValues, headers, progressListener, task);
    }

    private HttpResponse executeWithRetry(Context context, String url, HttpParams requestParams,
                                          List<String> parameterNames, List<Object> parameterValues,
                                          List<Header> headers, ProgressListener progressListener, CancellableTask task) throws IOException {
        Log.i(TAG, "Using URL " + url);

        final AtomicReference<Boolean> cancelled = new AtomicReference<Boolean>(false);
        int attempts = 0;
        while (true) {
            attempts++;
            HttpContext httpContext = new BasicHttpContext();
            final HttpPost request = new HttpPost(url);

            if (task != null) {
                // Attempt to abort the HTTP request if the task is cancelled.
                task.setOnCancelListener(new CancellableTask.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        cancelled.set(true);
                        request.abort();
                    }
                });
            }

            if (parameterNames != null) {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                for (int i = 0; i < parameterNames.size(); i++) {
                    params.add(new BasicNameValuePair(parameterNames.get(i), String.valueOf(parameterValues.get(i))));
                }
                request.setEntity(new UrlEncodedFormEntity(params, Encoding.UTF_8.name()));
            }

            if (requestParams != null) {
                request.setParams(requestParams);
                Log.d(TAG, "Socket read timeout: " + HttpConnectionParams.getSoTimeout(requestParams) + " ms.");
            }

            if (headers != null) {
                for (Header header : headers) {
                    request.addHeader(header);
                }
            }
            
            // Add default headers to identify this app
			request.addHeader("Content-Type", "application/json");
			request.addHeader("X-ApiKey", ApiConstants.instance().getApiKey());
			request.addHeader("User-Agent", ApiConstants.instance().getAppName());

			String userToken = Util.getUserToken(context);
			if (!ValidationHelper.isNullOrWhitespace(userToken))
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(userToken, ""), Encoding.UTF_8.name(), false));
			else
				Log.w(TAG, "No usertoken was specified for the request.");

            try {
                HttpResponse response = httpClient.execute(request, httpContext);
                return response;
            } catch (IOException x) {
                request.abort();
                if (attempts >= HTTP_REQUEST_MAX_ATTEMPTS || cancelled.get()) {
                    throw x;
                }
                if (progressListener != null) {
                    String msg = context.getResources().getString(R.string.music_service_retry, attempts, HTTP_REQUEST_MAX_ATTEMPTS - 1);
                    progressListener.updateProgress(msg);
                }
                Log.w(TAG, "Got IOException (" + attempts + "), will retry", x);
                increaseTimeouts(requestParams);
                Util.sleepQuietly(2000L);
            }
        }
    }

    private void increaseTimeouts(HttpParams requestParams) {
        if (requestParams != null) {
            int connectTimeout = HttpConnectionParams.getConnectionTimeout(requestParams);
            if (connectTimeout != 0) {
                HttpConnectionParams.setConnectionTimeout(requestParams, (int) (connectTimeout * 1.3F));
            }
            int readTimeout = HttpConnectionParams.getSoTimeout(requestParams);
            if (readTimeout != 0) {
                HttpConnectionParams.setSoTimeout(requestParams, (int) (readTimeout * 1.5F));
            }
        }
    }

}
