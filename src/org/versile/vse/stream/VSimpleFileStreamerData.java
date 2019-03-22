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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;

import org.versile.vse.stream.VStream.PosBase;


/**
 * Streamer data interface to a file.
 *
 * <p>This class is 'simple' in the sense it requires exclusive streamer data
 * control of the file. It is assumed that the streamer data object has full
 * control of the file, and no other process or code is modifying file content.
 * This also implies multiple streamer data objects can operate on the same file
 * if they are all set up in read-only mode, however if streamer data is
 * writable then only one single streamer data object is allowed on any single
 * file.</p>
 *
 * <p>The streamer data does not allow seeking past the current end-of-file.
 * However, for a writable file it allows moving the end-point by writing past
 * the current end-point.</p>
 */
public class VSimpleFileStreamerData extends VStreamerData<Byte> {

	RandomAccessFile file;
	boolean readable;
	boolean writable;
	VStreamMode req_mode;
	VStreamMode req_mask;
	VStreamMode opt_mode;
	long pos;
	byte[] buffer;
	boolean failed = false;


	/**
	 * Set up streamer data which access a file.
	 *
	 * @param file file accessed by streamer data
	 * @param config file access configuration
	 * @throws VStreamError
	 */
	public VSimpleFileStreamerData(RandomAccessFile file, Config config)
			throws VStreamError {

		this.file = file;
		this.readable = config.isReadable();
		this.writable = config.isWritable();
		buffer = new byte[config.getBufLen()];

		req_mode = new VStreamMode();
		req_mode.setStartBounded(true);
		req_mode.setEndBounded(true);

		VStreamMode _req_none = new VStreamMode();
		_req_none.setStartCanInc(true);
		_req_none.setStartCanDec(true);
		_req_none.setCanMoveStart(true);

		opt_mode = new VStreamMode();
		opt_mode.setDataLock(true);
		opt_mode.setStartLock(true);
		opt_mode.setEndLock(true);

		if (readable)
			opt_mode.setReadable(true);
		else
			_req_none.setReadable(true);

		VStreamMode _w_set;
		if (writable)
			_w_set = opt_mode;
		else
			_w_set = _req_none;
		_w_set.setWritable(true);
		_w_set.setEndCanDec(true);
		_w_set.setEndCanInc(true);
		_w_set.setCanMoveEnd(true);

		if (config.isCanSeekRew())
			opt_mode.setSeekRew(true);
		else
			_req_none.setSeekRew(true);

		if (config.isCanSeekFwd())
			opt_mode.setSeekFwd(true);
		else
			_req_none.setSeekFwd(true);

		req_mask = req_mode.orWith(_req_none);

		try {
			pos = file.getFilePointer();
		} catch (IOException e) {
			throw new VStreamError(e);
		}
	}

	@Override
	public synchronized Byte[] read(int maxNum) throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();

		if (!opt_mode.readable()) {
			throw new VStreamError("Stream not readable");
		}

		LinkedList<byte[]> chunks = new LinkedList<byte[]>();
		int tot_read = 0;
		while (maxNum > 0) {
			int _max_read = maxNum;
			if (_max_read > buffer.length)
				_max_read = buffer.length;
			int num_read;
			try {
				num_read = file.read(buffer, 0, _max_read);
			} catch (IOException e) {
				failed = true;
				file = null;
				buffer = null;
				throw new VStreamFailure(e);
			}
			if (num_read < 0)
				break;
			byte[] chunk = new byte[num_read];
			for (int i = 0; i < num_read; i++)
				chunk[i] = buffer[i];
			chunks.addLast(chunk);
			maxNum -= num_read;
			tot_read += num_read;
		}
		Byte[] result = new Byte[tot_read];
		int _spos = 0;
		while (!chunks.isEmpty()) {
			byte[] chunk = chunks.removeFirst();
			for (int i = 0; i < chunk.length; i++)
				result[_spos+i] = chunk[i];
			_spos += chunk.length;
		}
		pos += result.length;
		return result;
	}

	@Override
	public synchronized void write(Byte[] data) throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();

		if (!opt_mode.writable()) {
			throw new VStreamError("Stream not writable");
		}


		byte[] _data = new byte[data.length];
		for (int i = 0; i < data.length; i++)
			_data[i] = data[i];
		try {
			file.write(_data);
		} catch (IOException e) {
			failed = true;
			file = null;
			buffer = null;
			throw new VStreamFailure(e);
		}
		pos += _data.length;
	}

	@Override
	public synchronized long seek(long pos, PosBase posRef)
				throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();

		long target_pos = 0;
		long file_len;
		try {
			file_len = file.length();
		} catch (IOException e) {
			failed = true;
			file = null;
			buffer = null;
			throw new VStreamFailure(e);
		}

		if (posRef == VStream.PosBase.ABS || posRef == VStream.PosBase.START)
			target_pos = pos;
		else if (posRef == VStream.PosBase.END)
			target_pos = file_len + pos;
		else
			target_pos = this.pos + pos;

		if (target_pos < 0)
			throw new VStreamError("Cannot seek to negative position");
		else if (target_pos > file_len)
			throw new VStreamError("Cannot seek past end of file");
		if (target_pos > this.pos && !opt_mode.seekFwd())
			throw new VStreamError("Streamer data does not allow forward seek");
		if (target_pos < this.pos && !opt_mode.seekRew())
			throw new VStreamError("Streamer data does not allow rewind seek");

		try {
			file.seek(target_pos);
		} catch (IOException e) {
			throw new VStreamError("Cannot seek to requested position");
		}
		this.pos = target_pos;
		return target_pos;
	}

	@Override
	public void truncateBefore() throws VStreamError, VStreamFailure {
		// Illegal operation
		failed = true;
		file = null;
		buffer = null;
		throw new VStreamFailure("Illegal operation");
	}

	@Override
	public synchronized void truncateAfter() throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();
		try {
			file.getChannel().truncate(pos);
		} catch (IOException e) {
			failed = true;
			file = null;
			buffer = null;
			throw new VStreamFailure(e);
		}
	}

	@Override
	public void close() {
		try {
			file.close();
		} catch (IOException e) {
			failed = true;
		}
		file = null;
		buffer = null;
	}

	@Override
	public long getPosition() throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();
		try {
			return file.getFilePointer();
		} catch (IOException e) {
			failed = true;
			file = null;
			buffer = null;
			throw new VStreamFailure(e);
		}
	}

	@Override
	public org.versile.vse.stream.VStreamerData.Endpoint[] getEndpoints()
			throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();
		Endpoint[] result = new Endpoint[2];
		result[0] = new Endpoint(true, 0);
		try {
			result[1] = new Endpoint(true, file.length());
		} catch (IOException e) {
			failed = true;
			file = null;
			buffer = null;
			throw new VStreamFailure(e);
		}
		return result;
	}

	@Override
	public VStreamMode[] getRequiredMode() {
		return new VStreamMode[] {req_mode.clone(), req_mask.clone()};
	}

	@Override
	public VStreamMode getOptionalMode() {
		return opt_mode.clone();
	}

	void check_closed_or_failed()
			throws VStreamError, VStreamFailure {
		if (file == null)
			if (failed)
				throw new VStreamFailure("Streamer data had a failure");
			else
				throw new VStreamError("Streamer data error");
	}

	/**
	 * File streamer configuration.
	 *
	 * <p>If true, 'readable', 'writable', 'canSeekRew' and 'canSeekFwd' enable
	 * the corresponding properties on a file streamer. Default has none of
	 * the properties set.</p>
	 *
	 * <p>'bufLen' is the internal buffer length of file I/O operation.</p>
	 */
	public static class Config {
		int bufLen = 32768;
		boolean readable = false;
		boolean writable = false;
		boolean canSeekRew = false;
		boolean canSeekFwd = false;

		/**
		 * Enables all file access options.
		 */
		public void enableAll() {
			readable = true;
			writable = true;
			canSeekRew = true;
			canSeekFwd = true;
		}
		public int getBufLen() {
			return bufLen;
		}
		public void setBufLen(int bufLen) {
			this.bufLen = bufLen;
		}
		public boolean isReadable() {
			return readable;
		}
		public void setReadable(boolean readable) {
			this.readable = readable;
		}
		public boolean isWritable() {
			return writable;
		}
		public void setWritable(boolean writable) {
			this.writable = writable;
		}
		public boolean isCanSeekRew() {
			return canSeekRew;
		}
		public void setCanSeekRew(boolean canSeekRew) {
			this.canSeekRew = canSeekRew;
		}
		public boolean isCanSeekFwd() {
			return canSeekFwd;
		}
		public void setCanSeekFwd(boolean canSeekFwd) {
			this.canSeekFwd = canSeekFwd;
		}
	}
}
