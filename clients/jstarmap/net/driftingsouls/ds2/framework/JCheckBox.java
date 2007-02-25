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
package net.driftingsouls.ds2.framework;

import java.awt.Graphics2D;

/**
 * JCheckBox dient dazu eine Checkbox auf den Bildschrim zu zaubern.
 * Dazu werden zwei Bilder vom Server (checkbox_*.png) sowie ein {@link JTextField}
 * benutzt
 * 
 * @author Christopher Jung
 */
public class JCheckBox extends JWindow {
	private String text;
	private boolean checked;
	private JTextField textfield;
	
	/**
	 * Erstellt eine neue CheckBox
	 * 
	 * @param parent		Das Elternfenster
	 * @param windowmanager	Der WindowManager
	 */
	public JCheckBox(JWindow parent, IWindowManager windowmanager) {
		super(parent, windowmanager);
		
		text = "";
		checked = false;
		
		windowmanager.getImageCache().getImage("interface/jstarmap/checkbox_checked.png", false);
		windowmanager.getImageCache().getImage("interface/jstarmap/checkbox_unchecked.png", false);
		
		textfield = new JTextField(this, windowmanager, windowmanager.getDefaultFont());
		textfield.setPosition(20,0);
		textfield.setSize(getClientWidth()-20,getClientHeight());
		
		if( Math.max(textfield.getClientHeight(),16) != getClientHeight() ) {
			setClientSize(getClientWidth(),Math.max(textfield.getClientHeight(),16));
		}
		
		textfield.disableVScrolling();
		
		disableVScrolling();
	}
	
	/**
	 * Setzt den Text der CheckBox
	 * 
	 * @param text Der neue CheckBox-Text
	 */
	public void setText( String text ) {
		this.text = text;
		this.textfield.setText(text);
	}
	
	/**
	 * Gibt den CheckBox-Text zurück
	 * @return Der Text der CheckBox
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Stellt fest, ob die CheckBox aktiv ist oder nicht
	 * @return <code>true</code> für aktiv, sonst <code>false</code>
	 */
	public boolean isChecked() {
		return checked;
	}
	
	/**
	 * Setzt den Status der CheckBox
	 * 
	 * @param checked <code>true</code> für aktiv, sonst <code>false</code>
	 */
	public void setChecked( boolean checked ) {
		this.checked = checked;
	}

	@Override
	public boolean mousePressed(int x, int y, int button) {
		boolean result = super.mousePressed(x, y, button);
		if( (x <= 18) && (y <= 18) ) {
			checked = !checked;
			getWindowManager().requestRedraw(this);
			
			return true;
		}
		
		return result;
	}
	
	@Override
	public void onResize() {
		super.onResize();
		
		textfield.setSize(getClientWidth()-20,getClientHeight());
		
		if( Math.max(textfield.getClientHeight(),16) != getClientHeight() ) {
			setClientSize(getClientWidth(),Math.max(textfield.getClientHeight(),16));
		}
	}
	
	@Override
	public void paint(Graphics2D g) {
		super.paint(g);
		
		if( checked ) {
			g.drawImage(getWindowManager().getImageCache().getImage("interface/jstarmap/checkbox_checked.png", false),
						0,0,16,16,null);
		}
		else {
			g.drawImage(getWindowManager().getImageCache().getImage("interface/jstarmap/checkbox_unchecked.png", false),
						0,0,16,16,null);
		}
	}
}
