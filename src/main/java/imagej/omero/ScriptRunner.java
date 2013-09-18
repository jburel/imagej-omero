/*
 * #%L
 * Server- and client-side communication between ImageJ and OMERO.
 * %%
 * Copyright (C) 2013 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package imagej.omero;

import imagej.ImageJ;
import imagej.command.CommandInfo;
import imagej.core.commands.debug.SystemInformation;
import imagej.module.Module;

import java.io.IOException;

import org.scijava.AbstractContextual;
import org.scijava.Context;

/**
 * Executes ImageJ {@link Module}s as OMERO scripts.
 * 
 * @author Curtis Rueden
 * @see "https://www.openmicroscopy.org/site/support/omero4/developers/Modules/Scripts.html"
 */
public class ScriptRunner extends AbstractContextual {

	// -- Instance fields --

	/** The ImageJ application gateway. */
	private final ImageJ ij;

	// -- Constructors --

	public ScriptRunner() {
		this(new ImageJ());
	}

	public ScriptRunner(final Context context) {
		this(new ImageJ(context));
	}

	public ScriptRunner(final ImageJ ij) {
		this.ij = ij;
		setContext(ij.getContext());
	}

	// -- ScriptRunner methods --

	/** Gets the ImageJ application gateway used by this script runner. */
	public ImageJ ij() {
		return ij;
	}

	/** Toplevel invocation entry point from OMERO Jython scripts. */
	public void invoke(final String command) throws omero.ServerError,
		Glacier2.CannotCreateSessionException, Glacier2.PermissionDeniedException,
		IOException
	{
		ij.log().debug("invoke: " + command);

		// dump system properties, for debugging purposes
		ij.log().debug("System properties:");
		ij.log().debug(SystemInformation.getSystemProperties());

		// look up the requested command (FIXME: support non-command modules too)
		final CommandInfo info = ij.command().getCommand(command);
		if (info == null) {
			throw new IllegalArgumentException("No such command: " + command);
		}

		// initialize OMERO client session
		final omero.client c = new omero.client();

		// initialize module converter
		final ModuleAdapter adaptedModule =
			new ModuleAdapter(ij.getContext(), info, c);

		// perform appropriate action (either parse or launch)
		try {
			c.createSession().detachOnDestroy();
			final String parse = c.getProperty("omero.scripts.parse");
			if (!parse.isEmpty()) adaptedModule.params();
			else adaptedModule.launch();
		}
		finally {
			c.__del__();
		}
	}

}