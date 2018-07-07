package net.driftingsouls.ds2.server.framework;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.proxy.HibernateProxy;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Eine einzelne Permission fuer einen konkreten Benutzer.
 * Die Permission wird durch die Kategorie und die Aktion naeher
 * bestimmt.
 * @author christopherjung
 *
 */
@Entity
@Table(name="permission")
public class Permission implements Comparable<Permission>, PermissionDescriptor
{
	@SuppressWarnings("unused")
	@Id @GeneratedValue
	private int id;
	@SuppressWarnings("unused")
	@ManyToOne
	@JoinColumn(nullable = false)
	@ForeignKey(name="permission_fk_users")
	private BasicUser user;
	@Column(nullable = false)
	private String category;
	@Column(nullable = false)
	private String action;

	protected Permission()
	{
		// EMPTY
	}

	public Permission(BasicUser user, String category, String action)
	{
		this.user = user;
		this.category = category;
		this.action = action;
	}

	@Override
	public String getCategory()
	{
		return category;
	}

	@Override
	public String getAction()
	{
		return action;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if( obj instanceof HibernateProxy )
		{
			obj = ((HibernateProxy)obj).getHibernateLazyInitializer().getImplementation();
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		Permission other = (Permission) obj;
		if (action == null)
		{
			if (other.action != null)
			{
				return false;
			}
		}
		else if (!action.equals(other.action))
		{
			return false;
		}
		if (category == null)
		{
			return other.category == null;
		}
		else {
			return category.equals(other.category);
		}
	}

	@Override
	public int compareTo(@NotNull Permission o)
	{
		int diff = this.category.compareTo(o.category);
		if( diff != 0 )
		{
			return diff;
		}
		return this.action.compareTo(o.action);
	}
}
