
public interface Činnosť extends Comparable<Činnosť>
{
	public boolean činnosť();
	public boolean aktívny(); // Nie je „v limbe.“ Neaktívny == vymazaný.
	public long čas();
}
