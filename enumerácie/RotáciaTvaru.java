
package enumerácie;

public enum RotáciaTvaru
{
	Q0 { @Override public String toString() { return "0°"; }},
	Q1 { @Override public String toString() { return "90°"; }},
	Q2 { @Override public String toString() { return "180°"; }},
	Q3 { @Override public String toString() { return "270°"; }}
}
