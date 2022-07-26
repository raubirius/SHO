
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
	private final KontextováPoložka položkaZmeňNaEmitor;
	private final KontextováPoložka položkaZmeňNaZásobník;
	private final KontextováPoložka položkaZmeňNaČakáreň;
	private final KontextováPoložka položkaZmeňNaDopravník;
	private final KontextováPoložka položkaZmeňNaMenič;
	private final KontextováPoložka položkaZmeňNaUvoľňovač;
	private final KontextováPoložka položkaUpravKoeficienty;
	private final KontextováPoložka položkaUpravVizuály;
	private final KontextováPoložka položkaVymaž;
	private final KontextováPoložka položkaZmeňNaElipsu;
	private final KontextováPoložka položkaZmeňNaObdĺžnik;
	private final KontextováPoložka položkaZmeňNaOblýObdĺžnik;


	// Vnútorné stavy a vizualizácia:
	private boolean ťaháSa = false;
	private static Linka upravujeSa = null; // Toto riešenie bolo
		// implementované preto, aby sa v prípade úprav jednej linky ťahaním
		// neposúvali ostatné linky.
	private static int typÚprav = -1;
	private boolean označená = false;
	private double čas = 0.0;

	private String id = null; // Toto id je len pomocná informácia používaná
		// pri zápise do a čítaní liniek zo súboru. Nemá žiadne iné použitie
		// a žiadny iný význam. Pôvodný účel bolo umožniť zápis a čítanie
		// spojníc, ale je možné, že sa časom tento účel rozšíril.

	private String popis = null;
	private boolean popisPod = false;
	private ÚčelLinky účel = null;
	private double časovač = 1.0;
	private double rozptyl = 0.0;
	private int kapacita = 10;
	private TvarLinky tvar = null;
	private double mieraZaoblenia = 0.50;


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
		položkaUpravPopis = kontextováPonuka.pridajPoložku("Upraviť popis…");

		položkaZmeňNaEmitor = new KontextováPoložka("Emitor");
		položkaZmeňNaZásobník = new KontextováPoložka("Zásobník");
		položkaZmeňNaČakáreň = new KontextováPoložka("Čakáreň");
		položkaZmeňNaDopravník = new KontextováPoložka("Dopravník");
		položkaZmeňNaMenič = new KontextováPoložka("Menič");
		položkaZmeňNaUvoľňovač = new KontextováPoložka("Uvoľňovač");

		kontextováPonuka.pridajPonuku("Funkcia (účel)", položkaZmeňNaEmitor,
			položkaZmeňNaZásobník, položkaZmeňNaČakáreň, položkaZmeňNaDopravník,
			položkaZmeňNaMenič, položkaZmeňNaUvoľňovač);

		položkaZmeňNaElipsu = new KontextováPoložka("Elipsa");
		položkaZmeňNaObdĺžnik = new KontextováPoložka("Obdĺžnik");
		položkaZmeňNaOblýObdĺžnik = new KontextováPoložka("Oblý obdĺžnik");

		kontextováPonuka.pridajPonuku("Tvar", položkaZmeňNaElipsu,
			položkaZmeňNaObdĺžnik, položkaZmeňNaOblýObdĺžnik);

		položkaUpravKoeficienty = kontextováPonuka.pridajPoložku(
			"Upraviť koeficienty");
		položkaUpravVizuály = kontextováPonuka.pridajPoložku(
			"Upraviť vizuálne vlastnosti");

		kontextováPonuka.pridajOddeľovač();

		položkaVymaž = kontextováPonuka.pridajPoložku("Vymazať");


		// Reset – inicializácia:
		reset(popis);
		nekresliTvary();
		vrstva(1);
	}

	// Svisiace s konštrukciou (resp. inicializáciou) a aktualizáciou rozhrania:

	private void aktualizujKontextovúPonuku()
	{
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
		tvar = null;
		aktivuj(false);
		zrušSpojnice();
		vyraďZákazníkov();
		aktualizujKontextovúPonuku();
	}

	public void kopíruj(Linka iná)
	{
		// Všetko, čo sa dá treba nastavovať cez metódy‼
		skočNa(iná);
		pomer(iná.pomer());
		veľkosť(iná.veľkosť());
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


	// Úprava vlastností (individuálne aj hromadne):

	private final static String[] popisyÚpravyPopisu = new String[]
		{"Upravte popis linky:", "Umiestniť popis pod linku"};

	public void upravPopis()
	{
		Object[] údaje = {popis, popisPod};

		if (Svet.dialóg(popisyÚpravyPopisu, údaje, "Popis linky"))
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

			if (Svet.dialóg(popisyÚpravyPopisuOznačených, údaje,
				"Popis liniek"))
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

	/* TODO del old way
	public void upravPopis()
	{
		String novýPopis = upravReťazec(popis,
			"Upravte popis linky:", "Popis linky");
		if (null != novýPopis) popis(novýPopis);
	}

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
			String novýPopis = zadajReťazec(
				"Upravte spoločný popis označených liniek:",
				"Popis liniek");

			if (null != novýPopis)
			{
				int n = linky.size();
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená())
						linka.popis(novýPopis);
				}
			}
		}
	}*/


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
		zaoblenie(veľkosť() * mieraZaoblenia);
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


	private final static String[][] popisyKoeficientov = new String[][]
		{
			{"Časovač:", "Rozptyl:"},
				// Časovač je intervalom produkcie zákazníkov pre emitor,
				// trpezlivosťou zákazníkov pre čakáreň, časom dopravy pre
				// dopravník, časom spracovania pre iné linky, pričom je
				// irelevantný pre zásobníky.
			{"Kapacita:"}
		};

	public void upravKoeficienty()
	{
		if (ÚčelLinky.ZÁSOBNÍK == účel)
		{
			Object[] údaje = {new Double(kapacita)};

			if (Svet.dialóg(popisyKoeficientov[1], údaje, "Vlastnosti linky"))
				kapacita = ((Double)údaje[0]).intValue();
		}
		else
		{
			Object[] údaje = {časovač, rozptyl};

			if (Svet.dialóg(popisyKoeficientov[0], údaje, "Vlastnosti linky"))
			{
				časovač = (Double)údaje[0];
				rozptyl = (Double)údaje[1];
			}
		}
	}

	private final static String[][] popisyKoeficientovOznačených =
		new String[][]
		{
			{"Časovače označených liniek:",
				"Rozptyly označených liniek:"},
			{"Kapacity označených liniek:"},
			{"Časovače označených liniek (okrem zásobníkov):",
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
					if (ÚčelLinky.ZÁSOBNÍK == linka.účel)
						typ |= 2; else typ |= 1;

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

			if (Svet.dialóg(popisyKoeficientovOznačených[typ], údaje,
				"Vlastnosti liniek")) switch (typ)
			{
			case 0:
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená())
					{
						linka.časovač = (Double)údaje[0];
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
						linka.časovač = (Double)údaje[0];
						linka.rozptyl = (Double)údaje[1];
						linka.kapacita = ((Double)údaje[2]).intValue();
					}
				}
			}
		}
	}


	private final static String[] popisyVizuálov = new String[]
		{"Pomer šírky k výške:", "Veľkosť (výška):",
		"Miera zaoblenia rohov (len pre oblý obdĺžnik):",
		"Uhol (pootočenie; 90° – zvislá poloha):"};

	public void upravVizuály()
	{
		Object[] údaje = {pomer(), veľkosť(), mieraZaoblenia, uhol()};

		if (Svet.dialóg(popisyVizuálov, údaje, "Vizuálne vlastnosti linky"))
		{
			pomer((Double)údaje[0]);
			veľkosť((Double)údaje[1]);
			mieraZaoblenia((Double)údaje[2]);
			uhol((Double)údaje[3]);
			aktualizujSpojnice();
		}
	}

	private final static String[] popisyVizuálovOznačených = new String[]
		{"Pomery šírok k výškam:", "Veľkosti (výšky):",
		"Miery zaoblenia rohov (len pre oblé obdĺžniky):",
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
			double[] priemery = {0.0, 0.0, 0.0, 0.0};
			for (int i = 0; i < n; ++i)
			{
				Linka linka = linky.get(i);
				if (linka.aktívny() && linka.označená())
				{
					priemery[0] += linka.pomer();
					priemery[1] += linka.veľkosť();
					priemery[2] += linka.mieraZaoblenia();
					priemery[3] += linka.uhol();
					++nn;
				}
			}

			priemery[0] /= nn; // pomer
			priemery[1] /= nn; // veľkosť
			priemery[2] /= nn; // miera zaoblenia
			priemery[3] /= nn; // uhol

			Object[] údaje = new Object[] {priemery[0], priemery[1],
				priemery[2], priemery[3]};

			if (Svet.dialóg(popisyVizuálovOznačených, údaje,
				"Vizuálne vlastnosti označených liniek"))
			{
				for (int i = 0; i < n; ++i)
				{
					Linka linka = linky.get(i);
					if (linka.aktívny() && linka.označená())
					{
						linka.pomer((Double)údaje[0]);
						linka.veľkosť((Double)údaje[1]);
						linka.mieraZaoblenia((Double)údaje[2]);
						linka.uhol((Double)údaje[3]);
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

	public void popis(String popis)
	{
		this.popis = null == popis || popis.isEmpty() ? null : popis;
		kontextováPonuka.popis(null == this.popis ?
			"<html><i>«nepomenovaná linka»</i></html>" : this.popis);
		žiadajPrekreslenie();
	}


	public double mieraZaoblenia()
	{
		return mieraZaoblenia;
	}

	public void mieraZaoblenia(double mieraZaoblenia)
	{
		this.mieraZaoblenia = mieraZaoblenia;
		zaoblenie(TvarLinky.OBLÝ_OBDĹŽNIK == this.tvar ?
			(veľkosť() * mieraZaoblenia) : 0.0);
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
			súbor.zapíšVlastnosť("mieraZaoblenia", mieraZaoblenia);
			súbor.zapíšVlastnosť("uhol", uhol());
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
				pomer(pomer);
			}

			{
				Double veľkosť = súbor.čítajVlastnosť("veľkosť", (Double)50.0);
				veľkosť = null == veľkosť ? 50.0 : veľkosť;
				veľkosť(veľkosť);
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

			čas = Systém.čas;
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


	// TODO vymaž tento debugovací úsek
	// 
	// /*packagePrivate*/ static double rotovanéXRad(
	// 	double x, double y, double α)
	// {
	// 	if (0 == α) return x;
	// 	return (x * cos(α)) - (y * sin(α));
	// }
	// 
	// /*packagePrivate*/ static double rotovanéYRad(
	// 	double x, double y, double α)
	// {
	// 	if (0 == α) return y;
	// 	return (x * sin(α)) + (y * cos(α));
	// }
	// 
	// public boolean myšVoŠtvorci2(double polomer)
	// {
	// 	boolean jeV; double x1, y1;
	// 	if (uhol() % 90.0 == 0)
	// 	{
	// 		x1 = ÚdajeUdalostí.polohaMyšiX() - polohaX();
	// 		y1 = ÚdajeUdalostí.polohaMyšiY() - polohaY();
	// 		jeV = x1 >= -polomer && x1 <= polomer &&
	// 			y1 >= -polomer && y1 <= polomer;
	// 	}
	// 	else
	// 	{
	// 		// x₀ = xm − x
	// 		// y₀ = ym − y
	// 		double x0 = ÚdajeUdalostí.polohaMyšiX() - polohaX();
	// 		double y0 = ÚdajeUdalostí.polohaMyšiY() - polohaY();
	// 
	// 		// x₁ = x₀ . cos(α) − y₀ . sin(α)
	// 		// y₁ = x₀ . sin(α) + y₀ . cos(α)
	// 		double α = toRadians(90 - uhol());
	// 		x1 = rotovanéXRad(x0, y0, α);
	// 		y1 = rotovanéYRad(x0, y0, α);
	// 
	// 		// x₁ > -polomer && y₁ > -polomer && x₁ < polomer && y₁ < polomer
	// 		jeV = x1 >= -polomer && x1 <= polomer &&
	// 			y1 >= -polomer && y1 <= polomer;
	// 	}
	// 
	// 	/*if (jeV && 0 != zaoblenieX && 0 != zaoblenieY)
	// 	{
	// 		double zx = abs(zaoblenieX) / 2;
	// 		double zy = abs(zaoblenieY) / 2;
	// 		double Δzx = polomer - zx, Δzy = polomer - zy;
	// 
	// 		if (x1 > Δzx)
	// 		{
	// 			if (y1 > Δzy)
	// 			{
	// 				x1 -= Δzx; y1 -= Δzy;
	// 				x1 /= zx; y1 /= zy;
	// 				return (x1 * x1 + y1 * y1) <= 1;
	// 			}
	// 			else if (y1 < -Δzy)
	// 			{
	// 				x1 -= Δzx; y1 += Δzy;
	// 				x1 /= zx; y1 /= zy;
	// 				return (x1 * x1 + y1 * y1) <= 1;
	// 			}
	// 		}
	// 		else if (x1 < -Δzx)
	// 		{
	// 			if (y1 > Δzy)
	// 			{
	// 				x1 += Δzx; y1 -= Δzy;
	// 				x1 /= zx; y1 /= zy;
	// 				return (x1 * x1 + y1 * y1) <= 1;
	// 			}
	// 			else if (y1 < -Δzy)
	// 			{
	// 				x1 += Δzx; y1 += Δzy;
	// 				x1 /= zx; y1 /= zy;
	// 				return (x1 * x1 + y1 * y1) <= 1;
	// 			}
	// 		}
	// 	}*/
	// 
	// 	return jeV;
	// }
	// 
	// boolean príznak1 = false;
	// boolean príznak2 = false;
	// @Override public void pohybMyši()
	// {
	// 	príznak1 = príznak2 = false;
	// 	Poloha p = poloha();
	// 	double zx = zaoblenieX();
	// 	double zy = zaoblenieY();
	// 	zaoblenieX(0);
	// 	zaoblenieY(0);
	// 	// preskočVpravo(šírka() / 2);
	// 
	// 	príznak1 = myšVoŠtvorci(50);
	// 	príznak2 = myšVoŠtvorci2(50);
	// 	// príznak1 = myšVKruhu(8);
	// 	// príznak1 = myšVObdĺžniku(8, 8);
	// 
	// 	// strop.prepíšBod(this, červená);
	// 
	// 	/*Farba f = farba();
	// 	náhodnáPoloha();
	// 	if (myšVKruhu(50)) farba(červená); else farba(snehová);
	// 	kruh(5);
	// 	farba(f);*/
	// 
	// 	// odskoč(výška() / 2);
	// 	// boolean točiť = myšVKruhu(8);
	// 	poloha(p);
	// 	zaoblenieX(zx);
	// 	zaoblenieY(zy);
	// }


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
			odskoč(výška() / 2);
			boolean točiť = myšVKruhu(8);
			preskočVľavo(šírka() / 2);
			boolean meniťVýšku = myšVoŠtvorci(8);

			zaoblenieX(zx);
			zaoblenieY(zy);
			poloha(p);

			if (meniťVýšku)
			{
				upravujeSa = this;
				typÚprav = 2;
			}
			else if (meniťŠírku)
			{
				upravujeSa = this;
				typÚprav = 0;
			}
			else if (točiť)
			{
				upravujeSa = this;
				typÚprav = 1;
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
				case 0:
					{
						double vzdialenosť1 = vzdialenosťK(p1);
						double vzdialenosť2 = vzdialenosťK(p2);
						šírka(šírka() + 2 * (vzdialenosť2 - vzdialenosť1));
						aktualizujSpojnice();
					}
					break;

				case 1:
					{
						double uhol1 = smerNa(p1);
						double uhol2 = smerNa(p2);
						vľavo(uhol2 - uhol1);
					}
					break;

				case 2:
					{
						double vzdialenosť1 = vzdialenosťK(p1);
						double vzdialenosť2 = vzdialenosťK(p2);
						výška(výška() + 2 * (vzdialenosť2 - vzdialenosť1));
						aktualizujSpojnice();
					}
					break;
				}
			}
		}
		else if (ťaháSa)
		{
			Bod p = Bod.rozdiel(polohaMyši(), poslednáPolohaMyši());
			skoč(p.getX(), p.getY());
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
		else if (položkaVymaž.zvolená()) vymaž();
	}

	private void kresliZnačkyÚprav()
	{
		Farba f = farba();
		Poloha p = poloha();

		zaoblenie(0);
		preskočVpravo(šírka() / 2);
		if (this == upravujeSa && 0 == typÚprav)
			farba(svetločervená); else farba(snehová);
		vyplňŠtvorec(5); farba(f); kresliŠtvorec(5);

		// TODO vymaž tento debugovací úsek
		// 
		// if (príznak1) kresliŠtvorec(10);
		// if (príznak2) kresliŠtvorec(14);

		odskoč(výška() / 2);
		if (this == upravujeSa && 1 == typÚprav)
			farba(svetločervená); else farba(snehová);
		kruh(5); farba(f); kružnica(5);

		preskočVľavo(šírka() / 2);
		if (this == upravujeSa && 2 == typÚprav)
			farba(svetločervená); else farba(snehová);
		vyplňŠtvorec(5); farba(f); kresliŠtvorec(5);

		poloha(p);
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
			obdĺžnik();
			if (označená)
			{
				obdĺžnik(šírka() / 2 + 2, výška() / 2 + 2);
				kresliZnačkyÚprav();
			}
			else if (ťaháSa)
				obdĺžnik(šírka() / 2 - 2, výška() / 2 - 2);
		}
		else
		{
			elipsa();
			if (označená)
			{
				elipsa(šírka() / 2 + 2, výška() / 2 + 2);
				kresliZnačkyÚprav();
			}
			else if (ťaháSa)
				elipsa(šírka() / 2 - 2, výška() / 2 - 2);
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
			if (popisPod)
			{
				odskoč((výškaRiadka() + výška()) / 2);
				text(popis);
				skoč((výškaRiadka() + výška()) / 2);
			}
			else text(popis);
		}

		// TODO dať vypnuteľné
		{
			farba(čierna);
			skoč(10 + veľkosť() * pomer(), veľkosť() - výškaRiadka() / 2.0);

			if (ÚčelLinky.ZÁSOBNÍK == účel || ÚčelLinky.ČAKÁREŇ == účel)
			{
				text("Obsadenosť: " + zákazníci.size(), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else
			{
				int size = zákazníci.size();
				switch (size)
				{
				case 0: text("Voľný", KRESLI_PRIAMO); break;
				case 1: text("Obsadený", KRESLI_PRIAMO); break;
				default: text("Obsadenosť: " + size, KRESLI_PRIAMO);
				}
				skoč(0, -výškaRiadka());
			}

			if (ÚčelLinky.ZÁSOBNÍK == účel)
			{
				text("Kapacita: " + kapacita, KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else if (ÚčelLinky.ČAKÁREŇ == účel)
			{
				text("Kapacita: " + kapacita, KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());

				text("Trpezlivosť: " + F(časovač, 2) +
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2)),
					KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else if (ÚčelLinky.DOPRAVNÍK == účel)
			{
				text("Doprava: " + F(časovač, 2) +
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2)),
					KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else if (ÚčelLinky.EMITOR == účel)
			{
				text("Ďalší: " + F(čas, 3), KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());

				text("Interval: " + F(časovač, 2) +
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2)),
					KRESLI_PRIAMO);
				skoč(0, -výškaRiadka());
			}
			else
			{
				text("Spracovanie: " + F(časovač, 2) +
					(0.0 == rozptyl ? "" : " ± " + F(rozptyl, 2)),
					KRESLI_PRIAMO);
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
				// zákazníka neuvoľnila nejaká linka, aby ho tam mohol poslať,
				// pričom čakanie zákazníkov v zásobníku je časovo neobmedzené
				// a v čakárni majú zákazníci určitú mieru trpezlivosti čakania.

				Zákazník zákazník = zákazníci.firstElement();
				debug("prvýZákazník: ", zákazník);

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

							// TODO del:
							// zákazník.cieľ(linka, false);
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

		// Ak je zoznam zákazníkov prázdny (resp. v rámci kapacity),
		// tak je linka voľná. Emitory majú výnimku, tie majú kvázi
		// neobmedzenú kapacitu…

		if (null != účel) switch (účel)
		{
		case EMITOR: return retval = true;

		case ZÁSOBNÍK:
		case ČAKÁREŇ:
			return retval = zákazníci.size() < kapacita;

		case DOPRAVNÍK:
		case MENIČ:
		case UVOĽŇOVAČ: return retval = zákazníci.isEmpty();
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
		return (int)(čas() - iná.čas());
	}
}
