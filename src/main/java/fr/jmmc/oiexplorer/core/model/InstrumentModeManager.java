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
        
        // precision = 1e-10 ie 3 digits in nm:
        if (!NumberUtils.equals(src.getLambdaMin(), other.getLambdaMin(), 1e-10f)) {
            return false;
        }
        if (!NumberUtils.equals(src.getLambdaMax(), other.getLambdaMax(), 1e-10f)) {
            return false;
        }
        // precision = +/- 0.5:
        return (NumberUtils.equals(src.getLambdaMax(), other.getLambdaMax(), 5e-1f));
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
