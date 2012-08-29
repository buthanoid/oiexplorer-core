/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.jmcs.jaxb.JAXBFactory;
import fr.jmmc.jmcs.jaxb.XmlBindException;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.oiexplorer.core.model.plot.Axis;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinitions;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bourgesl
 */
public final class PlotDefinitionFactory {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(PlotDefinitionFactory.class.getName());
    /** Factory instance */
    private final static PlotDefinitionFactory instance = new PlotDefinitionFactory();
    /** plot (Vis2 - T3) vs spatial freq */
    public final static String PLOT_VIS2DATA_T3PHI_SPATIAL_FREQ = "VIS2DATA_T3PHI/SPATIAL_FREQ";
    /** plot (Vis2 - T3) vs wavelenth */
    public final static String PLOT_VIS2DATA_T3PHI_EFF_WAVE = "VIS2DATA_T3PHI/EFF_WAVE";
    /** plot (Vis2 - T3) vs wavelenth */
    public final static String PLOT_VIS2DATA_T3PHI_MJD = "VIS2DATA_T3PHI/MJD";
    /** default plot */
    public final static String PLOT_DEFAULT = PLOT_VIS2DATA_T3PHI_SPATIAL_FREQ;
    /** JAXB Factory */
    private static JAXBFactory jf;
    /** Presets Filename */
    private final static String PRESETS_FILENAME = "fr/jmmc/oiexplorer/core/model/plotDefinitionPresets.xml";
    /** default plot definitions */
    private final Map<String, PlotDefinition> defaults = new LinkedHashMap<String, PlotDefinition>();

    /** Return factory singleton instance */
    public static PlotDefinitionFactory getInstance() {
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
     */
    private void initializeDefaults() {

        logger.warn("Loading presets from : {}", PRESETS_FILENAME);

        PlotDefinitions presets;
        try {
            presets = (PlotDefinitions) loadObject(FileUtils.getResource(PRESETS_FILENAME));;
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
    
    /**
     * Load on object from url.
     * @param inputUrl File to load
     * @return unmarshalled object
     *
     * @throws IOException if an I/O exception occured
     * @throws IllegalStateException if an unexpected exception occured
     * @throws XmlBindException if a JAXBException was caught while creating an unmarshaller
     */
    private Object loadObject(URL inputUrl) throws IOException, IllegalStateException, XmlBindException {
        Object result = null;

        logger.warn("Load object from url : {}", inputUrl);

        try {
            result = getJAXBFactory().createUnMarshaller().unmarshal(new BufferedInputStream(inputUrl.openStream()));
        } catch (JAXBException ex) {
            handleException("Loading object from " + inputUrl, ex);
        }

        return result;
    }

    /**
     * Handle JAXB Exception to extract IO Exception or unexpected exceptions
     * @param message message
     * @param je jaxb exception
     * 
     * @throws IllegalStateException if an unexpected exception occured
     * @throws IOException if an I/O exception occured
     */
    protected static void handleException(final String message, final JAXBException je) throws IllegalStateException, IOException {
        final Throwable cause = je.getCause();
        if (cause != null) {
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
        }
        if (je instanceof UnmarshalException) {
            throw new IllegalArgumentException("The loaded file does not correspond to a valid file", je);
        }
        throw new IllegalStateException(message, je);
    }

    /** Return the jaxbfactory to manage PlotDefinition.xsd material.
     @return the JAXBFactory for PlotDefinition related objects
     */
    private JAXBFactory getJAXBFactory() {
        if (jf == null) {
            jf = JAXBFactory.getInstance(PlotDefinition.class.getPackage().getName());
        }
        return jf;
    }
}
