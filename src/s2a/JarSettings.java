/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s2a;

/**
 *
 * @author PC01
 */
public class JarSettings {
    
    String inputCSV	= "";
    String outputCSV 	= "";
    int startLine       = -1;
    int subAccountCol 	= -1;
    int asientoCol      = -1;
    int debeCol 	= -1;
    int haberCol 	= -1;
    int fechaCol        = -1;
    int casadoCol       = -1;
    int secsPerSubaccount = -1;
    int maxCores = 1;
    int asientoApertura = -1;//-aa
    int asientoCierre = -1;//-ac
    int asientoRegularizacion = -1;//-ar		
    boolean delphiEncoding = false;
    boolean isCasadoRefEnabled = false; //anota referencia con la que se casa
    String refFecha = "01/01/2015"; //fecha de referencia
    int globalMaxSecs = 120;
    int blockSize = 50;

    public int getGlobalMaxSecs() {
        return globalMaxSecs;
    }

    public void setGlobalMaxSecs(int globalMaxSecs) {
        this.globalMaxSecs = globalMaxSecs;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }
           
    public String getInputCSV() {
        return inputCSV;
    }

    public void setInputCSV(String inputCSV) {
        this.inputCSV = inputCSV;
    }

    public String getOutputCSV() {
        return outputCSV;
    }

    public void setOutputCSV(String outputCSV) {
        this.outputCSV = outputCSV;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getSubAccountCol() {
        return subAccountCol;
    }

    public void setSubAccountCol(int subAccountCol) {
        this.subAccountCol = subAccountCol;
    }

    public int getAsientoCol() {
        return asientoCol;
    }

    public void setAsientoCol(int asientoCol) {
        this.asientoCol = asientoCol;
    }

    public int getDebeCol() {
        return debeCol;
    }

    public void setDebeCol(int debeCol) {
        this.debeCol = debeCol;
    }

    public int getHaberCol() {
        return haberCol;
    }

    public void setHaberCol(int haberCol) {
        this.haberCol = haberCol;
    }

    public int getFechaCol() {
        return fechaCol;
    }

    public void setFechaCol(int fechaCol) {
        this.fechaCol = fechaCol;
    }

    public int getCasadoCol() {
        return casadoCol;
    }

    public void setCasadoCol(int casadoCol) {
        this.casadoCol = casadoCol;
    }
    
    public int getSecsPerSubaccount() {
        return secsPerSubaccount;
    }

    public void setSecsPerSubaccount(int secsPerSubaccount) {
        this.secsPerSubaccount = secsPerSubaccount;
    }

    public int getMaxCores() {
        return maxCores;
    }

    public void setMaxCores(int maxCores) {
        this.maxCores = maxCores;
    }

    public int getAsientoApertura() {
        return asientoApertura;
    }

    public void setAsientoApertura(int asientoApertura) {
        this.asientoApertura = asientoApertura;
    }

    public int getAsientoCierre() {
        return asientoCierre;
    }

    public void setAsientoCierre(int asientoCierre) {
        this.asientoCierre = asientoCierre;
    }

    public int getAsientoRegularizacion() {
        return asientoRegularizacion;
    }

    public void setAsientoRegularizacion(int asientoRegularizacion) {
        this.asientoRegularizacion = asientoRegularizacion;
    }

    public boolean isDelphiEncoding() {
        return delphiEncoding;
    }

    public void setDelphiEncoding(boolean delphiEncoding) {
        this.delphiEncoding = delphiEncoding;
    }

    public boolean isIsCasadoRefEnabled() {
        return isCasadoRefEnabled;
    }

    public void setIsCasadoRefEnabled(boolean isCasadoRefEnabled) {
        this.isCasadoRefEnabled = isCasadoRefEnabled;
    }

    public String getRefFecha() {
        return refFecha;
    }

    public void setRefFecha(String refFecha) {
        this.refFecha = refFecha;
    }
    
    
    
}
