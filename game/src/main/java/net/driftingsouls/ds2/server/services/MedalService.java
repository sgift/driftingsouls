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
package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.entities.UserRank;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Repraesentiert die Liste aller in DS bekannten Orden und Raenge.
 *
 * @author Christopher Jung
 *
 */
@Service
public class MedalService {
	@PersistenceContext
	private EntityManager em;
	
	/**
	 * Gibt den Rang mit der angegebenen ID zurueck.
	 * Falls kein Rang mit der ID existiert wird der 
	 * Standardrang zurueckgegeben
	 * 
	 * @param id Die ID des gewuenschten Ranges
	 * @return Der Rang oder der Standardrang
	 */
	public Rang rang( int id ) {
		Rang rang = em.find(Rang.class, id);
		if( rang == null )
		{
			rang = em.find(Rang.class, 0);
		}
		return rang;
	}
	
	/**
	 * Gibt die Liste der Raenge zurueck.
	 * @return die Liste der Raenge
	 */
	public Map<Integer,Rang> raenge() {
		List<Rang> raenge = em.createQuery("from Rang", Rang.class).getResultList();
		return new TreeMap<>(raenge.stream().collect(Collectors.toMap(Rang::getId, r -> r)));
	}
	
	/**
	 * Gibt den Orden mit der angegebenen ID zurueck.
	 * Falls kein Orden mit der ID existiert wird <code>null</code>
	 * zurueckgegeben.
	 * 
	 * @param id Die ID des gewuenschten Ordens
	 * @return Der Orden oder <code>null</code>
	 */
	public Medal medal( int id ) {
		return em.find(Medal.class, id);
	}
	
	/**
	 * Gibt die Liste der Orden zurueck.
	 * @return die Liste der Orden
	 */
	public List<Medal> medals() {
		return em.createQuery("from Medal", Medal.class).getResultList();
	}
}
