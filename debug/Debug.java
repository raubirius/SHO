
package debug;

import static java.lang.System.*;

public class Debug
{
	public static boolean debugOn = false;
	public static int level = 0;

	public static void printTraceRanges(Throwable t, Integer... es)
	{
		if (!debugOn) return;

		StackTraceElement[] stes = t.getStackTrace();
		Integer s = null;
		for (Integer e : es)
			if (null != e)
			{
				if (e < 0) e = stes.length + e;
				if (null == s) s = e; else
				if (s >= 0 && s < stes.length &&
					e >= 0 && e < stes.length)
				{
					for (int i = s; i <= e; ++i)
					{
						StackTraceElement ste = stes[i];
						out.print(ste.getClassName());
						out.print('.');
						out.print(ste.getMethodName());
						out.print(" (");
						out.print(ste.getFileName());
						out.print(':');
						out.print(ste.getLineNumber());
						out.print(") ");
					}
					s = null;
				}
			}
	}

	public static void printTraceElements(Throwable t, Integer... es)
	{
		if (!debugOn) return;

		StackTraceElement[] stes = t.getStackTrace();
		for (Integer e : es)
			if (null != e)
			{
				if (e < 0) e = stes.length + e;
				if (e >= 0 && e < stes.length)
				{
					StackTraceElement ste = stes[e];
					out.print(ste.getClassName());
					out.print('.');
					out.print(ste.getMethodName());
					out.print(" (");
					out.print(ste.getFileName());
					out.print(':');
					out.print(ste.getLineNumber());
					out.print(") ");
				}
			}
	}

	public static void printLevel()
	{
		if (!debugOn) return;

		for (int i = 0; i <= level; ++i) out.print(" #"); out.print(" ");
	}

	public static void printDebug(Object... args)
	{
		if (!debugOn) return;

		for (Object arg : args)
			if (null == arg) out.print("null");
			else out.print(arg.toString());
	}

	public static void printlnDebug(Object... args)
	{
		if (!debugOn) return;

		printDebug(args);
		out.println();
	}

	public static void debugIn(Object... args)
	{
		if (!debugOn) return;

		printLevel();
		printTraceElements(new Throwable(), 1);
		printlnDebug(args);
		++level;
	}

	public static void debugOut(Object... args)
	{
		if (!debugOn) return;

		if (0 != args.length) debug(args);
		--level;
	}

	public static void debug(Object... args)
	{
		if (!debugOn) return;

		printLevel();
		printlnDebug(args);
	}
}
