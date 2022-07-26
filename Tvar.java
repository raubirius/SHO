
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

import java.io.IOException;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import knižnica.*;

public class Tvar
{
	private static AffineTransform[] transformácie = new AffineTransform[15];
	static
	{
		transformácie[0] = AffineTransform.getScaleInstance(-1, 1);
		transformácie[1] = AffineTransform.getScaleInstance(1, -1);
		transformácie[2] = AffineTransform.getScaleInstance(-1, -1);

		for (int i = 0; i < 3; ++i)
		{
			int j = -1 - i;
			transformácie[3 + i * 4] =
				AffineTransform.getQuadrantRotateInstance(j);
			transformácie[4 + i * 4] = AffineTransform.getScaleInstance(-1, 1);
			transformácie[4 + i * 4].quadrantRotate(j);
			transformácie[5 + i * 4] = AffineTransform.getScaleInstance(1, -1);
			transformácie[5 + i * 4].quadrantRotate(j);
			transformácie[6 + i * 4] = AffineTransform.getScaleInstance(-1, -1);
			transformácie[6 + i * 4].quadrantRotate(j);
		}
	}

	private static class Záznam
	{
		String cesta = null;
		int použitie = 0;
		Shape[] tvary = null;
		Obrázok obrázok = null;
	}

	private final static TreeMap<String, Záznam> tvary = new TreeMap<>();

	private Tvar()
	{/* nedá sa vytvárať inštancie; je to zbytočné; všetko je statické */}


	public static void vymaž()
	{
		for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
		{
			// String názov = tvar.getKey();
			Záznam záznam = tvar.getValue();
			záznam.cesta = null;
			záznam.tvary = null;
			Svet.uvoľni(záznam.obrázok);
			záznam.obrázok = null;
		}
		tvary.clear();
	}

	public static String[] zoznam()
	{
		Vector<String> vektor = new Vector<>();
		for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
		{
			String názov = tvar.getKey();
			// Záznam záznam = tvar.getValue();
			vektor.add(názov);
		}
		String zoznam[] = new String[vektor.size()];
		zoznam = vektor.toArray(zoznam);
		vektor = null;
		return zoznam;
	}

	public static void naplňZoznamObrázkov(Vector<Obrázok> zoznam)
	{
		for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
		{
			// String názov = tvar.getKey();
			Záznam záznam = tvar.getValue();
			zoznam.add(záznam.obrázok);
		}
	}

	/* TODO asi vymaž (množno bude treba v budúcnosti?)
	public static int indexNázvu(String hľadanýNázov)
	{
		int i = 0;
		if (null == hľadanýNázov)
			for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
			{
				String názov = tvar.getKey();
				// Záznam záznam = tvar.getValue();
				if (null == názov) return i;
				++i;
			}
		else
			for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
			{
				String názov = tvar.getKey();
				// Záznam záznam = tvar.getValue();
				if (hľadanýNázov.equals(názov)) return i;
				++i;
			}
		return -1;
	}*/

	public static Obrázok obrázokPodľaNázvu(String hľadanýNázov)
	{
		if (null == hľadanýNázov)
			for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
			{
				String názov = tvar.getKey();
				Záznam záznam = tvar.getValue();
				if (null == názov) return záznam.obrázok;
			}
		else
			for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
			{
				String názov = tvar.getKey();
				Záznam záznam = tvar.getValue();
				if (hľadanýNázov.equals(názov)) return záznam.obrázok;
			}
		return null;
	}

	public static String názovPodľaObrázka(Obrázok obrázok)
	{
		for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
		{
			String názov = tvar.getKey();
			Záznam záznam = tvar.getValue();
			if (obrázok == záznam.obrázok) return názov;
		}
		return null;
	}


	private static GRobot kreslič;

	private static Záznam vytvor(String názov, String cesta)
	{ return vytvor(názov, cesta, false); }

	private static Záznam vytvor(String názov, String cesta, boolean prepíš)
	{
		if (!prepíš && tvary.containsKey(názov)) return null;

		String svg = "<svg><path d=\"" + cesta + "\" /></svg>";
		SVGPodpora svgPodpora; try {

		svgPodpora = new SVGPodpora();
		if (-1 != svgPodpora.pridajSVG(svg))
		{
			Shape[] tvary = new Shape[16];
			tvary[0] = SVGPodpora.presuňDoStredu(svgPodpora.daj(0));

			for (int i = 1; i < 16; ++i)
			{
				Shape transformovaný = SVGPodpora.presuňDoStredu(
					svgPodpora.dajVýsledný(0, transformácie[i - 1]));
				tvary[i] = transformovaný;
			}

			Záznam záznam = new Záznam();
			záznam.cesta = cesta;
			záznam.tvary = tvary;
			záznam.obrázok = new Obrázok(100, 100);
			if (null == kreslič)
			{
				kreslič = new GRobot();
				// kreslič.veľkosť(25);
				// kreslič.mierka…
				kreslič.skry();
			}

			kreslič.kresliDoObrázka(záznam.obrázok);
			Rectangle2D hranice = záznam.tvary[0].getBounds2D();
			double veľkosť = Math.max(hranice.getWidth(), hranice.getHeight());
			if (veľkosť > 95) kreslič.mierka(95 / veľkosť);
			else if (veľkosť < 55) kreslič.mierka(55 / veľkosť);
			else kreslič.mierka(1);
			kreslič.kresliTvar(záznam.tvary[0], true);

			Tvar.tvary.put(názov, záznam);
			return záznam;
		}

		return null; } finally { svgPodpora = null; }
	}

	private static String čítaj(String priečinok, String názov)
	{
		String názovSúboru = priečinok + "/" + názov + ".vector-path";
		Súbor súbor = new Súbor();
		try
		{
			String cesta = "";
			súbor.otvorNaČítanie(názovSúboru);
			String čítanie = súbor.čítajRiadok();
			if (null != čítanie)
			{
				cesta += čítanie;
				while (null != (čítanie = súbor.čítajRiadok()))
					cesta += " " + čítanie;
			}
			return cesta;
		}
		catch (IOException e)
		{
			new GRobotException("Chyba čítania tvaru.", null, e);
		}
		finally
		{
			try { súbor.zavri(); } catch (IOException e) {
				new GRobotException("Zlyhalo zatvorenie súboru tvaru.",
				null, e); } finally { súbor = null; }
		}
		return null;
	}

	public static void importuj(String priečinok)
	{
		String[] súbory = Súbor.zoznamSúborov(priečinok);
		for (String meno : súbory)
		{
			if (meno.endsWith(".vector-path"))
			{
				String názov = meno.substring(0, meno.length() - 12);
				if (!názov.isEmpty())
				{
					String cesta = čítaj(priečinok, názov);
					if (null != cesta) vytvor(názov, cesta);
				}
			}
		}
	}


	/**
	 * Eviduje použitie tvaru, aby sa vedelo, či má zmysel ho ukladať do
	 * súboru.
	 */
	public static void eviduj(String názov)
	{
		Záznam záznam = tvary.get(názov);
		if (null != záznam) ++záznam.použitie;
	}

	/**
	 * Odeviduje použitie tvaru (aby sa vedelo, či má/nemá zmysel ho ukladať
	 * do súboru).
	 */
	public static void odeviduj(String názov)
	{
		Záznam záznam = tvary.get(názov);
		if (null != záznam && záznam.použitie > 0) --záznam.použitie;
	}


	public static Shape daj(String názov)
	{
		Záznam záznam = tvary.get(názov);
		if (null == záznam) return null;
		return záznam.tvary[0];
	}

	public static Shape daj(String názov, byte transformovaný)
	{
		Záznam záznam = tvary.get(názov);
		if (null == záznam) return null;
		if (transformovaný >= 16) transformovaný %= 16;
		/*else if (transformovaný < 0)
		{
			transformovaný %= 16;
			if (transformovaný < 0)
				transformovaný = -transformovaný;
		}*/
		return záznam.tvary[transformovaný];
	}


	public static boolean ulož(Súbor súbor) // throws IOException
	{
		súbor.vnorMennýPriestorVlastností("tvary");
		try
		{
			int počet = 0;
			súbor.zapíšVlastnosť("počet", 0);

			for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
			{
				String názov = tvar.getKey();
				Záznam záznam = tvar.getValue();
				if (záznam.použitie > 0)
				{
					súbor.zapíšVlastnosť("názov[" +
						počet + "]", názov);
					súbor.zapíšVlastnosť("cesta[" +
						počet + "]", záznam.cesta);
					++počet;
				}
			}

			súbor.zapíšVlastnosť("počet", počet);
		}
		finally
		{
			súbor.vynorMennýPriestorVlastností();
		}
		return true;
	}

	public static void čítaj(Súbor súbor) throws IOException
	{
		súbor.vnorMennýPriestorVlastností("tvary");
		try
		{
			Integer počet = súbor.čítajVlastnosť("počet", 0);
			počet = null == počet ? 0 : počet;

			for (int i = 0; i < počet; ++i)
			{
				String názov = súbor.čítajVlastnosť(
					"názov[" + i + "]", (String)null);
				String cesta = súbor.čítajVlastnosť(
					"cesta[" + i + "]", (String)null);
				if (null != názov && null != cesta) vytvor(názov, cesta);
			}
		}
		finally
		{
			súbor.vynorMennýPriestorVlastností();
		}
	}
}
