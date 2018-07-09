package net.driftingsouls.ds2.server.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

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
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String title;
	@Lob
	@Column(nullable = false)
	private String author;
	private long date;
	@Column(nullable = false)
	@Lob
	private String shortDescription;
	@Column(name="txt", nullable = false)
	@Lob
	private String newsText;

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

	/**
	 * @param title Titel des Newseintrags.
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * @param author Autor, html-formatiert.
	 */
	public void setAuthor(String author)
	{
		this.author = author;
	}

	/**
	 * @param date Datum des Eintrags in Sekunden/1000.
	 */
	public void setDate(long date)
	{
		this.date = date;
	}

	/**
	 * @param shortDescription Die Kurzbeschreibung des Newseintrags.
	 */
	public void setShortDescription(String shortDescription)
	{
		this.shortDescription = shortDescription;
	}

	/**
	 * @param newsText Text des Newseintrags.
	 */
	public void setNewsText(String newsText)
	{
		this.newsText = newsText;
	}
}
