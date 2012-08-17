/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.oitools.OIFitsConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author bourgesl
 */
public final class PlotDefinitionFactory {

    private final static PlotDefinitionFactory instance = new PlotDefinitionFactory();
    /** plot (Vis2 - T3) vs spatial freq */
    public final static String PLOT_VIS2DATA_T3PHI_SPATIAL_FREQ = "VIS2DATA_T3PHI/SPATIAL_FREQ";
    /** plot (Vis2 - T3) vs wavelenth */
    public final static String PLOT_VIS2DATA_T3PHI_EFF_WAVE = "VIS2DATA_T3PHI/EFF_WAVE";
    /** plot (Vis2 - T3) vs wavelenth */
    public final static String PLOT_VIS2DATA_T3PHI_MJD = "VIS2DATA_T3PHI/MJD";
    /** default plot */
    public final static String PLOT_DEFAULT = PLOT_VIS2DATA_T3PHI_SPATIAL_FREQ;

    public static PlotDefinitionFactory getInstance() {
        return instance;
    }
    /* members */
    /** default plot definitions */
    private final Map<String, PlotDefinition> defaults = new LinkedHashMap<String, PlotDefinition>();

    private PlotDefinitionFactory() {
        initializeDefaults();
    }

    private void initializeDefaults() {
        PlotDefinition plotDef;

        plotDef = new PlotDefinition();
        plotDef.setxAxis(OIFitsConstants.COLUMN_SPATIAL_FREQ);
        plotDef.setIncludeZeroOnXAxis(true);
        plotDef.setxAxisScalingFactor(1e-6d);
        plotDef.setxAxisUnit("M\u03BB");
        plotDef.setyAxes(Arrays.asList(new String[]{OIFitsConstants.COLUMN_VIS2DATA, OIFitsConstants.COLUMN_T3PHI}));
        defaults.put(PLOT_VIS2DATA_T3PHI_SPATIAL_FREQ, plotDef);

        plotDef = new PlotDefinition();
        plotDef.setxAxis(OIFitsConstants.COLUMN_EFF_WAVE);
        plotDef.setxAxisScalingFactor(1e6d);
        plotDef.setxAxisUnit("micrometer");
        plotDef.setyAxes(Arrays.asList(new String[]{OIFitsConstants.COLUMN_VIS2DATA, OIFitsConstants.COLUMN_T3PHI}));
        defaults.put(PLOT_VIS2DATA_T3PHI_EFF_WAVE, plotDef);

        plotDef = new PlotDefinition();
        plotDef.setxAxis(OIFitsConstants.COLUMN_MJD);
        plotDef.setyAxes(Arrays.asList(new String[]{OIFitsConstants.COLUMN_VIS2DATA, OIFitsConstants.COLUMN_T3PHI}));
        defaults.put(PLOT_VIS2DATA_T3PHI_MJD, plotDef);

        if (false) {
            plotDef = new PlotDefinition();
            plotDef.setxAxis(OIFitsConstants.COLUMN_MJD);
            plotDef.setyAxes(Arrays.asList(new String[]{OIFitsConstants.COLUMN_UCOORD, OIFitsConstants.COLUMN_VCOORD}));
            defaults.put(PLOT_DEFAULT, plotDef);
        }
    }

    public List<String> getDefaultList() {
        return new ArrayList<String>(defaults.keySet());
    }

    public PlotDefinition getDefault(final String key) {
        return defaults.get(key);
    }
}
