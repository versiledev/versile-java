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

package org.versile.vse.time;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VFloat;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTaggedParseError;
import org.versile.orb.entity.VTuple;
import org.versile.orb.module.VModuleConverter;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;


/**
 * A time reference for Coordinated Universal Time.
 */
public class VUTCTime extends VEntity {

	/**
	 * VSE code for the VUTCTime type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"time", "utctime"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(5, 1));

	VInteger days;  // Index of day vs. January 1, 1970
	VFloat secs;    // Seconds into day

	/**
	 * Initialize time object.
	 *
	 * <p>Encodes a time reference in UTC format. The number of seconds must be
	 * 0 <= secs < 86402. The number of seconds in a day may be between 86399 and
	 * 86402 due to how the UTC standard handles leap seconds.</p>
	 *
	 * <p>If the 'seconds' argument can be evaluated to be outside allowed range
	 * (this may not be done if number is not base-2 or base-10), an exception
	 * is raised. If the number is not evaluated during construction, any value
	 * outside the allowed range is assumed to mean the minimum or maximum allowed
	 * value (whichever comes closer).</p>
	 *
	 * @param days index of day since Jan 1, 1970
	 * @param secs number of seconds into day
	 * @throws VEntityError seconds value known to be illegal
	 */
	public VUTCTime(VInteger days, VFloat secs)
		throws VEntityError {

		// Verify acceptable value of secs, when (practically) possible
		boolean _fail = false;
		try {
			BigDecimal _secs = secs.toBigDecimal();
			if ((_secs.compareTo(BigDecimal.ZERO) < 0) || (_secs.compareTo(BigDecimal.valueOf(86402)) > 0))
				_fail = true;
		}
		catch (VEntityError e) {
			// Ignore, cannot test because cannot (efficiently) evaluate number
		}
		if (_fail)
			throw new VEntityError();

		this.days = days;
		this.secs = secs;
	}

	/**
	 * Initialize time object.
	 *
	 * @param date time reference
	 */
	public VUTCTime(Date date) {
		if (date instanceof Timestamp) {
			// ns precision
			long millis = date.getTime();
			long days = millis / 86400000;
			millis -= days*86400000;
			BigDecimal secs = BigDecimal.valueOf(millis / 1000);
			BigDecimal nanos = BigDecimal.valueOf(((Timestamp)date).getNanos());
			secs = secs.add(nanos.divide(BigDecimal.valueOf(1000000000)));

			this.days = new VInteger(BigInteger.valueOf(days));
			this.secs = VFloat.fromBigDecimal(secs);
		}
		else {
			// ms precision
			long millis = date.getTime();
			long days = millis / 86400000;
			millis -= days*86400000;
			this.days = new VInteger(BigInteger.valueOf(days));
			this.secs = VFloat.fromBigDecimal(BigDecimal.valueOf(millis).divide(BigDecimal.valueOf(1000)));
		}
	}

	/**
	 * Get day index after Jan 1, 1970.
	 *
	 * @return day index (may be negative)
	 */
	public VInteger getDays() {
		return days;
	}

	/**
	 * Get number of seconds into day.
	 *
	 * <p>Currently returns {@link VFloat}. However, return type is
	 * {@link VEntity} in case the standard is later allowed to
	 * expand to allow other types. Thus, it cannot be assumed that
	 * the return number has to be a {@link VFloat}.</p>
	 *
	 * @return number of seconds
	 */
	public VEntity getSeconds() {
		return secs;
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VUTCTime.VSE_CODE.getTags(ctx);
		VEntity value = new VTuple(new VEntity[] {days, secs});
		return new VTagged(value, tags);
	}

	/**
	 * Get VSE decoder for tag data.
	 *
	 * @return decoder
	 * @throws VTaggedParseError
	 */
	static public VModuleDecoder.Decoder _v_vse_decoder() {
		class Decoder extends VModuleDecoder.Decoder {
			@Override
			public Pair decode(Object value, Object[] tags) throws VModuleError {
				class Combiner extends VCombiner {
					@Override
					public Object combine(Vector<Object> objects)
							throws CombineException {
						if (objects.size() != 2)
							throw new CombineException("Illegal number of VTagged values");
						VInteger days;
						VFloat secs;
						try {
							days = VInteger.valueOf(objects.get(0));
							secs = VFloat.valueOf(objects.get(1));
						} catch (VEntityError e) {
							throw new CombineException("Illegal values in VTagged encoding");
						}

						try {
							return new VUTCTime(days, secs);
						} catch (VEntityError e) {
							throw new CombineException("Illegal values in VTagged encoding");
						}
					}
				}
				if (tags.length > 0)
					throw new VModuleError("Illegal use of residual tags");
				VTuple elements = null;
				try {
					elements = VTuple.valueOf(value);
				} catch (VEntityError e) {
					throw new VModuleError();
				}
				Vector<Object> comb_items = new Vector<Object>();
				for (Object obj: elements)
					comb_items.add(obj);
				return new VCombiner.Pair(new Combiner(), comb_items);
			}
		}
		return new Decoder();
	}

	/**
	 * Get VSE converter for native type.
	 *
	 * @return converter
	 * @throws VTaggedParseError
	 */
	static public VModuleConverter.Converter _v_vse_converter() {
		class Converter extends VModuleConverter.Converter {
			@Override
			public Pair convert(Object obj) throws VModuleError {
				try {
					return VUTCTime._v_converter(obj);
				} catch (VEntityError e) {
					throw new VModuleError();
				}
			}
		}
		return new Converter();
	}

	/**
	 * Generates an entity converter structure for the entity's type.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @param obj native object to convert
	 * @return combiner structure for conversion.
	 */
	public static VCombiner.Pair _v_converter(Object obj)
			throws VEntityError {
		VUTCTime result;
		if (obj instanceof VUTCTime) {
			result = (VUTCTime) obj;
		}
		else if (obj instanceof Date) {
			result = new VUTCTime((Date)obj);
		}
		else
			throw new VEntityError("Cannot convert from provided structure");

		return new VCombiner.Pair(VCombiner.value(result), new Vector<Object>());
	}

	@Override
	public VCombiner.Pair _v_native_converter() {
		class Combiner extends VCombiner {
			VUTCTime val;
			public Combiner(VUTCTime val) {
				this.val = val;
			}
			@Override
			public Object combine(Vector<Object> objects)
					throws CombineException {
				if (objects.size() != 0)
					throw new CombineException();

				BigDecimal _time = new BigDecimal(val.days.getBigIntegerValue());
				_time = _time.multiply(BigDecimal.valueOf(86400));
				BigDecimal _secs;
				try {
					_secs = val.secs.toBigDecimal();
				} catch (VEntityError e) {
					throw new CombineException(e);
				}
				_time = _time.add(_secs).stripTrailingZeros();

				// Check whether ms representation can fit in Date format as long
				BigDecimal _t_ms = _time.multiply(BigDecimal.valueOf(1000));
				BigInteger _t_ms_int = _t_ms.toBigInteger();
				if ((_t_ms_int.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0)
						|| (_t_ms_int.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0)) {
					// Out of bounds for Date or Timestamp types
					throw new CombineException("Cannot represent as native type");
				}

				// Check whether time value has ms precision
				if (_t_ms.subtract(new BigDecimal(_t_ms_int)).compareTo(BigDecimal.ZERO) == 0)
					return new Date(_t_ms_int.longValue());

				// Check whether time value has ns precision
				BigDecimal _t_ns = _time.multiply(BigDecimal.valueOf(1000000000L));
				BigInteger _t_ns_int = _t_ns.toBigInteger();
				if (_t_ns.subtract(new BigDecimal(_t_ns_int)).compareTo(BigDecimal.ZERO) == 0) {
					Timestamp result = new Timestamp(_t_ms_int.longValue());
					BigDecimal _ns_part = _time.remainder(BigDecimal.ONE);
					_ns_part = _ns_part.scaleByPowerOfTen(9);
					result.setNanos(_ns_part.toBigInteger().intValue());
					return result;
				}

				// Cannot convert to native type
				throw new CombineException("Cannot represent as native type");
			}
		}
		return new VCombiner.Pair(new Combiner(this), new Vector<Object>());
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	@Override
	public String toString() {
		Object conv = _v_native();
		if (conv instanceof VUTCTime)
			return "UTC[1970.01.01 + " + days + " days + " + secs + "s]";
		else
			return conv.toString();
	}

	/**
	 * Converts input to a {@link VUTCTime}.
	 *
	 * <p>{@link VUTCTime} is returned as-is; Date and Timestamp are
	 * converted. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public VUTCTime valueOf(Object value)
		throws VEntityError {
		if (value instanceof VUTCTime)
			return (VUTCTime) value;
		else if (value instanceof Date)
			return new VUTCTime((Date)value);
		else
			throw new VEntityError("Cannot convert structure to VUTCTime");
	}

	/**
	 * Converts input to a Date or Timestamp.
	 *
	 * <p>Date or Timestamp is returned as-is. For {@link VUTCTime} conversion
	 * is attempted to the return type. Otherwise, an exception is raised.</p>
	 *
	 * <p>Note that returned type may be a Timestamp, capturing nanosecond
	 * precision date values.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public Date nativeOf(Object value)
		throws VEntityError {
		if (value instanceof VUTCTime)
			value = ((VUTCTime) value)._v_native();

		if (value instanceof Date)
			return (Date) value;
		else
			throw new VEntityError("Cannot represent as native type");
	}
}
