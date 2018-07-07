package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.config.ConfigFelsbrocken;
import net.driftingsouls.ds2.server.config.ConfigFelsbrockenSystem;
import net.driftingsouls.ds2.server.entities.GuiHelpText;
import net.driftingsouls.ds2.server.entities.IntTutorial;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeInfo;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeMaxValues;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.SchiffstypModifikation;
import net.driftingsouls.ds2.server.ships.ShipBaubar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Generator fuer Labels zu Objekten.
 */
public class ObjectLabelGenerator
{
	private  Map<Class<?>, Function<Object,String>> specialLabelGenerators = new HashMap<>();

	public ObjectLabelGenerator()
	{
		registerSpecialGenerator(ShipBaubar.class, (sb) -> sb.getType().getNickname());
		registerSpecialGenerator(User.class, User::getPlainname);
		registerSpecialGenerator(GuiHelpText.class, GuiHelpText::getPage);
		registerSpecialGenerator(FraktionsGuiEintrag.class, (fge) -> fge.getUser().getPlainname());
		registerSpecialGenerator(ResourceEntry.class, ResourceEntry::getPlainName);
		registerSpecialGenerator(ConfigFelsbrockenSystem.class, (cfs) -> cfs.getName()+"#"+cfs.getSystem().getName());
		registerSpecialGenerator(ConfigFelsbrocken.class, (cs) -> cs.getSystem().getName()+"#"+cs.getSystem().getSystem().getName()+"#"+cs.getChance());
		registerSpecialGenerator(IntTutorial.class, (it) -> (it.isReqBase()?"Basis,":"")+(it.isReqShip()?"Schiff,":"")+(it.isReqName()?"Name,":"")+(it.getBenoetigteSeite()!=null?""+it.getBenoetigteSeite().getId():""));
	    registerSpecialGenerator(UpgradeMaxValues.class, (umv) -> umv.getType().getName()+"#"+umv.getUpgradeType());
        registerSpecialGenerator(UpgradeInfo.class, (ui) -> ui.getType().getName()+"#"+ui.getUpgradeType());
		registerSpecialGenerator(SchiffstypModifikation.class, (sm) -> "["+sm.getId()+"] "+sm.toString());
		registerSpecialGenerator(Ally.class, Ally::getPlainname);
		registerSpecialGenerator(OrderableShip.class, (os) -> "["+os.getRasse().getName()+"] "+os.getShipType().getNickname());
    }

	@SuppressWarnings("unchecked")
	private <T> void registerSpecialGenerator(Class<T> cls, Function<T,String> transformer)
	{
		specialLabelGenerators.put(cls, (Function<Object, String>) transformer);
	}

	/**
	 * Generiert ein Label fuer eine Kombination von ID und Objektinstanz.
	 * @param identifier Die ID des Objekts
	 * @param entity Die Objektinstanz
	 * @return Das generierte Label
	 */
	public String generateFor(@Nullable Serializable identifier, @Nonnull Object entity)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		if (identifier == null && entity.getClass().isAnnotationPresent(Entity.class))
		{
			identifier = db.getIdentifier(entity);
		}

		String label;
		if( specialLabelGenerators.containsKey(entity.getClass()) )
		{
			label = specialLabelGenerators.get(entity.getClass()).apply(entity);
		}
		else
		{
			label = generateDefaultLabel(entity);
		}

		if( entity.getClass().isEnum() && label.equals(identifier) )
		{
			return label;
		}

		return label + (identifier != null ? " (" + identifier + ")" : "");
	}

	private String generateDefaultLabel(Object entity)
	{
		Class<?> type = entity.getClass();

		Method labelMethod = null;
		for (String m : Arrays.asList("getName", "getNickname", "toString"))
		{
			try
			{
				labelMethod = type.getMethod(m);
				break;
			}
			catch (NoSuchMethodException e)
			{
				// Ignore
			}
		}

		if( labelMethod == null )
		{
			throw new AssertionError("No toString");
		}

		try
		{
			return labelMethod.invoke(entity).toString();
		}
		catch (IllegalAccessException | InvocationTargetException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
