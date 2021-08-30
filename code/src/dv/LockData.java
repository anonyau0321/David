package dv;

public class LockData {
	public int address;
	public VC clock;
	
	LockData(int address)
	{
		this.address = address;
		this.clock = null;
	}
	
	public void updateClock(VC vc)
	{
		VC.copy(this.clock, vc);
	}
}
