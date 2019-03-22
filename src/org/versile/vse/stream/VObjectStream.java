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

package org.versile.vse.stream;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;


/**
 * Stream proxy for streams transferring object data.
 *
 * <p>Objects must be lazy-convertible to/from VEntity.</p>
 *
 * @param <T> stream object type
 */
public class VObjectStream<T> extends VStream<T> implements Iterable<T> {

	public VObjectStream(VStreamPeer<T> stream) {
		super(stream);
	}

	/**
	 * Reads stream data from the current read context (blocking).
	 *
	 * <p>The stream should normally have an active read context when calling this
	 * method. If a set of empty data elements is returned then end-of-stream
	 * was reached.</p>
	 *
	 * <p>The method may return fewer than 'maxNum' elements, however if returned
	 * data is not empty this does not imply end-of-stream.</p>
	 *
	 * <p>If the stream currently has an active write context, calling this method
	 * will initiate a new read context on the current position.</p>
	 *
	 * @param maxNum max elements to read
	 * @return data elements read
	 * @throws VStreamTimeout
	 * @throws VStreamError
	 */
	public T[] recv(int maxNum)
			throws VStreamError {
		try {
			return this.recv(maxNum, -1);
		} catch (VStreamTimeout e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads stream data from the current read context.
	 *
	 * <p>See {@link #recv(int)}. Raises {@link VStreamTimeout} if no data could
	 * be read before timeout expired.</p>
	 *
	 * @param maxNum max elements to read
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @throws VStreamTimeout
	 */
	public T[] recv(int maxNum, long timeout)
			throws VStreamTimeout, VStreamError {
		return this.recv(maxNum, timeout, 0);
	}

	/**
	 * Reads stream data from the current read context.
	 *
	 * <p>See {@link #recv(int)}. Raises {@link VStreamTimeout} if no data could
	 * be read before timeout expired.</p>
	 *
	 * @param maxNum max elements to read
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VStreamTimeout
	 */
	public T[] recv(int maxNum, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		return _stream.recv(maxNum, timeout, ntimeout);
	}

	/**
	 * Reads stream data from the current read context until end-of-stream.
	 *
	 * <p>See {@link #read(int)}.</p>
	 *
	 * @return data read
	 */
	public T[] read()
			throws VStreamError {
		return this.read(-1);
	}

	/**
	 * Reads stream data from the current read context.
	 *
	 * <p>See {@link #read(int, int)}. Uses 1MB as the recv chunk size for
	 * reading unlimited stream data.</p>
	 *
	 * @param num number of elements to read (or negative)
	 * @return data read
	 */
	public T[] read(int num)
			throws VStreamError {
		return this.read(num, 1024*1024);
	}

	/**
	 * Reads stream data from the current read context.
	 *
	 * <p>
	 * If 'num' is negative then all data is read from the stream until
	 * end-of-stream is reached (requesting end-of-stream policy must be set to
	 * true on the stream).
	 * </p>
	 *
	 * <p>
	 * This is a convenience method for performing blocking read operations
	 * until the requested amount of data has been read or end-of-stream is
	 * reached.
	 * </p>
	 *
	 * <p>The stream should normally have an active read context when calling this
	 * method. If a set of empty data elements is returned then end-of-stream
	 * was reached.</p>
	 *
	 * <p>The method may return fewer than 'num' elements if end-of-stream was
	 * reached. If the stream end-of-stream policy is not set to True then the
	 * call will block indefinitely when reaching the stream boundary.</p>
	 *
	 * <p>If the stream currently has an active write context, calling this method
	 * will initiate a new read context on the current position.</p>
	 *
	 * @param num number of elements to read (or negative)
	 * @param blockSize block size for internal recv operations when num < 0
	 * @return data read
	 */
	public T[] read(int num, int blockSize)
			throws VStreamError {
		LinkedList<T[]> chunks = new LinkedList<T[]>();
		int num_read = 0;
		int recv_lim;

		while (num < 0 || num_read < num) {
			if (num < 0)
				recv_lim = blockSize;
			else
				recv_lim = num - num_read;
			T[] data = this.recv(recv_lim);
			if (data.length == 0)
				break;
			chunks.addLast(data);
			num_read += data.length;
		}

		T[] result = _stream._buf.createArray(num_read);
		int pos = 0;
		for (T[] chunk: chunks) {
			for (int i = 0; i < chunk.length; i++)
				result[pos+i] = chunk[i];
			pos += chunk.length;
		}
		return result;
	}

	/**
	 * Writes data to a current active stream write context.
	 *
	 * <p>Returns the number of elements that could be written, which may be less
	 * than the number of elements held by *data*. Raises {@link VStreamTimeout}
	 * if no data could be written before timeout expired.</p>
	 *
	 * <p>There is no guarantee that accepted data was actually received and
	 * processed as expected by a peer streamer, it only acknowledges that data
	 * was sent to the peer streamer.</p>
	 *
	 * <p>If the stream currently has an active read context, calling this method
	 * will initiate a new write context on the current position.</p>
	 *
	 * <p>Due to read-ahead buffering effects, the start position of the peer
	 * streamer's resulting write context may not be deterministic. In order to
	 * control position, {@link #wseek} should be called first (if seek is
	 * allowed on stream), or read-ahead must be disabled.</p>
	 *
	 * @param data data to write
	 * @return number elements sent
	 */
	public int send(T[] data)
			throws VStreamError {
		try {
			return this.send(data, -1);
		} catch (Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes data to a current active stream write context.
	 *
	 * <p>See {@link #send(Object[])}. Raises {@link VStreamTimeout}
	 * if no data could be written before timeout expired.</p>
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 */
	public int send(T[] data, long timeout)
			throws VStreamTimeout, VStreamError {
		return this.send(data, timeout, 0);
	}

	/**
	 * Writes data to a current active stream write context.
	 *
	 * <p>See {@link #send(Object[])}. Raises {@link VStreamTimeout}
	 * if no data could be written before timeout expired.</p>
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @param ntimeout additional timeout in nanoseconds
	 */
	public int send(T[] data, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		return _stream.send(data, timeout, ntimeout);
	}

	/**
	 * Writes data to a current active stream write context.
	 *
	 * <p>This is a convenience method for performing blocking write operations
	 * until all data has been written. Writes all data before returning. If all
	 * data could not be written or an error condition occurs while reading, an
	 * exception is raised.</p>
	 *
	 * <p>There is no guarantee that accepted data was actually received and
	 * processed as expected by a peer streamer, it only acknowledges that data
	 * was sent to the peer streamer.</p>
	 *
	 * <p>If the stream currently has an active read context, calling this method
	 * will initiate a new write context on the current position.</p>
	 *
	 * <p>Due to read-ahead buffering effects, the start position of the peer
	 * streamer's resulting write context may not be deterministic. In order to
	 * control position, {@link #wseek} should be called first (if seek is
	 * allowed on stream), or read-ahead must be disabled.</p>
	 *
	 * @param data data to write
	 * @throws VStreamError
	 */
	public void write(T[] data)
			throws VStreamError {
		int num_unsent = data.length;
		T[] send_data = data;
		while(true) {
			int sent = this.send(send_data);
			num_unsent -= sent;
			if (num_unsent == 0)
				break;
			T[] _data = _stream._buf.createArray(num_unsent);
			for (int i = 0; i < num_unsent; i++)
				_data[i] = send_data[sent+i];
			send_data = _data;
		}
	}

	/**
	 * Creates an iterator for reading stream data elements.
	 *
	 * <p>See {@link #iterator(boolean)}. Creates an iterator which closes the
	 * stream when end-of-stream is reached.</p>
	 *
	 * @return iterator
	 */
	public Iterator<T> iterator() {
		return this.iterator(true);
	}

	/**
	 * Creates an iterator for reading stream data elements.
	 *
	 * <p>The stream must be set up with end-of-stream policy set to 'True'
	 * so the iterator can detect end-of-stream. The stream must also
	 * be readable.</p>
	 *
	 * <p>The iterator reads elements one element at at time until
	 * end-of-stream is reached. Read-ahead should be enabled on the stream
	 * (otherwise latency of single-element read will significantly
	 * affect performance).</p>
	 *
	 * <p>The iterator assumes it has complete control of stream I/O, and no other
	 * methods should be called on this stream object until the iterator has
	 * completed or is no longer used.</p>
	 *
	 * <p>If the iterator experiences stream error problems, its hasNext or next
	 * methods throw IllegalStateException.</p>
	 *
	 * @param closeAfter if true closes stream when no more data
	 * @return iterator
	 */
	public Iterator<T> iterator(boolean closeAfter) {
		class Iter implements Iterator<T> {
			boolean closeAfter;
			boolean done = false;
			boolean has_next = false;
			T next_item = null;
			public Iter(boolean closeAfter) {
				this.closeAfter = closeAfter;
			}
			@Override
			public synchronized boolean hasNext()
				throws IllegalStateException {
				if (done)
					return false;
				if (!has_next) {
					try {
						T[] data = recv(1);
						if (data.length > 0) {
							has_next = true;
							next_item = data[0];
						}
						else {
							done = true;
							if (closeAfter)
								close();
						}
					} catch (VStreamError e) {
						throw new IllegalStateException();
					}
				}
				return has_next;
			}
			@Override
			public synchronized T next()
					throws IllegalStateException {
				if (!this.hasNext())
					throw new NoSuchElementException();
				T result = next_item;
				has_next = false;
				next_item = null;
				return result;
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
		return new Iter(closeAfter);
	}
}
