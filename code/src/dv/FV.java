package dv;

public class FV {

	public EpochPair[] flag;
	
	FV()
	{
		this.flag = new EpochPair[VC.MAX_THREADS + 1];
	}
	
	public static void copy(FV fv1, FV fv2)
	{
		for(int i = 0; i < VC.MAX_THREADS + 1; i ++)
		{
			fv1.flag[i] = fv2.flag[i];
		}
	}
	
	
	public static void printFV(FV currentFV)
	{
		System.out.print("[");
		for(int i = VC.SPECIAL_ELEMENTS; i < VC.MAX_THREADS + 1; i ++)
		{
			System.out.print("(");
			if(currentFV.flag[i] != null)
			{
				if(currentFV.flag[i].source != null)
				{
					System.out.print(currentFV.flag[i].source.clock + "@" + currentFV.flag[i].source.tid + ", ");
				}
				
				if(currentFV.flag[i].sink != null)
				{
					System.out.print(currentFV.flag[i].sink.clock + "@" + currentFV.flag[i].sink.tid);
				}
			}
			System.out.print("), ");
		}
		System.out.println("]");
	}
}
