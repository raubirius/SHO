
package enumerácie;

public enum RežimVýberuLiniek
{
	POSTUPNÉ
	{
		@Override public String toString()
		{
			return "<html>postupné (cyklické) prechádzanie spojení<br />  " +
				"<i>to, ktorou linkou sa začne hľadanie ďalšej voľnej linky" +
				"</i><br />  <i>určuje cyklické počítadlo</i></html>";
		}
	},

	NÁHODNÉ
	{
		@Override public String toString()
		{
			return "<html>náhodné prechádzanie spojení vyvážené pravde" +
				"podobnosťami<br />  <i>každé spojenie má hodnotu, ktorá " +
				"určí váhu</i><br />  <i>pravdepodobnosti, že bude vybraná " +
				"linka, do ktorej</i><br />  <i>smeruje</i></html>";
		}
	},

	PODĽA_PRIORÍT
	{
		@Override public String toString()
		{
			return "<html>podľa priorít – uprednostňujúce linky s vyššou " +
				"prioritou<br />  <i>každé hľadanie voľnej linky sa vždy " +
				"začína v rovnakom</i><br />  <i>poradí, ktoré je určené " +
				"prioritami spojení</i></html>";
		}
	}
}
