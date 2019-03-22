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

import org.versile.common.util.VExceptionProxy;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VException;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTuple;
import org.versile.orb.external.Doc;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;
import org.versile.orb.service.VGatewayFactory;


/**
 * Example remote-enabled dispatcher for an echo service and adder service.
 *
 * <p>Provides an {@link org.versile.demo.Echoer} service for VRI path "/text/echo" and
 * an {@link org.versile.demo.Adder} service for VRI path "/math/adder".</p>
 *
 * <p>This class is intended for testing and demonstration only and is not
 * formally part of the Versile Java framework.</p>
 */
@Doc(doc="Provides a gateway to an echo and adder service")
public class SimpleGateway extends VExternal {

	/**
	 * Resolve relative VRI reference
	 *
	 * @param obj path as per VRI standard
	 * @return referenced resource
	 * @throws VExceptionProxy invalid path
	 * @throws VCallError illegal argument
	 */
	@Publish(show=true, ctx=false)
	public Object urlget(Object obj)
			throws VExceptionProxy, VCallError {
		Object[] o_path = null;
		if (obj instanceof VTuple)
			o_path = ((VTuple)obj).toArray();
		else if (obj instanceof Object[])
			o_path = (Object[]) obj;
		else
			throw new VCallError();

		String[] path = new String[o_path.length];
		for (int i = 0; i < path.length; i++) {
			if (o_path[i] instanceof VString)
				path[i] = ((VString)o_path[i]).getValue();
			else if (o_path[i] instanceof String)
				path[i] = (String)o_path[i];
			else
				throw new VCallError();
		}

		if (path.length < 2)
			throw new VException("No such path").getProxy();
		if (path[0].equals("text") && path[1].equals("echo"))
			return new Echoer();
		else if (path[0].equals("math") && path[1].equals("adder"))
			return new Adder();
		else
			throw new VException("No such path").getProxy();
	}

	/**
	 * Factory for {@link SimpleGateway} gateway objects.
	 */
	public static class Factory implements VGatewayFactory {
		@Override
		public VObject build() {
			return new SimpleGateway();
		}
	}
}
