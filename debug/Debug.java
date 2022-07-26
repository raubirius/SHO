
package debug;

import static java.lang.System.*;

public class Debug
{
	public static boolean debugOn = false;
	public static int level = 0;

	public static void printTraceElements(Throwable t, Integer... es)
	{
		if (!debugOn) return;

		StackTraceElement[] stes = t.getStackTrace();
		for (Integer e : es)
			if (null != e && e < stes.length)
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
