/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.function;

/**
 * This converter performs linear conversion (y = a.x + b)
 * @author bourgesl
 */
public final class LinearConverter implements Converter {

    /* members */
    /** scaling factor (a) */
    private final double scalingFactor;
    /** constant part (b) */
    private final double constant;

    /**
     * Public constructor
     * @param scalingFactor scaling factor (a)
     * @param constant constant part (b)
     */
    public LinearConverter(final double scalingFactor, final double constant) {
        this.scalingFactor = scalingFactor;
        this.constant = constant;
    }

    /**
     * Compute an output value given one input value using:
     * y = a.x + b
     * @param value input value (x)
     * @return output value (y)
     */
    public double evaluate(final double value) {
        return scalingFactor * value + constant;
    }
}
