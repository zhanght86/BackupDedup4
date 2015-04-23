package xjtu.dedup.preprocess;

public class CDCParameter {
	public static int[] CDC_M_Set = {216,371,407,517,2137,4096};
	public static int[] CDC_R_Set = {13,19,17,71,37,117};
//	public static int[] CDC_M_Set = new int[566];
//	public static int[] CDC_R_Set = new int[566];
	public static int CDC_M = 517;
	public static int CDC_R = 516;
	
//	static{
//		int count = 0;
//		for(int i=137; i<=4096 ;i=i+7){
//			CDC_M_Set[count] = i;
//			CDC_R_Set[count] = i-1;
//			count++;
//		}
//	}
	public static int totalcount = CDC_M_Set.length;
	public static void setCDC_M(int cdc_m){
		CDC_M = cdc_m;
	}
	
	public static void setCDC_R(int cdc_r){
		CDC_R = cdc_r;
	}
	
	public static int getCDC_M(){
		return CDC_M;
	}
	
	public static int getCDC_R(){
		return CDC_R;
	}
}
