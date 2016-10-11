/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.selection;

/**
 *
 * @author bourgesl
 */
public final class DataPoint {
    
    public static final DataPoint UNDEFINED = new DataPoint(Double.NaN, Double.NaN);

    /* members */
    private final double x;
    private final double y;

    public DataPoint(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "DataPoint{" + "x=" + x + ", y=" + y + '}';
    }

}
