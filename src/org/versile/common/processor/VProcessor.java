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

package org.versile.common.processor;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.versile.common.util.VLinearIDProvider;



/**
 * Thread-based asynchronous task processor.
 *
 * <p>Queues tasks and instantiates worker threads to execute tasks from the queue.
 * Worker threads are instantiated during constructions and continue to run until
 * either {@link #shutdown} or {@link #shutdownNow} has been called.</p>
 */
public class VProcessor {
	ExecutorService executor;
	boolean lazy_executor;

	boolean terminated = false;
	VLinearIDProvider group_id_provider;
	LinkedList<Task> tasks;
	Map<Object, Long> groups;

	/**
	 * Creates a default processor.
	 *
	 * <p>Default processor has 5 worker threads.</p>
	 */
	public VProcessor() {
		// Currently HARDCODED: minimum 5 and maximum 5 worker threads, due to ISSUE: erratic
		// thread pool executor in testing when core pool size < 2
		executor = new ThreadPoolExecutor(5, 5, 0L, TimeUnit.MILLISECONDS,
											new LinkedBlockingQueue<Runnable>() );
		lazy_executor = true;
		this.construct();
	}

	/**
	 * Creates a processor with a new executor.
	 *
	 * <p>Note: recommend the 'minWorkers' parameters should be at least 2 as lower values have
	 * showed erratic behavior of the associated generated executor in testing; the reason for
	 * such instability is not clear.</p>
	 *
	 * @param minWorkers minimum worker threads
	 * @param maxWorkers maximum worker threads
	 */
	public VProcessor(int minWorkers, int maxWorkers) {
		executor = new ThreadPoolExecutor(minWorkers, maxWorkers, 0L, TimeUnit.MILLISECONDS,
											new LinkedBlockingQueue<Runnable>() );
		lazy_executor = true;
		this.construct();
	}

	/**
	 * Creates a processor on an existing executor providing worker thread services.
	 *
	 * @param executor worker thread executor.
	 */
	public VProcessor(ExecutorService executor) {
		lazy_executor = false;
		this.executor = executor;
		this.construct();
	}

	void construct() {
		group_id_provider = new VLinearIDProvider();
		tasks = new LinkedList<Task>();
		groups = new Hashtable<Object, Long>();
	}

	@Override
	public void finalize() {
		if (lazy_executor) {
			this.shutdown();
		}
	}

	/**
	 * Checks if task queue has task(s) associated with a group.
	 *
	 * @param group associated group
	 * @return true if queue has associated tasks
	 */
	public synchronized boolean hasGroupCalls(Object group) {
		Long group_id = groups.get(group);
		if (group_id != null)
			for (Iterator<Task> it = tasks.listIterator() ; it.hasNext();) {
				Task task = it.next();
				if (task.group_id == group_id)
					return true;
			}
		return false;
	}

	/**
	 * Removes all tasks associated with group from the task queue.
	 *
	 * @param group associated group
	 */
	public synchronized void removeGroupCalls(Object group) {
		Long group_id = groups.get(group);
		if (group_id != null) {
			for (Iterator<Task> it = tasks.listIterator() ; it.hasNext();) {
				Task task = it.next();
				if (task.group_id == group_id)
					it.remove();
			}
		}
	}

	/**
	 * Adds a call group object which can be used to associate queued calls.
	 *
	 * @param group call group object
	 */
	public synchronized void removeGroup(Object group) {
		groups.remove(group);
	}

	/**
	 * Submits a task to the processor task queue.
	 *
	 * @param job task to queue
	 * @throws VProcessorException processor error
	 */
	public void submit(Runnable job)
		throws VProcessorException {
		this.submit(job, null);
	}

	/**
	 * Submits a task to the processor queue and associates to a task group.
	 *
	 * @param job task to queue
	 * @param group associated task group
	 * @throws VProcessorException processor error
	 */
	public synchronized void submit(Runnable job, Object group)
			throws VProcessorException {
		if (terminated)
			throw new VProcessorException("Processor was terminated");
		Task task = new Task();
		task.job = job;
		if (group == null)
			task.group_id = -1;
		else {
			Long group_id = groups.get(group);
			if (group_id == null) {
				group_id = group_id_provider.getID();
				groups.put(group, group_id);
				task.group_id = group_id;
			}
			task.group_id = group_id;
		}
		tasks.addLast(task);
		class Job implements Runnable {
			@Override
			public void run() {
				Task t = getNextTask();
				if (t != null)
					t.job.run();
			}
		}
		executor.submit(new Job());
	}

	/**
	 * Shuts down the processor after finishing submitted tasks.
	 */
	public synchronized void shutdown() {
		if (!terminated) {
			terminated = true;
			if (lazy_executor)
				executor.shutdown();
		}
	}

	/**
	 * Immediately shuts down the processor.
	 */
	public synchronized void shutdownNow() {
		if (!terminated) {
			terminated = true;
			tasks.clear();
			groups = new Hashtable<Object, Long>();
			if (lazy_executor)
				executor.shutdownNow();
		}
	}

	synchronized Task getNextTask() {
		return tasks.removeFirst();
	}

	class Task {
		public Runnable job;
		public long group_id;
	}
}
