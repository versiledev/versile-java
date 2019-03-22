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

import java.util.LinkedList;


/**
 * Stream proxy for streams transferring byte[] data.
 */
public class VByteStream extends VStream<Byte> {

    /**
     * Set up stream proxy.
     *
     * @param stream stream peer to proxy
     */
    public VByteStream(VStreamPeer<Byte> stream) {
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
	public byte[] recv(int maxNum)
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
	public byte[] recv(int maxNum, long timeout)
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
	public byte[] recv(int maxNum, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		Byte[] data = _stream.recv(maxNum, timeout, ntimeout);
		byte[] result = new byte[data.length];
		for (int i = 0; i < data.length; i++)
			result[i] = data[i];
		return result;
	}

	/**
	 * Reads stream data from the current read context until end-of-stream.
	 *
	 * <p>See {@link #read(int)}.</p>
	 *
	 * @return data read
	 */
	public byte[] read()
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
	public byte[] read(int num)
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
	public byte[] read(int num, int blockSize)
			throws VStreamError {
		LinkedList<byte[]> chunks = new LinkedList<byte[]>();
		int num_read = 0;
		int recv_lim;

		while (num < 0 || num_read < num) {
			if (num < 0)
				recv_lim = blockSize;
			else
				recv_lim = num - num_read;
			byte[] data = this.recv(recv_lim);
			if (data.length == 0)
				break;
			chunks.addLast(data);
			num_read += data.length;
		}

		byte[] result = new byte[num_read];
		int pos = 0;
		for (byte[] chunk: chunks) {
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
	public int send(byte[] data)
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
	 * <p>See {@link #send(byte[])}. Raises {@link VStreamTimeout}
	 * if no data could be written before timeout expired.</p>
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 */
	public int send(byte[] data, long timeout)
			throws VStreamTimeout, VStreamError {
		return this.send(data, timeout, 0);
	}

	/**
	 * Writes data to a current active stream write context.
	 *
	 * <p>See {@link #send(byte[])}. Raises {@link VStreamTimeout}
	 * if no data could be written before timeout expired.</p>
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @param ntimeout additional timeout in nanoseconds
	 */
	public int send(byte[] data, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		Byte[] send_data = new Byte[data.length];
		for (int i = 0; i < data.length; i++)
			send_data[i] = data[i];
		return _stream.send(send_data, timeout, ntimeout);
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
	public void write(byte[] data)
			throws VStreamError {
		int num_unsent = data.length;
		byte[] send_data = data;
		while(true) {
			int sent = this.send(send_data);
			num_unsent -= sent;
			if (num_unsent == 0)
				break;
			byte[] _data = new byte[num_unsent];
			for (int i = 0; i < num_unsent; i++)
				_data[i] = send_data[sent+i];
			send_data = _data;
		}
	}
}
