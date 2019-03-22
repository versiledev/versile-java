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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.versile.common.util.VBitfield;
import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VObjectIdentifier;
import org.versile.orb.entity.VBoolean;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VNone;
import org.versile.orb.entity.VString;



/**
 * Base class for ASN.1 objects.
 */
public abstract class VASN1Base {
	protected String name = null;
	protected VASN1Definition definition = null;

	/**
	 * Set up object without name or definition.
	 */
	public VASN1Base() {
	}

	/**
	 * Set up object with name and/or definition.
	 *
	 * @param name a name for the ASN.1 type (or null)
	 * @param definition a definition for the ASN.1 type (or null)
	 */
	public VASN1Base(String name, VASN1Definition definition) {
		this.name = name;
		this.definition = definition;
	}

	/**
	 * Get identifier (tag) of the ASN.1 object.
	 *
	 * @return associated tag
	 * @throws VASN1Exception cannot derive tag
	 */
	public abstract VASN1Tag getTag()
			throws VASN1Exception;

	/**
	 * Get a Java-native representation of the value.
	 *
	 * <p>Performs deep conversion.</p>
	 *
	 * @return native representation, or object itself.
	 */
	public Object getNative() {
		return this.getNative(true);
	}

	/**
	 * Get a Java-native representation of the value.
	 *
	 * @param deep if true perform deep native-conversion
	 * @return native representation, or object itself.
	 */
	public abstract Object getNative(boolean deep);

	/**
	 * Generate the object's DER representation including identifier octets.
	 *
	 * @return DER encoding
	 * @throws VASN1Exception encoder error
	 */
	public byte[] encodeDER()
			throws VASN1Exception {
		return this.encodeDER(true);
	}

	/**
	 * Generate the object's DER representation.
	 *
	 * @param withTag if true include identifier octets
	 * @return DER encoding
	 * @throws VASN1Exception encoder error
	 */
	public abstract byte[] encodeDER(boolean withTag)
			throws VASN1Exception;

	/**
	 * Validates against the object's definition (if set).
	 *
	 * <p>Validation is performed by DER encoding and then using the definition
	 * set on the object to parse.</p>
	 *
	 * @return true if validated or no definition
	 */
	public boolean validate() {
		return this.validate(false);
	}

	/**
	 * Validates against the object's definition.
	 *
	 * <p>Validation is performed by DER encoding and then using the definition
	 * set on the object to parse.</p>
	 *
	 * @param strict if true return false if no definition
	 * @return true if validated (or not strict and no definition)
	 */
	public boolean validate(boolean strict) {
		if (definition == null)
			return !strict;

		byte[] der;
		byte[] der2;
		try {
			der = this.encodeDER();
			VASN1Base reconstructed = this.definition.parseDER(der).getResult();
			der2 = reconstructed.encodeDER();
		} catch (VASN1Exception e) {
			return false;
		}
		if (der.length != der2.length)
			return false;
		for (int i = 0 ; i < der.length; i++)
			if (der[i] != der2[i])
				return false;
		return true;
	}

	/**
	 * Get type name set for object
	 *
	 * @return type name (or null)
	 */
	public String getASN1Name() {
		return name;
	}

	/**
	 * Get ASN.1 definition for object.
	 *
	 * @return ASN.1 definition (or null)
	 */
	public VASN1Definition getASN1Definition() {
		return definition;
	}

	/**
	 * Lazy-converts a native value to a VASN1Base.
	 *
	 * <p>Converts null and VNone to VASN1Null.</p>
	 *
	 * </p>Converts Boolean and VBoolean to VASN1Boolean.</p>
	 *
	 * <p>Converts Integer, Long, BigInteger, VInteger to VASN1Integer.</p>
	 *
	 * <p>Converts VBitfield to VASN1BitString.</p>
	 *
	 * <p>Converts byte[] and VBytes to VASN1OctetString.</p>
	 *
	 * <p>Converts VObjectIdentifier to VASN1ObjectIdentifier.</p>
	 *
	 * <p>Converts String and VString to VASN1UTF8String.</p>
	 *
	 * <p>Converts Date to VASN1UTCTime.</p>
	 *
	 * <p>Converts Object[] and List<?> to VASN1Sequence.</p>
	 *
	 * <p>Converts Set<?> to VASN1Set.</p>
	 *
	 * @param value value to convert
	 * @throws VASN1Exception value can not be lazy-converted
	 */
	public static VASN1Base lazy(Object value)
			throws VASN1Exception {
		if (value instanceof VASN1Base)
			return (VASN1Base) value;
		else if (value == null || value instanceof VNone)
			return new VASN1Null();
		else if (value instanceof Boolean)
			return new VASN1Boolean((Boolean)value);
		else if (value instanceof VBoolean)
			return new VASN1Boolean(((VBoolean)value).getValue());
		else if (value instanceof Integer || value instanceof Long || value instanceof BigInteger)
			return new VASN1Integer(new VInteger((Number)value));
		else if (value instanceof VInteger)
			return new VASN1Integer((VInteger)value);
		else if (value instanceof VBitfield)
			return new VASN1BitString((VBitfield)value);
		else if (value instanceof byte[])
			return new VASN1OctetString((byte[])value);
		else if (value instanceof VBytes)
			return new VASN1OctetString((VBytes)value);
		else if (value instanceof VObjectIdentifier)
			return new VASN1ObjectIdentifier((VObjectIdentifier)value);
		else if (value instanceof String)
			return new VASN1UTF8String((String)value);
		else if (value instanceof VString)
			return new VASN1UTF8String(((VString)value).getValue());
		else if (value instanceof Date)
			return new VASN1GeneralizedTime((Date)value);
		else if (value.getClass().isArray() || value instanceof List<?>) {
			List<?> list;
			if (value.getClass().isArray())
				list = Arrays.asList((Object[])value);
			else
				list = (List<?>)value;
			VASN1Sequence result = new VASN1Sequence();
			for (Object obj: list)
				result.append(VASN1Base.lazy(obj));
			return result;
		}
		else if (value instanceof Set<?>) {
			VASN1Set result = new VASN1Set();
			for (Object obj: (Set<?>)value)
				result.append(VASN1Base.lazy(obj));
			return result;
		}
		else
			throw new VASN1Exception("Value type not recognized");
	}

	/**
	 * Generate BER definite encoding of content octets length.
	 *
	 * <p>Uses short form if possible.</p>
	 *
	 * @param length length to encode
	 * @return BER definite length encoding
	 * @throws VASN1Exception BER encoding overflow
	 */
	public static byte[] berEncLengthDefinite(VInteger length)
			throws VASN1Exception {
		return VASN1Base.berEncLengthDefinite(length, true);
	}

	/**
	 * Generate BER definite encoding of content octets length.
	 *
	 * @param length length to encode
	 * @param useShort if True use short form if possible
	 * @return BER definite length encoding
	 * @throws VASN1Exception BER encoding overflow
	 */
	public static byte[] berEncLengthDefinite(VInteger length, boolean useShort)
			throws VASN1Exception {
		if (length.getBigIntegerValue().compareTo(BigInteger.valueOf(0x7f)) <= 0 && useShort)
			return new byte[] {(byte)length.getValue().intValue()};
		else {
			byte[] length_bytes = VInteger.posint_to_bytes(length.getValue());
			if (length_bytes.length >= 0x7f)
				throw new VASN1Exception("Length overflow");
			byte first_byte = (byte)(length_bytes.length | 0x80);
			byte[] result = new byte[length_bytes.length+1];
			result[0] = first_byte;
			for (int i = 0; i < length_bytes.length; i++)
				result[i+1] = length_bytes[i];
			return result;
		}
	}

	/**
	 * Returns BER indefinite length code.
	 *
	 * @return BER indefinite length code
	 */
	public static byte[] berEncLengthIndefinite() {
		return new byte[] {(byte)0x80};
	}

	/**
	 * Generates BER content encoding for definite length.
	 *
	 * @param content content to encode
	 * @return encoded content
	 */
	public static byte[] berEncContentDefinite(byte[] content) {
		return content;
	}

	/**
	 * Generates BER content encoding for indefinite length.
	 *
	 * @param content content to encode
	 * @return encoded content
	 */
	public static byte[] berEncContentIndefinite(byte[] content) {
		byte[] result = new byte[content.length + 2];
		for (int i = 0; i < content.length; i++)
			result[i] = content[i];
		result[content.length] = (byte)0x00;
		result[content.length+1] = (byte)0x00;
		return result;
	}

	/**
	 * Generate BER encoding for general tagged content.
	 *
	 * <p>Uses definite-length encoding and uses short-length encoding when possible.</p>
	 *
	 * @param tag tag of tagged content
	 * @param isConstructed if true constructed tag
	 * @param content binary content
	 * @return BER encoded content
	 * @throws VASN1Exception encoding error
	 */
	public static byte[] berEncTaggedContent(VASN1Tag tag, boolean isConstructed, byte[] content)
			throws VASN1Exception {
		return VASN1Base.berEncTaggedContent(tag, isConstructed, content, true, true);
	}

	/**
	 * Generate BER encoding for general tagged content.
	 *
	 * @param tag tag of tagged content
	 * @param isConstructed if true constructed tag
	 * @param content binary content
	 * @param useDefinite if true encode as definite-length
	 * @param useShort if true use short-length encoding if possible
	 * @return BER encoded content
	 * @throws VASN1Exception encoding error
	 */
	public static byte[] berEncTaggedContent(VASN1Tag tag, boolean isConstructed, byte[] content,
										     boolean useDefinite, boolean useShort)
			throws VASN1Exception {
		byte[] identifier = tag.encodeBER(isConstructed);
		byte[] length;
		byte[] _content;
		if (useDefinite) {
			length = VASN1Base.berEncLengthDefinite(new VInteger(content.length), useShort);
			_content = VASN1Base.berEncContentDefinite(content);
		}
		else {
			length = VASN1Base.berEncLengthIndefinite();
			_content = VASN1Base.berEncContentIndefinite(content);
		}
		VByteBuffer buf = new VByteBuffer();
		buf.append(identifier);
		buf.append(length);
		buf.append(_content);
		return buf.popAll();
	}
}
