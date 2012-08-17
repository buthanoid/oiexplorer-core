/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import java.util.List;

/**
 * Store plot information, data to plot, options...
 * @author mella, bourgesl
 */
public class PlotDefinition {

    /* members */
    /** flag indicating that flagged data are not displayed */
    private boolean skipFlaggedData = true;
    /** Store xAxis name */
    private String xAxis = null;
    /** Store xAxis unit */
    private String xAxisUnit = null;
    /** Store xAxis scaling factor (optional) */
    private Double xAxisScalingFactor = null;
    /** flag to include zero value on x Axis */
    private boolean includeZeroOnXAxis = false;
    /** Store yAxis */
    private List<String> yAxes = null;
    /** Store yAxis unit */
    private List<String> yAxesUnit = null;
    /** Store yAxis scaling factor (optional) */
    private List<Double> yAxesScalingFactor = null;
    /** Store yAxis default value range (optional) */
    private List<double[]> yAxesRange = null;

    /* options: grouping, ... */
    public PlotDefinition() {
        super();
    }

    public boolean isSkipFlaggedData() {
        return skipFlaggedData;
    }

    public void setSkipFlaggedData(boolean skipFlaggedData) {
        this.skipFlaggedData = skipFlaggedData;
    }

    public String getxAxis() {
        return xAxis;
    }

    public void setxAxis(String xAxis) {
        this.xAxis = xAxis;
    }

    public List<String> getyAxes() {
        return yAxes;
    }

    public void setyAxes(List<String> yAxes) {
        this.yAxes = yAxes;
    }

    public boolean isIncludeZeroOnXAxis() {
        return includeZeroOnXAxis;
    }

    public void setIncludeZeroOnXAxis(boolean includeZeroOnXAxis) {
        this.includeZeroOnXAxis = includeZeroOnXAxis;
    }

    public String getxAxisUnit() {
        return xAxisUnit;
    }

    public void setxAxisUnit(String xAxisUnit) {
        this.xAxisUnit = xAxisUnit;
    }

    public Double getxAxisScalingFactor() {
        return xAxisScalingFactor;
    }

    public void setxAxisScalingFactor(Double xAxisScalingFactor) {
        this.xAxisScalingFactor = xAxisScalingFactor;
    }

    public List<String> getyAxesUnit() {
        return yAxesUnit;
    }

    public void setyAxesUnit(List<String> yAxesUnit) {
        this.yAxesUnit = yAxesUnit;
    }

    public List<Double> getyAxesScalingFactor() {
        return yAxesScalingFactor;
    }

    public void setyAxesScalingFactor(List<Double> yAxesScalingFactor) {
        this.yAxesScalingFactor = yAxesScalingFactor;
    }

    public List<double[]> getyAxesRange() {
        return yAxesRange;
    }

    public void setyAxesRange(List<double[]> yAxesRange) {
        this.yAxesRange = yAxesRange;
    }
}
