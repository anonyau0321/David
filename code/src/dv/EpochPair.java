package dv;

public class EpochPair {
	public Epoch source;
	public Epoch sink;
	
	EpochPair()
	{
		this.source = null;
		this.sink = null;
	}
	
	EpochPair(Epoch sinkE)
	{
		this.source = null;
		this.sink = sinkE;
	}
	
	EpochPair(Epoch sourceE, Epoch sinkE)
	{
		this.source = sourceE;
		this.sink = sinkE;
	}

}
