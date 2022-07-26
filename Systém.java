
import java.awt.BasicStroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;
import knižnica.*;
import static java.lang.Math.*;
import static knižnica.Kláves.*;
import static knižnica.Svet.*;
import static knižnica.ÚdajeUdalostí.*;

import static debug.Debug.*;

/*
Dohoda:

 ☑ Písmo informácií o simulácii a linkách bude presne to písmo, ktoré bude
   mať aktuálne nastavené hlavný robot. Z tohto písma sa budú derivovať písma
   ostatných robotov.


TODO:

 • Určiť, ktoré funkcie nastavia príznak „zmenené“ (posunutie pohľadu nie;
   posunutie linky áno) a aplikovať tento príznak do systému save/load. To
   isté platí pre systém undo/redo (nemá zmysel vracať alebo opakovať
   posunutie pohľadu).

 • Spustiť simuláciu do určitého času a zozbierať výsledky. Urobiť to
   niekoľkokrát a zhromaždiť výsledky do tabuľky.
    ◦ Tiež: Spustiť simuláciu na pozadí s určitým pevne stanoveným časovým
      krokom.

 • Dokončiť funkcie undo/redo.

 • Pridať možnosť zrkadlového kreslenia tvaru.

 ✓ Kontextová ponuka pre označené linky.

 ✗ „Špeciálna“ kontextová ponuka oznamujúca, že žiadna linka nie je označená.

 ✓ Zablikanie linky, pre ktorú bola aktivovaná individuálna kontextová
   ponuka v prípade, že je niektorá linka označená.

 ✓ Nahradiť oblý obdĺžnik iným tvarom. (Oblý obdĺžnik je teraz predvolený.)

 ✗ Do ponuky ladenia pridať možnosť zobrazenia ladiacich informácií, ktoré
   bežne odchádzajú na štandardný výstup, v poznámkovom bloku. — To by nemalo
   význam. Načo by to bolo bežnému používateľovi. Radšej mu treba vhodne
   zobraziť skupinu iných informácií, ktoré pre neho budú skutočne užitočné.

 ✓ Pridať možnosť hromadného nastavenia •jednotlivých• parametrov liniek,
   čiže nie to, čo už jestvuje: viacero parametrov upravovaných v jednom
   dialógu naraz, ale len jedného a hromadne. (Lebo pri hromadných úpravách
   sa to môže neželane dosť mixovať.)

 ✓ Zarovnanie na mriežku. Upraviť parametre mriežky.

 ✓ Úprava vzhľadových vlastností. (Pomer, veľkosť, miera zaoblenia, uhol,
   veľkosť písma, poloha popisu, viacriadkovosť popisu.)

 ✗ (Podfarbenie? Zmena poradia?)

 ✓ Animovať zákazníkov v dopravníku tak, aby sa postupne presúvali
   od jedného kraja po druhý.

 ✓ Nejako rozmiesniť zákazníkov v zásobníku a čakárni (vedľa seba?, do
   kruhu? – nakoniec to bolo náhodne).

 ✓ S presúvaním linky presúvať aj zákazníkov v nej.

 ✓ Vytvoriť klávesnicový spôsob na vytvorenie spojnice medzi linkami:
    ◦ Označiť začiatok (klávesnicou, myšou… hocijako).
    ◦ Stlačiť klávesovú skratku (ESC to ruší).
    ◦ Označiť koniec (detto).
    ◦ Stlačiť klávesovú skratku – spojnica sa vytvorí.

 ✓ Ctrl + dvojklik na spojnicu ju vymaže.

 ✓ Ponuka Simulácia
    ◦ Zastaviť/spustiť
    ◦ Reštartovať
    ◦ Zmeniť rýchlosť

*/

public class Systém extends GRobot
{
	// Evidencia:
	public final static Vector<Činnosť> činnosti = new Vector<>();

	// Ikony na označenie položiek ponúk:
	public final static Obrázok ikonaOznačenia = new Obrázok(16, 16);
	public final static Obrázok ikonaNeoznačenia = new Obrázok(16, 16);

	// Čiary:
	private final static BasicStroke čiaraTvorbySpojníc = new BasicStroke(
		2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
		new float[]{6.0f, 6.0f}, 0.0f);

	private final static BasicStroke čiaraMriežky = new BasicStroke(
		0.65f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
		new float[]{5.0f, 5.0f}, 0.0f);


	// Globálny čas a rôzne príznaky a parametre:
	public static double čas = 0;
	public static double dilatácia = 1.0;

	private static int klik = 0;
	private static MouseEvent myš = null;

	private static boolean pauza = true;
	private static boolean krokuj = false;

	private static boolean zobrazInformácie = true;
	private static boolean zobrazMriežku = false;

	private static double mriežkaX = 20.0, mriežkaY = 0.0;
	private static double mriežkaŠírka = 20.0, mriežkaVýška = 0.0;
	private static double mriežkaUhol = 15.0;

	public static double globálneX = 0.0;
	public static double globálneY = 0.0;


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
	private final static String snapGrid = "snapGrid";
	private final static String toggleGrid = "toggleGrid";
	private final static String configGrid = "configGrid";
	private final static String deleteSelection = "deleteSelection";
	private final static String deleteConnectors = "deleteConnectors";
	private final static String duplicateSelection = "duplicateSelection";

	private final static String newLink = "newLink";
	private final static String newConnector = "newConnector";
	private final static String editLabels = "editLabels";

	private final static String clearPurpose = "clearPurpose";
	private final static String changeToEmitors = "changeToEmitors";
	private final static String changeToBuffers = "changeToBuffers";
	private final static String changeToWaitRooms = "changeToWaitRooms";
	private final static String changeToConveyors = "changeToConveyors";
	private final static String changeToChangers = "changeToChangers";
	private final static String changeToReleasers = "changeToReleasers";

	private final static String clearShapes = "clearShapes";
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

	private final static String toggleTypes = "toggleTypes";
	private final static String toggleTier = "toggleTier";
	private final static String toggleStepSim = "toggleStepSim";

	private final static String step = "step";


	// Hlavná ponuka:

	// private static PoložkaPonuky položkaPrepniPrichytávanie;
	private static PoložkaPonuky položkaPrepniMriežku;

	private static KontextováPoložka položkaZrušÚčely;
	private static KontextováPoložka položkaZmeňNaEmitory;
	private static KontextováPoložka položkaZmeňNaZásobníky;
	private static KontextováPoložka položkaZmeňNaČakárne;
	private static KontextováPoložka položkaZmeňNaDopravníky;
	private static KontextováPoložka položkaZmeňNaMeniče;
	private static KontextováPoložka položkaZmeňNaUvoľňovače;

	private static KontextováPoložka položkaZrušTvary;
	private static KontextováPoložka položkaZmeňNaElipsy;
	private static KontextováPoložka položkaZmeňNaObdĺžniky;
	private static KontextováPoložka položkaZmeňNaInéTvary;

	private static PoložkaPonuky položkaSpusti;
	private static PoložkaPonuky položkaPrepniInformácie;
	private static PoložkaPonuky položkaPrepniTypy;
	private static PoložkaPonuky položkaPrepniHladiny;
	private static PoložkaPonuky položkaPrepniKrokovanie;
	private static PoložkaPonuky položkaKrok;

	private static SpoločnáKontextováPonuka spoločnáKontextováPonuka;


	// Rôzne príznaky a pomocné atribúty:
	private boolean posúvajObjekty = false;
	private boolean tvorVýber = false;
	private final Vector<Linka> upravujVýber = new Vector<>();
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

		kresliDoObrázka(ikonaOznačenia);
		kruh(3);
		kresliDoObrázka(ikonaNeoznačenia);
		kružnica(3.5);
		kresliNaPodlahu();

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
		pridajKlávesovúSkratku(snapGrid, VK_R, 0);
		pridajKlávesovúSkratku(toggleGrid, VK_G, 0);
		pridajKlávesovúSkratku(configGrid, VK_G, SKRATKA_PONUKY |
			SHIFT_MASK | ALT_MASK);
		pridajKlávesovúSkratku(duplicateSelection, VK_D);
		pridajKlávesovúSkratku(deleteSelection, VK_DELETE, 0);
		pridajKlávesovúSkratku(deleteConnectors, VK_DELETE,
			SKRATKA_PONUKY | SHIFT_MASK);
		pridajKlávesovúSkratku(newLink, VK_M);
		pridajKlávesovúSkratku(newConnector, VK_M, SKRATKA_PONUKY |
			SHIFT_MASK);
		pridajKlávesovúSkratku(editLabels, VK_F2, 0);

		pridajKlávesovúSkratku(clearPurpose, VK_0);
		pridajKlávesovúSkratku(changeToEmitors, VK_1);
		pridajKlávesovúSkratku(changeToBuffers, VK_2);
		pridajKlávesovúSkratku(changeToWaitRooms, VK_3);
		pridajKlávesovúSkratku(changeToConveyors, VK_4);
		pridajKlávesovúSkratku(changeToChangers, VK_5);
		pridajKlávesovúSkratku(changeToReleasers, VK_6);

		pridajKlávesovúSkratku(clearShapes, VK_0,
			SKRATKA_PONUKY | ALT_MASK);
		pridajKlávesovúSkratku(changeToEllipses, VK_1,
			SKRATKA_PONUKY | ALT_MASK);
		pridajKlávesovúSkratku(changeToRectangles, VK_2,
			SKRATKA_PONUKY | ALT_MASK);
		pridajKlávesovúSkratku(changeToRoundRects, VK_3,
			SKRATKA_PONUKY | ALT_MASK);

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

		pridajKlávesovúSkratku(toggleTypes, VK_T, 0);
		pridajKlávesovúSkratku(toggleTier, VK_H, 0);
		pridajKlávesovúSkratku(toggleStepSim, VK_F8);
		pridajKlávesovúSkratku(step, VK_F8, ALT_MASK);

		vymažPonuku();


		spoločnáKontextováPonuka = new SpoločnáKontextováPonuka();

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

		{
			PoložkaPonuky undoItem = pridajPoložkuPonuky("Späť", VK_S);
			undoItem.príkaz(undo);
			PoložkaPonuky redoItem = pridajPoložkuPonuky("Znova", VK_O);
			redoItem.príkaz(redo);

			// TODO — vymaž po implementácií príkazov undo/redo:
			undoItem.setEnabled(false);
			redoItem.setEnabled(false);
		}

		pridajOddeľovačPonuky();

		pridajPoložkuPonuky("Označ všetky linky", VK_A).príkaz(selectAll);
		pridajPoložkuPonuky("Zruš označenie linek", VK_R).príkaz(deselectAll);
		pridajPoložkuPonuky("Označ nasledujúcu linku",
			VK_N).príkaz(selectNext);
		pridajPoložkuPonuky("Označ predchádzajúcu linku",
			VK_P).príkaz(selectPrevious);

		pridajOddeľovačPonuky();

		pridajPoložkuPonuky("Vystreď na označenie",
			VK_Y).príkaz(centerSelection);
		pridajPoložkuPonuky("Zarovnaj polohy označených",
			VK_Z).príkaz(locgridSelection);
		pridajPoložkuPonuky("Zaokrúhli rozmery označených",
			VK_K).príkaz(sizgridSelection);
		pridajPoložkuPonuky("Zaokrúhli pootočenia označených",
			VK_T).príkaz(anggridSelection);
		položkaPrepniMriežku = pridajPoložkuPonuky(
			"Prepni zobrazenie mriežky", VK_M);
		položkaPrepniMriežku.príkaz(toggleGrid);
		položkaPrepniMriežku.ikona(zobrazMriežku ?
			ikonaOznačenia : ikonaNeoznačenia);
		pridajPoložkuPonuky("Konfiguruj mriežky", VK_G).príkaz(configGrid);

		pridajOddeľovačPonuky();

		pridajPoložkuPonuky("Duplikuj označenie",
			VK_D).príkaz(duplicateSelection);
		pridajPoložkuPonuky("Vymaž označené…", VK_V).príkaz(deleteSelection);
		pridajPoložkuPonuky("Zruš spojenia označených…",
			VK_V).príkaz(deleteConnectors);


		// Linka:
		pridajPoložkuHlavnejPonuky("Linka", VK_L);

		pridajPoložkuPonuky("Nová linka", VK_N).príkaz(newLink);
		pridajPoložkuPonuky("Vytvor spojenia", VK_O).príkaz(newConnector);

		pridajOddeľovačPonuky();

			položkaZrušÚčely = new KontextováPoložka(
				"Zruš účely");
			položkaZrušÚčely.setMnemonic(VK_R);
			položkaZrušÚčely.príkaz(clearPurpose);
			položkaZmeňNaEmitory = new KontextováPoložka(
				"Zmeň na emitory");
			položkaZmeňNaEmitory.setMnemonic(VK_E);
			položkaZmeňNaEmitory.príkaz(changeToEmitors);
			položkaZmeňNaZásobníky = new KontextováPoložka(
				"Zmeň na zásobníky");
			položkaZmeňNaZásobníky.setMnemonic(VK_Z);
			položkaZmeňNaZásobníky.príkaz(changeToBuffers);
			položkaZmeňNaČakárne = new KontextováPoložka(
				"Zmeň na čakárne");
			položkaZmeňNaČakárne.setMnemonic(VK_A);
			položkaZmeňNaČakárne.príkaz(changeToWaitRooms);
			položkaZmeňNaDopravníky = new KontextováPoložka(
				"Zmeň na dopravníky");
			položkaZmeňNaDopravníky.setMnemonic(VK_D);
			položkaZmeňNaDopravníky.príkaz(changeToConveyors);
			položkaZmeňNaMeniče = new KontextováPoložka(
				"Zmeň na meniče");
			položkaZmeňNaMeniče.setMnemonic(VK_M);
			položkaZmeňNaMeniče.príkaz(changeToChangers);
			položkaZmeňNaUvoľňovače = new KontextováPoložka(
				"Zmeň na uvoľňovače");
			položkaZmeňNaUvoľňovače.setMnemonic(VK_U);
			položkaZmeňNaUvoľňovače.príkaz(changeToReleasers);

		pridajVnorenúPonuku("Zmeň účel označených",
			položkaZrušÚčely, null, položkaZmeňNaEmitory,
			položkaZmeňNaZásobníky, položkaZmeňNaČakárne,
			položkaZmeňNaDopravníky, položkaZmeňNaMeniče,
			položkaZmeňNaUvoľňovače).setMnemonic(VK_L);

		pridajPoložkuPonuky("Uprav koeficienty označených…",
			VK_K).príkaz(editParams);

		pridajOddeľovačPonuky();

		pridajPoložkuPonuky("Uprav popisy označených…",
			VK_P).príkaz(editLabels);

			položkaZrušTvary = new KontextováPoložka(
				"Nastav predvolené tvary");
			položkaZrušTvary.setMnemonic(VK_N);
			položkaZrušTvary.príkaz(clearShapes);
			položkaZmeňNaElipsy = new KontextováPoložka(
				"Zmeň na elipsy");
			položkaZmeňNaElipsy.setMnemonic(VK_E);
			položkaZmeňNaElipsy.príkaz(changeToEllipses);
			položkaZmeňNaObdĺžniky = new KontextováPoložka(
				"Zmeň na obdĺžniky");
			položkaZmeňNaObdĺžniky.setMnemonic(VK_O);
			položkaZmeňNaObdĺžniky.príkaz(changeToRectangles);
			položkaZmeňNaInéTvary = new KontextováPoložka(
				"Zmeň na iné tvary");
			položkaZmeňNaInéTvary.setMnemonic(VK_I);
			položkaZmeňNaInéTvary.príkaz(changeToRoundRects);

		pridajVnorenúPonuku("Zmeň tvary označených", položkaZrušTvary, null,
			položkaZmeňNaElipsy, položkaZmeňNaObdĺžniky,
			položkaZmeňNaInéTvary).
		setMnemonic(VK_T);

		pridajPoložkuPonuky("Uprav vizuálne parametre označených…",
			VK_Z).príkaz(editVisuals);

		pridajOddeľovačPonuky();

		pridajPoložkuPonuky("Prepni zobrazenie informácií označených liniek",
			VK_I).príkaz(toggleLinksInfo);
		pridajPoložkuPonuky("Skry informácie označených linek",
			VK_S).príkaz(hideLinksInfo);
		pridajPoložkuPonuky("Zobraz informácie označených liniek",
			VK_B).príkaz(showLinksInfo);


		// Simulácia:
		pridajPoložkuHlavnejPonuky("Simulácia", VK_M);

		položkaSpusti = pridajPoložkuPonuky("Spusti/pozastav", VK_S);
		položkaSpusti.príkaz(runPause); repauzuj();

		pridajPoložkuPonuky("Reštartuj", VK_R).príkaz(restart);

		pridajOddeľovačPonuky();

		pridajPoložkuPonuky("Rýchlosť plynutia času…",
			VK_C).príkaz(setTimer);
		položkaPrepniInformácie = pridajPoložkuPonuky(
			"Prepni zobrazenie informácií", VK_I);
		položkaPrepniInformácie.príkaz(toggleInfo);
		položkaPrepniInformácie.ikona(zobrazInformácie ?
			ikonaOznačenia : ikonaNeoznačenia);


		// Ladenie:
		pridajPoložkuHlavnejPonuky("Ladenie", VK_A);

		položkaPrepniTypy = pridajPoložkuPonuky(
			"Prepni zobrazenie typov liniek", VK_T);
		položkaPrepniTypy.príkaz(toggleTypes);
		položkaPrepniTypy.ikona(Linka.zobrazTypy ?
			ikonaOznačenia : ikonaNeoznačenia);

		položkaPrepniHladiny = pridajPoložkuPonuky(
			"Prepni zobrazenie hladín liniek", VK_H);
		položkaPrepniHladiny.príkaz(toggleTier);
		položkaPrepniHladiny.ikona(Linka.zobrazHladiny ?
			ikonaOznačenia : ikonaNeoznačenia);

		pridajOddeľovačPonuky();

		položkaPrepniKrokovanie = pridajPoložkuPonuky(
			"Prepni režim krokovania simulácie", VK_S);
		položkaPrepniKrokovanie.príkaz(toggleStepSim);

		položkaKrok = pridajPoložkuPonuky(
			"Krok (medzera alebo klik na plochu)", VK_K);
		položkaKrok.príkaz(step); rekrokuj();


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

		// Nastavenie predvolenej cesty dialógov nastavujeme až tu, aby sa
		// tým už kvázi nemalo šancu nič pokaziť.
		Súbor.predvolenáCestaDialógov(".");
	}


	// Všetko potrebné pohromade na vytvorenie spoločnej kontextovej ponuky.
	@SuppressWarnings("serial")
	private static class SpoločnáKontextováPonuka extends KontextováPonuka
	{
		private static KontextováPoložka položkaZrušÚčely;
		private static KontextováPoložka položkaZmeňNaEmitory;
		private static KontextováPoložka položkaZmeňNaZásobníky;
		private static KontextováPoložka položkaZmeňNaČakárne;
		private static KontextováPoložka položkaZmeňNaDopravníky;
		private static KontextováPoložka položkaZmeňNaMeniče;
		private static KontextováPoložka položkaZmeňNaUvoľňovače;

		private static KontextováPoložka položkaZrušTvary;
		private static KontextováPoložka položkaZmeňNaElipsy;
		private static KontextováPoložka položkaZmeňNaObdĺžniky;
		private static KontextováPoložka položkaZmeňNaInéTvary;

		public SpoločnáKontextováPonuka()
		{
			super("Hromadná úprava označených liniek");

				(položkaZrušÚčely = new KontextováPoložka(
					"" /* Zruš účely */)).príkaz(clearPurpose);
				(položkaZmeňNaEmitory = new KontextováPoložka(
					"" /* Zmeň na emitory */)).príkaz(changeToEmitors);
				(položkaZmeňNaZásobníky = new KontextováPoložka(
					"" /* Zmeň na zásobníky */)).príkaz(changeToBuffers);
				(položkaZmeňNaČakárne = new KontextováPoložka(
					"" /* Zmeň na čakárne */)).príkaz(changeToWaitRooms);
				(položkaZmeňNaDopravníky = new KontextováPoložka(
					"" /* Zmeň na dopravníky */)).príkaz(changeToConveyors);
				(položkaZmeňNaMeniče = new KontextováPoložka(
					"" /* Zmeň na meniče */)).príkaz(changeToChangers);
				(položkaZmeňNaUvoľňovače = new KontextováPoložka(
					"" /* Zmeň na uvoľňovače */)).príkaz(changeToReleasers);

			pridajPonuku("Zmeň účel označených",
				položkaZrušÚčely, null, položkaZmeňNaEmitory,
				položkaZmeňNaZásobníky, položkaZmeňNaČakárne,
				položkaZmeňNaDopravníky, položkaZmeňNaMeniče,
				položkaZmeňNaUvoľňovače);

			pridajPoložku("" /* Uprav koeficienty označených… */).
				príkaz(editParams);

			pridajOddeľovač();

			pridajPoložku("" /* Uprav popisy označených… */).
				príkaz(editLabels);

				(položkaZrušTvary = new KontextováPoložka(
					"" /* Nastav predvolené tvary */)).príkaz(clearShapes);
				(položkaZmeňNaElipsy = new KontextováPoložka(
					"" /* Zmeň na elipsy */)).príkaz(changeToEllipses);
				(položkaZmeňNaObdĺžniky = new KontextováPoložka(
					"" /* Zmeň na obdĺžniky */)).príkaz(changeToRectangles);
				(položkaZmeňNaInéTvary = new KontextováPoložka(
					"" /* Zmeň na iné tvary */)).príkaz(changeToRoundRects);

			pridajPonuku("Zmeň tvary označených", položkaZrušTvary,
				null, položkaZmeňNaElipsy, položkaZmeňNaObdĺžniky,
				položkaZmeňNaInéTvary);

			pridajPoložku("" /* Uprav vizuálne parametre označených… */).
				príkaz(editVisuals);

			pridajOddeľovač();

			pridajPoložku(""
				/* Prepni zobrazenie informácií označených liniek */).
				príkaz(toggleLinksInfo);
			pridajPoložku("" /* Skry informácie označených linek */).
				príkaz(hideLinksInfo);
			pridajPoložku("" /* Zobraz informácie označených liniek*/).
				príkaz(showLinksInfo);

			pridajOddeľovač();

			pridajPoložku("" /* Vymaž označené… */).
				príkaz(deleteSelection);
		}
	}


	// Rôzne akcie väčšinou zodpovedajúce vykonaniu príkazov položiek ponuky:

	public static boolean zobrazInformácie()
	{
		return zobrazInformácie;
	}

	public static void zobrazInformácie(boolean zobrazInformácie)
	{
		Systém.zobrazInformácie = zobrazInformácie;
		položkaPrepniInformácie.ikona(zobrazInformácie ?
			ikonaOznačenia : ikonaNeoznačenia);
		žiadajPrekreslenie();
	}

	public static void prepniZobrazenieInformácií()
	{
		zobrazInformácie = !zobrazInformácie;
		položkaPrepniInformácie.ikona(zobrazInformácie ?
			ikonaOznačenia : ikonaNeoznačenia);
		žiadajPrekreslenie();
	}

	public static double mriežkaX()
	{
		if (0 == mriežkaX) return mriežkaY;
		return mriežkaX;
	}

	public static double mriežkaY()
	{
		if (0 == mriežkaY) return mriežkaX;
		return mriežkaY;
	}

	public static double mriežkaŠírka()
	{
		if (0 == mriežkaŠírka)
		{
			if (0 == mriežkaVýška) return mriežkaX();
			return mriežkaVýška;
		}
		return mriežkaŠírka;
	}

	public static double mriežkaVýška()
	{
		if (0 == mriežkaVýška)
		{
			if (0 == mriežkaŠírka) return mriežkaY();
			return mriežkaŠírka;
		}
		return mriežkaVýška;
	}

	public static double mriežkaUhol()
	{
		return mriežkaUhol;
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

			// Z globálneho posunu „stačí“ použiť drobný rozdiel v rámci
			// rozostupov mriežky (resp. nie, že „stačí,“ v skutočnosti
			// je to nevyhnutné, lebo väčší posun by mriežku posunul mimo
			// zorné pole…).
			double posunX = globálneX % Πx;
			double posunY = globálneY % Πy;

			double x0 = najmenšieX() - 2 * Πx;
			double x1 = najväčšieX() + 2 * Πx;
			x0 = floor(x0 / Πx) * Πx + posunX;
			x1 = floor(x1 / Πx) * Πx;

			double y0 = najmenšieY() - 2 * Πy;
			double y1 = najväčšieY() + 2 * Πy;
			y0 = floor(y0 / Πy) * Πy + posunY;
			y1 = floor(y1 / Πy) * Πy;

			// Upozornenie: Mriežku rozmerov nemá zmysel kresliť, lebo objekty
			//      nie sú povinné byť zarovnané k mriežke polohy. Z toho
			//      vyplýva, že mriežka rozmerov nemá šancu byť správne
			//      zarovnaná (pretože nejestvuje žiadne „správne“ zarovnanie).

			farba(papierová);
			čiara(čiaraMriežky);

			for (double x = x0; x <= x1; x += Πx)
			{
				skočNa(x, y0);
				choďNa(x, y1);
			}

			for (double y = y0; y <= y1; y += Πy)
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
		Systém.zobrazMriežku = zobrazMriežku;
		položkaPrepniMriežku.ikona(zobrazMriežku ?
			ikonaOznačenia : ikonaNeoznačenia);
		prekresliMriežku();
	}

	public void prepniZobrazenieMriežky()
	{
		zobrazMriežku = !zobrazMriežku;
		položkaPrepniMriežku.ikona(zobrazMriežku ?
			ikonaOznačenia : ikonaNeoznačenia);
		prekresliMriežku();
	}

	public static void repauzuj()
	{
		pauza = pauza || 0 == Linka.početAktívnych();
		položkaSpusti.ikona(pauza ? ikonaNeoznačenia : ikonaOznačenia);
	}

	private static void pauza(boolean pauza)
	{
		Systém.pauza = pauza || 0 == Linka.početAktívnych();
		položkaSpusti.ikona(Systém.pauza ? ikonaNeoznačenia : ikonaOznačenia);
	}

	private static void rekrokuj()
	{
		položkaPrepniKrokovanie.ikona(krokuj ?
			ikonaOznačenia : ikonaNeoznačenia);
		položkaKrok.setEnabled(krokuj);
	}

	private static void krokuj(boolean pauza)
	{
		Systém.krokuj = krokuj;
		rekrokuj();
	}

	public void vymažSpojniceNaKurzore()
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
					spojnica.zdroj().zrušSpojnicu(spojnica.cieľ());
			}
		}
		repauzuj();
		Linka.zaraďDoHladín();
	}

	public void newSystem()
	{
		if (ÁNO == otázka("<html>Vykonanie tejto funkcie spôsobí stratu" +
			"<br />všetkých prípadných neuložených údajov.<br /> <br />" +
			"<b style='font-size: 125%'>Ste si naozaj istý,<br />" +
			"že ju chcete vykonať?</b><br /> </html>", "Nový systém"))
			resetSystému();
	}

	public void openSystem()
	{
		varovanie("Prípadné neuložené zmeny\naktuálneho systému sa stratia.",
			"Otvoriť systém");

		String cesta = Súbor.dialógOtvoriť("Otvoriť systém", "",
			"Konfigurácia systému hromadnej obsluhy (*.ksho)");
		if (null != cesta)
		{
			if (!cesta.endsWith(".ksho")) cesta += ".ksho";
			if (!Súbor.jestvuje(cesta)) chyba("Súbor „" + cesta +
				"“ nejestvuje.", "Súbor nebol nájdený");
			else
			{
				resetSystému();

				Súbor súbor = new Súbor();
				try
				{
					súbor.otvorNaČítanie(cesta);
					čítajKonfiguráciu2(súbor);
				}
				catch (Exception e)
				{
					chyba("Nastala chyba pri čítaní súboru\n„" + cesta +
						"“.", "Otvoriť systém");
				}
				finally
				{
					try { súbor.zavri(); } catch (Throwable t)
					{/* S tým aj tak nič nenarobím. */}
					súbor = null;
				}
			}
		}
	}

	public void saveSystem()
	{
		String cesta = Súbor.dialógUložiť("Uložiť systém", "",
			"Konfigurácia systému hromadnej obsluhy (*.ksho)");
		if (null != cesta)
		{
			if (!cesta.endsWith(".ksho")) cesta += ".ksho";

			if (!Súbor.jestvuje(cesta) || ÁNO == otázka(
				"Zadaný súbor:\n" + cesta + "\njestvuje.\n"+
				"Chcete ho prepísať?", "Uložiť systém"))
			{
				Súbor súbor = new Súbor();
				try
				{
					súbor.otvorNaZápis(cesta);
					zapíšKonfiguráciu(súbor);
				}
				catch (Exception e)
				{
					chyba("Nastala chyba pri zápise súboru\n„" + cesta +
						"“.", "Otvoriť systém");
				}
				finally
				{
					try { súbor.zavri(); } catch (Throwable t)
					{/* S tým aj tak nič nenarobím. */}
					súbor = null;
				}
			}
		}
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
		double posunX = globálneX % Πx;
		double posunY = globálneY % Πy;
		for (Linka linka : označené) linka.posuňAjZákazníkov(
			floor(linka.polohaX() / Πx) * Πx - linka.polohaX() + posunX,
			floor(linka.polohaY() / Πy) * Πy - linka.polohaY() + posunY);
	}

	public void sizgridSelection()
	{
		if (0 == mriežkaŠírka && 0 == mriežkaVýška) return;
		Linka[] označené = Linka.dajOznačené();
		double Πšírka = mriežkaŠírka, Πvýška = mriežkaVýška;
		if (0 == mriežkaŠírka) Πšírka = mriežkaVýška;
		if (0 == mriežkaVýška) Πvýška = mriežkaŠírka;
		for (Linka linka : označené)
		{
			linka.rozmery(
				floor(linka.šírka() / (2 * Πšírka)) * (2 * Πšírka),
				floor(linka.výška() / (2 * Πvýška)) * (2 * Πvýška));
			linka.aktualizujZaoblenie();
			linka.aktualizujSpojnice();
		}
	}

	public void anggridSelection()
	{
		if (0 == mriežkaUhol) return;
		Linka[] označené = Linka.dajOznačené();
		for (Linka linka : označené) linka.uhol(
			floor(linka.uhol() / mriežkaUhol) * mriežkaUhol);
	}


	private final static String[] popisyMriežky = new String[]
		{"<html><i>Poznámky: Pole „Mriežka“ dokáźe prijať až štyri<br />" +
			"hodnoty oddelené bodkočiarkami: mriežka súradníc<br />x, y " +
			"a mriežka rozmerov (šírky a výšky). Chýbajúce<br />(aj " +
			"vynechané) údaje sú doplnené automaticky.</i><br /> " +
			"<br />Mriežka:</html>", "Zaokrúhlenie uhlov:"};

	public void configGrid()
	{
		StringBuffer sb = new StringBuffer();

		double Πx = mriežkaX, Πy = mriežkaY,
			Πšírka = mriežkaŠírka, Πvýška = mriežkaVýška;

		if (0 == mriežkaX) Πx = mriežkaY;
		if (0 == mriežkaY) Πy = mriežkaX;
		if (0 == mriežkaŠírka)
		{
			if (0 == mriežkaVýška)
				Πšírka = Πx;
			else
				Πšírka = mriežkaVýška;
		}
		if (0 == mriežkaVýška)
		{
			if (0 == mriežkaŠírka)
				Πšírka = Πy;
			else
				Πvýška = mriežkaŠírka;
		}

		sb.append(Πx);
		if (Πx != Πy || Πx != Πšírka || Πšírka != Πvýška)
		{
			sb.append("; ");
			if (Πx != Πy) sb.append(Πy);
		}

		if (Πx != Πšírka || Πšírka != Πvýška)
		{
			sb.append("; ");
			sb.append(Πšírka);
			if (Πšírka != Πvýška)
			{
				sb.append("; ");
				sb.append(Πvýška);
			}
		}

		Object[] údaje = {sb.toString(), mriežkaUhol};

		if (dialóg(popisyMriežky, údaje, "Mriežky"))
		{
			if (null != údaje[0] && !((String)údaje[0]).isEmpty())
			{
				String údaj = (String)údaje[0];
				if (-1 != údaj.indexOf(';'))
				{
					String[] časti = údaj.split(";");
					if (0 != časti.length)
					{
						Double[] hodnoty = new Double[časti.length];
						for (int i = 0; i < časti.length; ++i)
							hodnoty[i] = reťazecNaReálneČíslo(časti[i]);

						if (hodnoty.length > 0 && null != hodnoty[0] &&
							!Double.isNaN(hodnoty[0]))
						{
							mriežkaŠírka = mriežkaX =
								Double.isFinite(hodnoty[0]) ? hodnoty[0] : 20.0;
							mriežkaVýška = mriežkaY = 0;
						}

						if (hodnoty.length > 1 && null != hodnoty[1] &&
							!Double.isNaN(hodnoty[1]))
						{
							mriežkaVýška = mriežkaY =
								Double.isFinite(hodnoty[1]) &&
								!hodnoty[1].equals(mriežkaX) ? hodnoty[1] : 0.0;
						}

						if (hodnoty.length > 2 && null != hodnoty[2] &&
							!Double.isNaN(hodnoty[2]))
						{
							mriežkaŠírka = Double.isFinite(hodnoty[2]) ?
								hodnoty[2] : mriežkaX;
						}

						if (hodnoty.length > 3 && null != hodnoty[3] &&
							!Double.isNaN(hodnoty[3]))
						{
							mriežkaVýška = Double.isFinite(hodnoty[3]) &&
								!hodnoty[3].equals(mriežkaŠírka) ?
								hodnoty[3] : 0.0;
						}
					}
				}
				else
				{
					údaj = údaj.trim();
					Double hodnota = reťazecNaReálneČíslo(údaj);
					if (!Double.isNaN(hodnota))
					{
						mriežkaŠírka = mriežkaX = null != hodnota &&
							Double.isFinite(hodnota) ? hodnota : 20.0;
						mriežkaVýška = mriežkaY = 0;
					}
				}

				prekresliMriežku();
			}

			if (!Double.isNaN((Double)údaje[1]))
			{
				Double údaj = (Double)údaje[1];
				mriežkaUhol = null != údaj && Double.isFinite(údaj) ?
					údaj : 15.0;
			}
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

		repauzuj();
		Linka.zaraďDoHladín();
	}

	public void newLink()
	{
		Linka.pridaj(zadajReťazec("Zadajte popis novej linky:",
			"Popis linky")).skočNaMyš();
		repauzuj();
		// Tu nie je treba zaradenie do hladín, lebo nová „svieža“ linka ešte
		// nemá žiadne spojnice a dôležité sú práve tie.
	}

	private final static Vector<Linka> connectorsFrom = new Vector<>();

	public static boolean jeZačiatokKonektora(Linka linka)
	{
		if (connectorsFrom.isEmpty()) return false;
		return -1 != connectorsFrom.indexOf(linka);
	}

	public static void newConnector()
	{
		if (0 == Linka.početOznačených())
			varovanie("Nie sú označené žiadne linky.", "Vytvorenie spojení");
		else
		{
			if (0 == connectorsFrom.size())
				Collections.addAll(connectorsFrom, Linka.dajOznačené());
			else
			{
				Linka[] označené = Linka.dajOznačené();

				for (Linka linka : označené)
					if (-1 != connectorsFrom.indexOf(linka))
					{
						chyba("Zdrojová a cieľová množina liniek na " +
							"vytvorenie\nnových spojení nesmie mať prienik!",
							"Vytvorenie spojení");
						return;
					}

				for (Linka linka1 : connectorsFrom)
					for (Linka linka2 : označené)
						linka1.spojnica(linka2);

				connectorsFrom.clear();
				Linka.zaraďDoHladín();
			}
		}
	}

	public static void deleteConnectors()
	{
		if (0 == Linka.početOznačených())
			varovanie("Nie sú označené žiadne linky.", "Vymazanie spojení");
		else if (ÁNO == otázka("Skutočne chcete vymazať všetky\nspojenia " +
			"medzi označenými linkami?", "Potvrdenie vymazania spojení"))
		{
			Linka[] označené = Linka.dajOznačené();
			for (int i = označené.length - 1; i >= 1; --i)
				for (int j = i - 1; j >= 0; --j)
				{
					označené[i].zrušSpojnicu(označené[j]);
					označené[j].zrušSpojnicu(označené[i]);
				}
		}
	}

	public void runPause()
	{
		if (pauza) spustiČasomieru();
		pauza(!pauza);
	}

	private void resetSystému()
	{
		pauza(true);
		krokuj(false);
		resetSimulácie();

		dilatácia = 1.0;

		zobrazInformácie(true);
		zobrazMriežku(false);

		mriežkaX = 20.0; mriežkaY = 0.0;
		mriežkaŠírka = 20.0; mriežkaVýška = 0.0;
		mriežkaUhol = 15.0; prekresliMriežku();
		globálneX = globálneY = 0.0;

		Linka.vymažVšetko();
	}

	private void resetSimulácie()
	{
		čas = 0;
		Zákazník.vyčisti();
		Linka.vyčisti();
		odídených = 0;
		vybavených = 0;
	}

	public void restart()
	{
		if (ÁNO == otázka("Skutočne chcete reštartovať simuláciu?",
			"Potvrdenie reštartu")) resetSimulácie();
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
		else if (deleteConnectors == príkaz) deleteConnectors();
		else if (newLink == príkaz) newLink();
		else if (newConnector == príkaz) newConnector();
		else if (editLabels == príkaz) Linka.upravPopisy();

		else if (clearPurpose == príkaz) Linka.zrušÚčely();
		else if (changeToEmitors == príkaz) Linka.zmeňNaEmitory();
		else if (changeToBuffers == príkaz) Linka.zmeňNaZásobníky();
		else if (changeToWaitRooms == príkaz) Linka.zmeňNaČakárne();
		else if (changeToConveyors == príkaz) Linka.zmeňNaDopravníky();
		else if (changeToChangers == príkaz) Linka.zmeňNaMeniče();
		else if (changeToReleasers == príkaz) Linka.zmeňNaUvoľňovače();

		else if (clearShapes == príkaz) Linka.zrušTvary();
		else if (changeToEllipses == príkaz) Linka.zmeňNaElipsy();
		else if (changeToRectangles == príkaz) Linka.zmeňNaObdĺžniky();
		else if (changeToRoundRects == príkaz) Linka.zmeňNaInéTvary();

		else if (editParams == príkaz) Linka.upravKoeficientyOznačených();
		else if (editVisuals == príkaz) Linka.upravVizuályOznačených();
		else if (toggleLinksInfo == príkaz) Linka.prepniInformácieOznačených();
		else if (hideLinksInfo == príkaz) Linka.skryInformácieOznačených();
		else if (showLinksInfo == príkaz) Linka.zobrazInformácieOznačených();
		else if (runPause == príkaz) runPause();
		else if (restart == príkaz) restart();
		else if (setTimer == príkaz) setTimer();
		else if (toggleInfo == príkaz) prepniZobrazenieInformácií();
		else if (toggleTypes == príkaz)
		{
			Linka.zobrazTypy = !Linka.zobrazTypy;
			položkaPrepniTypy.ikona(Linka.zobrazTypy ?
				ikonaOznačenia : ikonaNeoznačenia);
		}
		else if (toggleTier == príkaz)
		{
			Linka.zobrazHladiny = !Linka.zobrazHladiny;
			položkaPrepniHladiny.ikona(Linka.zobrazHladiny ?
				ikonaOznačenia : ikonaNeoznačenia);
		}
		else if (toggleStepSim == príkaz) krokuj(!krokuj);
		else if (step == príkaz) krok();
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
		case MEDZERA: if (debugOn || krokuj) krok(); break;
		case ESCAPE:
			if (!connectorsFrom.isEmpty())
				connectorsFrom.clear();
			else
				deselectAll();
			break;
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
				// Upravuj výber.
				posúvajObjekty = false;
				tvorVýber = true;
				upravujVýber.clear();
				Linka[] označené = Linka.dajOznačené();
				for (Linka označená : označené)
					upravujVýber.add(označená);
				tvorSpojnicu = false;
				mažSpojnicu = false;
				začiatokAkcie = polohaMyši();
				koniecAkcie = null;
			}
			else if (myš().isControlDown())
			{
				// Tvor spojnicu.
				posúvajObjekty = false;
				tvorVýber = false;
				upravujVýber.clear();
				tvorSpojnicu = true;
				mažSpojnicu = false;
				začiatokAkcie = polohaMyši();
				koniecAkcie = null;
			}
			else if (myš().isAltDown())
			{
				// Maž spojnicu.
				posúvajObjekty = false;
				tvorVýber = false;
				upravujVýber.clear();
				tvorSpojnicu = false;
				mažSpojnicu = true;
				začiatokAkcie = polohaMyši();
				koniecAkcie = null;
			}
			else if (myš().isShiftDown())
			{
				// Posúvaj objekty.
				posúvajObjekty = true;
				tvorVýber = false;
				upravujVýber.clear();
				tvorSpojnicu = false;
				mažSpojnicu = false;
			}
			else if (!Linka.myšVOznačenej())
			{
				// Tvor výber ak nie je žiadna linka označená.
				posúvajObjekty = false;
				tvorVýber = true;
				upravujVýber.clear();
				tvorSpojnicu = false;
				mažSpojnicu = false;
				začiatokAkcie = polohaMyši();
				koniecAkcie = null;
			}
		}
		else
		{
			// Posúvaj objekty stredným tlačidlom.
			posúvajObjekty = tlačidloMyši(STREDNÉ);
			tvorVýber = false;
			upravujVýber.clear();
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
			globálneX += p.getX();
			globálneY += p.getY();
			prekresliMriežku();
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
						if (-1 != upravujVýber.indexOf(linka)) continue;
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
						Linka.zaraďDoHladín();
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

	@Override public void prijatieVýzvy(GRobot autor, int kľúč)
	{
		// Keď ľubovoľná linka zaznamená začatie jej úprav, odošle príkaz
		// na zrušenie akýchkoľvek globálnych úprav:
		posúvajObjekty = false;
		tvorVýber = true;
		upravujVýber.clear();
		tvorSpojnicu = false;
		mažSpojnicu = false;
		začiatokAkcie = null;
		koniecAkcie = null;
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
					if (-1 != upravujVýber.indexOf(linka)) continue;
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
		if (debugOn || krokuj)
		{
			čas += 0.15;
			if (debugOn) System.out.println("\nkrok(" + čas + ")");
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
		if (debugOn || krokuj) krok(); else
		if (myš().getClickCount() > 1)
		{
			if (myš().isControlDown()) vymažSpojniceNaKurzore();
			else if (!Linka.myšVOznačenej())
			{
				if (tlačidloMyši(ĽAVÉ))
				{
					Linka.blikniOznačené();
					Linka.upravKoeficientyOznačených();
				}
				else if (tlačidloMyši(PRAVÉ))
				{
					klik = 5;
					myš = myš();
				}
			}
		}
		else if (tlačidloMyši(PRAVÉ) && !Linka.myšVOznačenej())
		{
			klik = 5;
			myš = myš();
		}
	}

	@Override public void tik()
	{
		if (klik > 0 && 0 >= --klik && null != myš)
		{
			if (0 < Linka.početOznačených())
			{
				Linka.blikniOznačené();
				if (myš.getClickCount() > 1)
					Svet.vykonaťNeskôr(() -> Linka.upravVizuályOznačených());
				else
					Svet.vykonaťNeskôr(() -> spoločnáKontextováPonuka.zobraz());
			}
			myš = null;
		}

		if (!debugOn && !krokuj) krok();
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

		súbor.zapíšVlastnosť("Δx", globálneX);
		súbor.zapíšVlastnosť("Δy", globálneY);

		try { Tvar.ulož(súbor); }
		catch (Throwable t) { t.printStackTrace(); }

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
			Double hodnota = súbor.čítajVlastnosť("Δx", globálneX);
			globálneX = null == hodnota ? 1.0 : hodnota;
			hodnota = súbor.čítajVlastnosť("Δy", globálneY);
			globálneY = null == hodnota ? 1.0 : hodnota;
		}

		{
			Boolean hodnota = súbor.čítajVlastnosť("pauza", false);
			pauza = null == hodnota ? false : hodnota;
			hodnota = súbor.čítajVlastnosť("zobrazInformácie", true);
			zobrazInformácie(null == hodnota ? true : hodnota);
			hodnota = súbor.čítajVlastnosť("zobrazMriežku", false);
			zobrazMriežku(null == hodnota ? true : hodnota);
		}

		{
			int početLiniek = Linka.počet();
			for (int i = 0; i < početLiniek; ++i)
				Linka.daj(i).deaktivuj();
		}

		try { Tvar.vymaž(); Tvar.čítaj(súbor); }
		catch (Throwable t) { t.printStackTrace(); }

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

		try { Tvar.importuj("tvary"); }
		catch (Throwable t) { t.printStackTrace(); }

		repauzuj();
		Linka.zaraďDoHladín();
	}


	// Hlavná metóda:
	public static void main(String[] args)
	{
		if (debugOn) režimLadenia(true, true);

		try { použiKonfiguráciu("Systém.cfg"); } catch (Throwable t)
		{
			Svet.chyba(t.getMessage(), "Chyba!");
			Svet.koniec();
			return;
		}

		Svet.skry();
		try { new Systém(); } catch (Throwable t) { t.printStackTrace(); }
		if (prvéSpustenie()) vystreď();
		Svet.zobraz();
	}
}
