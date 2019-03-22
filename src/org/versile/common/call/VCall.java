/**
 * Copyright (C) 2012-2013 Versile AS
 *
 * This file is part of Versile Java.
 *
 * Versile Java is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.versile.common.call;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;


/**
 * Reference to an asynchronous call.
 *
 * @param <T> return type of the call result
 */
public class VCall<T> {

	/**
	 * Used for holding a reference to a call which has associated handler(s), to
	 * prevent garbage collection of the calls.
	 */
	static Set<VCall<?>> handled_calls = new HashSet<VCall<?>>();

	boolean have_result = false;
	boolean cancelled = false;
	boolean pushed_cancelled = false;
	T result = null;
	Exception e_result = null;
	boolean is_exception = false;
	LinkedList<VCallResultHandler<T>> result_handlers;
	LinkedList<VCallExceptionHandler> exception_handlers;
	LinkedList<VCallCancelledHandler> cancellation_handlers;

	/**
	 * Create asynchronous call reference.
	 */
	public VCall() {
		result_handlers = new LinkedList<VCallResultHandler<T>>();
		exception_handlers = new LinkedList<VCallExceptionHandler>();
		cancellation_handlers = new LinkedList<VCallCancelledHandler>();
	}

	/**
	 * Check if a result is available
	 *
	 * @return true if available
	 */
	public synchronized boolean hasResult() {
		return have_result;
	}

	/**
	 * Waits indefinitely until a call result is available
	 *
	 * @throws VCallCancelled call was cancelled
	 */
	public synchronized void waitResult()
			throws VCallCancelled {
		try {
			this.waitResult(-1);
		}
		catch (VCallTimeout e) {
			// This should never happen for a blocking wait
			throw new RuntimeException();
		}
	}

	/**
	 * Waits until a call result is available.
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @throws VCallTimeout timeout expired before result was available
	 * @throws VCallCancelled call was cancelled
	 */
	public synchronized void waitResult(long timeout)
			throws VCallTimeout, VCallCancelled {
		this.waitResult(timeout, 0);
	}

	/**
	 * Waits until a call result is available.
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VCallTimeout timeout expired before result was available
	 * @throws VCallCancelled call was cancelled
	 */
	public synchronized void waitResult(long timeout, int ntimeout)
		throws VCallTimeout, VCallCancelled {
		if (have_result)
			return;
		else if (cancelled)
			throw new VCallCancelled();
		long start_time = 0L;
		long end_time = 0L;
		if (timeout >= 0) {
			start_time = System.nanoTime();
			end_time = start_time + 1000000L*timeout + ntimeout;
		}
		while (true) {
			if (have_result)
				return;
			else if (cancelled)
				throw new VCallCancelled();
			try {
				if (timeout < 0)
					wait();
				else {
					long time_left = end_time - System.nanoTime();
					if (time_left <= 0)
						throw new VCallTimeout();
					long msec = time_left / 1000000L;
					wait(msec, (int)(time_left-msec*1000000));
				}
			} catch (InterruptedException e) {
				// Ignore interrupt, treat it just as wait completion
			}
		}
	}

	/**
	 * Requests the result of the asynchronous call (blocking).
	 *
	 * @return call result
	 * @throws VCallException wrapper for an exception raised by the asynchronous call
	 * @throws VCallCancelled call was cancelled
	 */
	public synchronized T getResult()
		throws VCallException, VCallCancelled {
		this.waitResult();
		if (is_exception)
			throw new VCallException(e_result);
		else
			return result;
	}

	/**
	 * Requests the result of the asynchronous call.
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @return call result
	 * @throws VCallException wrapper for an exception raised by the asynchronous call
	 * @throws VCallCancelled call was cancelled
	 */
	public synchronized T getResult(long timeout)
		throws VCallException, VCallTimeout, VCallCancelled {
		this.waitResult(timeout);
		if (is_exception)
			throw new VCallException(e_result);
		else
			return result;
	}

	/**
	 * Requests the result of the asynchronous call.
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @param ntimeout additional timeout in nanoseconds
	 * @return call result
	 * @throws VCallException wrapper for an exception raised by the asynchronous call
	 * @throws VCallCancelled call was cancelled
	 */
	public synchronized T getResult(long timeout, int ntimeout)
		throws VCallException, VCallTimeout, VCallCancelled {
		this.waitResult(timeout);
		if (is_exception)
			throw new VCallException(e_result);
		else
			return result;
	}

	/**
	 * Cancels the asynchronous call.
	 *
	 * <p>Canceling a call means no results or exceptions will be received through this
	 * call reference. Derived classes may implement additional call cancellation
	 * mechanisms for aborting a call in progress. The default implementation will only
	 * cancel receiving a call result and remove any call result reference.</p>
	 *
	 */
	public synchronized void cancel() {
		if (!cancelled) {
			cancelled = true;
			result = null;
			e_result = null;
			notifyAll();
			result_handlers.clear();
			exception_handlers.clear();
			this._cancel();
			for (VCallCancelledHandler handler: cancellation_handlers)
				handler.callback();
			cancellation_handlers.clear();
			this.clearSelfReference();
		}
	}

	/**
	 * Adds a callback handler for when a call result is provided.
	 *
	 * <p>If a result is already available the callback is called immediately.</p>
	 *
	 * @param handler callback handler
	 */
	public synchronized void addResultHandler(VCallResultHandler<T> handler) {
		if (cancelled)
			return;
		else if (have_result) {
			if (!is_exception)
				handler.callback(result);
		}
		else {
			result_handlers.addLast(handler);
			this.setSelfReference();
		}
	}

	/**
	 * Adds a callback handler for when a call exception is provided.
	 *
	 * <p>If an exception is already available the callback is called immediately.</p>
	 *
	 * @param handler callback handler
	 */
	public synchronized void addExceptionHandler(VCallExceptionHandler handler) {
		if (cancelled)
			return;
		else if (have_result) {
			if (is_exception)
				handler.callback((Exception)result);
		}
		else {
			exception_handlers.addLast(handler);
			this.setSelfReference();
		}
	}

	/**
	 * Adds a result and exception callback handler for when a call exception is provided.
	 *
	 * <p>If a result or exception is already available the callback is called immediately.</p>
	 *
	 * @param rhandler result callback handler (or null)
	 * @param ehandler exception callback handler (or null)
	 */
	public synchronized void addHandlerPair(VCallResultHandler<T> rhandler, VCallExceptionHandler ehandler) {
		if (cancelled)
			return;
		else if (have_result) {
			if (!is_exception) {
				if (rhandler != null)
					rhandler.callback(result);
			}
			else if (ehandler != null)
				ehandler.callback((Exception)result);
		}
		else {
			if (rhandler != null) {
				result_handlers.addLast(rhandler);
				this.setSelfReference();
			}
			if (ehandler != null) {
				exception_handlers.addLast(ehandler);
				this.setSelfReference();
			}
		}
	}

	/**
	 * Adds a handler for a cancelled call.
	 *
	 * <p>If the call was already cancelled the handler is called immediately.</p>
	 *
	 * @param handler cancellation handler
	 */
	public synchronized void addCancellationHandler(VCallCancelledHandler handler) {
		if (have_result && !cancelled)
			return;
		if (cancelled)
			handler.callback();
		else {
			cancellation_handlers.addLast(handler);
			this.setSelfReference();
		}
	}

	/**
	 * Sets the result of the asynchronous call.
	 *
	 * <p>May only be called once and may not be called if
	 * {@link VCall#pushException(Exception)} was called.</p>
	 *
	 * @param result call result to set
	 * @throws VCallHaveResult a result or exception was already set
	 * @throws VCallCancelled call was cancelled
	 */
	public synchronized void pushResult(T result)
		throws VCallHaveResult, VCallCancelled {
		if (cancelled)
			throw new VCallCancelled();
		this._pushResult(result);
	}

	/**
	 * Sets an exception result of the asynchronous call.
	 *
	 * <p>May only be called once and may not be called if
	 * {@link VCall#pushResult(Object)} was called.</p>
	 *
	 * @param e call exception to set
	 * @throws VCallHaveResult a result or exception was already set
	 * @throws VCallCancelled call was cancelled
	 */
	public synchronized void pushException(Exception e)
			throws VCallHaveResult, VCallCancelled {
			if (cancelled)
				throw new VCallCancelled();
			this._pushException(e);
		}

	/**
	 * Sets the result of the asynchronous call.
	 *
	 * <p>Same as {@link VCall#pushResult(Object)}, but silently drops
	 * exceptions raised by that method. This is intended for use by code
	 * which is anyway going to ignore those exceptions, to enable more
	 * readable code.</p>
	 *
	 * @param result call result to set
	 */
	public synchronized void silentPushResult(T result) {
		try {
			pushResult(result);
		} catch (VCallOperationException e) {
			// SILENT
		}
	}

	/**
	 * Sets an exception result of the asynchronous call.
	 *
	 * <p>Same as {@link VCall#pushException(Exception)}, but silently drops
	 * exceptions raised by that method. This is intended for use by code
	 * which is anyway going to ignore those exceptions, to enable more
	 * readable code.</p>
	 *
	 * @param e call exception to set
	 * @throws VCallHaveResult a result or exception was already set
	 * @throws VCallCancelled call was cancelled
	 */
	public synchronized void silentPushException(Exception e) {
		try {
			pushException(e);
		} catch (VCallOperationException e2) {
			// SILENT
		}
	}

	/**
	 * Checks if call has been cancelled
	 *
	 * @return true if call was cancelled
	 */
	public synchronized boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Creates a handler for pushing a result to this call.
	 *
	 * <p>A value received by the handler callback is pushed to this call
	 * object as a call result.</p>
	 *
	 * @return result handler
	 */
	public VCallResultHandler<T> pushResultHandler() {
		class Handler implements VCallResultHandler<T> {

			@Override
			public void callback(T result) {
				silentPushResult(result);
			}
		}
		return new Handler();
	}

	/**
	 * Creates a handler for pushing an exception to this call.
	 *
	 * <p>An exception received by the handler callback is pushed to this call
	 * object as a call exception.</p>
	 *
	 * @return exception handler
	 */
	public VCallExceptionHandler pushExceptionHandler() {
		class Handler implements VCallExceptionHandler {
			@Override
			public void callback(Exception e) {
				silentPushException(e);
			}
		}
		return new Handler();
	}

	/**
	 * Creates a handler for pushing a fixed exception in response to an exception.
	 *
	 * <p>This can be used to trigger a standard exception for any handled exception.</p>
	 *
	 * @param e exception to push
	 * @return exception handler
	 */
	public VCallExceptionHandler pushExceptionHandler(Exception e) {
		class Handler implements VCallExceptionHandler {
			Exception exc;
			public Handler(Exception exc) {
				this.exc = exc;
			}
			@Override
			public void callback(Exception e) {
				silentPushException(exc);
			}
		}
		return new Handler(e);
	}

	synchronized void _pushResult(T result)
		throws VCallHaveResult {
		if (have_result)
			throw new VCallHaveResult();
		this.result = result;
		is_exception = false;
		have_result = true;
		this._pushCleanup();
		notifyAll();
		for (VCallResultHandler<T> handler: result_handlers)
			handler.callback(result);
		result_handlers.clear();
		exception_handlers.clear();
		cancellation_handlers.clear();
		this.clearSelfReference();
	}

	synchronized void _pushException(Exception e)
			throws VCallHaveResult {
		if (have_result)
			throw new VCallHaveResult();
		this.e_result = e;
		is_exception = true;
		have_result = true;
		this._pushCleanup();
		notifyAll();
		for (VCallExceptionHandler handler: exception_handlers)
			handler.callback(e);
		result_handlers.clear();
		exception_handlers.clear();
		cancellation_handlers.clear();
		this.clearSelfReference();
	}

	/**
	 * Called internally after a result or exception was pushed.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	protected void _pushCleanup() {
	}

	/**
	 * Called internally if the call is cancelled.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	protected void _cancel() {
	}

	void setSelfReference() {
		synchronized(handled_calls) {
			handled_calls.add(this);
		}
	}

	void clearSelfReference() {
		synchronized(handled_calls) {
			handled_calls.remove(this);
		}
	}
}
