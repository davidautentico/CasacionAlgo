package s2a.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import s2a.JarSettings;
import s2a.Movimiento;

import s2a.SubAccount;
import s2a.threading.CallableCasarSubcuenta;
import s2a.utils.MathUtils;

public class FileUtils {
	
	
    public static void writeDurations(JarSettings jar,
            LinkedHashMap<Long,String> refDurations) throws FileNotFoundException, 
            UnsupportedEncodingException,
            IOException{
    
        FileWriter writer = null;
        File archivo = null;
        FileReader fr = null;
        BufferedReader br = null;
		
        if (!jar.isDelphiEncoding())
            br = new BufferedReader(new FileReader(jar.getInputCSV()));
        else{
            br = new BufferedReader(new InputStreamReader(new FileInputStream(jar.getInputCSV()), "UTF-16LE"));
        }
	 
        System.out.println("input output: "+jar.getInputCSV()
            +" "+jar.getOutputCSV());
	writer = new FileWriter(jar.getOutputCSV());
        archivo = new File (jar.getInputCSV());
	fr = new FileReader (archivo);
	// Lectura del fichero
	String lineStr;
        long line = 0;
        while((lineStr=br.readLine())!=null){
            line++;
            String extra = "-1_1_1;-1";
            if (refDurations.containsKey(line)){
                extra = refDurations.get(line);
            }
            String finalStr = lineStr+';'+extra;
            writer.write(finalStr); 
            writer.write("\n");                                   
	}    
        writer.close();
    }
    
    public static LinkedHashMap  readFileToMap(JarSettings jar,
            List<String> diaryList,
            int subInicio,int subFinal,
            boolean isAlreadyCasado,
            LinkedHashMap asientosToDelete,
            LinkedHashMap linesNotProcessed,
            LinkedHashMap linesSaldoAffected
             ){

        return readFileToMapSaldo(diaryList,
                jar.getStartLine(),jar.getSubAccountCol(),jar.getAsientoCol(),
                jar.getDebeCol(),jar.getHaberCol(),jar.getFechaCol(),jar.getCasadoCol(),
                subInicio,subFinal,isAlreadyCasado,jar.isDelphiEncoding(),
                asientosToDelete,linesNotProcessed,linesSaldoAffected);                                
    }
    
    public static LinkedHashMap  readFileToMap(JarSettings jar,
                    int subInicio,int subFinal,
                    boolean isAlreadyCasado,
                    LinkedHashMap asientosToDelete,
                    LinkedHashMap linesNotProcessed
                    ){

        return readFileToMap(jar.getInputCSV(),
                jar.getStartLine(),jar.getSubAccountCol(),jar.getAsientoCol(),
                jar.getDebeCol(),jar.getHaberCol(),jar.getFechaCol(),
                subInicio,subFinal,isAlreadyCasado,jar.isDelphiEncoding(),
                asientosToDelete,linesNotProcessed);                                
    }
    
    /**
     * Trabaja desde memoria y mantiene las lineas que afectan a saldo en
     * el hash linesSaldoAffected
     * @param diaryList
     * @param startLine
     * @param subAccountCol
     * @param numAsientoCol
     * @param debeCol
     * @param haberCol
     * @param dateCol
     * @param subInicio
     * @param subFinal
     * @param isAlreadyCasado
     * @param delphiEncoding
     * @param asientosToDelete
     * @param linesNotProcessed
     * @return 
     */
    public static LinkedHashMap  readFileToMapSaldo(List<String> diaryList,
			int startLine,
			int subAccountCol,int numAsientoCol,
                        int debeCol,int haberCol,int dateCol,int casadoCol,
			int subInicio,int subFinal,
			boolean isAlreadyCasado,boolean delphiEncoding,
			LinkedHashMap asientosToDelete,
			LinkedHashMap linesNotProcessed,
                        LinkedHashMap linesSaldoAffected
			){
		
        //System.out.println("[readFileToMap] entrado");
	LinkedHashMap asientosNotAllowed = new LinkedHashMap();
	LinkedHashMap failedSubAccounts = new LinkedHashMap();
	ArrayList<String> arrayStr = new ArrayList<String>();
	LinkedHashMap subaccounts = new LinkedHashMap();
	String str = "";
	int numCols = 0;
	Calendar cal = Calendar.getInstance();
	try {			
            SubAccount sub = null;
            int line = 0;
            for (int i=0;i<diaryList.size();i++){
                str = diaryList.get(i);
                if (str.trim().length()==0) continue;
                line++;
                if (line>=startLine){                                    
                    String[] values = str.split(";",-1);
                    //System.out.println("Line "+line+": "+str+" || "+values.length);
                    numCols = values.length;
                    String subAccountNum = values[subAccountCol];					
                    //if (!subAccountNum.equalsIgnoreCase("475101000000")) continue; //debug						
                    long numAsiento = -1;
                    if (numAsientoCol>=0 && numAsientoCol<=values.length-1){
                        String numAsientoStr = values[numAsientoCol].replaceAll("\\.", "");
			if (numAsientoStr.trim().length()==0) continue;
			numAsiento = Long.valueOf(numAsientoStr);
                    }

                    if (isAsientoToDelete(asientosToDelete,numAsiento)){
                        asientosNotAllowed.put(numAsiento, 1);
                        linesNotProcessed.put(line,1);
                        continue;
                    }                    
                    //si está identificada la columna de casado y casado=0 (saldo)
                    //se respeta esa linea como saldo
                    if (casadoCol!=-1 && numCols>=casadoCol-1){
                        int casado = Integer.valueOf(values[casadoCol]);
                        if (casado==0){//afecta a saldo y se guarda se continua
                            linesSaldoAffected.put(line, 1);
                            System.out.println("[LINEA AFECTADA DE SALDO "+line);
                            continue;
                        }
                    }
                    //true significa que estamos en modo antiguedad y ahi si se analizan los subgrupos                 					
                    if (isAlreadyCasado){
                        int subgroup = -1;
                        try{
                            subgroup = Integer.valueOf(values[subAccountCol].substring(0, 3));
                        }catch(Exception e){
                            //System.out.println("[WARNING readFileToMap] subgroup not integer: "+values[subAccountCol].substring(0, 3));
                            failedSubAccounts.put(subAccountNum, 1);
                            subgroup = -1;
                        }
                        if (subgroup<subInicio || subgroup> subFinal) continue;
                    }                    
					
                    //System.out.println("antes debe y haber col "+debeCol+" "+haberCol);
                    long debe = 0;
                    long haber = 0;
                    //System.out.println("debe: "+values[debeCol]+" "+values[debeCol].trim().length());
                    String debeStr = values[debeCol];
                    String haberStr = values[haberCol];
                    if (debeStr.trim().length()>0)
                        debe =  MathUtils.doubleStrToLong(debeStr.split(" ")[0],2);
                    if (haberStr.trim().length()>0)
                        haber =  MathUtils.doubleStrToLong(haberStr.split(" ")[0],2);

                    //System.out.println("llegado debe haber "+debe+" "+haber);

                    int casado = 0;
                    if (isAlreadyCasado)
                            casado = Integer.valueOf(values[values.length-1]);
                    int day = -1;
                    int month = -1;
                    int year = -1;

                    boolean isCasado=false;
                    if (casado==1){
                            isCasado = true;
                    }
					
                    if (dateCol!=-1){
                            //System.out.println("llegado decode date "+dateCol);
                            DecodificationUtils.decodeDate(cal, values[dateCol]);
                            day = cal.get(Calendar.DAY_OF_MONTH);
                            month = cal.get(Calendar.MONTH);
                            year = cal.get(Calendar.YEAR);
                            //System.out.println(day+"//"+month+"//"+year);
                    }
                    sub = (SubAccount) subaccounts.get(subAccountNum);
                    if (sub==null){
                            sub = new SubAccount();
                            sub.setCodSubCuenta(subAccountNum);
                            subaccounts.put(subAccountNum, sub);
                            System.out.println("añadiendo subcuenta6: "+subAccountNum);
                    }

                    //System.out.println("antes de addMovimiento");

                    sub.addMovimiento(line,numAsiento,debe,haber,day,month,year,isCasado);
                    sub.incTotalMovimientos();
                }//startLine
            }//for diaryList
        } catch (Exception e) {
            System.out.println("[readFileToMap] ERROR: "+e.getMessage()+" .Line: "+str+" || "+numCols);
            String[] values = str.split(";");
            for (int i=0;i<values.length;i++){
                    System.out.println("[readFileToMap] ERROR: column "+i+" : "+values[i]);
            }
            e.printStackTrace();
            return null;
        }
				
        Iterator<SubAccount> it = failedSubAccounts.values().iterator();
        while (it.hasNext()){
                                //&& i<=0//debug
                SubAccount sub = it.next();
                System.out.println("Cuenta "+sub.getCodSubCuenta()+" fallo en su procesado");
        }

        Set s = asientosNotAllowed.keySet();
        System.out.println("Asientos que no se casaran: "+s.toString());

        return subaccounts;
    }
    
    public static LinkedHashMap  readFileToMap(List<String> diaryList,
			int startLine,
			int subAccountCol,int numAsientoCol,
                        int debeCol,int haberCol,int dateCol,int casadoCol,
			int subInicio,int subFinal,
			boolean isAlreadyCasado,boolean delphiEncoding,
			LinkedHashMap asientosToDelete,
			LinkedHashMap linesNotProcessed
			){
		
        //System.out.println("[readFileToMap] entrado");
	LinkedHashMap asientosNotAllowed = new LinkedHashMap();
	LinkedHashMap failedSubAccounts = new LinkedHashMap();
	ArrayList<String> arrayStr = new ArrayList<String>();
	LinkedHashMap subaccounts = new LinkedHashMap();
	String str = "";
	int numCols = 0;
	Calendar cal = Calendar.getInstance();
	try {			
            SubAccount sub = null;
            int line = 0;
            for (int i=0;i<diaryList.size();i++){
                str = diaryList.get(i);
                if (str.trim().length()==0) continue;
                line++;
                if (line>=startLine){                                    
                    String[] values = str.split(";",-1);
                    //System.out.println("Line "+line+": "+str+" || "+values.length);
                    numCols = values.length;
                    String subAccountNum = values[subAccountCol];					
                    //if (!subAccountNum.equalsIgnoreCase("475101000000")) continue; //debug						
                    long numAsiento = -1;
                    if (numAsientoCol>=0 && numAsientoCol<=values.length-1){
                        String numAsientoStr = values[numAsientoCol].replaceAll("\\.", "");
			if (numAsientoStr.trim().length()==0) continue;
			numAsiento = Long.valueOf(numAsientoStr);
                    }

                    if (isAsientoToDelete(asientosToDelete,numAsiento)){
                        asientosNotAllowed.put(numAsiento, 1);
                        linesNotProcessed.put(line,1);
                        //System.out.println("Line "+line+" not processed. aSIENTO:"+numAsiento);
                        continue;
                    }
                    
                    //si la columna casado viene definida (!=-1), entonces 
                    //es que nos vienen los saldos desde arriba y estos no se 
                    //deben modificar
                    if (casadoCol!=-1){
                        
                    }
					
                    if (isAlreadyCasado){//true significa que estamos en modo antiguedad y ahi si se analizan los subgrupos
                        int subgroup = -1;
                        try{
                            subgroup = Integer.valueOf(values[subAccountCol].substring(0, 3));
                        }catch(Exception e){
                            //System.out.println("[WARNING readFileToMap] subgroup not integer: "+values[subAccountCol].substring(0, 3));
                            failedSubAccounts.put(subAccountNum, 1);
                            subgroup = -1;
                        }
                        if (subgroup<subInicio || subgroup> subFinal) continue;
                    }                    
					
                    //System.out.println("antes debe y haber col "+debeCol+" "+haberCol);
                    long debe = 0;
                    long haber = 0;
                    //System.out.println("debe: "+values[debeCol]+" "+values[debeCol].trim().length());
                    String debeStr = values[debeCol];
                    String haberStr = values[haberCol];
                    if (debeStr.trim().length()>0)
                        debe =  MathUtils.doubleStrToLong(debeStr.split(" ")[0],2);
                    if (haberStr.trim().length()>0)
                        haber =  MathUtils.doubleStrToLong(haberStr.split(" ")[0],2);

                    //System.out.println("llegado debe haber "+debe+" "+haber);

                    int casado = 0;
                    if (isAlreadyCasado)
                            casado = Integer.valueOf(values[values.length-1]);
                    int day = -1;
                    int month = -1;
                    int year = -1;

                    boolean isCasado=false;
                    if (casado==1){
                            isCasado = true;
                    }
					
                    if (dateCol!=-1){
                            //System.out.println("llegado decode date "+dateCol);
                            DecodificationUtils.decodeDate(cal, values[dateCol]);
                            day = cal.get(Calendar.DAY_OF_MONTH);
                            month = cal.get(Calendar.MONTH);
                            year = cal.get(Calendar.YEAR);
                            //System.out.println(day+"//"+month+"//"+year);
                    }
                    sub = (SubAccount) subaccounts.get(subAccountNum);
                    if (sub==null){
                            sub = new SubAccount();
                            sub.setCodSubCuenta(subAccountNum);
                            subaccounts.put(subAccountNum, sub);
                            System.out.println("añadiendo subcuenta3: "+subAccountNum);
                    }
                    //System.out.println("antes de addMovimiento");
                    sub.addMovimiento(line,numAsiento,debe,haber,day,month,year,isCasado);
                    sub.incTotalMovimientos();
                    //debug
                    //sub.printMovimientosDate();
                    //return null;
                    //
                }//startLine
            }//for diaryList
        } catch (Exception e) {
            System.out.println("[readFileToMap] ERROR: "+e.getMessage()+" .Line: "+str+" || "+numCols);
            String[] values = str.split(";");
            for (int i=0;i<values.length;i++){
                    System.out.println("[readFileToMap] ERROR: column "+i+" : "+values[i]);
            }
            e.printStackTrace();
            return null;
        }
				
        Iterator<SubAccount> it = failedSubAccounts.values().iterator();
        while (it.hasNext()){
                                //&& i<=0//debug
                SubAccount sub = it.next();
                System.out.println("Cuenta "+sub.getCodSubCuenta()+" fallo en su procesado");
        }

        Set s = asientosNotAllowed.keySet();
        System.out.println("Asientos que no se casaran: "+s.toString());

        return subaccounts;
    }
    
	public static LinkedHashMap  readFileToMap(String fileName,
			int startLine,
			int subAccountCol,int numAsientoCol,int debeCol,int haberCol,int dateCol,
			int subInicio,int subFinal,
			boolean isAlreadyCasado,boolean delphiEncoding,
			LinkedHashMap asientosToDelete,
			LinkedHashMap linesNotProcessed
			){
		
		//System.out.println("[readFileToMap] entrado");
		LinkedHashMap asientosNotAllowed = new LinkedHashMap();
		LinkedHashMap failedSubAccounts = new LinkedHashMap();
		ArrayList<String> arrayStr = new ArrayList<String>();
		LinkedHashMap<String,SubAccount> subaccounts = new LinkedHashMap<String,SubAccount>();
		String str = "";
		int numCols = 0;
		Calendar cal = Calendar.getInstance();
		try {
			BufferedReader in = null;
			
			if (!delphiEncoding)
				in = new BufferedReader(new FileReader(fileName));
			else{
				in =new BufferedReader( new InputStreamReader(new FileInputStream(fileName), "UTF-16LE"));
			}
			SubAccount sub = null;
			int line = 0;
			while ((str = in.readLine()) != null) {
				if (str.trim().length()==0) continue;
				line++;
				//System.out.println("Line "+line+": "+str);
				//if (line>=1000) break;
				if (line>=startLine
						//&& line<=10
						){
					String[] values = str.split(";",-1);
					//System.out.println("Line "+line+": "+str+" || "+values.length);
					numCols = values.length;
					String subAccountNum = values[subAccountCol];
					
					//if (!subAccountNum.equalsIgnoreCase("475101000000")) continue; //debug
						
					long numAsiento = -1;
					if (numAsientoCol>=0 && numAsientoCol<=values.length-1){
						String numAsientoStr = values[numAsientoCol].replaceAll("\\.", "");
						if (numAsientoStr.trim().length()==0) continue;
						numAsiento = Long.valueOf(numAsientoStr);
					}
                                        //System.out.println(numAsiento+" || "+str);
					if (isAsientoToDelete(asientosToDelete,(int)numAsiento)){
						asientosNotAllowed.put(numAsiento, 1);
						linesNotProcessed.put(line,1);
                                                //System.out.println("lineas que no se procesaran: "+line);
						continue;
					}
					
					if (isAlreadyCasado){//true significa que estamos en modo antiguedad y ahi si se analizan los subgrupos
						int subgroup = -1;
						try{
							subgroup = Integer.valueOf(values[subAccountCol].substring(0, 3));
						}catch(Exception e){
							//System.out.println("[WARNING readFileToMap] subgroup not integer: "+values[subAccountCol].substring(0, 3));
							failedSubAccounts.put(subAccountNum, 1);
							subgroup = -1;
						}
						if (subgroup<subInicio || subgroup> subFinal) continue;
					}
					
					//System.out.println("antes debe y haber col "+debeCol+" "+haberCol);
                                        long debe = 0;
                                        long haber = 0;
                                        //System.out.println("debe: "+values[debeCol]+" "+values[debeCol].trim().length());
                                        String debeStr = values[debeCol];
                                        String haberStr = values[haberCol];
                                        if (debeStr.trim().length()>0)
                                            debe =  MathUtils.doubleStrToLong(debeStr.split(" ")[0],2);
                                        if (haberStr.trim().length()>0)
                                            haber =  MathUtils.doubleStrToLong(haberStr.split(" ")[0],2);
					
					//System.out.println("llegado debe haber "+debe+" "+haber);
					
					int casado = 0;
					if (isAlreadyCasado)
						casado = Integer.valueOf(values[values.length-1]);
					int day = -1;
					int month = -1;
					int year = -1;
					
					boolean isCasado=false;
					if (casado==1){
						isCasado = true;
					}
					
					if (dateCol!=-1){
						//System.out.println("llegado decode date "+dateCol);
						DecodificationUtils.decodeDate(cal, values[dateCol]);
						day = cal.get(Calendar.DAY_OF_MONTH);
						month = cal.get(Calendar.MONTH);
						year = cal.get(Calendar.YEAR);
						//System.out.println(day+"//"+month+"//"+year);
					}
                                        sub=null;
                                        if (subaccounts.containsKey(subAccountNum)){
                                            sub = (SubAccount) subaccounts.get(subAccountNum);
                                            if (sub==null) System.out.println("es null  | "+line); 
                                        }else{
                                            System.out.println("no contiene  | "+line); 
                                        }
					if (sub==null){
						sub = new SubAccount();
						sub.setCodSubCuenta(subAccountNum);
						subaccounts.put(subAccountNum, sub);
                                                System.out.println("añadiendo subcuenta4: "+subAccountNum+" | "+line+" || "+subaccounts.size());
					}
					sub.addMovimiento(line,numAsiento,debe,haber,day,month,year,isCasado);
					sub.incTotalMovimientos();
				}
			}
			in.close();
			//writeToFile("c:\\AS_2014_5201001.csv",arrayStr);
		} catch (Exception e) {
			System.out.println("[readFileToMap] ERROR: "+e.getMessage()+" .Line: "+str+" || "+numCols);
			String[] values = str.split(";");
			for (int i=0;i<values.length;i++){
				System.out.println("[readFileToMap] ERROR: column "+i+" : "+values[i]);
			}
			e.printStackTrace();
			return null;
		}
		
		
		Iterator<SubAccount> it = failedSubAccounts.values().iterator();
		while (it.hasNext()){
					//&& i<=0//debug
			SubAccount sub = it.next();
			System.out.println("Cuenta "+sub.getCodSubCuenta()+" fallo en su procesado");
		}
		
		Set s = asientosNotAllowed.keySet();
		System.out.println("Asientos que no se casaran: "+s.toString());
		
		return subaccounts;
	}

	public static LinkedHashMap  readFileToMap(String fileName,
			int startLine,
			int subAccountCol,int numAsientoCol,int debeCol,int haberCol,int dateCol,int nombreCol,
			int subInicio,int subFinal,
			Calendar startDate,Calendar endDate,
			boolean isAntiguedadMode,boolean delphiEncoding,
			int asientoApertura,
			LinkedHashMap saldosApertura,
			LinkedHashMap asientosToDelete,
			LinkedHashMap linesNotProcessed
			){
		
		//System.out.println("[readFileToMap] entrado");
		LinkedHashMap asientosNotAllowed = new LinkedHashMap();
		LinkedHashMap failedSubAccounts = new LinkedHashMap();
		ArrayList<String> arrayStr = new ArrayList<String>();
		LinkedHashMap subaccounts = new LinkedHashMap();
		String str = "";
		int numCols = 0;
		Calendar cal = Calendar.getInstance();
		try {
			BufferedReader in = null;
			
			if (!delphiEncoding)
				in = new BufferedReader(new FileReader(fileName));
			else{
				in =new BufferedReader( new InputStreamReader(new FileInputStream(fileName), "UTF-16LE"));
			}
			SubAccount sub = null;
			int line = 0;
			while ((str = in.readLine()) != null) {
				if (str.trim().length()==0) continue;
				line++;
				//System.out.println("Line "+line+": "+str);
				//if (line>=1000) break;
				if (line>=startLine
						//&& line<=10
						){
					String[] values = str.split(";",-1);
					//System.out.println("Line "+line+": "+str+" || "+values.length);
					numCols = values.length;
					String subAccountNum	= values[subAccountCol];
					String nombreSubCuenta	= values[nombreCol];
					long numAsiento = -1;
					if (numAsientoCol>=0 && numAsientoCol<=values.length-1){
						String numAsientoStr = values[numAsientoCol].replaceAll("\\.", "");
						if (numAsientoStr.trim().length()==0) continue;
						numAsiento = Long.valueOf(numAsientoStr);
					}

					//filtro por fecha
					int day = -1;
					int month = -1;
					int year = -1;
					if (dateCol!=-1){
						//System.out.println("llegado decode date "+dateCol);
						DecodificationUtils.decodeDate(cal, values[dateCol]);
						day = cal.get(Calendar.DAY_OF_MONTH);
						month = cal.get(Calendar.MONTH);
						year = cal.get(Calendar.YEAR);                                                
						if (cal.getTimeInMillis()<startDate.getTimeInMillis() 
								|| cal.getTimeInMillis()>endDate.getTimeInMillis()) continue; //se filtra si la fecha no se encuentra startdate-enddate
						//System.out.println(day+"//"+month+"//"+year);
					}
					
					//filtro del asiento
					if (isAsientoToDelete(asientosToDelete,numAsiento)){
						asientosNotAllowed.put(numAsiento, 1);
						linesNotProcessed.put(line,1);
						continue;
					}
					
					if (isAntiguedadMode){//true significa que estamos en modo antiguedad y ahi si se analizan los subgrupos
						int subgroup = -1;
						try{
							subgroup = Integer.valueOf(values[subAccountCol].substring(0, 3));
						}catch(Exception e){
							//System.out.println("[WARNING readFileToMap] subgroup not integer: "+values[subAccountCol].substring(0, 3));
							failedSubAccounts.put(subAccountNum, 1);
							subgroup = -1;
						}
						if (subgroup<subInicio || subgroup> subFinal) continue;
						//a�adimos comilla a cada subcuenta para su precesado en modo antiguedad
						subAccountNum = "'"+subAccountNum;
					}
					
					//System.out.println("antes debe y haber col "+debeCol+" "+haberCol);
					long debe =  MathUtils.doubleStrToLong(values[debeCol].split(" ")[0],2);
					long haber = MathUtils.doubleStrToLong(values[haberCol].split(" ")[0],2);
					
					//System.out.println("llegado debe haber "+debe+" "+haber);
					
					int casado = 0;
					if (isAntiguedadMode)
						casado = Integer.valueOf(values[values.length-1]);
					
					
					boolean isCasado=false;
					if (casado==1){
						isCasado = true;
					}
										
					sub = (SubAccount) subaccounts.get(subAccountNum);
					if (sub==null){
						//System.out.println("A�ADIDA SUBCUENTA: "+subAccountNum);
						sub = new SubAccount();
						sub.setCodSubCuenta(subAccountNum);
						sub.setNombreSubCuenta(nombreSubCuenta);
						subaccounts.put(subAccountNum, sub);
                                                System.out.println("añadiendo subcuenta5: "+subAccountNum);
					}
					//actualizamos saldo de apertura
					if (numAsiento==asientoApertura && numAsiento!=-1){
						if (!saldosApertura.containsKey(subAccountNum)){//creo la clave si no existe
							saldosApertura.put(subAccountNum, 0L);
							//System.out.println("saldo apertura cuenta "+subAccountNum +": "+0);
							//System.out.println("a�adido cuenta para saldo apertura: "+subAccountNum);
						}
						
						if (casado==0){ //no est� casado, actualizamos su saldo
							long saldo = (long) saldosApertura.get(subAccountNum)+debe-haber;
							saldosApertura.put(subAccountNum, saldo);
							sub.setSaldoApertura(saldo);
							//if (subAccountNum.equalsIgnoreCase("4300000000")){
								//System.out.println("saldo apertura cuenta "+subAccountNum +": "+saldo);
							//}
						}
						//if (subAccountNum.equalsIgnoreCase("4300000000")){
						//System.out.println("saldo cuenta apertura "+subAccountNum+" : "+sub.getSaldoApertura());
						//}
					}else{//no a�adimos movimientos pertenecientes a la apertura
						sub.addMovimiento(line,numAsiento,debe,haber,day,month,year,isCasado);
						sub.incTotalMovimientos();
					}
					//debug
					//sub.printMovimientosDate();
					//return null;
					//
				}
			}
			in.close();
			//writeToFile("c:\\AS_2014_5201001.csv",arrayStr);
		} catch (Exception e) {
			System.out.println("[readFileToMap] ERROR: "+e.getMessage()+" .Line: "+str+" || "+numCols);
			String[] values = str.split(";");
			for (int i=0;i<values.length;i++){
				System.out.println("[readFileToMap] ERROR: column "+i+" : "+values[i]);
			}
			e.printStackTrace();
			return null;
		}
		
		
		Iterator<SubAccount> it = failedSubAccounts.values().iterator();
		while (it.hasNext()){
					//&& i<=0//debug
			SubAccount sub = it.next();
			System.out.println("Cuenta "+sub.getCodSubCuenta()+" fall� en su procesado");
		}
		
		Set s = asientosNotAllowed.keySet();
		System.out.println("Asientos que no se casar�n: "+s.toString());
		
		//debug
		//printSaldos(saldosApertura);
		
		return subaccounts;
	}

	
	private static void printSaldos(LinkedHashMap saldosApertura) {
		Set<String> keys  = saldosApertura.keySet();
		
		for (String key: keys){
			System.out.println("[printSaldos] "+key+" "+saldosApertura.get(key));
		}
	}

	private static boolean isAsientoToDelete(LinkedHashMap asientosToDelete,
			long numAsiento) {

                //System.out.println("[isAsientoToDelete] "+asientosToDelete.toString()
                        //+" "+numAsiento+" "+asientosToDelete.get(numAsiento)+" "+asientosToDelete.get(7934));
		//if (asientosToDelete.get(numAsiento)!=null){
                if (asientosToDelete.containsKey(numAsiento)){
                     //System.out.println("[isAsientoToDelete] "+numAsiento +" true");
                    return true;
                }
		
		return false;
	}
        
        /**
	 * Escritura en disco con referencia de casamiento
	 * @param outputCSV
	 * @param subAccounts
	 * @param linesToKeep
	 * @throws IOException 
	 */
	public static void writeCSV(String inputCSV,String outputCSV,
			LinkedHashMap linesToKeep,
			LinkedHashMap linesNotProcessed,
                        LinkedHashMap linesSaldoAffected,
                        LinkedHashMap linesReferences,                        
			boolean delphiEncoding) throws IOException {
		
            FileWriter writer = null;
            File archivo = null;
            FileReader fr = null;
            BufferedReader br = null;
		
            if (!delphiEncoding)
                br = new BufferedReader(new FileReader(inputCSV));
            else{
		br =new BufferedReader( new InputStreamReader(new FileInputStream(inputCSV), "UTF-16LE"));
            }
	 
	    writer = new FileWriter(outputCSV);
	    archivo = new File (inputCSV);
	    fr = new FileReader (archivo);
	    // Lectura del fichero
	    String lineStr;
	    long line = 0;
	    while((lineStr=br.readLine())!=null){
                line++;
                boolean isWritten = false;
                boolean isCasado = false;
                String extra = "";
                if (linesNotProcessed.containsKey(line)){
                    extra+=";1";//CASADO
                    isWritten=true;
                    isCasado = true;
                }
                //lineas afectadas de saldo que provienen de casación inicial
                if (linesSaldoAffected!=null
                        && linesSaldoAffected.containsKey(line)){
                    extra+=";0";//SALDO-> NO CASADO
                    isWritten=true;
                    isCasado = false;
                }
                if (!isWritten){
                    if (linesToKeep.containsKey(line)){
                        boolean isCorrect = (boolean) linesToKeep.get(line);
                        if (isCorrect){//afectan a saldo-> casado = 0;
                            extra+=";0";
                            isWritten = true;
                            isCasado = false;
                        }else{
                            extra+=";1";
                            isWritten = true;
                            isCasado = true;
                        }
                    }
                    if (!isWritten){
                        extra+=";1";//casado 
                        isWritten = true;
                        isCasado = true;
                    }
                }
                //si es necesaria la referencia 
                //se anota el tipo y la referenciata tipo;
                if (linesReferences!=null){
                    if (linesReferences.containsKey(line)){
                        String referenceStr = (String)linesReferences.get(line);   
                        extra+=";"+referenceStr.split("_")[0]+
                                ";"+referenceStr.split("_")[1];
                    }else{
                        if (isCasado){
                            extra+=";0;0"; //casado;tipo;sin referencia, saldo subcuenta = 0                    
                        }else{//no casado
                            extra+=";-1;-1";
                        } //no casado;sin tipo;sin referencia (0)
                    }
                }
                //escribimos a disco
                String finalStr = lineStr+extra;
                System.out.println(line+" || "+extra);
                writer.write(finalStr); //afecta a saldo
                writer.write("\n"); 
           }           
	   writer.close();
	}

	/**
	 * Pendiente la escritura real a disco
	 * @param outputCSV
	 * @param subAccounts
	 * @param linesToKeep
	 * @throws IOException 
	 */
	public static void writeCSV(String inputCSV,String outputCSV,
			LinkedHashMap linesToKeep,
			LinkedHashMap linesNotProcessed,
			boolean delphiEncoding) throws IOException {
		
		FileWriter writer = null;
		File archivo = null;
                FileReader fr = null;
                BufferedReader br = null;
		
		if (!delphiEncoding)
			br = new BufferedReader(new FileReader(inputCSV));
		else{
			br =new BufferedReader( new InputStreamReader(new FileInputStream(inputCSV), "UTF-16LE"));
		}
	 
	    	writer = new FileWriter(outputCSV);
	        archivo = new File (inputCSV);
	        fr = new FileReader (archivo);
	        // Lectura del fichero
	        String lineStr;
	        int line = 0;
	        while((lineStr=br.readLine())!=null){
	        	line++;
	        	boolean isWritten = false;
	        	if (linesNotProcessed.containsKey(line)){
	        		writer.write(lineStr+";1"); //not processed
					writer.write("\n");
					continue;
	        	}
	            if (linesToKeep.containsKey(line)){
					boolean isCorrect = (boolean) linesToKeep.get(line);
					if (isCorrect){
						//escribir a fichero de salida (pendiente)
						 writer.write(lineStr+";0"); //afecta a saldo
						 writer.write("\n");
						 isWritten = true;
					}else{
						writer.write(lineStr+";1"); //casado
						writer.write("\n");
						isWritten = true;
					}
				}
	            if (!isWritten){
	            	writer.write(lineStr+";1"); //tipocasado
					writer.write("\n");
					isWritten = true;
	            }
	         }
           
	       writer.close();
	}
	
	/**
	 * 
	 * @param strings
	 * @throws IOException
	 */
	public static void writeCSV(String outputCSV,ArrayList<String> strings) throws IOException {
		
		FileWriter writer = null;
	    writer = new FileWriter(outputCSV);
	    
	    for (int i=0;i<strings.size();i++){
	    	String str = strings.get(i);
	    	writer.write(str);
			writer.write("\n");
	    }
	    writer.close();
	}
	
	public static ArrayList<String> readFileToArray(String fileName){
		
		ArrayList<String> csv = new ArrayList<String>();
		try {
		    BufferedReader in = new BufferedReader(new FileReader(fileName));
		    String str;
		    while ((str = in.readLine()) != null) {
		        csv.add(str);
		    }
		    in.close();
		} catch (IOException e) {
		}
		return csv;
	}

 
}
