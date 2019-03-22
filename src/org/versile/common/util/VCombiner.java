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

import java.util.Vector;


/**
 * Holds logic for combining objects into one combined object.
 *
 * <p>The class can be used as a base class for e.g. encoders or decoders.</p>
 */
public abstract class VCombiner {

	/**
	 * Perform combine operation.
	 *
	 * @param objects input objects to combine
	 * @return combined result
	 * @throws CombineException inputs cannot be combined
	 */
	public abstract Object combine(Vector<Object> objects)
		throws CombineException;

	/**
	 * Generates an "identity" combiner.
	 *
	 * <p>The resulting combiner takes a single object as input and returns
	 * that same object as the combined result.</p>
	 *
	 * @return identity combiner
	 */
	public static VCombiner identity() {
		class Identity extends VCombiner {
			@Override
			public Object combine(Vector<Object> objects) throws CombineException {
				if (objects.size() != 1)
					throw new CombineException();
				return objects.get(0);
			}

		}
		return new Identity();
	}

	/**
	 * Generates a "value" combiner.
	 *
	 * <p>The resulting combiner takes a provided value as input and returns
	 * a combiner which generates that value for an empty input set.</p>
	 *
	 * @param val value which combiner will produce
	 * @return identity combiner
	 */
	public static VCombiner value(Object val) {
		class Value extends VCombiner {
			Object val;
			public Value(Object val) {
				this.val = val;
			}
			@Override
			public Object combine(Vector<Object> objects) throws CombineException {
				if (objects.size() != 0)
					throw new CombineException();
				return val;
			}
		}
		return new Value(val);
	}

	/**
	 * Holds a combiner and a set of combiner inputs for delayed processing.
	 */
	public static class Pair {
		VCombiner combiner;
		Vector<Object> objects;
		public Pair(VCombiner aggregator, Vector<Object> objects) {
			this.combiner = aggregator;
			this.objects = objects;
		}
		public VCombiner getCombiner() {
			return combiner;
		}
		public Vector<Object> getObjects() {
			return objects;
		}
	}

	/**
	 * Combine operation exception.
	 */
	public static class CombineException extends Exception {
		private static final long serialVersionUID = 1L;

		public CombineException() {
			super();
		}

		public CombineException(String arg0, Throwable arg1) {
			super(arg0, arg1);
		}

		public CombineException(String arg0) {
			super(arg0);
		}

		public CombineException(Throwable arg0) {
			super(arg0);
		}

	}
}
