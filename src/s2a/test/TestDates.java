/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s2a.test;

import java.util.Calendar;
import s2a.utils.DateUtils;

/**
 *
 * @author PC01
 */
public class TestDates {
    
    public static void main(String[] args) {
                
        Calendar cal = Calendar.getInstance();
        String startDate    = DateUtils.datePrint(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), "/");
        
        System.out.println(cal.get(Calendar.DAY_OF_MONTH)
                +" "+cal.get(Calendar.MONTH)
                +" "+cal.get(Calendar.YEAR)
                +" || "+startDate);
    }
    
}
