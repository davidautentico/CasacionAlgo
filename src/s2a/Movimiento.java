package s2a;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import s2a.utils.DateUtils;

public class Movimiento {

  
	
	long line = -1; //linea del diario
	long numAsiento = -1;
	long debe = 0; //cantidad del debe en long -> long/100
	long haber = 0;//cantidad del haber en long -> long/100
	long importe = 0; //positivo si es debe, negativo si es haber
	int day = -1;
	int month = -1;
	int year = -1;
	boolean casado = false;//casado con otro/s movimiento
        boolean removed = false;
	
	public Movimiento(){};
	
	public Movimiento(long aLine,long aNumAsiento,long aDebe,long aHaber,int aDay,int aMonth,int aYear,boolean aCasado){
		this.line = aLine;
		this.numAsiento = aNumAsiento;
		this.debe = aDebe;
		this.haber = aHaber;
		this.day = aDay;
		this.month = aMonth;
		this.year = aYear;
		this.casado = aCasado;
		this.importe = aDebe-aHaber;
	}
        
    public static void remove(ArrayList<Movimiento> movimientos, int begin, int end) {
        // System.out.println("entrado a borrar ");
        int totalToRemove = end-begin+1;
        // System.out.println("totalToremove: "+totalToRemove);
        for (int i=1;i<=totalToRemove;i++){
            movimientos.remove(begin);
            //System.out.println("borrando "+begin);
        }
        //System.out.println("salido de borrar ");
    }
    
    public static void removeExclusion(ArrayList<Movimiento> movimientos,
                HashMap<Long,Integer> linesToKeep) {
          
        
        /*Iterator it = linesToKeep.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            long line = (long)pair.getKey();
            int value = (int)pair.getValue();
            if (value==0){
                movimientos.re
            }            
        }*/
        //String movsStr="";
       // for (int i=0;i<movimientos.size();i++) movsStr+= " "+movimientos.get(i).getLine();
        //System.out.println("movs: "+movsStr);
        int i = 0;
        while (i<movimientos.size()){            
            boolean isRemoved = false;
            if (linesToKeep.containsKey(movimientos.get(i).getLine())){
                int value = linesToKeep.get(movimientos.get(i).getLine());
                //System.out.println("[PROCESO 112b POR EL HABER] line value: "+movimientos.get(i).getLine()+" "+value); 
                if (value==0){//borro todos los que no forman parte del saldo
                    //System.out.println("[PROCESO 112b POR EL HABER] line value borrado: "
                          //  +movimientos.get(i).getLine()+" "+value+" "+(movimientos.get(i).debe-movimientos.get(i).getHaber())); 
                    isRemoved = true;
                }
            }            
            if (isRemoved){                
                movimientos.remove(i);
            }else{
                i++;
            }
        }        
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

        
        
	public long getLine() {
		return line;
	}

	public void setLine(long line) {
		this.line = line;
	}
	
	public long getNumAsiento() {
		return numAsiento;
	}

	public void setNumAsiento(long numAsiento) {
		this.numAsiento = numAsiento;
	}

	public long getDebe() {
		return debe;
	}

	public void setDebe(long debe) {
		this.debe = debe;
	}

	public long getHaber() {
		return haber;
	}

	public void setHaber(long haber) {
		this.haber = haber;
	}

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

        
	public long getImporte() {
		return importe;
	}

	public void setImporte(long importe) {
		this.importe = importe;
	}

	public boolean isCasado() {
		return casado;
	}

	public void setCasado(boolean casado) {
		this.casado = casado;
	}
	
	public void getCalendar(Calendar cal){
		cal.set(year,month,day,0,0,0);                
                //cal.set(Calendar.MILLISECOND, 0);
	}
	
	public String toString(){
		return line+" "+" "+debe+" "+haber+" "+importe+" "+casado;
	}
	
	public String toStringDate(String header){
		return header+" "+line+" "+day+"-"+month+"-"+year
                        +" "+DateUtils.datePrint(year, month, day,"/")
                        +" "+this.numAsiento
                        +" "+debe+" "+haber+" "+importe+" "+casado;
	}

	public void copy(Movimiento m) {
		// TODO Auto-generated method stub
		this.line = m.getLine();
		this.importe = m.getImporte();
		this.haber = m.getHaber();
		this.debe = m.getDebe();
		this.casado = m.isCasado();
                this.year = m.getYear();
                this.month = m.getMonth();
                this.day = m.getDay();
                this.numAsiento = m.getNumAsiento();
	}
	
	/**
	 * Ordena los movimientos por importe y despues por numAsiento
	 * @param movimientos
	 */
	public  static void sortMovimientos(ArrayList<Movimiento> movimientos) {
            
            Collections.sort(movimientos, new Comparator<Movimiento>() {
                public int compare(Movimiento m1, Movimiento m2) {
                    long importe1 = m1.getImporte();
		    long importe2 = m2.getImporte();
                    long asiento1 = m1.getNumAsiento();
                    long asiento2 = m2.getNumAsiento();
                    int result = -1;
                    if (importe1<importe2) result = -1;
                    if (importe1>importe2) result = 1;
                    if (importe1==importe2){
                        if (asiento1>asiento2){//los asientos menores tienen precedencia
                            result = -1;
                        }else if (asiento1<asiento2){
                            result = 1;
                        }else{
                            result = 0;
                        }
                    }
		    return result;
		}
            });
	}
	
	/**
	 * Ordena los movimientos por fecha
	 * @param movimientos
	 */
	public  static void sortMovimientosByDate(ArrayList<Movimiento> movimientos) {
		
		final Calendar cal1 = Calendar.getInstance();
		final Calendar cal2 = Calendar.getInstance();
		
		Collections.sort(movimientos, new Comparator<Movimiento>() {

		        public int compare(Movimiento m1, Movimiento m2) {
		        	m1.getCalendar(cal1);
		        	m2.getCalendar(cal2);
		        	return cal1.compareTo(cal2);
		        	/*return cal1.getTimeInMillis() < cal2.getTimeInMillis() ? -1
		        	         : cal1.getTimeInMillis() > cal2.getTimeInMillis()? 1
		        	         : 0;*/
		        }
		    });
	}
        
        /**
	 * Ordena los movimientos por fecha
	 * @param movimientos
	 */
	public  static void sortMovimientosByDateAsiento(ArrayList<Movimiento> movimientos,
                boolean ascending) {
		
		final Calendar cal1 = Calendar.getInstance();
		final Calendar cal2 = Calendar.getInstance();
                
		//Movimiento.printMovimientos("antes de ordenar", movimientos, true);
                
		Collections.sort(movimientos, new Comparator<Movimiento>() {

                    public int compare(Movimiento m1, Movimiento m2) {
                        m1.getCalendar(cal1);
                        m2.getCalendar(cal2);

                        int result = cal1.compareTo(cal2);
                        if (!ascending)
                            result = cal2.compareTo(cal1);                                

                        if (result==0){
                            if (m1.getNumAsiento()>m2.getNumAsiento())
                                result = -1;
                            else if (m1.getNumAsiento()<m2.getNumAsiento())                                       
                                result = 1;
                            else result = 0;    

                            if (!ascending){
                                if (m2.getNumAsiento()>m1.getNumAsiento())
                                    result = -1;
                                else if (m2.getNumAsiento()<m1.getNumAsiento())                                       
                                    result = 1;
                                else result = 0;    
                            }
                        }
                        return result;
                        /*return cal1.getTimeInMillis() < cal2.getTimeInMillis() ? -1
                                 : cal1.getTimeInMillis() > cal2.getTimeInMillis()? 1
                                 : 0;*/
                    }
		});
                //Movimiento.printMovimientos("despues de ordenar", movimientos, true);
	}
	
	/**
	 * Ordena los movimientos por fecha
	 * @param movimientos
	 */
	public  static void sortMovimientosByDate(ArrayList<Movimiento> movimientos,final boolean debug) {
	
		final Calendar cal1 = Calendar.getInstance();
		final  Calendar cal2 = Calendar.getInstance();
		
		Collections.sort(movimientos, new Comparator<Movimiento>() {

		        public int compare(Movimiento m1, Movimiento m2) {
		        	
		        	String msg1 = "[1] "+m1.toStringDate("")+" vs "+m2.toStringDate("")+"\n";
		        	
		        	m1.getCalendar(cal1);
		        	m2.getCalendar(cal2);
		        	String msg2 = "[2] "+cal1.get(Calendar.DAY_OF_MONTH)+"-"+cal1.get(Calendar.MONTH)+"-"+cal1.get(Calendar.YEAR)
		        			+" vs "
		        			+cal2.get(Calendar.DAY_OF_MONTH)+"-"+cal2.get(Calendar.MONTH)+"-"+cal2.get(Calendar.YEAR)
		        			+" || "+cal1.getTimeInMillis()+" vs "+cal2.getTimeInMillis()
		        			+"\n";
		        	String msg3 = msg1+" "+msg2;
		        	
		        	if (debug)
		        		System.out.println(msg3);
		        	
		        	if (cal1==null){
		        		return -1;
		        	}
		        	if (cal2==null){
		        		return 1;
		        	}

		        	long millis1 = cal1.get(Calendar.YEAR)*365+cal1.get(Calendar.DAY_OF_YEAR);
		        	long millis2 = cal2.get(Calendar.YEAR)*365+cal2.get(Calendar.DAY_OF_YEAR);
		        	
		        	if (millis1 == millis2) {
		        		return 0;
		        	}
		        	
		        	return Long.compare(cal1.getTimeInMillis(),cal2.getTimeInMillis());
		        }
		    });
	}
	
	public static void printMovimientos(String header,ArrayList<Movimiento> movimientos,boolean isWithDate){
		System.out.println(header);
		for (Movimiento m:movimientos){
			if (isWithDate)
				System.out.println(m.toStringDate(header));
			else
				System.out.println(m.toString());
		}
	}
        
        public static void printMovimientos(String header,ArrayList<Movimiento> movimientos,int begin,int end){
		System.out.println(header);
		for (int i=begin;i<=end;i++){
                    Movimiento m = movimientos.get(i);
                    System.out.println(m.toStringDate(header));			
		}
	}
}
