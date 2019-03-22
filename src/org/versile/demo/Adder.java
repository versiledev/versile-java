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

package org.versile.demo;

import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VInteger;
import org.versile.orb.external.Doc;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;


/**
 * Example remote-enabled service for tracking a rolling partial sum.
 *
 * <p>This class is intended for testing and demonstration only and is not
 * formally part of the Versile Java framework.</p>
 */
@Doc(doc="Tracks a rolling sum")
public class Adder extends VExternal {
	VInteger sum;

	public Adder() {
		sum = new VInteger(0);
	}

	/**
	 * Add to the rolling partial sum
	 *
	 * @param num number to add
	 * @return updated partial sum
	 * @throws VCallError invalid number argument
	 */
	@Publish(show=true, ctx=false)
	@Doc(doc="Add an integer to the rolling sum")
	public synchronized Object add(VInteger num)
		throws VCallError {
		try {
			sum = sum.lazyAdd(num);
		} catch (VEntityError e) {
			throw new VCallError();
		}
		return sum;
	}

	/**
	 * Get the current value of the partial sum
	 *
	 * @return partial sum
	 */
	@Publish(show=true, ctx=false)
	@Doc(doc="Returns the current value of the rolling sum")
	public synchronized Object result() {
		return sum;
	}

	/**
	 * Resets the partial sum to zero
	 */
	@Publish(show=true, ctx=false)
	@Doc(doc="Resets the rolling sum to zero")
	public synchronized void reset() {
		sum = new VInteger(0);
	}
}
