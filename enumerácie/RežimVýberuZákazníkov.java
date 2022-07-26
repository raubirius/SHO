
package enumerácie;

public enum RežimVýberuZákazníkov
{
	PRVÝ
	{
		@Override public String toString()
		{
			return "prvý – prvý prichádzajúci zákazník prvý aj odíde";
		}
	},

	POSLEDNÝ
	{
		@Override public String toString()
		{
			return "posledný – posledný prichádzajúci zákazník odíde prvý";
		}
	},

	NÁHODNÝ
	{
		@Override public String toString()
		{
			return "náhodný – je zvolený náhodný zákazník " +
				"(s rovnomerným rozložením)";
		}
	}
}
