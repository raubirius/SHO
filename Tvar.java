
import java.awt.Shape;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import knižnica.*;

public class Tvar
{
	private static class Záznam
	{
		String cesta = null;
		int použitie = 0;
		Shape tvar = null;
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
			záznam.tvar = null;
		}
		tvary.clear();
	}

	public static String[] zoznam()
	{
		Vector<String> vektor = new Vector<>();
		for (Map.Entry<String, Záznam> tvar : tvary.entrySet())
		{
			String názov = tvar.getKey();
			vektor.add(názov);
		}
		String zoznam[] = new String[vektor.size()];
		zoznam = vektor.toArray(zoznam);
		vektor = null;
		return zoznam;
	}


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
			Shape tvar = SVGPodpora.presuňDoStredu(svgPodpora.daj(0));
			Záznam záznam = new Záznam();
			záznam.cesta = cesta;
			záznam.tvar = tvar;
			tvary.put(názov, záznam);
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
		return záznam.tvar;
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
