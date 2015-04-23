package xjtu.dedup.mngtobject;

public class SparseVolume {
	//volume
	public static String vol_name = null;
	public static String vol_cap;
	public static String exp_chunk_size;
	public static String vol_type;
	public static boolean is_local = true;
	public static String RL;
	public static String vol_current_size;
	public SparseVolume(String vol_name,String vol_cap,String exp_chunk_size,String vol_type,boolean is_local){
		this.vol_name=vol_name;
		this.vol_cap=vol_cap;
		this.exp_chunk_size=exp_chunk_size;
		this.vol_type=vol_type;
		this.is_local=is_local;
		if(this.is_local)
			this.RL="L";
		else
			this.RL="R";
	}
}
