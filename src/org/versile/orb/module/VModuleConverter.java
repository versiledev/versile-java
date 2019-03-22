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

import org.versile.common.util.VCombiner;


/**
 * Converter for a VER encoded representation.
 */
public class VModuleConverter {

	Class<?>[] classes;
	Converter converter;

	/**
	 * Set up converter.
	 *
	 * @param classes classes resolved by converter
	 * @param converter conversion handler
	 */
	public VModuleConverter(Class<?>[] classes, Converter converter) {
		if (classes == null || converter == null)
			throw new IllegalArgumentException();
		this.classes = classes;
		this.converter = converter;
	}

	/**
	 * Get classes registered with the converter.
	 *
	 * @return classes
	 */
	public Class<?>[] getClasses() {
		return classes;
	}

	/**
	 * Get the associated converter.
	 *
	 * @return converter
	 */
	public Converter getConverter() {
		return converter;
	}


	/**
	 * Decoder for decoding a VER encoded data type
	 */
	public abstract static class Converter {
		/**
		 * Decode VTagged data as a VEntity.
		 *
		 * <p>Value and residual tags may be {@link org.versile.orb.entity.VEntity} however they
		 * may also have been converted to a native representation. The residual tags are the tags
		 * following the VER tags identifying the data type.</p>
		 *
		 * @param obj object to generate converter for
		 * @return converter
		 * @throws VModuleError error decoding
		 */
		public abstract VCombiner.Pair convert(Object obj)
			throws VModuleError;
	}
}
