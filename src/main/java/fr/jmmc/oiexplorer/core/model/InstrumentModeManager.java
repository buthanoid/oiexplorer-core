/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.oitools.model.InstrumentMode;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author bourgesl
 */
public final class InstrumentModeManager extends AbstractMapper<InstrumentMode> {
    /** smallest precision on wavelength */
    public final static float LAMBDA_PREC = 1e-10f;

    /** Singleton pattern */
    private final static InstrumentModeManager instance = new InstrumentModeManager();
    /** cache locals for Undefined InstrumentMode */
    private final static List<InstrumentMode> UNDEFINED_LOCALS = Arrays.asList(new InstrumentMode[]{InstrumentMode.UNDEFINED});

    /**
     * Return the Manager singleton
     * @return singleton instance
     */
    public static InstrumentModeManager getInstance() {
        return instance;
    }

    /**
     * Clear the mappings
     */
    public void clear() {
        super.clear();
        // insert mapping for Undefined:
        globalPerLocal.put(InstrumentMode.UNDEFINED, InstrumentMode.UNDEFINED);
        localsPerGlobal.put(InstrumentMode.UNDEFINED, UNDEFINED_LOCALS);
    }

    @Override
    protected boolean match(final InstrumentMode src, final InstrumentMode other) {
        if (NumberUtils.compare(src.getNbChannels(), other.getNbChannels()) != 0) {
            return false;
        }

        // precision = 1/2 channel width ie min(eff_band)/2
        float prec = 0.5f * Math.min(src.getBandMin(), other.getBandMin());
        
        if (Float.isNaN(prec) || prec < LAMBDA_PREC) {
            prec = LAMBDA_PREC;
        }

        // precision = 1e-10 ie 3 digits in nm:
        if (!NumberUtils.equals(src.getLambdaMin(), other.getLambdaMin(), prec)) {
            return false;
        }
        if (!NumberUtils.equals(src.getLambdaMax(), other.getLambdaMax(), prec)) {
            return false;
        }
        // precision = +/- 1:
        return (NumberUtils.equals(src.getLambdaMax(), other.getLambdaMax(), 1.0));
    }

    @Override
    protected InstrumentMode createGlobal(final InstrumentMode local) {
        return new InstrumentMode(local);
    }

    @Override
    protected String getName(final InstrumentMode src) {
        return src.getInsName();
    }
}
