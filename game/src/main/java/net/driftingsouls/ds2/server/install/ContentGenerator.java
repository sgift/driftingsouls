package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.*;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeInfo;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeMaxValues;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeType;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static net.driftingsouls.ds2.server.install.InstallUtils.mitContextUndSession;
import static net.driftingsouls.ds2.server.install.InstallUtils.mitTransaktion;

public class ContentGenerator
{
	private static final Logger LOG = LogManager.getLogger(ContentGenerator.class);

	public void generiereContent()
	{
		mitContextUndSession((context) -> {
			Session db = context.getDB();

			mitTransaktion("Setze Konfigurationseinstellungen", () -> {
				new ConfigService().get(WellKnownConfigValue.ENABLE_CHEATS).setValue("true");
				new ConfigService().get(WellKnownConfigValue.REGISTER_PM_SENDER).setValue("-10");
			});

			mitTransaktion("Erzeuge Sternensysteme", () -> new StarSystemContentGenerator().generiereSysteme(5));

			mitTransaktion("Erzeuge Modulslots", () -> new SchiffContentGenerator().erzeugeModulSlots());

			mitTransaktion("Erzeuge Tutorial", () -> {
				IntTutorial tutorial = new IntTutorial();
				tutorial.setReqBase(true);
				tutorial.setReqShip(true);
				tutorial.setReqName(false);
				tutorial.setHeadImg("data/interface/interactivetutorial/ars.gif");
				tutorial.setText("[font=arial]Guten Tag!\r\n\r\nWenn ich mich kurz vorstellen darf - Mein Name ist Konteradmiral Korrow und ich bin f&uuml;r die Betreuung neuer Siedler in den so genannten Lost Lands zust&auml;ndig. Also auch f&uuml;r Sie.\r\nIch unterstehe dem GCP Kolonialamt, das die Kolonisation der Asteroiden in diesen Systemen f&ouml;rdert. Sollten sie sp&auml;ter weitere Fragen haben, schicken Sie diese bitte an die ID -16. Die Fragen werden dann automatisch an mich weitergeleitet.[/font]");
				db.persist(tutorial);

				IntTutorial tutorial2 = new IntTutorial();
				tutorial2.setReqBase(true);
				tutorial2.setReqShip(true);
				tutorial2.setReqName(false);
				tutorial2.setHeadImg("data/interface/interactivetutorial/ars.gif");
				tutorial2.setText("[font=arial]Gut, Sie haben sich jetzt einen individuellen Namen gegeben. Weitere wichtige Punkte sind:\r\nDie Zeit in Drifting-Souls l&auml;uft in Ticks. Zwischen den Ticks k&ouml;nnen Sie frei agieren, bis Ihnen die Energie ausgeht oder die Antriebe vergl&uuml;hen. W&auml;hrend der Ticks sollten Sie keine Handlungen vornehmen, auch wenn dies m&ouml;glich ist, denn dann wird die Energieerzeugung der Schiffe, Produktion der Anlagen und Abk&uuml;hlung der Triebwerke berechnet. Die Ticks beginnen jeweils um 0, 4, 12, 16, 18, 20 und 22 Uhr MEZ bzw MESZ und dauern ca 15 Minuten - je drei Ticks sind ein Ingame-Tag.\r\nWenn Sie nun die Basenansicht aufrufen, werden Sie Ihren ersten besiedelten Asteroiden sehen k&ouml;nnen. Er verf&uuml;gt bereits &uuml;ber einige Geb&auml;ude und in seinen Lagerr&auml;umen sind auch Ressourcen verschiedener Art gelagert. \r\nWenn Sie den Namen oder die ID anklicken kommen Sie zur Asteroiden-Detail-Ansicht - wenn Sie den Mauszeiger &uuml;ber den Geb&auml;uden verweilen lassen, wird deren Bezeichnung eingeblendet. In DS wird von diesen Tooltipps h&auml;ufig Gebrauch gemacht und Sie k&ouml;nnen oft zus&auml;tzliche Kurz-Informationen dadurch erhalten. Sehen Sie sich Ihren Asteroiden nun ruhig an, kehren Sie dann hierher zur&uuml;ck und schalten Sie weiter.[/font]");
				db.persist(tutorial2);

				IntTutorial tutorial3 = new IntTutorial();
				tutorial3.setReqBase(true);
				tutorial3.setReqShip(true);
				tutorial3.setReqName(false);
				tutorial3.setHeadImg("data/interface/interactivetutorial/ars.gif");
				tutorial3.setText("[font=arial]Es empfiehlt sich den Tanker im Nebel stehen zu lassen und das Deuterium [img]http://ds2.drifting-souls.net/current/data/resources/Deuterium.gif[/img] jeweils mit dem Frachter abzuholen.\r\nNoch ein Wort zur Versorgung der Schiffe: Alle Produktion von Nahrung fliesst in den sogenannten Pool, aus dem im Tick Nahrung f&uuml;r Bev&ouml;lkerung auf den Asteroiden und Besatzungen auf den Schiffen entnommen wird, solange Ihre Produktion im positiven Bereich bleibt - zu sehen in der &Uuml;bersicht nach dem Nahrungssymbol [img]http://ds2.drifting-souls.net/current/data/resources/Nahrung.gif[/img] - wird nichts passieren. Sollten Sie aber zu viele Schiffe haben, kann es sein, dass sich der Pool leert - in diesem Falle werden Besatzungen auf den Schiffen verhungern und k&ouml;nnen von anderen Siedlern oder Piraten gekapert werden. Der Pool kann mit gekaufter Nahrung beladen werden, in dem Sie sie auf dem Asteroiden abladen und mit Hilfe des gr&uuml;nen Pfeils hinter Nahrung in der Asteroiden-Detail-Ansicht die Menge angeben, die sie in den Pool laden wollen (Angabe einer negativen Zahl entl&auml;dt den Pool)\r\n\r\nSoweit meine kleine Einf&uuml;hrung.\r\n\r\nDenken Sie daran, Sie k&ouml;nnen mich auch jeder Zeit via PM an ID -16 weitere Dinge fragen.\r\nEs lohnt sich &uuml;brigens auch, mal im IRC (irc.euirc.net - #ds und #ds-help) vorbei zu schauen. Dort finden sich garantiert Siedler, die Ihnen umsonst oder zu einem geringen Preis einige Rohstoffe &uuml;berlassen oder wertvolle Tipps geben k&ouml;nnen. Ein Java-Chat-Client befindet sich auf der Startseite ( oder hier: [url=http://ds.drifting-souls.net/index.php?action=javachat]Java-Chat[/url] )\r\n\r\nDer komplette Text dieser Einf&uuml;hrung ist auch in der Wiki zu finden [url=http://wiki.drifting-souls.net/Das_Ingame-Tutorial]Tutorial[/url]\r\n\r\nM&ouml;gen Sie viel Erfolg in den Lost Lands haben![/font]");
				db.persist(tutorial3);
			});

			mitTransaktion("Erzeuge Raenge", () -> {
				db.persist(new Rang(0, "data/interface/medals/rang0.png", "Ensign"));
				db.persist(new Rang(1, "data/interface/medals/rang1.png", "Leutnant"));
				db.persist(new Rang(2, "data/interface/medals/rang2.png", "Oberleutnant"));
				db.persist(new Rang(3, "data/interface/medals/rang3.png", "Kapitän"));
				db.persist(new Rang(4, "data/interface/medals/rang4.png", "Kommodore"));
				db.persist(new Rang(5, "data/interface/medals/rang5.png", "Flottenkommodore"));
				db.persist(new Rang(6, "data/interface/medals/rang6.png", "Konteradmiral"));
				db.persist(new Rang(7, "data/interface/medals/rang7.png", "Vizeadmiral"));
				db.persist(new Rang(8, "data/interface/medals/rang8.png", "Admiral"));
				db.persist(new Rang(9, "data/interface/medals/rang9.png", "Flottenadmiral"));
				db.persist(new Rang(10, "data/interface/medals/rang10.png", "Sternenadmiral"));
				db.persist(new Rang(11, "data/interface/medals/rang11.png", "Oberkommandierender"));
				db.persist(new Rang(12, "data/interface/medals/rang12.png", "Oberkommandierender"));
				db.persist(new Rang(13, "data/interface/medals/rang13.png", "Oberkommandierender"));
				db.persist(new Rang(14, "data/interface/medals/rang14.png", "Oberkommandierender"));
				db.persist(new Rang(15, "data/interface/medals/rang15.png", "Oberkommandierender"));
				db.persist(new Rang(16, "data/interface/medals/rang16.png", "Oberkommandierender"));
				db.persist(new Rang(99, "data/interface/medals/rang99.png", "Admin"));
				db.persist(new Rang(100, "data/interface/medals/rang100.png", "Gott"));
			});

			mitTransaktion("Erzeuge Orden", () -> {
				db.persist(new Medal("Orden der Tapferkeit", "data/interface/medals/tapferkeit1.png", "data/interface/medals/tapferkeit1_s.png"));
				db.persist(new Medal("Drifting Souls 2 Team", "data/interface/medals/ds2team.png", "data/interface/medals/ds2team_s.png"));
			});

			mitTransaktion("Erzeuge Fraktionsmaske", () -> {
				db.persist(new FraktionsGuiEintrag((User) db.get(User.class, -2), FraktionsGuiEintrag.Seite.VERSTEIGERUNG, FraktionsGuiEintrag.Seite.SHOP, FraktionsGuiEintrag.Seite.SONSTIGES));
				db.persist(new FraktionsGuiEintrag((User) db.get(User.class, -15), FraktionsGuiEintrag.Seite.AKTION_MELDEN));
				db.persist(new FraktionsGuiEintrag((User) db.get(User.class, -1), FraktionsGuiEintrag.Seite.BANK));
				FraktionsGuiEintrag di = new FraktionsGuiEintrag((User) db.get(User.class, -19), FraktionsGuiEintrag.Seite.ALLGEMEIN, FraktionsGuiEintrag.Seite.AUSBAU);
				di.setText("Demolition Incorporated ist das führende Unternehmen auf dem Gebiet des modernen Bergbaus.");
				db.persist(di);
			});

			mitTransaktion("Erzeuge ComNet-Kanaele", this::erzeugeComNetKanaele);

			mitTransaktion("Erzeuge NPC-Bestelldaten", () -> {
				db.persist(new OrderableOffizier("Ingenieur", 1, 0, 25, 20, 10, 5, 5));
				db.persist(new OrderableOffizier("Navigator", 1, 0, 5, 10, 30, 5, 10));
				db.persist(new OrderableOffizier("Sicherheitsexperte", 1, 0, 10, 25, 5, 35, 5));
				db.persist(new OrderableOffizier("Captain", 1, 0, 10, 10, 15, 5, 35));

				Rasse rasse = (Rasse) db.get(Rasse.class, 0);
				List<ShipType> shipTypes = Common.cast(db.createCriteria(ShipType.class).add(Restrictions.sqlRestriction("1=1 order by rand()"))
						.setMaxResults(10)
						.list());
				for (ShipType shipType : shipTypes)
				{
					db.persist(new OrderableShip(shipType, rasse, (int) (10 * Math.random())));
				}
			});

            mitTransaktion("Erzeuge Werte fuer Basis-Upgrades",
                    () -> Common.cast(db.createCriteria(BaseType.class).list()),
                    (BaseType bt) -> {
                        db.persist(new UpgradeMaxValues(bt, UpgradeType.CARGO, bt.getCargo()*2));
                        int step = Math.max(1000, (bt.getCargo()*2 - bt.getCargo()) / 5);
                        for (int i = 1; i * step <= (bt.getCargo()*2 - bt.getCargo()); i++) {
                            UpgradeInfo info = new UpgradeInfo(bt, UpgradeType.CARGO);
                            info.setModWert(i * step);
                            info.setMiningExplosive(100 + i * 50);
                            info.setOre(1000 + i * 250);
                            info.setPrice(10000 + i * 10000);
                            db.persist(info);
                        }
                        db.persist(new UpgradeMaxValues(bt, UpgradeType.FIELD, (int) (bt.getMaxTiles() * 1.5)));
                        step = Math.max(5, ((int)(bt.getMaxTiles()*1.5) - bt.getCargo()) / 5);
                        for (int i = 1; i * step <= (int)(bt.getMaxTiles()*1.5) - bt.getCargo(); i++) {
                            UpgradeInfo info = new UpgradeInfo(bt, UpgradeType.FIELD);
                            info.setModWert(i * step);
                            info.setMiningExplosive(50 + i * 30);
                            info.setOre(2000 + i * 500);
                            info.setPrice(20000 + i * 30000);
                            db.persist(info);
                        }
                    }
            );

			mitTransaktion("Fuelle Systeme mit Inhalt",
					() -> Common.cast(db.createCriteria(StarSystem.class).list()),
					(StarSystem sys) -> new StarSystemContentGenerator().fuelleSystem(sys)
			);

			mitTransaktion("Erzeuge Sprungpunkte zwischen den Systemen", () -> new StarSystemContentGenerator().generiereSprungpunkte());

			mitTransaktion("Kolonisiere Asteroiden mit bestehenden Benutzern",
					() -> Common.cast(db.createCriteria(User.class).add(Restrictions.ne("id", 0)).list()),
					(User user) -> kolonisiereAsteroidenFuer(user, ThreadLocalRandom.current().nextInt(1, 9)));

			mitTransaktion("Erzeuge Newseintrag", () -> {
				NewsEntry news = new NewsEntry(
						"Willkommen bei DS2",
						"Installationsprogramm",
						Common.time(),
						"Willkommen bei DS2",
						"Willkommen bei DS2\r\nDu hasst den Demodatensatz erfolgreich installiert.\r\nViel Spass beim Testen und Patchen!");

				db.persist(news);
			});

			mitTransaktion("Erzeuge Verkaufsdaten fuer GTU", () -> new GtuContentGenerator().erzeugeVerkaufsdaten());

			mitTransaktion("Erzeuge Handelsposten",
					() -> Common.cast(db.createCriteria(StarSystem.class).add(Restrictions.gt("id", 0)).list()),
					(StarSystem sys) -> new GtuContentGenerator().erzeugeHandelspostenInSystem(sys)
			);
		});
	}

	private void erzeugeComNetKanaele()
	{
		Session db = ContextMap.getContext().getDB();

		ComNetChannel standard = new ComNetChannel("Standard");
		standard.setReadAll(true);
		standard.setWriteAll(true);
		db.persist(standard);

		ComNetChannel notfrequenz = new ComNetChannel("Notfrequenz");
		notfrequenz.setReadAll(true);
		notfrequenz.setWriteAll(true);
		db.persist(notfrequenz);

		ComNetChannel gnn = new ComNetChannel("GNN News Network");
		gnn.setReadAll(true);
		gnn.setWriteNpc(true);
		db.persist(gnn);

		ComNetChannel nonRpg = new ComNetChannel("Non-RPG");
		nonRpg.setReadAll(true);
		nonRpg.setWriteNpc(true);
		db.persist(nonRpg);
	}

	private void kolonisiereAsteroidenFuer(User user, int anzahl)
	{
		LOG.info("Kolonisiere "+anzahl+" Basen fuer Benutzer "+user.getId());

		Session db = ContextMap.getContext().getDB();
		User nullUser = (User)db.get(User.class, 0);

		List<Base> basen = Common.cast(db.createCriteria(Base.class).add(Restrictions.eq("owner", nullUser)).add(Restrictions.sqlRestriction("1=1 order by rand()"))
				.setMaxResults(anzahl)
				.list());
		for( Base b : basen )
		{
			Integer[] bebauung = b.getBebauung();
			Integer[] active = b.getActive();
			bebauung[0] = 1;
			active[0] = 1;
			b.setBebauung(bebauung);
			b.setActive(active);

			b.setOwner(user);
			BaseStatus status = Base.getStatus(b);
			b.setEnergy(status.getEnergy());
			b.setBewohner(status.getLivingSpace());

			Cargo cargo = status.getProduction();
			for (ResourceEntry resourceEntry : cargo.getResourceList())
			{
				cargo.addResource(resourceEntry.getId(), Math.abs(resourceEntry.getCount1() * 10000));
			}

			b.setCargo(cargo);
		}
	}
}
