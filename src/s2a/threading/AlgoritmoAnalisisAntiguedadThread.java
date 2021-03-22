package s2a.threading;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import s2a.Movimiento;
import s2a.SubAccount;
import s2a.utils.DateUtils;
import s2a.utils.DecimalFormatRenderer;
import s2a.utils.DecodificationUtils;
import s2a.utils.FileUtils;
import s2a.utils.MathUtils;
import s2a.utils.Period;
import s2a.utils.Registry;

public class AlgoritmoAnalisisAntiguedadThread {

	//Creamos combobox
	JTable table = null;
	JComboBox fromCombo = null;
	JComboBox toCombo = null;
	
	int fInicio = 0;
	int fFin = 999;
	ArrayList<String> analisisDeuda = new ArrayList<String>();
	
	final int NOMBRE_SUBCUENTA_COL = 1;
	final int APERTURA_COL = 2;
	
	/**
	 * Procedimiento que hace el trabajo efectivo, de agrupacion de saldos por cada subcuenta
	 * @param subAccounts
	 * @param startDate
	 * @param endDate
	 * @param period
	 * @param periodSize
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 * @throws IOException 
	 */
	private void coreProcess(LinkedHashMap subAccounts,String outputCSV, Calendar startDate,
			Calendar endDate, Period period, int periodSize,int totalCores,int asientoApertura,
			LinkedHashMap saldosApertura) throws InterruptedException, ExecutionException, IOException {
		
	    ExecutorService executorService=Executors.newFixedThreadPool(totalCores);
        List<Callable<String>> tasks=new ArrayList<Callable<String>>();

        boolean debug = false;
        Iterator<SubAccount> it = subAccounts.values().iterator();
        int i=0;
		while (it.hasNext()
				//&& i<=0//debug
				){
			SubAccount sub = it.next();
			if (Math.abs(sub.recalculateSaldo())>=0.01){
				//Create MyCallable instance
		        Callable<String> callable = new CallableAnalisisAntiguedad(sub,startDate,endDate,period,periodSize,debug);
				tasks.add(callable);
			}
			i++;
		}
		System.out.println("total tasks added: "+tasks.size());
		int total=0;
		List<Future<String>> results = executorService.invokeAll(tasks);
        executorService.shutdown();
        
        String header="SUBCUENTA;NOMBRE;APERTURA;"+DateUtils.getBoxHeader(period)+";SALDO TOTAL;DEBE;HABER";//se añade debe y haber total
        System.out.println("Results size: "+results.size());
        analisisDeuda.add(header);
        ArrayList<String> res = new ArrayList<String>();//auxiliar
        DecimalFormat formatter = new DecimalFormat("###,##0.00");
        for (int j=0;j<results.size();j++){
        	Future<String> result = results.get(j);
        	String resultStr = result.get();
        	
        	ArrayList<String> resultArray = new ArrayList<String>(Arrays.asList(resultStr.split(";")));
        	//obtenemos codigo de subcuenta y su saldo de aperutra
        	String subCuentaCod = resultArray.get(0);
        	long saldoApertura = 0;
        	if (saldosApertura.containsKey(subCuentaCod)){
        		saldoApertura = (long) saldosApertura.get(subCuentaCod);
        	}
        	//add saldoApertura detras de subcuenta
        	resultArray.add(APERTURA_COL, formatter.format(saldoApertura*0.01)); //lo met
        	resultStr="";
        	for (int r=0;r<resultArray.size();r++) resultStr+=resultArray.get(r)+";";
        	
        	res.add(resultStr.trim());
        }
        //ordenamos
        sortRows(res); //comentado por debug
        //calculamos linea total final
        String totalRow = calculateTotalRow(";TOTAL;",res);//sin header
        res.add(0,totalRow);
        //copiamos
        for (int r=0;r<=res.size()-1;r++) analisisDeuda.add(res.get(r));
        //metemos lineas en blanco en posiciones 1 y 3
        int totalCols = analisisDeuda.get(0).split(";").length;
        String blankLine = "";
        for (int b=0;b<totalCols;b++){
        	blankLine +=";";
        }
        //lineas en blanco
        analisisDeuda.add(2,blankLine);
        analisisDeuda.add(1,blankLine);
        //Escritura a CSV
        FileUtils.writeCSV(outputCSV, analisisDeuda);
       //progreso a 100
       Registry.writeRegistryInt("HKCU\\Software\\s2a\\AnalisisDeuda", "progreso",100);
       
       //return analisisDeuda;
	}
	
	private String calculateTotalRow(String header,ArrayList<String> res) {
            String lastRow = "";
            if (res.size()==0) return "";

            int numCols = res.get(0).split(";").length;

            for (int i=APERTURA_COL;i<numCols;i++){ //por 2 porque la columna de idsubcuenta y el nombre de ella no suman
                long suml=0;
                for (int r=0;r<res.size();r++){
                        String row = res.get(r);
                        //System.out.println(row);
                        long partial = MathUtils.doubleStrToLong(row.split(";")[i],2);
                        suml += partial;
                        /*if (i==2){
                                System.out.println(row.split(";")[i]+" "+partial+" "+suml);
                        }*/
                }
                double sum=suml*0.01;
                DecimalFormat formatter = new DecimalFormat("###,##0.00");
                lastRow+=formatter.format(sum)+";";
            }
            return header+lastRow.trim();
	}

	/**
	 * app principal de generaciï¿½n del analisis de antiguedad
	 * @param inputCSV
	 * @param outputCSV
	 * @param startDate
	 * @param endDate
	 * @param startLine
	 * @param period
	 */
	public void runApp(String inputCSV,String outputCSV,
			int startLine,int subAccountCol,
			int asientoCol,
			int debeCol,int haberCol,
			int fechaCol,int nombreCol,
			int totalCores,
			String startDateStr,String endDateStr,String periodStr,
			Integer asientoApertura,
			String subInicioStr,String subFinalStr,
			boolean showTable,boolean delphiEncoding,
			LinkedHashMap asientosToDelete
			){
		
		Registry.writeRegistryString("HKCU\\Software\\s2a\\AnalisisDeuda", "mensaje","runApp");
		
		System.out.println("\n****PARAMETROS LEIDOS: "+inputCSV+" "+outputCSV
				+" STARTDATE : "+startDateStr+" ENDDATE : "+endDateStr
				+" STARTLINE : "+startLine+" PERIODSTR : "+periodStr
				+" ASIENTOAPERTURA : "+asientoApertura
				+" TOTALCORES: "+totalCores);
		
		//validacion de params
		/*if (!parametersValidation){
			return;
		}*/
		
		//preparacion de params
		Calendar startDate = DecodificationUtils.decodeDate(startDateStr);
		Calendar endDate   = DecodificationUtils.decodeDate(endDateStr);
                startDate.set(Calendar.HOUR_OF_DAY, 0);
                startDate.set(Calendar.MINUTE, 0);
                startDate.set(Calendar.SECOND, 0);
                endDate.set(Calendar.HOUR_OF_DAY, 23);
                endDate.set(Calendar.MINUTE, 59);
                endDate.set(Calendar.SECOND, 59);
		Period period      = DecodificationUtils.decodePeriod(periodStr);
		int periodSize     = DecodificationUtils.decodePeriodSize(periodStr);
		int subInicio      = Integer.valueOf(subInicioStr);
                int subFinal       = Integer.valueOf(subFinalStr);
		
		//lectura fichero
	    LinkedHashMap linesToKeep = new LinkedHashMap();
		LinkedHashMap linesNotProcessed = new LinkedHashMap();
		LinkedHashMap saldosApertura = new LinkedHashMap();
		
		LinkedHashMap subAccounts = FileUtils.readFileToMap(inputCSV,startLine,
				subAccountCol,asientoCol,debeCol,haberCol,fechaCol,nombreCol,
				subInicio,subFinal,startDate,endDate,true,delphiEncoding,asientoApertura,saldosApertura,asientosToDelete,linesNotProcessed);
		int totalInicioMovs = AlgoritmoCasacionThread.calculateTotalMovs(subAccounts);
		System.out.println("\n****FICHERO LEIDO Numero de subcuentas y total movimientos: "+subAccounts.size()+" "+totalInicioMovs);

		ArrayList<String> values = null;
		try {
			coreProcess(subAccounts,outputCSV,startDate,endDate,period,periodSize,totalCores,asientoApertura,saldosApertura);//realizamos el proceso y obtenemos lineas a mantener
		} catch (Exception e) {
			Registry.writeRegistryInt("HKCU\\Software\\s2a\\AnalisisDeuda", "estado",3);
	   		Registry.writeRegistryString("HKCU\\Software\\s2a\\AnalisisDeuda", "mensaje","Error: "+e.getMessage());
		   e.printStackTrace();
		   return; 
		}
		
		System.out.println("\n****ESCRITURA REGISTRO****");
   		Registry.writeRegistryInt("HKCU\\Software\\s2a\\AnalisisDeuda", "estado",2);
   		Registry.writeRegistryString("HKCU\\Software\\s2a\\AnalisisDeuda", "mensaje","OK");
   		
   		createDeudasTable(analisisDeuda,asientoApertura,showTable);
   		
   		System.out.println("\nPROGRAMA FINALIZADO");
	}
		
	private void sortRows(ArrayList<String> analisisDeuda) {
		// TODO Auto-generated method stub
				 Collections.sort(analisisDeuda, new Comparator<String>() {

				        public int compare(String m1, String m2) {
				        	String importe1 = m1.split(";")[0];
				        	String importe2 = m2.split(";")[0];
				        	return importe1.compareTo(importe2)<0 ? -1
				        	         : importe1.compareTo(importe2)>0 ? 1
				        	         : 0;
				        }
				    });
		
	}

	private void createDeudasTable(ArrayList<String> values,
			int asientoApertura,
			boolean showTable){
		
		//String totalLine = values.get(1);
		//totalLine += <html><b>
		
		//DefaultTableCellRenderer header = new DefaultTableCellRenderer();
		//header.setFont(header.getFont().deriveFont(Font.BOLD));
		
		//CREACION DE LA TABLA
   		Vector rowData = new Vector();
   	    for (int i = 1; i < values.size(); i++) {
   	      Vector colData = new Vector(Arrays.asList(values.get(i).split(";")));
   	      rowData.add(colData);
   	    }
   		Vector columnNamesV = new Vector(Arrays.asList(values.get(0).split(";")));
   		
   		int columnCount = columnNamesV.size();
   		int rowCount = rowData.size();
   		
   		//Creamos tabla
   		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
   		rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
   		table = new JTable(rowData, columnNamesV);
   		for ( int c=2;c<table.getColumnCount();c++)
   			table.getColumnModel().getColumn(c).setCellRenderer(rightRenderer);
   		
   		//table.getColumnModel().getColumn(2).setCellRenderer(
   	     //    new DecimalFormatRenderer() );
   		
   		//TableColumnModel model = table.getColumnModel();
   		//model.getColumn(0).setHeaderRenderer(header);
   		
      //creamos JFrame
   		JFrame f = new JFrame("ANALISIS DEUDA");
   		//f.setLayout(new BorderLayout());
   	    ImageIcon img = new ImageIcon("s2a.jpg");
   	    f.setIconImage(img.getImage());
   	    f.setExtendedState(JFrame.MAXIMIZED_BOTH); 
   	    //aï¿½adimos paneles
   	   // f.add(labelPanel,BorderLayout.NORTH);
   	   // f.add(tablePanel,BorderLayout.CENTER);
   	   
   	    f.add(new JScrollPane(table));
   	   
   	    /*tablePanel.add(new JScrollPane(table));
   	    labelPanel.add(labelCombo1);
   	    labelPanel.add(fromCombo);
   	    labelPanel.add(labelCombo2);
   	    labelPanel.add(toCombo);
   	    labelPanel.add(bFiltrar);*/
   	    
   	    //mostramos
   	    if (showTable)
   	    	f.setVisible(true);
	}
	

	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();
		
		if (args.length<11){
			System.out.println("[ERROR] Faltan params. Se esperan: input output startLine "
					+"subAccountCol debeCol haberCol fechaCol totalCores "
					+"startDate endDate period");
			return;
		}
		
		LinkedHashMap asientosToDelete = new LinkedHashMap();
		String inputCSV		= args[0];
		String outputCSV 	= args[1];
		int startLine       =  1;
		int subAccountCol 	= -1;
		int asientoCol      = -1;
		int debeCol 		= -1;
		int haberCol 		= -1;
		int fechaCol 		= -1;
		int nombreCol		= -1;//nombre subcuenta
		int maxCores        = 100;
		String startDate    = "01/01/1900";
		String endDate      = "31/12/2999";
		String period       = "-m";
		String subInicio    = "000";
		String subFinal     = "999";
		int asientoApertura = -1;
		boolean showTable = false;
		boolean delphiEncoding = false;
		
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
			if (option.contains("-nomCol")){
				nombreCol = Integer.valueOf(args[i].substring(7, args[i].length()));  
			}
			if (option.contains("-startDate")){
				startDate = option.substring(10, option.length());  
			}
			if (option.contains("-endDate")){
				endDate = option.substring(8, option.length());  
			}
			if (option.contains("-maxCores")){
				maxCores = Integer.valueOf(args[i].substring(9, args[i].length()));  
			}
			if (option.equalsIgnoreCase("-m") || option.equalsIgnoreCase("-t") || option.equalsIgnoreCase("-s")){
				period = option;  
			}
			if (option.contains("-subi")){
				subInicio = option.substring(5, option.length());  
			}
			if (option.contains("-subf")){
				subFinal = option.substring(5, option.length());  
			}
			if (option.equalsIgnoreCase("-SHOWTABLE")) showTable = true;
			if (option.equalsIgnoreCase("-encoding16")){
				delphiEncoding = true;
			}
			if (option.contains("-aa")){
				asientoApertura = Integer.valueOf(option.substring(3, option.length()));
			}
		}
		
		//if (asientoApertura!=-1)
			//asientosToDelete.put(asientoApertura, 1);//no introducimos el asiento de apertura en los asientos a borrar
		
		try {
			new AlgoritmoAnalisisAntiguedadThread().runApp(inputCSV,outputCSV,
					startLine,subAccountCol,
					asientoCol,
					debeCol,haberCol,fechaCol,nombreCol,
					maxCores,
					startDate,endDate,period,
					asientoApertura,
					subInicio,subFinal,
					showTable,delphiEncoding,asientosToDelete);
		} catch (Exception e) {
	            e.printStackTrace();
	    }
		
		long stopTime = System.currentTimeMillis();
		long diff = stopTime-startTime;
	}

}
