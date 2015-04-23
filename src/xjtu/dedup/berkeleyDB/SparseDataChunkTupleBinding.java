package xjtu.dedup.berkeleyDB;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.opendedup.hashing.HashFunctionPool;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.io.SparseDataChunk;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class SparseDataChunkTupleBinding extends TupleBinding {
	
	//把TupleInput转换为对象 
	@Override
	public Object entryToObject(TupleInput ti) {
		// TODO Auto-generated method stub
		boolean thedoop=ti.readBoolean();
		byte[] thehash=ti.readBytes(HashFunctionPool.hashLength).getBytes();
		boolean thelocalData=ti.readBoolean();
		long thetimeAdded=ti.readLong();
		int thechunkLen = 0;
		if(Main.iscdc)
			thechunkLen=ti.readInt();
		
		SparseDataChunk	sparseChunk=null;
		if(Main.iscdc)
			sparseChunk=new SparseDataChunk(thedoop,thehash,thelocalData,thetimeAdded,thechunkLen);
		else
			sparseChunk=new SparseDataChunk(thedoop,thehash,thelocalData,thetimeAdded);
		return sparseChunk;
	}

    //  把对象转换成TupleOutput 
	@Override
	public void objectToEntry(Object object, TupleOutput to) {
		// TODO Auto-generated method stub
		SparseDataChunk sparseChunk=(SparseDataChunk)object;
		to.writeBoolean(sparseChunk.isDoop());
		to.writeBytes(sparseChunk.getHash().toString());
		to.writeLong(sparseChunk.getTimeAdded());
		if(Main.iscdc)
			to.writeInt(sparseChunk.getChunkLen());
		
	}

}
