package s2a;

import java.util.LinkedHashMap;

public class TestLinkedHashMap {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LinkedHashMap m = new LinkedHashMap();
		
		for (int i=0;i<10;i++){
			m.put(i, i);
			int value = (int) m.get(i);
			m.put(i, value+1);
		}
		
		for (int i=0;i<m.size();i++){
			System.out.println(m.get(i));
		}
	}

}
