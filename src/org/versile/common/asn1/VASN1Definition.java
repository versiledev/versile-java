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

package org.versile.common.asn1;

import org.versile.common.asn1.VASN1Tag.Decoded;
import org.versile.orb.entity.VInteger;



/**
 * Definition of an ASN.1 data type.
 */
public abstract class VASN1Definition {

	/**
	 * Definition name (or null).
	 */
	protected String name = null;

	/**
	 * Set up definition without name.
	 */
	public VASN1Definition() {

	}

	/**
	 * Set up definition.
	 *
	 * @param name definition name
	 */
	public VASN1Definition(String name) {
		this.name = name;

	}

	/**
	 * Parses DER data for this definition and returns value.
	 *
	 * <p>Parses representation which includes identifier octets.</p>
	 *
	 * @param data DER encoded data to parse
	 * @return parse result structure
	 * @throws VASN1Exception parse error
	 */
	public ParseResult parseDER(byte[] data)
			throws VASN1Exception {
		return this.parseDER(data, true);
	}

	/**
	 * Parses DER data for this definition and returns value.
	 *
	 * @param data DER encoded data to parse
	 * @param withTag if true DER data includes identifier octets
	 * @return parse result structure
	 * @throws VASN1Exception parse error
	 */
	public abstract ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception;

	/**
	 * Get a tag for this type definition.
	 *
	 * @return tag for type definition (or null)
	 */
	public VASN1Tag getTag() {
		return null;
	}

	/**
	 * Get the type name of this ASN.1 type
	 *
	 * @return type name (or null)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Decode BER length.
	 *
	 * <p>Returns a decoded structure if data holds a definitive length
	 * structure. Returns null if indefinite length encoding was used, in which
	 * case the number of bytes read is one.</p>
	 *
	 * @param data length encoding bytes
	 * @return decoded structure for definite encoding, or null
	 * @throws VASN1Exception incomplete data or decoding error
	 */
	public static DecodedLength berDecLength(byte[] data)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("Incomplete data");
		int first = data[0] & 0xff;
		if (first <= 0x7f)
			return new DecodedLength(new VInteger(first), true, 1);
		else if (first == 0x80)
			return null;
		else {
			int num_bytes = first & 0x7f;
			if (data.length < num_bytes + 1)
				throw new VASN1Exception("Incomplete data");
			byte[] _data = new byte[num_bytes];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			return new DecodedLength(new VInteger(VInteger.bytes_to_posint(_data)), false, num_bytes+1);
		}
	}

	/**
	 * Decode BER-encoded content for definite-length encoding.
	 *
	 * @param data data to decode
	 * @param length content data length
	 * @return decoded data
	 * @throws VASN1Exception
	 */
	public static DecodedContent berDecContentDefinite(byte[] data, VInteger length)
			throws VASN1Exception {
		if (!(length.getValue() instanceof Integer))
			throw new VASN1Exception("Content structure too long to decode");
		int len = length.getValue().intValue();
		if (data.length < len)
			throw new VASN1Exception("Insufficient data");
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++)
			result[i] = data[i];
		return new DecodedContent(result, len);
	}

	/**
	 * Decode BER tagged content.
	 *
	 * @param data BER-encoded tagged content
	 * @return decoded content
	 * @throws VASN1Exception
	 */
	public static DecodedTaggedContent berDecTaggedContent(byte[] data)
			throws VASN1Exception {
		int total_read = 0;
		Decoded id_data = VASN1Tag.fromBER(data);
		VASN1Tag tag = id_data.getTag();
		int id_num = id_data.getNumRead();
		total_read += id_num;
		byte[] _data = new byte[data.length-id_num];
		for (int i = 0; i < data.length-id_num; i++)
			_data[i] = data[i+id_num];
		DecodedLength len_data = VASN1Definition.berDecLength(_data);
		DecodedContent content_data;
		boolean definite = (len_data != null);
		boolean shortEncoding = false;
		if (definite) {
			shortEncoding = len_data.isShortEncoded();
			total_read += len_data.getBytesRead();
			byte[] _data2 = new byte[_data.length-len_data.getBytesRead()];
			for (int i = 0; i < _data2.length ; i++)
				_data2[i] = _data[i + len_data.getBytesRead()];
			content_data = VASN1Definition.berDecContentDefinite(_data2, len_data.getDecodedLength());
			total_read += content_data.getBytesRead();
		}
		else {
			total_read += 1;
			byte[] _data2 = new byte[_data.length-1];
			for (int i = 0; i < _data2.length; i++)
				_data2[i] = _data[i+1];
			content_data = VASN1Definition.berDecContentIndefinite(_data2);
			total_read += content_data.getBytesRead();
		}
		return new DecodedTaggedContent(tag, id_data.isConstructed(), content_data.getContent(),
									    definite, shortEncoding, total_read);
	}

	/**
	 * Creates a tagged value definition.
	 *
	 * <p>Creates tagged value definition for an associated context tag number and value definition.</p>
	 *
	 * @param tagNumber tag number for a context tag
	 * @param def value definition
	 * @param explicit if true use explicit tagged value encoding
	 * @return tagged value definition
	 */
	public static VASN1DefTagged tagWithContext(int tagNumber, VASN1Definition def, boolean explicit) {
		return new VASN1DefTagged(new VASN1Tag(VASN1Tag.TagClass.CONTEXT, tagNumber), def, explicit);
	}

	/**
	 * Holds the result of a ASN.1 DER parse operation.
	 */
	public static class ParseResult {
		VASN1Base result;
		int numRead;
		public ParseResult(VASN1Base result, int numRead) {
			this.result = result;
			this.numRead = numRead;
		}
		/**
		 * Parsed object.
		 *
		 * @return parsed object
		 */
		public VASN1Base getResult() {
			return result;
		}
		/**
		 * Number of bytes read.
		 *
		 * @return bytes read
		 */
		public int getNumRead() {
			return numRead;
		}
	}

	/**
	 * Decoded BER length data.
	 */
	public static class DecodedLength {
		VInteger decodedLength;
		boolean shortEncoded;
		int bytesRead;
		public DecodedLength(VInteger decodedLength, boolean shortEncoded, int bytesRead) {
			this.decodedLength = decodedLength;
			this.shortEncoded = shortEncoded;
			this.bytesRead = bytesRead;
		}
		/**
		 * Get decoded length.
		 *
		 * @return decoded length
		 */
		public VInteger getDecodedLength() {
			return decodedLength;
		}
		/**
		 * Check if short encoding was used.
		 *
		 * @return true if short encoding used
		 */
		public boolean isShortEncoded() {
			return shortEncoded;
		}
		/**
		 * Get number of bytes read.
		 *
		 * @return number bytes read
		 */
		public int getBytesRead() {
			return bytesRead;
		}
	}

	/**
	 * Holds decoder result of decoded DER content.
	 */
	public static class DecodedContent {
		byte[] content;
		int bytesRead;
		DecodedContent(byte[] content, int bytesRead) {
			this.content = content;
			this.bytesRead = bytesRead;
		}
		/**
		 * Get decoded content.
		 *
		 * @return decoded content
		 */
		public byte[] getContent() {
			return content;
		}
		/**
		 * Get number bytes read.
		 *
		 * @return bytes read
		 */
		public int getBytesRead() {
			return bytesRead;
		}

	}

	/**
	 * Decode BER-encoded content for indefinite-length encoding.
	 *
	 * @param data data to decode
	 * @return decoded data
	 * @throws VASN1Exception
	 */
	public static DecodedContent berDecContentIndefinite(byte[] data)
			throws VASN1Exception {
		for (int pos = 0; pos < data.length-1; pos++)
			if (data[pos] == (byte)0x00 && data[pos+1] == (byte)0x00) {
				byte[] content = new byte[pos];
				for (int i = 0; i < pos; i++)
					content[i] = data[i];
				return new DecodedContent(content, pos+2);
			}
		throw new VASN1Exception("No content delimiter");
	}

	/**
	 * Decoded BER tagged-value content.
	 */
	public static class DecodedTaggedContent {
		VASN1Tag tag;
		boolean constructedTag;
		byte[] content;
		boolean definiteLength;
		boolean shortEncoded;
		int numRead;
		public DecodedTaggedContent(VASN1Tag tag, boolean constructedTag, byte[] content,
								    boolean definiteLength, boolean shortEncoded, int numRead) {
			this.tag = tag;
			this.constructedTag = constructedTag;
			this.content = content;
			this.definiteLength = definiteLength;
			this.shortEncoded = shortEncoded;
			this.numRead = numRead;
		}
		/**
		 * Get decoded tag.
		 *
		 * @return decoded tag
		 */
		public VASN1Tag getTag() {
			return tag;
		}
		/**
		 * Check if tag had constructed tag format.
		 *
		 * @return true if constructed tag
		 */
		public boolean isConstructedTag() {
			return constructedTag;
		}
		/**
		 * Get decoded content bytes.
		 *
		 * @return decoded content bytes
		 */
		public byte[] getContent() {
			return content;
		}
		/**
		 * Check if encoding used definitive length format.
		 *
		 * @return true if definitive length format
		 */
		public boolean isDefiniteLength() {
			return definiteLength;
		}
		/**
		 * Check if encoding length used short encoding format.
		 *
		 * <p>Only has meaning if definitive length format is used.</p>
		 *
		 * @return true if short encoding
		 */
		public boolean isShortEncoded() {
			return shortEncoded;
		}
		/**
		 * Get number of decoded bytes.
		 *
		 * @return number of decoded bytes
		 */
		public int getNumRead() {
			return numRead;
		}
	}
}
