package com.dmbstream.android.core;

import java.util.concurrent.Callable;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * Use this class if you need to dispatch expensive (long running) operations
 * from your Activity. The long running operation is provided to
 * {@link startLongRunningAction} as a {@link Callable}. The result of the
 * operation and any potential exception that occurred during the call are
 * passed to {@link LongRunningActionCallback.onLongRunningActionFinished},
 * which will be called on successful or unsuccessful completion of the
 * Callable.
 *
 * LICENSE STATEMENT (DO NOT REMOVE):
 * This code is in the public domain. You may use, alter, and redistribute it
 * free of any charges or obligations, with the following exceptions:
 * 1. You are not allowed to remove the statement naming the original author.
 * 2. You are not allowed to remove this license statement.
 *
 * @author Matthias Kaeppler
 * {@link http://brainflush.wordpress.com/2008/08/15/handling-long-running-operations-in-google-android}
 */
public final class LongRunningActionDispatcher<ResultType> {

    private Context context;

    private LongRunningActionCallback<ResultType> callback;

    /**
     * A progress dialog shown during long-lasting operations
     */
    private ProgressDialog progressDialog;

    private Handler finishedHandler = new Handler();

    public LongRunningActionDispatcher(Context context,
            LongRunningActionCallback<ResultType> callback) {
        this.context = context;
        this.callback = callback;
    }

    /**
     * Invoke this method to start long running operations which may block your
     * activity and therefore the main UI thread. A progress dialog will be
     * shown while the operation is executing.
     *
     * @param callable
     *            The callable
     * @param progressDialogTitle
     *            The progress dialog title
     * @param progressDialogMessage
     *            The progress dialog message
     */
    public void startLongRunningAction(final Callable<ResultType> callable,
            String progressDialogTitle, String progressDialogMessage) {

        progressDialog = ProgressDialog.show(context, progressDialogTitle,
                progressDialogMessage, true, false);

        new Thread(new Runnable() {

            public void run() {
                ResultType result = null;
                Exception error = null;
                try {
                    result = callable.call();
                } catch (Exception e) {
                    Log.e("LongRunningActionDispatcher", "Error starting long running action", e);
                    error = e;
                }

                final ResultType finalResult = result;
                final Exception finalError = error;
                finishedHandler.post(new Runnable() {

                    public void run() {
                        onLongRunningActionFinished(finalResult, finalError);
                    }
                });
            }
        }).start();
    }

    private void onLongRunningActionFinished(ResultType result, Exception error) {
        progressDialog.dismiss();
        callback.onLongRunningActionFinished(result, error);
    }
}