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

import java.math.BigInteger;
import java.util.LinkedList;

import org.versile.orb.entity.VInteger;



/**
 * An ASN.1 tag.
 */
public class VASN1Tag {

	/**
	 * ASN.1 tag class.
	 */
	public enum TagClass {
		/**
		 * Universal tag.
		 */
		UNIVERSAL,
		/**
		 * Application tag.
		 */
		APPLICATION,
		/**
		 * Context tag.
		 */
		CONTEXT,
		/**
		 * Private tag.
		 */
		PRIVATE
		};

	TagClass tag_cls;
	VInteger tag_number;

	/**
	 * Set up tag.
	 *
	 * @param tagClass tag class
	 * @param tagNumber tag number
	 * @throws VASN1Exception
	 */
	public VASN1Tag(TagClass tagClass, int tagNumber) {
		tag_cls = tagClass;
		tag_number = new VInteger(tagNumber);
	}

	/**
	 * Set up tag.
	 *
	 * @param tagClass tag class
	 * @param tagNumber tag number
	 * @throws VASN1Exception
	 */
	public VASN1Tag(TagClass tagClass, VInteger tagNumber) {
		tag_cls = tagClass;
		tag_number = tagNumber;
	}

	/**
	 * Generate a context-type tag.
	 *
	 * @param tagNumber tag number
	 * @return tag
	 */
	public static VASN1Tag contextTag(VInteger tagNumber) {
		return new VASN1Tag(TagClass.CONTEXT, tagNumber);
	}

	/**
	 * Get tag class of the tag.
	 *
	 * @return tag class
	 */
	public TagClass getTagClass() {
		return tag_cls;
	}


	/**
	 * Get tag number of the tag.
	 *
	 * @return tag number
	 */
	public VInteger getTagNumber() {
		return tag_number;
	}

	/**
	 * Encodes BER identifier octets for the tag.
	 *
	 * @param constructed if true use constructed mode
	 * @return identifier octets
	 */
	public byte[] encodeBER(boolean constructed) {
		return this.encodeDER(constructed);
	}

	/**
	 * Encodes DER identifier octets for the tag.
	 *
	 * @param constructed if true use constructed mode
	 * @return identifier octets
	 */
	public byte[] encodeDER(boolean constructed) {
		byte first = (byte)(VASN1Tag.tagClassToInt(tag_cls) & 0xff);
		if (constructed)
			first |= (byte)0x20;
		if (tag_number.getBigIntegerValue().compareTo(BigInteger.valueOf(31)) < 0) {
			first |= (byte)(tag_number.getValue().intValue() & 0xff);
			return VInteger.posint_to_bytes(first & 0xff);
		}
		else {
			first |= (byte)0x1f;
			LinkedList<Byte> buf = new LinkedList<Byte>();
			byte[] data = VInteger.posint_to_bytes(tag_number.getValue());
			String bits = "";
			for (int i = 0; i < data.length; i++) {
				String add_bits = Integer.toBinaryString(data[i] & 0xff);
				while (add_bits.length() < 8)
					add_bits = "0" + add_bits;
				bits += add_bits;
			}
			int first_bit = bits.indexOf("1");
			if (first_bit >= 0)
				bits = bits.substring(first_bit);
			else
				bits = "";
			while (!bits.isEmpty()) {
				int delim = bits.length() - 7;
				if (delim < 0)
					delim = 0;
				String nxt = "0" + bits.substring(delim);
				buf.addFirst((byte)(Integer.valueOf(nxt, 2) | 0x80));
				bits = bits.substring(0, delim);
			}
			byte last_byte = buf.removeLast();
			buf.addLast((byte)(last_byte & 0x7f));
			buf.addFirst(first);
			byte[] result = new byte[buf.size()];
			for (int i = 0; i < result.length; i++)
				result[i] = buf.removeFirst();
			return result;
		}
	}

	/**
	 * Decodes a tag from BER encoded octet identifier data.
	 *
	 * @param data BER encoded octet identifier
	 * @return decoded data
	 * @throws VASN1Exception incomplete data
	 */
	public static Decoded fromBER(byte[] data)
			throws VASN1Exception {
		return VASN1Tag.fromDER(data);
	}

	/**
	 * Decodes a tag from DER encoded octet identifier data.
	 *
	 * @param data DER encoded octet identifier
	 * @return decoded data
	 * @throws VASN1Exception incomplete data
	 */
	public static Decoded fromDER(byte[] data)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("Incomplete data");
		int pos = 0;
		int first = data[0] & 0xff;
		TagClass tag_cls = VASN1Tag.tagClassFromInt(first & 0xc0);
		boolean is_constructed = ((first & 0x20) != 0);
		VInteger tag_number;
		if ((first & 0x1f) < 0x1f)
			tag_number = new VInteger(first & 0x1f);
		else {
			String bin_str = "0";
			while(true) {
				pos += 1;
				if (pos >= data.length)
					throw new VASN1Exception("Incomplete data");
				int next_byte = data[pos] & 0xff;
				String add_bits = Integer.toBinaryString(next_byte & 0x7f);
				while(add_bits.length() < 7)
					add_bits = "0" + add_bits;
				bin_str += add_bits;
				if ((next_byte & 0x80) == 0)
					break;
			}
			tag_number = new VInteger(new BigInteger(bin_str, 2));
		}
		VASN1Tag tag = new VASN1Tag(tag_cls, tag_number);
		return new Decoded(tag, is_constructed, pos+1);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof VASN1Tag))
			return false;
		VASN1Tag other_tag = (VASN1Tag) other;
		return (other_tag.tag_cls == tag_cls && other_tag.tag_number.equals(tag_number));
	}

	@Override
	public int hashCode() {
		return VASN1Tag.tagClassToInt(tag_cls) ^ tag_number.hashCode();
	}

	@Override
	public String toString() {
		String tc;
		if (tag_cls == TagClass.UNIVERSAL)
			tc = "UNIVERSAL ";
		else if (tag_cls == TagClass.APPLICATION)
			tc = "APPLICATION ";
		else if (tag_cls == TagClass.PRIVATE)
			tc = "PRIVATE ";
		else
			tc = "";
		return "[ " + tc + "" + tag_number + " ]";
	}

	/**
	 * Get the tag class code associated with a tag class.
	 *
	 * @param tagClass tag class
	 * @return associated tag class code
	 */
	public static int tagClassToInt(TagClass tagClass) {
		if (tagClass == TagClass.UNIVERSAL)
			return 0x00;
		else if (tagClass == TagClass.APPLICATION)
			return 0x40;
		else if (tagClass == TagClass.CONTEXT)
			return 0x80;
		else
			return 0xc0; // TagClass.PRIVATE
	}

	/**
	 * Parses a tag class from a tag class code
	 *
	 * <p>Applies 0xc0 mask to the code before parsing.</p>
	 *
	 * @param tagClass tag class code
	 * @return associated tag class
	 */
	public static TagClass tagClassFromInt(int tagClass) {
		tagClass &= 0xc0;
		if (tagClass == 0x00)
			return TagClass.UNIVERSAL;
		else if (tagClass == 0x40)
			return TagClass.APPLICATION;
		else if (tagClass == 0x80)
			return TagClass.CONTEXT;
		else
			return TagClass.PRIVATE;
	}

	/**
	 * Decoded BER-encoded tag data.
	 */
	public static class Decoded {
		VASN1Tag tag;
		boolean constructed;
		int numRead;

		public Decoded(VASN1Tag tag, boolean constructed, int numRead) {
			this.tag = tag;
			this.constructed = constructed;
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
		 * True if decoded tag was constructed.
		 *
		 * @return true if constructed
		 */
		public boolean isConstructed() {
			return constructed;
		}

		/**
		 * Get number of bytes read to parse DER tag data.
		 *
		 * @return number bytes read
		 */
		public int getNumRead() {
			return numRead;
		}
	}
}
