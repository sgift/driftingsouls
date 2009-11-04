package net.driftingsouls.ds2.server.framework.authentication;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Repraesentiert einen "Remember Me"-Token in der Datenbank.
 * 
 * @author Sebastian Gift
 */
@Entity
@Table(name="sessions")
public class PermanentSession
{
	@Id
	private int id;
	private int userId;
	private String token;
	private long tick;
	private boolean useGfxPack;
	private int version;
	
	/**
	 * @return Die Id.
	 */
	public int getId() 
	{
		return id;
	}

	/**
	 * @param id Die Id.
	 */
	public void setId(int id) 
	{
		this.id = id;
	}
	
	/**
	 * @return <code>true</code>, wenn das Grafikpack verwendet werden soll.
	 */
	public boolean isUseGfxPack() 
	{
		return useGfxPack;
	}

	/**
	 * Setzt, ob das Grafikpack verwendet werden soll.
	 * 
	 * @param useGfxPack <code>true</code>, wenn das Grafikpack verwendet werden soll.
	 */
	public void setUseGfxPack(boolean useGfxPack) 
	{
		this.useGfxPack = useGfxPack;
	}

	/**
	 * @return Die user id.
	 */
	public int getUserId() 
	{
		return userId;
	}
	
	/**
	 * User id setzen.
	 * 
	 * @param userId Die user id.
	 */
	public void setUserId(int userId) 
	{
		this.userId = userId;
	}
	
	/**
	 * @return Der Zufallstoken.
	 */
	public String getToken() 
	{
		return token;
	}
	
	/**
	 * Setzt den Zufallstoken.
	 * 
	 * @param token Der Zufallstoken.
	 */
	public void setToken(String token) 
	{
		this.token = token;
	}
	
	
	/**
	 * @return Tick der Erzeugung.
	 */
	public long getTick() 
	{
		return tick;
	}
	
	/**
	 * Setzt den Tick.
	 * 
	 * @param timestamp Tick der Erzeugung.
	 */
	public void setTick(long timestamp) 
	{
		this.tick = timestamp;
	}

	/**
	 * @param version Die Version (Hibernate).
	 */
	public void setVersion(int version) 
	{
		this.version = version;
	}

	/**
	 * @return Die Version (Hibernate).
	 */
	public int getVersion() 
	{
		return version;
	}
}
