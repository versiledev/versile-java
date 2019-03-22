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

package org.versile.reactor;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.versile.common.call.VCall;


/**
 * Reactor implementation of the reactor pattern.
 *
 * <p>Uses selectors to drive an I/O based event loop.</p>
 */
public class VReactor extends Thread {

	Logger logger;

	Thread thread = null;
	boolean was_started = false;
	boolean pending_stop = false;

	TreeSet<VReactorCall> scheduled_calls;
	Lock scheduled_calls_lock;

	Selector selector;

	/**
	 * Set up a reactor.
	 *
	 * <p>The logger is available for logging by components connected to the reactor.</p>
	 *
	 * @param logger reactor logger (or null)
	 */
	public VReactor(Logger logger) {
		this.logger = logger;

		scheduled_calls = new TreeSet<VReactorCall>();
		scheduled_calls_lock = new ReentrantLock();

		try {
			selector = Selector.open();
		}
		catch (IOException e) {
			throw new RuntimeException();
		}
	}

	/**
	 * Executes the reactor main loop.
	 *
	 * <p>The reactor can be started as a thread, or the reactor main loop can be
	 * run directly. The loop executes until the reactor is stopped (by stopping
	 * itself or by a call to {@link #stopReactor()}.</p>
	 *
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try {
			if (was_started)
				throw new RuntimeException("Can only start reactor once");
			thread = Thread.currentThread();
			this.started();

			LinkedList<VReactorCall> pending_calls = new LinkedList<VReactorCall>();

			while(true) {
				if (pending_stop)
					break;

				try {
					boolean do_wait = false;
					long wait_time = 0;
					synchronized(scheduled_calls_lock) {
						if (!scheduled_calls.isEmpty()) {
							do_wait = true;
							wait_time = (scheduled_calls.first().scheduledTime - System.nanoTime()) / 1000000L;
						}
					}
					if (do_wait) {
						if (wait_time > 0)
							selector.select(wait_time);

						// If <= 0 we do nothing, wait until next reactor loop to perform any select() - this
						// prevents interruption of any effects of wakeup
					}
					else {
						selector.select();
					}
				} catch (IOException e) {
					// Critical error
					break;
				}

				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = it.next();
					it.remove();

					VHandler handler = (VHandler) key.attachment();
					if (handler == null)
						continue;
					if (!key.isValid()) {
						continue;
					}

					try {
						if (key.isConnectable())
							((VConnectingHandler)handler).handleConnect();
						if (key.isAcceptable())
							((VAcceptingHandler)handler).handleAccept();
						if (key.isReadable())
							((VIOHandler)handler).doRead();
						if (key.isWritable())
							((VIOHandler)handler).doWrite();
					}
					catch (CancelledKeyException e) {
					}
				}

				synchronized(scheduled_calls_lock) {
					VReactorCall call;
					boolean have_time = false;
					long current_time = 0L;
					while (!scheduled_calls.isEmpty()) {
						if (!have_time) {
							current_time = System.nanoTime();
							have_time = true;
						}
						call = scheduled_calls.first();
						if (call.scheduledTime <= current_time) {
							pending_calls.addLast(call);
							scheduled_calls.pollFirst();
						}
						else
							break;
					}
				}

				while (!pending_calls.isEmpty()) {
					VReactorCall call = pending_calls.removeFirst();
					try {
						VReactorFunction func = call.function;
						if (func != null) {
							Object result = func.execute();
							call.silentPushResult(result);
						}
					} catch (Exception e) {
						call.silentPushException(e);
					}
				}
			}

			synchronized(scheduled_calls_lock) {
				scheduled_calls.clear();
			}
		} finally {
			try {
				if (selector != null)
					selector.close();
			} catch (IOException e) {
				// SILENT
			}
		}
	}

	/**
	 * Stops a running reactor.
	 *
	 * <p>Thread-safe call which can be called from outside the reactor main thread.</p>
	 */
	public final void stopReactor() {
		pending_stop = true;
		if (thread != Thread.currentThread() && selector != null)
			selector.wakeup();
	}

	/**
	 * Starts monitoring read events for handler.
	 *
	 * <p>Should only be called by the reactor main thread.</p>
	 *
	 * @param handler event handler
	 * @throws IOException error on the I/O subsystem generating events
	 */
	public void startReading(VIOHandler handler)
			throws IOException {
		this.startHandling(handler, SelectionKey.OP_READ);
	}

	/**
	 * Stops monitoring read events for handler.
	 *
	 * <p>Should only be called by the reactor main thread.</p>
	 *
	 * @param handler event handler
	 */
	public void stopReading(VIOHandler handler) {
		this.stopHandling(handler, SelectionKey.OP_READ);
	}

	/**
	 * Starts monitoring write events for handler.
	 *
	 * <p>Should only be called by the reactor main thread.</p>
	 *
	 * @param handler event handler
	 * @throws IOException error on the I/O subsystem generating events
	 */
	public void startWriting(VIOHandler handler)
			throws IOException {
		this.startHandling(handler, SelectionKey.OP_WRITE);
	}

	/**
	 * Stops monitoring write events for handler.
	 *
	 * <p>Should only be called by the reactor main thread.</p>
	 *
	 * @param handler event handler
	 */
	public void stopWriting(VIOHandler handler) {
		this.stopHandling(handler, SelectionKey.OP_WRITE);
	}

	/**
	 * Starts monitoring connect events for handler.
	 *
	 * <p>Should only be called by the reactor main thread.</p>
	 *
	 * @param handler event handler
	 * @throws IOException error on the I/O subsystem generating events
	 */
	public void startHandlingConnect(VConnectingHandler handler)
			throws IOException {
		this.startHandling(handler, SelectionKey.OP_CONNECT);
	}

	/**
	 * Stops monitoring connect events for handler.
	 *
	 * <p>Should only be called by the reactor main thread.</p>
	 *
	 * @param handler event handler
	 */
	public void stopHandlingConnect(VConnectingHandler handler) {
		this.stopHandling(handler, SelectionKey.OP_CONNECT);
	}

	/**
	 * Starts monitoring accept events for handler.
	 *
	 * <p>Should only be called by the reactor main thread.</p>
	 *
	 * @param handler event handler
	 * @throws IOException error on the I/O subsystem generating events
	 */
	public void startHandlingAccept(VAcceptingHandler handler)
			throws IOException {
		this.startHandling(handler, SelectionKey.OP_ACCEPT);
	}

	/**
	 * Stops monitoring accept events for handler.
	 *
	 * <p>Should only be called by the reactor main thread.</p>
	 *
	 * @param handler event handler
	 */
	public void stopHandlingAccept(VAcceptingHandler handler) {
		this.stopHandling(handler, SelectionKey.OP_ACCEPT);
	}

	/**
	 * Log a low-level message to the reactor logger.
	 *
	 * <p>Logs with level "FINEST". Ignored if no logger is set up
	 * on the reactor.</p>
	 *
	 * @param msg log message
	 */
	public void log(String msg) {
		this.log(Level.FINEST, msg);
	}

	/**
	 * Log to reactor logger.
	 *
	 * <p>Ignored if no logger is set up on the reactor.</p>
	 *
	 * @param level logging level
	 * @param msg log message
	 */
	public void log(Level level, String msg) {
		if (logger != null)
			logger.log(level, msg);
	}

	/**
	 * Get reactor thread.
	 *
	 * @return reactor thread
	 */
	public Thread getThread() {
		return thread;
	}

	private void startHandling(VHandler handler, int operation)
			throws IOException {
		SelectableChannel channel = handler.getChannel();

		SelectionKey key = channel.keyFor(selector);
		try {
			if (key == null) {
				try {
					key = channel.register(selector, operation);
					key.attach(handler);
				} catch (ClosedChannelException e) {
					throw new IOException();
				}
			}
			else {
				VHandler _handler = (VHandler) key.attachment();

				if (_handler == null) {
					key.attach(handler);
					_handler = handler;
				}

				if(handler == _handler) {
					int previous_ops = key.interestOps();
					key.interestOps(previous_ops | operation);
				}
				else {
					throw new IOException();
				}
			}
		} catch (CancelledKeyException e) {
			throw new IOException();
		}
	}

	private void stopHandling(VHandler handler, int operation) {
		SelectableChannel channel = handler.getChannel();

		SelectionKey key = channel.keyFor(selector);
		if (key == null)
			return;

		if (!(key.isValid())) {
			return;
		}

		try {
			int key_ops = key.interestOps();
			key_ops = (key_ops | operation) ^ operation;
			key.interestOps(key_ops);

			// Even if key_ops is zero, 'key' will still remain registered with the selector
			// until the socket channel is closed ()

		} catch (CancelledKeyException e) {
		}
	}

	/**
	 * Schedule a function call for (near) immediate execution.
	 *
	 * <p>Thread-safe call which may be called from outside the reactor main thread.</p>
	 *
	 * <p>Calls that are sequentially scheduled to this method are guaranteed to be executed
	 * in the same order they were scheduled.</p>
	 *
	 * @param function function to call
	 * @return call reference
	 */
	public VCall<Object> schedule(VReactorFunction function) {
		return this.schedule(function, 0L);
	}

	/**
	 * Schedule a function call for execution.
	 *
	 * <p>Thread-safe call which may be called from outside the reactor main thread.</p>
	 *
	 * @param function function to call
	 * @param delay time delay in milliseconds
	 * @return call reference
	 */
	public VReactorCall schedule(VReactorFunction function, long delay) {
		long call_time = System.nanoTime() + 1000000L*delay;
		VReactorCall call = new VReactorCall(this, function, call_time);
		synchronized(scheduled_calls_lock) {
			scheduled_calls.add(call);
		}
		selector.wakeup();
		return call;
	}

	/**
	 * Removes a scheduled call if it has not already been executed.
	 *
	 * <p>Thread-safe call which may be called from outside the reactor main thread.</p>
	 *
	 * @param call pending call
	 */
	protected void unschedule(VReactorCall call) {
		synchronized(scheduled_calls_lock) {
			scheduled_calls.remove(call);
		}
		call.reactor = null;
	}

	/**
	 * Called internally when the reactor is started.
	 */
	protected void started() {
	}
}
