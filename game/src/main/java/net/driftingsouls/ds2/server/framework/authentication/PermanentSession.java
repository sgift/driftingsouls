package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.framework.BasicUser;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
	@Id @GeneratedValue(strategy= GenerationType.IDENTITY)
	private Integer id;
	@ManyToOne(optional = false)
	@JoinColumn(name="userId", nullable = false)
	@ForeignKey(name="sessions_fk_users")
	private BasicUser user;
	@Column(nullable = false)
	private String token;
	private long tick;
	private int version;

	/**
	 * Konstruktor.
	 */
	public PermanentSession()
	{
		this.token = "";
	}
	
	/**
	 * @return Die Id.
	 */
	public Integer getId()
	{
		return id;
	}

	/**
	 * @param id Die Id.
	 */
	public void setId(Integer id)
	{
		this.id = id;
	}

	/**
	 * @return Der User.
	 */
	public BasicUser getUser()
	{
		return user;
	}
	
	/**
	 * User setzen.
	 * 
	 * @param user Der User
	 */
	public void setUser(BasicUser user)
	{
		this.user = user;
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
