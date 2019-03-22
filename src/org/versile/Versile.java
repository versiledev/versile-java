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

package org.versile;


/**
 * Global Versile Java configuration.
 *
 * <p>This class used to track license information from before licensing
 * was changed to the LGPL v3. Its use is deprecated and it is
 * retained solely for compatibility.</p>
 */
public class Versile {

	// Hardcoded after license was changed to LGPLv3
	static Boolean copyleft = true;
	static String copyleftLicense = "LGPLv3";
	static String copyleftUrl = "https://www.gnu.org/licenses/lgpl.txt";

	/**
	 * Deprecated after change to LGPLv3 and has no effect..
	 */
	public static void setAGPL(String url, String... otherLicenseNames)
		throws IllegalArgumentException {
	}

	/**
	 * Deprecated after change to LGPLv3 and has no effect..
	 */
	public static void setInternalUseAGPL() {
	}

	/**
	 * Deprecated after change to LGPLv3 and has no effect..
	 */
	public static void setCommercial() {
	}

	/**
	 * Holds information about copyleft type rights for the Versile Java license used.
	 */
	public static class CopyleftInfo {
		Boolean copyleft;
		String licenseType;
		String url;

		/**
		 * Set up copyleft info object.
		 *
		 * @param copyleft true if copyleft type rights granted, false if not, null if unknown
		 * @param licenseType if copyleft is true, name of license type(s)
		 * @param url if copyleft is true, URL to license and download information
		 */
		public CopyleftInfo(Boolean copyleft, String licenseType, String url) {
			this.copyleft = copyleft;
			this.licenseType = licenseType;
			this.url = url;
		}

		/**
		 * Gets copyleft information.
		 *
		 * @return true if copyleft applies, false if not, or null if not specified
		 */
		public Boolean getCopyleft() {
			return copyleft;
		}

		/**
		 * Gets copyleft license type information
		 *
		 * @return if copyleft applies returns string of license type(s), otherwise null
		 */
		public String getLicenseType() {
			return licenseType;
		}

		/**
		 * Gets URL for copyleft license information and download instructions.
		 *
		 * @return if copyleft applies returns string, otherwise null
		 */
		public String getCopyleftUrl() {
			return url;
		}
	}

	/**
	 * Get globally set copyleft information.
	 *
	 * @return copyleft data
	 */
	public static CopyleftInfo copyleft() {
		return new CopyleftInfo(copyleft, copyleftLicense, copyleftUrl);
	}

	/**
	 * Deprecated after change to LGPLv3 and has no effect..
	 */
	public static void resetCopyleftInfo() {
	}
}
