
import java.util.Vector;

@SuppressWarnings("serial")
public class CestaZákazníka extends Vector<BodCesty>
{
	public boolean bolSpokojný;
	public String meno;

	public CestaZákazníka()
	{
		clear();
	}

	@Override public void clear()
	{
		super.clear();
		bolSpokojný = true;
		meno = null;
	}
}
