
import java.awt.BasicStroke;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import java.util.TreeMap;
import knižnica.*;
import static java.lang.Math.*;
import static knižnica.Kláves.*;
import static knižnica.Svet.*;
import static knižnica.ÚdajeUdalostí.*;

import static debug.Debug.*;

/*
TODO

 • Pridať možnosť hromadného nastavenia •jednotlivých• parametrov liniek,
   čiže nie to, čo už jestvuje: viacero parametrov upravovaných v jednom
   dialógu naraz, ale len jedného a hromadne. (Lebo pri hromadných úpravách
   sa to môže neželane dosť mixovať.)

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

	// Čiary:
	private BasicStroke čiaraTvorbySpojníc = new BasicStroke(2.0f,
		BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
		new float[]{6.0f, 6.0f}, 0.0f);

	private BasicStroke čiaraMriežky = new BasicStroke(0.65f,
		BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
		new float[]{5.0f, 5.0f}, 0.0f);


	// Globálny čas a rôzne príznaky a vlastnosti:
	public static double čas = 0;
	public static double dilatácia = 1.0;
	public static boolean pauza = false;
	private boolean zobrazInformácie = true;
	private boolean zobrazMriežku = false;
	private double mriežkaX = 20.0, mriežkaY = 0.0;
	private double mriežkaŠírka = 20.0, mriežkaVýška = 0.0;
	private double mriežkaUhol = 15.0;


	// Globálna štatistika:
	public static int odídených = 0;
	public static int vybavených = 0;


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
	private final static String locgridSelection = "locgridSelection";
	private final static String sizgridSelection = "sizgridSelection";
	private final static String anggridSelection = "anggridSelection";
	private final static String toggleGrid = "toggleGrid";
	private final static String configGrid = "configGrid";
	private final static String deleteSelection = "deleteSelection";
	private final static String duplicateSelection = "duplicateSelection";
	private final static String newLink = "newLink";
	private final static String editLabels = "editLabels";

	private final static String clearPurpose = "clearPurpose";
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
	private final static String toggleLinksInfo = "toggleLinksInfo";
	private final static String hideLinksInfo = "hideLinksInfo";
	private final static String showLinksInfo = "showLinksInfo";
	private final static String toggleOutlines = "toggleOutlines";
	private final static String hideOutlines = "hideOutlines";
	private final static String showOutlines = "showOutlines";
	private final static String runPause = "runPause";
	private final static String restart = "restart";
	private final static String setTimer = "setTimer";
	private final static String toggleInfo = "toggleInfo";


	// Hlavná ponuka:

	private PoložkaPonuky položkaPrepniMriežku;

	private KontextováPoložka položkaZrušÚčely;
	private KontextováPoložka položkaZmeňNaEmitory;
	private KontextováPoložka položkaZmeňNaZásobníky;
	private KontextováPoložka položkaZmeňNaČakárne;
	private KontextováPoložka položkaZmeňNaDopravníky;
	private KontextováPoložka položkaZmeňNaMeniče;
	private KontextováPoložka položkaZmeňNaUvoľňovače;

	private KontextováPoložka položkaZmeňNaElipsy;
	private KontextováPoložka položkaZmeňNaObdĺžniky;
	private KontextováPoložka položkaZmeňNaObléObdĺžniky;

	private PoložkaPonuky položkaPrepniInformácie;


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
		čiara(čiaraTvorbySpojníc);


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
		pridajKlávesovúSkratku(locgridSelection, VK_G);
		pridajKlávesovúSkratku(sizgridSelection, VK_G,
			SKRATKA_PONUKY | SHIFT_MASK);
		pridajKlávesovúSkratku(anggridSelection, VK_G,
			SKRATKA_PONUKY | ALT_MASK);
		pridajKlávesovúSkratku(toggleGrid, VK_G, 0);
		pridajKlávesovúSkratku(configGrid, VK_G, SKRATKA_PONUKY |
			SHIFT_MASK | ALT_MASK);
		pridajKlávesovúSkratku(duplicateSelection, VK_D);
		pridajKlávesovúSkratku(deleteSelection, VK_DELETE, 0);
		pridajKlávesovúSkratku(newLink, VK_M);
		pridajKlávesovúSkratku(editLabels, VK_F2, 0);

		pridajKlávesovúSkratku(clearPurpose, VK_0);
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
		pridajKlávesovúSkratku(editVisuals, VK_F8, 0);
		pridajKlávesovúSkratku(toggleLinksInfo, VK_I, 0);
		pridajKlávesovúSkratku(hideLinksInfo, VK_I);
		pridajKlávesovúSkratku(showLinksInfo, VK_I,
			SKRATKA_PONUKY | SHIFT_MASK);
		pridajKlávesovúSkratku(toggleOutlines, VK_U, 0);
		pridajKlávesovúSkratku(hideOutlines, VK_U);
		pridajKlávesovúSkratku(showOutlines, VK_U,
			SKRATKA_PONUKY | SHIFT_MASK);
		pridajKlávesovúSkratku(runPause, VK_F5, 0);
		pridajKlávesovúSkratku(restart, VK_F5);
		pridajKlávesovúSkratku(setTimer, VK_F3, 0);
		pridajKlávesovúSkratku(toggleInfo, VK_F6, 0);

		vymažPonuku();


		// Súbor:
		pridajPoložkuHlavnejPonuky("Súbor", VK_S);
		pridajPoložkuPonuky("Nový systém", VK_N).príkaz(newSystem);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Otvoriť systém…", VK_O).príkaz(openSystem);
		pridajPoložkuPonuky("Uložiť systém", VK_U).príkaz(saveSystem);
		pridajOddeľovačPonuky();
		pridajPoložkuPonukyPrekresliť();
		pridajPoložkuPonukyKoniec();


		// Úpravy:
		pridajPoložkuHlavnejPonuky("Úpravy", VK_P);
		pridajPoložkuPonuky("Späť", VK_S).príkaz(undo);
		pridajPoložkuPonuky("Znova", VK_O).príkaz(redo);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Označ všetky linky", VK_A).príkaz(selectAll);
		pridajPoložkuPonuky("Zruš označenie linek", VK_R).príkaz(deselectAll);
		pridajPoložkuPonuky("Označ nasledujúcu linku",
			VK_N).príkaz(selectNext);
		pridajPoložkuPonuky("Označ predchádzajúcu linku",
			VK_P).príkaz(selectPrevious);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Vystreď označenie", VK_Y).príkaz(centerSelection);
		pridajPoložkuPonuky("Zarovnaj polohy označených",
			VK_Z).príkaz(locgridSelection);
		pridajPoložkuPonuky("Zokrúhli rozmery označených",
			VK_K).príkaz(sizgridSelection);
		pridajPoložkuPonuky("Zokrúhli pootočenie označených",
			VK_T).príkaz(anggridSelection);
		položkaPrepniMriežku = pridajPoložkuPonuky(
			"Prepni zobrazenie mriežky", VK_M);
		položkaPrepniMriežku.príkaz(toggleGrid);
		pridajPoložkuPonuky("Konfiguruj mriežky", VK_G).príkaz(configGrid);

		pridajOddeľovačPonuky();

		pridajPoložkuPonuky("Duplikuj označenie",
			VK_D).príkaz(duplicateSelection);
		pridajPoložkuPonuky("Vymaž označené…", VK_V).príkaz(deleteSelection);


		// Linka:
		pridajPoložkuHlavnejPonuky("Linka", VK_L);
		pridajPoložkuPonuky("Nová linka", VK_N).príkaz(newLink);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Uprav popisy označených…",
			VK_P).príkaz(editLabels);


		položkaZrušÚčely = new KontextováPoložka("Zruš účely");
		položkaZrušÚčely.setMnemonic(VK_R);
		položkaZrušÚčely.príkaz(clearPurpose);
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
			položkaZrušÚčely, null, položkaZmeňNaEmitory,
			položkaZmeňNaZásobníky, položkaZmeňNaČakárne,
			položkaZmeňNaDopravníky, položkaZmeňNaMeniče,
			položkaZmeňNaUvoľňovače).setMnemonic(VK_F);


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
			VK_K).príkaz(editParams);
		pridajPoložkuPonuky("Uprav vizuálne vlastnosti označených…",
			VK_Z).príkaz(editVisuals);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Prepni zobrazenie informácií označených liniek",
			VK_I).príkaz(toggleLinksInfo);
		pridajPoložkuPonuky("Skry informácie označených linek",
			VK_S).príkaz(hideLinksInfo);
		pridajPoložkuPonuky("Zobraz informácie označených liniek",
			VK_B).príkaz(showLinksInfo);
		/* TODO del pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Prepni zobrazenie obrysov označených liniek",
			VK_O).príkaz(toggleOutlines);
		pridajPoložkuPonuky("Skry obrysy označených linek",
			VK_R).príkaz(hideOutlines);
		pridajPoložkuPonuky("Zobraz obrysy označených liniek",
			VK_A).príkaz(showOutlines);*/


		// Simulácia:
		pridajPoložkuHlavnejPonuky("Simulácia", VK_S);
		pridajPoložkuPonuky("Spusti/pozastav", VK_S).príkaz(runPause);
		pridajPoložkuPonuky("Reštartuj", VK_R).príkaz(restart);
		pridajOddeľovačPonuky();
		pridajPoložkuPonuky("Rýchlosť plynutia času…",
			VK_C).príkaz(setTimer);
		položkaPrepniInformácie = pridajPoložkuPonuky(
			"Prepni zobrazenie informácií", VK_I);
		položkaPrepniInformácie.príkaz(toggleInfo);


		kresliDoObrázka(ikonaOznačenia);
		kruh(3);
		kresliNaPodlahu();


		// Toto riešenie je nevyhnutné, aby sa zároveň správne načítali
		// globálne konfiguračné nastavenia a zároveň, aby táto metóda bola
		// k dispozícii pre funkciu čítania systému zo súboru:
		new ObsluhaUdalostí()
		{
			@Override public void čítajKonfiguráciu(Súbor súbor)
				throws IOException { čítajKonfiguráciu2(súbor); }
		};


		// Druhá časť (globálnej) inicializácie:
		spustiČasomieru();
		spustiČasovač();
	}


	// Rôzne akcie väčšinou zodpovedajúce vykonaniu príkazov položiek ponuky:

	public boolean zobrazInformácie()
	{
		return zobrazInformácie;
	}

	public void zobrazInformácie(boolean zobrazInformácie)
	{
		this.zobrazInformácie = zobrazInformácie;
		položkaPrepniInformácie.ikona(zobrazInformácie ? ikonaOznačenia : null);
		žiadajPrekreslenie();
	}

	public void prepniZobrazenieInformácií()
	{
		zobrazInformácie = !zobrazInformácie;
		položkaPrepniInformácie.ikona(zobrazInformácie ? ikonaOznačenia : null);
		žiadajPrekreslenie();
	}

	public boolean zobrazMriežku()
	{
		return zobrazMriežku;
	}

	public void prekresliMriežku()
	{
		podlaha.vymažGrafiku();
		if (zobrazMriežku && (0 != mriežkaX || 0 != mriežkaY)) try
		{
			double Πx = mriežkaX, Πy = mriežkaY;
			if (0 == mriežkaX) Πx = mriežkaY;
			if (0 == mriežkaY) Πy = mriežkaX;

			double x0 = najmenšieX() - 2 * Πx;
			double x1 = najväčšieX() + 2 * Πx;
			x0 = floor(x0 / Πx) * Πx;
			x1 = floor(x1 / Πx) * Πx;

			double y0 = najmenšieY() - 2 * Πy;
			double y1 = najväčšieY() + 2 * Πy;
			y0 = floor(y0 / Πy) * Πy;
			y1 = floor(y1 / Πy) * Πy;

			čiara(čiaraMriežky);
			farba(papierová);

			for (double x = x0; x <= x1; x += Πx)
			{
				skočNa(x, y0);
				choďNa(x, y1);
			}

			for (double y = x0; y <= y1; y += Πy)
			{
				skočNa(x0, y);
				choďNa(x1, y);
			}
		}
		finally
		{
			čiara(čiaraTvorbySpojníc);
		}
		žiadajPrekreslenie();
	}

	public void zobrazMriežku(boolean zobrazMriežku)
	{
		this.zobrazMriežku = zobrazMriežku;
		položkaPrepniMriežku.ikona(zobrazMriežku ? ikonaOznačenia : null);
		prekresliMriežku();
	}

	public void prepniZobrazenieMriežky()
	{
		zobrazMriežku = !zobrazMriežku;
		položkaPrepniMriežku.ikona(zobrazMriežku ? ikonaOznačenia : null);
		prekresliMriežku();
	}

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
		// TODO
		chyba("Táto funkcia je vo vývoji.", "Nový systém");
	}

	public void openSystem()
	{
		// TODO
		chyba("Táto funkcia je vo vývoji.", "Otvoriť systém…");
	}

	public void saveSystem()
	{
		// TODO
		chyba("Táto funkcia je vo vývoji.", "Uložiť systém");
	}

	public void undo()
	{
		// TODO
		chyba("Táto funkcia je vo vývoji.", "Späť");
	}

	public void redo()
	{
		// TODO
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
			{
				GRobot robot = (GRobot)činnosť;
				robot.skoč(-x, -y);
				if (robot.smerujeDoCieľa())
				{
					Bod b = robot.cieľ();
					b.posuň(-x, -y);
					robot.upravCieľ(b);
				}
			}
		}
	}

	public void locgridSelection()
	{
		if (0 == mriežkaX && 0 == mriežkaY) return;
		Linka[] označené = Linka.dajOznačené();
		double Πx = mriežkaX, Πy = mriežkaY;
		if (0 == mriežkaX) Πx = mriežkaY;
		if (0 == mriežkaY) Πy = mriežkaX;
		for (Linka linka : označené) linka.posuňAjZákazníkov(
			floor(linka.polohaX() / Πx) * Πx - linka.polohaX(),
			floor(linka.polohaY() / Πy) * Πy - linka.polohaY());
	}

	public void sizgridSelection()
	{
		if (0 == mriežkaŠírka && 0 == mriežkaVýška) return;
		Linka[] označené = Linka.dajOznačené();
		double Πšírka = mriežkaŠírka, Πvýška = mriežkaVýška;
		if (0 == mriežkaŠírka) Πšírka = mriežkaVýška;
		if (0 == mriežkaVýška) Πvýška = mriežkaŠírka;
		for (Linka linka : označené) linka.rozmery(
			floor(linka.šírka() / Πšírka) * Πšírka,
			floor(linka.výška() / Πvýška) * Πvýška);
	}

	public void anggridSelection()
	{
		if (0 == mriežkaUhol) return;
		Linka[] označené = Linka.dajOznačené();
		for (Linka linka : označené) linka.uhol(
			floor(linka.uhol() / mriežkaUhol) * mriežkaUhol);
	}

	public void configGrid()
	{
		// TODO
		chyba("Táto funkcia je vo vývoji.", "Konfiguruj mriežky");
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
		else if (locgridSelection == príkaz) locgridSelection();
		else if (sizgridSelection == príkaz) sizgridSelection();
		else if (anggridSelection == príkaz) anggridSelection();
		else if (toggleGrid == príkaz) prepniZobrazenieMriežky();
		else if (configGrid == príkaz) configGrid();
		else if (duplicateSelection == príkaz) duplicateSelection();
		else if (deleteSelection == príkaz) Linka.vymažOznačené();
		else if (newLink == príkaz) newLink();
		else if (editLabels == príkaz) Linka.upravPopisy();

		else if (clearPurpose == príkaz) Linka.zrušÚčely();
		else if (changeToEmitors == príkaz) Linka.zmeňNaEmitory();
		else if (changeToBuffers == príkaz) Linka.zmeňNaZásobníky();
		else if (changeToWaitRooms == príkaz) Linka.zmeňNaČakárne();
		else if (changeToConveyors == príkaz) Linka.zmeňNaDopravníky();
		else if (changeToChangers == príkaz) Linka.zmeňNaMeniče();
		else if (changeToReleasers == príkaz) Linka.zmeňNaUvoľňovače();

		else if (changeToEllipses == príkaz) Linka.zmeňNaElipsy();
		else if (changeToRectangles == príkaz) Linka.zmeňNaObdĺžniky();
		else if (changeToRoundRects == príkaz) Linka.zmeňNaObléObdĺžniky();

		else if (editParams == príkaz) Linka.upravKoeficientyOznačených();
		else if (editVisuals == príkaz) Linka.upravVizuályOznačených();
		else if (toggleLinksInfo == príkaz) Linka.prepniInformácieOznačených();
		else if (hideLinksInfo == príkaz) Linka.skryInformácieOznačených();
		else if (showLinksInfo == príkaz) Linka.zobrazInformácieOznačených();
		/* TODO del else if (toggleOutlines == príkaz) Linka.prepniObrysOznačených();
		else if (hideOutlines == príkaz) Linka.skryObrysOznačených();
		else if (showOutlines == príkaz) Linka.zobrazObrysOznačených(); */
		else if (runPause == príkaz) runPause();
		else if (restart == príkaz) restart();
		else if (setTimer == príkaz) setTimer();
		else if (toggleInfo == príkaz) prepniZobrazenieInformácií();
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
				{
					GRobot robot = (GRobot)činnosť;
					robot.skoč(p.getX(), p.getY());
					if (robot.smerujeDoCieľa())
					{
						Bod b = robot.cieľ();
						b.posuň(p);
						robot.upravCieľ(b);
					}
				}
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


	private String formátujČas(double čas)
	{
		StringBuffer sb = new StringBuffer();
		long lčas = (long)čas;
		long h =  lčas / 3_600;
		long m = (lčas / 60) % 60;
		long s =  lčas % 60;
		čas -= lčas; čas *= 1_000;
		lčas = (long)čas;
		if (h > 0)
		{
			sb.append(h);
			sb.append(':');

			if (m < 10) sb.append('0');
			sb.append(m);
			sb.append(':');

			if (s < 10) sb.append('0');
			sb.append(s);
			sb.append('.');

			if (lčas < 100) sb.append('0');
			if (lčas < 10) sb.append('0');
			sb.append(lčas);
		}
		else if (m > 0)
		{
			sb.append(m);
			sb.append(':');

			if (s < 10) sb.append('0');
			sb.append(s);
			sb.append('.');

			if (lčas < 100) sb.append('0');
			if (lčas < 10) sb.append('0');
			sb.append(lčas);
		}
		else
		{
			sb.append(s);
			sb.append('.');

			if (lčas < 100) sb.append('0');
			if (lčas < 10) sb.append('0');
			sb.append(lčas);
		}
		return sb.toString();
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

		if (zobrazInformácie)
		{
			farba(čierna);
			skočNa(ľavýOkraj() + 10, hornýOkraj() - výškaRiadka());

			if (0 != čas)
			{
				if (čas >= 60)
					text(S("Čas: ", F(čas, 3), " (", formátujČas(čas), ")"),
						KRESLI_PRIAMO);
				else
					text(S("Čas: ", F(čas, 3)), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}

			if (1 != dilatácia)
			{
				text(S("Rýchlosť: ", F(dilatácia, 2)), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}

			if (0 != odídených)
			{
				text(S("Odišlo nevybavených: ", odídených), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}

			if (0 != vybavených)
			{
				text(S("Vybavených: ", vybavených), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}

			boolean prvý = true;
			Linka[] aktívne = Linka.dajAktívne();
			for (Linka linka : aktívne)
			{
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
					text(S(linka.skráťPopis(100), ": ", počet), KRESLI_PRIAMO);
					skoč(-10, -výškaRiadka());
				}
			}
		}
	}


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

			for (int i = 0; i < počet; ++i)
			{
				Činnosť činnosť = činnosti.get(i);
				if (činnosť.aktívny() && činnosť.činnosť())
					opakuj = true;
			}
		}
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
		súbor.zapíšVlastnosť("dilatácia", dilatácia);
		súbor.zapíšVlastnosť("pauza", pauza);
		súbor.zapíšVlastnosť("zobrazInformácie", zobrazInformácie);
		súbor.zapíšVlastnosť("zobrazMriežku", zobrazMriežku);

		súbor.vnorMennýPriestorVlastností("mriežka"); try {
			súbor.zapíšVlastnosť("x", mriežkaX);
			súbor.zapíšVlastnosť("y", mriežkaY);
			súbor.zapíšVlastnosť("šírka", mriežkaŠírka);
			súbor.zapíšVlastnosť("výška", mriežkaVýška);
			súbor.zapíšVlastnosť("uhol", mriežkaUhol);
		} finally { súbor.vynorMennýPriestorVlastností(); }

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

	// Toto riešenie je nevyhnutné, aby sa zároveň správne načítali
	// globálne konfiguračné nastavenia a zároveň, aby táto metóda bola
	// k dispozícii pre funkciu čítania systému zo súboru:
	public void čítajKonfiguráciu2(Súbor súbor) throws IOException
	{
		{
			Double hodnota = súbor.čítajVlastnosť("dilatácia", 1.0);
			dilatácia = null == hodnota ? 1.0 : hodnota;
		}
		{
			Boolean hodnota = súbor.čítajVlastnosť("pauza", false);
			pauza = null == hodnota ? false : hodnota;
			hodnota = súbor.čítajVlastnosť("zobrazInformácie", true);
			zobrazInformácie(null == hodnota ? true : hodnota);
			hodnota = súbor.čítajVlastnosť("zobrazMriežku", false);
			zobrazMriežku(null == hodnota ? true : hodnota);
		}


		súbor.vnorMennýPriestorVlastností("mriežka"); try {
			Double hodnota = súbor.čítajVlastnosť("x", 20.0);
			mriežkaX = null == hodnota ? 20.0 : hodnota;
			hodnota = súbor.čítajVlastnosť("y", 0.0);
			mriežkaY = null == hodnota ? 0.0 : hodnota;
			hodnota = súbor.čítajVlastnosť("šírka", 20.0);
			mriežkaŠírka = null == hodnota ? 20.0 : hodnota;
			hodnota = súbor.čítajVlastnosť("výška", 0.0);
			mriežkaVýška = null == hodnota ? 0.0 : hodnota;
			hodnota = súbor.čítajVlastnosť("uhol", 15.0);
			mriežkaUhol = null == hodnota ? 15.0 : hodnota;
		} finally { súbor.vynorMennýPriestorVlastností(); }

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
