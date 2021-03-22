package s2a.threading;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Callable;

import s2a.Movimiento;
import s2a.SubAccount;
import s2a.utils.DateUtils;
import s2a.utils.Period;

public class CallableAnalisisAntiguedad implements Callable<String> {

	SubAccount sub = null;
	Calendar startDate = null;
	Calendar endDate = null;
	int periodSize = -1;
	Period period = null;
	Boolean debug = false;
	
	public CallableAnalisisAntiguedad(SubAccount sub,Calendar startDate,Calendar endDate,
			Period period,int periodSize,boolean debug){
		
		this.sub = sub;
		this.startDate = startDate;
		this.endDate = endDate;
		this.period = period;
		this.debug = debug;
		this.periodSize = periodSize;
	}
	
	@Override
	public String call() throws Exception {
		//System.out.println("Analizando subcuenta: "+sub.getNumSubCuenta());
		String header = sub.getCodSubCuenta();
		//ordenar por fecha
		Movimiento.sortMovimientosByDate(sub.getMovimientos(),false); //comentado en debug
		if (debug){
			Movimiento.printMovimientos(header,sub.getMovimientos(),true);
		}
		//hacerlo mensual primeramente, mensual=12 cajones, trimestral= 3 cajones, semestral = 2 cajones
		ArrayList<Movimiento> movimientos = sub.getMovimientos();
		Calendar beginCal = startDate;
		beginCal.set(beginCal.get(Calendar.YEAR), beginCal.get(Calendar.MONTH), beginCal.get(Calendar.DATE), 0, 0);
		Calendar endCal = endDate;
		endCal.set(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DATE), 23, 59);
		Calendar actualCal = Calendar.getInstance();
		int totalBoxes = DateUtils.decodeNumberBoxes(period);
		ArrayList<Long> boxes = new ArrayList<Long>();
		for (int i=0;i<totalBoxes;i++) boxes.add(0L);
		for (int i=0;i<movimientos.size();i++){
			Movimiento m = movimientos.get(i);
			m.getCalendar(actualCal);
			//System.out.println("actualCal: "+DateUtils.datePrint(actualCal)+" || "+m.toStringDate(""));
			if (m.isCasado()) continue;//no interesa si esta casado
			if (actualCal.getTimeInMillis()<beginCal.getTimeInMillis()) continue;
			if (actualCal.getTimeInMillis()>endCal.getTimeInMillis()) break;
			//System.out.println(sub.getCodSubCuenta()+" || actualCal: "+DateUtils.datePrint(actualCal)+" "+m.toStringDate("")+" || valido");
			int actualBox = DateUtils.getActualBox(period,actualCal);
			long actualAmount = boxes.get(actualBox);
			boxes.set(actualBox, actualAmount+m.getDebe()-m.getHaber());
		}
	
		DecimalFormat formatter = new DecimalFormat("###,##0.00");
		
		String res = sub.getCodSubCuenta()+";"+sub.getNombreSubCuenta()+";";//+totalBoxes+";";
		for (int i=0;i<boxes.size();i++){
			double value = boxes.get(i)*0.01;
			res+=formatter.format(value)+";";
			//res+=value+";";
		}
		double value      = sub.recalculateSaldo()*0.01;
                double totalDebe  = sub.recalculateDebe()*0.01;
                double totalHaber = sub.recalculateHaber()*0.01;
		res+=formatter.format(value)+";";//añado total mas debe y mas haber
                //AÑADIR TOTAL DEBE (SIN APERTURA)
                res+=formatter.format(totalDebe)+";";//añado total mas debe y mas haber
                //AÑADIR TOTAL HABER (SIN APERTURA)
		res+=formatter.format(totalHaber)+";";//añado total mas debe y mas haber
		return res;
	}

}
