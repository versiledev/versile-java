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

package org.versile.common.util;

import java.io.UnsupportedEncodingException;

import org.versile.orb.entity.VInteger;



/**
 * Implements Base64 encoding and decoding (RFC 2045).
 */
public class VBase64 {

	static byte[] CHARS;
	static int[] INVERSE = new int[0x100];
    static {
    	try {
			CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			throw new RuntimeException(e);
		}
    	for (int i = 0; i < INVERSE.length; i++)
    		INVERSE[i] = -1;
    	for(int i=0; i< CHARS.length; i++){
            INVERSE[CHARS[i]]= i;
        }
    }

	/**
	 * Encodes binary data as Base64.
	 *
	 * @param data data to encode
	 * @return Base64-encoded data
	 */
	public static byte[] encode(byte[] data) {
		VByteBuffer buf = new VByteBuffer(data);
		VByteBuffer out_buf = new VByteBuffer();
		int padding = data.length % 3;
		if (padding > 0)
			padding = 3 - padding;
		for (int i = 0; i < padding; i++)
			buf.append((byte)0x00);
		int rounds = 0;
		byte[] enc = new byte[4];
		while(buf.hasData()) {
			rounds += 1;
			int triplet = VInteger.bytes_to_posint(buf.pop(3)).intValue();
			enc[0] = CHARS[(triplet >>> 18) & 0x3f];
			enc[1] = CHARS[(triplet >>> 12) & 0x3f];
			enc[2] = CHARS[(triplet >>> 6) & 0x3f];
			enc[3] = CHARS[(triplet & 0x3f)];
			out_buf.append(enc);
			if (rounds == 19) {
				rounds = 0;
				out_buf.append(new byte[] {(byte)0x0d, (byte)0x0a});
			}
		}
		if (rounds != 0)
			out_buf.append(new byte[] {(byte)0x0d, (byte)0x0a});
		byte[] result = out_buf.popAll();
		if (padding > 0)
			result[result.length-3] = (byte)0x3d;
		if (padding > 1)
			result[result.length-4] = (byte)0x3d;
		return result;
	}

	/**
	 * Encodes binary data as Base64.
	 *
	 * @param data data to encode
	 * @return Base64-encoded data
	 */
	public static String encodeString(byte[] data) {
		try {
			return new String(VBase64.encode(data), "ASCII");
		} catch (UnsupportedEncodingException e) {
			// should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Decodes Base64-encoded binary data.
	 *
	 * @param data Base64-encoded data
	 * @return decoded binary data
	 * @throws VBase64Exception invalid encoded data
	 */
	public static byte[] decode(byte[] data)
			throws VBase64Exception {
		VByteBuffer buf = new VByteBuffer();
		int num_on_line = 0;
		boolean had_termination = false;
		for (byte b: data) {
			if (INVERSE[b & 0xff] >= 0) {
				if (had_termination)
					throw new VBase64Exception("Termination character followed by data");
				num_on_line += 1;
				if (num_on_line > 76)
					throw new VBase64Exception("Too many characters on line (RFC2045 allows max 76)");
				buf.append(b);
			}
			else if (b == (byte)0x3d) {
				had_termination = true;
				buf.append(b);
			}
			else if (b == (byte)0x0a)
				num_on_line = 0;
			else {
				// Do nothing, discarded
			}

		}
		if (buf.length() % 4 > 0)
			throw new VBase64Exception("Invalid length of Base64 input data");
		data = buf.popAll();
		if (data.length == 0)
			return new byte[0];
		int padding = 0;
		if (data[data.length-1] == (byte)0x3d) {
			data[data.length-1] = (byte)0x41;
			padding = 1;
		}
		if (data[data.length-2] == (byte)0x3d) {
			data[data.length-2] = (byte)0x41;
			padding = 2;
		}

		buf.append(data);
		VByteBuffer out_buf = new VByteBuffer();
		int[] as_int = new int[4];
		byte[] _bdata = new byte[3];
		while(buf.hasData()) {
			byte[] element = buf.pop(4);
			for (int i = 0; i < 4; i++) {
				int num = INVERSE[element[i] & 0xff];
				if (num < 0)
					throw new VBase64Exception("Illegal Base64 character");
				as_int[i] = num;
			}
			int val = 0;
			val = (as_int[0] << 18)  & 0xfc0000;
			val |= (as_int[1] << 12) & 0x03f000;
			val |= (as_int[2] << 6)  & 0x000fc0;
			val |= as_int[3]         & 0x00003f;
			_bdata[0] = (byte)((val >>> 16) & 0xff);
			_bdata[1] = (byte)((val >>> 8) & 0xff);
			_bdata[2] = (byte)(val & 0xff);
			out_buf.append(_bdata);
		}
		return out_buf.pop(out_buf.length()-padding);
	}

	/**
	 * Decodes Base64-encoded binary data.
	 *
	 * @param data Base64-encoded data
	 * @return decoded binary data
	 * @throws VBase64Exception invalid encoded data
	 */
	public static byte[] decode(String data)
			throws VBase64Exception {
		try {
			return VBase64.decode(data.getBytes("ASCII"));
		} catch (UnsupportedEncodingException e) {
			throw new VBase64Exception(e);
		}
	}

	/**
	 * Base64-encodes data and encapsulates within a BEGIN/END block.
	 *
	 * @param name block name for BEGIN/END lines
	 * @param data data to encode inside block
	 * @return encoded block
	 * @throws VBase64Exception encoder error (cannot encode block name)
	 */
	public static byte[] encodeBlock(String name, byte[] data)
			throws VBase64Exception {
		try {
			VByteBuffer buf = new VByteBuffer();
			buf.append(("-----BEGIN " + name + "-----").getBytes("ASCII"));
			buf.append(new byte[] {(byte)0x0d, (byte)0x0a});
			buf.append(VBase64.encode(data));
			buf.append(("-----END " + name + "-----").getBytes("ASCII"));
			buf.append(new byte[] {(byte)0x0d, (byte)0x0a});
			return buf.popAll();
		} catch (UnsupportedEncodingException e) {
			throw new VBase64Exception(e);
		}
	}

	/**
	 * Base64-encodes data and encapsulates within a BEGIN/END block.
	 *
	 * @param name block name for BEGIN/END lines
	 * @param data data to encode inside block
	 * @return encoded block
	 * @throws VBase64Exception encoder error (cannot encode block name)
	 */
	public static String encodeBlockString(String name, byte[] data)
			throws VBase64Exception {
		try {
			return new String(VBase64.encodeBlock(name, data), "ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new VBase64Exception(e);
		}
	}

	/**
	 * Decodes a block of base64 data with BEGIN/END delimiters.
	 *
	 * <p>Must start with "----BEGIN [name]-----" on first line and similar
	 * last line with "END" instead of "BEGIN".</p>
	 *
	 * <p>Strips the input data of leading and trailing whitespace before parsing.</p>
	 *
	 * @param block block to decode
	 * @return decoded data
	 * @throws VBase64Exception data format error
	 */
	public static BlockData decodeBlock(byte[] block)
			throws VBase64Exception {
		String s_block;
		try {
			s_block = new String(block, "ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new VBase64Exception(e);
		}
		return VBase64.decodeBlock(s_block);
	}

	/**
	 * Decodes a block of base64 data with BEGIN/END delimiters.
	 *
	 * <p>Must start with "----BEGIN [name]-----" on first line and similar
	 * last line with "END" instead of "BEGIN".</p>
	 *
	 * <p>Strips the input data of leading and trailing whitespace before parsing.</p>
	 *
	 * @param blockName required block name
	 * @param block block to decode
	 * @return decoded data
	 * @throws VBase64Exception data format error or mismatching block name
	 */
	public static byte[] decodeBlock(String blockName, byte[] block)
			throws VBase64Exception {
		String s_block;
		try {
			s_block = new String(block, "ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new VBase64Exception(e);
		}
		BlockData dec = VBase64.decodeBlock(s_block);
		if (!dec.getBlockName().equals(blockName))
			throw new VBase64Exception("Block name mismatch");
		return dec.getData();
	}

	/**
	 * Decodes a block of base64 data with BEGIN/END delimiters.
	 *
	 * <p>Must start with "----BEGIN [name]-----" on first line and similar
	 * last line with "END" instead of "BEGIN".</p>
	 *
	 * <p>Strips the input data of leading and trailing whitespace before parsing.</p>
	 *
	 * @param blockName required block name
	 * @param block block to decode
	 * @return decoded data
	 * @throws VBase64Exception data format error or mismatching block name
	 */
	public static byte[] decodeBlock(String blockName, String block)
			throws VBase64Exception {
		BlockData dec = VBase64.decodeBlock(block);
		if (!dec.getBlockName().equals(blockName))
			throw new VBase64Exception("Block name mismatch");
		return dec.getData();
	}

	/**
	 * Decodes a block of base64 data with BEGIN/END delimiters.
	 *
	 * <p>Must start with "----BEGIN [name]-----" on first line and similar
	 * last line with "END" instead of "BEGIN".</p>
	 *
	 * <p>Strips the input data of leading and trailing whitespace before parsing.</p>
	 *
	 * @param block block to decode
	 * @return decoded data
	 * @throws VBase64Exception data format error
	 */
	public static BlockData decodeBlock(String block)
			throws VBase64Exception {
		block = block.trim();
		String[] lines = block.split("\n");
		if (lines.length < 2)
			throw new VBase64Exception("Invalid data");

		String first_line = lines[0].trim();
		if (!(first_line.startsWith("-----BEGIN ")))
			throw new VBase64Exception("Invalid data");
		if (!first_line.endsWith("-----"))
			throw new VBase64Exception("Invalid data");
		String name = first_line.substring(11, first_line.length()-5);

		String last_line = lines[lines.length-1].trim();
		if (!last_line.equals("-----END " + name + "-----"));

		String base64_data;
		if (lines.length == 2)
			base64_data = "";
		else {
			base64_data = lines[1];
			for (int i = 2; i < lines.length-1; i++)
				base64_data += "\n" + lines[i];
		}
		byte[] decoded_data = VBase64.decode(base64_data);

		return new BlockData(name, decoded_data);
	}

	/**
	 * Result of a block decode operation.
	 *
	 * <p>Holds the result of {@link VBase64#decodeBlock(byte[])} and similar operations for
	 * parsing a base64 block with BEGIN/END header information.</p>
	 */
	public static class BlockData {
		String blockName;
		byte[] data;
		public BlockData(String blockName, byte[] data) {
			this.blockName = blockName;
			this.data = data;
		}
		/**
		 * Get the name set on the block.
		 *
		 * @return block name
		 */
		public String getBlockName() {
			return blockName;
		}
		/**
		 * Get the data contained in the block.
		 *
		 * @return decoded base64 data
		 */
		public byte[] getData() {
			return data;
		}
	}

	/**
	 * Exception in Bas64 decode operation.
	 */
	public static class VBase64Exception extends Exception {
		private static final long serialVersionUID = 6548014368835635910L;
		public VBase64Exception() {
			super();
		}
		public VBase64Exception(String message, Throwable cause) {
			super(message, cause);
		}
		public VBase64Exception(String message) {
			super(message);
		}
		public VBase64Exception(Throwable cause) {
			super(cause);
		}
	}
}
