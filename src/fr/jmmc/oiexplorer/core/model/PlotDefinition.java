/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/

package fr.jmmc.oiexplorer.core.model;

/**
 * Store plot information, data to plot, options...
 * @author mella, bourgesl
 */

public class PlotDefinition {
    
    /* members */
    
    /** Store xAxis name */
    String xAxis=null; 
    
    /** Store yAxis */
    String yAxis=null;
    
    /* options: grouping, ... */
    
    public PlotDefinition(String xAxis, String yAxis){
        this.xAxis=xAxis;
        this.yAxis=yAxis;        
    }    
    
}
