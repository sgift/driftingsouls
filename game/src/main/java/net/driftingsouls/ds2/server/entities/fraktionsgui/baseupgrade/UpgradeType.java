/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.framework.Common;

import java.util.Optional;

/**
 * Moegliche Ausbauten fuer Asteroiden.
 */
public enum UpgradeType
{
	CORE("CoreUpgrade", "Core-Entfernung")
	{
		@Override
		public void doWork(UpgradeInfo info, Base base)
		{
			int tilemod = info.getModWert();
			if (tilemod > 0)
			{
				base.setCoreActive(false);
				base.setCore(null);
			}
		}

		@Override
		public boolean checkUpgrade(UpgradeInfo upgrade, Base base)
		{
			Optional<UpgradeMaxValues> upgrademaxvalue = base.getKlasse().getUpgradeMaxValues().stream().filter(u -> u.getUpgradeType() == UpgradeType.CORE).findFirst();
			boolean hasCore = base.getCore() != null;
			if (upgrademaxvalue.isPresent() && hasCore)
			{
				int maxvalue = upgrademaxvalue.get().getMaximalwert();
				return maxvalue > 0;
			}
			return false;
		}

		@Override
		public String errorMsg()
		{
			return "Dieser Asteroid hat keinen eingebauten Core, oder er kann nicht entfernt werden.";
		}

		@Override
		public String getUpgradeText(int modwert)
		{
			if (modwert > 0)
			{
				return "Core entfernen";
			}
			return "Core nicht entfernen";
		}
	},
	FIELD("FieldUpgrade", "Felder-Ausbau")
	{
		@Override
		public void doWork(UpgradeInfo info, Base base)
		{
			int tilemod = info.getModWert();
			int actualtiles = base.getHeight() * base.getWidth();
			int supposedtiles = actualtiles + tilemod;
			if (tilemod > 0)
			{
				int height = (int) Math.sqrt(supposedtiles);
				while (supposedtiles % height != 0)
				{
					height--;
				}
				int width = supposedtiles / height;
				base.setHeight(height);
				base.setWidth(width);
			}
		}

		@Override
		public boolean checkUpgrade(UpgradeInfo upgrade, Base base)
		{
			Optional<UpgradeMaxValues> upgrademaxvalue = base.getKlasse().getUpgradeMaxValues().stream().filter(u -> u.getUpgradeType() == UpgradeType.FIELD).findFirst();
			if (upgrademaxvalue.isPresent())
			{
				int actualvalue = base.getWidth() * base.getHeight();
				int maxvalue = upgrademaxvalue.get().getMaximalwert();
				return actualvalue + upgrade.getModWert() <= maxvalue;
			}
			return false;
		}

		@Override
		public String errorMsg()
		{
			return "Dieser Auftrag überschreitet die Maximalgrenze der Felderausbauten.";
		}

		@Override
		public String getUpgradeText(int modwert)
		{
			return "+" + Common.ln(modwert) + " Felder";
		}
	},
	CARGO("CargoUpgrade", "Cargo-Ausbau")
	{
		@Override
		public void doWork(UpgradeInfo info, Base base)
		{
			int cargomod = info.getModWert();
			base.setMaxCargo(base.getMaxCargo() + cargomod);
		}

		@Override
		public boolean checkUpgrade(UpgradeInfo upgrade, Base base)
		{
			Optional<UpgradeMaxValues> upgrademaxvalue = base.getKlasse().getUpgradeMaxValues().stream().filter(u -> u.getUpgradeType() == UpgradeType.CARGO).findFirst();
			if (upgrademaxvalue.isPresent())
			{
				long actualvalue = base.getMaxCargo();
				int maxvalue = upgrademaxvalue.get().getMaximalwert();
				return actualvalue + upgrade.getModWert() <= maxvalue;
			}
			return false;
		}

		@Override
		public String errorMsg()
		{
			return "Dieser Auftrag überschreitet die Maximalgrenze der Cargoausbauten.";
		}

		@Override
		public String getUpgradeText(int modwert)
		{
			return "+" + Common.ln(modwert) + " Cargo";
		}
	},
    EPS("EPSUpgrade", "Energiespeicher-Ausbau")
    {
        @Override
        public void doWork(UpgradeInfo info, Base base)
        {
            int mod = info.getModWert();
            base.setMaxEnergy(base.getMaxEnergy() + mod);
        }

        @Override
        public boolean checkUpgrade(UpgradeInfo upgrade, Base base)
        {
            Optional<UpgradeMaxValues> upgrademaxvalue = base.getKlasse().getUpgradeMaxValues().stream().filter(u -> u.getUpgradeType() == UpgradeType.EPS).findFirst();
            if (upgrademaxvalue.isPresent())
            {
                long actualvalue = base.getMaxEnergy();
                int maxvalue = upgrademaxvalue.get().getMaximalwert();
				return actualvalue + upgrade.getModWert() <= maxvalue;
            }
            return false;
        }

        @Override
        public String errorMsg()
        {
            return "Dieser Auftrag überschreitet die Maximalgrenze des Energiespeichers.";
        }

        @Override
        public String getUpgradeText(int modwert)
        {
            return "+" + Common.ln(modwert) + " Energiespeicher";
        }
    };

	private final String name;
	private final String description;

	/**
	 * Leerer Konstruktor.
	 * @param name Der Name des Ausbaus
	 * @param description Die Beschreibung des Ausbaus
	 */
    UpgradeType(String name, String description)
	{
		this.name = name;
		this.description = description;
	}

	/**
	 * Gibt den Namen des UpgradeTyps zurueck.
	 *
	 * @return der Name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gibt die Beschreibung des UpgradeTyps zurueck.
	 *
	 * @return die Beschreibung
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * Fuehrt den Ausbau zu diesem Job durch.
	 *
	 * @param info der Ausbau der durchgefuhert werden soll
	 */
	public abstract void doWork(UpgradeInfo info, Base base);

	/**
	 * Prueft, ob dieses upgrade, bei gegebenen Maximalwerten noch ausgebaut werden darf.
	 *
	 * @param upgrade das Upgrade das geprueft werden soll
	 * @param base der Asteroid fuer den geprueft werden soll
	 * @return <code>true</code>, falls dieses upgrade durchgefuhert werden darf, sonst <code>false</code>
	 */
	public abstract boolean checkUpgrade(UpgradeInfo upgrade, Base base);

	/**
	 * Gibt die Fehlermeldung zurueck, die angezeigt wird, wenn dieser Typ nicht zulaessig ist.
	 *
	 * @return Die Fehlermeldung
	 */
	public abstract String errorMsg();

	/**
	 * Gibt den Text bei der Auswahl dieses Typs zurueck.
	 *
	 * @return der Text zur Auswahl
	 */
	public abstract String getUpgradeText(int modwert);
}
