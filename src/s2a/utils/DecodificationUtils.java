package s2a.utils;

import java.util.Calendar;

public class DecodificationUtils {

	public static int decodePeriodSize(String periodStr) {
		//int value = Integer.valueOf(periodStr.substring(2, periodStr.length()));
		return 0;
	}

	public static Period decodePeriod(String periodStr) {
		// TODO Auto-generated method stub
		if (periodStr.substring(1, 2).equalsIgnoreCase("M"))
			return Period.MENSUAL;
		if (periodStr.substring(1, 2).equalsIgnoreCase("T"))
			return Period.TRIMESTRAL;
		if (periodStr.substring(1, 2).equalsIgnoreCase("S"))
			return Period.SEMESTRAL;
			
		return Period.MENSUAL;
	}

	public static Calendar decodeDate(String dateStr) {
		// TODO Auto-generated method stub
		int len = dateStr.length();
		int d = -1;
		int m = -1;
		int y = -1;
		
		if (len==8){
			d = Integer.valueOf(dateStr.substring(0, 2));
			m = Integer.valueOf(dateStr.substring(3, 5));
			y = Integer.valueOf(dateStr.substring(6, 8));
		}
		
		if (len==10){
			d = Integer.valueOf(dateStr.substring(0, 2));
			m = Integer.valueOf(dateStr.substring(3, 5));
			y = Integer.valueOf(dateStr.substring(6, 10));
		}
		
		if (y!=-1){
			Calendar cal = Calendar.getInstance();
			cal.set(y, m-1,d);
			return cal;
		}
		
		return null;
	}
	
	public static void decodeDate(Calendar cal,String dateStr) {
		// TODO Auto-generated method stub
		int len = dateStr.length();
		int d = -1;
		int m = -1;
		int y = -1;
		
		if (len==8){
			d = Integer.valueOf(dateStr.substring(0, 2));
			m = Integer.valueOf(dateStr.substring(3, 5));
			y = Integer.valueOf(dateStr.substring(6, 8));
		}
		
		if (len==10){
			d = Integer.valueOf(dateStr.substring(0, 2));
			m = Integer.valueOf(dateStr.substring(3, 5));
			y = Integer.valueOf(dateStr.substring(6, 10));
		}
		
		if (y!=-1){
			if (cal==null)
				cal = Calendar.getInstance();
			cal.set(y, m-1,d);
		}
	}

}
