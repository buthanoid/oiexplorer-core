/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.jmcs.jaxb.JAXBFactory;
import fr.jmmc.jmcs.jaxb.JAXBUtils;
import fr.jmmc.jmcs.jaxb.XmlBindException;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.oiexplorer.core.model.plot.Axis;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinitions;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bourgesl
 */
public final class PlotDefinitionFactory {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(PlotDefinitionFactory.class.getName());
    /** default plot */
    public final static String PLOT_DEFAULT = "VIS2DATA_T3PHI/SPATIAL_FREQ"; // TODO: externalize in presets.xml
    /** Presets Filename */
    private final static String PRESETS_FILENAME = "fr/jmmc/oiexplorer/core/resource/plotDefinitionPresets.xml";
    /** Factory instance */
    private static volatile PlotDefinitionFactory instance = null;

    /* members */
    /** default plot definitions */
    private final Map<String, PlotDefinition> defaults = new LinkedHashMap<String, PlotDefinition>();

    /** 
     * Return the factory singleton instance 
     */
    public static PlotDefinitionFactory getInstance() {
        if (instance == null) {
            instance = new PlotDefinitionFactory();
        }
        return instance;
    }

    /**
     * Private constructor for singleton pattern.
     */
    private PlotDefinitionFactory() {
        initializeDefaults();
    }

    /**
     * Initialize default presets extracted from preset file.
     * @throws IllegalStateException if the preset file is not found, an I/O exception occured, unmarshalling failed
     */
    private void initializeDefaults() throws IllegalStateException {

        PlotDefinitions presets;
        try {
            JAXBFactory jbf = JAXBFactory.getInstance(PlotDefinition.class.getPackage().getName());

            URL presetUrl = FileUtils.getResource(PRESETS_FILENAME);

            logger.info("Loading presets from : {}", presetUrl);

            presets = (PlotDefinitions) JAXBUtils.loadObject(presetUrl, jbf);

        } catch (IOException ioe) {
            throw new IllegalStateException("Can't load default preset file from " + PRESETS_FILENAME, ioe);
        } catch (IllegalStateException ise) {
            throw new IllegalStateException("Can't load default preset file from " + PRESETS_FILENAME, ise);
        } catch (XmlBindException xbe) {
            throw new IllegalStateException("Can't load default preset file from " + PRESETS_FILENAME, xbe);
        }

        /* Store defaults computing names (actually, as described in constants ) */
        for (PlotDefinition plotDefinition : presets.getPlotDefinitions()) {
            StringBuilder sb = new StringBuilder();
            for (Axis yAxis : plotDefinition.getYAxes()) {
                sb.append(yAxis.getName()).append("_");
            }
            sb.replace(sb.length() - 1, sb.length(), "/");
            sb.append(plotDefinition.getXAxis().getName());

            defaults.put(sb.toString(), plotDefinition);
        }
    }

    /** Get default presets.
     * @return List of default plotDefinitions
     */
    public List<String> getDefaultList() {
        return new ArrayList<String>(defaults.keySet());
    }

    /** Get the default preset given to its name. 
     @return PlotDefinition associated to given name.*/
    public PlotDefinition getDefault(final String key) {
        return defaults.get(key);
    }
}
