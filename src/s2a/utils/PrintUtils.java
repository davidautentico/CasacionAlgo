package s2a.utils;


import java.text.DecimalFormat;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author PC01
 */
public class PrintUtils {
    
    public static String print2dec(double d){
        DecimalFormat df = new DecimalFormat("#.00");
        return df.format(d);
    }
    
}
