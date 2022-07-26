
import java.awt.BasicStroke;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import java.util.TreeMap;
import knižnica.*;
import static knižnica.Kláves.*;
import static knižnica.Svet.*;
import static knižnica.ÚdajeUdalostí.*;

import static debug.Debug.*;

/*
TODO

 • Pridať možnosť hromadnej úpravy jednotlivých parametrov liniek, čiže nie
   viacero parametrov v dialógu naraz, ale len jedného.

 • Dokončiť funkcie undo/redo.

 • Zarovnanie na mriežku. Upraviť parameter mriežky.
 • Úprava vzhľadových vlastností. (pomer, veľkosť, miera zaoblenia, uhol,
   písmo, poloha popisu, viacriadkovosť popisu)
 • (podfarbenie? zmena poradia?)

 ✓ Animovať zákazníkov v dopravníku tak, aby sa postupne presúvali
   od jedného kraja po druhý.

 ✓ Nejako rozmiesniť zákazníkov v zásobníku a čakárni (vedľa seba?, do
   kruhu? – nakoniec to bolo náhodne).

 ✓ S presúvaním linky presúvať aj zákazníkov v nej.

 • Vytvoriť klávesnicový spôsob na vytvorenie spojnice medzi linkami:
    ◦ Označiť začiatok (klávesnicou, myšou… hocijako).
    ◦ Stlačiť klávesovú skratku (ESC to ruší).
    ◦ Označiť koniec (detto).
    ◦ Stlačiť klávesovú skratku – spojnica sa vytvorí.

 ✓ Ctrl + dvojklik na spojnicu ju vymaže.

Ponuka Simulácia

 • Zastaviť/spustiť
 • Reštartovať
 • Zmeniť rýchlosť

*/

public class Systém extends GRobot
{
	// Evidencia:
	public final static Vector<Činnosť> činnosti = new Vector<>();

	// Ikona na označenie položiek ponúk:
	public final static Obrázok ikonaOznačenia = new Obrázok(16, 16);


	// Globálny čas a rôzne príznaky:
	public static double čas = 0;
	public static double dilatácia = 1.0;
	public static boolean pauza = false;
	// public static boolean úniky = false; // TODO del
	// public static int úniky = 0;


	// Globálna štatistika:
	public static int odídených = 0;
	public static int vybavených = 0;
	// public final static TreeMap<Linka, Integer> mapaOdchodov = new TreeMap<>(); // TODO del


	// Konštanty príkazov klávesových skratiek:
	private final static String newSystem = "newSystem";
	private final static String openSystem = "openSystem";
	private final static String saveSystem = "saveSystem";
	private final static String undo = "undo";
	private final static String redo = "redo";
	private final static String selectAll = "selectAll";
	private final static String deselectAll = "deselectAll";
	private final static String selectNext = "selectNext";
	private final static String selectPrevious = "selectPrevious";
	private final static String centerSelection = "centerSelection";
	private final static String deleteSelection = "deleteSelection";
	private final static String duplicateSelection = "duplicateSelection";
	private final static String newLink = "newLink";
	private final static String editLabels = "editLabels";

	private final static String changeToEmitors = "changeToEmitors";
	private final static String changeToBuffers = "changeToBuffers";
	private final static String changeToWaitRooms = "changeToWaitRooms";
	private final static String changeToConveyors = "changeToConveyors";
	private final static String changeToChangers = "changeToChangers";
	private final static String changeToReleasers = "changeToReleasers";

	private final static String changeToEllipses = "changeToEllipses";
	private final static String changeToRectangles = "changeToRectangles";
	private final static String changeToRoundRects = "changeToRoundRects";

	private final static String editParams = "editParams";
	private final static String editVisuals = "editVisuals";
	private final static String runPause = "runPause";
	private final static String restart = "restart";
	private final static String setTimer = "setTimer";


	// Hlavná ponuka:

	private KontextováPoložka položkaZmeňNaEmitory;
	private KontextováPoložka položkaZmeňNaZásobníky;
	private KontextováPoložka položkaZmeňNaČakárne;
	private KontextováPoložka položkaZmeňNaDopravníky;
	private KontextováPoložka položkaZmeňNaMeniče;
	private KontextováPoložka položkaZmeňNaUvoľňovače;

	private KontextováPoložka položkaZmeňNaElipsy;
	private KontextováPoložka položkaZmeňNaObdĺžniky;
	private KontextováPoložka položkaZmeňNaObléObdĺžniky;


	// Rôzne príznaky a pomocné atribúty:
	private boolean posúvajObjekty = false;
	private boolean tvorVýber = false;
	private boolean tvorSpojnicu = false;
	private boolean mažSpojnicu = false;
	private Bod začiatokAkcie = null;
	private Bod koniecAkcie = null;


	// Cieľom súkromnosti konštruktora je vytvorenie jedinej inštancie
	// tejto triedy.
	private Systém()
	{
		// Prvá časť (globálnej) inicializácie:
		super(šírkaZariadenia(), výškaZariadenia(),
			"Simulátor systémov hromadnej obsluhy");
		nekresli();

		čiara(new BasicStroke(2.0f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, 1.0f, new float[]{6.0f, 6.0f}, 0.0f));


		// Zostavenie ovládania a ponuky:

		pridajKlávesovúSkratku(newSystem, VK_N);
		pridajKlávesovúSkratku(openSystem, VK_O);
		pridajKlávesovúSkratku(saveSystem, VK_S);
		pridajKlávesovúSkratku(undo, VK_Z);
		pridajKlávesovúSkratku(redo, VK_Z, SKRATKA_PONUKY | SHIFT_MASK);
		pridajKlávesovúSkratku(selectAll, VK_A);
		pridajKlávesovúSkratku(deselectAll, VK_A, SKRATKA_PONUKY | SHIFT_MASK);
		pridajKlávesovúSkratku(selectNext, VPRAVO);
		pridajKlávesovúSkratku(selectPrevious, VĽAVO);
		pridajKlávesovúSkratku(centerSelection, VK_H);
		pridajKlávesovúSkratku(duplicateSelection, VK_D);
		pridajKlávesovúSkratku(deleteSelection, VK_DELETE, 0);
		pridajKlávesovúSkratku(newLink, VK_M);
		pridajKlávesovúSkratku(editLabels, VK_F2, 0);

		pridajKlávesovúSkratku(changeToEmitors, VK_1);
		pridajKlávesovúSkratku(changeToBuffers, VK_2);
		pridajKlávesovúSkratku(changeToWaitRooms, VK_3);
		pridajKlávesovúSkratku(changeToConveyors, VK_4);
		pridajKlávesovúSkratku(changeToChangers, VK_5);
		pridajKlávesovúSkratku(changeToReleasers, VK_6);

		pridajKlávesovúSkratku(changeToEllipses, VK_1,
			SKRATKA_PONUKY | SHIFT_MASK);
		pridajKlávesovúSkratku(changeToRectangles, VK_2,
			SKRATKA_PONUKY | SHIFT_MASK);
		pridajKlávesovúSkratku(changeToRoundRects, VK_3,
			SKRATKA_PONUKY | SHIFT_MASK);

		pridajKlávesovúSkratku(editParams, VK_F9, 0);
		pridajKlávesovúSkratku(editVisuals, VK_F7, 0);
		pridajKlávesovúSkratku(runPause, VK_F5, 0);
		pridajKlávesovúSkratku(restart, VK_F5);
		pridajKlávesovúSkratku(setTimer, VK_F3, 0);

		vymažPonuku();

		pridajPoložkuHlavnejPonuky("Súbor", VK_S);
		pridajPoložkuPonuky("Nový systém", VK_N).príkaz(newSystem);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Otvoriť systém…", VK_O).príkaz(openSystem);
		pridajPoložkuPonuky("Uložiť systém", VK_U).príkaz(saveSystem);
		pridajOddeľovačPonuky();
		pridajPoložkuPonukyPrekresliť();
		pridajPoložkuPonukyKoniec();

		pridajPoložkuHlavnejPonuky("Úpravy", VK_U);
		pridajPoložkuPonuky("Späť", VK_T).príkaz(undo);
		pridajPoložkuPonuky("Znova", VK_T).príkaz(redo);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Označ všetky linky", VK_A).príkaz(selectAll);
		pridajPoložkuPonuky("Zruš označenie linek", VK_Z).príkaz(deselectAll);
		pridajPoložkuPonuky("Označ nasledujúcu linku",
			VK_N).príkaz(selectNext);
		pridajPoložkuPonuky("Označ predchádzajúcu linku",
			VK_P).príkaz(selectPrevious);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Vystreď označenie", VK_S).príkaz(centerSelection);
		pridajPoložkuPonuky("Duplikuj označenie",
			VK_S).príkaz(duplicateSelection);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Vymaž označené…", VK_V).príkaz(deleteSelection);

		pridajPoložkuHlavnejPonuky("Linka", VK_L);
		pridajPoložkuPonuky("Nová linka", VK_N).príkaz(newLink);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Uprav popisy označených…",
			VK_P).príkaz(editLabels);


		položkaZmeňNaEmitory = new KontextováPoložka("Zmeň na emitory");
		položkaZmeňNaEmitory.setMnemonic(VK_E);
		položkaZmeňNaEmitory.príkaz(changeToEmitors);
		položkaZmeňNaZásobníky = new KontextováPoložka("Zmeň na zásobníky");
		položkaZmeňNaZásobníky.setMnemonic(VK_Z);
		položkaZmeňNaZásobníky.príkaz(changeToBuffers);
		položkaZmeňNaČakárne = new KontextováPoložka("Zmeň na čakárne");
		položkaZmeňNaČakárne.setMnemonic(VK_A);
		položkaZmeňNaČakárne.príkaz(changeToWaitRooms);
		položkaZmeňNaDopravníky = new KontextováPoložka("Zmeň na dopravníky");
		položkaZmeňNaDopravníky.setMnemonic(VK_D);
		položkaZmeňNaDopravníky.príkaz(changeToConveyors);
		položkaZmeňNaMeniče = new KontextováPoložka("Zmeň na meniče");
		položkaZmeňNaMeniče.setMnemonic(VK_M);
		položkaZmeňNaMeniče.príkaz(changeToChangers);
		položkaZmeňNaUvoľňovače = new KontextováPoložka("Zmeň na uvoľňovače");
		položkaZmeňNaUvoľňovače.setMnemonic(VK_U);
		položkaZmeňNaUvoľňovače.príkaz(changeToReleasers);

		pridajVnorenúPonuku("Zmeň funkciu (účel) označených",
			položkaZmeňNaEmitory, položkaZmeňNaZásobníky,
			položkaZmeňNaČakárne, položkaZmeňNaDopravníky,
			položkaZmeňNaMeniče, položkaZmeňNaUvoľňovače).setMnemonic(VK_F);


		položkaZmeňNaElipsy = new KontextováPoložka("Zmeň na elipsy");
		položkaZmeňNaElipsy.setMnemonic(VK_E);
		položkaZmeňNaElipsy.príkaz(changeToEllipses);
		položkaZmeňNaObdĺžniky = new KontextováPoložka("Zmeň na obdĺžniky");
		položkaZmeňNaObdĺžniky.setMnemonic(VK_O);
		položkaZmeňNaObdĺžniky.príkaz(changeToRectangles);
		položkaZmeňNaObléObdĺžniky = new KontextováPoložka(
			"Zmeň na oblé obdĺžniky");
		položkaZmeňNaObléObdĺžniky.setMnemonic(VK_B);
		položkaZmeňNaObléObdĺžniky.príkaz(changeToRoundRects);

		pridajVnorenúPonuku("Zmeň tvar označených", položkaZmeňNaElipsy,
			položkaZmeňNaObdĺžniky, položkaZmeňNaObléObdĺžniky).
		setMnemonic(VK_T);


		pridajPoložkuPonuky("Uprav koeficienty označených…",
			VK_I).príkaz(editParams);
		pridajPoložkuPonuky("Uprav vizuálne vlastnosti označených…",
			VK_Z).príkaz(editVisuals);


		pridajPoložkuHlavnejPonuky("Simulácia", VK_S);
		pridajPoložkuPonuky("Spusti/pozastav", VK_S).príkaz(runPause);
		pridajPoložkuPonuky("Reštartuj", VK_R).príkaz(restart);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Rýchlosť plynutia času…",
			VK_C).príkaz(setTimer);


		kresliDoObrázka(ikonaOznačenia);
		kruh(3);
		kresliNaStrop();


		// Druhá časť (globálnej) inicializácie:
		spustiČasomieru();
		spustiČasovač();
	}


	// Rôzne akcie väčšinou zodpovedajúce vykonaniu príkazov položiek ponuky:

	public void vymažLinkyNaKurzore()
	{
		Bod myš = polohaMyši();
		Linka[] aktívne = Linka.dajAktívne();
		for (Linka linka : aktívne)
		{
			Spojnica[] spojnice = linka.spojniceZ();
			for (Spojnica spojnica : spojnice)
			{
				Line2D.Double tvar = spojnica.tvar();
				if (vzdialenosťBoduOdÚsečky(myš.polohaX(), myš.polohaY(),
					prepočítajSpäťX(tvar.x1), prepočítajSpäťY(tvar.y1),
					prepočítajSpäťX(tvar.x2), prepočítajSpäťY(tvar.y2)) < 10)
				{
					/* správa("Spojnica z " + spojnica.zdroj() + " do " +
						spojnica.cieľ()); */
					spojnica.zdroj().zrušSpojnicu(spojnica.cieľ());
				}
			}
		}
	}

	public void newSystem()
	{
		chyba("Táto funkcia je vo vývoji.", "Nový systém");
	}

	public void openSystem()
	{
		chyba("Táto funkcia je vo vývoji.", "Otvoriť systém…");
	}

	public void saveSystem()
	{
		chyba("Táto funkcia je vo vývoji.", "Uložiť systém");
	}

	public void undo()
	{
		chyba("Táto funkcia je vo vývoji.", "Späť");
	}

	public void redo()
	{
		chyba("Táto funkcia je vo vývoji.", "Znova");
	}

	public void selectAll()
	{
		Linka[] linky = Linka.daj();
		int početLiniek = linky.length;
		for (int i = 0; i < početLiniek; ++i)
			linky[i].označ(true);
	}

	public void deselectAll()
	{
		Linka[] linky = Linka.daj();
		int početLiniek = linky.length;
		for (int i = 0; i < početLiniek; ++i)
			linky[i].označ(false);
	}

	public void selectNext()
	{
		Linka[] aktívne = Linka.dajAktívne();
		int početAktívnych = aktívne.length;
		int aktuálna = 0;
		for (int i = 0; i < početAktívnych; ++i)
		{
			if (aktívne[i].označená()) aktuálna = i;
			aktívne[i].označ(false);
		}

		if (++aktuálna >= početAktívnych) aktuálna = 0;
		aktívne[aktuálna].označ(true);
	}

	public void selectPrevious()
	{
		Linka[] aktívne = Linka.dajAktívne();
		int početAktívnych = aktívne.length;
		int aktuálna = početAktívnych - 1;
		for (int i = početAktívnych - 1; i >= 0; --i)
		{
			if (aktívne[i].označená()) aktuálna = i;
			aktívne[i].označ(false);
		}

		if (--aktuálna < 0) aktuálna = početAktívnych - 1;
		aktívne[aktuálna].označ(true);
	}

	public void centerSelection()
	{
		Linka[] označené = Linka.dajOznačené();
		int početOznačených = označené.length;

		if (0 == početOznačených) return;

		double x = 0, y = 0;
		for (int i = 0; i < početOznačených; ++i)
		{
			x += označené[i].polohaX();
			y += označené[i].polohaY();
		}
		x /= početOznačených; y /= početOznačených;

		int počet = činnosti.size();
		for (int i = 0; i < počet; ++i)
		{
			Činnosť činnosť = činnosti.get(i);
			if (činnosť instanceof GRobot)
				((GRobot)činnosť).skoč(-x, -y);
		}
	}

	public void duplicateSelection()
	{
		Linka[] označené = Linka.dajOznačené();
		int početOznačených = označené.length;
		Vector<Linka> nové = new Vector<>();

		for (int i = 0; i < početOznačených; ++i)
		{
			Linka linka = Linka.pridaj(null);
			linka.kopíruj(označené[i]);
			nové.add(linka);
		}

		deselectAll();
		for (Linka linka : nové)
		{
			linka.skoč(2 * linka.veľkosť(), -2 * linka.veľkosť());
			linka.označ(true);
		}

		nové.clear();
		nové = null;
	}

	public void newLink()
	{
		Linka.pridaj(zadajReťazec("Zadajte popis novej linky:",
			"Popis linky")).skočNaMyš();
	}

	public void runPause()
	{
		if (pauza) spustiČasomieru();
		pauza = !pauza;
	}

	public void restart()
	{
		if (ÁNO == otázka("Skutočne chcete reštartovať simuláciu?",
			"Potvrdenie reštartu"))
		{
			čas = 0;
			Zákazník.vyčisti();
			Linka.vyčisti();
			odídených = 0;
			vybavených = 0;
			// mapaOdchodov.clear(); // TODO del
		}
	}

	public void setTimer()
	{
		Double hodnota = upravReálneČíslo(dilatácia,
			"Upravte koeficient rýchlosti plynutia času:",
			"Úprava rýchlosti plynutia času");
		if (null != hodnota)
		{
			if (hodnota < 0.1) hodnota = 0.1;
			dilatácia = hodnota;
		}
	}


	// Obsluha udalostí:

	// (rezervované)
	// @Override public void voľbaPoložkyPonuky() { }
	// @Override public void voľbaKontextovejPoložky() { }

	@Override public void klávesováSkratka()
	{
		String príkaz = príkazSkratky();
		if (newSystem == príkaz) newSystem();
		else if (openSystem == príkaz) openSystem();
		else if (saveSystem == príkaz) saveSystem();
		else if (undo == príkaz) undo();
		else if (redo == príkaz) redo();
		else if (selectAll == príkaz) selectAll();
		else if (deselectAll == príkaz) deselectAll();
		else if (selectNext == príkaz) selectNext();
		else if (selectPrevious == príkaz) selectPrevious();
		else if (centerSelection == príkaz) centerSelection();
		else if (duplicateSelection == príkaz) duplicateSelection();
		else if (deleteSelection == príkaz) Linka.vymažOznačené();
		else if (newLink == príkaz) newLink();
		else if (editLabels == príkaz) Linka.upravPopisy();

		else if (changeToEmitors == príkaz)   Linka.zmeňNaEmitory();
		else if (changeToBuffers == príkaz)   Linka.zmeňNaZásobníky();
		else if (changeToWaitRooms == príkaz) Linka.zmeňNaČakárne();
		else if (changeToConveyors == príkaz) Linka.zmeňNaDopravníky();
		else if (changeToChangers == príkaz)  Linka.zmeňNaMeniče();
		else if (changeToReleasers == príkaz) Linka.zmeňNaUvoľňovače();

		else if (changeToEllipses == príkaz)   Linka.zmeňNaElipsy();
		else if (changeToRectangles == príkaz) Linka.zmeňNaObdĺžniky();
		else if (changeToRoundRects == príkaz) Linka.zmeňNaObléObdĺžniky();

		else if (editParams == príkaz) Linka.upravKoeficientyOznačených();
		else if (editVisuals == príkaz) Linka.upravVizuályOznačených();
		else if (runPause == príkaz) runPause();
		else if (restart == príkaz) restart();
		else if (setTimer == príkaz) setTimer();
	}

	@Override public void stlačenieKlávesu()
	{
		Bod p = null;
		if (!klávesnica().isControlDown()) switch (kláves())
		{
		case HORE: p = new Bod(0, 10); break;
		case DOLE: p = new Bod(0, -10); break;
		case VPRAVO: p = new Bod(10, 0); break;
		case VĽAVO: p = new Bod(-10, 0); break;
		}

		if (null != p)
		{
			Linka[] označené = Linka.dajOznačené();
			int početOznačených = označené.length;

			for (int i = 0; i < početOznačených; ++i)
				označené[i].skoč(p.getX(), p.getY());
		}
	}


	@Override public void stlačenieTlačidlaMyši()
	{
		if (tlačidloMyši(ĽAVÉ))
		{
			if (myš().isControlDown() && myš().isAltDown())
			{
				posúvajObjekty = false;
				tvorVýber = true;
				tvorSpojnicu = false;
				mažSpojnicu = false;
				začiatokAkcie = polohaMyši();
				koniecAkcie = null;
			}
			else if (myš().isControlDown())
			{
				posúvajObjekty = false;
				tvorVýber = false;
				tvorSpojnicu = true;
				mažSpojnicu = false;
				začiatokAkcie = polohaMyši();
				koniecAkcie = null;
			}
			else if (myš().isAltDown())
			{
				posúvajObjekty = false;
				tvorVýber = false;
				tvorSpojnicu = false;
				mažSpojnicu = true;
				začiatokAkcie = polohaMyši();
				koniecAkcie = null;
			}
			else
			{
				posúvajObjekty = myš().isShiftDown();
				tvorVýber = false;
				tvorSpojnicu = false;
				mažSpojnicu = false;
			}
		}
		else
		{
			posúvajObjekty = tlačidloMyši(STREDNÉ);
			tvorVýber = false;
			tvorSpojnicu = false;
			mažSpojnicu = false;
		}
	}

	@Override public void ťahanieMyšou()
	{
		if (mažSpojnicu || tvorSpojnicu || tvorVýber)
		{
			koniecAkcie = polohaMyši();
			žiadajPrekreslenie();
		}
		else if (posúvajObjekty)
		{
			Bod p = Bod.rozdiel(polohaMyši(), poslednáPolohaMyši());
			int počet = činnosti.size();
			for (int i = 0; i < počet; ++i)
			{
				Činnosť činnosť = činnosti.get(i);
				if (činnosť instanceof GRobot)
					((GRobot)činnosť).skoč(p.getX(), p.getY());
			}
		}
	}

	@Override public void uvoľnenieTlačidlaMyši()
	{
		if (mažSpojnicu || tvorSpojnicu || tvorVýber)
		{
			if (null != začiatokAkcie && null != koniecAkcie)
			{
				if (tvorVýber)
				{
					double
						x1 = začiatokAkcie.polohaX(),
						y1 = začiatokAkcie.polohaY(),
						x2 = koniecAkcie.polohaX(),
						y2 = koniecAkcie.polohaY();

					if (x1 > x2)
					{
						double x = x1;
						x1 = x2; x2 = x;
					}

					if (y1 > y2)
					{
						double y = y1;
						y1 = y2; y2 = y;
					}

					Linka[] aktívne = Linka.dajAktívne();
					for (Linka linka : aktívne)
					{
						Poloha p = linka.poloha();
						linka.označ(p.polohaX() >= x1 && p.polohaX() <= x2 &&
							p.polohaY() >= y1 && p.polohaY() <= y2);
					}
				}
				else
				{
					Linka začiatok = null, koniec = null;
					Linka[] aktívne = Linka.dajAktívne();
					for (Linka linka : aktívne)
					{
						if (linka.bodV(začiatokAkcie)) začiatok = linka;
						if (linka.bodV(koniecAkcie)) koniec = linka;
					}

					if (null != začiatok && null != koniec)
					{
						if (mažSpojnicu)
							začiatok.zrušSpojnicu(koniec);
						else
							začiatok.spojnica(koniec);
					}
				}
			}

			tvorVýber = false;
			tvorSpojnicu = false;
			mažSpojnicu = false;
			začiatokAkcie = null;
			koniecAkcie = null;
		}
		posúvajObjekty = false;
	}

	@Override public void kresliTvar()
	{
		if (mažSpojnicu || tvorSpojnicu)
		{
			if (mažSpojnicu) farba(svetločervená); else farba(svetlošedá);
			if (null != začiatokAkcie)
				skočNa(začiatokAkcie);
			if (null != koniecAkcie)
				choďNa(koniecAkcie);
		}
		else if (tvorVýber)
		{
			farba(zelená);
			if (null != začiatokAkcie && null != koniecAkcie)
			{
				double x1, y1, x2, y2;

				skočNa(začiatokAkcie);
				choďNa(x1 = začiatokAkcie.polohaX(),
					y2 = koniecAkcie.polohaY());
				choďNa(koniecAkcie);
				choďNa(x2 = koniecAkcie.polohaX(),
					y1 = začiatokAkcie.polohaY());
				choďNa(začiatokAkcie);

				if (x1 > x2)
				{
					double x = x1;
					x1 = x2; x2 = x;
				}

				if (y1 > y2)
				{
					double y = y1;
					y1 = y2; y2 = y;
				}

				Linka[] aktívne = Linka.dajAktívne();
				for (Linka linka : aktívne)
				{
					Poloha p = linka.poloha();
					linka.označ(p.polohaX() >= x1 && p.polohaX() <= x2 &&
						p.polohaY() >= y1 && p.polohaY() <= y2);
				}
			}
		}

		// TODO dať vypnuteľné
		{
			farba(čierna);
			skočNa(ľavýOkraj() + 10, hornýOkraj() - výškaRiadka());

			if (0 != čas)
			{
				text("Čas: " + F(čas, 3), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}

			if (1 != dilatácia)
			{
				text("Rýchlosť: " + F(dilatácia, 2), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}

			if (0 != odídených)
			{
				text("Odišlo nevybavených: " + odídených, KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}

			if (0 != vybavených)
			{
				text("Vybavených: " + vybavených, KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}

			// // TODO revise
			boolean prvý = true;
			Linka[] aktívne = Linka.dajAktívne();
			for (Linka linka : aktívne)
			// for (Map.Entry<Linka, Integer> záznam : mapaOdchodov.entrySet())
			{
				// Linka linka = záznam.getKey();
				// Integer počet = záznam.getValue();
				int počet = linka.odídených;

				if (0 != počet)
				{
					if (prvý)
					{
						skoč(0, -výškaRiadka());
						text("Linky nevybavených:", KRESLI_PRIAMO);
						skoč(0, -1.5 * výškaRiadka());
						prvý = false;
					}

					skoč(10, 0);
					text(linka.skráťPopis(100) + ": " + počet, KRESLI_PRIAMO);
					skoč(-10, -výškaRiadka());
				}
			}
		}

		// if (úniky) // TODO del
		/*{
			farba(červená);
			skočNa(0, hornýOkraj() - výškaRiadka());
			// text("Boli zaznamenané časové úniky.");
			text("" + úniky);
		}*/
	}


	// int aaa = 100; // TODO del

	private void krok()
	{
		int brzda;
		if (debugOn)
		{
			čas += 0.15;
			System.out.println("\nkrok(" + čas + ")");
			brzda = 1;
		}
		else
		{
			if (pauza) return;
			čas += zastavČasomieru() * dilatácia;
			brzda = 100_000;
		}

		boolean opakuj = true;
		while (opakuj && --brzda >= 0)
		{
			opakuj = false;
			Collections.sort(činnosti);
			int počet = činnosti.size();

			// TODO del
			/*if (aaa > 0)
			{
				--aaa;
				if (10 == aaa)
				for (int i = 0; i < počet; ++i)
					System.out.println("činnosti.get(" + i + "): " +
						činnosti.get(i));
			}*/

			for (int i = 0; i < počet; ++i)
			{
				Činnosť činnosť = činnosti.get(i);
				if (činnosť.aktívny() && činnosť.činnosť())
					opakuj = true;
			}
		}

		// TODO del
		// úniky = !debugOn && brzda <= 0;
		// úniky = brzda;
	}

	@Override public void klik()
	{
		if (myš().getClickCount() > 1)
		{
			if (myš().isControlDown()) vymažLinkyNaKurzore();
			else if (tlačidloMyši(ĽAVÉ)) Linka.upravKoeficientyOznačených();
			else Linka.upravVizuályOznačených();
		}
		else if (debugOn) krok();
	}

	@Override public void tik()
	{
		if (!debugOn) krok();
		if (neboloPrekreslené()) prekresli();
		spustiČasomieru();
	}


	@Override public boolean konfiguráciaZmenená()
	{ return true; }

	@Override public void zapíšKonfiguráciu(Súbor súbor) throws IOException
	{
		Linka[] linky = Linka.daj();
		int početLiniek = 0;
		súbor.zapíšVlastnosť("početLiniek", početLiniek);
		Vector<Linka> zapísané = new Vector<>();

		for (int i = 0; i < linky.length; ++i)
		{
			if (linky[i].aktívny())
			{
				súbor.zapíšPrázdnyRiadokVlastností();
				linky[i].ulož(súbor, "linka[" + početLiniek + "]");
				++početLiniek;
				zapísané.add(linky[i]);
			}
		}

		súbor.zapíšPrázdnyRiadokVlastností();
		súbor.zapíšVlastnosť("početLiniek", početLiniek);

		for (Linka linka : zapísané) linka.uložSpojnice(súbor);

		zapísané.clear();
		zapísané = null;
	}

	@Override public void čítajKonfiguráciu(Súbor súbor) throws IOException
	{
		{
			int početLiniek = Linka.počet();
			for (int i = 0; i < početLiniek; ++i)
				Linka.daj(i).deaktivuj();
		}

		Integer početLiniek = súbor.čítajVlastnosť("početLiniek", 0);
		početLiniek = null == početLiniek ? 0 : početLiniek;
		Vector<Linka> prečítané = new Vector<>();

		for (int i = 0; i < početLiniek; ++i)
		{
			Linka linka = Linka.pridaj(null);
			linka.čítaj(súbor, "linka[" + i + "]");
			prečítané.add(linka);
		}

		for (Linka linka : prečítané) linka.čítajSpojnice(súbor);

		prečítané.clear();
		prečítané = null;
	}


	// Hlavná metóda:
	public static void main(String[] args)
	{
		použiKonfiguráciu("Systém.cfg");
		Svet.skry();
		try { new Systém(); } catch (Throwable t) { t.printStackTrace(); }
		Svet.zobraz();
	}
}
