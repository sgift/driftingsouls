package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.DBTest;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.units.UnitType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for the two queries in {@link User} that compared an association against a
 * parameter bound to an id: {@code Forschung.req1/req2/req3} in {@code dropResearch} and
 * {@code UnitCargoEntry.unittype} in {@code isKnownUnit}.
 *
 * <p>Same defect class as {@code WerftObjectRepairCostsTest}: the old Hibernate {@code Session}
 * bound ints against the foreign key column unchecked, while {@code EntityManager.setParameter}
 * type-checks and throws {@code IllegalArgumentException: Parameter value [..] did not match
 * expected type}.
 */
public class UserAssociationBindingTest extends DBTest
{
	@Test
	public void dropResearch_forAResearchOthersDependOn_dropsThoseToo()
	{
		mitTransaktion(() -> {
			User user = persist(new User("owner", "***", 0, "", new Cargo(), "owner@localhost"));

			Forschung basis = persist(forschung("Basis"));
			Forschung dependent = persist(forschung("Abhaengig"));
			dependent.setReq1(basis);

			user.addResearch(basis);
			user.addResearch(dependent);

			user.dropResearch(basis);

			assertFalse("The dependent research should have been dropped along with its requirement",
					user.hasResearched(dependent));
			assertFalse(user.hasResearched(basis));
		});
	}

	@Test
	public void isKnownUnit_forAHiddenUnitTypeTheUserDoesNotOwn_isFalse()
	{
		mitTransaktion(() -> {
			User user = persist(new User("owner", "***", 0, "", new Cargo(), "owner@localhost"));

			UnitType unitType = new UnitType();
			unitType.setName("Testeinheit");
			unitType.setBuildCosts(new Cargo());
			unitType.setPicture("none.png");
			unitType.setRes(persist(forschung("Einheitenforschung")));
			// Only hidden types reach the queries under test; visible ones return early.
			unitType.setHidden(true);
			persist(unitType);

			assertFalse(user.isKnownUnit(unitType));

			unitType.setHidden(false);
			assertTrue(user.isKnownUnit(unitType));
		});
	}

	private Forschung forschung(String name)
	{
		Forschung forschung = new Forschung();
		forschung.setName(name);
		forschung.setCosts(new Cargo());
		forschung.setDescription("");
		return forschung;
	}
}
