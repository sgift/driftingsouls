/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Sebastian Gift
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
package net.driftingsouls.ds2.server.scripting.transfer;

/**
 * Factoryklasse fuer Transferstrategien. Eine Transferstrategie erlaubt den
 * Transfer von Resourcen von einem Objekt zu einem Anderen.
 * @author Sebastian Gift
 *
 */
public class TransferStrategyFactory {
	/**
	 * Returns the strategy based on wished strategy and reversed parameter.
	 * Correct owners are enforced (same owner when way is reversed).
	 * 
	 * @param transferType Strategy the scriptwriter wanted.
	 * @param reversed True if the strategy has to be adjusted because we transfer from the second ship.
	 * @param from Ship that transfers.
	 * @param to Shio that gets resources.
	 * @return Strategy for transfering.
	 */
	public static TransferStrategy getStrategy(String transferType, boolean reversed, int from, int to) {
		//Andersrum? Dann mal umdrehen das Ding
		boolean forceSameOwner = false;
		if( reversed ) {
			transferType = new StringBuilder(transferType).reverse().toString();
			forceSameOwner = true;
		}

		switch (transferType)
		{
			case "sts":
				return new StsTransfer(from, to, forceSameOwner);
			case "stb":
				return new StbTransfer(from, to, forceSameOwner);
			case "bts":
				return new BtsTransfer(from, to, forceSameOwner);
			case "btb":
				return new BtbTransfer(from, to, forceSameOwner);
		}
		
		return null;
	}
}
