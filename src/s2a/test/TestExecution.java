package s2a.test;

import java.io.IOException;

public class TestExecution {

	public static void main(String[] args){
		// TODO Auto-generated method stub
		String executionLine = 
				//"cmd /c start "+'"'+"C:\\PARSERS\\casacion de saldos\\jar\\AlgoritmoCasacion_ecar2012.bat"+'"';
				"cmd /c "+'"'+"C:\\PARSERS\\casacion de saldos\\jar\\AlgoritmoCasacion_ecar2012.bat"+'"';
		System.out.println(executionLine);
		 try {
			Process process = Runtime.getRuntime().exec(executionLine);
			process.waitFor();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				 
	}

}
