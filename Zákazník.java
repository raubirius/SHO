
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
	public static double faktorZrýchlenia = 2.5;


	// Vnútorné stavy:
	private Linka vLinke = null;
	private boolean odchádza = false;
	private double interval = 0.0;
	private double čas = 0.0;


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
			vyraďZLinky();
			domov();
		}
		else
		{
			if (vLinke.evidujZákazníka(this))
				priraďKLinke(vLinke);
			skočNa(vLinke);
		}

		interval = 0.0;
		čas = Systém.čas;

		zrušCieľ();
		maximálnaRýchlosť(50.0 * Systém.dilatácia);
		zrýchlenie(0, false);
		rýchlosť(0, false);

		odchádza = false;
		aktivuj(false);
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
	}


	@Override public boolean činnosť()
	{
		Boolean retval = null; try { debugIn("(", this, ")");

		if (čas < Systém.čas)
		{
			if (odchádza)
			{
				náhodnýSmer();
				skoč();
			}
			else if (null != vLinke)
			{
				// Čakanie v zásobníku je časovo neobmedzené, preto musí
				// zásobník neustále sledovať, či sa neuvoľnila nejaká linka
				// pre prvého čakajúceho zákazníka, aby ho tam mohol poslať.

				if (vLinke.jeZásobník()) return retval = false;

				if (smerujeDoCieľa()) skočNaCieľ();

				Spojnica[] spojnice = vLinke.spojniceZ();
				for (Spojnica spojnica : spojnice)
				{
					GRobot cieľ = spojnica.cieľ();
					if (cieľ instanceof Linka)
					{
						Linka linka = (Linka)cieľ;
						debug("cieľ: ", linka);
						if (linka.evidujZákazníka(this))
						{
							priraďKLinke(linka);
							pridajInterval((linka.jeEmitor() ||
								linka.jeZásobník()) ? 0.0 :
								linka.interval());

							cieľ(linka, false);
							maximálnaRýchlosť(50.0 * Systém.dilatácia);
							zrýchlenie(faktorZrýchlenia * Systém.dilatácia,
								false);
							rýchlosť(0, false);

							if (linka.jeZásobník()) return retval = false;
							return retval = čas < Systém.čas;
						}
					}
				}

				if (null != vLinke && vLinke.jeUvoľňovač())
					uvoľniMa(); else odíď();
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

		vyraďZLinky();

		zrýchlenie(0, false);
		rýchlosť(0, false);

		trvanieAktivity(50);
		odchádza = true;

		náhodnýSmer();
		skoč();

		} finally { debugOut(); }
	}

	private void uvoľniMa()
	{
		try { debugIn("(", this, ")");

		vyraďZLinky();
		deaktivuj();

		} finally { debugOut(); }
	}

	// ‼Pozor‼ Linka.evidujZákazníka(Zákazník) treba vykonať zvlášť‼
	// (Jednak to má návratovú hodnotu boolean, ktorú treba zachytávať
	// a jednak je táto metóda použitá viackrát v tejto triede v takých
	// situáciách, kedy volanie „evidujZákazníka“ nie je vyhovujúce.)
	public void priraďKLinke(Linka vLinke)
	{
		try { debugIn(vLinke, " (", this, ")");

		vyraďZLinky();
		this.vLinke = vLinke;

		} finally { debugOut("this.vLinke: ", this.vLinke); }
	}

	public void vyraďZLinky()
	{
		try { debugIn("(", this, ")");

		if (null != vLinke)
		{
			vLinke.odoberZákazníka(this);
			vLinke = null;
		}

		} finally { debugOut("this.vLinke: ", this.vLinke); }
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
