/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.framework.pipeline;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.reader.Reader;

/**
 * Eine Pipeline basierend auf Reader.
 * @author Christopher Jung
 *
 */
public class ReaderPipeline implements Pipeline {
	private Class<? extends Reader> reader;
	private String file;
	
	/**
	 * Konstruktor.
	 * @param reader Der zu verwendende Reader
	 * @param file Die zu lesende Datei
	 */
	public ReaderPipeline(Class<? extends Reader> reader, String file) {
		this.reader = reader;
		this.file = file;
	}

	@Override
	public void execute(Context context) throws Exception {
		Reader reader = this.reader.getDeclaredConstructor().newInstance();
		
		reader.read(context, this);
	}

	/**
	 * Gibt die zu lesende Datei zurueck.
	 * @return Die zu lesende Datei
	 */
	public String getFile() {
		return this.file;
	}
	
	/**
	 * Setzt die zu lesende Datei.
	 * @param file Die zu lesende Datei
	 */
	public void setFile(String file) {
		this.file = file;
	}
}
