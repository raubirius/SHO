
import java.awt.BasicStroke;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;
import knižnica.*;
import static knižnica.Kláves.*;
import static knižnica.Svet.*;
import static knižnica.ÚdajeUdalostí.*;

import static debug.Debug.*;

/*
TODO:

 • Zarovnanie na mriežku. Upraviť parameter mriežky.
 • Úprava vzhľadových vlastností. (pomer, veľkosť, zaoblenie, písmo, poloha
   popisu, viacriadkovosť popisu)
 • (podfarbenie? zmena poradia?)

 • (kapacita do dopravníka je zbytočná; úvodné zdžanie (aby mohol „nastúpiť“
    ďalší zákazník) tiež nie je potrebné, všetko sa dá docieliť vhodnou
    kombináciou rôznych typov liniek)

*/

public class Systém extends GRobot
{
	// Evidencia:
	public final static Vector<Činnosť> činnosti = new Vector<>();

	// Ikona na označenie položiek ponúk:
	public final static Obrázok ikonaOznačenia = new Obrázok(16, 16);


	// Globálny čas:
	public static double čas = 0;
	public static double dilatácia = 1.0;


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
	private final static String editParams = "editParams";


	// Hlavná ponuka:
	// TODO-del private PoložkaPonuky položkaUpravPopisy;

	private KontextováPoložka položkaZmeňNaEmitory;
	private KontextováPoložka položkaZmeňNaZásobníky;
	private KontextováPoložka položkaZmeňNaDopravníky;
	private KontextováPoložka položkaZmeňNaMeniče;
	private KontextováPoložka položkaZmeňNaUvoľňovače;

	private KontextováPoložka položkaZmeňNaElipsy;
	private KontextováPoložka položkaZmeňNaObdĺžniky;
	private KontextováPoložka položkaZmeňNaObléObdĺžniky;

	// TODO-del private PoložkaPonuky položkaUpravKoeficientyOznačených;


	// Rôzne príznaky a pomocné atribúty:
	private boolean posúvajObjekty = false;
	private boolean tvorSpojnicu = false;
	private boolean mažSpojnicu = false;
	private Bod začiatokSpojnice = null;
	private Bod koniecSpojnice = null;


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
		pridajKlávesovúSkratku(editParams, VK_F9, 0);

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
		// TODO-del (položkaUpravPopisy =
		pridajPoložkuPonuky("Uprav popisy označených…",
			VK_P).príkaz(editLabels);


		položkaZmeňNaEmitory = new KontextováPoložka("Zmeň na emitory");
		položkaZmeňNaEmitory.setMnemonic(VK_E);
		položkaZmeňNaZásobníky = new KontextováPoložka("Zmeň na zásobníky");
		položkaZmeňNaZásobníky.setMnemonic(VK_Z);
		položkaZmeňNaDopravníky = new KontextováPoložka("Zmeň na dopravníky");
		položkaZmeňNaDopravníky.setMnemonic(VK_D);
		položkaZmeňNaMeniče = new KontextováPoložka("Zmeň na meniče");
		položkaZmeňNaMeniče.setMnemonic(VK_M);
		položkaZmeňNaUvoľňovače = new KontextováPoložka("Zmeň na uvoľňovače");
		položkaZmeňNaUvoľňovače.setMnemonic(VK_U);

		pridajVnorenúPonuku("Zmeň funkciu (účel) označených",
			položkaZmeňNaEmitory, položkaZmeňNaZásobníky,
			položkaZmeňNaDopravníky, položkaZmeňNaMeniče,
			položkaZmeňNaUvoľňovače).setMnemonic(VK_F);


		položkaZmeňNaElipsy = new KontextováPoložka("Zmeň na elipsy");
		položkaZmeňNaElipsy.setMnemonic(VK_E);
		položkaZmeňNaObdĺžniky = new KontextováPoložka("Zmeň na obdĺžniky");
		položkaZmeňNaObdĺžniky.setMnemonic(VK_O);
		položkaZmeňNaObléObdĺžniky = new KontextováPoložka(
			"Zmeň na oblé obdĺžniky");
		položkaZmeňNaObléObdĺžniky.setMnemonic(VK_B);

		pridajVnorenúPonuku("Zmeň tvar označených", položkaZmeňNaElipsy,
			položkaZmeňNaObdĺžniky, položkaZmeňNaObléObdĺžniky).
		setMnemonic(VK_T);


		// TODO-del (položkaUpravKoeficientyOznačených =
		pridajPoložkuPonuky("Uprav koeficienty označených…",
			VK_I).príkaz(editParams);
		// pridajOddeľovačPonuky();


		kresliDoObrázka(ikonaOznačenia);
		kruh(3);
		kresliNaStrop();


		// Druhá časť (globálnej) inicializácie:
		spustiČasomieru();
		spustiČasovač();
	}


	// Rôzne akcie väčšinou zodpovedajúce vykonaniu príkazov položiek ponuky:

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
		Linka.pridaj(zadajReťazec("Zadajte popis novej linky",
			"Vlastnosti linky")).skočNaMyš();
	}


	// Obsluha udalostí:

	/* TODO-del private boolean overOznačenie()
	{
		if (0 == Linka.početOznačených())
		{
			varovanie("Nie sú označené žiadne linky.", "Varovanie…");
			return false;
		}
		return true;
	}*/

	@Override public void voľbaPoložkyPonuky()
	{
		// if (položkaUpravPopisy.aktivovaná())
		// TODO-del {
		// 	if (overOznačenie())
				// Linka.upravPopisy();
		// }
		// else if (položkaUpravKoeficientyOznačených.aktivovaná())
		// TODO-del {
		// 	if (overOznačenie())
				// Linka.upravKoeficientyOznačených();
		// }
	}

	@Override public void voľbaKontextovejPoložky()
	{
		if (položkaZmeňNaEmitory.aktivovaná()) Linka.zmeňNaEmitory();
		else if (položkaZmeňNaZásobníky.aktivovaná()) Linka.zmeňNaZásobníky();
		else if (položkaZmeňNaDopravníky.aktivovaná()) Linka.zmeňNaDopravníky();
		else if (položkaZmeňNaMeniče.aktivovaná()) Linka.zmeňNaMeniče();
		else if (položkaZmeňNaUvoľňovače.aktivovaná()) Linka.zmeňNaUvoľňovače();
		else if (položkaZmeňNaElipsy.aktivovaná()) Linka.zmeňNaElipsy();
		else if (položkaZmeňNaObdĺžniky.aktivovaná()) Linka.zmeňNaObdĺžniky();
		else if (položkaZmeňNaObléObdĺžniky.aktivovaná())
			Linka.zmeňNaObléObdĺžniky();
	}

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
		else if (editParams == príkaz) Linka.upravKoeficientyOznačených();
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


	/*
	TODO
	Vytvor klávesnicový spôsob na vytvorenie spojnice medzi linkami:
	 • Označ začiatok
	 • Stlač klávesovú skratku (ESC to ruší)
	 • Označ koniec
	 • Stlač klávesovú skratku
	
	Ctrl + klik na spojnicu = vymazanie.
	*/
	@Override public void stlačenieTlačidlaMyši()
	{
		if (tlačidloMyši(ĽAVÉ))
		{
			if (myš().isControlDown())
			{
				posúvajObjekty = false;
				tvorSpojnicu = false;
				mažSpojnicu = true;
				začiatokSpojnice = polohaMyši();
				koniecSpojnice = null;
			}
			else if (myš().isAltDown())
			{
				posúvajObjekty = false;
				tvorSpojnicu = true;
				mažSpojnicu = false;
				začiatokSpojnice = polohaMyši();
				koniecSpojnice = null;
			}
			else
			{
				posúvajObjekty = myš().isShiftDown();
				tvorSpojnicu = false;
				mažSpojnicu = false;
			}
		}
		else
		{
			posúvajObjekty = tlačidloMyši(STREDNÉ);
			tvorSpojnicu = false;
			mažSpojnicu = false;
		}
	}

	@Override public void ťahanieMyšou()
	{
		if (mažSpojnicu || tvorSpojnicu)
		{
			koniecSpojnice = polohaMyši();
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
		if (mažSpojnicu || tvorSpojnicu)
		{
			if (null != začiatokSpojnice && null != koniecSpojnice)
			{
				Linka začiatok = null, koniec = null;
				Linka[] aktívne = Linka.dajAktívne();
				for (Linka linka : aktívne)
				{
					if (linka.bodV(začiatokSpojnice)) začiatok = linka;
					if (linka.bodV(koniecSpojnice)) koniec = linka;
				}

				if (null != začiatok && null != koniec)
				{
					if (mažSpojnicu)
						začiatok.zrušSpojnicu(koniec);
					else
						začiatok.spojnica(koniec);
				}
			}

			tvorSpojnicu = false;
			mažSpojnicu = false;
			začiatokSpojnice = null;
			koniecSpojnice = null;
		}
		posúvajObjekty = false;
	}

	@Override public void kresliTvar()
	{
		if (mažSpojnicu || tvorSpojnicu)
		{
			if (mažSpojnicu) farba(svetločervená); else farba(svetlošedá);
			if (null != začiatokSpojnice)
				skočNa(začiatokSpojnice);
			if (null != koniecSpojnice)
				choďNa(koniecSpojnice);
		}

		// TODO: dať vypnuteľné:
		{
			farba(čierna);
			skočNa(ľavýOkraj() + 10, hornýOkraj() - výškaRiadka());
			text("Čas: " + F(čas, 3), KRESLI_PRIAMO);
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
			if (tlačidloMyši(ĽAVÉ)) Linka.upravKoeficientyOznačených();
			// TODO upraviť parametre vzhľadu
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
