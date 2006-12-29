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

package net.driftingsouls.ds2.jstarmap;

import java.awt.event.InputEvent;

import net.driftingsouls.ds2.framework.JDialog;
import net.driftingsouls.ds2.framework.JTextField;
import net.driftingsouls.ds2.framework.JWindow;
import net.driftingsouls.ds2.framework.IWindowManager;

/**
 * JAboutDialog repräsentiert einen Textdialog, welcher Infos über die Sternenkarte anzeigen
 * soll. Der Text wird jedoch ausserhalb des Dialogs via {@link #setText} gesetzt.
 * 
 * @author Christopher Jung
 */
public class JAboutDialog extends JDialog {	
	private boolean autoHeight;
	private JTextField textfield;
	
	public JAboutDialog( JWindow parent, IWindowManager windowmanager, String myfont ) {
		super( parent, windowmanager );
		
		autoHeight = false;
		
		textfield = new JTextField( this, windowmanager, myfont );
		textfield.setSize(getClientWidth(),getClientHeight());
		textfield.setPosition(0,0);
	}
	
	public void setText( String text ) {
		textfield.setText( text );
	}
	
	public void setSize( int width, int height ) {
		super.setSize(width,height);
		
		textfield.setSize(getClientWidth(),getClientHeight());
	}
	
	public void setClientSize( int width, int height ) {
		super.setClientSize(width,height);
		
		textfield.setClientSize(getClientWidth(),getClientHeight());
	}
	
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed(x, y, button);
		
		if( button == InputEvent.BUTTON3_MASK ) {
			getWindowManager().setVisibility(this, false);
			
			return true;
		}
		
		return result;
	}
}