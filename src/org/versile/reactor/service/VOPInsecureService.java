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

package org.versile.reactor.service;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

import org.versile.common.processor.VProcessor;
import org.versile.orb.link.VLinkAuth;
import org.versile.orb.link.VLinkException;
import org.versile.orb.service.VGatewayFactory;
import org.versile.reactor.VReactor;


/**
 * Service class for insecure (plaintext) VOP service.
 *
 * <p>Sets up default configuration for insecure (plaintext) VOP service. Should
 * not be used for services which can be accessed securely.</p>
 *
 * <p>Normally services should be set up as secure services only, and {@link VOPService}
 * should be used instead.</p>
 */
public class VOPInsecureService extends VOPService {

	/**
	 * Set up insecure service.
	 *
	 * <p>For parameters see {@link VOPService}. Sets up with lazy-created channel, processor
	 * and reactor, and with default {@link VOPInsecureService} configuration parameters.</p>
	 */
	public VOPInsecureService(VGatewayFactory gwFactory, VLinkAuth linkAuthorizer)
			throws IOException, VLinkException {
		super(gwFactory, null, linkAuthorizer, new VOPInsecureServiceConfig());
	}

	/**
	 * Set up insecure service.
	 *
	 * <p>For parameters see {@link VOPService}. Sets up with default
	 * {@link VOPInsecureService} configuration parameters.</p>
	 */
	public VOPInsecureService(VGatewayFactory gwFactory,
			VLinkAuth linkAuthorizer, ServerSocketChannel channel, VProcessor processor,
			VReactor reactor) throws IOException, VLinkException {
		super(gwFactory, null, linkAuthorizer, channel, processor,
				reactor, new VOPInsecureServiceConfig());
	}

	/**
	 * Set up insecure service.
	 *
	 * <p>For parameters see {@link VOPService}. Sets up with provided
	 * {@link VOPInsecureService} configuration parameters. Only insecure plaintext
	 * transport should be enabled on the configuration object.</p>
	 */
	public VOPInsecureService(VGatewayFactory gwFactory,
			VLinkAuth linkAuthorizer, ServerSocketChannel channel, VProcessor processor,
			VReactor reactor, VOPInsecureServiceConfig config)
		throws IOException, VLinkException {
		super(gwFactory, null, linkAuthorizer, channel, processor,
				reactor, config);
	}
}
