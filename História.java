
import java.util.Vector;

public class História
{
	private static enum Typ
	{
		POPIS, POLOHA, ÚČEL, ČASOVAČ, ROZPTYL, KAPACITA, TVAR, POMER, VEĽKOSŤ,
		ZAOBLENIE, NOVÝ, KÓPIA
	}

	private static class Akcia
	{
		Linka linka;
		Typ typ;
		Object údajSpäť = null;
		Object údajVpred = null;
		Akcia(Linka linka, Typ typ, Object údajSpäť, Object údajVpred)
		{
			this.linka = linka;
			this.typ = typ;
			this.údajSpäť = údajSpäť;
			this.údajVpred = údajVpred;
		}
	}

	@SuppressWarnings("serial")
	private static class Dávka extends Vector<Akcia>
	{
		boolean dokončená = false;
	}

	private static final Vector<Dávka> história = new Vector<>();

	private static int poloha = -1;


	public static void pridaj(Akcia akcia)
	{
		if (poloha < -1) poloha = -1;
		Dávka dávka;
		if (poloha < 0 || história.get(poloha).dokončená)
		{
			dávka = new Dávka();
			história.setSize(++poloha);
			história.add(dávka);
		}
		else dávka = história.get(poloha);
		dávka.add(akcia);
	}

	public static void dokonči()
	{
		if (poloha >= 0)
		{
			Dávka dávka = história.get(poloha);
			if (null != dávka && !dávka.dokončená)
			{
				// rezervované miesto na prípadné doplnenie akcií
				dávka.dokončená = true;
			}
		}
	}

	public static void popis(Linka linka, String nový)
	{
		pridaj(new Akcia(linka, Typ.POPIS, linka.popis(), nový));
	}
}
