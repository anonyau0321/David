package dv;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

public class TransactionalHBGraph {
	public static int OCTET_FINALIZER_THREAD_ID = 0 + VC.SPECIAL_ELEMENTS;
	public static int DACAPO_DRIVER_THREAD_OCTET_ID = 1 + VC.SPECIAL_ELEMENTS;
	public static int TRANSCNT = 0;
	public static boolean nonseria = false;
	
	public ArrayList<RVMThread> ThreadMap;
	public Map<Integer, MemoryData> MemoryMap;
	public Map<Integer, LockData> LockMap;
	public ArrayList<Integer> AtomicList;
	public ArrayList<Integer> DetectList;
	public ArrayList<Integer> threadCNT;
	public int transCNT;
	public int writeCNT;
	public int readCNT;
	public int lockCNT;
	
	TransactionalHBGraph()
	{
		this.ThreadMap = new ArrayList<RVMThread>();
		for(int id = VC.SPECIAL_ELEMENTS; id < VC.MAX_THREADS + 1; id++) //initialize the threadMap and create a RVMThread object for each thread
		{
			ThreadMap.add(new RVMThread(id));
		}
		this.MemoryMap = new HashMap<Integer, MemoryData>();
		this.LockMap = new HashMap<Integer, LockData>();
		this.AtomicList = new ArrayList<Integer>();
		this.DetectList = new ArrayList<Integer>();
		this.threadCNT = new ArrayList<Integer>();
		this.transCNT = 0;
		this.writeCNT = 0;
		this.readCNT = 0;
		this.lockCNT = 0;
		this.nonseria = false;
	}
	
	public void update_AtomicList() {
		// TODO Auto-generated method stub
		for(int i = 0; i < this.DetectList.size(); i++)
		{
			if (!this.AtomicList.contains(this.DetectList.get(i)))
			{
				this.AtomicList.add(this.DetectList.get(i));
			}
		}
		
	}

	public int get_atomic() {
		// TODO Auto-generated method stub
		return this.AtomicList.size();
	}

	public void initData() {
		// TODO Auto-generated method stub
		this.ThreadMap = new ArrayList<RVMThread>();
		for(int id = VC.SPECIAL_ELEMENTS; id < VC.MAX_THREADS + 1; id++)
		{
			ThreadMap.add(new RVMThread(id));
		}
		this.MemoryMap = new HashMap<Integer, MemoryData>();
		this.LockMap = new HashMap<Integer, LockData>();
		this.threadCNT = new ArrayList<Integer>();
		this.transCNT = 0;
		this.writeCNT = 0;
		this.readCNT = 0;
		this.lockCNT = 0;
		
	}

	public ArrayList<Integer> get_atomicList() {
		// TODO Auto-generated method stub
		return this.AtomicList;
	}
	

	public void startTransaction(int threadid, int site) {
		// TODO Auto-generated method stub
		if (threadid == DACAPO_DRIVER_THREAD_OCTET_ID || threadid == OCTET_FINALIZER_THREAD_ID) //not for JGF benchmark
		{
			return;
		}
		
		RVMThread threadState = this.ThreadMap.get(threadid - 1);
		if(threadState.currentInTransaction == false && threadState.currentTransaction.isUnary && !this.AtomicList.contains(site))
		{
			threadState.currVC.incrementClock(threadid);
			threadState.numOfNodes++;
			threadState.set_currTransaction(new Transaction(threadState.numOfNodes, false, false, site, threadid));
			threadState.currentTransaction.set_beginVC(threadState.currVC);
			threadState.currentTransaction.set_currVC(threadState.currVC);
			threadState.currentInTransaction = true;
			
			//create new SEV for each transaction
			threadState.currFV = new FV();
			Epoch current = new Epoch(threadState.currVC.clock[threadid], threadid);
			EpochPair currentRun = new EpochPair(current);
			threadState.currFV.flag[threadid] = currentRun;
			
			
			if(!this.threadCNT.contains(threadid))
			{
				this.threadCNT.add(threadid);
			}
			this.transCNT++;
		
		}
		
		
	}

	public void endTransaction(int threadid, int site) {
		// TODO Auto-generated method stub
		if (threadid == DACAPO_DRIVER_THREAD_OCTET_ID || threadid == OCTET_FINALIZER_THREAD_ID)
		{
			return;
		}
		
		RVMThread threadState = this.ThreadMap.get(threadid - 1);
		if(threadState.currentInTransaction == true && threadState.currentTransaction.siteID == site)
		{
			threadState.currentInTransaction = false;
			//create an unary transaction to merge outside operations
			threadState.currVC.incrementClock(threadid);
			threadState.numOfNodes++;
			threadState.set_currTransaction(new Transaction(threadState.numOfNodes, true, false, site, threadid));
			threadState.currentTransaction.set_beginVC(threadState.currVC);
			threadState.currentTransaction.set_currVC(threadState.currVC);
			this.transCNT ++;
			
		}
		
	}

	public void processRead(int threadid, int address) {
		// TODO Auto-generated method stub
		if (threadid == DACAPO_DRIVER_THREAD_OCTET_ID || threadid == OCTET_FINALIZER_THREAD_ID)
		{
			return;
		}
		this.readCNT ++;
		RVMThread threadState = this.ThreadMap.get(threadid - 1);
		Transaction currRead = threadState.currentTransaction;
		if(this.MemoryMap.containsKey(address)) //not the first access
		{
			MemoryData MemVar = this.MemoryMap.get(address);
			if(MemVar.writeVC != null) //last write exists
			{
				VC lastWrite = MemVar.writeVC;
				
				int lwThread = lastWrite.getTid();
				
				if (lwThread != threadid && MemVar.readVCMap.get(threadid - 1) == null) //this is the first read of this thread since last write
				{
					if (buildHB(lastWrite, currRead))  //create write-read edge
					{
						VC.copy(currRead.currVC, threadState.currVC);//keep track with the shadow state of V(t)
					}												  
				}
				
			}
			VC read = new VC(threadid);
			VC.copy(read, currRead.currVC);  //update last read
			
			MemVar.readVCMap.set(threadid - 1, read);
	
			if(!MemVar.setRead)
			{
				MemVar.setRead = true;
			}
		}
		else   //this is the first access to this variable
		{
			MemoryData MemVar = new MemoryData(address);
			VC read = new VC(threadid);
			VC.copy(read, currRead.currVC);  //update last read
		
			MemVar.readVCMap.set(threadid - 1, read);
			MemVar.setRead = true;
			this.MemoryMap.put(address, MemVar);
		}
		
	}

	public void processWrite(int threadid, int address) {
		// TODO Auto-generated method stub
		if (threadid == DACAPO_DRIVER_THREAD_OCTET_ID || threadid == OCTET_FINALIZER_THREAD_ID)
		{
			return;
		}
		this.writeCNT++;
		RVMThread threadState = this.ThreadMap.get(threadid - 1);
		Transaction currWrite = threadState.currentTransaction;
		if(this.MemoryMap.containsKey(address)) //not the first access
		{
			MemoryData MemVar = this.MemoryMap.get(address);
			if(MemVar.setRead) //this variable has been read after last write
			{
				for(int id = VC.SPECIAL_ELEMENTS; id < VC.MAX_THREADS + 1; id++)
				{
					if(MemVar.readVCMap.get(id - 1) != null && id != threadid) //thread id has read this variable and id != threadid, we can build read->write dependence
					{
						if(buildHB(MemVar.readVCMap.get(id - 1), currWrite))  //create read-write edges
						{
							VC.copy(currWrite.currVC, threadState.currVC);
						}
					}
				}
				MemVar.readVCMap = new ArrayList<VC>(); //clrear all reads 
				for(int id = VC.SPECIAL_ELEMENTS; id < VC.MAX_THREADS + 1; id ++)
				{
					MemVar.readVCMap.add(null);
				}
			}
			else //there is no read between two writes
			{
				if(MemVar.writeVC != null) //if last write exists
				{
					VC lastWrite = MemVar.writeVC;
					int lwThread = lastWrite.getTid();
					
					if(lwThread != threadid)   //create write-write dependence
					{
						if(buildHB(lastWrite, currWrite)) 
						{
							VC.copy(currWrite.currVC, threadState.currVC);
						}
					}
				}
				
			}
			MemVar.writeVC = new VC(threadid);
			VC.copy(MemVar.writeVC, currWrite.currVC);  //update last write
		}
		else  //this is the first access to this variable
		{
			MemoryData MemVar = new MemoryData(address);
			MemVar.writeVC = new VC(threadid);
			VC.copy(MemVar.writeVC, currWrite.currVC);
			this.MemoryMap.put(address, MemVar);
		}
		
		
	}

	public void processAcquire(int threadid, int address) {
		// TODO Auto-generated method stub
		if (threadid == DACAPO_DRIVER_THREAD_OCTET_ID || threadid == OCTET_FINALIZER_THREAD_ID)
		{
			return;
		}
		this.lockCNT++;
		RVMThread threadState = this.ThreadMap.get(threadid - 1);
		Transaction currentAcq = threadState.currentTransaction;
		if(this.LockMap.containsKey(address))  //create rel-acq dependence
		{
			LockData LockVar = this.LockMap.get(address);
			if(LockVar.clock != null) //last release exists
			{
				VC lastRel = LockVar.clock;
				int lastThread = lastRel.getTid();
				
				if(lastThread != threadid)
				{
					if(buildHB(lastRel, currentAcq)) 
					{
						VC.copy(currentAcq.currVC, threadState.currVC);
					}
				}
			}
			
		}
		else  //this is the first access to this lock
		{
			LockData LockVar = new LockData(address); 
			this.LockMap.put(address, LockVar);
		}
		
	}

	public void processRelease(int threadid, int address) {
		// TODO Auto-generated method stub
		if (threadid == DACAPO_DRIVER_THREAD_OCTET_ID || threadid == OCTET_FINALIZER_THREAD_ID)
		{
			return;
		}
		
		RVMThread threadState = this.ThreadMap.get(threadid - 1);
		Transaction currentRel = threadState.currentTransaction;
		if(this.LockMap.containsKey(address))
		{
			LockData LockVar = this.LockMap.get(address);
			LockVar.clock = new VC(threadid);	
			VC.copy(LockVar.clock, currentRel.currVC);  //update the clock of this lock
		}
		
	}
	
	public boolean buildHB(VC source, Transaction dest)
	{
		int sourceID = source.getTid();
		int destID = dest.threadid;
		if(!needHB(sourceID, destID))
		    return false;

		RVMThread threadState = this.ThreadMap.get(destID - 1);
		boolean updated = VC.join(threadState.currVC, source);  //update the clock by join operation
		updateSEV(source, dest);
		
		if(checkHB(source, dest))
		{
			if(!this.DetectList.contains(dest.siteID))
			{
				this.DetectList.add(dest.siteID);
			}
			System.out.println("*********DV************"); //report the cycles
			locateCycleSequence(source, dest);
			System.out.println(dest.siteID);
			System.out.println("**************************");
			if(!threadState.TransactionList.contains(dest.transactionID)) //for each transaction, only report once
			{
				
				threadState.TransactionList.add(dest.transactionID);
				this.TRANSCNT = this.TRANSCNT + 1;
			}
		}
		else if(checkCycle(source, dest)) //disable for non-increasing cycles
		{
			this.nonseria = true;
//			System.out.println("*********non-serializable trace************");
//			System.out.println("**************************");
		}
		
		//update TVC
		updateTVC(source, dest, sourceID, destID); //enable the detection of non-serializable trace
		
		
		return updated; //clock has been updated
	}
	
	private void locateCycleSequence(VC source, Transaction dest)
	{
		//the last and the first transaction in this cycle should be dest
		LinkedList<Epoch> sequence = new LinkedList<Epoch>(); 
		int destTid = dest.threadid;
		int sourceTid = source.getTid();
		int errorFlag = 0;
		RVMThread threadState = this.ThreadMap.get(destTid - 1);
		

		//search from the last to the beginning along the increasing THB
		Epoch last = new Epoch(source.clock[destTid], destTid);
		sequence.add(last);
		Epoch beforeLast = new Epoch(source.clock[sourceTid], sourceTid);
		sequence.add(beforeLast);
		
		EpochPair previous = threadState.currFV.flag[sourceTid];
		if(previous != null && previous.sink.clock < beforeLast.clock)
		{
			sequence.add(previous.sink);
		}
		Epoch target = previous.source;
		while(true)
		{
			if(Epoch.Equal(target, last))
			{
				sequence.add(target); // find the dest transaction --> rebuild the path from beforeLast to dest, a cycle exists
				break;
			}
			
			sequence.add(target);
			previous = threadState.currFV.flag[target.tid];
			if(previous != null && previous.sink.clock < target.clock)
			{
				sequence.add(previous.sink);
			}
			target = previous.source;
			
			errorFlag ++;
			
			if(errorFlag == VC.MAX_THREADS)
			{
				//endless loop error, no path found
				//print the error threadState.currFV
				System.out.println("error!!!");
				FV.printFV(threadState.currFV);
				break;
			}
			
		}	
		if(errorFlag <= VC.MAX_THREADS)
		{
			for(int i = sequence.size() - 1; i > 0; i--)
			{
				System.out.print(sequence.get(i).clock + "@" + sequence.get(i).tid + "-->");
			}
			System.out.println(sequence.get(0).clock + "@" + sequence.get(0).tid);
		}
		
	}
	
	private void updateSEV(VC source, Transaction dest)
	{
		int destTid = dest.threadid;
		int sourceTid = source.getTid();
		RVMThread threadState = this.ThreadMap.get(destTid - 1);
		RVMThread sourceState = this.ThreadMap.get(sourceTid - 1);
		Epoch sourceE = new Epoch(source.clock[sourceTid], sourceTid);
		Epoch sinkE = new Epoch(dest.currVC.clock[destTid], destTid);
		
		//just check whether need to update the SEV of the source thread, only when currSourceTrans is the currently running transaction of source thread
		if(sourceState.currFV.flag[sourceTid] != null && Epoch.Equal(sourceState.currFV.flag[sourceTid].sink, sourceE) && sourceState.currFV.flag[destTid] == null) 
		{
			EpochPair ss = new EpochPair(sourceE, sinkE);
			sourceState.currFV.flag[destTid] = ss;
		}
		
		backPropagateSEV(sourceTid, destTid, source, dest, sourceE, sinkE);
	}
	
	private void backPropagateSEV(int sourceID, int destID, VC source, Transaction dest, Epoch sourceE, Epoch sinkE)
	{
		for(int id = VC.SPECIAL_ELEMENTS; id < VC.MAX_THREADS + 1; id++)
		{
			if(id != sourceID && id != destID)
			{
				//check whether the sinkE need to propagate to other threads through sourceE
				RVMThread threadState = this.ThreadMap.get(id - 1);
				//(1)other thread must have a currently running transaction->has begin event, (2)must have an increasing THB relation from that trans to current dest, (3) has path to source transaction, (4) no path to sink thread before 
				if(threadState.currentInTransaction && threadState.currentTransaction.beginVC.clock[id] <= source.clock[id] && (threadState.currFV.flag[sourceID] != null && Epoch.LessEqual(threadState.currFV.flag[sourceID].sink, sourceE)) && threadState.currFV.flag[destID] == null)
				{
					EpochPair ss = new EpochPair(sourceE, sinkE);
					threadState.currFV.flag[destID] = ss;
				}
				
				
			}
		}
	}
	
	private void updateTVC(VC source, Transaction dest, int sourceID, int destID)
	{
		RVMThread destState = this.ThreadMap.get(destID - 1);
		RVMThread sourceState = this.ThreadMap.get(sourceID - 1);
		int sourceC = source.clock[sourceID];
		int sinkC = destState.currVC.clock[destID];
		ArrayList<Integer> Tids11 = new ArrayList<Integer>();
		ArrayList<Integer> Tids12 = new ArrayList<Integer>();
		if(sourceState.reverVC.clock[sourceID] == sourceC) //same source transaction
		{
			if(sourceState.reverVC.clock[destID] == 0 || sourceState.reverVC.clock[destID] > sinkC)
			{
				sourceState.reverVC.clock[destID] = sinkC;
			}
			forwardPropagate(Tids11, sourceID, destID);
			backPropagate(Tids12, sourceID, destID, sourceC, sinkC);
		}
		else if(sourceState.reverVC.clock[sourceID] < sourceC)
		{
			sourceState.reverVC.initialVC();
			sourceState.reverVC.clock[sourceID] = sourceC;
			sourceState.reverVC.clock[destID] = sinkC;
			forwardPropagate(Tids11, sourceID, destID);
			backPropagate(Tids12, sourceID, destID, sourceC, sinkC);
		}
		
	}
	
	private ArrayList<Integer> forwardPropagate(ArrayList<Integer> Tids, int sourceID, int destID)
	{
		ArrayList<Integer> Tid2 = Tids;
		Tid2.add(sourceID);
		Tid2.add(destID);
		RVMThread destState = this.ThreadMap.get(destID - 1);
		RVMThread sourceState = this.ThreadMap.get(sourceID - 1);
		if(sourceState.reverVC.clock[destID] > 0 && destState.reverVC.clock[destID] > 0 && sourceState.reverVC.clock[destID] <= destState.reverVC.clock[destID] ) 
		{
			for(int id = VC.SPECIAL_ELEMENTS; id < VC.MAX_THREADS; id ++)
			{
				if(id == sourceID)
				{
					continue;
				}
				
				if((destState.reverVC.clock[id] != 0 && sourceState.reverVC.clock[id] > destState.reverVC.clock[id]) || (sourceState.reverVC.clock[id] == 0 && destState.reverVC.clock[id] > 0))
				{
					sourceState.reverVC.clock[id] = destState.reverVC.clock[id];
					
					if(!Tid2.contains(id))
					{
						Tid2 = forwardPropagate(Tid2, sourceID, id);
					}
				}
			}
		}
		return Tid2;
	}
	
	private ArrayList<Integer> backPropagate(ArrayList<Integer> Tids, int sourceID, int destID, int source, int sink)
	{
		ArrayList<Integer> Tid1 = Tids;
		Tid1.add(sourceID);
		Tid1.add(destID);
		for(int id = VC.SPECIAL_ELEMENTS; id < VC.MAX_THREADS; id ++)
		{
			if(Tid1.contains(id))
			{
				continue;
			}
			
			RVMThread threadState1 = this.ThreadMap.get(id - 1);
			if(threadState1.reverVC.clock[id] > RVMThread.START_TRANSACTION_ID) //running transaction has outgoing edge
			{
				if(threadState1.reverVC.clock[sourceID] <= source)
				{
					if(threadState1.reverVC.clock[destID] == 0 || (sink != 0 && threadState1.reverVC.clock[destID] > sink))
					{
						threadState1.reverVC.clock[destID] = sink;
					}
					forwardPropagate(new ArrayList<Integer>(), id, destID); //update TVC of thread (id)
					Tid1 = backPropagate(Tid1, id, destID, threadState1.reverVC.clock[id], sink); //back propagate to other threads
				}
			}
		}
		return Tid1;
	}
	
	private boolean checkCycle(VC source, Transaction dest) {
		// TODO Auto-generated method stub
		if(dest.isUnary || dest.transactionID == RVMThread.START_TRANSACTION_ID)
			return false;
		int sourceID = source.getTid();
		int destID = dest.threadid;
		
		RVMThread destState = this.ThreadMap.get(destID - 1);
		if(dest.currVC.clock[destID] == destState.reverVC.clock[destID])  //current ongoing transaction dest has outgoing edges
		{
			if(destState.reverVC.clock[sourceID] > 0 && VC.lessThan(destState.reverVC, source, sourceID)) //current ongoing transaction dest has direct or indirect outgoing edge
			{																				//to source or predecessor transaction in source thread
				return true;
			}
		}
		return false;
	}

//	private boolean checkHB(VC source, Transaction dest) {
//		// TODO Auto-generated method stub
//		if(dest.isUnary || dest.transactionID == RVMThread.START_TRANSACTION_ID)
//			return false;
//		if(VC.lessThan(dest.beginVC, source, dest.threadid)) //dest.beginVC happens-before source ==> source can see dest.begin event
//		{
//			return true;
//		}
//		return false;
//	}
	private boolean checkHB(VC source, Transaction dest) {
		// TODO Auto-generated method stub
		if(dest.isUnary || dest.transactionID == RVMThread.START_TRANSACTION_ID)
			return false;
		int sourceID = source.getTid();
		int destID = dest.threadid;
		RVMThread destState = this.ThreadMap.get(destID - 1);
		Epoch sourceE = new Epoch(source.clock[sourceID], sourceID);
		if(destState.currentInTransaction && destState.currFV.flag[sourceID] != null && Epoch.Less(destState.currFV.flag[sourceID].sink, sourceE))
		{
			return true;
		}
		else if(destState.currentInTransaction && destState.currFV.flag[sourceID] != null && Epoch.Equal(destState.currFV.flag[sourceID].sink, sourceE) && VC.lessThan(dest.beginVC, source, dest.threadid)) //dest.beginVC happens-before source ==> source can see dest.begin event
		{
			return true;
		}
		return false;
	}

	private static boolean needHB(int sourceID, int destID) {
		   if (sourceID == destID) { 
		     return false; // No cross-thread dependence
		   }   
		   // This is especially problematic for avrora9
		   if (sourceID == OCTET_FINALIZER_THREAD_ID || destID == OCTET_FINALIZER_THREAD_ID) {
		     return false;
		   }    
		   // We avoid cycle detection from the driver thread in DaCapo, which is currently Thread 1. Sync changes with AVD.
		   if (sourceID == DACAPO_DRIVER_THREAD_OCTET_ID || destID == DACAPO_DRIVER_THREAD_OCTET_ID) {
		     return false;
		   }

		   return true;
		 }

	
	
}
