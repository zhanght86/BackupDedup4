package org.opendedup.util.date;

import java.util.Calendar;

public class WeekPolicy {
	/*
	 * Every value represent different backup type.For example,Monday =0 represent full backup,
	 * Monday=1 represent incremental backup ,Monday=2 represent differental backup.
	 * */
	public WeekPolicy(){
		
	}
	public WeekPolicy(int mon,int tue,int wed,int thu,int fri,int sat,int sun){
		this.Monday=mon;
		this.Tuesday=tue;
		this.Wednesday=wed;
		this.Thursday=thu;
		this.Friday=fri;
		this.Saturday=sat;
		this.Sunday=sun;
	}
	public int Monday=-1;
	public int Tuesday=-1;
	public int Wednesday=-1;
	public int Thursday=-1;
	public int Friday=-1;
	public int Saturday=-1;
	public int Sunday=-1;
	
	public int getbackupType(){
		int btype = -1;
		Calendar   calendar   =   Calendar.getInstance(); 
		if((this.Monday==0&&calendar.get(Calendar.DAY_OF_WEEK)==2)||(this.Tuesday==0&&calendar.get(Calendar.DAY_OF_WEEK)==3)
 		||(this.Wednesday==0&&calendar.get(Calendar.DAY_OF_WEEK)==4)||(this.Thursday==0&&calendar.get(Calendar.DAY_OF_WEEK)==5)
 		||(this.Friday==0&&calendar.get(Calendar.DAY_OF_WEEK)==6)||(this.Saturday==0&&calendar.get(Calendar.DAY_OF_WEEK)==7)
 			||(this.Sunday==0&&calendar.get(Calendar.DAY_OF_WEEK)==1)){
			btype = 0;
		}else if((this.Monday==1&&calendar.get(Calendar.DAY_OF_WEEK)==2)||(this.Tuesday==1&&calendar.get(Calendar.DAY_OF_WEEK)==3)
		 		||(this.Wednesday==1&&calendar.get(Calendar.DAY_OF_WEEK)==4)||(this.Thursday==1&&calendar.get(Calendar.DAY_OF_WEEK)==5)
		 		||(this.Friday==1&&calendar.get(Calendar.DAY_OF_WEEK)==6)||(this.Saturday==1&&calendar.get(Calendar.DAY_OF_WEEK)==7)
		 			||(this.Sunday==1&&calendar.get(Calendar.DAY_OF_WEEK)==1)){
			btype = 1;
		}else if((this.Monday==2&&calendar.get(Calendar.DAY_OF_WEEK)==2)||(this.Tuesday==2&&calendar.get(Calendar.DAY_OF_WEEK)==3)
		 		||(this.Wednesday==2&&calendar.get(Calendar.DAY_OF_WEEK)==4)||(this.Thursday==2&&calendar.get(Calendar.DAY_OF_WEEK)==5)
		 		||(this.Friday==2&&calendar.get(Calendar.DAY_OF_WEEK)==6)||(this.Saturday==2&&calendar.get(Calendar.DAY_OF_WEEK)==7)
		 			||(this.Sunday==2&&calendar.get(Calendar.DAY_OF_WEEK)==1)){
			btype = 2;
		}
		return btype;		
	}
	
	public String getWeekofDayWhenBackup(){
		String weekofday = null;
		if(this.Monday !=-1){
			weekofday = "Monday";	
		}else if(this.Tuesday != -1){
			weekofday = "Tuesday";
		}else if(this.Wednesday !=-1){
			weekofday = "Wednesday";
		}else if(this.Thursday != -1){
			weekofday = "Thursday";
		}else if(this.Friday != -1){
			weekofday = "Friday";
		}else if(this.Saturday != -1){
			weekofday = "Saturday";
		}else if(this.Sunday != -1){
			weekofday = "Sunday";
		}
		return weekofday;
			
	}
}
