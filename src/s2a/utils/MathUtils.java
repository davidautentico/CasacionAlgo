package s2a.utils;

import java.util.ArrayList;



public class MathUtils {
	
	public static int sigNum(long n){            
            return n < 0 ? -1: 1;
        }
        
	public static long doubleStrToLong(String doubleStr,int maxDec){	
		String doubleStr2 = doubleStr.replaceAll("\\.", "");
		if (doubleStr2.trim().equalsIgnoreCase("")) return 0;
		
		
		int commaPos = doubleStr2.indexOf(',');
		if (commaPos==-1) return Long.valueOf(doubleStr2)*100; //no hay coma
		//con comma
		if (commaPos==doubleStr2.length()-1){//no hay digitos a la derecha
			return Long.valueOf(doubleStr2.substring(0, commaPos))*100; //no hay coma
		}

		if (commaPos==doubleStr2.length()-2){//solo un decimal
			return Long.valueOf(doubleStr2.substring(0, commaPos)
					+doubleStr2.substring(commaPos+1, doubleStr2.length()))*10; //1 decimal
		}
		
		if (commaPos==doubleStr2.length()-3){//dos decimales
			return Long.valueOf(doubleStr2.substring(0, commaPos)
					+doubleStr2.substring(commaPos+1, doubleStr2.length())); //completo
		}
		
		return 0;
	}
	
	public static void generadorCombinaciones(String valuesStr,int maxLevel){
		ArrayList<String> levels = new ArrayList<String>();
		for (int i=0;i<maxLevel;i++) levels.add("");
		String[] values = valuesStr.split(" ");
		//creacion cubetas
		//nivel 1 :
		String level1="";
		for (int i=0;i<values.length;i++){
			level1 +=values[i]+" ";//1 2 3
		}
		//nivel 2:
		String level2=" ";
		String[] valuesL1 = level1.split(" ");
		for (int i=0;i<values.length;i++){
			for (int j=0;j<valuesL1.length;j++){
				String valueL1 = valuesL1[j];
				String total = values[i]+valueL1;
				level2+=total+" ";
			}
		}
		System.out.println("level1: "+level1);
		System.out.println("level2: "+level2);
		
	}
	
	/*public static void generadorCombinaciones(String valuesStr,int maxLevel){
		ArrayList<String> levels = new ArrayList<String>();
		for (int i=0;i<maxLevel;i++) levels.add("");
		String[] values = valuesStr.split(" ");
		
		for (int i=0;i<values.length;i++){	
			long value = Long.valueOf(values[i]);//obtenemos
			System.out.println(" a colocar en niveles: "+value);
			for (int j=0;j<maxLevel;j++){//cubeta de cada nivel
				String levelj = levels.get(j);//cubetaj,separada por ||
				long valuej = 0;
				if (!levelj.trim().equalsIgnoreCase("")){
					valuej = Long.valueOf(levelj.trim());
				}
				levelj+=(valuej+value)+" || ";
				levels.set(j, levelj);
			}
		}
		
		for (int i=0;i<levels.size();i++){
			String value = levels.get(i);
			System.out.println("Level "+i+" "+value);
		}
	}*/

	public static void generadorCombinaciones(ArrayList<Long> values,int maxLevel){
		ArrayList<String> levels = new ArrayList<String>();
		for (int i=0;i<=values.size();i++){
			long value = values.get(i);
			for (int j=1;j<=maxLevel;j++){//cubeta de cada nivel
				String levelj = levels.get(j);
				long valuej = 0;
				if (!levelj.trim().equalsIgnoreCase("")){
					valuej = Long.valueOf(levelj.trim());
				}
				levelj+=(valuej+value);
			}
		}
	}
	
	public static void generateNumbers4(String valuesStr){
		String[] values = valuesStr.split(" ");
		for (int i=0;i<values.length;i++){
			for (int j=i+1;j<values.length;j++){
				for (int s=j+1;s<values.length;s++){
					for (int t=s+1;t<values.length;t++){
						long n1 = Long.valueOf(values[i]);
						long n2 = Long.valueOf(values[j]);
						long n3 = Long.valueOf(values[s]);
						long n4 = Long.valueOf(values[t]);
						//System.out.println(n1+" "+n2+" "+n3+" "+n4);
					}
				}
			}
		}
	}
	
	public static void generateNumbers3(String valuesStr){
		String[] values = valuesStr.split(" ");
		for (int i=0;i<values.length;i++){
			for (int j=i+1;j<values.length;j++){
				for (int s=j+1;s<values.length;s++){
					long n1 = Long.valueOf(values[i]);
					long n2 = Long.valueOf(values[j]);
					long n3 = Long.valueOf(values[s]);
				}
			}
		}
	}
	
	public static void generateNumbers3(ArrayList<Integer> values){
		for (int i=0;i<values.size();i++){
			for (int j=i+1;j<values.size();j++){
				for (int s=j+1;s<values.size();s++){
					long n1 = Long.valueOf(values.get(i));
					long n2 = Long.valueOf(values.get(j));
					long n3 = Long.valueOf(values.get(s));
				}
			}
		}
	}
	
	public static void generateNumbers2(String valuesStr){
		String[] values = valuesStr.split(" ");
		for (int i=0;i<values.length;i++){
			for (int j=i+1;j<values.length;j++){
				long n1 = Long.valueOf(values[i]);
				long n2 = Long.valueOf(values[j]);
			}
		}
	}
	
	public static void generateNumbers2(ArrayList<Integer> values){
		for (int i=0;i<values.size();i++){
			for (int j=i+1;j<values.size();j++){
				long n1 = Long.valueOf(values.get(i));
				long n2 = Long.valueOf(values.get(j));
			}
		}
	}
	
	public static void initMaxArray(ArrayList<Integer>array,int size,int value){
		for (int i=0;i<size;i++){
			array.add(value);
		}
	}
	
	public static void main(String[] args) {
		String values = "1 2 3";
		//MathUtils.generadorCombinaciones(values, 1);
		values="";
		ArrayList<Integer> valuesArr = new ArrayList<Integer>();
		for (int i=1;i<=20000;i++){
			values+=i+" ";
			valuesArr.add(i);
		}
		
		
		String d1 = "3,45";
		String d2 = "3,";
		String d3 = "3,4";
		System.out.println(MathUtils.doubleStrToLong(d1, 2)
				+" "+MathUtils.doubleStrToLong(d2, 2)
				+" "+MathUtils.doubleStrToLong(d3, 2)
				);
		/*ArrayList<Integer> array = new ArrayList<Integer>();
		System.out.println("comenzand customArr");
		long t1 =  System.nanoTime();
		MathUtils.generateNumbers2(valuesArr);
		long t2 =  System.nanoTime();
		System.out.println("terminado customArr");*/
		/*System.out.println("comenzand customStr");
		t1 =  System.nanoTime();
		MathUtils.generateNumbers2(values);
		t2 =  System.nanoTime();
		System.out.println("terminado customStr");*/
		
		 // Create the initial vector
		   //ICombinatoricsVector<String> initialVector = Factory.createVector(
		      //new String[] { "1", "2", "3", "4", "5" } );
	
		/*	ICombinatoricsVector<String> initialVector = Factory.createVector(
				      values.trim().split(" ") );
			   Generator<String> gen4 = Factory.createSimpleCombinationGenerator(initialVector, 4);
			   System.out.println("comenzand iter4");
			   long t3 =  System.nanoTime();
			   for (ICombinatoricsVector<String> combination : gen4) {
			      //System.out.println(combination);
			      //System.out.println(combination.getVector());
			   }
			   long t4 =  System.nanoTime();
			   long time1 = t2-t1;
			   long time2 = t4-t3;
			   System.out.println(time1+" || "+time2+" || "+(time1-time2));
		*/
		System.out.println("PROGRAMA TERMINADO");
	}
}
