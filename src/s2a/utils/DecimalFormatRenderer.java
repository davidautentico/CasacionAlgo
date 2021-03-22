package s2a.utils;

import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

	
public class DecimalFormatRenderer extends DefaultTableCellRenderer {
	      private static final DecimalFormat formatter = new DecimalFormat("###,##0.0000");
	 
	      public Component getTableCellRendererComponent(
	         JTable table, Object value, boolean isSelected,
	         boolean hasFocus, int row, int column) {
	    	  
	    	  Number valueNum = null;
	         // First format the cell value as required
	    	 try{
	    		if (value.toString().trim().equalsIgnoreCase("")) return null;
		    	long valueLong100 = MathUtils.doubleStrToLong(value.toString(), 2);
		    	valueNum = valueLong100*0.01;
	         	value = formatter.format(valueNum);
	    	 }catch(Exception e){
	    		 System.out.println("[error] value: "+value+" "+valueNum+" .Description: "+e.getMessage());
	    		 return null;
	    	 }
	            // And pass it on to parent class
	 
	         return super.getTableCellRendererComponent(
	            table, value, isSelected, hasFocus, row, column );
	      }
}
