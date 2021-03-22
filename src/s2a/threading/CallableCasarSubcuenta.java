package s2a.threading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import s2a.Movimiento;
import s2a.SubAccount;
import s2a.utils.MathUtils;

public class CallableCasarSubcuenta implements Callable{
	
	SubAccount sub = null;
	AtomicInteger sum = null;
	AtomicBoolean isBreakPointReached = new AtomicBoolean(false);
	long startTime = 0;
	static long stopTime = 0;
	int maxSecs = 5;
        boolean isPagoAplazadoMode = false;
	Boolean debug = false;
	long saldoInicial = 0;
	long movimientosIniciales  = 0;
        long casamientoRef = 1;//cada vez que se produce un casamiento, se incrementa, para indicar quien se ha casado con quien
        int casadoCol = -1;
	ArrayList<Long> lineasCasadas;
	
	public CallableCasarSubcuenta(SubAccount sub,AtomicInteger sum,int maxSecs,
                boolean isPagoAplazadoMode,int casadoCol, boolean debug){
            
            this.lineasCasadas = new ArrayList<>();
            this.sub = sub;
            this.sum = sum;
            this.maxSecs = maxSecs;
            this.debug = debug;
            this.movimientosIniciales = sub.getMovimientos().size();
            this.saldoInicial = sub.recalculateSaldo();
            this.isPagoAplazadoMode = isPagoAplazadoMode;
            this.casadoCol = casadoCol;
	}
        
        
        /**
         * Proveniente del processType1, da por hecho que se encuentra ordenado
         * de más viejo a más nuevo, se trata de casas haber con acumulación de debes
         * o debes con acumulación de haberes
         * @param saldo 
         */
        public void processType1_2(long saldo){
            
           // System.out.println("proccess1_2e y movimientos "+sub.getMovimientos().size());
            
            HashMap<Long,Integer> linesToKeep = new HashMap<Long,Integer>();
            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            //en este caso como buscamos casamientos, por defecto, todo es saldo
            for (int i=0;i<=movimientos.size()-1;i++){
                linesToKeep.put(movimientos.get(i).getLine(),1);
            }
            ArrayList<Integer>indexes = new ArrayList<Integer>();
            for (int i=0;i<=movimientos.size()-1;i++){
                indexes.add(0);
            }
            
            if (saldo>0){ 
                 ArrayList<Integer> icasados = new ArrayList<Integer>();
                for (int i=0;i<movimientos.size();i++){
                    icasados.add(0);
                }
                int actualIndex = movimientos.size()-1;
                for (int i=actualIndex;i>=0;i--){
                    if (i<actualIndex) continue;
                    Movimiento m = movimientos.get(i);
                    long saldoMove = m.getDebe()-m.getHaber();
                    indexes.set(i,1);//en caso de casarse siempre va a 1
                    //RESET INDEXES
                    for (int x=i-1;i>=0;i--) indexes.set(x,0);
                    if (saldoMove>0){//debe
                        long saldoAcc = 0;
                        for (int j=i-1;j>=0;j--){
                            if (icasados.get(j)==1) continue;
                            Movimiento mj = movimientos.get(j);
                            long saldoMovej = mj.getDebe()-mj.getHaber();
                            if (saldoMovej<0  && mj.getHaber()>0){//buscamos por haberes
                                saldoAcc += saldoMovej;
                                indexes.set(j,1);//se podrian casar
                                if (saldoMove+saldoAcc==0){
                                    //se casan
                                    casamientoRef = 12;
                                    sub.casarTodosInclusion((short)12,casamientoRef,indexes,linesToKeep,icasados);
                                    actualIndex = j-1;//movemos i..
                                    break;
                                }      
                                //PROCEDIMIENTO ADICIONAL, BORRANDO DE UN SUBCONJUNTO
                                //BUSCANDO ESA DIFERENCIA
                                if (Math.abs(saldoAcc)>Math.abs(saldoMove)){                                                              
                                    break;
                                }//PROCEDIMIENTO ADICIONAL
                            }
                        }
                    }
                }
            }else if (saldo<0){  
                //System.out.println("Saldo: "+saldo+" "+movimientos.size());
                
                ArrayList<Integer> icasados = new ArrayList<Integer>();
                for (int i=0;i<movimientos.size();i++){
                    icasados.add(0);
                }
                int actualIndex = movimientos.size()-1;
                for (int i=actualIndex;i>=0;i--){
                    if (i<actualIndex) continue;
                    Movimiento m = movimientos.get(i);
                   // System.out.println("Movimientoi: "+i+" || "+m.toString());
                    long saldoMove = m.getDebe()-m.getHaber();
                    indexes.set(i,1);//en caso de casarse siempre va a 1
                    //System.out.println("antes de saldo");
                    //RESET INDEXES
                    for (int x=i-1;i>=0;i--) indexes.set(x,0);
                    if (saldoMove<0){//haber
                        long saldoAcc = 0;
                        for (int j=i-1;j>=0;j--){
                            if (icasados.get(j)==1) continue;                            
                            Movimiento mj = movimientos.get(j);
                            long saldoMovej = mj.getDebe()-mj.getHaber();
                            /*System.out.println(">>>Movimientoj: "+j+" || "+m.toString()
                                    +" || "+mj.getDebe()+" "+mj.getHaber()
                                    +" || "+saldoAcc);*/
                            if (saldoMovej>0 && mj.getDebe()>0){                                
                                saldoAcc += saldoMovej;
                                indexes.set(j,1);//se podrian casar
                                /*System.out.println("*** j y acc: "+j+" || "+mj.getDebe()+" "+mj.getHaber()
                                        +" sj|"+saldoMovej
                                        +" sacc| "+saldoAcc
                                        +" res| "+(Math.abs(saldoAcc)-Math.abs(saldoMove))
                                );*/
                                if (saldoMove+saldoAcc==0){
                                     //se casan
                                    casamientoRef = 12;
                                    //System.out.println("[1_2 se casan0] "+calculoSaldo(sub.getMovimientos()));
                                    sub.casarTodosInclusion((short)12,casamientoRef,indexes,linesToKeep,icasados);
                                    //System.out.println("[1_2 se casan1] "+calculoSaldo(sub.getMovimientos()));
                                    actualIndex = j-1;//movemos i..
                                    break;
                                }                                
                                //PROCEDIMIENTO ADICIONAL, BORRANDO DE UN SUBCONJUNTO
                                //BUSCANDO ESA DIFERENCIA
                                if (Math.abs(saldoAcc)>Math.abs(saldoMove)){                                     
                                    break;
                                }//PROCEDIMIENTO ADICIONAL
                            }
                        }
                    }
                }
            }//saldo
            
            //ahora borramos de movimientos lo que nos sobra, y que no esta en linestokeep
            //System.out.println("[1_2 antes de remove] "+calculoSaldo(sub.getMovimientos()));
            Movimiento.removeExclusion(movimientos,linesToKeep);
            //System.out.println("[1_2 despues de remove] "+calculoSaldo(sub.getMovimientos()));
        }
        
        /**
         * Necesita estar ordenador de mas viejo a mas nuevo
         */
        public void processType0(){
            //if (this.isPagoAplazadoMode){//creo q no tiene sentido ya..
             casamientoRef = 1;
             processType45();
            //}
        }
	/**
	 * Casacion de cuentas con saldo 0, todos los movimientos de esa subcuenta a 0
         * Es casacmiento en bloque por definición
         * La referencia es siempre 1
         * Aqui llega ordenado de más viejo a más nuevo
	 * @param subaccounts
	 */
	public void processType1(){
            //si hay pago aplazado pasamos siempre por el proceso de 1vs1
            long saldo = sub.recalculateSaldo();
            boolean isFound = false;
            System.out.println("[processType1] saldo: "+saldo);
            
            if (saldo==0 && !this.isPagoAplazadoMode){                
                //indicamos que se casan todos en bloque e incrementamos
                //la referencia
                casamientoRef = 1;
                sub.casarTodos((short)1,casamientoRef);
		sub.getMovimientos().clear();   
                //System.out.println("[processType1] todos Casados");
            }else {//DRP 09-10-2018
                //1) PRIMER METODO BÚSQUEDA DIRECTA DE SALDO
                //COMO YA SE ENCUENTRA ORDENADO DE MÁS VIEJO A MÁS NUEVO
                //SE VA ACUMULANDO HASTA QUE SUME
                
                if (saldo>0){
                    //buscamos por el debe, debe estar ordenado por fecha
                    ArrayList<Movimiento> movimientos = sub.getMovimientos();
                    long saldoAcc = 0;
                    HashMap<Long,Integer> linesToKeep = new HashMap<Long,Integer>();
                    ArrayList<Integer>indexes = new ArrayList<Integer>();
                    for (int i=0;i<=movimientos.size()-1;i++){
                        linesToKeep.put(movimientos.get(i).getLine(),0);
                    }                    
                    //1.1.1 modo no acumulativo 1:1
                    for (int i=0;i<=movimientos.size()-1;i++){
                        Movimiento m = movimientos.get(i);
                        long saldoMove = m.getDebe()-m.getHaber();
                        indexes.add(0);//por defecto se pone que se va a casar
                        if (saldoMove>0){                            
                            saldoAcc = saldoMove;
                            long diff = saldo-saldoAcc;
                            //System.out.println("[PROCESO 111 MOV SIDD] se casa index "+i+" || "+m.toStringDate(""));
                            if (diff==0){
                                indexes.set(indexes.size()-1,1);//en este caso siempre formaria parte del saldo
                                linesToKeep.put(m.getLine(),1);//puede formar parte del saldo, la linea
                                //combinación correcta, tenemos que casar todos menos los
                                casamientoRef = 11;
                                //marcamos como casados todos los que estan fuera de indexes, sólo si suman 000                                
                                sub.casarTodosExclusion((short)11,casamientoRef,indexes);
                                Movimiento.removeExclusion(movimientos,linesToKeep);  
                                System.out.println("[PROCESO 111 POR EL DEBE] se casa index "+i+" || "+m.toStringDate(""));
                                isFound = true;
                                break;
                            }
                        }
                    }      
                    //1.1.2 modo acumulatimov
                    if (!isFound){
                        saldoAcc = 0;
                        //reseteamos las lineas que pueden formar parte del saldo
                        for (int i=0;i<=movimientos.size()-1;i++){
                            linesToKeep.put(movimientos.get(i).getLine(),0);
                        }
                        for (int i=0;i<=movimientos.size()-1;i++){
                            Movimiento m = movimientos.get(i);
                            long saldoMove = m.getDebe()-m.getHaber();
                            indexes.add(0);//se va a casar seguro si se encuentra por esta forma
                            if (saldoMove>0){
                                linesToKeep.put(m.getLine(),1);//puede formar parte del saldo, la linea
                                indexes.set(indexes.size()-1,1);//en este caso siempre formaria parte del saldo, si es que se encuentra
                                saldoAcc += saldoMove;
                                long diff = saldo-saldoAcc;
                                if (diff==0){
                                    //combinación correcta, tenemos que casar todos menos los
                                    casamientoRef = 11;
                                    //marcamos como casados todos los que estan fuera de indexes                                
                                    sub.casarTodosExclusion((short)11,casamientoRef,indexes);
                                    Movimiento.removeExclusion(movimientos,linesToKeep);  
                                    //System.out.println("[PROCESO 112 POR EL DEBE] se casa index "+i+" || "+m.toStringDate(""));
                                    isFound = true;
                                    break;
                                }
                            }
                        }      
                    }
                    //1.1.3 todosv
                     if (!isFound){
                        saldoAcc = 0;
                        //reseteamos las lineas que pueden formar parte del saldo
                        for (int i=0;i<=movimientos.size()-1;i++){
                            linesToKeep.put(movimientos.get(i).getLine(),0);
                        }
                        for (int i=0;i<=movimientos.size()-1;i++){
                            Movimiento m = movimientos.get(i);
                            long saldoMove = m.getDebe()-m.getHaber();
                            indexes.add(0);//se va a casar seguro si se encuentra por esta forma
                            if (Math.abs(saldoMove)>=0.01){
                                linesToKeep.put(m.getLine(),1);//puede formar parte del saldo, la linea
                                indexes.set(indexes.size()-1,1);//en este caso siempre formaria parte del saldo, si es que se encuentra
                                saldoAcc += saldoMove;
                                long diff = saldo-saldoAcc;                                
                                System.out.println("[PROCESO 113 MOV SIDD] index "+i+" || "+m.toStringDate("")+" || "+saldo+" "+saldoMove+" "+diff);
                                if (diff==0){
                                    //combinación correcta, tenemos que casar todos menos los
                                    casamientoRef = 11;
                                    //marcamos como casados todos los que estan fuera de indexes                                
                                    sub.casarTodosExclusion((short)11,casamientoRef,indexes);
                                    Movimiento.removeExclusion(movimientos,linesToKeep);  
                                    //System.out.println("[PROCESO 112 POR EL DEBE] se casa index "+i+" || "+m.toStringDate(""));
                                    isFound = true;
                                    break;
                                }
                            }
                        }      
                    }
                }else if (saldo<0){
                     //buscamos por el debe, debe estar ordenado por fecha
                     //System.out.println("saldo<0 "+saldo);
                    ArrayList<Movimiento> movimientos = sub.getMovimientos();
                    long saldoAcc = 0;
                    HashMap<Long,Integer> linesToKeep = new HashMap<Long,Integer>();
                    ArrayList<Integer>indexes = new ArrayList<Integer>();
                    for (int i=0;i<=movimientos.size()-1;i++){
                        linesToKeep.put(movimientos.get(i).getLine(),0);
                    }
                    for (int i=0;i<=movimientos.size()-1;i++){
                        Movimiento m = movimientos.get(i);
                        long saldoMove = m.getDebe()-m.getHaber();
                        indexes.add(0);
                        if (saldoMove<0){//sólo por haber
                            saldoAcc = saldoMove;
                            long diff = saldo-saldoAcc;
                            if (diff==0){
                                indexes.set(indexes.size()-1,1);
                                linesToKeep.put(m.getLine(),1);//puede formar parte del saldo, la linea                                
                                //combinación correcta, tenemos que casar todos menos los
                                casamientoRef = 11;
                                //marcamos como casados todos los que estan fuera de indexes                                
                                sub.casarTodosExclusion((short)11,casamientoRef,indexes);
                                Movimiento.removeExclusion(movimientos,linesToKeep); 
                                //System.out.println("[PROCESO 112 POR EL HABER FOUND] se casa index "+i+" || "+m.toStringDate(""));
                                //System.out.println("se casa.."+calculoSaldo(movimientos));
                                isFound = true;
                                //break;
                            }
                        }
                    }   
                    if (!isFound){
                        indexes.clear();
                        saldoAcc = 0;
                        for (int i=0;i<=movimientos.size()-1;i++){
                            linesToKeep.put(movimientos.get(i).getLine(),0);
                        }
                        for (int i=0;i<=movimientos.size()-1;i++){
                            Movimiento m = movimientos.get(i);
                            long saldoMove = m.getDebe()-m.getHaber();
                            indexes.add(0);
                            if (saldoMove<0){//sólo por haber
                                linesToKeep.put(m.getLine(),1);//puede formar parte del saldo, la linea
                                indexes.set(indexes.size()-1,1);
                                saldoAcc += saldoMove;
                                long diff = saldo-saldoAcc;
                                if (diff==0){                                    
                                    //combinación correcta, tenemos que casar todos menos los
                                    casamientoRef = 11;
                                    //marcamos como casados todos los que estan fuera de indexes                                
                                    sub.casarTodosExclusion((short)11,casamientoRef,indexes);
                                    Movimiento.removeExclusion(movimientos,linesToKeep);
                                     //System.out.println("se casa.."+calculoSaldo(movimientos));
                                    //System.out.println("[PROCESO 112b POR EL HABER] movimientos left: "+movimientos.size()+" linestokeep = "+linesToKeep.size()+" "+saldo+" "+saldoAcc); 
                                    isFound = true;
                                    break;
                                }
                            }
                        }   
                    }
                    if (!isFound){
                        indexes.clear();
                        saldoAcc = 0;
                        for (int i=0;i<=movimientos.size()-1;i++){
                            linesToKeep.put(movimientos.get(i).getLine(),0);
                        }
                        for (int i=0;i<=movimientos.size()-1;i++){
                            Movimiento m = movimientos.get(i);
                            long saldoMove = m.getDebe()-m.getHaber();
                            indexes.add(0);
                            if (Math.abs(saldoMove)>=0.01){//sólo por haber
                                linesToKeep.put(m.getLine(),1);//puede formar parte del saldo, la linea
                                indexes.set(indexes.size()-1,1);
                                saldoAcc += saldoMove;
                                long diff = saldo-saldoAcc;
                                if (diff==0){                                    
                                    //combinación correcta, tenemos que casar todos menos los
                                    casamientoRef = 11;
                                    //marcamos como casados todos los que estan fuera de indexes                                
                                    sub.casarTodosExclusion((short)11,casamientoRef,indexes);
                                    Movimiento.removeExclusion(movimientos,linesToKeep);
                                     //System.out.println("se casa.."+calculoSaldo(movimientos));
                                    //System.out.println("[PROCESO 112b POR EL HABER] movimientos left: "+movimientos.size()+" linestokeep = "+linesToKeep.size()+" "+saldo+" "+saldoAcc); 
                                    isFound = true;
                                    break;
                                }
                            }
                        }   
                    }
                }
            }
             //System.out.println("antes de 1_2 "+calculoSaldo(sub.getMovimientos()));
            
           
            //lento
            processType1_2(saldo);//fallo aqui
           // System.out.println("despues de 1_2 "+calculoSaldo(sub.getMovimientos()));
	}
	
	/**
	 * Casacion de movimientos que acumulen 0, empezando desde el principio por fecha
         * Rferencia siempre a 2, necesita que esté ordenado de más viejo a mas nuevo..
	 * @param subaccounts
	 */
	public void processType2(boolean debug){
            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            long saldoAcc = 0;
            int lastZeroIndex = -1;
            
            if (debug){
                for (int i=movimientos.size()-1;i>=0;i--){
                    Movimiento m = movimientos.get(i);
                     if (debug)
                    System.out.println("[PROCESS TYPE 2] "+m.toStringDate(""));
                }
            }
            //viene ordenado de mas viejo a mas nuevo , luego empiezo por detras
            
            for (int i=movimientos.size()-1;i>=0;i--){
		Movimiento m = movimientos.get(i);
		long saldo = m.getDebe()-m.getHaber();
		saldoAcc += saldo;
		if (saldoAcc==0) lastZeroIndex = i;
                //totalToRemove++;
            }
            int totalToRemove = 0;
            if (lastZeroIndex>=0) 
                totalToRemove = movimientos.size()-1 -lastZeroIndex +1;
            boolean isCasado = false;
            //eliminamos lastZeroIndex+1 elementos
            saldoAcc = 0;
            if (debug)
                System.out.println("[PROCESS TYPE 2] totalToRemove: "+totalToRemove);
            for (int i=1;i<=totalToRemove;i++){
                int idx = movimientos.size()-1; 
                Movimiento m = movimientos.get(idx);
		long saldo = m.getDebe()-m.getHaber();
		saldoAcc+=saldo;
		if (debug)
                    System.out.println(sub.getMovimientos().get(idx).toString()+" "+saldoAcc);
                lineasCasadas.add(m.getLine());
                sub.casar((short)2,casamientoRef, m.getLine());//añadimos el casamiento
                isCasado = true;
                sub.getMovimientos().remove(idx);
            }           
	}
        
        /**
	 * Casacion de movimientos que acumulen 0, empezando desde el principio por fecha
         * Rferencia siempre a 2, necesita que esté ordenado de más viejo a mas nuevo..
         * En el caso de pago aplazado se van casando los bloques cada vez que se alcance 0
	 * @param subaccounts
	 */
	public void processType2_pagoAplazado(boolean debug){
            //saldo para ver si vamos por debe o por haber 
            //aqui guardaremos las lineas que han de ser eliminadas (casadas)
            LinkedHashMap<Long,Integer> linesToKeep = new LinkedHashMap();
            
            //estructuras necesarias
            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            LinkedHashMap<Long,ArrayList> debeMap = new LinkedHashMap();
            LinkedHashMap<Long,ArrayList> haberMap = new LinkedHashMap();
            for (int i=0;i<movimientos.size();i++){
                linesToKeep.put(movimientos.get(i).getLine(),1);    
            }
                        
            int totalDebe = 0;
            int totalHaber = 0;
            for (int i=movimientos.size()-1;i>=0;i--){
                Movimiento m = movimientos.get(i);
                long line = m.getLine();
                long debeAmount = m.getDebe();
                long haberAmount = m.getHaber();
                long saldo = debeAmount-haberAmount;
                
                if (saldo>=0){
                    if (!debeMap.containsKey(saldo)){
                        debeMap.put(saldo, new ArrayList<Long>());
                    }
                    debeMap.get(saldo).add(line);
                    totalDebe++;
                }else{
                    if (!haberMap.containsKey(-saldo)){
                        haberMap.put(-saldo, new ArrayList<Long>());
                    }
                    haberMap.get(-saldo).add(line);
                    totalHaber++;
                }
            }      
            if (debug)
                System.out.println("[PROCESS2_PA] sizes debe y haber "+movimientos.size()+" "+totalDebe+" "+totalHaber);
            System.out.println("[before proceso2 ..] "+movimientos.size());
            ArrayList<Long> indexes = new ArrayList<Long>();
            if (haberMap.size()>=debeMap.size()){
                long saldoHaberAcc = 0;
                for (int i=movimientos.size()-1;i>=0;i--){
                    Movimiento m = movimientos.get(i);
                    long line = m.getLine();
                    if (linesToKeep.get(line)==0) continue;
                    long debeAmount = m.getDebe();
                    long haberAmount = m.getHaber();
                    long saldo = debeAmount-haberAmount;
                    if (saldo<0){
                        saldoHaberAcc = -saldo;
                        
                        if (debeMap.containsKey(saldoHaberAcc)){
                            //se casan
                            ArrayList<Long> linesDebe = debeMap.get(saldoHaberAcc);
                            if (linesDebe.size()>0){
                                long lineDebe = linesDebe.get(0);
                                linesToKeep.put(line,0);
                                linesToKeep.put(lineDebe,0);                                
                                linesDebe.remove(0);
                                if (debug)
                                    System.out.println("[PROCESS2_PA] casados "+line+" "+lineDebe);
                                
                                //CASAMOS line con lineToDelete
                                sub.casar((short)2,casamientoRef,line);
                                sub.casar((short)2,casamientoRef,lineDebe);
                                casamientoRef++;
                            }                            
                            if (linesDebe.size()==0) debeMap.remove(saldoHaberAcc);
                        }else{    
                            if (debug)
                                System.out.println("[PROCESS2_PA] saldo a probar "+saldo);
                            
                            indexes.clear();
                            indexes.add(line);
                            for (int j=i-1;j>=0;j--){
                                Movimiento mj = movimientos.get(j);
                                long saldodj = mj.getDebe()-mj.getHaber();
                                if (saldodj<0){
                                    indexes.add(mj.getLine());
                                    saldoHaberAcc += -saldodj;   
                                    if (debug)
                                        System.out.println("***[PROCESS2_PA] saldo acc"+saldodj+" || "+saldoHaberAcc);
                                    if (debeMap.containsKey(saldoHaberAcc)){
                                         //se casan
                                        ArrayList<Long> linesDebe = debeMap.get(saldoHaberAcc);
                                        if (linesDebe.size()>0){
                                            long lineDebe = linesDebe.get(0);
                                            indexes.add(lineDebe);
                                            for (int t=0;t<=indexes.size()-1;t++){
                                                long l = indexes.get(t);
                                                sub.casar((short)2,casamientoRef,l);
                                                linesToKeep.put(l,0);
                                                System.out.println("***[PROCESS2_PA] casando linea "+l);
                                            }
                                            sub.casar((short)2,casamientoRef,lineDebe);
                                            //anotamos casamiento
                                            if (debug)
                                                System.out.println("***[PROCESS2_PA] casamiento "+casamientoRef);
                                            casamientoRef++;                               
                                            linesDebe.remove(0);
                                        }                            
                                        if (linesDebe.size()==0) debeMap.remove(saldoHaberAcc);                                        
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }else{
                long saldoDebeAcc = 0;
                for (int i=movimientos.size()-1;i>=0;i--){
                    Movimiento m = movimientos.get(i);
                    long line = m.getLine();
                    if (linesToKeep.get(line)==0) continue;
                    long debeAmount = m.getDebe();
                    long haberAmount = m.getHaber();
                    long saldo = debeAmount-haberAmount;
                    if (saldo>0){
                        saldoDebeAcc = saldo;
                        
                        if (haberMap.containsKey(saldoDebeAcc)){
                            //se casan
                            ArrayList<Long> linesHaber = haberMap.get(saldoDebeAcc);
                            if (linesHaber.size()>0){
                                long lineHaber = linesHaber.get(0);
                                linesToKeep.put(line,0);
                                linesToKeep.put(lineHaber,0);                                
                                linesHaber.remove(0);
                                if (debug)
                                    System.out.println("[PROCESS2_PA] casados "+line+" "+lineHaber);                                
                                //CASAMOS line con lineToDelete
                                sub.casar((short)2,casamientoRef,line);
                                sub.casar((short)2,casamientoRef,lineHaber);
                                casamientoRef++;
                            }                            
                            if (linesHaber.size()==0) debeMap.remove(saldoDebeAcc);
                        }else{    
                            if (debug)
                                System.out.println("[PROCESS2_PA] saldo a probar "+saldo);
                            
                            indexes.clear();
                            indexes.add(line);
                            for (int j=i-1;j>=0;j--){
                                Movimiento mj = movimientos.get(j);
                                long saldodj = mj.getDebe()-mj.getHaber();
                                if (saldodj>0){
                                    indexes.add(mj.getLine());
                                    saldoDebeAcc += saldodj;   
                                    if (debug)
                                        System.out.println("***[PROCESS2_PA] saldo acc"+saldodj+" || "+saldoDebeAcc);
                                    if (haberMap.containsKey(saldoDebeAcc)){
                                         //se casan
                                        ArrayList<Long> linesHaber = haberMap.get(saldoDebeAcc);
                                        if (linesHaber.size()>0){
                                            long lineHaber = linesHaber.get(0);
                                            indexes.add(lineHaber);
                                            for (int t=0;t<=indexes.size()-1;t++){
                                                long l = indexes.get(t);
                                                sub.casar((short)2,casamientoRef,l);
                                                linesToKeep.put(l,0);
                                                System.out.println("***[PROCESS2_PA] casando linea "+l);
                                            }
                                            sub.casar((short)2,casamientoRef,lineHaber);
                                            //anotamos casamiento
                                            if (debug)
                                                System.out.println("***[PROCESS2_PA] casamiento "+casamientoRef);
                                            casamientoRef++;                               
                                            linesHaber.remove(0);
                                        }                            
                                        if (linesHaber.size()==0) haberMap.remove(saldoDebeAcc);                                        
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // y ahora borramos
             //elminamos movimientos con complejidad O(n)
            System.out.println("[tras proceso2 borrar ..] "+linesToKeep.size());
            Movimiento.removeExclusion(movimientos,linesToKeep);
            System.out.println("[tras proceso2 ..] "+movimientos.size());
	}
	
	/**
	 * Busca que el saldo acumulado sea = al saldo total de la subcuenta, si lo encuentra pone todos los siguientes
	 * movimientos como casados, 
         * Habría que ordenarlo a la inversa..
	 * @param subaccount
	 */
	public void processType3(boolean debug){
            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            double saldoAcc = 0;
            int beginCasados = -1;
            int endCasados = -1;
            
           // for (int i=0;i<movimientos.size()-1;i++){//el ultimo movimiento siempre coincide con el saldo
               // Movimiento m = movimientos.get(i);
                //System.out.println("[PROCESS 3] mov: "+m.toStringDate(""));    
           // }
            for (int i=0;i<movimientos.size()-1;i++){//el ultimo movimiento siempre coincide con el saldo
                Movimiento m = movimientos.get(i);
                //System.out.println("[PROCESS 3] mov: "+m.toStringDate(""));
                long saldo = m.getDebe()-m.getHaber();
                saldoAcc += saldo;
                if (saldoAcc==sub.getSaldoAcc()){
                    //subaccount.applyCasados(i+1,movimientos.size()-1,true);
                    beginCasados = i+1;
                    endCasados = movimientos.size()-1; 
                    break;
                }
            }
            if (beginCasados<0) return;
            if (debug){
                saldoAcc = 0;
                for (int i=0;i<=beginCasados-1;i++){
                        Movimiento m = movimientos.get(i);
                        long saldo = m.getDebe()-m.getHaber();
                        saldoAcc+=saldo;
                        //System.out.println(sub.getMovimientos().get(i).toString()+" "+saldoAcc);
                }
            }
            //eliminamos endCasados-beginCasados+1 elementos
            boolean isCasado = false;
            for (long i=beginCasados;i<=endCasados;i++){
                Movimiento m = movimientos.get(beginCasados);
                isCasado = true;
                sub.casar((short)3,casamientoRef, m.getLine());//añadimos el casamiento
                sub.getMovimientos().remove(beginCasados);
                lineasCasadas.add(m.getLine());
            }        
            
	}
	
	private void manageLinkedLines(LinkedHashMap left,LinkedHashMap right,
                LinkedHashMap linesToRemove,long line,long leftAmount){
            if (right.containsKey(leftAmount)){//se borra del debe y del haber
                ArrayList<Long> lines = (ArrayList<Long>) right.get(leftAmount); 
                if (lines.size()>0){//existen lineas en la derecha-> se borra 1 de cada 1
                    linesToRemove.put(line,1);//linea a borrar del debe
                    linesToRemove.put(lines.get(lines.size()-1),1);//linea a borrar del haber
                    //añadimos casamiento
                    sub.casar((short)4,casamientoRef, line);//añadimos el casamiento del debe
                    sub.casar((short)4,casamientoRef, lines.get(lines.size()-1));//añadimos el casamiento del haber
                    casamientoRef++;//incrementamos referencia
                    //borramos cantidad
                    lines.remove(lines.size()-1);//y borramos el ultimo del haber-> venian ordenados de nuevo a viejo
                    //if (sub.getCodSubCuenta().equalsIgnoreCase("1000000")){
                        //System.out.println("casamiento managedLinks "+leftAmount+" "+line+" "+lines.get(lines.size()-1));
                    //}
                
                }
                if (lines.isEmpty()){//borramos clave si no hay mas elementos
                    right.remove(leftAmount);
                }
            }else{//se aniade a left
                ArrayList<Long> lines = null;
                if (left.containsKey(leftAmount)){
                    lines = (ArrayList<Long>) left.get(leftAmount); 
                }
                if (lines==null) lines = new ArrayList<>();
                lines.add(line);
                left.put(leftAmount, lines);
            }		
	}
	
	/**
	 * Marca como casados los importes iguales
	 * movimientos como casados
         * Previamente está ordenado de mas viejo a mas nuevo, luego hay que ir 
         * cogiendo por el final si hay importes repetidos
         * CasamientoRef empieza en 4
	 * @param subaccount
	 * @throws InterruptedException 
	 */
	/*public  void processType45(){
            LinkedHashMap linesToRemove = new LinkedHashMap();

            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            LinkedHashMap debeMap = new LinkedHashMap();
            LinkedHashMap haberMap = new LinkedHashMap();
            boolean finished = false;
            //formamos array de borrado haciendo matching 1 vs 1
            for (int i=0;i<movimientos.size() && !finished;i++){
                //System.out.println("proccess45: "+i+" / "+movimientos.size());
                Movimiento m = movimientos.get(i);
                long line = m.getLine();
                long debeAmount = m.getDebe();
                long haberAmount = m.getHaber();
                if (debeAmount>0){ //movimiento en el debe
                    manageLinkedLines(debeMap,haberMap,linesToRemove,line,debeAmount);
                }else if (haberAmount>0){
                    manageLinkedLines(haberMap,debeMap,linesToRemove,line,haberAmount);
                }               
            }
            //elminamos movimientos con complejidad O(n)
           // System.out.println("antes de removeMoves()");
            removeMoves(linesToRemove);//se marcan como borrados
           // System.out.println("despues de removeMoves()");
	}*/
        
        /**
         * Casamiento 1 a 1
         * Viene ya ordenado de más viejo a más nuevo,
         * Simplemnete creamos las dos listas Hash de debe y de haber
         */
        public  void processType45(){
            LinkedHashMap<Long,Boolean> linesToRemove = new LinkedHashMap();

            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            LinkedHashMap<Long,ArrayList> debeMap = new LinkedHashMap();
            LinkedHashMap<Long,ArrayList> haberMap = new LinkedHashMap();
            for (int i=0;i<movimientos.size();i++){
                Movimiento m = movimientos.get(i);
                long line = m.getLine();
                long debeAmount = m.getDebe();
                long haberAmount = m.getHaber();
                long saldo = debeAmount-haberAmount;
                
                if (saldo>=0){
                    if (!debeMap.containsKey(saldo)){
                        debeMap.put(saldo, new ArrayList<Long>());
                    }
                    debeMap.get(saldo).add(line);
                }else{
                    if (!haberMap.containsKey(-saldo)){
                        haberMap.put(-saldo, new ArrayList<Long>());
                    }
                    haberMap.get(-saldo).add(line);
                }
            }             
            //hacemos el matching 1:1
            for (int i=0;i<movimientos.size();i++){
                Movimiento m = movimientos.get(i);                                
                long line = m.getLine();
                long debeAmount = m.getDebe();
                long haberAmount = m.getHaber();
                long saldo = debeAmount-haberAmount;
                
                if (linesToRemove.containsKey(line)) continue;
  
                if (saldo>=0){
                    //buscamos en haber
                    if (haberMap.containsKey(saldo)){
                        ArrayList<Long> lines = haberMap.get(saldo);
                        if (lines!=null && lines.size()>0){                            
                            long lineToDelete = lines.get(0);
                            //CASAMOS line con lineToDelete
                            sub.casar((short)4,casamientoRef,line);//añadimos el casamiento del debe
                            sub.casar((short)4,casamientoRef,lineToDelete);//añadimos el casamiento del haber
                            casamientoRef++;//incrementamos referencia
                            
                            System.out.println("[CASANDO 45] "+line+" "+lineToDelete+" "+saldo);
                            //marcamos para borrar y la quitamos del array
                            linesToRemove.put(line,true);
                            linesToRemove.put(lineToDelete,true);
                            lines.remove(0);
                        }
                    }
                }
            }            
            //elminamos movimientos con complejidad O(n)
            removeMoves(linesToRemove);//se marcan como borrados
	}
	
	public void apply3dSum(ArrayList<Movimiento> movimientos,int index,
                short type,
                boolean debug) throws InterruptedException{
            int i = index;
            while (i<movimientos.size()-2){               
                long importe1 = movimientos.get(i).getImporte();
                int t = movimientos.size()-1;
                int j = i+1;
                boolean removed = false;
                while (j<t && !removed){
                        long importe2 = movimientos.get(j).getImporte();
                        long importe3 = movimientos.get(t).getImporte();

                        long sum = importe1+importe2+importe3;
                        if (sum==0){
                                lineasCasadas.add(movimientos.get(t).getLine());
                                lineasCasadas.add(movimientos.get(j).getLine());
                                lineasCasadas.add(movimientos.get(i).getLine());
                                //añadimos casamientos
                                sub.casar(type,casamientoRef, movimientos.get(t).getLine());
                                sub.casar(type,casamientoRef, movimientos.get(j).getLine());
                                sub.casar(type,casamientoRef, movimientos.get(i).getLine());
                                casamientoRef++;
                                //eliminamos de movimientos
                                movimientos.remove(t);
                                movimientos.remove(j);
                                movimientos.remove(i);

                                if (debug){
                                        System.out.println("[apply3dSum] deleted: "+importe1+" + "+importe2+" + "+importe3);
                                }
                                removed = true;
                        }else if (sum>0){
                                t--;
                        }else if (sum<0){
                                j++;
                        }
                        //finalizacion por tiempo
                        long actualTime = System.currentTimeMillis();
                        if (actualTime>=stopTime){
                                //System.out.println("[apply3dSum] saliendo");
                                return;
                        }else{
                                //Thread.sleep(1);
                        }
                }
                if (!removed){
                        i++;
                }
            }
	}
	
	private void apply4dSum(ArrayList<Movimiento> movimientos,int index,
                short type,
                boolean debug){
		//System.out.println("[apply4dSum] init");
		int i = index;
		//long beginTime = System.nanoTime();
		//long endTime = System.nanoTime();
		while (i<movimientos.size()-3){
			//if (i%10==0)
				//System.out.println("processing i..."+i);
			/*if (debug){
				System.out.println("processing..."+i);
			}*/
			long importe1 = movimientos.get(i).getImporte();
			int j = i+1;
			boolean removed = false;
			while (j<movimientos.size()-2 && !removed){
				//if (j%10==0)
				//System.out.println("processing j..."+j);
				long importe2 = movimientos.get(j).getImporte();
				int t = j+1;
				int s = movimientos.size()-1;
				while (t<s && !removed){
					long importe3 = movimientos.get(t).getImporte();
					long importe4 = movimientos.get(s).getImporte();
					long sum = importe1+importe2+importe3+importe4;
					if (sum==0){
						lineasCasadas.add(movimientos.get(s).getLine());
						lineasCasadas.add(movimientos.get(t).getLine());
						lineasCasadas.add(movimientos.get(j).getLine());
						lineasCasadas.add(movimientos.get(i).getLine());
                                                //añadimos casamientos
                                                sub.casar(type,casamientoRef, movimientos.get(s).getLine());
                                                sub.casar(type,casamientoRef, movimientos.get(t).getLine());
                                                sub.casar(type,casamientoRef, movimientos.get(j).getLine());
                                                sub.casar(type,casamientoRef, movimientos.get(i).getLine());
                                                casamientoRef++;
                                                //eliminamos de movimientos
						movimientos.remove(s);
						movimientos.remove(t);
						movimientos.remove(j);
						movimientos.remove(i);
						
						if (debug){
							System.out.println("[apply4dSum] deleted: "+importe1+" + "+importe2+" + "+importe3+" + "+importe4);
						}
						removed = true;
					}else if (sum>0){
						s--;
					}else if (sum<0){
						t++;
					}
					//finalizacion por tiempo
					long actualTime = System.currentTimeMillis();
					if (actualTime>=stopTime){
						//System.out.println("[apply4dSum] return");
						return;
					}else{
						//Thread.sleep(1);
					}
				}//t
				j++;//incrementamos siempre porque sale si removed = true;
			}//j
			if (!removed){
				i++;
			}
		}//i
		//System.out.println("[apply4dSum] finished");
	}
	
	private void apply5dSum(ArrayList<Movimiento> movimientos,int index,
                short type,
                boolean debug){
		//System.out.println("[apply5dSum] init");
		int i = index;
		while (i<movimientos.size()-4){
			/*if (debug){
				System.out.println("[apply5dSum] processing..."+i);
			}*/
			long importe1 = movimientos.get(i).getImporte();
			int j = i+1;
			boolean removed = false;
			while (j<movimientos.size()-3 && !removed){
				long importe2 = movimientos.get(j).getImporte();
				int t = j+1;
				while (t<movimientos.size()-2 && !removed){
					long importe3 = movimientos.get(t).getImporte();
					int s = t+1;
					int z = movimientos.size()-1;
					while (s<z && !removed){
						long importe4 = movimientos.get(s).getImporte();
						long importe5 = movimientos.get(z).getImporte();
						long sum = importe1+importe2+importe3+importe4+importe5;
						if (sum==0){							
                                                    lineasCasadas.add(movimientos.get(z).getLine());
                                                    lineasCasadas.add(movimientos.get(s).getLine());
                                                    lineasCasadas.add(movimientos.get(t).getLine());
                                                    lineasCasadas.add(movimientos.get(j).getLine());
                                                    lineasCasadas.add(movimientos.get(i).getLine());
                                                    //añadimos casamientos
                                                    sub.casar(type,casamientoRef, movimientos.get(z).getLine());
                                                    sub.casar(type,casamientoRef, movimientos.get(s).getLine());
                                                    sub.casar(type,casamientoRef, movimientos.get(t).getLine());
                                                    sub.casar(type,casamientoRef, movimientos.get(j).getLine());
                                                    sub.casar(type,casamientoRef, movimientos.get(i).getLine());
                                                    casamientoRef++;
                                                    //eliminamos de movimientos
                                                    movimientos.remove(z);
                                                    movimientos.remove(s);
                                                    movimientos.remove(t);
                                                    movimientos.remove(j);
                                                    movimientos.remove(i);

                                                    if (debug){
                                                            System.out.println("[apply5dSum] deleted: "+importe1+" + "+importe2+" + "+importe3+" + "+importe4+" + "+importe5);
                                                    }
                                                    removed = true;
						}else if (sum>0){
							z--;
						}else if (sum<0){
							s++;
						}
						//System.out.println("[apply5dSum] no breakreached: "+isBreakPointReached+" "+task.printBreakInfo());

						long actualTime = System.currentTimeMillis();
						if (actualTime>=stopTime){
							//System.out.println("[apply5dSum] STOP TIME");
							return;
						}else{
							//Thread.sleep(1);
						}
					}//s
					t++;//incrementamos siempre porque sale si removed = true;
				}//t
				j++;
			}//j
			if (!removed){
				i++;
			}
		}//i
		//System.out.println("[apply5dSum] finished");
	}
	
	/**
	 * La lista debe estar ordenada por saldos de menor a mayor (ascending)
	 * Suma a+b+c = 0; 
	 * @param subaccount
	 * @throws InterruptedException 
	 */
	public void processType6(boolean debug){
            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            try {
                apply3dSum(movimientos,0,(short)6,debug);
            } catch (InterruptedException e) {			
                e.printStackTrace();
            }
	}
		
	/**
	 * La lista debe estar ordenada por saldos de menor a mayor (ascending)
	 * Suma a+b+c+d = 0; 
	 * @param subaccount
	 * @throws InterruptedException 
	 */
	public  void processType7(boolean debug){
            //System.out.println("[processType7] init");
            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            apply4dSum(movimientos,0,(short)7,debug);
	}
	
	/**
	 * La lista debe estar ordenada por saldos de menor a mayor (ascending)
	 * Suma a+b+c+d+e = 0; 
	 * @param subaccount
	 * @throws InterruptedException 
	 */
	public void processType8(boolean debug){
            //System.out.println("[processType8] init");
            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            apply5dSum(movimientos,0,(short)8,debug);
	}
	
	/**
	 * Reconstrucción hacia atrás del saldo
         * Necesita movimientos ordenados de menor a mayor
	 * 
	 * @param subaccount
	 */
	public void processType9(boolean debug){   
             //if (sub.getCodSubCuenta().contains("4300067"))
                //System.out.println("[processType9] Subcuenta: "+sub.getCodSubCuenta()+" "+sub.recalculateSaldo());
            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            int i = movimientos.size()-1;
            long acc = 0;
            while (i>=0){
                Movimiento m = movimientos.get(i);
		acc += m.getImporte();                
                if (acc==saldoInicial){
                    //borrar movimientos dsde movimientos.size()-1 a i y salir
                    int movs = movimientos.size();
                    //System.out.println("a borrar "+sub.getCodSubCuenta());
                    sub.casarTodos((short)9,0,i-1,casamientoRef);
                    Movimiento.remove(movimientos,0,i-1);
                    break;
                }
                i--;
            }
	}
	
	/**
	 * elminamos movimientos con complejidad O(n)
	 * @param subaccount
	 * @param linesToRemove
	 */
	private void removeMoves(LinkedHashMap linesToRemove){
		                                
            ArrayList<Movimiento> movimientos = sub.getMovimientos();
            ArrayList<Movimiento> movimientosNotRemoved = new ArrayList<Movimiento>();
            int i = 0;
            int last=0;
           System.out.println("[removeMoves] movimientos y lineas a eliminar: "+movimientos.size()+" "+linesToRemove.size());
            while (i<movimientos.size()){
                Movimiento m = movimientos.get(i);
                long line = m.getLine();
                if (linesToRemove.containsKey(line)){
                    //movimientos.remove(i);//muy lento es mejor marcar
                    m.setRemoved(true);
                    lineasCasadas.add(line);                     
                    i++;
                }else{
                    Movimiento m2 = new Movimiento(m.getLine(),m.getNumAsiento(),m.getDebe(),m.getHaber(),m.getDay(),m.getMonth(),m.getYear(),m.isCasado());
                    movimientosNotRemoved.add(m2);
                    i++;
                }              
            }
            
            sub.assignMovimientos(movimientosNotRemoved);
            //.getMovimientos().clear();
            //sub.movimientos = movimientosNotRemoved;
	}

    public SubAccount getSub() {
        return sub;
    }

    public void setSub(SubAccount sub) {
        this.sub = sub;
    }

    public AtomicInteger getSum() {
        return sum;
    }

    public void setSum(AtomicInteger sum) {
        this.sum = sum;
    }

    public AtomicBoolean getIsBreakPointReached() {
        return isBreakPointReached;
    }

    public void setIsBreakPointReached(AtomicBoolean isBreakPointReached) {
        this.isBreakPointReached = isBreakPointReached;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public static long getStopTime() {
        return stopTime;
    }

    public static void setStopTime(long stopTime) {
        CallableCasarSubcuenta.stopTime = stopTime;
    }

    public int getMaxSecs() {
        return maxSecs;
    }

    public void setMaxSecs(int maxSecs) {
        this.maxSecs = maxSecs;
    }

    public boolean isIsPagoAplazadoMode() {
        return isPagoAplazadoMode;
    }

    public void setIsPagoAplazadoMode(boolean isPagoAplazadoMode) {
        this.isPagoAplazadoMode = isPagoAplazadoMode;
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public long getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(long saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public long getMovimientosIniciales() {
        return movimientosIniciales;
    }

    public void setMovimientosIniciales(long movimientosIniciales) {
        this.movimientosIniciales = movimientosIniciales;
    }

    public long getCasamientoRef() {
        return casamientoRef;
    }

    public void setCasamientoRef(long casamientoRef) {
        this.casamientoRef = casamientoRef;
    }

    public ArrayList<Long> getLineasCasadas() {
        return lineasCasadas;
    }

    public void setLineasCasadas(ArrayList<Long> lineasCasadas) {
        this.lineasCasadas = lineasCasadas;
    }
	
	private void executeStep(int index,boolean debug){
		
            switch(index){
                case 0:
                     if (this.isPagoAplazadoMode){
                         processType0();
                     }
                     break;
                case 1:
                    casamientoRef = 1;
                    processType1();//busqueda de saldoordenado de amyor fecha a menor
                    break;
		case 2:
                    casamientoRef = 1;
                    if (this.isPagoAplazadoMode){
                        processType2_pagoAplazado(debug);
                    }else{
                        processType2(debug);
                    }
                    break;
                case 3: 
                    casamientoRef = 1;
                    processType3(debug);
                    break;
                case 4:
                    casamientoRef = 1;
                    processType45();//casacion, interesa ordenado de menor fecha a mayor
                    break;
		case 5:
                    casamientoRef = 1;
                    processType6(debug);//casacion
                    break;
		case 6:
                    casamientoRef = 1;
                    processType7(debug);//casacion
                    break;
		case 7:
                    casamientoRef = 1;
                    processType8(debug);
                    break;
                case 8:
                    casamientoRef = 1;
                    //processType9(debug);
                    break;
                case 9:
                    casamientoRef = 1;
                    processType9(debug);
                    break;
            }
	}
        
        private long calculoSaldo(ArrayList<Movimiento> movs){
            long saldo = 0;
            for (int i=0;i<movs.size();i++){
                Movimiento m = movs.get(i);
                saldo += m.getDebe()-m.getHaber();
            }
            return saldo;
        }
	
	private void executeCoreProcess() throws InterruptedException{
             System.out.println("***EXECUTE CORE PROCESS***");
            int pmax = 9;          
            //PROCESOS de 1 a 7 (4-5 JUNTOS)
            boolean finished = false;
            boolean sorted = false;
            int p=1;
            long saldo = calculoSaldo(sub.getMovimientos());
            System.out.println(sub.getCodSubCuenta()+" [BUCLE] Empezando movimientos: "+sub.getMovimientos().size()+" || "+saldo);
           debug = true;
            for (p=0;p<=pmax && !finished;p++){
                if (debug)
                    System.out.println("["+sub.getCodSubCuenta()+"] proccess siguiente y movimientos "+p+" "+sub.getMovimientos().size());
                if (p>=5 && !sorted){
                    Movimiento.sortMovimientos(sub.getMovimientos());
                    
                    sorted = true;
                }
                if (p==1){//antes = 8
                   // System.out.println(sub.getCodSubCuenta()+" [antes ordenar] ");
                    //para el pago aplazado necesitamos ordenar de mas viejo a mas nuevo
                    //para casacion de atras adelante
                   Movimiento.sortMovimientosByDateAsiento(sub.getMovimientos(),false);  
                   
                    //Movimiento.printMovimientos(sub.getCodSubCuenta(), sub.getMovimientos(), 0,sub.getMovimientos().size()-1);
                    //System.out.println(sub.getCodSubCuenta()+" [fin ordenar] ");   
                  // Movimiento.printMovimientos(sub.getCodSubCuenta(), sub.getMovimientos(), 0,sub.getMovimientos().size()-1);
                }                  
                if (p==4){
                    Movimiento.sortMovimientosByDateAsiento(sub.getMovimientos(),true);
                }                
                if (p==9){//volvemos a ordenar por fecha
                   // System.out.println(sub.getCodSubCuenta()+" [antes ordenar 9] ");
                    Movimiento.sortMovimientosByDateAsiento(sub.getMovimientos(),false);
                    //System.out.println(sub.getCodSubCuenta()+" [fin ordenar 9] ");
                }                                  
                executeStep(p,debug);
                //Movimiento.printMovimientos(sub.getCodSubCuenta(), sub.getMovimientos(), 0,sub.getMovimientos().size()-1);
                saldo = calculoSaldo(sub.getMovimientos());
                //System.out.println(sub.getCodSubCuenta()+" [BUCLE] terminado paso "+p+" movimientos que quedan: "+sub.getMovimientos().size()+" || "+saldo);
                if (sub.getMovimientos().size()<2){
                    finished = true;
                }
                long actualTime = System.currentTimeMillis();
                if (actualTime>=stopTime && p>=4){ //al menos tiene que hacer el 1vs1                      
                    finished = true;
                }else{
                    //Thread.sleep(1);
                }                
            }           
            //System.out.println("saliendo del core: "+sub.getCodSubCuenta());           
	}
        
        /**
         * Realiza la casación en el orden y por los métodos indicados en
         * array steps
         * @param steps: valores del 1 al 8 (4y5 se corresponden al mismo,5=process6)
         * @throws InterruptedException 
         */
        private void executeCoreProcessCustomSteps(ArrayList<Integer> steps) throws InterruptedException{
            int pmax = 8;          
            //PROCESOS de 1 a 7 (4-5 JUNTOS)
            boolean finished = false;
            boolean sorted = false;
            boolean sortedDateAsiento = false;
            
            for (int i=0;i<steps.size();i++){
                int step = steps.get(i);
                if (step>=5 && step<8 && !sorted){
                    Movimiento.sortMovimientos(sub.getMovimientos());
                    sorted = true;
                }
                if (step==8 && !sortedDateAsiento){
                    Movimiento.sortMovimientosByDateAsiento(sub.getMovimientos(),false);
                    sortedDateAsiento = true;
                }
                
                executeStep(step,debug);
                if (sub.getMovimientos().size()<2){
                    finished = true;
                }
                long actualTime = System.currentTimeMillis();
                if (actualTime>=stopTime){                     
                    finished = true;
                }else{
                    //Thread.sleep(1);
                }   
            }                   
            //pase lo que pase el procemiento 1 siempre se realiza
            executeStep(1,debug);
	}
	
	@Override
	public SubAccount call() throws Exception {
            startTime = System.currentTimeMillis();
            stopTime = startTime + maxSecs*1000;
            //if (!this.isPagoAplazadoMode){
            executeCoreProcess();
            /*}else {
                //se pasa primero el 1vs1 luego el 1vs2 y el 1vs3
                ArrayList<Integer> steps = new ArrayList<Integer>();
                steps.add(5);steps.add(6);steps.add(7);
                steps.add(0);
                executeCoreProcessCustomSteps(steps);
            }*/
            sum.addAndGet(1);
            //System.out.println("total subcuentas: "+sum.get());
            return sub;
	}
}
