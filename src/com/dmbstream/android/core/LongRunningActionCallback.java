package com.dmbstream.android.core;

/**
 * LICENSE STATEMENT (DO NOT REMOVE):
 * This code is in the public domain. You may use, alter, and redistribute it
 * free of any charges or obligations, with the following exceptions:
 * 1. You are not allowed to remove the statement naming the original author.
 * 2. You are not allowed to remove this license statement.
 *
 * @author Matthias Kaeppler
 * {@link http://brainflush.wordpress.com/2008/08/15/handling-long-running-operations-in-google-android}
 */
public interface LongRunningActionCallback<ResultType> {

    /**
     * Called when the callable provided to
     * {@link LongRunningActionDispatcher.startLongRunningAction} completes.
     *
     * @param <ResultType>
     *            The result type of callable.call()
     * @param result
     *            Whatever the callable returns if it completes successfully, or
     *            null if an exception was thrown
     * @param error
     *            Whatever the callable throws if it executes in error, or null
     *            if it completed successfully
     */
    void onLongRunningActionFinished(ResultType result, Exception error);
}