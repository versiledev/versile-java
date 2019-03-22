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


/**
 * Holds stream mode settings.
 *
 * <p>The various setter/getter methods set, clear or check the modes associated with the
 * various mode flags defined on the class.</p>
 */
public class VStreamMode {

	/**
	 * Mode bit for readable stream.
	 */
	public static int READABLE = 0x0001;

	/**
	 * Mode bit for writable stream.
	 */
	public static int WRITABLE = 0x0002;

	/**
	 * Mode bit for bounded stream data start position.
	 */
	public static int START_BOUNDED = 0x0004;

	/**
	 * Mode bit indicating stream data start position may decrease.
	 */
	public static int START_CAN_DEC = 0x0008;

	/**
	 * Mode bit indicating stream data start position may increase.
	 */
	public static int START_CAN_INC = 0x0010;

	/**
	 * Mode bit indicating stream is allowed to move start position.
	 */
	public static int CAN_MOVE_START = 0x0020;

	/**
	 * Mode bit for bounded stream data end position.
	 */
	public static int END_BOUNDED = 0x0040;

	/**
	 * Mode bit indicating stream data end position may decrease.
	 */
	public static int END_CAN_DEC = 0x0080;

	/**
	 * Mode bit indicating stream data end position may increase.
	 */
	public static int END_CAN_INC = 0x0100;

	/**
	 * Mode bit indicating stream is allowed to move end position.
	 */
	public static int CAN_MOVE_END = 0x0200;

	/**
	 * Mode bit indicating reverse-seeking is allowed.
	 */
	public static int SEEK_REW = 0x0400;

	/**
	 * Mode bit indicating forward-seeking is allowed.
	 */
	public static int SEEK_FWD = 0x0800;

	/**
	 * Mode bit indicating stream data cannot be modified by any source.
	 */
	public static int FIXED_DATA = 0x1000;

	/**
	 * Mode bit indicating stream data can only be modified by this source.
	 */
	public static int DATA_LOCK = 0x02000;

	/**
	 * Mode bit indicating start pos can only be modified by this streamer.
	 */
	public static int START_LOCK = 0x4000;

	/**
	 * Mode bit indicating end pos can only be modified by this streamer.
	 */
	public static int END_LOCK = 0x8000;

	/**
	 * Mask for all the mode bits used for tracked modes.
	 */
	public static int MASK = 0xffff;

	int mode;

	/**
	 * Set up with all mode flags disabled.
	 */
	public VStreamMode() {
		mode = 0;
	}

	/**
	 * Set up with the provided mode flags.
	 *
	 * @param mode bitwise or of mode flags
	 */
	public VStreamMode(int mode) {
		this.mode = mode & 0xffff;
	}

	public synchronized boolean readable() {
		return getFlag(READABLE);
	}

	public synchronized void setReadable(boolean state) {
		setFlag(READABLE, state);
	}

	public synchronized boolean writable() {
		return getFlag(WRITABLE);
	}

	public synchronized void setWritable(boolean state) {
		setFlag(WRITABLE, state);
	}

	public synchronized boolean startBounded() {
		return getFlag(START_BOUNDED);
	}

	public synchronized void setStartBounded(boolean state) {
		setFlag(START_BOUNDED, state);
	}

	public synchronized boolean startCanDec() {
		return getFlag(START_CAN_DEC);
	}

	public synchronized void setStartCanDec(boolean state) {
		setFlag(START_CAN_DEC, state);
	}

	public synchronized boolean startCanInc() {
		return getFlag(START_CAN_INC);
	}

	public synchronized void setStartCanInc(boolean state) {
		setFlag(START_CAN_INC, state);
	}

	public synchronized boolean canMoveStart() {
		return getFlag(CAN_MOVE_START);
	}

	public synchronized void setCanMoveStart(boolean state) {
		setFlag(CAN_MOVE_START, state);
	}

	public synchronized boolean endBounded() {
		return getFlag(END_BOUNDED);
	}

	public synchronized void setEndBounded(boolean state) {
		setFlag(END_BOUNDED, state);
	}

	public synchronized boolean endCanDec() {
		return getFlag(END_CAN_DEC);
	}

	public synchronized void setEndCanDec(boolean state) {
		setFlag(END_CAN_DEC, state);
	}

	public synchronized boolean endCanInc() {
		return getFlag(END_CAN_INC);
	}

	public synchronized void setEndCanInc(boolean state) {
		setFlag(END_CAN_INC, state);
	}

	public synchronized boolean canMoveEnd() {
		return getFlag(CAN_MOVE_END);
	}

	public synchronized void setCanMoveEnd(boolean state) {
		setFlag(CAN_MOVE_END, state);
	}

	public synchronized boolean seekRew() {
		return getFlag(SEEK_REW);
	}

	public synchronized void setSeekRew(boolean state) {
		setFlag(SEEK_REW, state);
	}

	public synchronized boolean seekFwd() {
		return getFlag(SEEK_FWD);
	}

	public synchronized void setSeekFwd(boolean state) {
		setFlag(SEEK_FWD, state);
	}

	public synchronized boolean fixedData() {
		return getFlag(FIXED_DATA);
	}

	public synchronized void setFixedData(boolean state) {
		setFlag(FIXED_DATA, state);
	}

	public synchronized boolean dataLock() {
		return getFlag(DATA_LOCK);
	}

	public synchronized void setDataLock(boolean state) {
		setFlag(DATA_LOCK, state);
	}

	public synchronized boolean startLock() {
		return getFlag(START_LOCK);
	}

	public synchronized void setStartLock(boolean state) {
		setFlag(START_LOCK, state);
	}

	public synchronized boolean endLock() {
		return getFlag(END_LOCK);
	}

	public synchronized void setEndLock(boolean state) {
		setFlag(END_LOCK, state);
	}

	/**
	 * Get bitwise or of mode flags for set modes.
	 *
	 * @return mode flags
	 */
	public synchronized int getFlags() {
		return mode;
	}

	/**
	 * Generate a mode object with all modes of both mode objects.
	 *
	 * @param other mode object to or with
	 * @return mode object with resulting mode
	 */
	public synchronized VStreamMode orWith(VStreamMode other) {
		int _mode = this.mode;
		_mode |= other.mode;
		return new VStreamMode(_mode);
	}

	/**
	 * Perform AND on modes.
	 *
	 * <p>Generate a mode object with only modes set on both mode objects.</p>
	 *
	 * @param other mode object to and with
	 * @return mode object with resulting mode
	 */
	public synchronized VStreamMode andWith(VStreamMode other) {
		int _mode = this.mode;
		_mode &= other.mode;
		return new VStreamMode(_mode);
	}

	/**
	 * Perform XOR on modes.
	 *
	 * <p>Generate a mode object with only modes set on one (but not both) both objects.</p>
	 *
	 * @param other mode object to xor with
	 * @return mode object with resulting mode
	 */
	public synchronized VStreamMode xorWith(VStreamMode other) {
		int _mode = this.mode;
		_mode ^= other.mode;
		return new VStreamMode(_mode);
	}

	/**
	 * Subtract modes.
	 *
	 * <p>Generate a mode object with modes on this object minus the modes set on 'other'.</p>
	 *
	 * @param other mode object whose set flags to clear on this mode
	 * @return mode object with resulting mode
	 */
	public synchronized VStreamMode subtract(VStreamMode other) {
		int _mode  = this.mode;
		int _o_mode = other.mode;
		_mode |= _o_mode;
		_mode ^= _o_mode;
		return new VStreamMode(_mode);
	}

	/**
	 * Checks whether all modes enabled by other object are set on this object.
	 *
	 * @param other other mode object
	 * @return true if all enabled 'other' modes are enabled on this object
	 */
	public synchronized boolean contains(VStreamMode other) {
		return ((mode | other.mode) == mode);
	}

	/**
	 * Generate a mode object with all modes of this object switched.
	 *
	 * @return inverse mode object with all modes switched.
	 */
	public synchronized VStreamMode inverse() {
		return new VStreamMode (mode ^ MASK);
	}

	/**
	 * Checks whether mode object has any modes enabled.
	 *
	 * @return true if any modes are enabled
	 */
	public synchronized boolean isEmpty() {
		return (mode == 0);
	}

	/**
	 * Checks whether a bitwise OR of stream mode bits is a valid combination.
	 *
	 * @param mode bitwise OR of mode bits to check
	 * @return true if valid
	 */
	public static boolean validateMode(int mode) {
		if ((mode & (READABLE | WRITABLE))== 0)
			return false; // Must be readable or writable
		if ((mode & WRITABLE) != 0 && (mode & FIXED_DATA) != 0)
			return false; // Constant stream data cannot be writable
		if ((mode & CAN_MOVE_START) != 0 && (mode & (START_CAN_DEC | START_CAN_INC)) == 0)
			return false; // Invalid start position move flag combination
		if ((mode & CAN_MOVE_END) != 0 && (mode & (END_CAN_DEC | END_CAN_INC)) == 0)
			return false; // Invalid end position move flag combination
		return true;
	}

	/**
	 * Checks whether mode settings is a valid combination.
	 *
	 * @return true if valid
	 */
	public synchronized boolean validate() {
		return VStreamMode.validateMode(this.mode);
	}

	@Override
	public synchronized VStreamMode clone() {
		return new VStreamMode(this.mode);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VStreamMode)
			return (((VStreamMode)obj).mode == mode);
		else
			return false;
	}

	@Override
	public int hashCode() {
		return ((Integer)mode).hashCode();
	}

	@Override
	public String toString() {
		return Integer.toHexString(mode);
	}

	boolean getFlag(int flag) {
		return ((mode & flag) != 0);
	}

	void setFlag(int flag, boolean state) {
		mode |= flag;
		if (!state)
			mode ^= flag;
	}
}
