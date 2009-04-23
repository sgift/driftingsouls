package net.driftingsouls.ds2.server.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Ein Newseintrag.
 * 
 * @author Sebastian Gift
 */
@Entity
@Table(name="portal_news")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class NewsEntry
{

	@Id
	private int id;
	private String title;
	private String author;
	private long date;
	@Column(name="txt")
	private String description;
	
	/**
	 * @return ID des Newseintrags.
	 */
	public int getId()
	{
		return id;
	}
	
	/**
	 * @return Titel des Newseintrags.
	 */
	public String getTitle()
	{
		return title;
	}
	
	/**
	 * @return Autor, html-formatiert.
	 */
	public String getAuthor()
	{
		return author;
	}
	
	/**
	 * @return Datum des Eintrags in Sekunden.
	 */
	public long getDate()
	{
		return date;
	}
	
	/**
	 * @return Text des Newseintrags.
	 */
	public String getDescription()
	{
		return description;
	}
}
