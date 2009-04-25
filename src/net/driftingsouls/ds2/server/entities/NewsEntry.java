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
	/**
	 * Fuer Hibernate.
	 */
	public NewsEntry()
	{}
	
	/**
	 * Legt einen neuen Newseintrag an.
	 * 
	 * @param title Der Titel des Eintrags.
	 * @param author Der Autor.
	 * @param date Das Datum, im DS-Datumsformat.
	 * @param shortDescription Eine kurze Beschreibung.
	 * @param text Der eigentliche Eintrag.
	 */
	public NewsEntry(String title, String author, long date, String shortDescription, String text)
	{
		this.title = title;
		this.author = author;
		this.date = date;
		this.shortDescription = shortDescription;
		this.newsText = text;
	}
	
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
	 * @return Datum des Eintrags in Sekunden/1000.
	 */
	public long getDate()
	{
		return date;
	}
	
	/**
	 * @return Text des Newseintrags.
	 */
	public String getNewsText()
	{
		return newsText;
	}
	
	/**
	 * @return Die Kurzbeschreibung des Newseintrags.
	 */
	public String getShortDescription()
	{
		return shortDescription;
	}
	
	@Id
	private int id;
	private String title;
	private String author;
	private long date;
	private String shortDescription;
	@Column(name="txt")
	private String newsText;
	
}
