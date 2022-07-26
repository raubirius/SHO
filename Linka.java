
import java.io.IOException;
import java.util.TreeSet;
import java.util.Vector;
import knižnica.*;
import enumerácie.*;
import static knižnica.Svet.*;
import static knižnica.ÚdajeUdalostí.*;
import static java.lang.Math.*;

import static debug.Debug.*;

public class Linka extends GRobot implements Činnosť
{
	// Blok ladiacich informácií:
	private static int counter = 0; private int idnum = counter++;
	@Override public String toString() { return "Linka_" + idnum + "; čas: " +
	čas + "; účel: " + účel + "; zákazníci.size(): " + zákazníci.size(); }


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
	private final KontextováPoložka položkaZmeňNaDopravník;
	private final KontextováPoložka položkaZmeňNaMenič;
	private final KontextováPoložka položkaZmeňNaUvoľňovač;

	private final KontextováPoložka položkaZmeňNaElipsu;
	private final KontextováPoložka položkaZmeňNaObdĺžnik;
	private final KontextováPoložka položkaZmeňNaOblýObdĺžnik;

	private final KontextováPoložka položkaUpravKoeficienty;
	private final KontextováPoložka položkaUpravVizuály;
	private final KontextováPoložka položkaPrepniInformácie;
	// TODO del private final KontextováPoložka položkaPrepniObrys;
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
	private double čas = 0.0;
	private int hladina = -1;
		// TODO Zaraďovanie do hladín je dôležité pri triedení pred každým
		// cyklom času. Prvé musia byť zaradené tie linky, ktoré sú v nižšej
		// („skoršej“) hladine. Zaraďovanie do hladín prebieha vždy po vložení
		// alebo odstránení linky alebo spojnice a to tak, že najprv sú
		// hladiny všetkých liniek zresetované, potom sú vyhľadané a zaradené
		// do zoznamu prvej hladiny všetky linky, ktoré nemajú žiadneho
		// predchodcu (ak také jestvujú), do druhej hladiny sú zaradené všetky
		// linky, ktoré sú nasledovníkmi prvej hladiny a neboli ešte označené
		// a tak ďalej.
		// 
		// Otázka znie, či zoraďovať systémy od najnižšej hladiny po
		// najvyššiu, alebo naopak. Prikláňam sa k prvému variantu, pretože
		// tým vznikne vyššia šanca na zablokovanie prvého procesu druhým
		// a tým vznikne vyššia šanca na odhalenie slabých miest simulovaného
		// systému.

	private String id = null; // Toto id je len pomocná informácia používaná
		// pri zápise do a čítaní liniek zo súboru. Nemá žiadne iné použitie
		// a žiadny iný význam. Pôvodný účel bolo umožniť zápis a čítanie
		// spojníc, ale je možné, že sa časom tento účel rozšíril.

	private String popis = null;
	private String[] riadkyPopisu = null;
	private boolean popisPod = false;
	private ÚčelLinky účel = null;
	private double časovač = 1.0;
	private double rozptyl = 0.0;
	private int kapacita = 10;
	private TvarLinky tvar = null;
	private double mieraZaoblenia = 0.50;
	private int početČiarObrysu = 1;
	private double rozostupyČiarObrysu = 2.0;
	private boolean zobrazInformácie = true;


	// Evidencia zákazníkov:
	private final Vector<Zákazník> zákazníci = new Vector<>();


	// Konštruktor musí byť súkromný, aby sa dali recyklovať neaktívne linky
	// (poznámka: funkciu getInstance plní metóda pridaj):
	private Linka(String popis)
	{
		// Registrácia liniek:
		linky.add(this);
		Systém.činnosti.add(this);


		// Zostavenie kontextovej ponuky:
		kontextováPonuka = new KontextováPonuka("—");
		položkaUpravPopis = kontextováPonuka.pridajPoložku("Uprav popis…");

		položkaZrušÚčel = new KontextováPoložka("«žiadny»");
		položkaZmeňNaEmitor = new KontextováPoložka("Emitor");
		položkaZmeňNaZásobník = new KontextováPoložka("Zásobník");
		položkaZmeňNaČakáreň = new KontextováPoložka("Čakáreň");
		položkaZmeňNaDopravník = new KontextováPoložka("Dopravník");
		položkaZmeňNaMenič = new KontextováPoložka("Menič");
		položkaZmeňNaUvoľňovač = new KontextováPoložka("Uvoľňovač");

		kontextováPonuka.pridajPonuku("Funkcia (účel)", položkaZrušÚčel, null,
			položkaZmeňNaEmitor, položkaZmeňNaZásobník, položkaZmeňNaČakáreň,
			položkaZmeňNaDopravník, položkaZmeňNaMenič, položkaZmeňNaUvoľňovač);

		položkaZmeňNaElipsu = new KontextováPoložka("Elipsa");
		položkaZmeňNaObdĺžnik = new KontextováPoložka("Obdĺžnik");
		položkaZmeňNaOblýObdĺžnik = new KontextováPoložka("Oblý obdĺžnik");

		kontextováPonuka.pridajPonuku("Tvar", položkaZmeňNaElipsu,
			položkaZmeňNaObdĺžnik, položkaZmeňNaOblýObdĺžnik);

		položkaUpravKoeficienty = kontextováPonuka.pridajPoložku(
			"Uprav koeficienty");
		položkaUpravVizuály = kontextováPonuka.pridajPoložku(
			"Uprav vizuálne vlastnosti");
		položkaPrepniInformácie = kontextováPonuka.pridajPoložku(
			"Zobraz informácie");
		/* TODO del položkaPrepniObrys = kontextováPonuka.pridajPoložku(
			"Zobraz obrys"); */

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
		položkaZrušÚčel.ikona(null == účel ? Systém.ikonaOznačenia : null);

		položkaZmeňNaEmitor.ikona(
			ÚčelLinky.EMITOR == účel ? Systém.ikonaOznačenia : null);
		položkaZmeňNaZásobník.ikona(
			ÚčelLinky.ZÁSOBNÍK == účel ? Systém.ikonaOznačenia : null);
		položkaZmeňNaČakáreň.ikona(
			ÚčelLinky.ČAKÁREŇ == účel ? Systém.ikonaOznačenia : null);
		položkaZmeňNaDopravník.ikona(
			ÚčelLinky.DOPRAVNÍK == účel ? Systém.ikonaOznačenia : null);
		položkaZmeňNaMenič.ikona(
			ÚčelLinky.MENIČ == účel ? Systém.ikonaOznačenia : null);
		položkaZmeňNaUvoľňovač.ikona(
			ÚčelLinky.UVOĽŇOVAČ == účel ? Systém.ikonaOznačenia : null);

		položkaZmeňNaElipsu.ikona(
			TvarLinky.ELIPSA == tvar ? Systém.ikonaOznačenia : null);
		položkaZmeňNaObdĺžnik.ikona(
			TvarLinky.OBDĹŽNIK == tvar ? Systém.ikonaOznačenia : null);
		položkaZmeňNaOblýObdĺžnik.ikona(
			TvarLinky.OBLÝ_OBDĹŽNIK == tvar ? Systém.ikonaOznačenia : null);

		položkaPrepniInformácie.ikona(
			zobrazInformácie ? Systém.ikonaOznačenia : null);
		/*položkaPrepniObrys.ikona(
			zobrazObrys ? Systém.ikonaOznačenia : null); TODO del */
	}

	@Override public Spojnica spojnica(GRobot cieľ)
	{
		Spojnica spojnica = super.spojnica(cieľ, šedá);
		if (null == spojnica) return null;

		if (null != tvar && TvarLinky.ELIPSA != tvar)
			spojnica.orezanieZačiatku(obdĺžnik());
		else
			spojnica.orezanieZačiatku(elipsa());

		if (cieľ instanceof Linka)
		{
			Linka linka = (Linka)cieľ;
			if (null != linka.tvar && TvarLinky.ELIPSA != linka.tvar)
				spojnica.orezanieKonca(linka.obdĺžnik());
			else
				spojnica.orezanieKonca(linka.elipsa());
		}

		spojnica.definujZnačkuKonca(šípka);

		return spojnica;
	}

	private void aktualizujSpojnice()
	{
		Spojnica[] spojnice = spojniceZ();
		for (Spojnica spojnica : spojnice)
			spojnica(spojnica.cieľ());

		spojnice = spojniceDo();
		for (Spojnica spojnica : spojnice)
			spojnica.zdroj().spojnica(this);
	}

	private void reset(String popis)
	{
		farba(svetločervená); // (signalizácia chyby)
		hrúbkaČiary(2);
		pomer(1.2);
		veľkosť(50.0);
		zaoblenie(0.0);
		uhol(90.0);
		popis(popis);
		popisPod = false;
		čas = Systém.čas;
		účel = null;
		časovač = 1.0;
		rozptyl = 0.0;
		kapacita = 10;
		tvar = null;
		mieraZaoblenia = 0.5;
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
		popisPod = iná.popisPod;

		// Metóda účel nastavuje čas, vyraďuje zákazníkov a aktualizuje pouku.
		// To všetko sa robí nižšie…
		účel = iná.účel;

		časovač = iná.časovač;
		rozptyl = iná.rozptyl;
		kapacita = iná.kapacita;

		// Nastavovanie tvaru sa dá robiť cez sériu samostatných metód.
		// Každá robí takmer to isté s drobnými odchýlkami. Všetko je však
		// v zásade pokryté nižšie…
		tvar = iná.tvar;
		mieraZaoblenia(iná.mieraZaoblenia);
		zobrazInformácie = iná.zobrazInformácie;
		početČiarObrysu = iná.početČiarObrysu;
		rozostupyČiarObrysu = iná.rozostupyČiarObrysu;

		čas = iná.čas;

		// Kopíruj zdrojové spojnice inej linky
		{
			Spojnica[] spojnice = iná.spojniceZ();
			for (Spojnica spojnica : spojnice)
				spojnica(spojnica.cieľ());
		}

		// Kopíruj cieľové spojnice inej linky
		{
			Spojnica[] spojnice = iná.spojniceDo();
			for (Spojnica spojnica : spojnice)
				spojnica.zdroj().spojnica(this);
		}

		vyraďZákazníkov();
		aktualizujKontextovúPonuku();
		aktualizujSpojnice();
	}


	// Statická časť (zväčša súvisiaca s evidenciou):

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
			case DOPRAVNÍK:
			case MENIČ:
			case UVOĽŇOVAČ:
				if (null == popis && null == linka.popis) return linka;
				else if (null != popis && popis.equals(linka.popis))
					return linka;
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

	public static int početOznačených()
	{
		int početOznačených = 0, počet = linky.size();
		for (int i = 0; i < počet; ++i)
		{
			Linka linka = linky.get(i);
			if (linka.aktívny() && linka.označená())
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
			if (linka.aktívny() && linka.označená())
				označené.add(linka);
		}

		Linka[] pole = new Linka[označené.size()];
		pole = označené.toArray(pole);

		označené.clear();
		označené = null;

		return pole;
	}


	public static void vyčisti()
	{
		for (Linka linka : linky)
			if (linka.aktívny())
			{
				linka.odídených = linka.vybavených = 0;
				linka.čas = Systém.čas;
				if (linka.jeEmitor()) linka.čas += linka.interval();
			}
	}


	// Úprava vlastností (individuálne aj hromadne):

	private final static String[] popisyÚpravyPopisu = new String[]
		{"Upravte popis linky:", "Umiestniť popis pod linku"};

	public void upravPopis()
	{
		Object[] údaje = {null == popis ? "" : popis, popisPod};

		if (dialóg(popisyÚpravyPopisu, údaje, "Popis linky"))
		{
			popis((String)údaje[0]);
			popisPod = (Boolean)údaje[1];
		}
	}

	private final static String[] popisyÚpravyPopisuOznačených = new String[]
		{"Upravte spoločný popis označených liniek:",
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
				if (linka.aktívny() && linka.označená())
				{
					linka.upravPopis();
					break;
				}
		}
		else
		{
			TreeSet<String> zoznam = new TreeSet<>();
			int n = linky.size(), nn = 0;
			double priemerUmiestnenia = 0.0;
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená())
				{
					if (null != linka.popis)
						zoznam.add(linka.popis);
					priemerUmiestnenia += linka.popisPod ? 1.0 : 0.0;
					++nn;
				}
			}

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

			Object[] údaje = new Object[] {zlúčeniePopisov.toString(),
				priemerUmiestnenia > 0.5 ? true : false};

			if (dialóg(popisyÚpravyPopisuOznačených, údaje, "Popis liniek"))
			{
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená())
					{
						linka.popis((String)údaje[0]);
						linka.popisPod = (Boolean)údaje[1];
					}
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
					linka.zmeňNaČakáreň();
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
					linka.zmeňNaUvoľňovač();
			}
		}
	}


	public void zmeňNaElipsu()
	{
		zaoblenie(0);
		tvar = TvarLinky.ELIPSA;
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
					linka.zmeňNaElipsu();
			}
		}
	}


	public void zmeňNaObdĺžnik()
	{
		zaoblenie(0);
		tvar = TvarLinky.OBDĹŽNIK;
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
					linka.zmeňNaObdĺžnik();
			}
		}
	}


	public void zmeňNaOblýObdĺžnik()
	{
		zaoblenie(min(výška(), šírka()) * mieraZaoblenia);
		tvar = TvarLinky.OBLÝ_OBDĹŽNIK;
		aktualizujKontextovúPonuku();
		aktualizujSpojnice();
	}

	public static void zmeňNaObléObdĺžniky()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Zmena tvaru liniek na oblé obdĺžniky");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená())
				{
					linka.zmeňNaOblýObdĺžnik();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená())
					linka.zmeňNaOblýObdĺžnik();
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
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
				if (linka.aktívny() && linka.označená())
					linka.zobrazInformácie(true);
			}
		}
	}


	/* TODO del public static void prepniObrysOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Prepnutie obrysov pri označených linkách");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená())
				{
					linka.prepniZobrazenieObrysu();
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená())
					linka.prepniZobrazenieObrysu();
			}
		}
	}*/

	/* TODO del public static void skryObrysOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Prepnutie obrysov pri označených linkách");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená())
				{
					linka.zobrazObrys(false);
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená())
					linka.zobrazObrys(false);
			}
		}
	}*/

	/* TODO del public static void zobrazObrysOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Prepnutie obrysov pri označených linkách");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená())
				{
					linka.zobrazObrys(true);
					break;
				}
		}
		else
		{
			int n = linky.size();
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená())
					linka.zobrazObrys(true);
			}
		}
	}*/


	private final static String[][] popisyKoeficientov = new String[][]
		{
			// Časovač je intervalom produkcie zákazníkov pre emitor,
			// trpezlivosťou zákazníkov pre čakáreň, časom dopravy pre
			// dopravník, časom spracovania pre iné linky, pričom je
			// irelevantný pre zásobníky.
			{"Časovač:", "Rozptyl:"},
			{"Kapacita:"},
			{"Časovač:", "Rozptyl:", "Kapacita:"}
		};

	public void upravKoeficienty()
	{
		if (ÚčelLinky.ZÁSOBNÍK == účel)
		{
			Object[] údaje = {new Double(kapacita)};

			if (dialóg(popisyKoeficientov[1], údaje, "Vlastnosti linky"))
			{
				if (!Double.isNaN((Double)údaje[0]))
					kapacita = ((Double)údaje[0]).intValue();
			}
		}
		else if (null == účel || ÚčelLinky.ČAKÁREŇ == účel)
		{
			Object[] údaje = {časovač, rozptyl, new Double(kapacita)};

			if (dialóg(popisyKoeficientov[2], údaje, "Vlastnosti linky"))
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

			if (dialóg(popisyKoeficientov[0], údaje, "Vlastnosti linky"))
			{
				if (!Double.isNaN((Double)údaje[0]))
					časovač = (Double)údaje[0];
				if (!Double.isNaN((Double)údaje[1]))
					rozptyl = (Double)údaje[1];
			}
		}
	}

	private final static String[][] popisyKoeficientovOznačených =
		new String[][]
		{
			{"<html><i>Poznámka: Ak chcete niektorý spoločný parameter " +
				"ignorovať<br />(nenastaviť), údaj vymažte a nechajte " +
				"políčko prázdne.</i><br /> <br />Časovače označených " +
				"liniek:</html>", "Rozptyly označených liniek:"},
			{"Kapacity označených liniek:"},
			{"<html><i>Poznámka: Ak chcete niektorý spoločný parameter " +
				"ignorovať<br />(nenastaviť), údaj vymažte a nechajte " +
				"políčko prázdne.</i><br /> <br />Časovače označených " +
				"liniek (okrem zásobníkov):</html>",
				"Rozptyly označených liniek (okrem zásobníkov):",
				"Kapacity označených liniek (len pre zásobníky):"}
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
				if (linka.aktívny() && linka.označená())
				{
					linka.upravKoeficienty();
					break;
				}
		}
		else
		{
			int n = linky.size(), nn = 0, typ = 0;
			double[] priemery = {0.0, 0.0, 0.0};
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená())
				{
					if (ÚčelLinky.ZÁSOBNÍK == linka.účel) typ |= 2;
					else if (null == linka.účel ||
						ÚčelLinky.ČAKÁREŇ == linka.účel) typ |= 3;
					else typ |= 1;

					priemery[0] += linka.časovač;
					priemery[1] += linka.rozptyl;
					priemery[2] += linka.kapacita;
					++nn;
				}
			}

			priemery[0] /= nn; // časovač
			priemery[1] /= nn; // rozptyl
			priemery[2] /= nn; // kapacita
			priemery[2] = (int)priemery[2];

			if (--typ < 0) return;

			Object[] údaje;
			switch (typ)
			{
			case 0:
				// časovač a rozptyl
				údaje = new Object[] {priemery[0], priemery[1]};
				break;

			case 1:
				// kapacita
				údaje = new Object[] {priemery[2]};
				break;

			default:
				// časovač, rozptyl a kapacita
				údaje = new Object[] {priemery[0], priemery[1], priemery[2]};
			}

			if (dialóg(popisyKoeficientovOznačených[typ], údaje,
				"Vlastnosti liniek")) switch (typ)
			{
			case 0:
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená())
					{
						if (!Double.isNaN((Double)údaje[0]))
							linka.časovač = (Double)údaje[0];
						if (!Double.isNaN((Double)údaje[1]))
							linka.rozptyl = (Double)údaje[1];
					}
				}
				break;

			case 1:
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená())
					{
						if (!Double.isNaN((Double)údaje[0]))
							linka.kapacita = ((Double)údaje[0]).intValue();
					}
				}
				break;

			default:
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená())
					{
						if (!Double.isNaN((Double)údaje[0]))
							linka.časovač = (Double)údaje[0];
						if (!Double.isNaN((Double)údaje[1]))
							linka.rozptyl = (Double)údaje[1];
						if (!Double.isNaN((Double)údaje[2]))
							linka.kapacita = ((Double)údaje[2]).intValue();
					}
				}
			}
		}
	}


	private final static String[] popisyVizuálov = new String[]
		{"Pomer šírky k výške:", "Veľkosť (výška):",
		"Miera zaoblenia rohov (len pre oblý obdĺžnik):",
		"Počet čiar obrysu (0 – n):", "Rozostupy medzi čiarami obrysu:",
		"Uhol (pootočenie; 90° – zvislá poloha):"};

	public void upravVizuály()
	{
		Object[] údaje = {pomer(), veľkosť(), mieraZaoblenia,
			new Double(početČiarObrysu), rozostupyČiarObrysu, uhol()};

		if (dialóg(popisyVizuálov, údaje, "Vizuálne vlastnosti linky"))
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
		"Veľkosti (výšky):", "Miery zaoblenia rohov (len pre oblé obdĺžniky):",
		"Počty čiar obrysov (0 – n):",
		"Uhly (pootočenia; 90° – zvislá poloha):"};

	public static void upravVizuályOznačených()
	{
		int početOznačených = početOznačených();
		if (0 == početOznačených)
			varovanie("Nie sú označené žiadne linky.",
				"Úprava vizuálnych vlastností liniek");
		else if (1 == početOznačených)
		{
			for (Linka linka : linky)
				if (linka.aktívny() && linka.označená())
				{
					linka.upravVizuály();
					break;
				}
		}
		else
		{
			int n = linky.size(), nn = 0;
			double[] priemery = {0.0, 0.0, 0.0, 0.0, 0.0};
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená())
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
				"Vizuálne vlastnosti označených liniek"))
			{
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená())
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
			"Potvrdenie vymazania")) deaktivuj();
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
				if (linka.aktívny() && linka.označená())
				{
					linka.vymaž();
					break;
				}
		}
		else
		{
			if (ÁNO == otázka("Prajete si vymazať všetky označené linky?",
				"Potvrdenie vymazania"))
			{
				int n = linky.size();
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená())
						linka.deaktivuj();
				}
			}
		}
	}


	public String popis()
	{
		return popis;
	}

	public String skráťPopis(int dĺžka)
	{
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

			if (-1 != popis.indexOf("\\n"))
			{
				riadkyPopisu = popis.split("\\\\n");
				popis = this.popis.replace("\\n", " ");
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


	public double mieraZaoblenia()
	{
		return mieraZaoblenia;
	}

	private void aktualizujZaoblenie()
	{
		zaoblenie(TvarLinky.OBLÝ_OBDĹŽNIK == this.tvar ?
			(min(výška(), šírka()) * mieraZaoblenia) : 0.0);
	}

	public void mieraZaoblenia(double mieraZaoblenia)
	{
		if (mieraZaoblenia < 0) mieraZaoblenia = 0;
		this.mieraZaoblenia = mieraZaoblenia;
		zaoblenie(TvarLinky.OBLÝ_OBDĹŽNIK == this.tvar ?
			(min(výška(), šírka()) * mieraZaoblenia) : 0.0);
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


	/* TODO del public boolean zobrazObrys()
	{
		return zobrazObrys;
	}

	public void zobrazObrys(boolean zobrazObrys)
	{
		this.zobrazObrys = zobrazObrys;
		aktualizujKontextovúPonuku();
		žiadajPrekreslenie();
	}

	public void prepniZobrazenieObrysu()
	{
		zobrazObrys = !zobrazObrys;
		aktualizujKontextovúPonuku();
		žiadajPrekreslenie();
	}*/


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
			súbor.zapíšVlastnosť("popisPod", popisPod);
			súbor.zapíšVlastnosť("x", polohaX());
			súbor.zapíšVlastnosť("y", polohaY());
			súbor.zapíšVlastnosť("účel", účel);
			súbor.zapíšVlastnosť("časovač", časovač);
			súbor.zapíšVlastnosť("rozptyl", rozptyl);
			súbor.zapíšVlastnosť("kapacita", kapacita);

			súbor.zapíšVlastnosť("tvar", tvar);
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
			}

			{
				Integer hodnota = súbor.čítajVlastnosť("kapacita", (Integer)10);
				kapacita = null == hodnota ? 10 : hodnota;
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

			{
				String tvar = súbor.čítajVlastnosť("tvar", (String)null);
				if (null == tvar) this.tvar = null; else switch (tvar)
				{
				case "ELIPSA": this.tvar = TvarLinky.ELIPSA; break;
				case "OBDĹŽNIK": this.tvar = TvarLinky.OBDĹŽNIK; break;
				case "OBLÝ_OBDĹŽNIK": this.tvar = TvarLinky.OBLÝ_OBDĹŽNIK;
					break;
				default: this.tvar = null; break;
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

			čas = Systém.čas;
			if (jeEmitor()) čas += interval();
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
							spojnica(linka);
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
		if (null != tvar && TvarLinky.ELIPSA != tvar) return myšVObdĺžniku();
		return myšVElipse();
	}

	@Override public boolean bodV(Poloha bod)
	{
		if (neaktívny()) return false;
		if (null != tvar && TvarLinky.ELIPSA != tvar) return bodVObdĺžniku(bod);
		return bodVElipse(bod);
	}

	@Override public void klik()
	{
		if (tlačidloMyši(ĽAVÉ))
		{
			if (myš().isControlDown())
			{
				if (myšV()) prepniOznačenie();
			}
			else if (!myš().isAltDown() && !myš().isShiftDown())
			{
				označ(myšV());
			}
		}
		else if (tlačidloMyši(PRAVÉ))
		{
			if (myšV()) kontextováPonuka.zobraz();
		}
	}


	@Override public void stlačenieTlačidlaMyši()
	{
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
			else ťaháSa = myšV() || (aktívny() && označená);
		}
	}

	@Override public void ťahanieMyšou()
	{
		if (null != upravujeSa)
		{
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
		ťaháSa = false;
		upravujeSa = null;
	}

	@Override public void voľbaKontextovejPoložky()
	{
		if (položkaUpravPopis.zvolená()) upravPopis();
		else if (položkaZrušÚčel.zvolená()) zrušÚčel();
		else if (položkaZmeňNaEmitor.zvolená()) zmeňNaEmitor();
		else if (položkaZmeňNaZásobník.zvolená()) zmeňNaZásobník();
		else if (položkaZmeňNaČakáreň.zvolená()) zmeňNaČakáreň();
		else if (položkaZmeňNaDopravník.zvolená()) zmeňNaDopravník();
		else if (položkaZmeňNaMenič.zvolená()) zmeňNaMenič();
		else if (položkaZmeňNaUvoľňovač.zvolená()) zmeňNaUvoľňovač();
		else if (položkaZmeňNaElipsu.zvolená()) zmeňNaElipsu();
		else if (položkaZmeňNaObdĺžnik.zvolená()) zmeňNaObdĺžnik();
		else if (položkaZmeňNaOblýObdĺžnik.zvolená()) zmeňNaOblýObdĺžnik();
		else if (položkaUpravKoeficienty.zvolená()) upravKoeficienty();
		else if (položkaUpravVizuály.zvolená()) upravVizuály();
		else if (položkaPrepniInformácie.zvolená())
			prepniZobrazenieInformácií();
		/* TODO del else if (položkaPrepniObrys.zvolená())
			prepniZobrazenieObrysu(); */
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

		// TODO del
		// text("veľ: " + F(veľkosť(), 2) + ";  pom: " + F(pomer(), 2) + ";  š×v: " + F(šírka(), 2) + "×" + F(výška(), 2));
	}

	@Override public void kresliTvar()
	{
		/*
		šedá     – bez funkcie
		modrá    – generuje zákazníkov
		fialová  – zhromažďuje zákazníkov
		hnedá    – prepravuje zákazníkov medzi linkami
		oranžová – transformuje (mení, spracúva) zákazníkov
		zelená   – uvoľňuje zákazníkov (definitívne ich vybavuje)
		*/

		Farba farba = šedá;
		if (null != účel) switch (účel)
		{
		case EMITOR: farba = modrá; break;
		case ZÁSOBNÍK: farba = fialová; break;
		case ČAKÁREŇ: farba = tyrkysová; break;
		case DOPRAVNÍK: farba = hnedá; break;
		case MENIČ: farba = oranžová; break;
		case UVOĽŇOVAČ: farba = zelená; break;
		}

		farba(farba);

		if (null != tvar && TvarLinky.ELIPSA != tvar)
		{
			double š = šírka() / 2, v = výška() / 2;
			for (int i = 0; i < početČiarObrysu && š >= 0 && v >= 0;
				++i, š -= rozostupyČiarObrysu, v -= rozostupyČiarObrysu)
				obdĺžnik(š, v);

			hrúbkaČiary(0.5);
			if (označená)
			{
				obdĺžnik(šírka() / 2 + 2.5, výška() / 2 + 2.5);
				kresliZnačkyÚprav();
			}
			else if (ťaháSa)
				obdĺžnik(šírka() / 2 - 1.5, výška() / 2 - 1.5);
		}
		else
		{
			double š = šírka() / 2, v = výška() / 2;
			for (int i = 0; i < početČiarObrysu && š >= 0 && v >= 0;
				++i, š -= rozostupyČiarObrysu, v -= rozostupyČiarObrysu)
				elipsa(š, v);

			hrúbkaČiary(0.5);
			if (označená)
			{
				elipsa(šírka() / 2 + 2.5, výška() / 2 + 2.5);
				kresliZnačkyÚprav();
			}
			else if (ťaháSa)
				elipsa(šírka() / 2 - 1.5, výška() / 2 - 1.5);
		}

		if (0 != časovač)
		{
			skoč(veľkosť() + 10);
			zaoblenie(0);
			vyplňObdĺžnik((šírka() / 2.0) * max(0,
				(čas - Systém.čas) / časovač), 6);
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
					skoč(((riadkyPopisu.length - 1) * výškaRiadka()) / 2);
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

		if (zobrazInformácie)
		{
			farba(čierna);
			skoč(10 + veľkosť() * pomer(), veľkosť() - výškaRiadka() / 2.0);

			if (ÚčelLinky.ZÁSOBNÍK == účel || ÚčelLinky.ČAKÁREŇ == účel)
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
					text("Voľný", KRESLI_PRIAMO); break;
				case 1: text("Obsadený", KRESLI_PRIAMO); break;
				default: text(S("Obsadenosť: ", size), KRESLI_PRIAMO);
				}
				skoč(0, -výškaRiadka());
			}

			if (null == účel)
			{
				text(S("Kapacita: ", kapacita), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());

				text(S("Časovač: ", F(časovač, 2),
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2))),
					KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
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
			else if (ÚčelLinky.DOPRAVNÍK == účel)
			{
				text(S("Doprava: ", F(časovač, 2),
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2))),
					KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else if (ÚčelLinky.EMITOR == účel)
			{
				text(S("Ďalší: ", F(čas, 3)), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());

				text(S("Interval: ", F(časovač, 2),
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2))),
					KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else
			{
				text(S("Spracovanie: ", F(časovač, 2),
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2))),
					KRESLI_PRIAMO);
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
		}
	}

	@Override public void aktivácia()
	{
		zobraz();
	}

	@Override public void deaktivácia()
	{
		// Keď je linka vymazaná (deaktivovaná), tak z nej treba vyhodiť
		// aj všetkých zákazníkov (a zrušiť spojnice).
		zrušSpojnice();
		vyraďZákazníkov();
		skry();
	}


	@Override public boolean činnosť()
	{
		Boolean retval = null; try { debugIn("(", this, ")");

		if (null != účel) switch (účel)
		{
		case EMITOR:
			{
				if (čas < Systém.čas)
				{
					Zákazník zákazník = Zákazník.nový(this);
					debug("novýZákazník: ", zákazník);
					čas += interval();
					return retval = true;
				}
			}
			break;

		case ZÁSOBNÍK:
		case ČAKÁREŇ:
			if (!zákazníci.isEmpty())
			{
				// Zásobníky a ćakárne sledujú, či sa pre prvého čakajúceho
				// zákazníka neuvoľnila nejaká linka, aby sa tam mohol poslať,
				// pričom čakanie zákazníkov v zásobníku je časovo neobmedzené
				// a v čakárni majú zákazníci určitú mieru trpezlivosti čakania.

				Zákazník zákazník = zákazníci.firstElement(); // TODO Podľa
					// režimu sa berie prvý, posledný, náhodný… (???) TODO
				debug("prvýZákazník: ", zákazník);

				// TODO .dajLinku() – pozri podrobnosti v Zákazníkovi.
				Spojnica[] spojnice = spojniceZ();
				for (Spojnica spojnica : spojnice)
				{
					GRobot cieľ = spojnica.cieľ();
					if (cieľ instanceof Linka)
					{
						Linka linka = (Linka)cieľ;
						debug("cieľ: ", linka);
						if (linka.evidujZákazníka(zákazník))
						{
							zákazník.priraďKLinke(linka);
							zákazník.nastavInterval((linka.jeEmitor() ||
								linka.jeZásobník()) ? 0.0 : linka.interval());

							zákazník.upravCieľPodľaLinky(true);

							zákazník.maximálnaRýchlosť(
								Zákazník.faktorMaximálnejRýchlosti *
								Systém.dilatácia);
							zákazník.zrýchlenie(Zákazník.faktorZrýchlenia *
								Systém.dilatácia, false);
							zákazník.rýchlosť(0, false);

							return retval = zákazník.čas() < Systém.čas;
						}
					}
				}
			}
			break;
		}

		return retval = false;

		} finally { debugOut("Linka.činnosť: ", retval); }
	}


	// Simulácia…

	public boolean jeVoľná()
	{
		Boolean retval = null; try { debugIn("(", this, ")");

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
			return retval = zákazníci.size() < kapacita;

		case MENIČ:
		case UVOĽŇOVAČ:
			return retval = zákazníci.isEmpty();
		}

		return retval = false;

		} finally { debugOut("Linka.jeVoľná: ", retval); }
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
		try { debugIn(účel, " (", this, "); Systém.čas: ", Systém.čas);

		this.účel = účel;
		čas = Systém.čas;
		vyraďZákazníkov();
		aktualizujKontextovúPonuku();

		} finally { debugOut(); }
	}

	// ‼Pozor‼ Zákazník.priraďKLinke(Linka) treba vykonať zvlášť‼
	public boolean evidujZákazníka(Zákazník zákazník)
	{
		Boolean retval = null; try { debugIn(zákazník, " (", this, ")");

		if (jeVoľná())
		{
			zákazník.vyraďZLinky();
			zákazníci.add(zákazník);
			return retval = true;
		}
		return retval = false;

		} finally { debugOut("Linka.evidujZákazníka: ", retval); }
	}

	// ‼Pozor‼ Lepšie je použiť volanie Zákazník.vyraďZLinky(), ktoré volá
	// túto metódu pre aktuálnu linku zákazníka.
	public void odoberZákazníka(Zákazník zákazník)
	{
		try { debugIn(zákazník, "; zákazníci.indexOf(zákazník): ",
			zákazníci.indexOf(zákazník), " (", this, ")");

		zákazníci.remove(zákazník);

		} finally { debugOut("zákazníci.size(): ", zákazníci.size()); }
	}

	public void vyraďZákazníkov()
	{
		try { debugIn("(", this, ")");

		while (!zákazníci.isEmpty())
		{
			Zákazník zákazník = zákazníci.lastElement();
			zákazník.vyraďZLinky();
			zákazníci.remove(zákazník); // (pre istotu)
		}

		} finally { debugOut("zákazníci.size(): ", zákazníci.size()); }
	}


	// Implementácia rozhrania Činnosť extends Comparable…

	public long čas()
	{
		return (long)(čas * 10_000);
	}

	public int compareTo(Činnosť iná)
	{
		if (iná instanceof Zákazník) return -1;
		return (int)(čas() - iná.čas());
	}
}
