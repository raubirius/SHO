
import java.util.Vector;

public class História
{
	public static class ÚdajeSpojnice
	{
		// TODO:
		// Vytváranie/rušenie spojníc potrebuje unikátne ID liniek. Pri
		// čítaní/zápise sa vždy generujú nové podľa aktuálneho počtu. Ich
		// „recyklovanie“ na účely histórie by bolo komplikované (a preto
		// potenciálne nespoľahlivé). Mechanizmus histórie potrebuje ID-čka
		// nielen dynamicky generovať (pri zabezpečení unikátnosti), ale aj
		// ukladať do údajov histórie a obnovovať ich podľa údajov histórie
		// (všetko pri zabezpečení unikátnosti).
	}

	public static class ÚdajeLinky
	{
		
	}


	private static enum Typ
	{
		// TODO: Pozor‼ Posunutie (zmena polohy) musí byť merané v relatívnych
		// jednotkách, lebo asynchrónne sa môže ľubovoľne posúvať pohľad…

		POPIS, VEĽKOSŤ_PÍSMA, POPIS_POD, POLOHA, ÚČEL, ČASOVAČ, ROZPTYL,
		POČIATOČNÝ_ČAS, LIMIT, KAPACITA, ZOZNAM/*_MIEN*/, CYKLICKÝ/*_ZOZNAM*/,
		/*REŽIM_*/VÝBER_ZÁKAZNÍKOV, /*REŽIM_*/VÝBER_LINIEK,
		/*REŽIM_*/KRESLENIE, /*NÁZOV_*/TVAR, TRANSFORMÁCIE/*_TVARU*/, POMER,
		VEĽKOSŤ, UHOL, ZAOBLENIE, /*POČET_*/ČIAR, ROZOSTUPY/*_ČIAR*/,
		/*ZOBRAZ_*/INFORMÁCIE, NOVÝ, KÓPIA, VYMAŽ,

		CIEĽ/*_SPOJNICE*/, VÁHA/*_SPOJNICE*/,

		DILATÁCIA/*_ČASU*/,
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
