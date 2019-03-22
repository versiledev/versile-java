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

package org.versile.vse.semantics;


import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTaggedParseError;
import org.versile.orb.entity.VTuple;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;


/**
 * A VSE reference to a 'concept'.
 */
public abstract class VConcept extends VEntity {

	/**
	 * VSE code for the VSE concept type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"semantics", "concept"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(6, 1));

	// Dynamically loaded semantics extension base class for concepts
	static Class<?> _extension_cls = null;
	// Concept ID lookup method
	static Method _extension_id_m = null;
	// Wikipedia name lookup method
	static Method _extension_wname_m = null;
	// Flag whether loading attempt was performed
	static boolean _extension_loaded = false;
	// Lock for extension class access
	static Lock _extension_lock = new ReentrantLock();

    /**
     * Type code for Platform Defined concept.
     */
    public static int TYPE_VP_DEFINED = 1;

    /**
     * Type code for English wikipedia associated concept.
     */
    public static int TYPE_EN_WIKIPEDIA = 2;

    /**
     * Type code for URL based concept.
     */
    public static int TYPE_URL = 3;

    /**
	 * Get the associated VSE concept type.
	 *
	 * @return concept type
	 */
	public abstract int conceptType();

    /**
     * Get equivalent (if any) representations for another concept type.
     *
     * <p>Returns an empty array if no alternative representations are available
     * or if the type code has an illegal value. Default implementation has no
     * equivalents other than the concept itself.</p>
     *
     * @param type Versile Platform type code for concept type
     * @return array of alternative compatible representations
     */
    public VConcept[] getEquivalents(int type) {
    	if (type == conceptType())
    		return new VConcept[] {this};
    	else
    		return new VConcept[] {};
    }

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public abstract VTagged _v_as_tagged(VIOContext ctx);

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
					//Object value;
					Object[] tags;
					//public Combiner(Object value, Object[] tags) {
					public Combiner(Object[] tags) {
						//this.value = value;
						this.tags = tags;
					}
					@Override
					public Object combine(Vector<Object> objects)
							throws CombineException {
						if (tags.length != 1)
							throw new CombineException("VConcept requires a single residual tag");
						if (objects.size() != 1)
							throw new CombineException("VConcept decoding error");

						Object value = objects.toArray()[0];

						Number ctype;
						try {
							ctype = VInteger.nativeOf(tags[0]);
						} catch (VEntityError e) {
							throw new CombineException("Illegal concept type in residual tag");
						}

						if (ctype.equals(TYPE_URL)) {
							Object[] _tuple;
							String _domain, _postfix;
							try {
								_tuple = VTuple.nativeOf(value);
								if (_tuple.length != 2)
									throw new CombineException("Illegal URL defined concept encoding");
								_domain = VString.nativeOf(_tuple[0]);
								_postfix = VString.nativeOf(_tuple[1]);
							} catch (VEntityError e) {
								throw new CombineException("Illegal URL defined concept encoding");
							}
							if (_domain.equals("http://en.wikipedia.org/wiki/")
								|| _domain.equals("https://en.wikipedia.org/wiki/")) {
								// Dynamically convert to English Wikipedia URL if domain matches
								ctype = TYPE_EN_WIKIPEDIA;
								value = _postfix;
							}
							else {
								try {
									return new VUrlConcept(_domain, _postfix);
								} catch (Exception e) {
									throw new CombineException("Illegal URL concept encoding");
								}
							}
						}

						if (ctype.equals(TYPE_EN_WIKIPEDIA)) {
							String _name;
							try {
								_name = VString.nativeOf(value);
							} catch (VEntityError e) {
								throw new CombineException("Illegal English Wikipedia type concept encoding");
							}

							// Check if there is a match with an associated platform defined concept and
							// semantics extension is loaded; if so convert to platform defined concept type
							Object _conv = null;
							loadExtension();
							try {
								if (_extension_wname_m != null)
									_conv = _extension_wname_m.invoke(null, _name);
							} catch (Exception e) {
								// Conversion failed; do nothing (handled below due to _conv = null)
							}
							if (_conv != null)
								return _conv;
							else
								return new VEnWikipediaConcept(_name);
						}

						if (ctype.equals(TYPE_VP_DEFINED)) {
							BigInteger _num;
							try {
								_num = VInteger.valueOf(value).getBigIntegerValue();
							} catch (VEntityError e) {
								throw new CombineException("Illegal platform defined concept encoding");
							}
							if ((_num.compareTo(BigInteger.valueOf(VPlatformConcept.MIN_ID_VAL)) < 0)
								|| (_num.compareTo(BigInteger.valueOf(VPlatformConcept.MAX_ID_VAL)) > 0))
								throw new CombineException("Illegal platform defined concept encoding");
							int _cnum = _num.intValue();

							// Check if semantics extension is loaded and there is a match with an implemented
							// platform defined concept; if so convert to platform defined concept type
							Object _conv = null;
							loadExtension();
							try {
								if (_extension_id_m != null)
									_conv = _extension_id_m.invoke(null, _cnum);
							} catch (Exception e) {
								// Conversion failed; do nothing (handled below due to _conv = null)
							}
							if (_conv != null)
								return _conv;
							else {
								try {
									return new VPlatformConcept(_cnum);
								} catch (Exception e) {
									throw new CombineException("Illegal platform defined concept encoding");
								}
							}
						}

						// Fallback if no value was decoded
						throw new CombineException("Invalid concept type in residual tag");
					}
				}
				if (tags.length != 1)
					throw new VModuleError("Illegal use of residual tags");
				Vector<Object> comb_items = new Vector<Object>();
				comb_items.add(value);
				return new VCombiner.Pair(new Combiner(tags), comb_items);
			}
		}
		return new Decoder();
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	/**
	 * Dynamically tries to load extension, if attempt was not already made.
	 */
	static void loadExtension() {
		synchronized(_extension_lock) {
			if (!_extension_loaded) {
				_extension_loaded = true;
				String _cname = "org.versile.ext.semantics.concept.VExtConcept";
				try {
					_extension_cls = ClassLoader.getSystemClassLoader().loadClass(_cname);
					_extension_id_m = _extension_cls.getMethod("getConceptForId", Integer.class);
					_extension_wname_m = _extension_cls.getMethod("getConceptForWName", String.class);
				} catch (Exception e) {
					// We just ignore; the result will be that _extension_cls remains null,
					// which indicates it could not be loaded
					_extension_cls = null;
					_extension_id_m = null;
					_extension_wname_m = null;
				}
			}
		}
	}

}
