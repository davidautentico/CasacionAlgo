package s2a.threading;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import s2a.Casamiento;
import s2a.JarSettings;

import s2a.Movimiento;
import s2a.SubAccount;
import s2a.test.ExecutorInvokeAll;
import s2a.utils.DecodificationUtils;
import s2a.utils.FileUtils;
import s2a.utils.LogUtils;
import s2a.utils.Registry;

public class AlgoritmoCasacionThread {
    
        boolean isPagoAplazadoMode = false;
	
	public static int calculateTotalMovs(LinkedHashMap subs){
		int total = 0;
		Iterator<SubAccount> it = subs.values().iterator();
		while (it.hasNext()){
			SubAccount sub1 = it.next();
			total+=sub1.getMovimientos().size();
		}
		
		return total;
	}
	
	public static void printImporte(LinkedHashMap subs){
		int total = 0;
		long debe = 0;
		long haber = 0;
		Iterator<SubAccount> it = subs.values().iterator();
		while (it.hasNext()){
			SubAccount sub1 = it.next();
			total+=sub1.getMovimientos().size();
			ArrayList<Movimiento> movimientos = sub1.getMovimientos();
			for (int i=0;i<movimientos.size();i++){
				debe += movimientos.get(i).getDebe();
				haber += movimientos.get(i).getHaber();
			}
		}
		System.out.println("DEBE HABER : "+debe+" "+haber+" || "+(debe-haber)+" || "+subs.size());
	}
	
	/**
	 * Proceso principal para casar subcuentas
	 * @param subAccountsCopy
	 * @param i
	 * @param l
	 * @param secsPerSubaccount
	 * @param linesToKeep
	 * @param j
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 */
	private void coreProcess(LinkedHashMap subAccounts,int maxSecs,
                int totalCores,LinkedHashMap linesToKeep,LinkedHashMap linesRef,
                boolean isCasadoRef) throws InterruptedException, ExecutionException {
            //vairable para guardar las subcuentas procesadas
            AtomicInteger subAccountsProcessed = new AtomicInteger();	    
	    //int processors = Runtime.getRuntime().availableProcessors();
	    //System.out.println("Cores disponibles: "+totalCores);
	    ExecutorService executorService=Executors.newFixedThreadPool(totalCores);
            List<Callable<CallableCasarSubcuenta>> tasks=new ArrayList<Callable<CallableCasarSubcuenta>>();
            boolean debug = false;        
            int totalInicioMovs = 0;        
            int i=0;
            Iterator<SubAccount> it = subAccounts.values().iterator();
            while (it.hasNext()
                //&& i<=0//debug
		){
                SubAccount sub = it.next();
		totalInicioMovs+=sub.getMovimientos().size();
		if (Math.abs(sub.recalculateSaldo())>0 
                    //&& sub.getMovimientos().size()>625000 && sub.getMovimientos().size()<628000
                    )
                    tasks.add(new CallableCasarSubcuenta(sub,subAccountsProcessed,maxSecs,false,-1,debug));
                    System.out.println("[coreProcess] added: "+sub.getCodSubCuenta());
                    i++;
            }
            System.out.println("[coreProcess] total tasks added: "+tasks.size());
            int total=0;
            List<Future<CallableCasarSubcuenta>> results = executorService.invokeAll(tasks);
       
            executorService.shutdown();
            //progreso a 100
            Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "progreso",100);
            //guardamos lineas que afectan a saldo (las que nos han quedado)
            System.out.println("PROCESO CASACION FINALIZADO");
            it = subAccounts.values().iterator();
            while (it.hasNext()){
                SubAccount sub = it.next();
                long saldo = sub.recalculateSaldo();
                System.out.println(sub.getCodSubCuenta()+" "+saldo);
		if (Math.abs(saldo)>0){
                    ArrayList<Movimiento> movimientos = sub.getMovimientos();
                    for (i=0;i<movimientos.size();i++){
                        long line = movimientos.get(i).getLine();
                        linesToKeep.put(line, true);//afectan a saldo
                    }
                    ArrayList<Casamiento> casamientos = sub.getCasamientos();
                    for (i=0;i<casamientos.size();i++){
                        long line = casamientos.get(i).getLine();
                        String ref = casamientos.get(i).getTipo()+"_"+casamientos.get(i).getNumRef();
                        linesRef.put(line,ref);
                        System.out.println("[coreProcess] "+line+" "+ref);
                    }
                    //.out.println("total linesref: "+linesRef.size());
		}
                //Para linesRef, donde importan todos los casamientos
                //tambien usamos las cuentas a saldo 0
                if (Math.abs(sub.recalculateSaldo())==0){
                    ArrayList<Movimiento> movimientos = sub.getMovimientos();
                    for (i=0;i<movimientos.size();i++){
                        long line = movimientos.get(i).getLine();
                        String ref = "0_1";//el tipo es 0, no es casamiento en realidad y la referencia 1
                        linesRef.put(line,ref);
                    }
                }
            }
	}
        
        /**
         * Proceso por bloque
         * 
         * @param subAccounts
         * @param maxSecs
         * @param maxGlobalSecs
         * @param blockSize
         * @param totalCores
         * @param linesToKeep
         * @param linesRef
         * @param isCasadoRef
         * @throws InterruptedException
         * @throws ExecutionException 
         */
        private void coreProcessBlocks(LinkedHashMap subAccounts,
                int maxSecs,int maxGlobalSecs,int blockSize,
                int totalCores,
                int casadoCol,
                LinkedHashMap linesToKeep,
                LinkedHashMap linesRef,
                boolean isCasadoRef) throws InterruptedException, ExecutionException {
            //vairable para guardar las subcuentas procesadas
            AtomicInteger subAccountsProcessed = new AtomicInteger();	    
	    //int processors = Runtime.getRuntime().availableProcessors();
	    //System.out.println("Cores disponibles: "+totalCores);
	    ExecutorService executorService=Executors.newFixedThreadPool(totalCores);
            List<Callable<CallableCasarSubcuenta>> tasks=new ArrayList<Callable<CallableCasarSubcuenta>>();
            boolean debug = false;        
            int totalInicioMovs = 0;        
            int i=0;
            int tasksAdded  = 0;
            long startTime  = System.currentTimeMillis();
            long stopTime   = startTime + maxGlobalSecs*1000;
            //las subaccounts deberia de ordenarse de menor a mayor movimientos, para tener el mayor numero
            //de subcuentas procesadas
             // not yet sorted
            List<SubAccount> sortedSubAccounts = new ArrayList<SubAccount>(subAccounts.values());

            Collections.sort(sortedSubAccounts, new Comparator<SubAccount>() {

                public int compare(SubAccount s1, SubAccount s2) {
                    return s1.getMovimientos().size() - s2.getMovimientos().size();
                }
            });

            //Collection coll = subAccounts.values();
            //Collections.sort(subAccounts);
            
            System.out.println("[coreProcess] totalsubcuentas: "+subAccounts.size());
            
            //Iterator<SubAccount> it = subAccounts.values().iterator();
            long coreStartTime = System.currentTimeMillis();
            Iterator<SubAccount> it = sortedSubAccounts.iterator();
            while (it.hasNext()
                //&& i<=0//debug
		){
                
                 //chequeamos si se han añadido suficientes tareas
                if (tasksAdded>=blockSize){
                    //ejecutamos las tareas
                    //System.out.println("total tasks added block: "+tasks.size());
                    //invocamos las tareas del bloque
                    List<Future<CallableCasarSubcuenta>> results = executorService.invokeAll(tasks);
                    //inicializamos el array
                    tasks.clear();
                    tasksAdded = 0;
                }
                //chequeamos tiempo por si se ha excedido
                long actualTime = System.currentTimeMillis();
                if (actualTime>=stopTime){ //tiempo excedido, salimos     
                    //System.out.println("Max Global Time expired: "+maxGlobalSecs);
                    tasks.clear();
                    tasksAdded = 0;
                    break;
                }
                
                SubAccount sub = it.next();
		totalInicioMovs+=sub.getMovimientos().size();
		if (Math.abs(sub.recalculateSaldo())>0 || isPagoAplazadoMode
                        //&& sub.getMovimientos().size()>800000//prueba, solo subcuentas por debajo de 100000 movimientos
                        //&& sub.getMovimientos().size()<840000
                    //&& sub.getMovimientos().size()>625000 && sub.getMovimientos().size()<628000
                    ){
                    //System.out.println("task added size: "+sub.getCodSubCuenta()+" || "+sub.getMovimientos().size()+" || "+totalInicioMovs);
                    tasks.add(new CallableCasarSubcuenta(sub,subAccountsProcessed,maxSecs,isPagoAplazadoMode,casadoCol,debug));
                    System.out.println("[coreProcess] added: "+sub.getCodSubCuenta());
                    i++;
                    tasksAdded++;
                }
            }
            
             //no se ha liberado el array
            if (tasksAdded>0){
                //ejecutamos las tareas
                    System.out.println("total tasks added exit block: "+tasks.size());
                    //invocamos las tareas del bloque
                    List<Future<CallableCasarSubcuenta>> results = executorService.invokeAll(tasks);
                    //inicializamos el array
                    tasks.clear();
                    tasksAdded = 0;
            }
               
            executorService.shutdown();
            
            long coreEndTime = System.currentTimeMillis();
            long diff = coreEndTime-coreStartTime;
            System.out.println("[coreProcessBlocks] Tiempo de proceso casación: "+diff/1000+" segundos");
            
            //progreso a 100
            Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "progreso",100);
            //guardamos lineas que afectan a saldo (las que nos han quedado)
            //System.out.println("PROCESO CASACION FINALIZADO");
            it = subAccounts.values().iterator();
            while (it.hasNext()){
                SubAccount sub = it.next();
                long saldo = sub.recalculateSaldo();
		if (Math.abs(saldo)>0 || isPagoAplazadoMode){
                    ArrayList<Movimiento> movimientos = sub.getMovimientos();
                    for (i=0;i<movimientos.size();i++){
                        long line = movimientos.get(i).getLine();
                        linesToKeep.put(line, true);//afectan a saldo
                         //System.out.println("[coreProcess blocks saldo] "+line);
                    }
                    ArrayList<Casamiento> casamientos = sub.getCasamientos();
                    for (i=0;i<casamientos.size();i++){
                        long line = casamientos.get(i).getLine();
                        String ref = casamientos.get(i).getTipo()+"_"+casamientos.get(i).getNumRef();
                        linesRef.put(line,ref);
                        linesToKeep.put(line, false);//afectan a saldo
                        //System.out.println("[coreProcess blocks casamiento] "+line+" "+ref);
                    }
                    //.out.println("total linesref: "+linesRef.size());
		}
                //Para linesRef, donde importan todos los casamientos
                //tambien usamos las cuentas a saldo 0
                if (Math.abs(sub.recalculateSaldo())==0){
                    ArrayList<Movimiento> movimientos = sub.getMovimientos();
                    for (i=0;i<movimientos.size();i++){
                        long line = movimientos.get(i).getLine();
                        String ref = "0_1";//el tipo es 0, no es casamiento en realidad y la referencia 1
                        linesRef.put(line,ref);
                    }
                }
            }
	}
        
        
        /**
	 * Proceso principal para casar subcuentas,casando tambien aquellas con
         * saldo 0 en modo especial
	 * @param subAccountsCopy
	 * @param i
	 * @param l
	 * @param secsPerSubaccount
	 * @param linesToKeep
	 * @param j
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 */
	private void coreProcessAll(LinkedHashMap subAccounts,int maxSecs,
                int totalCores,LinkedHashMap linesToKeep,LinkedHashMap linesRef,
                boolean isCasadoRef) throws InterruptedException, ExecutionException {
            
            System.out.println("[coreProcessAll] iniciado");
            AtomicInteger subAccountsProcessed = new AtomicInteger();
	    ExecutorService executorService=Executors.newFixedThreadPool(totalCores);
            List<Callable<CallableCasarSubcuenta>> tasks=new ArrayList<Callable<CallableCasarSubcuenta>>();
            boolean debug = false;        
            int totalInicioMovs = 0;        
            int i=0;
            Iterator<SubAccount> it = subAccounts.values().iterator();
            while (it.hasNext()
                //&& i<=0//debug
		){
                SubAccount sub = it.next();
		totalInicioMovs+=sub.getMovimientos().size();		
                //pagoAplazadoMode
                tasks.add(new CallableCasarSubcuenta(sub,subAccountsProcessed,maxSecs,true,-1,debug));
                i++;
            }
            System.out.println("total tasks added: "+tasks.size());
            int total=0;
            List<Future<CallableCasarSubcuenta>> results = executorService.invokeAll(tasks);
       
            executorService.shutdown();
            //progreso a 100
            Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "progreso",100);
            //guardamos lineas que afectan a saldo (las que nos han quedado)
            //System.out.println("PROCESO CASACION FINALIZADO");
            it = subAccounts.values().iterator();
            while (it.hasNext()){
                SubAccount sub = it.next();		
                    ArrayList<Movimiento> movimientos = sub.getMovimientos();
                    for (i=0;i<movimientos.size();i++){
                        long line = movimientos.get(i).getLine();
                        linesToKeep.put(line, true);//afectan a saldo
                    }
                    ArrayList<Casamiento> casamientos = sub.getCasamientos();
                    for (i=0;i<casamientos.size();i++){
                        long line = casamientos.get(i).getLine();
                        String ref = casamientos.get(i).getTipo()+"_"+casamientos.get(i).getNumRef();
                        linesRef.put(line,ref);
                    }		
            }
	}
        
        /**
	 * Proceso principal para casar subcuentas,casando tambien aquellas con
         * saldo 0 en modo especial.
         * Lo hace en modo bloques, si el siguiente bloque ha excedido el tiempo
         * no lo hace
	 * @param subAccountsCopy
	 * @param i
	 * @param l
	 * @param secsPerSubaccount
	 * @param linesToKeep
	 * @param j
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 */
	private void coreProcessAllBlocks(LinkedHashMap subAccounts,int maxSecs,
                int totalCores,LinkedHashMap linesToKeep,LinkedHashMap linesRef,
                boolean isCasadoRef,
                int globalMaxSecs,int blockSize) throws InterruptedException, ExecutionException {
            
            System.out.println("[coreProcessAllBlocks] iniciado");
            AtomicInteger subAccountsProcessed = new AtomicInteger();
	    ExecutorService executorService=Executors.newFixedThreadPool(totalCores);
            List<Callable<CallableCasarSubcuenta>> tasks=new ArrayList<Callable<CallableCasarSubcuenta>>();
            boolean debug = false;        
            int totalInicioMovs = 0;        
            int i=0;
            //iterador para las subcuentas
            Iterator<SubAccount> it = subAccounts.values().iterator();
            int tasksAdded = 0;
            long startTime = System.currentTimeMillis();
            long stopTime = startTime + globalMaxSecs*1000;
            while (it.hasNext()
                //&& i<=0//debug
		){
                
                //chequeamos si se han añadido suficientes tareas
                if (tasksAdded>=blockSize){
                    //ejecutamos las tareas
                    System.out.println("total tasks added block: "+tasks.size());
                    //invocamos las tareas del bloque
                    List<Future<CallableCasarSubcuenta>> results = executorService.invokeAll(tasks);
                    //inicializamos el array
                    tasks.clear();
                    tasksAdded = 0;
                }
                //chequeamos tiempo por si se ha excedido
                long actualTime = System.currentTimeMillis();
                if (actualTime>=stopTime){ //tiempo excedido, salimos     
                    System.out.println("Max Global Time expired: "+globalMaxSecs);
                    break;
                }
                
                //obtenemos la subcuenta del iterador
                SubAccount sub = it.next();
		totalInicioMovs+=sub.getMovimientos().size();		
                //añadimos a la tarea la subcuenta
                tasks.add(new CallableCasarSubcuenta(sub,subAccountsProcessed,maxSecs,true,-1,debug));
                i++;
                tasksAdded++; 
            }
            

            //apagamos el servicio
            executorService.shutdown();
            //progreso a 100
            Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "progreso",100);
            //guardamos lineas que afectan a saldo (las que nos han quedado)
            System.out.println("PROCESO CASACION FINALIZADO");
            it = subAccounts.values().iterator();
            while (it.hasNext()){
                SubAccount sub = it.next();		
                    ArrayList<Movimiento> movimientos = sub.getMovimientos();
                    for (i=0;i<movimientos.size();i++){
                        long line = movimientos.get(i).getLine();
                        linesToKeep.put(line, true);//afectan a saldo
                    }
                    ArrayList<Casamiento> casamientos = sub.getCasamientos();
                    for (i=0;i<casamientos.size();i++){
                        long line = casamientos.get(i).getLine();
                        String ref = casamientos.get(i).getTipo()+"_"+casamientos.get(i).getNumRef();
                        linesRef.put(line,ref);
                    }		
            }
	}
	
	public void runApp(JarSettings jarSettings,
			LinkedHashMap asientosToDelete
			){
		System.out.println("[runApp] entrado");
		Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","runApp1.0");
		System.out.println("[runApp] escrito registro");		
		//fichero csv una vez formado
		LinkedHashMap linesToKeep = new LinkedHashMap();
		LinkedHashMap linesNotProcessed = new LinkedHashMap();
                LinkedHashMap linesReferences = new LinkedHashMap();
		LinkedHashMap subAccounts = FileUtils.readFileToMap(jarSettings,0,999,
                        false,asientosToDelete,linesNotProcessed);
		int totalInicioMovs = AlgoritmoCasacionThread.calculateTotalMovs(subAccounts);
		System.out.println("\n****FICHERO LEIDO Numero de subcuentas y total movimientos: "+subAccounts.size()+" "+totalInicioMovs);
		
		//int totalCores = maxCores;
		System.out.println("\n****Numero de cores: "+jarSettings.getMaxCores());
		
		//hacemos una copia del original
		LinkedHashMap subAccountsCopy = new LinkedHashMap();//creamos una cuenta para trabajar
		SubAccount.copySubAccountsHash(subAccounts,subAccountsCopy);
                printImporte(subAccountsCopy);
		System.out.println("\n****COPIA DEL ORIGINAL REALIZADA. REALIZANDO CASACI�N... ****");
	    
		try {
			/*coreProcess(subAccountsCopy,jarSettings.getSecsPerSubaccount(),                                
                                jarSettings.getMaxCores(),linesToKeep,linesReferences,
                                jarSettings.isIsCasadoRefEnabled());//realizamos el proceso y obtenemos lineas a mantener
                        */
                        coreProcessBlocks(subAccountsCopy,jarSettings.getSecsPerSubaccount(),
                                jarSettings.getGlobalMaxSecs(),jarSettings.getBlockSize(),
                                jarSettings.getMaxCores(),
                                jarSettings.getCasadoCol(),
                                linesToKeep,linesReferences,
                                jarSettings.isIsCasadoRefEnabled());//realizamos el proceso y obtenemos lineas a mantener
			printImporte(subAccountsCopy);
		} catch (Exception e) {
			Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "estado",3);
	   		Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","Error: "+e.getMessage());
		   e.printStackTrace();//salimos
		   return;
		}
		int totalFinMovs = linesToKeep.size();
		double per = 100.0-totalFinMovs*100.0/totalInicioMovs;
		System.out.println("\n****MOVIMIENTOS INICIALES FINALES CASADOS: "
				+totalInicioMovs+" "+totalFinMovs
				+" "+(totalInicioMovs-totalFinMovs)
				+" "+per+" %");
   	  
   	  	//escribimos a csv
   		System.out.println("\n****FINALIZANDO CASACION.ESCRIBIENDO CSV... ****");
   		try {
			FileUtils.writeCSV(jarSettings.getInputCSV(),jarSettings.getOutputCSV(),
                                linesToKeep,linesNotProcessed,null,null,
                                jarSettings.isDelphiEncoding());
		} catch (IOException e) {
			Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "estado",3);
	   		Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","Error: "+e.getMessage());
		   e.printStackTrace();
		   return; //salimos
		}
   			
   		System.out.println("\n****ESCRITURA REGISTRO****");
   		Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "estado",2);
   		Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","OK");
   			
   		System.out.println("\nPROGRAMA FINALIZADO");
	}
        
        /**
         * En esta versión devolvemos las referencias de los casamientos
         * para su procesamiento por fuera
         * La escritura a disco es opcional
         * @param jarSettings
     * @param diaryList
         * @param asientosToDelete
         * @param linesReferences
         * @param hasToWriteCSV 
         */
        public List<String> runApp(JarSettings jarSettings,
			LinkedHashMap asientosToDelete,
                        LinkedHashMap linesReferences,
                        boolean isPagoAplazado,
                        boolean hasAllSubs,
                        boolean hasToWriteCSV
			) throws IOException{
            
                this.isPagoAplazadoMode = isPagoAplazado;
		System.out.println("[runApp] entrado PAmode= "+isPagoAplazado);
		Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","runApp");
		System.out.println("[runApp] escrito registro");
		//fichero csv una vez formado
		LinkedHashMap linesToKeep = new LinkedHashMap();
		LinkedHashMap linesNotProcessed = new LinkedHashMap();
                LinkedHashMap linesSaldoAffected = new LinkedHashMap();
                if (linesReferences==null)
                    linesReferences = new LinkedHashMap();                
                //carga de todo el diario en memoria
                List<String> diaryList = null;
                if (jarSettings.isDelphiEncoding()){                
                     diaryList = Files.readAllLines(new File(jarSettings.getInputCSV()).toPath(), 
                        Charset.forName("UTF-16LE"));
                }else{            
                    diaryList = Files.readAllLines(new File(jarSettings.getInputCSV()).toPath(),
                        Charset.forName("ISO-8859-1"));
                }	                
             
                System.out.println("[runApp] diario ordenado size: "+diaryList.size());
		LinkedHashMap subAccounts = FileUtils.readFileToMap(jarSettings,
                        diaryList,
                        0,999,
                        false,
                        asientosToDelete,linesNotProcessed,linesSaldoAffected
                );
		int totalInicioMovs = AlgoritmoCasacionThread.calculateTotalMovs(subAccounts);
		System.out.println("\n****FICHERO LEIDO Numero de subcuentas y total movimientos: "+subAccounts.size()+" "+totalInicioMovs+" || "+diaryList.size());
		printImporte(subAccounts);
		//int totalCores = maxCores;
		System.out.println("\n****Numero de cores: "+jarSettings.getMaxCores());
		
		//hacemos una copia del original
		LinkedHashMap subAccountsCopy = new LinkedHashMap();//creamos una cuenta para trabajar
		SubAccount.copySubAccountsHash(subAccounts,subAccountsCopy);
		System.out.println("\n****COPIA DEL ORIGINAL REALIZADA. REALIZANDO CASACI�N... ****");
	    
                //return null;
		
                try {
                    if (!hasAllSubs){
			coreProcess(subAccountsCopy,jarSettings.getSecsPerSubaccount(),
                                jarSettings.getMaxCores(),linesToKeep,linesReferences,
                                jarSettings.isIsCasadoRefEnabled());//realizamos el proceso y obtenemos lineas a mantener
                    }else{
                         coreProcessBlocks(subAccountsCopy,
                                jarSettings.getSecsPerSubaccount(),
                                jarSettings.getGlobalMaxSecs(),
                                jarSettings.getBlockSize(),
                                jarSettings.getMaxCores(),
                                jarSettings.getCasadoCol(),
                                linesToKeep,
                                linesReferences,
                                jarSettings.isIsCasadoRefEnabled());//realizamos el proceso y obtenemos lineas a mantener
			printImporte(subAccountsCopy);
                    }
			printImporte(subAccountsCopy);
		} catch (Exception e) {
			Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "estado",3);
	   		Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","Error: "+e.getMessage());
		   e.printStackTrace();//salimos
		   return null;
		}
		int totalFinMovs = linesToKeep.size();
		double per = 100.0-totalFinMovs*100.0/totalInicioMovs;
		System.out.println("\n****MOVIMIENTOS INICIALES FINALES CASADOS: "
				+totalInicioMovs+" "+totalFinMovs
				+" "+(totalInicioMovs-totalFinMovs)
				+" "+per+" %");
               
   	  	//escribimos a csv
                if (hasToWriteCSV){
                    System.out.println("\n****FINALIZANDO CASACION.ESCRIBIENDO CSV... ****");
                    try {
                            FileUtils.writeCSV(jarSettings.getInputCSV(),jarSettings.getOutputCSV(),
                                    linesToKeep,
                                    linesNotProcessed,
                                    linesSaldoAffected,
                                    linesReferences,
                                    jarSettings.isDelphiEncoding());
                    } catch (IOException e) {
                            Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "estado",3);
                            Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","Error: "+e.getMessage());
                       e.printStackTrace();
                       return null; //salimos
                    }
                }
   			
   		System.out.println("\n****ESCRITURA REGISTRO****");
   		Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "estado",2);
   		Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","OK");
   			
   		System.out.println("\nPROGRAMA FINALIZADO");
                
                return diaryList;
	}
	
	private static void printParameters(String paramStr,String valuesStr){
		
		String[] parameters = paramStr.split(" ");
		String[] values = valuesStr.split(" ");
		//System.out.println(valuesStr);
		System.out.println("****PARAMETROS****");
		for (int i=0;i<parameters.length;i++){
			String param = parameters[i];
			String value = values[i];
			System.out.println(param+": "+value);
		}
		System.out.println("******************");
	}

	public static void main(String[] args) {
		
                LogUtils log = new LogUtils("algoritmoCasacion.log",false);
                log.addToLog("Algoritmo de Casacion iniciado total argumentos: "+args.length);
                //log.close();
                System.out.println("Main iniciado");
		long startTime = System.currentTimeMillis();
		if (args.length<5){
			System.out.println("[ERROR] Faltan params. Se esperan: input output subAccountCol debeCol haberCol secsPerAccount");
                        log.addToLog("[ERROR] Faltan params. Se esperan: input output subAccountCol debeCol haberCol secsPerAccount");
                        log.close();
			return;
		}
		
		LinkedHashMap asientosToDelete = new LinkedHashMap();
		String inputCSV		= args[0];
		String outputCSV 	= args[1];
		int startLine           = -1;
		int subAccountCol 	= -1;
		int asientoCol          = -1;
		int debeCol 		= -1;
		int haberCol 		= -1;
                int fechaCol            = -1;
		int secsPerSubaccount   = -1;
                int secsGlobal          = 120;
                int blockSize           = 50;
		int maxCores = Runtime.getRuntime().availableProcessors();
		long asientoApertura = -1;//-aa
		long asientoCierre = -1;//-ac
		long asientoRegularizacion = -1;//-ar		
		boolean delphiEncoding = false;
                boolean isCasadoRefEnabled = false; //anota referencia con la que se casa
                
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
			if (option.contains("-secs")){
				secsPerSubaccount = Integer.valueOf(args[i].substring(5, args[i].length()));  
			}
                        if (option.contains("-maxglobalsecs")){
				secsGlobal = Integer.valueOf(args[i].substring(14, args[i].length()));  
			}
                        if (option.contains("-blockSize")){
				blockSize = Integer.valueOf(args[i].substring(10, args[i].length()));  
			}
			if (option.contains("-maxCores")){
				maxCores = Integer.valueOf(args[i].substring(9, args[i].length()));  
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
                        //anota referencia con la que se casa
                        if (option.contains("-car")){
				isCasadoRefEnabled = true; 
			}
		}
                //se pasan las opciones a una clase para no pasar
                //un chorro de parametros
                JarSettings jarSettings = new JarSettings();
                jarSettings.setInputCSV(inputCSV);
                jarSettings.setOutputCSV(outputCSV);
                jarSettings.setStartLine(startLine);
                jarSettings.setSubAccountCol(subAccountCol);
                jarSettings.setAsientoCol(asientoCol);
                jarSettings.setDebeCol(debeCol);
                jarSettings.setHaberCol(haberCol);
                jarSettings.setFechaCol(fechaCol);
                jarSettings.setSecsPerSubaccount(secsPerSubaccount);
                jarSettings.setMaxCores(maxCores);
                jarSettings.setDelphiEncoding(delphiEncoding);
                jarSettings.setIsCasadoRefEnabled(isCasadoRefEnabled);
                jarSettings.setBlockSize(blockSize);
                jarSettings.setGlobalMaxSecs(secsGlobal);
		
		if (asientoApertura!=-1)
			asientosToDelete.put(asientoApertura, 1);
		if (asientoCierre!=-1){
			asientosToDelete.put(asientoCierre,1);
                        System.out.println("Añadiendo asiento de cierre: "+asientoCierre);
                }
		if (asientoRegularizacion!=-1)
			asientosToDelete.put(asientoRegularizacion,1);
		
		String headers = "startLine subCol asientoCol debeCol haberCol secsPerAccount maxGlobalSecs blockSize maxCores asientoApertura asientoCierre asientoRegularizacion isCasadoRefEnabled";
		String values = startLine+" "+subAccountCol+" "+asientoCol+" "+debeCol+" "+haberCol
				+" "+secsPerSubaccount+" "+secsGlobal+" "+blockSize
                                +" "+maxCores+" "+asientoApertura+" "+asientoCierre
				+" "+asientoRegularizacion+" "+isCasadoRefEnabled;
		
		printParameters(headers,values);
		//buscar lineas partidas
                System.out.println("Buscando errores");
                
                
                System.out.println("FIN Buscando errores");
                //core
		try {
                    new AlgoritmoCasacionThread().runApp(jarSettings,
                        asientosToDelete);
		} catch (Exception e) {
	            e.printStackTrace();
                    Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "estado",3);
                    Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","Error: "+e.getMessage());
                }
		long stopTime = System.currentTimeMillis();
		long diff = stopTime-startTime;
		System.out.println("Tiempo de proceso: "+diff/1000+" segundos");
		
	}

}
