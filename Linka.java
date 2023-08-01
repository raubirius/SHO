
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Vector;
import knižnica.*;
import knižnica.podpora.ScrollTextPane;
import enumerácie.*;
import static knižnica.Svet.*;
import static knižnica.ÚdajeUdalostí.*;
import static knižnica.log.Log.*;
import static java.lang.Math.*;


public class Linka extends GRobot implements Činnosť
{
	// Blok ladiacich informácií:
	private static int counter = 0; private int idnum = counter++;
	@Override public String toString() { return "Linka_" + idnum + "; čas: " +
	čas + "; účel: " + účel + "; zákazníci.size(): " + zákazníci.size(); }
	public static boolean zobrazTypy = false;
	public static boolean zobrazHladiny = false;


	// Definícia kreslenia koncových značiek čiar spojníc:
	public static KreslenieTvaru šípka = r ->
	{
		r.farba(tmavošedá);
		r.vpravo(30);
		r.dopredu(10);
		r.odskoč(10);
		r.vľavo(60);
		r.dopredu(10);
	};


	// Evidencia liniek:
	private final static Vector<Linka> linky = new Vector<>();


	// Kontextová ponuka:
	private final KontextováPonuka kontextováPonuka;
	private final KontextováPoložka položkaUpravPopis;

	private final KontextováPoložka položkaZrušÚčel;
	private final KontextováPoložka položkaZmeňNaEmitor;
	private final KontextováPoložka položkaZmeňNaZásobník;
	private final KontextováPoložka položkaZmeňNaČakáreň;
	private final KontextováPoložka položkaZmeňNaZastávku;
	private final KontextováPoložka položkaZmeňNaDopravník;
	private final KontextováPoložka položkaZmeňNaMenič;
	private final KontextováPoložka položkaZmeňNaUvoľňovač;

	private final KontextováPoložka položkaRežimVýberuLiniek;

	private final KontextováPoložka položkaZrušTvar;
	private final KontextováPoložka položkaZmeňNaElipsu;
	private final KontextováPoložka položkaZmeňNaObdĺžnik;
	private final KontextováPoložka položkaZmeňNaInýTvar;

	private final KontextováPoložka položkaUpravKoeficienty;
	private final KontextováPoložka položkaUpravZoznamMien;
	private final KontextováPoložka položkaUpravVizuály;
	private final KontextováPoložka položkaPrepniInformácie;
	private final KontextováPoložka položkaVymaž;


	// Verejné štatistiky:
	public int odídených = 0;
	public int vybavených = 0;


	// Vnútorné stavy a vizualizácia:
	private boolean ťaháSa = false;
	private static Linka upravujeSa = null; // Toto riešenie bolo
		// implementované preto, aby sa v prípade úprav jednej linky ťahaním
		// neposúvali ostatné linky.
	private static int typÚprav = -1;
		// (meniťŠírku = 0; točiť = 1; meniťRozmery = 2; meniťVýšku = 3;
		// meniťMieruZaoblenia = 4)
	private boolean označená = false;
	private int bliká = 0;
	private int klik = 0;
	private MouseEvent myš = null;
	private double čas = 0.0;
	private int hladina = -1;
		// Zaraďovanie do hladín je dôležité pri triedení pred každým cyklom
		// času. Prvé musia byť zaradené tie linky, ktoré sú v nižšej
		// („skoršej“) hladine. Zaraďovanie do hladín musí byť vykonané vždy
		// po vložení alebo odstránení spojnice medzi linkami. Detaily
		// o spôsobe zaraďovania do hladín pozri pri statickej metóde
		// zaraďDoHladín.
		// 
		// Bolo treba vyriešiť otázku, či zoraďovať systémy od najnižšej
		// hladiny po najvyššiu, alebo naopak. Priklonili sme sa k prvému
		// variantu, pretože tým vzniká vyššia šanca na zablokovanie prvého
		// procesu druhým a tým vzniká aj vyššia šanca na odhalenie slabých
		// miest simulovaného systému.

	private int produkcia = 0;
	private int limit = 0; // (Poznámka: Nezlučuj to s kapacitou.
		// Predvolené hodnoty sú iné. Je lepšie, keď je to rozdelené.)

	private String id = null; // Toto id je len pomocná informácia používaná
		// pri zápise do a čítaní liniek zo súboru. Nemá žiadne iné použitie
		// a žiadny iný význam. Pôvodný účel bolo umožniť zápis a čítanie
		// spojníc, ale je možné, že sa časom tento účel rozšíril.

	private String popis = null;
	private float veľkosťPísma = 16.0f;
	private String[] riadkyPopisu = null;
	private boolean popisPod = false;
	private ÚčelLinky účel = null;
	private double počiatočnýČas = 0.0;
	private double časovač = 1.0;
	private double rozptyl = 0.0;
	private int kapacita = 10; // (Poznámka: Nezlučuj to s limitom.
		// Predvolené hodnoty sú iné. Je lepšie, keď je to rozdelené.)

	// Zoznam mien zákazníkov:
	private Vector<String> zoznamMien = null;
	private boolean cyklickýZoznam = true;
	private int indexMena = 0;

	private RežimVýberuZákazníkov režimVýberuZákazníkov =
		RežimVýberuZákazníkov.PRVÝ;
	private RežimVýberuLiniek režimVýberuLiniek = RežimVýberuLiniek.POSTUPNÉ;

	// Pomocná premenná k režimu výberu liniek:
	private int ďalšiaSpojnica = -1;

	private RežimKresleniaLinky režimKreslenia = null;
	private String názovTvaru = null;
	private byte transformácieTvaru = 0;
	private Shape tvarTvaru = null;
	private double mieraZaoblenia = 0.50;
	private int početČiarObrysu = 1; // Poznámka: Nulovým (alebo menším)
		// počtom čiar obrysu viem objekt skryť.
	private double rozostupyČiarObrysu = 2.0;
	private boolean zobrazInformácie = true;

	// Evidencia zákazníkov:
	private final Zoznam<Zákazník> zákazníci = new Zoznam<>();


	// Konštruktor musí byť súkromný, aby sa dali recyklovať neaktívne linky
	// (poznámka: funkciu getInstance plní metóda pridaj):
	private Linka(String popis)
	{
		// vlastnýTvar("vlastnýTvar.png"); // TODO: potom vymaž; slúžilo na
			// otestovanie frameworku; ešte sa k tomu bude treba vrátiť, ale
			// na to bude treba naprogramovať vlastnú testovaciu miniaplikáciu.

		// Registrácia liniek:
		linky.add(this);
		Systém.činnosti.add(this);


		// Zostavenie kontextovej ponuky:
		kontextováPonuka = new KontextováPonuka("—");

			položkaZrušÚčel = new KontextováPoložka(
				"<html><i>«žiadny»</i></html>");
			položkaZmeňNaEmitor = new KontextováPoložka("Emitor");
			položkaZmeňNaZásobník = new KontextováPoložka("Zásobník");
			položkaZmeňNaČakáreň = new KontextováPoložka("Čakáreň");
			položkaZmeňNaZastávku = new KontextováPoložka("Zastávka");
			položkaZmeňNaDopravník = new KontextováPoložka("Dopravník");
			položkaZmeňNaMenič = new KontextováPoložka("Menič");
			položkaZmeňNaUvoľňovač = new KontextováPoložka("Uvoľňovač");

		kontextováPonuka.pridajPonuku("Účel", položkaZrušÚčel, null,
			položkaZmeňNaEmitor, položkaZmeňNaZásobník, položkaZmeňNaČakáreň,
			položkaZmeňNaZastávku, položkaZmeňNaDopravník, položkaZmeňNaMenič,
			položkaZmeňNaUvoľňovač);

		položkaRežimVýberuLiniek = kontextováPonuka.pridajPoložku(
			"Režim výberu nasledujúcich liniek…");

		položkaUpravKoeficienty = kontextováPonuka.pridajPoložku(
			"Koeficienty…");

		položkaUpravZoznamMien = kontextováPonuka.pridajPoložku(
			"Zoznam mien zákazníkov…");

		kontextováPonuka.pridajOddeľovač();

		položkaUpravPopis = kontextováPonuka.pridajPoložku("Popis…");

			položkaZrušTvar = new KontextováPoložka(
				"<html><i>«predvolený»</i></html>");
			položkaZmeňNaElipsu = new KontextováPoložka("Elipsa");
			položkaZmeňNaObdĺžnik = new KontextováPoložka("Obdĺžnik");
			položkaZmeňNaInýTvar = new KontextováPoložka("Iný tvar");

		kontextováPonuka.pridajPonuku("Tvar", položkaZrušTvar, null,
			položkaZmeňNaElipsu, položkaZmeňNaObdĺžnik,
			položkaZmeňNaInýTvar);

		položkaUpravVizuály = kontextováPonuka.pridajPoložku(
			"Vizuálne parametre…");
		položkaPrepniInformácie = kontextováPonuka.pridajPoložku(
			"Zobraz informácie");

		kontextováPonuka.pridajOddeľovač();

		položkaVymaž = kontextováPonuka.pridajPoložku("Vymaž");


		// Reset – inicializácia:
		reset(popis);
		nekresliTvary();
		vrstva(1);
	}

	// Svisiace s konštrukciou (resp. inicializáciou) a aktualizáciou rozhrania:

	private void aktualizujKontextovúPonuku()
	{
		položkaZrušÚčel.ikona(null == účel ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);

		položkaZmeňNaEmitor.ikona(ÚčelLinky.EMITOR == účel ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
		položkaZmeňNaZásobník.ikona(ÚčelLinky.ZÁSOBNÍK == účel ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
		položkaZmeňNaČakáreň.ikona(ÚčelLinky.ČAKÁREŇ == účel ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
		položkaZmeňNaZastávku.ikona(ÚčelLinky.ZASTÁVKA == účel ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
		položkaZmeňNaDopravník.ikona(ÚčelLinky.DOPRAVNÍK == účel ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
		položkaZmeňNaMenič.ikona(ÚčelLinky.MENIČ == účel ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
		položkaZmeňNaUvoľňovač.ikona(ÚčelLinky.UVOĽŇOVAČ == účel ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);

		položkaZrušTvar.ikona(null == režimKreslenia ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
		položkaZmeňNaElipsu.ikona(RežimKresleniaLinky.ELIPSA ==
			režimKreslenia ? Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
		položkaZmeňNaObdĺžnik.ikona(RežimKresleniaLinky.OBDĹŽNIK ==
			režimKreslenia ? Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
		položkaZmeňNaInýTvar.ikona(RežimKresleniaLinky.VLASTNÝ_TVAR ==
			režimKreslenia ? Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);

		položkaPrepniInformácie.ikona(zobrazInformácie ?
			Systém.ikonaOznačenia : Systém.ikonaNeoznačenia);
	}

	// Vytvorenie (alebo zrušenie) spojnice musí mať za následok preradenie
	// liniek do hladín, ale je efektívnejšie vykonať toto preradenie
	// nárazovo, najmä po vytvorení série spojníc, napríklad pri duplikovaní
	// liniek, preto nie je preradenie súčasťou tejto metódy (ale treba na to
	// pamätať – bolo by to zdrojom chýb).
	@Override public Spojnica spojnica(GRobot cieľ)
	{
		Spojnica spojnica = super.spojnica(cieľ, šedá);
		if (null == spojnica) return null;

		if (RežimKresleniaLinky.ELIPSA != režimKreslenia)
		{
			aktualizujZaoblenie();
			spojnica.orezanieZačiatku(obdĺžnik());
		}
		else
			spojnica.orezanieZačiatku(elipsa());

		if (cieľ instanceof Linka)
		{
			Linka linka = (Linka)cieľ;
			if (RežimKresleniaLinky.ELIPSA != linka.režimKreslenia)
			{
				linka.aktualizujZaoblenie();
				spojnica.orezanieKonca(linka.obdĺžnik());
			}
			else
				spojnica.orezanieKonca(linka.elipsa());
		}

		spojnica.definujZnačkuKonca(šípka);

		Spojnica opačná = cieľ.dajSpojnicu(this);
		if (null != opačná)
		{
			spojnica.vysunutie(10);
			opačná.vysunutie(10);
		}

		return spojnica;
	}

	@Override public void zrušSpojnicu(GRobot cieľ)
	{
		super.zrušSpojnicu(cieľ);
		Spojnica opačná = cieľ.dajSpojnicu(this);
		if (null != opačná) opačná.vysunutie(0);
	}

	public void aktualizujSpojnice()
	{
		Spojnica[] spojnice = spojniceZ();
		for (Spojnica spojnica : spojnice)
			spojnica(spojnica.cieľ()); // (prekryté)

		spojnice = spojniceDo();
		for (Spojnica spojnica : spojnice)
			spojnica.zdroj().spojnica(this); // (prekryté)

		// zaraďDoHladín() – sa deje „nárazovo“ – po dokončení
		// 	kopírovania/mazania liniek; pri vzniku novej podobnej udalosti
		// 	treba pamätať na to, že na jej konci treba volať aj metódu
		// 	zaraďDoHladín
	}

	private void preevidujTvar(String názov)
	{ preevidujTvar(názov, false, (byte)0); }
	private void preevidujTvar(String názov, boolean prispôsobPomer)
	{ preevidujTvar(názov, prispôsobPomer, (byte)0); }
	private void preevidujTvar(String názov, boolean prispôsobPomer,
		byte transformujTvar)
	{
		if (null != názovTvaru) Tvar.odeviduj(názovTvaru);

		názovTvaru = názov;
		if (null == názovTvaru)
		{
			mierka(1);
			mierkaPomeru(1);
			tvarTvaru = null;
			transformácieTvaru = 0;
		}
		else
		{
			tvarTvaru = Tvar.daj(názovTvaru, transformujTvar);
			transformácieTvaru = transformujTvar;
			if (null == tvarTvaru)
			{
				mierka(1);
				mierkaPomeru(1);
				názovTvaru = null;
				tvarTvaru = null;
				transformácieTvaru = 0;
			}
			else
			{
				mierka(1);
				mierkaPomeru(1);

				double výškaTvaru = tvarTvaru.getBounds2D().getHeight();
				double šírkaTvaru = tvarTvaru.getBounds2D().getWidth();

				mierka(výška() / výškaTvaru);
				mierkaPomeru(šírka() / (šírkaTvaru * mierka()));
				if (prispôsobPomer)
					šírka(šírkaTvaru * mierka());
				Tvar.eviduj(názovTvaru);
			}
		}
	}

	private void reset(String popis)
	{
		farba(svetločervená); // (signalizácia chyby)
		hrúbkaČiary(2);
		pomer(1.2);
		veľkosť(50.0);
		uhol(90.0);
		popis(popis);
		veľkosťPísma(hlavnýRobot().písmo().veľkosť());
		popisPod = false;

		označená = false;
		bliká = 0;
		klik = 0;
		myš = null;

		počiatočnýČas = 0.0;
		čas = Systém.čas; // + počiatočnýČas;

		produkcia = 0;
		limit = 0;

		účel = null;

		časovač = 1.0;
		rozptyl = 0.0;
		kapacita = 10;

		if (null != zoznamMien)
		{
			zoznamMien.clear();
			zoznamMien = null;
		}
		cyklickýZoznam = true;

		režimVýberuZákazníkov = RežimVýberuZákazníkov.PRVÝ;
		režimVýberuLiniek = RežimVýberuLiniek.POSTUPNÉ;

		ďalšiaSpojnica = -1;

		režimKreslenia = null;
		preevidujTvar(null);
		mieraZaoblenia(0.5);
		zobrazInformácie = true;
		početČiarObrysu = 1;
		rozostupyČiarObrysu = 2.0;

		aktivuj(false);
		zrušSpojnice();
		vyraďZákazníkov();
		aktualizujKontextovúPonuku();
	}

	public void kopíruj(Linka iná)
	{
		// Všetko, čo sa dá treba nastavovať cez metódy‼
		skočNa(iná);
		pomer(iná.pomer() < 0 ? 0 : iná.pomer());
		veľkosť(iná.veľkosť() < 0 ? 0 : iná.veľkosť());
		uhol(iná.uhol());
		popis(iná.popis);
		veľkosťPísma(null);
		popisPod = iná.popisPod;

		produkcia = iná.produkcia;
		limit = iná.limit;

		// Metóda účel nastavuje čas, vyraďuje zákazníkov a aktualizuje pouku.
		// To všetko sa robí nižšie…
		účel = iná.účel;

		počiatočnýČas = iná.počiatočnýČas;
		časovač = iná.časovač;
		rozptyl = iná.rozptyl;
		kapacita = iná.kapacita;

		if (null == iná.zoznamMien)
		{
			if (null != zoznamMien)
			{
				zoznamMien.clear();
				zoznamMien = null;
			}
		}
		else
		{
			if (null == zoznamMien)
				zoznamMien = new Vector<String>();
			else
				zoznamMien.clear();
			zoznamMien.addAll(iná.zoznamMien);
		}
		cyklickýZoznam = iná.cyklickýZoznam;

		režimVýberuZákazníkov = iná.režimVýberuZákazníkov;
		režimVýberuLiniek = iná.režimVýberuLiniek;

		ďalšiaSpojnica = iná.ďalšiaSpojnica;

		// Nastavovanie tvaru sa dá robiť cez sériu samostatných metód.
		// Každá robí takmer to isté s drobnými odchýlkami. Všetko je však
		// v zásade pokryté nižšie…
		režimKreslenia = iná.režimKreslenia;
		preevidujTvar(iná.názovTvaru, false, iná.transformácieTvaru);
		if (null == názovTvaru) režimKreslenia = null;
		mieraZaoblenia(iná.mieraZaoblenia);
		zobrazInformácie = iná.zobrazInformácie;
		početČiarObrysu = iná.početČiarObrysu;
		rozostupyČiarObrysu = iná.rozostupyČiarObrysu;

		označená = false;
		bliká = 0;
		klik = 0;
		myš = null;
		čas = iná.čas;

		// Kopíruj zdrojové spojnice inej linky
		{
			Spojnica[] spojnice = iná.spojniceZ();
			for (Spojnica spojnica : spojnice)
				spojnica(spojnica.cieľ()); // (prekryté)
		}

		// Kopíruj cieľové spojnice inej linky
		{
			Spojnica[] spojnice = iná.spojniceDo();
			for (Spojnica spojnica : spojnice)
				spojnica.zdroj().spojnica(this); // (prekryté)
		}

		vyraďZákazníkov();
		aktualizujKontextovúPonuku();
		aktualizujSpojnice(); // (TODO: Nechápem, načo tu robím toto? Proces
			// kopírovania (vyššie) robí v podstate to isté, nie? Asi to tu
			// zostalo navyše, ale na overenie by bolo treba testy.)
	}


	public void blikni()
	{
		bliká = 10;
	}


	// Statická časť (zväčša súvisiaca s evidenciou):


	// Tieto zoznamy pre metódu zaraďDoHladín budú treba neustále, preto sú
	// uchovávané permanentne zvonka tejto metódy:
	private static Vector<Linka> vHladine1 = new Vector<>(),
		vHladine2 = new Vector<>();

	// (Pozri aj komentár pri súkromnom atribúte hladina.)
		// Zaraďovanie do hladín sa vykonáva takto:
		// 
		//  1. najprv sú hladiny všetkých liniek zresetované,
		//  2. potom sú vyhľadané a zaradené do zoznamu prvej hladiny všetky
		//     linky, ktoré nemajú žiadneho predchodcu (ak také jestvujú),
		//  3. do druhej (ďalšej) hladiny sú zaradené všetky linky, ktoré sú
		//     nasledovníkmi prvej (predchádzajúcej) hladiny a neboli ešte
		//     označené;
		//  4. tento proces (od bodu 2) sa opakuje dokedy nie je zoznam liniek
		//     v aktuálnej hladine prázdny.
	public static void zaraďDoHladín()
	{
		// Reset všetkých hladín
		for (Linka linka : linky) linka.hladina = 0;

		// Nájdenie všetkých liniek prvej úrovne:
		vHladine1.clear();
		for (Linka linka : linky)
			if (linka.aktívny() && !linka.súSpojniceDo())
			{
				linka.hladina = 1;
				vHladine1.add(linka);
			}

		int ďalšiaHladina = 2;

		// Začatie cyklu prehľadávania aktuálnej hladiny a pridávania liniek
		// do nasledujúcej hladiny…
		if (!vHladine1.isEmpty()) do
		{
			// Vyčistenie zoznamu liniek nasledujúcej hladiny a jeho naplnenie:
			vHladine2.clear();
			for (Linka linka1 : vHladine1)
			{
				Spojnica[] spojnice = linka1.spojniceZ();
				for (Spojnica spojnica : spojnice)
				{
					GRobot robot = spojnica.cieľ();
					if (robot instanceof Linka)
					{
						Linka linka2 = (Linka)robot;
						if (linka2.aktívny() && 0 == linka2.hladina)
						{
							linka2.hladina = ďalšiaHladina;
							vHladine2.add(linka2);
						}
					}
				}
			}
			++ďalšiaHladina;

			// Výmena zoznamov hladín:
			Vector<Linka> vHladine3 = vHladine1;
			vHladine1 = vHladine2; vHladine2 = vHladine3;

		} while (!vHladine1.isEmpty());
	}


	public static Linka pridaj(String popis)
	{
		int n = linky.size();
		for (int i = 0; i < n; ++i)
		{
			Linka linka = linky.get(i);
			if (linka.neaktívny())
			{
				linka.reset(popis);
				return linka;
			}
		}

		Linka linka = new Linka(popis);
		return linka;
	}

	public static int počet()
	{
		return linky.size();
	}

	public static Linka daj(int i)
	{
		return linky.get(i);
	}

	public static Linka[] daj()
	{
		Linka[] linky = new Linka[Linka.linky.size()];
		return Linka.linky.toArray(linky);
	}

	public static Linka daj(String popis)
	{
		int n = linky.size();
		for (int i = 0; i < n; ++i)
		{
			Linka linka = linky.get(i);
			if (linka.aktívny()) switch (linka.účel)
			{
			case ZÁSOBNÍK:
			case ČAKÁREŇ:
			case ZASTÁVKA:
			case DOPRAVNÍK:
			case MENIČ:
			case UVOĽŇOVAČ:
				if (null == popis)
				{
					if (null == linka.popis) return linka;
				}
				else
				{
					if (popis.equals(linka.popis)) return linka;
				}
			}
		}
		return null;
	}

	public static int početAktívnych()
	{
		int početAktívnych = 0, počet = linky.size();
		for (int i = 0; i < počet; ++i)
			if (linky.get(i).aktívny()) ++početAktívnych;
		return početAktívnych;
	}

	public static Linka[] dajAktívne()
	{
		int počet = linky.size();
		Vector<Linka> aktívne = new Vector<>();

		for (int i = 0; i < počet; ++i)
		{
			Linka linka = linky.get(i);
			if (linka.aktívny()) aktívne.add(linka);
		}

		Linka[] pole = new Linka[aktívne.size()];
		pole = aktívne.toArray(pole);

		aktívne.clear();
		aktívne = null;

		return pole;
	}

	public static boolean žiadnyAktívnyEmitor()
	{
		int počet = linky.size();

		for (int i = 0; i < počet; ++i)
		{
			Linka linka = linky.get(i);
			if (linka.aktívny() && null != linka.účel &&
				ÚčelLinky.EMITOR == linka.účel &&
				(0 == linka.limit || linka.produkcia < linka.limit))
				return false;
		}

		return true;
	}

	public static int početOznačených()
	{
		int početOznačených = 0, počet = linky.size();
		for (int i = 0; i < počet; ++i)
		{
			Linka linka = linky.get(i);
			if (linka.aktívny() && linka.označená)
				++početOznačených;
		}
		return početOznačených;
	}

	public static Linka[] dajOznačené()
	{
		int počet = linky.size();
		Vector<Linka> označené = new Vector<>();

		for (int i = 0; i < počet; ++i)
		{
			Linka linka = linky.get(i);
			if (linka.aktívny() && linka.označená)
				označené.add(linka);
		}

		Linka[] pole = new Linka[označené.size()];
		pole = označené.toArray(pole);

		označené.clear();
		označené = null;

		return pole;
	}

	public static boolean myšVOznačenej()
	{
		for (Linka linka : linky)
			if (linka.aktívny() && linka.myšV()) return true;
		return false;
	}

	public static void blikniOznačené()
	{
		for (Linka linka : linky)
			if (linka.aktívny() && linka.označená) linka.blikni();
	}


	public static void vyčisti()
	{
		for (Linka linka : linky)
			if (linka.aktívny())
			{
				linka.produkcia = 0;
				linka.odídených = linka.vybavených = 0;
				linka.čas = Systém.čas;
				linka.ďalšiaSpojnica = -1;
				linka.indexMena = 0;
				if (linka.jeEmitor())
					linka.čas += linka.interval() + linka.počiatočnýČas;
				else if (null != linka.účel && null != linka.zoznamMien)
					linka.zoznamMien.clear();
			}
	}


	// Úprava vlastností (individuálne aj hromadne):

	private final static ScrollTextPane stp0 = new ScrollTextPane(); static
	{
		stp0.forbidTabulator(true);
		stp0.setPreferredSize(new Dimension(300, 50));
	}

	private final static String[] popisyÚpravyPopisu = new String[]
		{"Upravte popis linky:", "Veľkosť písma:", "Umiestniť popis pod linku"};

	public void upravPopis()
	{
		stp0.setText(null == popis ? "" : popis);
		Object[] údaje = {stp0, new Double(veľkosťPísma), popisPod};

		if (dialóg(popisyÚpravyPopisu, údaje, "Popis linky"))
		{
			popis(stp0.getText());
			if (!Double.isNaN((Double)údaje[1]))
				veľkosťPísma((Double)údaje[1]);
			popisPod = (Boolean)údaje[2];
		}
	}

	private final static String[] popisyÚpravyPopisuOznačených = new String[]
		{"Upravte spoločný popis označených liniek:", "Veľkosť písma:",
		"Umiestňovať popisy pod linky"};

	public static void upravPopisy()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Úprava popisov liniek");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.upravPopis();
					break;
				}
		}
		else
		{
			TreeSet<String> zoznam = new TreeSet<>();
			int n = linky.size(), nn = 0;
			double priemerPísma = 0.0, priemerUmiestnenia = 0.0;
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
				{
					if (null != linka.popis)
						zoznam.add(linka.popis);
					priemerPísma += linka.veľkosťPísma;
					priemerUmiestnenia += linka.popisPod ? 1.0 : 0.0;
					++nn;
				}
			}

			priemerPísma /= nn;
			priemerUmiestnenia /= nn;
			StringBuffer zlúčeniePopisov = null;

			for (String jeden : zoznam)
			{
				if (null == zlúčeniePopisov)
					zlúčeniePopisov = new StringBuffer(jeden);
				else
				{
					zlúčeniePopisov.append(' ');
					zlúčeniePopisov.append(jeden);
				}
			}

			if (null == zlúčeniePopisov) zlúčeniePopisov = new StringBuffer();

			stp0.setText(zlúčeniePopisov.toString());
			Object[] údaje = new Object[] {stp0, priemerPísma,
				priemerUmiestnenia > 0.5 ? true : false};

			if (dialóg(popisyÚpravyPopisuOznačených, údaje, "Popisy liniek"))
			{
				String popis = stp0.getText();
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená)
					{
						linka.popis(popis);
						if (!Double.isNaN((Double)údaje[1]))
							linka.veľkosťPísma((Double)údaje[1]);
						linka.popisPod = (Boolean)údaje[2];
					}
				}
			}
		}
	}


	public RežimVýberuZákazníkov režimVýberuZákazníkov()
	{
		return null == režimVýberuZákazníkov ?
			RežimVýberuZákazníkov.PRVÝ : režimVýberuZákazníkov;
	}

	public void režimVýberuZákazníkov(RežimVýberuZákazníkov režim)
	{
		this.režimVýberuZákazníkov = null == režim ?
			RežimVýberuZákazníkov.PRVÝ : režim;
		žiadajPrekreslenie();
	}


	public RežimVýberuLiniek režimVýberuLiniek()
	{
		return null == režimVýberuLiniek ?
			RežimVýberuLiniek.POSTUPNÉ : režimVýberuLiniek;
	}

	public void režimVýberuLiniek(RežimVýberuLiniek režim)
	{
		this.režimVýberuLiniek = null == režim ?
			RežimVýberuLiniek.POSTUPNÉ : režim;
		ďalšiaSpojnica = -1;
		žiadajPrekreslenie();
	}


	private final static String[] popisyÚpravyRežimuVýberuLiniek =
		new String[] {"Upravte režim výberu nasledujúcich liniek:"};

	public void zmeňRežimVýberuLiniek()
	{
		Object[] údaje = {null == režimVýberuLiniek ?
			RežimVýberuLiniek.POSTUPNÉ : režimVýberuLiniek};

		if (dialóg(popisyÚpravyRežimuVýberuLiniek, údaje,
			"Režim výberu zákazníkov linky"))
			režimVýberuLiniek((RežimVýberuLiniek)údaje[0]);
	}

	private final static String[]
		popisyÚpravyRežimuVýberuLiniekOznačených = new String[]
		{"Zmeňte režim výberu nasledujúcich liniek označených liniek:"};

	public static void zmeňRežimyVýberuLiniek()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena režimu výberu zákazníkov označených liniek");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňRežimVýberuLiniek();
					break;
				}
		}
		else
		{
			int n = linky.size();
			RežimVýberuLiniek režim = null;
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
				{
					if (null == režim)
						režim = linka.režimVýberuLiniek;
					else if (režim != linka.režimVýberuLiniek)
					{
						režim = null;
						break;
					}
				}
			}

			Object[] údaje = new Object[] {null == režim ?
				RežimVýberuLiniek.POSTUPNÉ : režim};

			if (dialóg(popisyÚpravyRežimuVýberuLiniekOznačených, údaje,
				"Režim výberu nasledujúcich liniek označených liniek"))
			{
				režim = (RežimVýberuLiniek)údaje[0];
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená)
						linka.režimVýberuLiniek(režim);
				}
			}
		}
	}


	public void zrušÚčel()
	{
		účel(null);
	}

	public static void zrušÚčely()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zrušenie účelov liniek");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zrušÚčel();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zrušÚčel();
			}
		}
	}


	public void zmeňNaEmitor()
	{
		účel(ÚčelLinky.EMITOR);
	}

	public static void zmeňNaEmitory()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena liniek na emitory");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaEmitor();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zmeňNaEmitor();
			}
		}
	}


	public void zmeňNaZásobník()
	{
		účel(ÚčelLinky.ZÁSOBNÍK);
	}

	public static void zmeňNaZásobníky()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena liniek na zásobníky");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaZásobník();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zmeňNaZásobník();
			}
		}
	}


	public void zmeňNaČakáreň()
	{
		účel(ÚčelLinky.ČAKÁREŇ);
	}

	public static void zmeňNaČakárne()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena liniek na čakárne");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaČakáreň();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zmeňNaČakáreň();
			}
		}
	}


	public void zmeňNaZastávku()
	{
		účel(ÚčelLinky.ZASTÁVKA);
	}

	public static void zmeňNaZastávky()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena liniek na čakárne");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaZastávku();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zmeňNaZastávku();
			}
		}
	}


	public void zmeňNaDopravník()
	{
		účel(ÚčelLinky.DOPRAVNÍK);
	}

	public static void zmeňNaDopravníky()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena liniek na dopravníky");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaDopravník();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zmeňNaDopravník();
			}
		}
	}


	public void zmeňNaMenič()
	{
		účel(ÚčelLinky.MENIČ);
	}

	public static void zmeňNaMeniče()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena liniek na meniče");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaMenič();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zmeňNaMenič();
			}
		}
	}


	public void zmeňNaUvoľňovač()
	{
		účel(ÚčelLinky.UVOĽŇOVAČ);
	}

	public static void zmeňNaUvoľňovače()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena liniek na uvoľňovače");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaUvoľňovač();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zmeňNaUvoľňovač();
			}
		}
	}


	public void zrušTvar()
	{
		zaoblenie((min(výška(), šírka()) / 2) * mieraZaoblenia);
		režimKreslenia = null;
		preevidujTvar(null);
		aktualizujKontextovúPonuku();
		aktualizujSpojnice();
	}

	public static void zrušTvary()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zrušenie tvarov liniek");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zrušTvar();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zrušTvar();
			}
		}
	}


	public void zmeňNaElipsu()
	{
		zaoblenie(0);
		režimKreslenia = RežimKresleniaLinky.ELIPSA;
		preevidujTvar(null);
		aktualizujKontextovúPonuku();
		aktualizujSpojnice();
	}

	public static void zmeňNaElipsy()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena tvaru liniek na elipsy");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaElipsu();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zmeňNaElipsu();
			}
		}
	}


	public void zmeňNaObdĺžnik()
	{
		zaoblenie(0);
		režimKreslenia = RežimKresleniaLinky.OBDĹŽNIK;
		preevidujTvar(null);
		aktualizujKontextovúPonuku();
		aktualizujSpojnice();
	}

	public static void zmeňNaObdĺžniky()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena tvaru liniek na obdĺžniky");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaObdĺžnik();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zmeňNaObdĺžnik();
			}
		}
	}


	private static class VoľbaTvaru
	{
		public final String názov;
		public final byte transformácie;
		public final boolean prispôsobPomer;
		public VoľbaTvaru(String názov, byte transformácie,
			boolean prispôsobPomer)
		{
			this.názov = názov;
			this.transformácie = transformácie;
			this.prispôsobPomer = prispôsobPomer;
		}
	}

	private final static String[] popisyVoľbyTvaruSingulár = new String[]
		{"Zvoľte tvar linky:", "Zrkadliť horizontálne (sprava doľava)",
		"Zrkadliť vertikálne (zhora nadol)", "Pootočiť tvar",
		"Prispôsobiť pomer strán linky k rozmerom zvoleného tvaru"};

	private final static String[] popisyVoľbyTvaruPlurál = new String[]
		{"Zvoľte nový tvar pre označené linky:",
		"Zrkadliť horizontálne (sprava doľava)",
		"Zrkadliť vertikálne (zhora nadol)", "Pootočiť tvary",
		"Prispôsobiť pomery strán liniek k rozmerom zvoleného tvaru"};

	private static VoľbaTvaru vyberTvar(String názov, byte transformácie)
	{ return vyberTvar(názov, transformácie, popisyVoľbyTvaruSingulár,
		"Výber vlastného tvaru linky"); }
	private static VoľbaTvaru vyberTvar(String názov, byte transformácie,
		String[] popisyVoľbyTvaru, String titulok)
	{
		Zoznam<Obrázok> zoznam = new Zoznam<>(Tvar.obrázokPodľaNázvu(názov));
		Tvar.naplňZoznamObrázkov(zoznam);

		Boolean zrkadliHorizontálne = 0 != (transformácie & 1);
		Boolean zrkadliVertikálne   = 0 != (transformácie & 2);

		RotáciaTvaru kvadrant = RotáciaTvaru.Q0;
		switch (transformácie & 12)
		{
		case 4: kvadrant = RotáciaTvaru.Q1; break;
		case 8: kvadrant = RotáciaTvaru.Q2; break;
		case 12: kvadrant = RotáciaTvaru.Q3; break;
		}

		Object[] údaje = {zoznam, zrkadliHorizontálne, zrkadliVertikálne,
			kvadrant, Boolean.TRUE};

		String zvolený = null;
		boolean pomer = true;

		if (dialóg(popisyVoľbyTvaru, údaje, titulok))
		{
			transformácie = (byte)(
				((null != údaje[1] && (Boolean)údaje[1]) ? 1 : 0) |
				((null != údaje[2] && (Boolean)údaje[2]) ? 2 : 0));

			switch ((RotáciaTvaru)údaje[3])
			{
			case Q1: transformácie |= 4; break;
			case Q2: transformácie |= 8; break;
			case Q3: transformácie |= 12; break;
			}

			zvolený = Tvar.názovPodľaObrázka(zoznam.daj(0));
			pomer = null == údaje[4] || (Boolean)údaje[4];
		}

		zoznam.vymaž(); zoznam = null;
		return new VoľbaTvaru(zvolený, transformácie, pomer);
	}

	public void zmeňNaInýTvar()
	{
		VoľbaTvaru voľbaTvaru = vyberTvar(názovTvaru, transformácieTvaru);
		if (null != voľbaTvaru.názov)
		{
			zaoblenie((min(výška(), šírka()) / 2) * mieraZaoblenia);
			režimKreslenia = RežimKresleniaLinky.VLASTNÝ_TVAR;

			preevidujTvar(voľbaTvaru.názov, voľbaTvaru.prispôsobPomer,
				voľbaTvaru.transformácie);
			if (null == názovTvaru) režimKreslenia = null;

			aktualizujKontextovúPonuku();
			aktualizujSpojnice();
		}
	}

	public void zmeňNaInýTvar(String zvolený, boolean prispôsobPomer,
		byte transformujTvar)
	{
		zaoblenie((min(výška(), šírka()) / 2) * mieraZaoblenia);
		režimKreslenia = RežimKresleniaLinky.VLASTNÝ_TVAR;

		preevidujTvar(zvolený, prispôsobPomer, transformujTvar);
		if (null == názovTvaru) režimKreslenia = null;

		aktualizujKontextovúPonuku();
		aktualizujSpojnice();
	}

	public static void zmeňNaInéTvary()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena tvaru liniek na iné tvary");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zmeňNaInýTvar();
					break;
				}
		}
		else
		{
			int n = linky.size();
			String názovTvaru = null;
			{
				// Ak sú názvy všetkých označených liniek rovnaké (pričom
				// prázdne názvy sú ignorované), tak sa tento spoločný názov
				// použije ako predvolená hodnota výberu tvaru.
				int i = 0;
				for (; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená &&
						null != linka.názovTvaru)
					{
						názovTvaru = linka.názovTvaru;
						break;
					}
				}

				for (; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená &&
						null != linka.názovTvaru &&
						!názovTvaru.equals(linka.názovTvaru))
					{
						názovTvaru = null;
						break;
					}
				}
			}
			byte transformácieTvaru = 0;
			{
				// TODO: treba otestovať transformácie.
				int i = 0;
				for (; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená &&
						null != linka.názovTvaru)
					{
						transformácieTvaru = linka.transformácieTvaru;
						break;
					}
				}

				for (; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená &&
						null != linka.názovTvaru &&
						transformácieTvaru != linka.transformácieTvaru)
					{
						transformácieTvaru = 0;
						break;
					}
				}
			}

			VoľbaTvaru voľbaTvaru = vyberTvar(
				názovTvaru, transformácieTvaru, popisyVoľbyTvaruPlurál,
				"Výber vlastného tvaru označených liniek");

			if (null != voľbaTvaru.názov)
			{
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená)
						linka.zmeňNaInýTvar(voľbaTvaru.názov,
							voľbaTvaru.prispôsobPomer,
							voľbaTvaru.transformácie);
				}
			}
		}
	}


	public static void prepniInformácieOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Prepnutie informácií pri označených linkách");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.prepniZobrazenieInformácií();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.prepniZobrazenieInformácií();
			}
		}
	}

	public static void skryInformácieOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Prepnutie informácií pri označených linkách");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zobrazInformácie(false);
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zobrazInformácie(false);
			}
		}
	}

	public static void zobrazInformácieOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Prepnutie informácií pri označených linkách");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.zobrazInformácie(true);
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
					linka.zobrazInformácie(true);
			}
		}
	}


	private final static String[][] popisyKoeficientov = new String[][]
		{
			// Časovač je intervalom produkcie zákazníkov pre emitor,
			// trpezlivosťou zákazníkov pre čakáreň, časom dopravy pre
			// dopravník, časom spracovania pre iné linky, pričom je
			// irelevantný pre zásobníky.

			// Linky s nedefinovaným účelom:
			{"Časovač:", "Rozptyl:", "Počiatočný čas emitora:",
				"Limit emitora:", "Kapacita:", "Režim výberu zákazníkov:"},

			// Len emitory:
			{"Časovač:", "Rozptyl:", "Počiatočný čas:", "Limit:"},

			// Len zásobníky:
			{"Kapacita:", "Režim výberu zákazníkov:"},

			// Len čakárne:
			{"Časovač:", "Rozptyl:", "Kapacita:", "Režim výberu zákazníkov:"},

			// Len zastávky:
			{"Časovač:", "Rozptyl:", "Kapacita:"},

			// Všetky ostatné linky s definovaným účelom:
			{"Časovač:", "Rozptyl:"}
		};

	public void upravKoeficienty()
	{
		if (null == účel)
		{
			Object[] údaje = {časovač, rozptyl, počiatočnýČas,
				new Double(limit), new Double(kapacita),
				null == režimVýberuZákazníkov ? RežimVýberuZákazníkov.PRVÝ :
				režimVýberuZákazníkov};

			if (dialóg(popisyKoeficientov[0], údaje, "Koeficienty linky"))
			{
				if (!Double.isNaN((Double)údaje[0]))
					časovač = (Double)údaje[0];
				if (!Double.isNaN((Double)údaje[1]))
					rozptyl = (Double)údaje[1];
				if (!Double.isNaN((Double)údaje[2]))
					počiatočnýČas = (Double)údaje[2];
				if (!Double.isNaN((Double)údaje[3]))
					limit = ((Double)údaje[3]).intValue();
				if (!Double.isNaN((Double)údaje[4]))
					kapacita = ((Double)údaje[4]).intValue();
				režimVýberuZákazníkov((RežimVýberuZákazníkov)údaje[5]);
			}
		}
		else if (ÚčelLinky.EMITOR == účel)
		{
			Object[] údaje = {časovač, rozptyl, počiatočnýČas,
				new Double(limit)};

			if (dialóg(popisyKoeficientov[1], údaje, "Koeficienty linky"))
			{
				if (!Double.isNaN((Double)údaje[0]))
					časovač = (Double)údaje[0];
				if (!Double.isNaN((Double)údaje[1]))
					rozptyl = (Double)údaje[1];
				if (!Double.isNaN((Double)údaje[2]))
					počiatočnýČas = (Double)údaje[2];
				if (!Double.isNaN((Double)údaje[3]))
					limit = ((Double)údaje[3]).intValue();
			}
		}
		else if (ÚčelLinky.ZÁSOBNÍK == účel)
		{
			Object[] údaje = {new Double(kapacita), null ==
				režimVýberuZákazníkov ? RežimVýberuZákazníkov.PRVÝ :
				režimVýberuZákazníkov};

			if (dialóg(popisyKoeficientov[2], údaje, "Koeficienty linky"))
			{
				if (!Double.isNaN((Double)údaje[0]))
					kapacita = ((Double)údaje[0]).intValue();
				režimVýberuZákazníkov((RežimVýberuZákazníkov)údaje[1]);
			}
		}
		else if (ÚčelLinky.ČAKÁREŇ == účel)
		{
			Object[] údaje = {časovač, rozptyl, new Double(kapacita),
				null == režimVýberuZákazníkov ? RežimVýberuZákazníkov.PRVÝ :
				režimVýberuZákazníkov};

			if (dialóg(popisyKoeficientov[3], údaje, "Koeficienty linky"))
			{
				if (!Double.isNaN((Double)údaje[0]))
					časovač = (Double)údaje[0];
				if (!Double.isNaN((Double)údaje[1]))
					rozptyl = (Double)údaje[1];
				if (!Double.isNaN((Double)údaje[2]))
					kapacita = ((Double)údaje[2]).intValue();
				režimVýberuZákazníkov((RežimVýberuZákazníkov)údaje[3]);
			}
		}
		else if (ÚčelLinky.ZASTÁVKA == účel)
		{
			Object[] údaje = {časovač, rozptyl, new Double(kapacita)};

			if (dialóg(popisyKoeficientov[4], údaje, "Koeficienty linky"))
			{
				if (!Double.isNaN((Double)údaje[0]))
					časovač = (Double)údaje[0];
				if (!Double.isNaN((Double)údaje[1]))
					rozptyl = (Double)údaje[1];
				if (!Double.isNaN((Double)údaje[2]))
					kapacita = ((Double)údaje[2]).intValue();
			}
		}
		else
		{
			Object[] údaje = {časovač, rozptyl};

			if (dialóg(popisyKoeficientov[5], údaje, "Koeficienty linky"))
			{
				if (!Double.isNaN((Double)údaje[0]))
					časovač = (Double)údaje[0];
				if (!Double.isNaN((Double)údaje[1]))
					rozptyl = (Double)údaje[1];
			}
		}
	}

	private final static String úvodnáPoznámkaKoeficientovOznačených =
		"<html><i>Poznámka: Ak chcete niektorý spoločný parameter " +
		"ignorovať<br />(nenastaviť), údaj vymažte a nechajte políčko " +
		"prázdne.</i><br /> <br />"; // Musí byť ukončená </html>

	private final static String[] popisyKoeficientovOznačených = new String[]
		{
			"Časovače označených liniek (okrem zásobníkov):",
			"Rozptyly označených liniek (okrem zásobníkov):",
			"Počiatočné časy označených emitorov:",
			"Limity označených emitorov:",
			"Kapacity označených liniek (len pre zásobníky/čakárne):",
			"Režimy výberu zákazníkov označených liniek (len pre " +
				"zásobníky/čakárne):"
		};

	public static void upravKoeficientyOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Úprava koeficientov liniek");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.upravKoeficienty();
					break;
				}
		}
		else
		{
			// Bity typu koeficientov:
			//  1 – časovač + rozptyl
			//  2 – počiatočný čas + limit
			//  4 – kapacita
			//  8 – režim výberu zákazníkov

			int n = linky.size(), typ = 0;
			double[] priemery = {0.0, 0.0, 0.0, 0.0, 0.0};
			RežimVýberuZákazníkov režim = null;

			{
				int n1 = 0, n2 = 0, n3 = 0;

				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená)
					{
						int pridaj = 0;

						if (null == linka.účel) pridaj = 15;
						else if (ÚčelLinky.EMITOR == linka.účel) pridaj = 3;
						else if (ÚčelLinky.ZÁSOBNÍK == linka.účel) pridaj = 12;
						else if (ÚčelLinky.ČAKÁREŇ == linka.účel) pridaj = 13;
						else if (ÚčelLinky.ZASTÁVKA == linka.účel) pridaj = 5;
						else pridaj = 1;

						if (pridaj <= 0) continue;

						if (0 != (pridaj & 1))
						{
							priemery[0] += linka.časovač;
							priemery[1] += linka.rozptyl;
							++n1;
						}

						if (0 != (pridaj & 2))
						{
							priemery[2] += linka.počiatočnýČas;
							priemery[3] += linka.limit;
							++n2;
						}

						if (0 != (pridaj & 4))
						{
							priemery[4] += linka.kapacita;
							++n3;
						}

						if (0 != (pridaj & 8))
						{
							if (null == režim)
								režim = linka.režimVýberuZákazníkov;
							else if (režim != linka.režimVýberuZákazníkov)
								režim = RežimVýberuZákazníkov.PRVÝ;
						}

						typ |= pridaj;
					}
				}

				if (0 != n1)
				{
					priemery[0] /= n1; // časovač
					priemery[1] /= n1; // rozptyl
				}

				if (0 != n2)
				{
					priemery[2] /= n2; // počiatočný čas
					priemery[3] /= n2; // limit
					priemery[3] = (int)priemery[3]; // (zaokrúhlenie limitu)
				}

				if (0 != n3)
				{
					priemery[4] /= n3; // kapacita
					priemery[4] = (int)priemery[4]; // (zaokrúhlenie kapacity)
				}

				// (Režim sa dodatočne nespracúva.)
			}

			if (typ <= 0) return;

			int iČasovača = -1, iRozptylu = -1,
				iPočiatočnéhoČasu = -1, iLimitu = -1,
				iKapacity = -1, iRežimu = -1;

			int početÚdajov = 0;
			if (0 != (typ & 1)) početÚdajov += 2;
			if (0 != (typ & 2)) početÚdajov += 2;
			if (0 != (typ & 4)) početÚdajov += 1;
			if (0 != (typ & 8)) početÚdajov += 1;

			String[] popisy = new String[početÚdajov];
			Object[] údaje = new Object[početÚdajov];

			{
				int poradie = 0;

				if (0 != (typ & 1))
				{
					// časovač + rozptyl
					údaje[poradie] = priemery[0];
					údaje[poradie + 1] = priemery[1];

					if (0 == poradie)
						popisy[poradie] = úvodnáPoznámkaKoeficientovOznačených +
							popisyKoeficientovOznačených[0] + "</html>";
					else
						popisy[poradie] = popisyKoeficientovOznačených[0];
					popisy[poradie + 1] = popisyKoeficientovOznačených[1];

					iČasovača = poradie;
					iRozptylu = poradie + 1;

					poradie += 2;
				}

				if (0 != (typ & 2))
				{
					// počiatočný čas + limit
					údaje[poradie] = priemery[2];
					údaje[poradie + 1] = priemery[3];

					if (0 == poradie)
						popisy[poradie] = úvodnáPoznámkaKoeficientovOznačených +
							popisyKoeficientovOznačených[2] + "</html>";
					else
						popisy[poradie] = popisyKoeficientovOznačených[2];
					popisy[poradie + 1] = popisyKoeficientovOznačených[3];

					iPočiatočnéhoČasu = poradie;
					iLimitu = poradie + 1;

					poradie += 2;
				}

				if (0 != (typ & 4))
				{
					// kapacita
					údaje[poradie] = priemery[4];

					if (0 == poradie)
						popisy[poradie] = úvodnáPoznámkaKoeficientovOznačených +
							popisyKoeficientovOznačených[4] + "</html>";
					else
						popisy[poradie] = popisyKoeficientovOznačených[4];

					iKapacity = poradie;
					poradie += 1;
				}

				if (0 != (typ & 8))
				{
					// režim výberu zákazníkov
					údaje[poradie] = null == režim ?
						RežimVýberuZákazníkov.PRVÝ : režim;

					if (0 == poradie)
						popisy[poradie] = úvodnáPoznámkaKoeficientovOznačených +
							popisyKoeficientovOznačených[5] + "</html>";
					else
						popisy[poradie] = popisyKoeficientovOznačených[5];

					iRežimu = poradie;
					poradie += 1;
				}
			}


			if (dialóg(popisy, údaje, "Koeficienty liniek"))
			{
				// časovač + rozptyl
				if (0 != (typ & 1))
				{
					for (int i = 0; i < n; ++i)
					{
						Linka linka = linky.get(i);
						if (linka.aktívny() && linka.označená)
						{
							if (!Double.isNaN((Double)údaje[iČasovača]))
								linka.časovač = (Double)údaje[iČasovača];
							if (!Double.isNaN((Double)údaje[iRozptylu]))
								linka.rozptyl = (Double)údaje[iRozptylu];
						}
					}
				}

				// počiatočný čas + limit
				if (0 != (typ & 2))
				{
					for (int i = 0; i < n; ++i)
					{
						Linka linka = linky.get(i);
						if (linka.aktívny() && linka.označená)
						{
							if (!Double.isNaN((Double)údaje[iPočiatočnéhoČasu]))
								linka.počiatočnýČas = (Double)údaje[
									iPočiatočnéhoČasu];
							if (!Double.isNaN((Double)údaje[iLimitu]))
								linka.limit = ((Double)údaje[
									iLimitu]).intValue();
						}
					}
				}

				// kapacita
				if (0 != (typ & 4))
				{
					for (int i = 0; i < n; ++i)
					{
						Linka linka = linky.get(i);
						if (linka.aktívny() && linka.označená)
						{
							if (!Double.isNaN((Double)údaje[iKapacity]))
								linka.kapacita = ((Double)údaje[
									iKapacity]).intValue();
						}
					}
				}

				// režim výberu zákazníkov
				if (0 != (typ & 8))
				{
					for (int i = 0; i < n; ++i)
					{
						Linka linka = linky.get(i);
						if (linka.aktívny() && linka.označená)
						{
							linka.režimVýberuZákazníkov(
								(RežimVýberuZákazníkov)údaje[iRežimu]);
						}
					}
				}
			}
		}
	}


	private final static ScrollTextPane stp1 = new ScrollTextPane(); static
	{
		stp1.forbidTabulator(true);
		stp1.setPreferredSize(new Dimension(300, 150));
	}

	private final static String[] popisyÚpravyZoznamuMien = new String[]
		{"<html><i>Poznámka: Zoznam mien má význam vkladať len do " +
			"emitorov.<br />Pre ostatné linky sú zoznamy vždy pri reštarte " +
			"simulácie vymazané.<br />Naopak, uvoľňovače počas simulácie " +
			"zbierajú mená korektne<br />uvoľnených zákazníkov, takže pri " +
			"nich má význam zoznam<br />skontrolovať po skončení " +
			"simulácie.</i>" +
		"<br /> <br />Zoznam mien zákazníkov linky:</html>",
		"Cyklicky opakovať mená zákazníkov"};

	public void upravZoznamMien()
	{
		boolean editable = null == účel || ÚčelLinky.EMITOR == účel;
		stp1.setEditable(editable);

		if (editable || null == popis || popis.isEmpty())
			stp1.setText(zoznamMien());
		else
			stp1.setText(popis + "\n" + zoznamMien());

		Object[] údaje = editable ? new Object[] {stp1, cyklickýZoznam} :
			new Object[] {stp1};

		if (dialóg(popisyÚpravyZoznamuMien, údaje,
			"Zoznam mien zákazníkov linky") && editable)
		{
			zoznamMien(stp1.getText());
			cyklickýZoznam = (Boolean)údaje[1];
		}
	}

	private final static String[] popisyÚpravyZoznamuMienOznačených =
		new String[] {
			"<html><i>Poznámka: Zoznam mien má význam vkladať len do " +
			"emitorov.<br />Pre ostatné linky sú zoznamy vždy pri reštarte " +
			"simulácie vymazané.<br />Naopak, uvoľňovače počas simulácie " +
			"zbierajú mená korektne<br />uvoľnených zákazníkov, takže pri " +
			"nich má význam zoznam<br />skontrolovať po skončení " +
			"simulácie.</i>" +
			"<br /> <br />Spoločný zoznamy mien zákazníkov označených " +
			"liniek:</html>",
			"Cyklicky opakovať mená zákazníkov liniek"};

	public static void upravZoznamyMienOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Úprava zoznamov mien zákazníkov liniek");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.upravZoznamMien();
					break;
				}
		}
		else
		{
			StringBuffer zlúčenieZoznamov = null;
			double priemerCyklickosti = 0.0;
			int n = linky.size();

			boolean editable = false;
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená &&
					(null == linka.účel || ÚčelLinky.EMITOR == linka.účel))
				{
					editable = true;
					break;
				}
			}

			{
				Vector<String> zoznam = new Vector<>();
				int nn = 0;

				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená)
					{
						if (null != linka.zoznamMien)
						{
							if (!editable)
							{
								if (!zoznam.isEmpty())
									zoznam.add("");
								if (null != linka.popis &&
									!linka.popis.isEmpty())
									zoznam.add(linka.popis);
							}
							zoznam.addAll(linka.zoznamMien);
						}
						priemerCyklickosti += linka.cyklickýZoznam ? 1.0 : 0.0;
						++nn;
					}
				}

				priemerCyklickosti /= nn;

				for (String meno : zoznam)
				{
					if (null == zlúčenieZoznamov)
						zlúčenieZoznamov = new StringBuffer(meno);
					else
					{
						zlúčenieZoznamov.append('\n');
						zlúčenieZoznamov.append(meno);
					}
				}
			}

			if (null == zlúčenieZoznamov)
				zlúčenieZoznamov = new StringBuffer();

			stp1.setEditable(editable);
			stp1.setText(zlúčenieZoznamov.toString());
			Object[] údaje = editable ?
				new Object[] {stp1, priemerCyklickosti >= 0.5 ? true : false} :
				new Object[] {stp1};

			if (dialóg(popisyÚpravyZoznamuMienOznačených, údaje,
				"Zoznamy mien zákazníkov liniek") && editable)
			{
				String zoznam = stp1.getText();
				boolean cyklický = (Boolean)údaje[1];

				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená)
					{
						if (null == linka.účel ||
							ÚčelLinky.EMITOR == linka.účel)
						{
							linka.zoznamMien(zoznam);
							linka.cyklickýZoznam = cyklický;
						}
					}
				}
			}
		}
	}


	private final static String[] popisyVizuálov = new String[]
		{"Pomer šírky k výške:", "Veľkosť (výška):",
		"Miera zaoblenia rohov predvoleného tvaru:",
		"Počet čiar obrysu (0 – n):", "Rozostupy medzi čiarami obrysu:",
		"Orientácia (90° – predvolená):"};

	public void upravVizuály()
	{
		Object[] údaje = {pomer(), veľkosť(), mieraZaoblenia,
			new Double(početČiarObrysu), rozostupyČiarObrysu, uhol()};

		if (dialóg(popisyVizuálov, údaje, "Vizuálne parametre linky"))
		{
			if (!Double.isNaN((Double)údaje[0]))
				pomer((Double)údaje[0] < 0 ? 0 : (Double)údaje[0]);
			if (!Double.isNaN((Double)údaje[1]))
				veľkosť((Double)údaje[1] < 0 ? 0 : (Double)údaje[1]);
			if (!Double.isNaN((Double)údaje[2]))
				mieraZaoblenia((Double)údaje[2]);
			if (!Double.isNaN((Double)údaje[3]))
				početČiarObrysu = ((Double)údaje[3]).intValue();
			if (!Double.isNaN((Double)údaje[4]))
				rozostupyČiarObrysu = (Double)údaje[4];
			if (!Double.isNaN((Double)údaje[5]))
				uhol((Double)údaje[5]);
			aktualizujSpojnice();
		}
	}

	private final static String[] popisyVizuálovOznačených = new String[]
		{"<html><i>Poznámka: Ak chcete niektorý spoločný parameter " +
			"ignorovať<br />(nenastaviť), údaj vymažte a nechajte políčko " +
			"prázdne.</i><br /> <br />Pomery šírok k výškam:</html>",
		"Veľkosti (výšky):", "Miery zaoblenia rohov predvoleného tvaru:",
		"Počty čiar obrysov (0 – n):", "Rozostupy medzi čiarami obrysov:",
		"Orientácie (90° – predvolená orientácia):"};

	public static void upravVizuályOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Úprava vizuálnych vlastností liniek");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.upravVizuály();
					break;
				}
		}
		else
		{
			int n = linky.size(), nn = 0;
			double[] priemery = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená)
				{
					priemery[0] += linka.pomer();
					priemery[1] += linka.veľkosť();
					priemery[2] += linka.mieraZaoblenia();
					priemery[3] += linka.početČiarObrysu;
					priemery[4] += linka.rozostupyČiarObrysu;
					priemery[5] += linka.uhol();
					++nn;
				}
			}

			priemery[0] /= nn; // pomer
			priemery[1] /= nn; // veľkosť
			priemery[2] /= nn; // miera zaoblenia
			priemery[3] /= nn; // počet čiar obrysu
			priemery[4] /= nn; // rozostupy čiar obrysu
			priemery[5] /= nn; // uhol

			Object[] údaje = new Object[] {priemery[0], priemery[1],
				priemery[2], floor(priemery[3]), priemery[4], priemery[5]};

			if (dialóg(popisyVizuálovOznačených, údaje,
				"Vizuálne parametre označených liniek"))
			{
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená)
					{
						if (!Double.isNaN((Double)údaje[0]))
							linka.pomer((Double)údaje[0] < 0 ? 0 :
								(Double)údaje[0]);
						if (!Double.isNaN((Double)údaje[1]))
							linka.veľkosť((Double)údaje[1] < 0 ? 0 :
								(Double)údaje[1]);
						if (!Double.isNaN((Double)údaje[2]))
							linka.mieraZaoblenia((Double)údaje[2]);
						if (!Double.isNaN((Double)údaje[3]))
							linka.početČiarObrysu =
								((Double)údaje[3]).intValue();
						if (!Double.isNaN((Double)údaje[4]))
							linka.rozostupyČiarObrysu = (Double)údaje[4];
						if (!Double.isNaN((Double)údaje[5]))
							linka.uhol((Double)údaje[5]);
						linka.aktualizujSpojnice();
					}
				}
			}
		}
	}


	public void vymaž()
	{
		if (ÁNO == otázka("Prajete si túto linku vymazať?",
			"Potvrdenie vymazania linky"))
		{
			deaktivuj();
			Systém.repauzuj();
			Systém.naplňZoznamSpojníc();
			zaraďDoHladín();
		}
	}

	public static void vymažVšetko() // Vymaže všetky linky
	{
		for (Linka linka : linky)
			if (linka.aktívny()) linka.deaktivuj();
	}

	public static void vymažOznačené()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Vymazanie označených liniek");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená)
				{
					linka.vymaž();
					break;
				}
		}
		else
		{
			if (ÁNO == otázka("Prajete si vymazať všetky označené linky?",
				"Potvrdenie vymazania liniek"))
			{
				int n = linky.size();
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená)
						linka.deaktivuj();
				}
				Systém.repauzuj();
				Systém.naplňZoznamSpojníc();
				zaraďDoHladín();
			}
		}
	}


	public String popis()
	{
		return popis;
	}

	public String skráťPopis(int dĺžka)
	{
		if (null == popis) return null;
		if (popis.length() > dĺžka + 1)
			return popis.substring(0, dĺžka) + "…";
		return popis;
	}

	public void popis(String popis)
	{
		if (null == popis || popis.isEmpty())
		{
			this.popis = null;
			riadkyPopisu = null;
			kontextováPonuka.popis(
				"<html><i>«nepomenovaná linka»</i></html>");
		}
		else
		{
			this.popis = popis;

			if (-1 != popis.indexOf("\n"))
			{
				riadkyPopisu = popis.split("\\r?\\n|\\n\\r?");
				popis = this.popis.replaceAll("\\r?\\n|\\n\\r?", " ");
			}
			else
				riadkyPopisu = null;

			if (popis.length() > 31) // skráťPopis(30)
				kontextováPonuka.popis(popis.substring(0, 30) + "…");
			else
				kontextováPonuka.popis(popis);

		}
		žiadajPrekreslenie();
	}


	public float veľkosťPísma() { return veľkosťPísma; }

	public void veľkosťPísma(float veľkosťPísma)
	{
		Písmo písmo = hlavnýRobot().písmo();
		if (písmo.veľkosť() == veľkosťPísma)
			písmo(písmo);
		else
			písmo(písmo.deriveFont(veľkosťPísma));
		this.veľkosťPísma = veľkosťPísma;
	}

	public void veľkosťPísma(Double veľkosťPísma)
	{
		if (null == veľkosťPísma)
			veľkosťPísma(hlavnýRobot().písmo().veľkosť());
		else veľkosťPísma(veľkosťPísma.floatValue());
	}


	public String zoznamMien()
	{
		if (null == zoznamMien) return "";

		StringBuffer zlúčenieZoznamu = null;

		for (String meno : zoznamMien)
		{
			if (null == zlúčenieZoznamu)
				zlúčenieZoznamu = new StringBuffer(meno);
			else
			{
				zlúčenieZoznamu.append('\n');
				zlúčenieZoznamu.append(meno);
			}
		}

		if (null == zlúčenieZoznamu)
			zlúčenieZoznamu = new StringBuffer();

		return zlúčenieZoznamu.toString();
	}

	public void zoznamMien(String zoznam)
	{
		if (null == zoznam || zoznam.isEmpty())
		{
			if (null != zoznamMien) zoznamMien.clear();
			zoznamMien = null;
		}
		else
		{
			if (null == zoznamMien)
				zoznamMien = new Vector<String>();
			else
				zoznamMien.clear();

			String[] mená = zoznam.split("\\r?\\n|\\n\\r?");
			for (String meno : mená) zoznamMien.add(meno);
		}
	}

	public void pridajMeno(String meno)
	{
		if (null == zoznamMien)
			zoznamMien = new Vector<String>();
		zoznamMien.add(meno);
	}


	public boolean cyklickýZoznamMien()
	{ return cyklickýZoznam; }

	public void cyklickýZoznamMien(boolean cyklický)
	{ this.cyklickýZoznam = cyklický; }


	public double mieraZaoblenia()
	{
		return mieraZaoblenia;
	}

	public void aktualizujZaoblenie()
	{
		zaoblenie(null == this.režimKreslenia ||
			RežimKresleniaLinky.VLASTNÝ_TVAR == this.režimKreslenia ?
			((min(výška(), šírka()) / 2) * mieraZaoblenia) : 0.0);
	}

	public void mieraZaoblenia(double mieraZaoblenia)
	{
		if (mieraZaoblenia < 0) mieraZaoblenia = 0;
		this.mieraZaoblenia = mieraZaoblenia;
		zaoblenie(null == this.režimKreslenia ||
			RežimKresleniaLinky.VLASTNÝ_TVAR == this.režimKreslenia ?
			((min(výška(), šírka()) / 2) * mieraZaoblenia) : 0.0);
	}


	// (Tieto dve metódy sú tu definované v podstate len kvôli úplnosti.
	// Neráta sa s ich praktickým využitím.)
	public int početČiarObrysu() { return početČiarObrysu; }
	public void početČiarObrysu(int početČiarObrysu) {
		this.početČiarObrysu = početČiarObrysu; }


	// (Tieto dve metódy sú tu definované v podstate len kvôli úplnosti.
	// Neráta sa s ich praktickým využitím.)
	public double rozostupyČiarObrysu() { return rozostupyČiarObrysu; }
	public void rozostupyČiarObrysu(double rozostupyČiarObrysu) {
		this.rozostupyČiarObrysu = rozostupyČiarObrysu; }


	public boolean zobrazInformácie()
	{
		return zobrazInformácie;
	}

	public void zobrazInformácie(boolean zobrazInformácie)
	{
		this.zobrazInformácie = zobrazInformácie;
		aktualizujKontextovúPonuku();
		žiadajPrekreslenie();
	}

	public void prepniZobrazenieInformácií()
	{
		zobrazInformácie = !zobrazInformácie;
		aktualizujKontextovúPonuku();
		žiadajPrekreslenie();
	}


	public boolean označená()
	{
		return označená;
	}

	public void označ(boolean označená)
	{
		this.označená = označená;
		žiadajPrekreslenie();
	}

	public void prepniOznačenie()
	{
		označená = !označená;
		žiadajPrekreslenie();
	}


	// Súborové aktivity:

	public boolean ulož(Súbor súbor, String názov) // throws IOException
	{
		if (neaktívny()) return false;
		súbor.vnorMennýPriestorVlastností(id = názov);
		try
		{
			súbor.zapíšVlastnosť("popis", popis);
			súbor.zapíšVlastnosť("veľkosťPísma", veľkosťPísma);
			súbor.zapíšVlastnosť("popisPod", popisPod);
			súbor.zapíšVlastnosť("x", polohaX());
			súbor.zapíšVlastnosť("y", polohaY());
			súbor.zapíšVlastnosť("účel", účel);
			súbor.zapíšVlastnosť("časovač", časovač);
			súbor.zapíšVlastnosť("rozptyl", rozptyl);
			súbor.zapíšVlastnosť("počiatočnýČas", počiatočnýČas);
			súbor.zapíšVlastnosť("limit", limit);
			súbor.zapíšVlastnosť("kapacita", kapacita);

			{
				if (null == zoznamMien || (null != účel &&
					účel != ÚčelLinky.EMITOR))
					súbor.zapíšVlastnosť("početMien", null);
				else
				{
					súbor.zapíšVlastnosť("početMien", zoznamMien.size());
					for (int i = 0; i < zoznamMien.size(); ++i)
						súbor.zapíšVlastnosť("meno[" + i + "]",
							zoznamMien.get(i));
				}
				súbor.zapíšVlastnosť("cyklickýZoznam", cyklickýZoznam);
			}

			switch (režimVýberuZákazníkov)
			{
			case POSLEDNÝ:
				súbor.zapíšVlastnosť("režimVýberuZákazníkov", "POSLEDNÝ");
				break;

			case NÁHODNÝ:
				súbor.zapíšVlastnosť("režimVýberuZákazníkov", "NÁHODNÝ");
				break;

			default:
				súbor.zapíšVlastnosť("režimVýberuZákazníkov", "PRVÝ");
			}

			switch (režimVýberuLiniek)
			{
			case NÁHODNÉ:
				súbor.zapíšVlastnosť("režimVýberuLiniek", "NÁHODNÉ");
				break;

			case PODĽA_PRIORÍT:
				súbor.zapíšVlastnosť("režimVýberuLiniek", "PODĽA_PRIORÍT");
				break;

			default:
				súbor.zapíšVlastnosť("režimVýberuLiniek", "POSTUPNÉ");
			}

			súbor.zapíšVlastnosť("režimKreslenia", režimKreslenia);
			súbor.zapíšVlastnosť("názovTvaru", názovTvaru);
			súbor.zapíšVlastnosť("transformácieTvaru", transformácieTvaru);
			súbor.zapíšVlastnosť("pomer", pomer());
			súbor.zapíšVlastnosť("veľkosť", veľkosť());
			súbor.zapíšVlastnosť("uhol", uhol());
			súbor.zapíšVlastnosť("mieraZaoblenia", mieraZaoblenia);
			súbor.zapíšVlastnosť("početČiarObrysu", početČiarObrysu);
			súbor.zapíšVlastnosť("rozostupyČiarObrysu", rozostupyČiarObrysu);
			súbor.zapíšVlastnosť("zobrazInformácie", zobrazInformácie);
		}
		finally
		{
			súbor.vynorMennýPriestorVlastností();
		}
		return true;
	}

	public void čítaj(Súbor súbor, String názov) throws IOException
	{
		súbor.vnorMennýPriestorVlastností(id = názov);
		try
		{
			popis(súbor.čítajVlastnosť("popis", (String)null));
			veľkosťPísma(súbor.čítajVlastnosť("veľkosťPísma", (Double)null));

			{
				Boolean kde = súbor.čítajVlastnosť("popisPod", false);
				popisPod = null == kde ? false : kde;
			}

			{
				Double x = súbor.čítajVlastnosť("x", (Double)0.0);
				Double y = súbor.čítajVlastnosť("y", (Double)0.0);
				skočNa(null == x ? 0 : x, null == y ? 0 : y);
			}

			{
				String účel = súbor.čítajVlastnosť("účel", (String)null);
				if (null == účel) this.účel = null; else switch (účel)
				{
				case "EMITOR": this.účel = ÚčelLinky.EMITOR; break;
				case "ZÁSOBNÍK": this.účel = ÚčelLinky.ZÁSOBNÍK; break;
				case "ČAKÁREŇ": this.účel = ÚčelLinky.ČAKÁREŇ; break;
				case "ZASTÁVKA": this.účel = ÚčelLinky.ZASTÁVKA; break;
				case "DOPRAVNÍK": this.účel = ÚčelLinky.DOPRAVNÍK; break;
				case "MENIČ": this.účel = ÚčelLinky.MENIČ; break;
				case "UVOĽŇOVAČ": this.účel = ÚčelLinky.UVOĽŇOVAČ; break;
				default: this.účel = null; break;
				}
			}

			{
				Double hodnota = súbor.čítajVlastnosť("časovač", (Double)1.0);
				časovač = null == hodnota ? 1.0 : hodnota;
				hodnota = súbor.čítajVlastnosť("rozptyl", 0.0);
				rozptyl = null == hodnota ? 0.0 : hodnota;
				hodnota = súbor.čítajVlastnosť("počiatočnýČas", (Double)0.0);
				počiatočnýČas = null == hodnota ? 0.0 : hodnota;
			}

			{
				Integer hodnota = súbor.čítajVlastnosť("limit", (Integer)0);
				limit = null == hodnota ? 0 : hodnota;
				hodnota = súbor.čítajVlastnosť("kapacita", (Integer)10);
				kapacita = null == hodnota ? 10 : hodnota;
			}

			{
				if (null != zoznamMien) zoznamMien.clear();
				Integer počet = súbor.čítajVlastnosť(
					"početMien", (Integer)null);
				if (null == počet || 0 == počet)
					zoznamMien = null;
				else
				{
					if (null == zoznamMien) zoznamMien = new Vector<String>();
					for (int i = 0; i < počet; ++i)
					{
						String meno = súbor.čítajVlastnosť(
							"meno[" + i + "]", (String)null);
						if (null != meno) zoznamMien.add(meno);
					}
				}

				Boolean hodnota = súbor.čítajVlastnosť(
					"cyklickýZoznam", cyklickýZoznam);
				if (null == hodnota) cyklickýZoznam = true;
				else cyklickýZoznam = hodnota;
			}

			{
				Double pomer = súbor.čítajVlastnosť("pomer", (Double)1.2);
				pomer = null == pomer ? 1.2 : pomer;
				pomer(pomer < 0 ? 0 : pomer);
			}

			{
				Double veľkosť = súbor.čítajVlastnosť("veľkosť", (Double)50.0);
				veľkosť = null == veľkosť ? 50.0 : veľkosť;
				veľkosť(veľkosť < 0 ? 0 : veľkosť);
			}

			{
				Double uhol = súbor.čítajVlastnosť("uhol", (Double)90.0);
				uhol = null == uhol ? 90.0 : uhol;
				uhol(uhol);
			}

			byte transformácieTvaru = 0; // keby toto tu nie je lokálne, tak
				// preevidujTvar(null) nižšie to prepíše…
			{
				Integer transformuj = súbor.čítajVlastnosť(
					"transformácieTvaru", (int)transformácieTvaru);
				transformácieTvaru = null == transformuj ? (byte)0 :
					transformuj.byteValue();
			}

			{
				String režimVýberuZákazníkov = súbor.čítajVlastnosť(
					"režimVýberuZákazníkov", (String)null);

				if (null == režimVýberuZákazníkov)
					this.režimVýberuZákazníkov =
						RežimVýberuZákazníkov.PRVÝ;
				else
					switch (režimVýberuZákazníkov)
					{
					case "POSLEDNÝ":
						this.režimVýberuZákazníkov =
							RežimVýberuZákazníkov.POSLEDNÝ;
						break;

					case "NÁHODNÝ":
						this.režimVýberuZákazníkov =
							RežimVýberuZákazníkov.NÁHODNÝ;
						break;

					default:
						this.režimVýberuZákazníkov =
							RežimVýberuZákazníkov.PRVÝ;
					}
			}

			{
				String režimVýberuLiniek = súbor.čítajVlastnosť(
					"režimVýberuLiniek", (String)null);

				if (null == režimVýberuLiniek)
					this.režimVýberuLiniek =
						RežimVýberuLiniek.POSTUPNÉ;
				else
					switch (režimVýberuLiniek)
					{
					case "NÁHODNÉ":
						this.režimVýberuLiniek =
							RežimVýberuLiniek.NÁHODNÉ;
						break;

					case "PODĽA_PRIORÍT":
						this.režimVýberuLiniek =
							RežimVýberuLiniek.PODĽA_PRIORÍT;
						break;

					default:
						this.režimVýberuLiniek =
							RežimVýberuLiniek.POSTUPNÉ;
					}
			}

			{
				preevidujTvar(null);
				String režimKreslenia = súbor.čítajVlastnosť(
					"režimKreslenia", (String)null);
				if (null == režimKreslenia) this.režimKreslenia = null; else
				switch (režimKreslenia)
				{
				case "ELIPSA":
					this.režimKreslenia = RežimKresleniaLinky.ELIPSA;
					break;

				case "OBDĹŽNIK":
					this.režimKreslenia = RežimKresleniaLinky.OBDĹŽNIK;
					break;

				case "VLASTNÝ_TVAR":
					this.režimKreslenia = RežimKresleniaLinky.VLASTNÝ_TVAR;
					preevidujTvar(súbor.čítajVlastnosť(
						"názovTvaru", (String)null), false, transformácieTvaru);
					if (null == názovTvaru) this.režimKreslenia = null;
					break;

				default: this.režimKreslenia = null; break;
				}
			}

			{
				Double hodnota = súbor.čítajVlastnosť(
					"mieraZaoblenia", (Double)0.50);
				mieraZaoblenia(null == hodnota ? 0.50 : hodnota);
			}

			{
				Integer hodnota = súbor.čítajVlastnosť(
					"početČiarObrysu", (Integer)1);
				početČiarObrysu = null == hodnota ? 1 : hodnota;
			}

			{
				Double hodnota = súbor.čítajVlastnosť(
					"rozostupyČiarObrysu", (Double)2.0);
				rozostupyČiarObrysu = null == hodnota ? 2.0 : hodnota;
			}

			{
				Boolean hodnota = súbor.čítajVlastnosť(
					"zobrazInformácie", (Boolean)true);
				zobrazInformácie = null == hodnota ? true : hodnota;
			}

			produkcia = 0;
			označená = false;
			bliká = 0;
			klik = 0;
			myš = null;
			čas = Systém.čas;
			ďalšiaSpojnica = -1;
			if (jeEmitor()) čas += interval() + počiatočnýČas;
			vyraďZákazníkov();
			aktualizujKontextovúPonuku();
		}
		finally
		{
			súbor.vynorMennýPriestorVlastností();
		}
	}

	public boolean uložSpojnice(Súbor súbor) // throws IOException
	{
		if (neaktívny()) return false;
		súbor.vnorMennýPriestorVlastností(id);
		try
		{
			Spojnica[] spojnice = spojniceZ();
			int početSpojníc = spojnice.length;
			súbor.zapíšVlastnosť("početSpojníc", početSpojníc);

			for (int i = 0; i < početSpojníc; ++i)
			{
				Spojnica spojnica = spojnice[i];
				súbor.vnorMennýPriestorVlastností("spojnica[" + i + "]");
				try
				{
					GRobot cieľovýRobot = spojnica.cieľ();
					if (cieľovýRobot instanceof Linka)
					{
						Linka cieľováLinka = (Linka)cieľovýRobot;
						súbor.zapíšVlastnosť("cieľ", cieľováLinka.id);
					}
					súbor.zapíšVlastnosť("váha", spojnica.parameter("váha"));
				}
				finally
				{
					súbor.vynorMennýPriestorVlastností();
				}
			}
		}
		finally
		{
			súbor.vynorMennýPriestorVlastností();
		}
		return true;
	}

	public void čítajSpojnice(Súbor súbor) throws IOException
	{
		súbor.vnorMennýPriestorVlastností(id);
		try
		{
			Integer početSpojníc = súbor.čítajVlastnosť("početSpojníc", 0);
			početSpojníc = null == početSpojníc ? 0 : početSpojníc;

			for (int i = 0; i < početSpojníc; ++i)
			{
				súbor.vnorMennýPriestorVlastností("spojnica[" + i + "]");
				try
				{
					String cieľ = súbor.čítajVlastnosť("cieľ", (String)null);
					if (null != cieľ) for (Linka linka : linky)
						if (cieľ.equals(linka.id))
						{
							Spojnica spojnica = spojnica(linka);
							Double váha = súbor.čítajVlastnosť("váha",
								(Double)null);
							if (null != váha && !Double.isFinite(váha))
								váha = null;
							spojnica.parameter("váha", váha);
							break;
						}
				}
				finally
				{
					súbor.vynorMennýPriestorVlastností();
				}
			}
		}
		finally
		{
			súbor.vynorMennýPriestorVlastností();
		}
	}


	// Obsluha udalostí:

	@Override public boolean myšV()
	{
		if (neaktívny()) return false;
		if (RežimKresleniaLinky.ELIPSA != režimKreslenia)
			return myšVObdĺžniku();
		return myšVElipse();
	}

	@Override public boolean bodV(Poloha bod)
	{
		if (neaktívny()) return false;
		if (RežimKresleniaLinky.ELIPSA != režimKreslenia)
			return bodVObdĺžniku(bod);
		return bodVElipse(bod);
	}

	@Override public void klik()
	{
		if (neaktívny()) return;

		if (tlačidloMyši(ĽAVÉ))
		{
			if (myš().isControlDown() || myš().isShiftDown())
			{
				if (myšV()) prepniOznačenie();
			}
			else if (!myš().isAltDown())
			{
				klik = 7;
				myš = myš();
			}
		}
		else if (tlačidloMyši(PRAVÉ))
		{
			if (myšV())
			{
				klik = 7;
				myš = myš();
			}
		}
	}


	@Override public void stlačenieTlačidlaMyši()
	{
		if (neaktívny()) return;

		ťaháSa = false;
		if (tlačidloMyši(ĽAVÉ) && !myš().isShiftDown() &&
			!myš().isAltDown() && !myš().isControlDown())
		{
			// Overí kliknutie do značiek úprav:
			Poloha p = poloha();
			double zx = zaoblenieX();
			double zy = zaoblenieY();
			zaoblenieX(0);
			zaoblenieY(0);

			preskočVpravo(šírka() / 2);
			boolean meniťŠírku = myšVoŠtvorci(8);
			skoč(výška() / 2);
			boolean točiť = myšVKruhu(8);
			odskoč(výška());
			boolean meniťRozmery = myšVoŠtvorci(8);
			preskočVľavo(šírka() / 2);
			boolean meniťVýšku = myšVoŠtvorci(8);

			skoč(výška());
			preskočVľavo((šírka() - min(výška(),
				šírka()) * mieraZaoblenia) / 2);
			boolean meniťMieruZaoblenia = myšVKruhu(8);

			zaoblenieX(zx);
			zaoblenieY(zy);
			poloha(p);

			if (meniťRozmery) // <— sem
			{
				upravujeSa = this;
				typÚprav = 2; // ^ pozri hore ^
			}
			else if (meniťVýšku) // <— sem
			{
				upravujeSa = this;
				typÚprav = 3; // ^ pozri hore ^
			}
			else if (meniťŠírku) // <— sem
			{
				upravujeSa = this;
				typÚprav = 0; // ^ pozri hore ^
			}
			else if (točiť) // <— sem
			{
				upravujeSa = this;
				typÚprav = 1; // ^ pozri hore ^
			}
			else if (meniťMieruZaoblenia) // <— sem
			{
				upravujeSa = this;
				typÚprav = 4; // ^ pozri hore ^
			}
			else if (myšV())
			{
				ťaháSa = true;
			}
			else if (označená)
			{
				for (Linka linka : linky) if (this != linka &&
					linka.aktívny() && linka.myšV())
				{
					ťaháSa = true;
					break;
				}
			}

			// Linka, ktorá zaznamená začatie jej úprav odošle príkaz
			// na zrušenie globálnych úprav:
			if (null != upravujeSa || ťaháSa) vyzviRoboty();
		}
	}

	@Override public void ťahanieMyšou()
	{
		if (neaktívny()) return;

		if (this == upravujeSa)
		{
			Bod p1 = poslednáPolohaMyši();
			Bod p2 = polohaMyši();
			switch (typÚprav)
			{
			case 0: // meniťŠírku
				{
					double vzdialenosť1 = vzdialenosťK(p1);
					double vzdialenosť2 = vzdialenosťK(p2);
					double nováŠírka = šírka() + 2 *
						(vzdialenosť2 - vzdialenosť1);
					if (nováŠírka < 0) nováŠírka = 0;
					šírka(nováŠírka);
					aktualizujZaoblenie();
					aktualizujSpojnice();
				}
				break;

			case 1: // upravujeSa
				{
					double uhol1 = smerNa(p1);
					double uhol2 = smerNa(p2);
					vľavo(uhol2 - uhol1);
				}
				break;

			case 2: // meniťRozmery
				{
					p1.otoč(this, 90 - uhol());
					p2.otoč(this, 90 - uhol());
					double rozdielX = p2.getX() - p1.getX();
					double rozdielY = p1.getY() - p2.getY();
					double nováŠírka = šírka() + 2 * rozdielX;
					double nováVýška = výška() + 2 * rozdielY;
					if (nováŠírka < 0) nováŠírka = 0;
					if (nováVýška < 0) nováVýška = 0;
					rozmery(nováŠírka, nováVýška);
					aktualizujZaoblenie();
					aktualizujSpojnice();
				}
				break;

			case 3: // meniťVýšku
				{
					double vzdialenosť1 = vzdialenosťK(p1);
					double vzdialenosť2 = vzdialenosťK(p2);
					double nováVýška = výška() + 2 *
						(vzdialenosť2 - vzdialenosť1);
					if (nováVýška < 0) nováVýška = 0;
					výška(nováVýška);
					aktualizujZaoblenie();
					aktualizujSpojnice();
				}
				break;

			case 4: // meniťMieruZaoblenia
				{
					p1.otoč(this, 90 - uhol());
					p2.otoč(this, 90 - uhol());
					double rozdielX = p2.getX() - p1.getX();
					mieraZaoblenia(mieraZaoblenia() +
						2 * rozdielX / min(výška(), šírka()));
					aktualizujSpojnice();
				}
				break;
			}
		}
		else if (ťaháSa)
		{
			Bod p = Bod.rozdiel(polohaMyši(), poslednáPolohaMyši());
			posuňAjZákazníkov(p.getX(), p.getY());
		}
	}

	public void posuňAjZákazníkov(double Δx, double Δy)
	{
		skoč(Δx, Δy);
		for (Zákazník zákazník : zákazníci)
		{
			zákazník.skoč(Δx, Δy);
			if (zákazník.smerujeDoCieľa())
			{
				Bod b = zákazník.cieľ();
				b.posuň(Δx, Δy);
				zákazník.upravCieľ(b);
			}
		}
	}

	@Override public void uvoľnenieTlačidlaMyši()
	{
		if (neaktívny()) return;
		if (this == upravujeSa) upravujeSa = null;
		else if (ťaháSa) ťaháSa = false;
	}

	@Override public void voľbaKontextovejPoložky()
	{
		if (neaktívny()) return;

		if (položkaUpravPopis.zvolená()) upravPopis();
		else if (položkaZrušÚčel.zvolená()) zrušÚčel();
		else if (položkaZmeňNaEmitor.zvolená()) zmeňNaEmitor();
		else if (položkaZmeňNaZásobník.zvolená()) zmeňNaZásobník();
		else if (položkaZmeňNaČakáreň.zvolená()) zmeňNaČakáreň();
		else if (položkaZmeňNaZastávku.zvolená()) zmeňNaZastávku();
		else if (položkaZmeňNaDopravník.zvolená()) zmeňNaDopravník();
		else if (položkaZmeňNaMenič.zvolená()) zmeňNaMenič();
		else if (položkaZmeňNaUvoľňovač.zvolená()) zmeňNaUvoľňovač();
		else if (položkaRežimVýberuLiniek.zvolená())
			zmeňRežimVýberuLiniek();
		else if (položkaZrušTvar.zvolená()) zrušTvar();
		else if (položkaZmeňNaElipsu.zvolená()) zmeňNaElipsu();
		else if (položkaZmeňNaObdĺžnik.zvolená()) zmeňNaObdĺžnik();
		else if (položkaZmeňNaInýTvar.zvolená()) zmeňNaInýTvar();
		else if (položkaUpravKoeficienty.zvolená()) upravKoeficienty();
		else if (položkaUpravZoznamMien.zvolená()) upravZoznamMien();
		else if (položkaUpravVizuály.zvolená()) upravVizuály();
		else if (položkaPrepniInformácie.zvolená())
			prepniZobrazenieInformácií();
		else if (položkaVymaž.zvolená()) vymaž();
	}

	private void kresliZnačkyÚprav()
	{
		Farba f = farba();
		Poloha p = poloha();

		zaoblenie(0);
		preskočVpravo(šírka() / 2);
		if (this == upravujeSa && 0 == typÚprav) // meniťŠírku
			farba(svetločervená); else farba(snehová);
		vyplňŠtvorec(5); farba(f); kresliŠtvorec(5);

		skoč(výška() / 2);
		if (this == upravujeSa && 1 == typÚprav) // točiť
			farba(svetločervená); else farba(snehová);
		kruh(5); farba(f); kružnica(5);

		odskoč(výška());
		if (this == upravujeSa && 2 == typÚprav) // meniťRozmery
			farba(svetločervená); else farba(snehová);
		vyplňŠtvorec(5); farba(f); kresliŠtvorec(5);

		preskočVľavo(šírka() / 2);
		if (this == upravujeSa && 3 == typÚprav) // meniťVýšku
			farba(svetločervená); else farba(snehová);
		vyplňŠtvorec(5); farba(f); kresliŠtvorec(5);

		skoč(výška());
		preskočVľavo((šírka() - min(výška(), šírka()) * mieraZaoblenia) / 2);
		if (this == upravujeSa && 4 == typÚprav) // meniťMieruZaoblenia
			farba(svetločervená); else farba(snehová);
		kruh(5); farba(f); kružnica(5);

		poloha(p);
	}

	@Override public void kresliTvar()
	{
		if (neaktívny()) return;

		// šedá      – bez účelu
		// modrá     – generuje zákazníkov
		// fialová   – zhromažďuje zákazníkov
		// tyrkysová – zhromažďuje zákazníkov s mierou trpezlivosti
		// hnedá     – prepravuje zákazníkov medzi linkami
		// oranžová  – transformuje (mení, spracúva) zákazníkov
		// zelená    – uvoľňuje zákazníkov (definitívne ich vybavuje)

		// Kreslenie základného tvaru:
		if (0 == bliká || 0 == (bliká / 2) % 2)
		{
			Farba farba = šedá;
			if (null != účel) switch (účel)
			{
			case EMITOR: farba = modrá; break;
			case ZÁSOBNÍK: farba = fialová; break;
			case ČAKÁREŇ: farba = tyrkysová; break;
			case ZASTÁVKA: farba = atramentová; break;
			case DOPRAVNÍK: farba = hnedá; break;
			case MENIČ: farba = oranžová; break;
			case UVOĽŇOVAČ: farba = zelená; break;
			}

			farba(farba);

			if (RežimKresleniaLinky.ELIPSA != režimKreslenia)
			{
				if (RežimKresleniaLinky.VLASTNÝ_TVAR == režimKreslenia &&
					null != tvarTvaru)
				{
					if (početČiarObrysu > 1)
					{
						// Záloha pred kreslením viacerých čiar obrysu:
						double veľkosťB = veľkosť(), pomerB = pomer();

						try
						{
							double š = šírka() - (2 * rozostupyČiarObrysu),
								v = výška() - (2 * rozostupyČiarObrysu);

							for (int i = 0; i < početČiarObrysu &&
								š >= 0 && v >= 0; ++i,
								š -= (2 * rozostupyČiarObrysu),
								v -= (2 * rozostupyČiarObrysu))
							{
								kresliTvar(tvarTvaru, true);
								rozmery(š, v);
							}
						}
						finally
						{
							veľkosť(veľkosťB);
							pomer(pomerB);
						}
					}
					else if (1 == početČiarObrysu)
					{
						kresliTvar(tvarTvaru, true);
					}
				}
				else
				{
					double š = šírka() / 2, v = výška() / 2;

					for (int i = 0; i < početČiarObrysu
						&& š >= 0 && v >= 0; ++i,
						š -= rozostupyČiarObrysu,
						v -= rozostupyČiarObrysu)
					{
						zaoblenie(min(š, v) * mieraZaoblenia);
						obdĺžnik(š, v);
					}
				}
			}
			else
			{
				double š = šírka() / 2, v = výška() / 2;
				for (int i = 0; i < početČiarObrysu && š >= 0 && v >= 0;
					++i, š -= rozostupyČiarObrysu, v -= rozostupyČiarObrysu)
					elipsa(š, v);
			}

			if (0 != časovač && čas > Systém.čas &&
				(0 == limit || produkcia < limit))
			{
				skoč(veľkosť() + 10);
				zaoblenie(0);
				double š = (čas - Systém.čas) / časovač;
				if (š > 1.0)
				{
					š = 1.0 / š;
					vyplňObdĺžnik((šírka() / 2.0) * š, 6);
					kresliObdĺžnik(šírka() / 2.0, 6);
				}
				else vyplňObdĺžnik((šírka() / 2.0) * š, 6);
				odskoč(veľkosť() + 10);
			}

			if (null != popis)
			{
				if (null != riadkyPopisu)
				{
					Poloha p = poloha();
					if (popisPod)
						odskoč((výškaRiadka() + výška()) / 2);
					else
						skoč(((riadkyPopisu.length - 1) *
							výškaRiadka()) / 2);
					double skok = výškaRiadka();
					for (String riadokPopisu : riadkyPopisu)
					{
						text(riadokPopisu);
						odskoč(skok);
					}
					poloha(p);
				}
				else
				{
					if (popisPod)
					{
						Poloha p = poloha();
						odskoč((výškaRiadka() + výška()) / 2);
						text(popis);
						poloha(p);
					}
					else text(popis);
				}
			}
		}

		// Kreslenie súvisiace s označením a rôznymi UI aktivitami linky:
		hrúbkaČiary(0.5);
		if (RežimKresleniaLinky.ELIPSA != režimKreslenia)
		{
			if (Systém.jeZačiatokKonektora(this))
			{
				obdĺžnik(šírka() / 2 + 4.4, výška() / 2 + 4.4);
				obdĺžnik(šírka() / 2 + 7.7, výška() / 2 + 7.7);
				if (označená) kresliZnačkyÚprav();
			}
			else if (označená)
			{
				obdĺžnik(šírka() / 2 + 2.5, výška() / 2 + 2.5);
				kresliZnačkyÚprav();
			}
			else if (ťaháSa)
				obdĺžnik(šírka() / 2 - 1.5, výška() / 2 - 1.5);
			else if (this == upravujeSa) kresliZnačkyÚprav();
		}
		else
		{
			if (Systém.jeZačiatokKonektora(this))
			{
				elipsa(šírka() / 2 + 4.4, výška() / 2 + 4.4);
				elipsa(šírka() / 2 + 7.7, výška() / 2 + 7.7);
				if (označená) kresliZnačkyÚprav();
			}
			else if (označená)
			{
				elipsa(šírka() / 2 + 2.5, výška() / 2 + 2.5);
				kresliZnačkyÚprav();
			}
			else if (ťaháSa)
				elipsa(šírka() / 2 - 1.5, výška() / 2 - 1.5);
			else if (this == upravujeSa) kresliZnačkyÚprav();
		}

		// „Kreslenie“ (alias výpis) informácií (o linke):

		{ Písmo písmo = písmo(); try { písmo(hlavnýRobot().písmo());

		skoč(10 + veľkosť() * pomer(), veľkosť() - výškaRiadka() / 2.0);

		if (zobrazTypy || zobrazHladiny)
		{
			farba(červená);

			if (zobrazTypy)
			{
				text(S("Typ/účel: ", účel), KRESLI_PRIAMO);
				skoč(0, -1 * výškaRiadka());
			}

			if (zobrazHladiny)
			{
				text(S("Hladina: ", hladina), KRESLI_PRIAMO);
				skoč(0, -1 * výškaRiadka());
			}

			skoč(0, -0.5 * výškaRiadka());
		}

		if (zobrazInformácie)
		{
			farba(čierna);

			if (ÚčelLinky.ZÁSOBNÍK == účel || ÚčelLinky.ČAKÁREŇ == účel ||
				ÚčelLinky.ZASTÁVKA == účel)
			{
				text(S("Obsadenosť: ", zákazníci.size()), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else
			{
				int size = zákazníci.size();
				switch (size)
				{
				case 0: if (null == účel) skoč(0, výškaRiadka()); else
					text("Voľná", KRESLI_PRIAMO); break;
				case 1: text("Obsadená", KRESLI_PRIAMO); break;
				default: text(S("Obsadenosť: ", size), KRESLI_PRIAMO);
				}
				skoč(0, -výškaRiadka());
			}

			if (null == účel)
			{
				text(S("Kapacita: ", kapacita), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());

				text(S("Časovač: ", F(časovač, 2), (0.0 == rozptyl ? "" :
					" ± " + F(rozptyl, 2))), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());

				if (0 != limit)
				{
					if (limit < 0)
						text("Produkcia je pre túto linku zastavená.",
							KRESLI_PRIAMO);
					else
						text(S("Produkcia: ", produkcia, " / ", limit),
							KRESLI_PRIAMO);
					skoč(0, -výškaRiadka());
				}
			}
			else if (ÚčelLinky.ZÁSOBNÍK == účel)
			{
				text(S("Kapacita: ", kapacita), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else if (ÚčelLinky.ČAKÁREŇ == účel)
			{
				text(S("Kapacita: ", kapacita), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());

				text(S("Trpezlivosť: ", F(časovač, 2),
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2))),
					KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else if (ÚčelLinky.ZASTÁVKA == účel)
			{
				text(S("Kapacita: ", kapacita), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());

				text(S("Čakanie: ", F(časovač, 2),
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2))),
					KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else if (ÚčelLinky.DOPRAVNÍK == účel)
			{
				text(S("Doprava: ", F(časovač, 2), (0.0 == rozptyl ? "" :
					" ± " + F(rozptyl, 2))), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else if (ÚčelLinky.EMITOR == účel)
			{
				if (0 == limit || produkcia < limit)
					text(S("Ďalší: ", F(čas, 3)), KRESLI_PRIAMO);
				else
					text("Bol dosiahnutý limit produkcie.", KRESLI_PRIAMO);

				skoč(0, -výškaRiadka());

				text(S("Interval: ", F(časovač, 2),
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2)),
					(0.0 == počiatočnýČas ? "" : " (+ počiatok: " +
						F(počiatočnýČas, 2) + ")")),
					KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());

				if (0 != limit)
				{
					if (limit < 0)
						text("Produkcia je pre túto linku zastavená.",
							KRESLI_PRIAMO);
					else
						text(S("Produkcia: ", produkcia, " / ", limit),
							KRESLI_PRIAMO);
					skoč(0, -výškaRiadka());
				}
			}
			else
			{
				text(S("Spracovanie: ", F(časovač, 2), (0.0 == rozptyl ? "" :
					" ± " + F(rozptyl, 2))), KRESLI_PRIAMO);
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

			if (null != zoznamMien && !zoznamMien.isEmpty())
			{
				text(S("Pomenovaných: ", zoznamMien.size()), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
		}

		} finally { písmo(písmo); }}
	}

	@Override public void aktivácia()
	{
		zobraz();
	}

	@Override public void deaktivácia()
	{
		// Keď je linka vymazaná (deaktivovaná), tak z nej treba vyhodiť
		// aj všetkých zákazníkov, odevidovať tvar (a zrušiť spojnice).
		zrušSpojnice();
		vyraďZákazníkov();
		preevidujTvar(null);
		skry();
	}


	@Override public void tik()
	{
		if (bliká > 0)
		{
			--bliká;
			žiadajPrekreslenie();
		}

		if (klik > 0 && 0 >= --klik && null != myš)
		{
			if (ĽAVÉ == myš.getButton())
			{
				if (myš.getClickCount() > 1)
				{
					if (myšV())
					{
						blikni();
						Svet.vykonaťNeskôr(() -> upravKoeficienty());
					}
				}
				else
				{
					označ(myšV());
				}
			}
			else
			{
				blikni();
				if (myš.getClickCount() > 1)
					Svet.vykonaťNeskôr(() -> upravVizuály());
				else
					Svet.vykonaťNeskôr(() -> kontextováPonuka.zobraz());
			}

			myš = null;
		}
	}

	@Override public boolean činnosť()
	{
		Boolean retval = null; try { logIn("(", this, ")");

		if (null != účel) switch (účel)
		{
		case EMITOR:
			{
				if (čas < Systém.čas)
				{
					if (0 == limit || produkcia < limit)
					{
						Zákazník zákazník = Zákazník.nový(this);
						if (null != zoznamMien && (cyklickýZoznam ||
							indexMena < zoznamMien.size()))
						{
							zákazník.pomenuj(zoznamMien.get(
								indexMena % zoznamMien.size()));
							++indexMena;
						}
						logInfo("novýZákazník: ", zákazník);
						++produkcia;
					}
					čas += interval();
					return retval = true;
				}
			}
			break;

		case ZÁSOBNÍK:
		case ČAKÁREŇ:
			// ZASTÁVKA je spracúvaná v zákazníkovi
			if (!zákazníci.isEmpty())
			{
				// Zásobníky a ćakárne sledujú, či sa pre prvého čakajúceho
				// zákazníka neuvoľnila nejaká linka, aby sa tam mohol poslať,
				// pričom čakanie zákazníkov v zásobníku je časovo neobmedzené
				// a v čakárni majú zákazníci určitú mieru trpezlivosti čakania.

				Zákazník zákazník = dajZákazníka();
				Boolean failed = dajLinku(zákazník, false);
				if (null != failed) return retval = failed;
			}
			break;
		}

		return retval = false;

		} finally { logOut("Linka.činnosť: ", retval); }
	}


	// Simulácia…

	private final static double implicitnáVáha = 0.5;

	private final static Comparator<Spojnica> komparátorSpojníc =
	new Comparator<Spojnica>()
	{
		public int compare(Spojnica s1, Spojnica s2)
		{
			Object o1 = s1.parameter("váha");
			double váha1 = implicitnáVáha;
			if (o1 instanceof Double) váha1 = (Double)o1;

			Object o2 = s2.parameter("váha");
			double váha2 = implicitnáVáha;
			if (o2 instanceof Double) váha2 = (Double)o2;

			// if (váha1 < váha2) return -1;
			// if (váha1 > váha2) return 1;

			// Pozor, komparátor musí byť prevrátený:
			if (váha1 < váha2) return 1;
			if (váha1 > váha2) return -1;

			return 0;
		}
	};


	// Táto metóda je kľúčová pri rozhodovaní zákazníkov a emitorov, čo ďalej,
	// kam poslať seba (v prípade zákazníka) alebo nového vygenerovaného
	// zákazníka (v prípade emitora). Metóda hľadá linku, ktorá je prepojená
	// s emitorom alebo tou linkou, v ktorej sa nachádza aktuálny zákazník
	// a ktorá je voľná. Prepojenie sa určuje podľa spojníc a hľadanie
	// prebieha podľa stanovených priorít (pravdepodobností).
	// 
	// Režimy/spôsoby výberu/hľadania voľných liniek (spolu s návrhom
	// implementácie, ktorý bol priebežne adaptovaný a optimalizovaný):
	// 
	//  • postupné (cyklické) prechádzanie spojení:
	//    ◦ to, ktorou linkou sa začne hľadanie ďalšej voľnej linky určí
	//      cyklické počítadlo;
	//  • náhodné prechádzanie spojení vyvážené pravdepodobnosťami:
	//    ◦ každé spojenie má hodnotu, ktorá určí váhu pravdepodobnosti, že
	//      bude vybraná linka, do ktorej smeruje;
	//      ▪ z hodnôt sa zostaví „pásmo,“ v ktorom je každá linka zastúpená
	//        dĺžkou ekvivalentnou jej váhe;
	//      ▪ algoritmus následne generuje náhodné hodnoty v rozsahu nula až
	//        dĺžka pásma, to znamená, že každá hodnota sa zaradí niekde
	//        v rámci pásma, čo určí linku, ktorá má byť preskúmaná (či je
	//        voľná);
	//      ▪ toto sa bude vykonávať, kým nebude nenájdená voľná linka,
	//        pričom treba obsadené linky postupne zo zoznamu vyraďovať, aby
	//        sa hľadanie nevykonávalo nekonečne dlho;
	//  • podľa priorít – uprednostňujú sa linky s vyššou prioritou:
	//    ◦ každé hľadanie voľnej linky sa bude vždy začínať v rovnakom
	//      poradí, ktoré bude určené prioritami spojení.
	// 
	// Počas implementácie vysvitlo, že návratovou hodnotou tejto funkcie
	// by mali byť tri stavy:
	// 
	//  • linka je priradená a zákazník je zamestnaný,
	//  • to isté, len zákazník zostane nezamestnaný,
	//  • nenašla sa žiadna voľná linka.
	// 
	// Namiesto definovania novej enumerácie bol využitý objektový typ
	// Boolean, kde tretí stav je signalizovaný hodnotou null.
	// 
	public Boolean dajLinku(Zákazník zákazník, boolean pridajInterval)
	{
		Spojnica[] spojnice = spojniceZ();
		Arrays.sort(spojnice, komparátorSpojníc);

		if (RežimVýberuLiniek.POSTUPNÉ == režimVýberuLiniek)
		{
			if (++ďalšiaSpojnica >= spojnice.length) ďalšiaSpojnica = 0;
		}
		else
		{
			if (RežimVýberuLiniek.NÁHODNÉ == režimVýberuLiniek)
			{
				double pásmo[] = new double[spojnice.length];
				double rozsah = 0;

				for (int i = 0; i < spojnice.length; ++i)
				{
					Object o = spojnice[i].parameter("váha");
					double váha = implicitnáVáha;
					if (o instanceof Double) váha = (Double)o;
					pásmo[i] = (rozsah += váha);
				}

				voľba: for (int i = 0; i < spojnice.length; ++i)
				{
					Object o = spojnice[i].cieľ();
					if (!(o instanceof Linka)) continue;
					Linka cieľová = (Linka)o;

					double hodnota = náhodnéReálneČíslo(0, rozsah);

					for (int j = 0; j < pásmo.length; ++j)
					{
						if (hodnota <= pásmo[j])
						{
							if (cieľová.jeVoľná())
							{
								ďalšiaSpojnica = j;
								break voľba;
							}

							break;
						}
					}
				}
			}
			else ďalšiaSpojnica = 0;
		}

		for (int i = 0; i < spojnice.length; ++i)
		{
			Spojnica spojnica = spojnice[
				(i + ďalšiaSpojnica) % spojnice.length];

			GRobot cieľ = spojnica.cieľ();
			if (cieľ instanceof Linka)
			{
				Linka linka = (Linka)cieľ;
				logInfo("cieľ: ", linka);
				if (linka.evidujZákazníka(zákazník))
				{
					zákazník.priraďKLinke(linka, true);

					double interval = (linka.jeEmitor() ||
						linka.jeZásobník()) ? 0.0 : linka.interval();
					if (pridajInterval)
						zákazník.pridajInterval(interval);
					else
						zákazník.nastavInterval(interval);

					zákazník.upravCieľPodľaLinky(true);

					zákazník.maximálnaRýchlosť(
						Zákazník.faktorMaximálnejRýchlosti *
						Systém.dilatácia);
					zákazník.zrýchlenie(Zákazník.faktorZrýchlenia *
						Systém.dilatácia, false);
					zákazník.rýchlosť(0, false);

					if (linka.jeZásobník()) return false;
					return zákazník.čas() < Systém.čas;
				}
			}
		}

		return null;
	}


	public boolean jeVoľná()
	{
		Boolean retval = null; try { logIn("(", this, ")");

		// Ak je zoznam zákazníkov prázdny (resp. v rámci kapacity), tak je
		// linka voľná.
		// 
		// Dopravníky majú výnimku, tie majú v podstate neobmedzenú kapacitu.
		// Kapacita dopravníka by bola kontraproduktívna. Každý zákazník musí
		// dostať svoje miesto… Kapacita sa dá dodatočne vynútiť zásobníkom
		// umiestneným pred dopravník. Keď potrebujeme, aby sa tovar ukladal
		// na dopravník s určitým odstupom (t. j. docieliť určité úvodné
		// zdržanie, aby mohol nastúpiť ďalší zákazník), tak pred dopravník
		// treba zaradiť menič (opäť, spolu so zásobníkom, lebo by nastávali
		// úniky zákazníkov.
		// 
		// (V podstate všetko/všeličo sa dá docieliť vhodnou kombináciou
		// rôznych typov liniek.)
		// 
		// Emitory majú tiež výnimku, ale z nich zákazníci okamžite
		// odchádzajú, takže pri nich je táto vlastnosť v podstate
		// irelevantná.

		if (null != účel) switch (účel)
		{
		case EMITOR:
		case DOPRAVNÍK:
			return retval = true;

		case ZÁSOBNÍK:
		case ČAKÁREŇ:
		case ZASTÁVKA:
			return retval = zákazníci.size() < kapacita;

		case MENIČ:
		case UVOĽŇOVAČ:
			return retval = zákazníci.isEmpty();
		}

		return retval = false;

		} finally { logOut("Linka.jeVoľná: ", retval); }
	}

	public double interval()
	{
		return časovač + náhodnéReálneČíslo(rozptyl);
	}

	public ÚčelLinky účel()
	{
		return účel;
	}

	public boolean jeEmitor()
	{
		return ÚčelLinky.EMITOR == účel;
	}

	public boolean jeZásobník()
	{
		return ÚčelLinky.ZÁSOBNÍK == účel;
	}

	public boolean jeČakáreň()
	{
		return ÚčelLinky.ČAKÁREŇ == účel;
	}

	public boolean jeZastávka()
	{
		return ÚčelLinky.ZASTÁVKA == účel;
	}

	public boolean jeDopravník()
	{
		return ÚčelLinky.DOPRAVNÍK == účel;
	}

	public boolean jeMenič()
	{
		return ÚčelLinky.MENIČ == účel;
	}

	public boolean jeUvoľňovač()
	{
		return ÚčelLinky.UVOĽŇOVAČ == účel;
	}

	public void účel(ÚčelLinky účel)
	{
		try { logIn(účel, " (", this, "); Systém.čas: ", Systém.čas);

		produkcia = 0;
		this.účel = účel;
		čas = Systém.čas;
		if (jeEmitor()) čas += interval() + počiatočnýČas;
		vyraďZákazníkov();
		aktualizujKontextovúPonuku();

		} finally { logOut(); }
	}

	public Zákazník dajZákazníka()
	{
		if (zákazníci.isEmpty()) return null;

		if (null != režimVýberuZákazníkov) switch (režimVýberuZákazníkov)
		{
		case POSLEDNÝ:
			{
				Zákazník zákazník = zákazníci.lastElement();
				logInfo("poslednýZákazník: ", zákazník);
				return zákazník;
			}
			// break;

		case NÁHODNÝ:
			{
				Zákazník zákazník = zákazníci.náhodný();
				logInfo("náhodnýZákazník: ", zákazník);
				return zákazník;
			}
			// break;
		}

		Zákazník zákazník = zákazníci.firstElement();
		logInfo("prvýZákazník: ", zákazník);
		return zákazník;
	}

	// ‼Pozor‼ Zákazník.priraďKLinke(Linka) treba vykonať zvlášť‼
	public boolean evidujZákazníka(Zákazník zákazník)
	{
		Boolean retval = null; try { logIn(zákazník, " (", this, ")");

		if (jeVoľná())
		{
			zákazník.vyraďZLinky(false);
			zákazníci.add(zákazník);
			return retval = true;
		}
		return retval = false;

		} finally { logOut("Linka.evidujZákazníka: ", retval); }
	}

	// ‼Pozor‼ Lepšie je použiť volanie Zákazník.vyraďZLinky(), ktoré volá
	// túto metódu pre aktuálnu linku zákazníka.
	public void odoberZákazníka(Zákazník zákazník)
	{
		try { logIn(zákazník, "; zákazníci.indexOf(zákazník): ",
			zákazníci.indexOf(zákazník), " (", this, ")");

		zákazníci.remove(zákazník);

		} finally { logOut("zákazníci.size(): ", zákazníci.size()); }
	}

	public void vyraďZákazníkov()
	{
		try { logIn("(", this, ")");

		while (!zákazníci.isEmpty())
		{
			Zákazník zákazník = zákazníci.lastElement();
			zákazník.vyraďZLinky(false);
			zákazníci.remove(zákazník); // (pre istotu)
		}

		} finally { logOut("zákazníci.size(): ", zákazníci.size()); }
	}


	// Implementácia rozhrania Činnosť extends Comparable…

	public long čas()
	{
		return (long)(čas * 10_000);
	}

	public int compareTo(Činnosť iná)
	{
		if (iná instanceof Zákazník) return -1;
		if (čas() == iná.čas())
		{
			if (iná instanceof Linka)
				return hladina - ((Linka)iná).hladina;
		}
		return (int)(čas() - iná.čas());
	}
}
