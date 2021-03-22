/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s2a;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Clase que define el objeto casamiento con el número de línea y la referencia
 * con la que se linka a otras líneas
 * @author david
 */
public class Casamiento {
    String subAccount = "-1";
    long line = -1;
    short tipo = -1;
    long numRef  = -1;

    public String getSubAccount() {
        return subAccount;
    }

    public void setSubAccount(String subAccount) {
        this.subAccount = subAccount;
    }
       
    public long getLine() {
        return line;
    }

    public void setLine(long line) {
        this.line = line;
    }

    public short getTipo() {
        return tipo;
    }

    public void setTipo(short tipo) {
        this.tipo = tipo;
    }
    
    public long getNumRef() {
        return numRef;
    }

    public void setNumRef(long numRef) {
        this.numRef = numRef;
    }  
    
   
}
