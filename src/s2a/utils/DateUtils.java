package s2a.utils;

import java.util.Calendar;

public class DateUtils {

	public static int getActualBox(Period period,Calendar cal){
		
		if (period==Period.MENSUAL){
			return cal.get(Calendar.MONTH);
		}
		
		if (period==Period.TRIMESTRAL){
			int actualMonth = cal.get(Calendar.MONTH);
			return actualMonth/3;
		}
		
		if (period==Period.SEMESTRAL){
			return cal.get(Calendar.MONTH)<Calendar.JULY ? 0:1;
		}
		
		return 0;
	}
	
	public static String getBoxHeader(Period period){
		
		if (period==Period.MENSUAL){
			return "ENERO;FEBRERO;MARZO;ABRIL;MAYO;JUNIO;JULIO;AGOSTO;SEPTIEMBRE;OCTUBRE;NOVIEMBRE;DICIEMBRE";
		}
		
		if (period==Period.TRIMESTRAL){
			return "1ER TRIMESTRE;2DO TRIMESTRE;3ER TRIMESTRE;4TO TRIMESTRE";
		}
		
		if (period==Period.SEMESTRAL){
			return "1ER SEMESTRE;2DO SEMESTRE";
		}
		
		return "";
	}
	
	public static int decodeNumberBoxes(Period period){
		
		if (period==Period.ANUAL) return 1;
		if (period==Period.SEMANAL) return 48;
		if (period==Period.QUINCENAL) return 24;
		if (period==Period.MENSUAL) return 12;
		if (period==Period.TRIMESTRAL) return 4;
		if (period==Period.SEMESTRAL) return 2;
		
		return 0;
	}
	
	public static String datePrint(int year,int month,int day,String sep){
		 int d  = day+100;
		 int mn = (month+1)+100;
		 int y  = year;
		 
		 return String.valueOf(d).substring(1)+sep+String.valueOf(mn).substring(1)+sep+String.valueOf(y);
	}
	
	public static String datePrint(Calendar gc){
		 int d = gc.get(Calendar.DAY_OF_MONTH)+100;
		 int m = gc.get(Calendar.MONTH)+1+100;
		 int y = gc.get(Calendar.YEAR);
		 int h = gc.get(Calendar.HOUR_OF_DAY)+100;
		 int mn = gc.get(Calendar.MINUTE)+100;
		 int ss = gc.get(Calendar.SECOND)+100;
		 
		 return String.valueOf(d).substring(1)+"-"+String.valueOf(m).substring(1)+"-"+String.valueOf(y)
				 +" "+String.valueOf(h).substring(1)+":"+String.valueOf(mn).substring(1)
				 +":"+String.valueOf(ss).substring(1);
	}
}
