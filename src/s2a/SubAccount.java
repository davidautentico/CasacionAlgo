package s2a;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class SubAccount {
	String codSubCuenta = "-1";//tamanio maximo: 20 digitos
	String nombreSubCuenta = "Subcuenta no definida";
	long saldoApertura = 0;
	long saldoAcc = 0;
	long totalDebe = 0; //en realidad son double XX,XX multiplicados por 100, para cantidad real, dividir por 100
	long totalHaber = 0;
	int totalMovimientos = 0;
	int totalCasados = 0;
	ArrayList<Movimiento> movimientos = new ArrayList<>();
	ArrayList<Movimiento> movimientosDebe = new ArrayList<>();
	ArrayList<Movimiento> movimientosHaber = new ArrayList<>();
        ArrayList<Casamiento> casamientos = new ArrayList<>();

        public SubAccount() {           
        }

        public ArrayList<Casamiento> getCasamientos() {
            return casamientos;
        }
        public void setCasamientos(ArrayList<Casamiento> casamientos) {
            this.casamientos = casamientos;
        }		
	public String getNombreSubCuenta() {
		return nombreSubCuenta;
	}
	public void setNombreSubCuenta(String nombreSubCuenta) {
		this.nombreSubCuenta = nombreSubCuenta;
	}
	public long getSaldoApertura() {
		return saldoApertura;
	}
	public void setSaldoApertura(long saldoApertura) {
		this.saldoApertura = saldoApertura;
	}
	public String getCodSubCuenta() {
		return codSubCuenta;
	}
	public void setCodSubCuenta(String codSubCuenta) {
		this.codSubCuenta = codSubCuenta;
	}
	public long getSaldoAcc() {
		return saldoAcc;
	}
	public void setSaldoAcc(long saldoAcc) {
		this.saldoAcc = saldoAcc;
	}
	
	public long getTotalDebe() {
		return totalDebe;
	}
	public void setTotalDebe(long totalDebe) {
		this.totalDebe = totalDebe;
	}
	public long getTotalHaber() {
		return totalHaber;
	}
	public void setTotalHaber(long totalHaber) {
		this.totalHaber = totalHaber;
	}
	
	public int getTotalMovimientos() {
		return totalMovimientos;
	}
	public void setTotalMovimientos(int totalMovimientos) {
		this.totalMovimientos = totalMovimientos;
	}
 
	public int getTotalCasados() {
		return totalCasados;
	}
	public void setTotalCasados(int totalCasados) {
		this.totalCasados = totalCasados;
	}
	public ArrayList<Movimiento> getMovimientos() {
		return movimientos;
	}
	public void setMovimientos(ArrayList<Movimiento> movimientos) {
		this.movimientos = movimientos;
	}
	public ArrayList<Movimiento> getMovimientosDebe() {
		return movimientosDebe;
	}
	public void setMovimientosDebe(ArrayList<Movimiento> movimientosDebe) {
		this.movimientosDebe = movimientosDebe;
	}
	public ArrayList<Movimiento> getMovimientosHaber() {
		return movimientosHaber;
	}
	public void setMovimientosHaber(ArrayList<Movimiento> movimientosHaber) {
		this.movimientosHaber = movimientosHaber;
	}
	
	
	public void addDebe(long amount){
		totalDebe+=amount;
		saldoAcc+=amount;
	}
	
	public void addHaber(long amount){
		totalHaber+=amount;
		saldoAcc-=amount;
	}
	

	public void incTotalMovimientos(){
		totalMovimientos++;
	}
	
	public String toString(){
		this.totalMovimientos = this.getMovimientosDebe().size()+this.getMovimientosHaber().size();
		return codSubCuenta
				+" "+this.nombreSubCuenta
				+" "+this.totalMovimientos
				+" ("+this.getMovimientosDebe().size()
				+" "+this.getMovimientosHaber().size()
				+") "+this.saldoAcc;
	}
	
	public void copy(SubAccount account){
		this.saldoApertura	= account.saldoApertura;
		this.codSubCuenta = account.getCodSubCuenta();
		this.saldoAcc = account.getSaldoAcc();
		this.totalCasados = account.getTotalCasados();
		this.totalDebe = account.getTotalDebe();
		this.totalHaber = account.getTotalHaber();
		
		//copia de movimientos
		for (int i=0;i<account.getMovimientos().size();i++){
			Movimiento m = new Movimiento();
			m.copy(account.getMovimientos().get(i));
			this.getMovimientos().add(m);
		}
		/*for (int i=0;i<this.movimientosDebe.size();i++){
			Movimiento m = new Movimiento();
			m.copy(this.getMovimientosDebe().get(i));
			newAccount.getMovimientosDebe().add(m);
		}
		for (int i=0;i<this.movimientosHaber.size();i++){
			Movimiento m = new Movimiento();
			m.copy(this.getMovimientosHaber().get(i));
			newAccount.getMovimientosHaber().add(m);
		}*/
		
		
	}
	
	
	/**genera nivel 2 (el1+el2) de movimientos
	 * 
	 * @param haber
	 */
	/*public void generateLevel2(boolean haber){

		ArrayList<Movimiento> array = movimientosHaber;
		if (!haber)
			array = movimientosDebe;
		
		System.out.println("[algocasacion2] array size: "+array.size());
		for (int i=0;i<array.size();i++){
			Movimiento m1 = array.get(i);
			int line1 = m1.getLine();
			long amount1 = m1.getAmount();
			for (int j=i+1;j<array.size()-1;j++){
				Movimiento m2 = array.get(j);
				int line2 = m2.getLine();
				long amount2 = m2.getAmount();
				
				long sum = amount1+amount2;
				
				ArrayList<Integer> lines = null;
				if (haberLevel2.containsKey(sum)){
					//lines = (ArrayList<Integer>) haberLevel2.get(sum); 
				}
				if (lines==null){
					//lines = new ArrayList<Integer>();
					//this.haberLevel2.put(sum, lines);
				}
				
				this.haberLevel2.put(sum, null);
			}
		}
	}*/
	
	
	/***************ALGORITMOS DE CASACION**************/
	/**
	 * Casacion 1 a 1
	 * @param debeStart
	 */
	/*public void algoCasacion1(boolean debeStart){
		ArrayList<Long> sorted = new ArrayList<Long>();
		long maxHaber =0;
		ArrayList<Integer> linesToRemove = new ArrayList<Integer>();
		
		ArrayList<Movimiento> left = this.movimientosDebe;
		LinkedHashMap right = this.haberLevel1;
		int totalRight = this.movimientosHaber.size();
		if (!debeStart){
			left = movimientosHaber;
			right = this.debeLevel1;
			totalRight = this.movimientosDebe.size();
		}
		
		int i = 0;
		while (i<left.size()){
			Movimiento m = left.get(i);
			long lamount = m.getAmount();
			int count = 0;
			boolean matched = false;
			if (right.get(lamount)!=null){
				ArrayList<Integer> lines = (ArrayList<Integer>) right.get(lamount);
				count = lines.size();
				if (count>0){
					matched = true;
					//elimino la primera linea
					int line = lines.get(0);
					//elimino de los movimientos el movimiento de line
					linesToRemove.add(line);
					lines.remove(0);
					totalRight--;
				}
				if (count==0){
					right.remove(lamount);
					//System.out.println("removed "+right.size());
				}else{
					right.put(lamount, lines);
				}
			}
			if (matched){
				left.remove(i);
			}else{
				insertSorted(sorted,lamount);
				i++;
			}
		}
		
		printSorted(sorted,1,10000000);
		int debeMovs = left.size();
		int rightCount=0;
		
		int haberMovs = right.size();
		int totalMovs = debeMovs+totalRight;
		System.out.println("subcuenta totalmovs aBorrar totalLeft totalright || "+
				this.numSubCuenta+" "+totalMovs+" "+linesToRemove.size()
				+" "+debeMovs+" "+totalRight);
	}*/
	
	private static void insertSorted(ArrayList<Long> sorted, long hd) {
		// TODO Auto-generated method stub
		for (int i=0;i<sorted.size();i++){
			long actual = sorted.get(i);
			if (hd>=actual){
				sorted.add(i, hd);
				return;
			}
		}
		sorted.add(hd);
	}
	private static void printSorted(ArrayList<Long> sorted, long hd1,long hd2) {
		// TODO Auto-generated method stub
		for (int i=0;i<sorted.size();i++){
			long actual = sorted.get(i);
			if (actual>=hd1 && actual<=hd2)
			System.out.println(actual);
		}
	}
	
	
	
	
	public void addMovimiento(int line,
			long numAsiento,long aDebe, long aHaber,
			int aDay,int aMonth,int aYear,
			boolean aCasado) {
		// TODO Auto-generated method stub
		Movimiento m = new Movimiento(line,numAsiento,aDebe,aHaber,aDay,aMonth,aYear,aCasado);
		this.movimientos.add(m);
		this.saldoAcc+=aDebe-aHaber;
	}
	
	public void applyCasados(int begin, int end, boolean isCasado) {
		// TODO Auto-generated method stub
		for (int i=begin;i<=end;i++){
			this.movimientos.get(i).setCasado(isCasado);
		}
	}
	
	public long recalculateSaldo() {
		// TODO Auto-generated method stub
		this.saldoAcc = 0;
		for (int i=0;i<this.movimientos.size();i++){
			if (!this.movimientos.get(i).isCasado())//si no esta casado afecta a saldo
				saldoAcc+=this.movimientos.get(i).getDebe()-this.movimientos.get(i).getHaber();
		}
                System.out.println("[processType1] saldoacc + saldo apertura: "+saldoAcc+" "+saldoApertura);
		return saldoAcc+saldoApertura;
	}
        
        public long recalculateDebe() {
		// TODO Auto-generated method stub
		this.totalDebe = 0;
		for (int i=0;i<this.movimientos.size();i++){
			if (!this.movimientos.get(i).isCasado())//si no esta casado afecta a saldo
				totalDebe+=this.movimientos.get(i).getDebe();//-this.movimientos.get(i).getHaber();
		}
                if (saldoApertura>=0)
                    return totalDebe+saldoApertura;
                
                return totalDebe;
	}
        
        public long recalculateHaber() {
		// TODO Auto-generated method stub
		this.totalHaber = 0;
		for (int i=0;i<this.movimientos.size();i++){
			if (!this.movimientos.get(i).isCasado())//si no esta casado afecta a saldo
				totalHaber+=this.movimientos.get(i).getHaber();
		}
                if (saldoApertura<0)
                    return totalHaber+Math.abs(saldoApertura);
                
                return totalHaber;
	}
        
	public void printMovimientos() {
		// TODO Auto-generated method stub
		for (int i=0;i<this.movimientos.size();i++){
			System.out.println(this.movimientos.get(i).toString());
		}
		
	}
	public void printMovimientosDate() {
		// TODO Auto-generated method stub
		for (int i=0;i<this.movimientos.size();i++){
			System.out.println(this.movimientos.get(i).toStringDate(""));
		}
		
	}
	
	public static void copySubAccountsHash(LinkedHashMap subs1,LinkedHashMap subs2){
		Iterator<SubAccount> it = subs1.values().iterator();
		while (it.hasNext()){
			SubAccount sub1 = it.next();
			SubAccount sub2 = new SubAccount();
			sub2.copy(sub1);
			subs2.put(sub1.getCodSubCuenta(), sub2);
		}
		/*System.out.println("[copySubAccountsHash] copy: "+subs2.size()
				+" "+calculateTotalMovs(subs1)
				+" "+calculateTotalMovs(subs2)
				);*/
	}
        
    public void casar(short tipo,long reference,long line){
        
        Casamiento c = new Casamiento();
        c.setLine(line);
        c.setTipo(tipo);
        c.setNumRef(reference);
        
        this.casamientos.add(c);
    }

    public void casarTodos(short tipo,long reference) {
        
        for (int i=0;i<this.movimientos.size();i++){
            Movimiento m = movimientos.get(i);
            Casamiento c = new Casamiento();
            c.setLine(m.getLine());
            c.setTipo(tipo);
            c.setNumRef(reference);
            
            this.casamientos.add(c);
        }
    }
    
    public void casarTodos(short tipo,long reference,int beginIdx,int endIdx) {
        
        for (int i=beginIdx;i<=endIdx;i++){
            Movimiento m = movimientos.get(i);
            Casamiento c = new Casamiento();
            c.setLine(m.getLine());
            c.setTipo(tipo);
            c.setNumRef(reference);
            
            this.casamientos.add(c);
        }
    }
    
     public void casarTodosExclusion(short tipo,long reference,ArrayList<Integer>indexes) {
        
        int limit = indexes.size()-1;
        for (int i=0;i<this.movimientos.size();i++){            
            if (i>limit || indexes.get(i)==0){            
                Movimiento m = movimientos.get(i);
                Casamiento c = new Casamiento();
                c.setLine(m.getLine());
                c.setTipo(tipo);
                c.setNumRef(reference);
                //System.out.println("[casarTodosExclusion] "+m.getLine());
                this.casamientos.add(c);
            }
        }
    }
     
    public void casarTodosInclusion(short tipo,
            long reference,
            ArrayList<Integer>indexes,
            HashMap<Long,Integer> linesToKeep,//se actualiza,
            ArrayList<Integer> icasados
    ) {
        try{
            long saldo = 0;
            for (int i=0;i<this.movimientos.size();i++){  
                int index = indexes.get(i);
                //System.out.println("size icasados: "+icasados.size());                
                if (index==1){    
                    icasados.set(i,index);
                    Movimiento m = movimientos.get(i);
                    Casamiento c = new Casamiento();
                    c.setLine(m.getLine());
                    c.setTipo(tipo);
                    c.setNumRef(reference);
                    linesToKeep.put(m.getLine(),0);//se actualiza
                    saldo += (m.getDebe()-m.getHaber());
                   // System.out.println("[casarTodosInclusion] casado: "+i+" "+(m.getDebe()-m.getHaber())+" | "+saldo);
                    this.casamientos.add(c);
                }
            }
        }catch(Exception e){
            System.out.println("[casarTodosInclusion] excepcion: "+e.getMessage());
        }
        //System.out.println("[casarTodosInclusion] saliendo");
    } 
    
    public void casarTodos(short tipo,int begin,int end,long reference) {
        
        if (begin<0) begin = 0;
        if (end>movimientos.size()-1) end = movimientos.size();
        for (int i=begin;i<=end;i++){
            Movimiento m = movimientos.get(i);
            Casamiento c = new Casamiento();
            c.setLine(m.getLine());
            c.setTipo(tipo);
            c.setNumRef(reference);
            
            this.casamientos.add(c);
        }
    }

     
    public void assignMovimientos(ArrayList<Movimiento> movimientos2) {        
        this.movimientos.clear();
        this.movimientos = movimientos2;
        
	/*this.saldoAcc+=0;
        for (int i=0;i<movimientos2.size();i++){
            Movimiento m = movimientos2.get(i);
            //this.addMovimiento((int)m.getLine(),m.getNumAsiento(),m.getDebe(),m.getHaber(),m.getDay(),m.getMonth(),m.getYear(),m.isCasado());
            this.movimientos.add(m);
            this.saldoAcc+=m.getDebe()-m.getHaber();
        }*/
    }
	
}
