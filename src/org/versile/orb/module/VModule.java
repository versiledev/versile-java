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

package org.versile.orb.module;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.versile.common.util.VObjectIdentifier;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTuple;



/**
 * Handles parsing of VER encoded objects.
 */
public class VModule {

	Map<Object, VModuleDecoder> names;
	Map<VObjectIdentifier, VModuleDecoder> oids;
	Set<VModuleDecoder> decoders;

	Map<Class<?>, VModuleConverter> classes;
	Set<VModuleConverter> converters;

	public VModule() {
		names = new Hashtable<Object, VModuleDecoder>();
		oids = new Hashtable<VObjectIdentifier, VModuleDecoder>();
		decoders = new HashSet<VModuleDecoder>();

		classes = new Hashtable<Class<?>, VModuleConverter>();
		converters = new HashSet<VModuleConverter>();
	}

	/**
	 * Adds a module decoder.
	 *
	 * @param decoder module to add
	 * @throws VModuleError error adding decoder
	 */
	public synchronized void addDecoder(VModuleDecoder decoder)
		throws VModuleError {
		if (decoder.getName() != null) {
			Vector<Object> key = VModule.nameVerToKey(decoder.getName(), decoder.getVersion());
			if (names.get(key) != null)
				throw new VModuleError("Name/version already registered: " + key);
			else
				names.put(key, decoder);
		}
		if (decoder.getOID() != null) {
			if (oids.get(decoder.getOID()) != null)
				throw new VModuleError("OID already registered");
			oids.put(decoder.getOID(), decoder);
		}
		decoders.add(decoder);
	}

	/**
	 * Adds a module converter.
	 *
	 * @param converter
	 * @throws VModuleError error adding converter
	 */
	public synchronized void addConverter(VModuleConverter converter)
		throws VModuleError {
		for (Class<?> cls: converter.getClasses())
			if (classes.get(cls) != null)
				throw new VModuleError("Class already registered");
		for (Class<?> cls: converter.getClasses())
			classes.put(cls, converter);
		converters.add(converter);
	}

	/**
	 * Removes a decoder from the module.
	 *
	 * @param decoder decoder to remove
	 * @throws VModuleError error removing decoder
	 */
	public synchronized void removeDecoder(VModuleDecoder decoder)
		throws VModuleError {
		if (decoder.getName() != null) {
			Vector<Object> key = VModule.nameVerToKey(decoder.getName(), decoder.getVersion());
			names.remove(key);
		}
		if (decoder.getOID() != null) {
			oids.remove(decoder.getOID());
		}
		decoders.remove(decoder);
	}

	/**
	 * Removes a converter from the module.
	 *
	 * @param converter converter to remove
	 * @throws VModuleError error removing converter
	 */
	public synchronized void removeConverter(VModuleConverter converter)
		throws VModuleError {
		for (Class<?> cls: converter.getClasses())
			classes.remove(cls);
		converters.remove(converter);
	}

	/**
	 * Get the decoder associated with provided name and version.
	 *
	 * @param name name list resolved by the decoder
	 * @param version version resolved by the decoder
	 * @return decoder
	 * @throws VModuleError unknown decoder
	 */
	public synchronized VModuleDecoder decoderFromName(String[] name, VInteger[] version)
		throws VModuleError {
		Vector<Object> key = VModule.nameVerToKey(name, version);
		VModuleDecoder result = names.get(key);
		if (result == null)
			throw new VModuleError("Unknown decoder");
		return result;
	}

	/**
	 * Get the decoder associated with provided object identifier.
	 *
	 * @param oid object idenfitier resolved by the decoder
	 * @return decoder
	 * @throws VModuleError unknown decoder
	 */
	public synchronized VModuleDecoder decoderFromOID(VObjectIdentifier oid)
		throws VModuleError {
		VModuleDecoder result = oids.get(oid);
		if (result == null)
			throw new VModuleError("Unknown decoder");
		return result;
	}

	/**
	 * Get the converter associated with provided class.
	 *
	 * @param type class resolved by converter
	 * @return converter
	 * @throws VModuleError unknown converter
	 */
	public synchronized VModuleConverter converterFromType(Class<?> type)
		throws VModuleError {
		VModuleConverter result = classes.get(type);
		if (result == null)
			throw new VModuleError("Converter for class not registered");
		return result;
	}

	/**
	 * Get the decoders registered with the module.
	 *
	 * @return decoders
	 */
	public synchronized VModuleDecoder[] getDecoders() {
		return decoders.toArray(new VModuleDecoder[0]);
	}

	/**
	 * Get the converters registered with the module.
	 *
	 * @return converters
	 */
	public synchronized VModuleConverter[] getConverters() {
		return converters.toArray(new VModuleConverter[0]);
	}

	/**
	 * Returns a value as a VObject
	 *
	 * <p>Convenience method for resolving an object which may be either a proxy or a VObject.</p>
	 *
	 * @param obj a VObject or VProxy
	 * @throws VModuleError invalid argument type
	 */
	public static VObject asVObject(Object obj)
		throws VModuleError {
		if (obj instanceof VProxy)
			return ((VProxy)obj).get();
		else if(obj instanceof VObject)
			return (VObject)obj;
		else
			throw new VModuleError("Cannot represent as a VObject");
	}

	/**
	 * Generate VER tags for a name and version.
	 *
	 * <p>Convenience method for use by VER encoders.</p>
	 *
	 * @param name name list
	 * @param version version numbers
	 * @return leading tags for the VER encoded format
	 */
	public static VEntity[] nameTags(String[] name, VInteger[] version) {
		VEntity[] result = new VEntity[3];
		result[0] = new VInteger(-1);
		LinkedList<VEntity> tmp = new LinkedList<VEntity>();
		for (String s: name)
			tmp.addLast(new VString(s));
		result[1] = new VTuple(tmp);
		result[2] = new VTuple(version);
		return result;
	}

	/**
	 * Generate VER tags for a provided (complete) object identifier.
	 *
	 * <p>Convenience method for use by VER encoders.</p>
	 *
	 * @param oid encoding format object identifier
	 * @return leading tags for the VER encoded format
	 */
	public static VEntity[] oidTags(VObjectIdentifier oid) {
		VInteger[] ids = oid.getIdentifiers();
		VEntity[] result = new VEntity[ids.length+1];
		result[0] = new VInteger(10*ids.length);
		for (int i = 0; i < ids.length; i++)
			result[i+1] = ids[i];
		return result;
	}

	static Vector<Object> nameVerToKey(String[] name, VInteger[] version) {
		Vector<Object> result = new Vector<Object>();
		for (String s: name)
			result.add(s);
		for (VInteger i: version)
			result.add(i);
		return result;
	}
}
