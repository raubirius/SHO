
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
		maximálnaRýchlosť(faktorMaximálnejRýchlosti * Systém.dilatácia);
		zrýchlenie(0, false);
		rýchlosť(0, false);

		odchádza = false;
		aktivuj(false);
	}


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
			else if (vLinke.jeZásobník() || vLinke.jeČakáreň())
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
			Zákazník Zákazník = zákazníci.get(i);
			if (Zákazník.aktívny()) aktívni.add(Zákazník);
		}

		Zákazník[] pole = new Zákazník[aktívni.size()];
		pole = aktívni.toArray(pole);

		aktívni.clear();
		aktívni = null;

		return pole;
	}

	public static void vyčisti()
	{
		for (Zákazník zákazník : zákazníci)
			if (zákazník.aktívny())
			{
				zákazník.vyraďZLinky();
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

				// TODO: .dajLinku(), ktorá je voľná – čo sa určuje podľa
				// spojníc a podľa stanovených priorít (pravdepodobností).
				// 
				// Režimy:
				// 
				//  • postupné prehľadávanie (bude cyklické počítadlo, ktoré
				//    vždy určí, ktorou linkou sa začne hľadanie voľnej linky)
				//  • náhodné – vyvážené pravdepodobnosťami (každá spojnica
				//    bude mať hodnotu, ktorá určí váhu pravdepodobnosti, že
				//    bude vybraná – hľadá sa algoritmom, ktorý bude vždy
				//    vyraďovať použité spojnice zo zoznamu spojníc, kým tam
				//    nezostane len jedna, ktorá keď nebude voľná… smola;
				//    samozrejme, že sa vyberie prvá voľná v poradí)
				//  • podľa priorít (každé prehľadávanie sa vždy začne
				//    v rovnakom poradí, ktoré bude určené podľa priorít)
				// 

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

							upravCieľPodľaLinky(true);

							maximálnaRýchlosť(faktorMaximálnejRýchlosti *
								Systém.dilatácia);
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

		++Systém.odídených;
		if (null != vLinke) ++vLinke.odídených;

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

		++Systém.vybavených;
		if (null != vLinke)
			++vLinke.vybavených;

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
		if (iná instanceof Linka) return 1;
		return (int)(čas() - iná.čas());
	}
}
