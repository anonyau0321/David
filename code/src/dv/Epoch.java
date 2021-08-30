package dv;

public class Epoch {
	public int clock;
	public int tid;
	
	
	Epoch(int clock, int tid)
	{
		this.clock = clock;
		this.tid = tid;
	}
	
	
	public static boolean Equal(Epoch e1, Epoch e2)
	{
		if (e1.tid == e2.tid && e1.clock == e2.clock)
		{
			return true;
		}
		return false;
	}
	
	
	public static boolean LessEqual(Epoch e1, Epoch e2)
	{
		if (e1.tid == e2.tid && e1.clock <= e2.clock)
		{
			return true;
		}
		return false;
	}
	
	public static boolean Less(Epoch e1, Epoch e2)
	{
		if (e1.tid == e2.tid && e1.clock < e2.clock)
		{
			return true;
		}
		return false;
	}
	
	

}
