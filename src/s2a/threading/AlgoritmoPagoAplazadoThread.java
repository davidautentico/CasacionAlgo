/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s2a.threading;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import s2a.Casamiento;
import s2a.CasamientoMode;
import s2a.JarSettings;
import s2a.SubAccount;
import s2a.utils.DateUtils;
import s2a.utils.DecodificationUtils;
import s2a.utils.FileUtils;
import s2a.utils.MathUtils;
import s2a.utils.PrintUtils;
import s2a.utils.Registry;

/**
 *
 * @author PC01
 */
public class AlgoritmoPagoAplazadoThread {
    
    private int debeHaberIndex = -1;
    
        
    private void fillDurations(String sub_ref,LinkedHashMap<Long, String> durations, 
             ArrayList<Long> lines,
             short value) {
        for (int i=0;i<lines.size();i++){
           long key = lines.get(i);
           String sub_ref_value = sub_ref+";"+value;
           durations.put(key,sub_ref_value);
        }
    }
     
    private void fillDurations(String sub_ref,LinkedHashMap<Long, String> durations, 
            ArrayList<Long> lines,ArrayList<Short> values) {
          
        for (int i=0;i<lines.size();i++){
           long key = lines.get(i);
           short value = values.get(i);
           String sub_ref_value = sub_ref+";"+value;
           durations.put(key,sub_ref_value);
        }
    }
     
     private int calculateDaysDiff1vs1(List<String> diaryList, int line1, int line2,
            Calendar cal1,Calendar cal2,
            JarSettings jarSettings) {
        
        String line1str = diaryList.get(line1);
        String line2str = diaryList.get(line2);
        DecodificationUtils.decodeDate(cal1, line1str.split(";",-1)[jarSettings.getFechaCol()]);
        DecodificationUtils.decodeDate(cal2, line2str.split(";",-1)[jarSettings.getFechaCol()]);
        int days1 = cal1.get(Calendar.YEAR)*365+cal1.get(Calendar.DAY_OF_YEAR);
        int days2 = cal2.get(Calendar.YEAR)*365+cal2.get(Calendar.DAY_OF_YEAR);        
        return Math.abs(days1-days2);        
    }
     
    private ArrayList<Short> calculateDaysDiff1vsN(List<String> diaryList, ArrayList<Long> lines,
            Calendar cal1,Calendar cal2,
            JarSettings jarSettings) {
         
        ArrayList<Short> values = new ArrayList<Short>();   
        for (int i=0;i<lines.size();i++){
            values.add((short)-1);
        }
        //calculamos fecha del indice unico
        long lineNumDHIndex = lines.get(debeHaberIndex); 
        String lineDHstr = diaryList.get((int)lineNumDHIndex-1); 
        DecodificationUtils.decodeDate(cal1, lineDHstr.split(";",-1)[jarSettings.getFechaCol()]);
        int days1 = cal1.get(Calendar.YEAR)*365+cal1.get(Calendar.DAY_OF_YEAR);
        int maxDiff = 0;
        //System.out.println("debe haber Index: "+lineDHstr+" || "+(debeHaberIndex+1));
        //para cada movimiento calculamos su diferencia con el unico
        for (int i=0;i<lines.size();i++){
            long lineNum = lines.get(i);
            String line2Str = diaryList.get((int) (lineNum-1));
            
            if (i!=debeHaberIndex){
                DecodificationUtils.decodeDate(cal2, line2Str.split(";",-1)[jarSettings.getFechaCol()]);
                int days2 = cal2.get(Calendar.YEAR)*365+cal2.get(Calendar.DAY_OF_YEAR);
                int diff = Math.abs(days1-days2);
                values.set(i, (short)diff);
                if (Math.abs(days2-days1)>=maxDiff) maxDiff = Math.abs(days2-days1);
                //System.out.println("line days1 days2: "+line2Str+" || "+(lineNum)+" "+days1+" "+days2);
            }
        }
        //colocamos la diferencia mayor en el lado del que tenemos 1 movimiento
        values.set(debeHaberIndex, (short)maxDiff);
        
        return values;        
    }
     
    private ArrayList<Long> getImporteLines(List<String> diaryList,
            ArrayList<Long> lines,
            JarSettings jarSettings
            ){
        
        ArrayList<Long> importes = new ArrayList<Long>();        
        for (int i=0;i<lines.size();i++){
            long line = lines.get(i);           
            long importe = getImporteLine(diaryList,line,jarSettings);
            importes.add(importe);
        }
        
        return importes;
    }
    
    private long getImporteLine(List<String> diaryList,
            long line,
            JarSettings jarSettings) {
        
        long importe = -99999999;
        String lineStr = diaryList.get((int)line-1);
        long debe = 0;
        long haber = 0;
        String debeStr  = lineStr.split(";",-1)[jarSettings.getDebeCol()];
        String haberStr = lineStr.split(";",-1)[jarSettings.getHaberCol()];
        if (debeStr.trim().length()>0)
            debe =  MathUtils.doubleStrToLong(debeStr.split(" ")[0],2);
        if (haberStr.trim().length()>0)
            haber =  MathUtils.doubleStrToLong(haberStr.split(" ")[0],2);
                    
        return debe-haber;
    }

    private void processNM(String sub_ref,List<String> diaryList,
            ArrayList<Long> lines,
            Calendar cal1,Calendar cal2,
            JarSettings jarSettings, 
            LinkedHashMap<Long, String> durations) {
        
        //ordenamos en ascendente:( ej. 100,2500,4686)
        //Collections.sort(lines);
        //sort lines por fecha
        lines.sort(new Comparator<Long>() {
                    @Override
                    public int compare(Long o1, Long o2) {
                        int l1 = o1.intValue()-1;
                        int l2 = o2.intValue()-1;
                        String o1Str = diaryList.get(l1);
                        String o2Str = diaryList.get(l2);
                        DecodificationUtils.decodeDate(cal1, o1Str.split(";",-1)[jarSettings.getFechaCol()]);
                        DecodificationUtils.decodeDate(cal2, o2Str.split(";",-1)[jarSettings.getFechaCol()]);
                        cal1.set(Calendar.HOUR_OF_DAY,0);
                        cal1.set(Calendar.MINUTE,0);
                        cal1.set(Calendar.SECOND,0);
                        cal2.set(Calendar.HOUR_OF_DAY,0);
                        cal2.set(Calendar.MINUTE,0);
                        cal2.set(Calendar.SECOND,0);
                        return cal1.compareTo(cal2);
                    }
                });
        //System.out.println("[processNM] lines sorted");
        //conseguimos los importes de estas lineas
        ArrayList<Long> importeLines = getImporteLines(diaryList,lines,jarSettings);
        int i = 0;
        long importeLine_i = 0;
        long importeLine_j = 0;
        while (lines.size()>0){
            long line_i = lines.get(i);
            //long importeLine_i = getImporteLine(diaryList,line_i,jarSettings);
            importeLine_i = importeLines.get(i);
            int sign_i = MathUtils.sigNum(importeLine_i);
            int j = i+1;
            boolean i_removed = false;
            //System.out.println("Importe_i a eliminar: "+importeLine_i+" || "+diaryList.get((int)line_i-1));
            while (!i_removed && j<lines.size()){//mientras no se borre la linea i,avanzamos
                long line_j = lines.get(j);
                //long importeLine_j = getImporteLine(diaryList,line_j,jarSettings);
                importeLine_i = importeLines.get(i);
                importeLine_j = importeLines.get(j);
                int sign_j = MathUtils.sigNum(importeLine_j);
                //System.out.println(importeLine_i+" "+importeLine_j+ " || "+sign_i+" "+sign_j);
                if (sign_i==sign_j){
                    j++;
                    continue;
                }//si es del mismo signo buscamos el siguiente
                //signos distnos debe vs haber o haber vs debe
                long amountLeft = Math.abs(importeLine_i)-Math.abs(importeLine_j);
                //System.out.println(importeLine_i+" "+importeLine_j+ " || "+amountLeft);
                //importe_i eliminado, calculamos duracion i
                //y actualizamos importe_j
                int duracion_i = -1;
                int duracion_j = -1;
                int days = calculateDaysDiff1vs1(diaryList,
                            (int)line_i-1,(int)line_j-1,
                            cal1,cal2,jarSettings);  
                //System.out.println("DAYS: "+days);
                //vemos cual hay que ajustar
                if (amountLeft<=0){//borrado de i
                    duracion_i = days;                     
                    if (amountLeft==0){//borrado de j
                        duracion_j = days;
                    }
                    //actualizacion importes i,j
                    importeLines.set(i, 0L);
                    importeLines.set(j, (importeLine_i+importeLine_j));
                    //System.out.println("(debe<haber)actualizacion i j: "+0+" "+(importeLine_i+importeLine_j));
                }else{ //calculamos solo distancia j, que se elimina 
                    duracion_j = days;
                    //actualizacion importes i,j
                    importeLines.set(i, (importeLine_i+importeLine_j));
                    importeLines.set(j, 0L);
                    //System.out.println("(haber>debe)actualizacion i j: "+(importeLine_i+importeLine_j)+" "+0);
                }
                //borrado de los importes
                boolean j_removed = false;
                if (duracion_j>=0){//borrado de j
                    //System.out.println("[processNM] importe_j removed: "+importeLine_j+" || "+line_j);
                    lines.remove(j);
                    importeLines.remove(j);
                    durations.put(line_j, sub_ref+";"+duracion_j);
                    j_removed = true;
                }
                if (duracion_i>=0){//borrado de i
                    //System.out.println("[processNM] importe_i removed: "+importeLine_i+" || "+line_i);
                    lines.remove(i);
                    importeLines.remove(i);
                    durations.put(line_i, sub_ref+";"+duracion_i);
                    i_removed = true;
                }
                if (!j_removed) j++;
            }  
            if (!i_removed){
                System.out.println("ERROR I NO REMOVED");
                break;
            }
        }
    }
     
   //Se calcula la distancia en horas del saldo con el dia actual   
    private void fillSaldoLines(List<String> diaryList, 
            LinkedHashMap<Long, String> durations, JarSettings jarSettings) {
        
        Calendar cal = Calendar.getInstance();
        String refDate = jarSettings.getRefFecha();
        DecodificationUtils.decodeDate(cal, refDate);
        int days1 = cal.get(Calendar.YEAR)*365+cal.get(Calendar.DAY_OF_YEAR);
        for (int i=0;i<diaryList.size();i++){
            long line = i+1;
            if (line<jarSettings.getStartLine()) continue;
            String lineStr = diaryList.get(i);
            if (!durations.containsKey(line)){//pertenece a saldo
                //System.out.println(lineStr+" "+jarSettings.getFechaCol());
                DecodificationUtils.decodeDate(cal, lineStr.split(";",-1)[jarSettings.getFechaCol()]);
                int days2 = cal.get(Calendar.YEAR)*365+cal.get(Calendar.DAY_OF_YEAR);
                int diffDays = Math.abs(days2-days1);
                String extra = "-1_-1_-1;"+diffDays;
                durations.put(line, extra);
            }
        }
    }
    
    private CasamientoMode countDebeHaberLines(List<String> diaryList,
            ArrayList<Long> lines,JarSettings jarSettings){
       
        int debeCount = 0;
        int haberCount = 0;
        int indexDebe = -1;
        int indexHaber = -1;
        for (int i=0;i<lines.size();i++){
            long lineNum = lines.get(i);
            String lineStr = diaryList.get((int) (lineNum-1));           
            String[] values = lineStr.split(";",-1);
            long debe = 0;
            long haber = 0;
            String debeStr  = values[jarSettings.getDebeCol()];
            String haberStr = values[jarSettings.getHaberCol()];
            if (debeStr.trim().length()>0)
                debe =  MathUtils.doubleStrToLong(debeStr.split(" ")[0],2);
            if (haberStr.trim().length()>0)
                haber =  MathUtils.doubleStrToLong(haberStr.split(" ")[0],2);
            
            if (debe>0 && haber==0){
                indexDebe = i;
                debeCount++;
            }
            if (debe==0 && haber>0){
                indexHaber = i;
                haberCount++;
            }
            if (debe>0 && haber>0){
                //System.out.println("FAILLmas "+lineNum+" || "+lineStr);
            }
            if (debe==0 && haber==0){
                //System.out.println("FAILLzero "+lineNum+" || "+lineStr);
            }
        }
        
        if (debeCount==1 && haberCount==1) return CasamientoMode.ONE_TO_ONE;
        if (debeCount==1 && haberCount>1){
            debeHaberIndex = indexDebe;
            return CasamientoMode.ONE_TO_N;
        }
        if (debeCount>1 && haberCount==1){
            debeHaberIndex = indexHaber;
            return CasamientoMode.N_TO_ONE;
        }
        if (debeCount>1 && haberCount>1) return CasamientoMode.N_TO_M;
        if (debeCount==0 && haberCount==0){
            //System.out.println("CASO ZERO: "+debeCount+" "+haberCount);
            return CasamientoMode.ZERO_TO_ZERO;
        }
        
        System.out.println("CASO NONE: "+debeCount+" "+haberCount);
        return CasamientoMode.NONE;
    }
    
    /**
     * Calcula la distancia en días por cada casamiento (sub_ref)
     * @param diaryList
     * @param casamientos 
     */
    private void executeCoreProcess(List<String> diaryList, 
             LinkedHashMap<String, ArrayList<Long>> casamientos,
             JarSettings jarSettings,
             LinkedHashMap<Long,String> durations) {
        
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        int totalCasamientos = 0;
        int totalCasamientos_none = 0;
        int totalCasamientos_0_0 = 0;
        int totalCasamientos_1_1 = 0;
        int totalCasamientos_1_n = 0;
        int totalCasamientos_n_1 = 0;     
        int totalCasamientos_n_m = 0; 
        int totalLinesNM = 0;
        int totalReduccionNM1 = 0;
        int tipo0 = 0;
        for (Entry<String, ArrayList<Long>> entry : casamientos.entrySet()) {            
            String sub_ref  = entry.getKey();         
            ArrayList<Long> lines = entry.getValue();
            String tipo = sub_ref.split("_")[1];  
            totalCasamientos++;
            //System.out.println("Procesando casamiento: "+totalCasamientos);
            //1) averiguar en que caso estamos, contabilizamos el numero de movimientos
            //en el debe y en el haber
            debeHaberIndex = -1;
            CasamientoMode casamientoMode = countDebeHaberLines(diaryList,lines,jarSettings);
          // System.out.println(sub_ref+" "+casamientoMode.name());
            
            //EN EL CASO ZERO,el numero de dias se pone a -1 porque
            //realmento no hay casamiento
            if (casamientoMode==CasamientoMode.ZERO_TO_ZERO){
                fillDurations(sub_ref,durations,lines,(short)-1);
                //("CASO ZERO: "+sub_ref);
                totalCasamientos_0_0++;
                continue;
            }
            //EN EL CASO ONE_TO_ONE el número de dias es la resta inmediata
            if (casamientoMode==CasamientoMode.ONE_TO_ONE){
                int diffDays = calculateDaysDiff1vs1(diaryList,
                        (int)(lines.get(0)-1),(int)(lines.get(1)-1),cal1,cal2,jarSettings);
                
                fillDurations(sub_ref,durations,lines,(short)diffDays);
                totalCasamientos_1_1++;
                continue;
            }            
            //EN EL CASO ONE_TO_N el número de dias es la resta inmediata
            if (casamientoMode==CasamientoMode.ONE_TO_N){
                ArrayList<Short>  diffDays= calculateDaysDiff1vsN(diaryList,
                        lines,cal1,cal2,jarSettings);
                
                fillDurations(sub_ref,durations,lines,diffDays);              
                totalCasamientos_1_n++;
                continue;
            }
            if (casamientoMode==CasamientoMode.N_TO_ONE){
                //System.out.println("[executeCoreProcess] Procesando N_1 lines: "+lines.size());
                ArrayList<Short>  diffDays= calculateDaysDiff1vsN(diaryList,
                        lines,cal1,cal2,jarSettings);
                
                fillDurations(sub_ref,durations,lines,diffDays);              
                totalCasamientos_n_1++;
                continue;
            }
            //caso conflictivo,pendiente de calcular
            if (casamientoMode==CasamientoMode.N_TO_M){
               // System.out.println("[executeCoreProcess] Procesando N_M lines: "+lines.size());
                processNM(sub_ref,diaryList,lines,cal1,cal2,jarSettings,durations);               
                totalCasamientos_n_m++;
                continue;
            }
            
            if (casamientoMode==CasamientoMode.NONE){
               // System.out.println("CASO NONE: "+sub_ref);
                totalCasamientos_none++;
            }                       
        }
              
        //calculo distancias de partidas que forman el saldo (-1)
        fillSaldoLines(diaryList,durations,jarSettings);
        //
        int sumTotal = totalCasamientos_1_1+totalCasamientos_1_n+
                totalCasamientos_n_1+totalCasamientos_n_m+
                totalCasamientos_none+totalCasamientos_0_0
                ;

        System.out.println("total2 tipo0 casamientos2 casamientos: "
                +" "+tipo0
                +" "+totalCasamientos
                +" "+sumTotal
                +" "+PrintUtils.print2dec(totalCasamientos_1_1*100.0/totalCasamientos)
                +" "+PrintUtils.print2dec(totalCasamientos_1_n*100.0/totalCasamientos)
                +" "+PrintUtils.print2dec(totalCasamientos_n_1*100.0/totalCasamientos)
                +" "+PrintUtils.print2dec(totalCasamientos_n_m*100.0/totalCasamientos)
                +" || "+PrintUtils.print2dec(totalReduccionNM1*100.0/totalLinesNM)
        );
    }
     
    public void runApp(JarSettings jarSettings,LinkedHashMap asientosToDelete) throws IOException{        
         //1.)llamada al algoritmo de casacion con la opcion car a true
        //y writetoCsv a false
        Registry.writeRegistryInt("HKCU\\Software\\s2a\\PagoAplazadoProveedores", "estado",1);
	Registry.writeRegistryString("HKCU\\Software\\s2a\\PagoAplazadoProveedores", "mensaje","runApp");
            
        System.out.println("input: "+new File(jarSettings.getInputCSV()).toPath());
           
        jarSettings.setIsCasadoRefEnabled(true);
        LinkedHashMap<Long, String> linesReferences = new LinkedHashMap();
        AlgoritmoCasacionThread algoCasacion = new AlgoritmoCasacionThread();        
        //procesa todas las subcuentas y devuelve el diario ordenado
        //en java no se puede cambiar la referencia, por parametro, luego
        //siempre devolvemos el diario como retorno
        List<String> diaryList = algoCasacion.runApp(jarSettings,asientosToDelete,linesReferences,true,true,false);   
        if (diaryList==null){
            System.out.println("diaryList IS NULL");
        }else{
            System.out.println("diaryList size: "+diaryList.size());
        }
        
        //2.)factorizamos los casamientos, agrupamos las lineas por referencia
        //y subcuenta
        int maxSize = 0;
        LinkedHashMap<String, ArrayList<Long>> casamientos = new LinkedHashMap();
        for (Entry<Long, String> entry : linesReferences.entrySet()) {
            long line   = entry.getKey();
            String tipo = entry.getValue().split("_")[0];
            String ref  = entry.getValue().split("_")[1];
            //necesitamos la subcuenta asociada
            //System.out.println("[runApp] obteniedo linea "+(line-1));
            String lineStr = diaryList.get((int) line-1);//empieza por 0
            String subcuentaCod = lineStr.split(";",-1)[jarSettings.getSubAccountCol()];
            String sub_ref = subcuentaCod+"_"+tipo+"_"+ref;
            ArrayList<Long> references = null;
            if (!casamientos.containsKey(sub_ref)){
                references = new ArrayList<Long>();
                casamientos.put(sub_ref, references);
            }
            references = casamientos.get(sub_ref);
            //añado la linea al indice subcuenta+tipo+referencia
            references.add(line);
            if (references.size()>maxSize) maxSize = references.size();
            //System.out.println("sub_ref: "+sub_ref);
        }
        System.out.println("FACTORIZACION TERMINADA. Claves añadidas y maxSize: "+casamientos.size()+" "+maxSize);  
        //3.)aplicamos algoritmo de calculo de distancias tanto de los casados,
        //como los saldos
        LinkedHashMap<Long,String> refDurations = new LinkedHashMap<>();
        executeCoreProcess(diaryList,casamientos,jarSettings,refDurations);
        //4.)Escribimos a disco las duraciones
        FileUtils.writeDurations(jarSettings,refDurations);
        
        Registry.writeRegistryInt("HKCU\\Software\\s2a\\PagoAplazadoProveedores", "estado",2);
   	Registry.writeRegistryString("HKCU\\Software\\s2a\\PagoAplazadoProveedores", "mensaje","OK");
    }
    
    public static void main(String[] args) {
        
        LinkedHashMap asientosToDelete = new LinkedHashMap();
        String inputCSV		= args[0];
	String outputCSV 	= args[1];
	int startLine           = -1;
	int subAccountCol 	= -1;
	int asientoCol          = -1;
	int debeCol 		= -1;
	int haberCol 		= -1;
        int fechaCol            = -1;
        int casadoCol           = -1;
        Calendar cal = Calendar.getInstance();
        String refDate    = DateUtils.datePrint(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), "/");
        int secsPerSubaccount = -1;
	int maxCores = Runtime.getRuntime().availableProcessors();
	int asientoApertura = -1;//-aa
	int asientoCierre = -1;//-ac
	int asientoRegularizacion = -1;//-ar		
	boolean delphiEncoding = false;
        boolean isCasadoRefEnabled = true; //anota referencia con la que se casa
        
        for (int i=0;i<args.length;i++){
            String option = args[i];
            if (option.contains("-startLine")){
                startLine = Integer.valueOf(args[i].substring(10, args[i].length())); 
            }
            if (option.contains("-subCol")){
                subAccountCol = Integer.valueOf(args[i].substring(7, args[i].length()));  
            }
            if (option.contains("-asiCol")){
                asientoCol = Integer.valueOf(args[i].substring(7, args[i].length()));  
            }
            if (option.contains("-debCol")){
                debeCol = Integer.valueOf(args[i].substring(7, args[i].length()));  
            }
            if (option.contains("-habCol")){
                haberCol = Integer.valueOf(args[i].substring(7, args[i].length()));  
            }
            if (option.contains("-fecCol")){
                fechaCol = Integer.valueOf(args[i].substring(7, args[i].length()));  
            }
            if (option.contains("-casCol")){
                casadoCol = Integer.valueOf(args[i].substring(7, args[i].length()));  
            }
            if (option.contains("-secs")){
                secsPerSubaccount = Integer.valueOf(args[i].substring(5, args[i].length()));  
            }
            if (option.contains("-maxCores")){
                maxCores = Integer.valueOf(args[i].substring(9, args[i].length()));  
            }
            if (option.contains("-refDate")){
		refDate = option.substring(8, option.length());  
            }
            if (option.contains("-encoding16")){
                delphiEncoding = true;
            }
            if (option.contains("-aa")){
                asientoApertura = Integer.valueOf(args[i].substring(3, args[i].length())); 
            }
            if (option.contains("-ac")){
                asientoCierre = Integer.valueOf(args[i].substring(3, args[i].length())); 
            }
            if (option.contains("-ar")){
                asientoRegularizacion = Integer.valueOf(args[i].substring(3, args[i].length())); 
            }                       
        }//for options
                
        if (asientoApertura!=-1)
            asientosToDelete.put(asientoApertura, 1);
        if (asientoCierre!=-1)
            asientosToDelete.put(asientoCierre,1);
	if (asientoRegularizacion!=-1)
            asientosToDelete.put(asientoRegularizacion,1);
        
        JarSettings jarSettings = new JarSettings();        
        jarSettings.setInputCSV(inputCSV);
        jarSettings.setOutputCSV(outputCSV);
        jarSettings.setStartLine(startLine);
        jarSettings.setSubAccountCol(subAccountCol);
        jarSettings.setAsientoCol(asientoCol);
        jarSettings.setDebeCol(debeCol);
        jarSettings.setHaberCol(haberCol);
        jarSettings.setFechaCol(fechaCol);
        jarSettings.setCasadoCol(casadoCol);
        jarSettings.setRefFecha(refDate);
        jarSettings.setSecsPerSubaccount(secsPerSubaccount);
        jarSettings.setMaxCores(maxCores);
        jarSettings.setDelphiEncoding(delphiEncoding);
        jarSettings.setIsCasadoRefEnabled(true);
                
        try{
            new AlgoritmoPagoAplazadoThread().runApp(jarSettings,
                        asientosToDelete);
        }catch (Exception e){
            e.printStackTrace();
            Registry.writeRegistryInt("HKCU\\Software\\s2a\\PagoAplazadoProveedores", "estado",3);
	    Registry.writeRegistryString("HKCU\\Software\\s2a\\PagoAplazadoProveedores", "mensaje","Error: "+e.getMessage());
        }
       
        
    }

    

   

    

   

}
