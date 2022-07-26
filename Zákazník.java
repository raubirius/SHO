
import java.awt.Font;
import java.util.Vector;
import knižnica.*;
import static knižnica.Svet.*;
import static java.lang.Math.*;

import static debug.Debug.*;

public class Zákazník extends GRobot implements Činnosť
{
	// Blok ladiacich informácií:
	private static int counter = 0; private int idnum = counter++;
	@Override public String toString() { return "Zákazník_" + idnum +
	"; čas: " + čas + "; odchádza: " + odchádza + "; interval: " + interval +
	"; vLinke: " + vLinke; }


	// Evidencia:
	private final static Vector<Zákazník> zákazníci = new Vector<>();


	// Globálna konfigurácia zákazníkov:
	public static double faktorZrýchlenia = 5.0;
	public static double faktorMaximálnejRýchlosti = 100.0;


	// Vnútorné stavy:
	private Linka vLinke = null;
	private boolean odchádza = false;
	private double interval = 0.0;
	private double čas = 0.0;

	private String meno = null;

	// Priebeh vybavovania:
	private final CestaZákazníka cesta = new CestaZákazníka();


	// Konštruktor musí byť súkromný, aby sa dali recyklovať neaktívni
	// zákazníci (poznámka: funkciu getInstance plní metóda nový):
	private Zákazník(Linka vLinke)
	{
		// Registrácia zákazníkov:
		zákazníci.add(this);
		Systém.činnosti.add(this);

		// Reset – inicializácia:
		reset(vLinke);

		vrstva(2);
		farba(červená);
		hrúbkaČiary(2);
		zdvihniPero();
	}

	// Reset – inicializácia:
	public void reset(Linka vLinke)
	{
		if (null == vLinke)
		{
			vyraďZLinky(false);
			domov();
		}
		else
		{
			if (vLinke.evidujZákazníka(this))
				priraďKLinke(vLinke, false);
			skočNa(vLinke);
		}

		if (null == písmoZákazníkov)
			písmoZákazníkov = hlavnýRobot().písmo().deriveFont(14.0f);

		písmo(písmoZákazníkov);
		interval = 0.0;
		čas = Systém.čas;

		zrušCieľ();
		maximálnaRýchlosť(faktorMaximálnejRýchlosti * Systém.dilatácia);
		zrýchlenie(0, false);
		rýchlosť(0, false);

		meno = null;
		odchádza = false;
		aktivuj(false);

		cesta.clear();
		cesta.add(new BodCesty(this.vLinke));
	}


	private static Font písmoZákazníkov = null;

	// Veľkosť písma (pre istotu, keby bolo treba dobudúcna):
	/*
	private float veľkosťPísma = 16.0f;

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
	*/


	// Rôzne aktivity:

	public void upravCieľPodľaLinky() { upravCieľPodľaLinky(false); }
	public void upravCieľPodľaLinky(boolean počiatočný)
	{
		if (null != vLinke)
		{
			if (vLinke.jeDopravník())
			{
				Bod poloha = vLinke.poloha();

				if (0 != interval)
				{
					double percento = max(0,
						(čas - Systém.čas) / interval);

					vLinke.preskočVľavo(vLinke.šírka() *
						percento - (vLinke.šírka() / 2));
				}

				if (smerujeDoCieľa() || počiatočný)
					upravCieľ(vLinke, false);
				else
					skočNa(vLinke);

				vLinke.poloha(poloha);
			}
			else if (vLinke.jeZásobník() || vLinke.jeČakáreň() ||
				vLinke.jeZastávka())
			{
				Bod poloha = poloha();
				double uhol = uhol();

				poloha(vLinke);
				uhol(vLinke);
				double š = (vLinke.šírka() / 2) - veľkosť() - 5;
				double v = (vLinke.výška() / 2) - veľkosť() - 5;
				preskoč(náhodnéReálneČíslo(-š, š), náhodnéReálneČíslo(-v, v));
				Bod cieľ = poloha();

				uhol(uhol);
				poloha(poloha);

				upravCieľ(cieľ, false);
			}
			else
			{
				upravCieľ(vLinke, false);
			}
		}
	}

	public String meno() { return meno; }
	public Zákazník pomenuj(String meno)
	{
		cesta.meno = this.meno = meno;
		return this;
	}


	// Statická časť (zväčša súvisiaca s evidenciou):

	public static Zákazník nový(Linka vLinke)
	{
		int n = zákazníci.size();
		for (int i = 0; i < n; ++i)
		{
			Zákazník zákazník = zákazníci.get(i);
			if (zákazník.neaktívny())
			{
				zákazník.reset(vLinke);
				return zákazník;
			}
		}

		Zákazník zákazník = new Zákazník(vLinke);
		return zákazník;
	}

	public static int počet()
	{
		return zákazníci.size();
	}

	public static Zákazník daj(int i)
	{
		return zákazníci.get(i);
	}

	public static Zákazník[] daj()
	{
		Zákazník[] zákazníci = new Zákazník[Zákazník.zákazníci.size()];
		return Zákazník.zákazníci.toArray(zákazníci);
	}

	public static Zákazník[] dajAktívnych()
	{
		int počet = zákazníci.size();
		Vector<Zákazník> aktívni = new Vector<>();

		for (int i = 0; i < počet; ++i)
		{
			Zákazník zákazník = zákazníci.get(i);
			if (zákazník.aktívny()) aktívni.add(zákazník);
		}

		Zákazník[] pole = new Zákazník[aktívni.size()];
		pole = aktívni.toArray(pole);

		aktívni.clear();
		aktívni = null;

		return pole;
	}

	public static boolean žiadnyAktívny()
	{
		int počet = zákazníci.size();
		for (int i = 0; i < počet; ++i)
			if (zákazníci.get(i).aktívny()) return false;
		return true;
	}

	public static void vyčisti()
	{
		for (Zákazník zákazník : zákazníci)
			if (zákazník.aktívny())
			{
				zákazník.vyraďZLinky(false);
				zákazník.deaktivuj();
			}
	}


	// Obsluha udalostí:

	@Override public void aktivácia()
	{
		zobraz();
	}

	@Override public void deaktivácia()
	{
		zrýchlenie(0, false);
		rýchlosť(0, false);

		náhodnýSmer();
		skry();
	}

	@Override public void dosiahnutieCieľa()
	{
		zrýchlenie(0, false);
		rýchlosť(0, false);
	}


	@Override public void kresliTvar()
	{
		if (null != vLinke) switch (vLinke.účel())
		{
		case EMITOR: farba(svetlomodrá); break;
		case ZÁSOBNÍK: farba(svetlofialová); break;
		case ČAKÁREŇ: farba(svetlotyrkysová); break;
		case ZASTÁVKA: farba(svetloatramentová); break;
		case DOPRAVNÍK: farba(svetlohnedá); break;
		case MENIČ: farba(svetlooranžová); break;
		case UVOĽŇOVAČ: farba(svetlozelená); break;
		}

		if (0 != interval)
		{
			skoč(veľkosť() + 10);
			vyplňObdĺžnik((šírka() / 1.8) * max(0,
				(čas - Systém.čas) / interval), 6);
			odskoč(veľkosť() + 10);
		}

		kruh();
		farba(čierna);
		kružnica();

		if (null != meno && !meno.isEmpty())
		{
			skoč(0, veľkosť() + výškaRiadka());
			text(meno, KRESLI_NA_STRED);
		}
	}


	@Override public boolean činnosť()
	{
		Boolean retval = null; try { debugIn("(", this, ")");

		// (Iba animačno-grafická záležitosť. Nemá vplyv na simuláciu:
		if (null != vLinke && vLinke.jeDopravník()) upravCieľPodľaLinky();

		if (čas < Systém.čas)
		{
			if (odchádza)
			{
				náhodnýSmer();
				skoč();
			}
			else if (null != vLinke)
			{
				// Čakanie v zásobníku je (na rozdiel od čakárne) časovo
				// neobmedzené:
				if (vLinke.jeZásobník()) return retval = false;
					// (Preto je kľúčové, aby zásobník neustále sledoval,
					// či sa neuvoľnila nejaká linka pre toho čakajúceho
					// zákazníka, ktorý je na rade s obsluhou (aby ho tam
					// mohol poslať), inak by zákazníci v zásobníku čakali
					// donekonečna. To sa deje v triede Linka.)

				if (smerujeDoCieľa()) skočNaCieľ();

				Boolean failed = vLinke.dajLinku(this, true);
				if (null != failed) return retval = failed;

				if (null != vLinke)
				{
					if (vLinke.jeUvoľňovač()) uvoľniMa();
					else if (!vLinke.jeZastávka()) odíď();
				}
				else odíď();
			}
			else
			{
				odíď();
			}
		}
		return retval = false;

		} finally { debugOut("Zákazník.činnosť: ", retval); }
	}


	// Simulácia (úprava stavov):

	public void pridajInterval(double interval)
	{
		try { debugIn(interval, " (", this, ")");

		this.čas += (this.interval = interval);

		} finally { debugOut("this.čas: ", this.čas); }
	}

	public void nastavInterval(double interval)
	{
		try { debugIn(interval, " (", this, ")");

		this.čas = Systém.čas + (this.interval = interval);

		} finally { debugOut("this.čas: ", this.čas); }
	}

	private void odíď()
	{
		try { debugIn("(", this, ")");

		++Systém.odídených;
		if (null != vLinke) ++vLinke.odídených;
		if (null != meno)
		{
			String popis = null != vLinke ? vLinke.popis() : null;
			if (null != popis)
				Systém.zoznamOdídených.add(meno + "\t" + popis);
			else
				Systém.zoznamOdídených.add(meno);
		}

		vyraďZLinky(true);

		zrýchlenie(0, false);
		rýchlosť(0, false);

		trvanieAktivity(50);
		odchádza = true;
		cesta.bolSpokojný = false;

		náhodnýSmer();
		skoč();

		} finally { debugOut(); }
	}

	private void uvoľniMa()
	{
		try { debugIn("(", this, ")");

		++Systém.vybavených;
		if (null != vLinke)
		{
			++vLinke.vybavených;
			if (null != meno)
				vLinke.pridajMeno(meno);
		}

		vyraďZLinky(true);
		deaktivuj();

		} finally { debugOut(); }
	}

	// ‼Pozor‼ Linka.evidujZákazníka(Zákazník) treba vykonať zvlášť‼
	// (Jednak to má návratovú hodnotu boolean, ktorú treba zachytávať
	// a jednak je táto metóda použitá viackrát v tejto triede v takých
	// situáciách, kedy volanie „evidujZákazníka“ nie je vyhovujúce.)
	public void priraďKLinke(Linka vLinke, boolean evidujBodCesty)
	{
		try { debugIn(vLinke, " (", this, ")");

		vyraďZLinky(false);
		this.vLinke = vLinke;
		if (evidujBodCesty) cesta.add(new BodCesty(this.vLinke));

		} finally { debugOut("this.vLinke: ", this.vLinke); }
	}

	public void vyraďZLinky(boolean evidujBodCesty)
	{
		try { debugIn("(", this, ")");

		if (null != vLinke)
		{
			vLinke.odoberZákazníka(this);
			vLinke = null;
		}
		if (evidujBodCesty) cesta.add(new BodCesty(this.vLinke));

		} finally { debugOut("this.vLinke: ", this.vLinke); }
	}


	// Implementácia rozhrania Činnosť extends Comparable…

	public long čas()
	{
		return (long)(čas * 10_000);
	}

	public int compareTo(Činnosť iná)
	{
		if (iná instanceof Linka) return 1;
		return (int)(čas() - iná.čas());
	}
}
