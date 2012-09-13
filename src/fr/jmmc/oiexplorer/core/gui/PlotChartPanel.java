/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmal.image.ColorModels;
import fr.jmmc.jmal.image.ImageUtils;
import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.oiexplorer.core.gui.action.ExportPDFAction;
import fr.jmmc.oiexplorer.core.gui.chart.BoundedLogAxis;
import fr.jmmc.oiexplorer.core.gui.chart.BoundedNumberAxis;
import fr.jmmc.oiexplorer.core.gui.chart.ChartMouseSelectionListener;
import fr.jmmc.oiexplorer.core.gui.chart.ChartUtils;
import fr.jmmc.oiexplorer.core.gui.chart.CombinedCrosshairOverlay;
import fr.jmmc.oiexplorer.core.gui.chart.EnhancedChartMouseListener;
import fr.jmmc.oiexplorer.core.gui.chart.FastXYErrorRenderer;
import fr.jmmc.oiexplorer.core.gui.chart.PDFOptions;
import fr.jmmc.oiexplorer.core.gui.chart.PDFOptions.Orientation;
import fr.jmmc.oiexplorer.core.gui.chart.PDFOptions.PageSize;
import fr.jmmc.oiexplorer.core.gui.chart.SelectionOverlay;
import fr.jmmc.oiexplorer.core.gui.chart.dataset.FastIntervalXYDataset;
import fr.jmmc.oiexplorer.core.gui.chart.dataset.OITableSerieKey;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEvent;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventListener;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventType;
import fr.jmmc.oiexplorer.core.model.oi.Plot;
import fr.jmmc.oiexplorer.core.model.plot.Axis;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;
import fr.jmmc.oiexplorer.core.util.Constants;
import fr.jmmc.oitools.meta.ColumnMeta;
import fr.jmmc.oitools.meta.Units;
import fr.jmmc.oitools.model.OIData;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OITable;
import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.IndexColorModel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleInsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel provides the chart panel representing one OIFitsExplorer plot instance (using its subset and plot definition)
 * 
 * @author bourgesl
 */
public final class PlotChartPanel extends javax.swing.JPanel implements ChartProgressListener, EnhancedChartMouseListener, ChartMouseSelectionListener,
                                                                         PDFExportable, OIFitsCollectionManagerEventListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(PlotChartPanel.class.getName());
    /** default color model (aspro - Rainbow) */
    private final static IndexColorModel RAINBOW_COLOR_MODEL = ColorModels.getColorModel("Rainbow");
    /** data margin in percents (5%) */
    private final static double MARGIN_PERCENTS = 5d / 100d;
    /** double formatter for wave lengths */
    private final static NumberFormat df4 = new DecimalFormat("0.000#");

    /* members */
    /** OIFitsCollectionManager singleton */
    private final OIFitsCollectionManager ocm = OIFitsCollectionManager.getInstance();
    /** plot identifier */
    private String plotId = null;
    /** plot object reference (read only) */
    private Plot plot = null;
    /** flag to indicate if this plot has data */
    private boolean hasData = false;
    /* plot data */
    /** jFreeChart instance */
    private JFreeChart chart;
    /** combined xy plot sharing domain axis */
    private CombinedDomainXYPlot combinedXYPlot;
    /** mapping between xy plot and subplot index */
    private Map<XYPlot, Integer> plotMapping = new IdentityHashMap<XYPlot, Integer>();
    /** mapping between subplot index and xy plot (reverse) */
    private Map<Integer, XYPlot> plotIndexMapping = new HashMap<Integer, XYPlot>();
    /** chart panel */
    private ChartPanel chartPanel;
    /** crosshair overlay */
    private CombinedCrosshairOverlay crosshairOverlay = null;
    /** selection overlay */
    private SelectionOverlay selectionOverlay = null;
    /* TODO: List<XYPlot> */
    /** xy plot instance 1 */
    private XYPlot xyPlotPlot1;
    /** xy plot instance 2 */
    private XYPlot xyPlotPlot2;
    /* TODO: List<XYTextAnnotation> */
    /** JMMC annotation */
    private XYTextAnnotation aJMMCPlot1 = null;
    /** JMMC annotation */
    private XYTextAnnotation aJMMCPlot2 = null;

    /**
     * Constructor
     */
    public PlotChartPanel() {
        ocm.getPlotChangedEventNotifier().register(this);

        initComponents();
        postInit();
    }

    /**
     * Free any ressource or reference to this instance :
     * remove this instance from OIFitsCollectionManager event notifiers
     */
    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("dispose: {}", ObjectUtils.getObjectInfo(this));
        }

        ocm.unbind(this);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Export the chart component as a PDF document
     */
    @Override
    public void performPDFAction() {
        // if no OIFits data, discard action:
        if (getOiFitsSubset() != null) {
            ExportPDFAction.exportPDF(this);
        }
    }

    /**
     * Return the PDF default file name
     * [Vis2_<TARGET>_<INSTRUMENT>_<CONFIGURATION>_<DATE>]
     * @return PDF default file name
     */
    @Override
    public String getPDFDefaultFileName() {

        // TODO: fix this code as the plot will become generic !

        // TODO: keep values from dataset: insName, baselines, dateObs ... IF HAS DATA (filtered)

        if (getOiFitsSubset() != null && getPlotDefinition() != null) {

            final OIFitsFile oiFitsSubset = getOiFitsSubset();

            // TODO: use plot columns to generate file name ?
            
            final boolean hasVis2 = oiFitsSubset.hasOiVis2();
            final boolean hasT3 = oiFitsSubset.hasOiT3();

            final Set<String> distinct = new LinkedHashSet<String>();

            final StringBuilder sb = new StringBuilder(32);

            if (hasVis2) {
                sb.append("Vis2_");
            }
            if (oiFitsSubset.hasOiT3()) {
                sb.append("T3_");
            }

            // Add target name:
            final String altName = getTargetName().replaceAll(Constants.REGEXP_INVALID_TEXT_CHARS, "_");

            sb.append(altName).append('_');

            // Add distinct arrNames:
            final GetOIDataString arrNameOperator = new GetOIDataString() {
                public String getString(final OIData oiData) {
                    return oiData.getArrName();
                }
            };
            if (hasVis2) {
                getDistinct(oiFitsSubset.getOiVis2(), distinct, arrNameOperator);
            }
            if (hasT3) {
                getDistinct(oiFitsSubset.getOiT3(), distinct, arrNameOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, "_", "_", 3, "MULTI_ARRNAME");
            }

            sb.append('_');

            // Add unique insNames:
            final GetOIDataString insNameOperator = new GetOIDataString() {
                public String getString(final OIData oiData) {
                    return oiData.getInsName();
                }
            };
            distinct.clear();
            if (hasVis2) {
                getDistinct(oiFitsSubset.getOiVis2(), distinct, insNameOperator);
            }
            if (hasT3) {
                getDistinct(oiFitsSubset.getOiT3(), distinct, insNameOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, "_", "_", 3, "MULTI_INSNAME");
            }

            sb.append('_');

            // Add unique baselines:
            distinct.clear();
            if (hasVis2) {
//                getDistinctStaNames(oiFitsSubset.getOiVis2(), distinct);
                getDistinctStaConfs(oiFitsSubset.getOiVis2(), distinct);
            }
            if (hasT3) {
//                getDistinctStaNames(oiFitsSubset.getOiT3(), distinct);
                getDistinctStaConfs(oiFitsSubset.getOiT3(), distinct);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, "-", "_", 3, "MULTI_CONF");
            }

            sb.append('_');

            // Add unique dateObs:
            final GetOIDataString dateObsOperator = new GetOIDataString() {
                public String getString(final OIData oiData) {
                    return oiData.getDateObs();
                }
            };
            distinct.clear();
            if (hasVis2) {
                getDistinct(oiFitsSubset.getOiVis2(), distinct, dateObsOperator);
            }
            if (hasT3) {
                getDistinct(oiFitsSubset.getOiT3(), distinct, dateObsOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, "_", "_", 3, "MULTI_DATE");
            }

            sb.append('.').append(PDF_EXT);

            return sb.toString();
        }
        return null;
    }

    /**
     * Return the PDF options
     * @return PDF options
     */
    @Override
    public PDFOptions getPDFOptions() {
        return new PDFOptions(PageSize.A3, Orientation.Landscape);
    }

    /**
     * Return the chart to export as a PDF document
     * @return chart
     */
    @Override
    public JFreeChart prepareChart() {
        return this.chart;
    }

    /**
     * Callback indicating the chart was processed by the PDF engine
     */
    @Override
    public void postPDFExport() {
        // no-op
    }

    /**
     * This method is useful to set the models and specific features of initialized swing components :
     */
    private void postInit() {

        final boolean usePlotCrossHairSupport = false;
        final boolean useSelectionSupport = false; // TODO: enable selection ASAP (TODO sub plot support)

        final boolean showLegend = false;

        // create chart and add listener :
        this.combinedXYPlot = new CombinedDomainXYPlot(ChartUtils.createAxis(""));
        this.combinedXYPlot.setGap(10.0D);
        this.combinedXYPlot.setOrientation(PlotOrientation.VERTICAL);

        configureCrosshair(this.combinedXYPlot, usePlotCrossHairSupport);

        this.chart = ChartUtils.createChart(null, this.combinedXYPlot, showLegend);
        this.chart.addProgressListener(this);
        this.chartPanel = ChartUtils.createChartPanel(this.chart, false);

        // zoom options :
        this.chartPanel.setDomainZoomable(Constants.ENABLE_ZOOM);
        this.chartPanel.setRangeZoomable(Constants.ENABLE_ZOOM);

        // enable mouse wheel:
        this.chartPanel.setMouseWheelEnabled(true);

        if (useSelectionSupport) {
            this.selectionOverlay = new SelectionOverlay(this.chartPanel, this);
            this.chartPanel.addOverlay(this.selectionOverlay);
        }

        if (!usePlotCrossHairSupport) {
            this.crosshairOverlay = new CombinedCrosshairOverlay();
            this.chartPanel.addOverlay(crosshairOverlay);
        }

        if (useSelectionSupport || !usePlotCrossHairSupport) {
            this.chartPanel.addChartMouseListener(this);
        }

        this.add(this.chartPanel);

        // Create sub plots (TODO externalize):

        this.xyPlotPlot1 = createScientificScatterPlot(null, "", usePlotCrossHairSupport);

        this.aJMMCPlot1 = ChartUtils.createJMMCAnnotation(Constants.JMMC_ANNOTATION);
        this.xyPlotPlot1.getRenderer().addAnnotation(this.aJMMCPlot1, Layer.BACKGROUND);

        this.xyPlotPlot2 = createScientificScatterPlot(null, "", usePlotCrossHairSupport);

        this.aJMMCPlot2 = ChartUtils.createJMMCAnnotation(Constants.JMMC_ANNOTATION);
        this.xyPlotPlot2.getRenderer().addAnnotation(this.aJMMCPlot2, Layer.BACKGROUND);

        if (!usePlotCrossHairSupport) {
            Integer plotIndex = Integer.valueOf(1);
            crosshairOverlay.addDomainCrosshair(plotIndex, createCrosshair());
            crosshairOverlay.addRangeCrosshair(plotIndex, createCrosshair());

            plotIndex = Integer.valueOf(2);
            crosshairOverlay.addDomainCrosshair(plotIndex, createCrosshair());
            crosshairOverlay.addRangeCrosshair(plotIndex, createCrosshair());
        }

        resetPlot();
    }

    private static Crosshair createCrosshair() {
        final Crosshair crosshair = new Crosshair(Double.NaN);
        crosshair.setPaint(Color.BLUE);
        crosshair.setLabelVisible(true);
        crosshair.setLabelFont(ChartUtils.DEFAULT_TEXT_SMALL_FONT);
        crosshair.setLabelBackgroundPaint(new Color(255, 255, 0, 200));
        return crosshair;
    }

    /**
     * Create custom scatter plot with several display options (error renderer)
     * @param xAxisLabel x axis label
     * @param yAxisLabel y axis label
     * @param usePlotCrossHairSupport flag to use internal crosshair support on plot
     * @return xy plot
     */
    private static XYPlot createScientificScatterPlot(final String xAxisLabel, final String yAxisLabel, final boolean usePlotCrossHairSupport) {

        final XYPlot plot = ChartUtils.createScatterPlot(null, xAxisLabel, yAxisLabel, null, PlotOrientation.VERTICAL, false, false);

        plot.setNoDataMessage("No data for " + yAxisLabel);

        // enlarge right margin to have last displayed value:
        plot.setInsets(new RectangleInsets(2d, 10d, 2d, 20d));

        configureCrosshair(plot, usePlotCrossHairSupport);

        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        final FastXYErrorRenderer renderer = (FastXYErrorRenderer) plot.getRenderer();

        // only display Y error:
        renderer.setDrawXError(false);

        // force to use the base shape
        renderer.setAutoPopulateSeriesShape(false);

        // reset colors :
        renderer.setAutoPopulateSeriesPaint(false);
        renderer.clearSeriesPaints(false);

        // define deprecated methods to set renderer options for ALL series (performance):
        renderer.setShapesVisible(true);
        renderer.setShapesFilled(true);
        renderer.setDrawOutlines(false);

        renderer.setItemLabelsVisible(false);

        // define error bar settings:
        renderer.setErrorStroke(AbstractRenderer.DEFAULT_STROKE);
        renderer.setCapLength(0d);
        renderer.setErrorPaint(new Color(192, 192, 192, 128));

        return plot;
    }

    private static void configureCrosshair(final XYPlot plot, final boolean usePlotCrossHairSupport) {
        // configure xyplot or overlay crosshairs:
        plot.setDomainCrosshairLockedOnData(usePlotCrossHairSupport);
        plot.setDomainCrosshairVisible(usePlotCrossHairSupport);

        plot.setRangeCrosshairLockedOnData(usePlotCrossHairSupport);
        plot.setRangeCrosshairVisible(usePlotCrossHairSupport);
    }

    /* EnhancedChartMouseListener implementation */
    /**
     * Return true if this listener implements / uses this mouse event type
     * @param eventType mouse event type
     * @return true if this listener implements / uses this mouse event type
     */
    @Override
    public boolean support(final int eventType) {
        return (eventType == EnhancedChartMouseListener.EVENT_CLICKED);
    }

    /**
     * Handle click on plot
     * @param chartMouseEvent chart mouse event
     */
    @Override
    public void chartMouseClicked(final ChartMouseEvent chartMouseEvent) {
        final int i = chartMouseEvent.getTrigger().getX();
        final int j = chartMouseEvent.getTrigger().getY();

        if (this.chartPanel.getScreenDataArea().contains(i, j)) {
            final Point2D point2D = this.chartPanel.translateScreenToJava2D(new Point(i, j));

            final PlotRenderingInfo plotInfo = this.chartPanel.getChartRenderingInfo().getPlotInfo();

            final int subplotIndex = plotInfo.getSubplotIndex(point2D);
            if (subplotIndex == -1) {
                return;
            }

            // data area for sub plot:
            final Rectangle2D dataArea = plotInfo.getSubplotInfo(subplotIndex).getDataArea();

            final Integer plotIndex = Integer.valueOf(subplotIndex + 1);

            final XYPlot xyPlot = this.plotIndexMapping.get(plotIndex);
            if (xyPlot == null) {
                return;
            }

            final ValueAxis domainAxis = xyPlot.getDomainAxis();
            final double domainValue = domainAxis.java2DToValue(point2D.getX(), dataArea, xyPlot.getDomainAxisEdge());

            final ValueAxis rangeAxis = xyPlot.getRangeAxis();
            final double rangeValue = rangeAxis.java2DToValue(point2D.getY(), dataArea, xyPlot.getRangeAxisEdge());

            if (logger.isDebugEnabled()) {
                logger.debug("Mouse coordinates are (" + i + ", " + j + "), in data space = (" + domainValue + ", " + rangeValue + ")");
            }

            // aspect ratio:
            final double xRatio = dataArea.getWidth() / Math.abs(domainAxis.getUpperBound() - domainAxis.getLowerBound());
            final double yRatio = dataArea.getHeight() / Math.abs(rangeAxis.getUpperBound() - rangeAxis.getLowerBound());

            // find matching data ie. closest data point according to its screen distance to the mouse clicked point:
            Point2D dataPoint = findDataPoint(xyPlot, domainValue, rangeValue, xRatio, yRatio);

            List<Crosshair> xCrosshairs = this.crosshairOverlay.getDomainCrosshairs(plotIndex);
            if (xCrosshairs.size() == 1) {
                xCrosshairs.get(0).setValue(dataPoint.getX());
            }
            List<Crosshair> yCrosshairs = this.crosshairOverlay.getRangeCrosshairs(plotIndex);
            if (yCrosshairs.size() == 1) {
                yCrosshairs.get(0).setValue(dataPoint.getY());
            }

            // update other plot crosshairs:
            for (Integer index : this.plotIndexMapping.keySet()) {
                if (index != plotIndex) {
                    final XYPlot otherPlot = this.plotIndexMapping.get(index);
                    if (otherPlot != null) {
                        xCrosshairs = this.crosshairOverlay.getDomainCrosshairs(index);
                        if (xCrosshairs.size() == 1) {
                            xCrosshairs.get(0).setValue(dataPoint.getX());
                        }
                        yCrosshairs = this.crosshairOverlay.getRangeCrosshairs(index);
                        if (yCrosshairs.size() == 1) {
                            yCrosshairs.get(0).setValue(Double.NaN);
                        }
                    }
                }
            }
        }
    }

    /**
     * Not implemented
     * @param chartMouseEvent useless
     */
    @Override
    public void chartMouseMoved(final ChartMouseEvent chartMouseEvent) {
        if (false) {
            chartMouseClicked(chartMouseEvent);
        }
    }

    /**
     * Handle rectangular selection event
     *
     * @param selection the selected region.
     */
    @Override
    public void mouseSelected(final Rectangle2D selection) {
        logger.debug("mouseSelected: rectangle {}", selection);

        // TODO: determine which plot to use ?
        
        // find data points:
        final List<Point2D> points = findDataPoints(selection);

        // push data points to overlay for rendering:
        this.selectionOverlay.setPoints(points);
    }

    /**
     * Find data point closest in FIRST dataset to the given coordinates X / Y
     * @param xyPlot xy plot to get its dataset
     * @param anchorX domain axis coordinate
     * @param anchorY range axis coordinate
     * @param xRatio pixels per data on domain axis
     * @param yRatio pixels per data on range axis
     * @return found Point2D (data coordinates) or Point2D(NaN, NaN)
     */
    private static Point2D findDataPoint(final XYPlot xyPlot, final double anchorX, final double anchorY, final double xRatio, final double yRatio) {
        final XYDataset dataset = xyPlot.getDataset();

        if (dataset != null) {

            // TODO: move such code elsewhere : ChartUtils or XYDataSetUtils ?

            final long startTime = System.nanoTime();

            double minDistance = Double.POSITIVE_INFINITY;
            int matchSerie = -1;
            int matchItem = -1;

            double x, y, dx, dy, distance;

            // NOTE: not optimized

            // standard case - plain XYDataset
            for (int serie = 0, seriesCount = dataset.getSeriesCount(), item, itemCount; serie < seriesCount; serie++) {
                itemCount = dataset.getItemCount(serie);
                for (item = 0; item < itemCount; item++) {
                    x = dataset.getXValue(serie, item);
                    y = dataset.getYValue(serie, item);

                    if (!Double.isNaN(x) && !Double.isNaN(y)) {
                        // converted in pixels:
                        dx = (x - anchorX) * xRatio;
                        dy = (y - anchorY) * yRatio;

                        distance = dx * dx + dy * dy;

                        if (distance < minDistance) {
                            minDistance = distance;
                            matchSerie = serie;
                            matchItem = item;
                        }
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("findDataPoint: duration = {} ms.", 1e-6d * (System.nanoTime() - startTime));
            }

            if (matchItem != -1) {
                final double matchX = dataset.getXValue(matchSerie, matchItem);
                final double matchY = dataset.getYValue(matchSerie, matchItem);

                if (logger.isDebugEnabled()) {
                    logger.debug("Matching item [serie = " + matchSerie + ", item = " + matchItem + "] : (" + matchX + ", " + matchY + ")");
                }

                return new Point2D.Double(matchX, matchY);
            }
        }

        logger.debug("No Matching item.");

        return new Point2D.Double(Double.NaN, Double.NaN);
    }

    /**
     * Find data points inside the given Shape (data coordinates)
     * @param shape shape to use
     * @return found list of Point2D (data coordinates) or empty list
     */
    private List<Point2D> findDataPoints(final Shape shape) {
        // TODO: generalize which plot use
        final XYDataset dataset = this.xyPlotPlot1.getDataset();

        final List<Point2D> points = new ArrayList<Point2D>();

        if (dataset != null) {
            // TODO: move such code elsewhere : ChartUtils or XYDataSetUtils ?

            final long startTime = System.nanoTime();
            /*
             int matchSerie = -1;
             int matchItem = -1;
             */
            double x, y;

            // NOTE: not optimized

            // standard case - plain XYDataset
            for (int serie = 0, seriesCount = dataset.getSeriesCount(), item, itemCount; serie < seriesCount; serie++) {
                itemCount = dataset.getItemCount(serie);
                for (item = 0; item < itemCount; item++) {
                    x = dataset.getXValue(serie, item);
                    y = dataset.getYValue(serie, item);

                    if (!Double.isNaN(x) && !Double.isNaN(y)) {

                        if (shape.contains(x, y)) {
                            // TODO: keep data selection (pointer to real data)
                /*
                             matchSerie = serie;
                             matchItem = item;
                             */
                            points.add(new Point2D.Double(x, y));
                        }
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("findDataPoints: duration = {} ms.", 1e-6d * (System.nanoTime() - startTime));
            }
        }
        return points;
    }

    /**
     * Plot the generated file synchronously (useless).
     * This code must be executed by the Swing Event Dispatcher thread (EDT)
     */
    public void plot() {
        logger.debug("plot");
        this.updatePlot();
    }

    /**
     * Reset plot
     */
    private void resetPlot() {
        this.hasData = false;

        // disable chart & plot notifications:
        this.chart.setNotify(false);
        this.xyPlotPlot1.setNotify(false);
        this.xyPlotPlot2.setNotify(false);
        try {
            // reset title:
            ChartUtils.clearTextSubTitle(this.chart);

            removeAllSubPlots();

            // reset dataset:
            this.xyPlotPlot1.setDataset(null);
            this.xyPlotPlot2.setDataset(null);

            // useless:
//            applyColorTheme();

            this.resetOverlays();

            this.chartPanel.setVisible(this.hasData);

        } finally {
            // restore chart & plot notifications:
            this.xyPlotPlot2.setNotify(true);
            this.xyPlotPlot1.setNotify(true);
            this.chart.setNotify(true);
        }
    }

    private void removeAllSubPlots() {
        // remove all sub plots: 
        // Note: use toArray() to avoid concurrentModification exceptions:
        for (Object plot : this.combinedXYPlot.getSubplots().toArray()) {
            final XYPlot xyPlot = (XYPlot) plot;
            this.combinedXYPlot.remove((XYPlot) plot);

            final Integer index = this.plotMapping.remove(xyPlot);
            this.plotIndexMapping.remove(index);
        }
    }

    /**
     * Refresh the plot using chart data.
     * This code is executed by the Swing Event Dispatcher thread (EDT)
     */
    private void updatePlot() {
        // check subset:
        if (getOiFitsSubset() == null || getPlotDefinition() == null) {
            resetPlot();
            return;
        }

        final long start = System.nanoTime();

        this.hasData = false;

        // disable chart & plot notifications:
        this.chart.setNotify(false);
        this.xyPlotPlot1.setNotify(false);
        this.xyPlotPlot2.setNotify(false);

        try {
            // title :
            ChartUtils.clearTextSubTitle(this.chart);

            final OIFitsFile oiFitsSubset = getOiFitsSubset();

            final boolean hasVis2 = oiFitsSubset.hasOiVis2();
            final boolean hasT3 = oiFitsSubset.hasOiT3();

            final Set<String> distinct = new LinkedHashSet<String>();

            // TODO: fix this code as the plot will become generic !

            // TODO: keep values from dataset: insName, baselines, dateObs ... IF HAS DATA (filtered)

            final StringBuilder sb = new StringBuilder(32);

            // Add distinct arrNames:
            final GetOIDataString arrNameOperator = new GetOIDataString() {
                public String getString(final OIData oiData) {
                    return oiData.getArrName();
                }
            };
            if (hasVis2) {
                getDistinct(oiFitsSubset.getOiVis2(), distinct, arrNameOperator);
            }
            if (hasT3) {
                getDistinct(oiFitsSubset.getOiT3(), distinct, arrNameOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, " ", " / ");
            }

            sb.append(" - ");

            // Add unique insNames:
            final GetOIDataString insNameOperator = new GetOIDataString() {
                public String getString(final OIData oiData) {
                    return oiData.getInsName();
                }
            };
            distinct.clear();
            if (hasVis2) {
                getDistinct(oiFitsSubset.getOiVis2(), distinct, insNameOperator);
            }
            if (hasT3) {
                getDistinct(oiFitsSubset.getOiT3(), distinct, insNameOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, " ", " / ");
            }

            sb.append(" ");

            // Add wavelength ranges:
            distinct.clear();
            if (hasVis2) {
                getDistinctWaveLengthRange(oiFitsSubset.getOiVis2(), distinct);
            }
            if (hasT3) {
                getDistinctWaveLengthRange(oiFitsSubset.getOiT3(), distinct);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, " ", " / ");
            }

            sb.append(" - ");

            // Add unique baselines:
            distinct.clear();
            if (hasVis2) {
//                getDistinctStaNames(oiFitsSubset.getOiVis2(), distinct);
                getDistinctStaConfs(oiFitsSubset.getOiVis2(), distinct);
            }
            if (hasT3) {
//                getDistinctStaNames(oiFitsSubset.getOiT3(), distinct);
                getDistinctStaConfs(oiFitsSubset.getOiT3(), distinct);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, " ", " / ");
            }

            ChartUtils.addSubtitle(this.chart, sb.toString());

            // date - Source:
            sb.setLength(0);
            sb.append("Day: ");

            // Add unique dateObs:
            final GetOIDataString dateObsOperator = new GetOIDataString() {
                public String getString(final OIData oiData) {
                    return oiData.getDateObs();
                }
            };
            distinct.clear();
            if (hasVis2) {
                getDistinct(oiFitsSubset.getOiVis2(), distinct, dateObsOperator);
            }
            if (hasT3) {
                getDistinct(oiFitsSubset.getOiT3(), distinct, dateObsOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, " ", " / ");
            }

            sb.append(" - Source: ").append(getTargetName());

            ChartUtils.addSubtitle(this.chart, sb.toString());

            // computed data are valid :
            // TODO: externalize dataset creation using SwingWorker to be able to 
            // - cancel long data processing task
            // - do not block EDT !
            this.hasData = updateChart();

            if (this.hasData) {
                applyColorTheme();
            }

            this.resetOverlays();

            this.chartPanel.setVisible(this.hasData);

        } finally {
            // restore chart & plot notifications:
            this.xyPlotPlot2.setNotify(true);
            this.xyPlotPlot1.setNotify(true);
            this.chart.setNotify(true);
        }

        if (logger.isInfoEnabled()) {
            logger.info("plot : duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }
    }

    /**
     * reset overlays
     */
    private void resetOverlays() {
        // reset crossHairs:
        if (this.crosshairOverlay != null) {
            for (Integer plotIndex : this.plotMapping.values()) {
                for (Crosshair ch : this.crosshairOverlay.getDomainCrosshairs(plotIndex)) {
                    ch.setValue(Double.NaN);
                }
                for (Crosshair ch : this.crosshairOverlay.getRangeCrosshairs(plotIndex)) {
                    ch.setValue(Double.NaN);
                }
            }
        }

        // reset selection:
        if (this.selectionOverlay != null) {
            this.selectionOverlay.reset();
        }
    }

    /**
     * Update the datasets
     * @return true if vis2 has data to plot
     */
    private boolean updateChart() {

        final long start = System.nanoTime();

        final OIFitsFile oiFitsSubset = getOiFitsSubset();
        final PlotDefinition plotDef = getPlotDefinition();

        removeAllSubPlots();

        boolean showV2 = false;
        boolean showT3 = false;

        ColumnMeta xMeta = null;

        BoundedNumberAxis axis;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        boolean xUseLog = false;

        if (!plotDef.getYAxes().isEmpty()) {

            if (oiFitsSubset.getNbOiTables() > 0) {
                final FastIntervalXYDataset<OITableSerieKey, OITableSerieKey> dataset = new FastIntervalXYDataset<OITableSerieKey, OITableSerieKey>();
                // reset dataset so free memory:
                this.xyPlotPlot2.setDataset(null);

                double minY = Double.POSITIVE_INFINITY;
                double maxY = Double.NEGATIVE_INFINITY;
                boolean yUseLog = false;

                PlotInfo info;
                Range axisRange;
                ColumnMeta yMeta = null;

                int tableIndex = 0;
                for (OITable oiTable : oiFitsSubset.getOiTables()) {

                    info = updatePlot(this.xyPlotPlot1, (OIData) oiTable, tableIndex, plotDef, 0, dataset);

                    if (info != null) {
                        showV2 = true;
                        xUseLog = info.xUseLog;
                        yUseLog = info.yUseLog;

                        // combine X range:
                        axisRange = info.xRange;
                        minX = Math.min(minX, axisRange.getLowerBound());
                        maxX = Math.max(maxX, axisRange.getUpperBound());

                        // combine Y range:
                        axisRange = info.yRange;
                        minY = Math.min(minY, axisRange.getLowerBound());
                        maxY = Math.max(maxY, axisRange.getUpperBound());

                        // update X axis Label:
                        if (xMeta == null && info.xMeta != null) {
                            xMeta = info.xMeta;
                        }
                        // update Y axis Label:
                        if (yMeta == null && info.yMeta != null) {
                            yMeta = info.yMeta;
                        }
                    }
                    tableIndex++;
                }

                if (showV2) {
                    logger.info("xyPlotV2: nbSeries = {}", dataset.getSeriesCount());

                    // TODO: fix boundaries according to standard data boundaries (VIS between 0-1 ...)
                    logger.debug("rangeAxis: {} - {}", minY, maxY);

                    // Add margin:
                    if (yUseLog) {
                        double minTen = Math.floor(Math.log10(minY));
                        double maxTen = Math.ceil(Math.log10(maxY));

                        if (maxTen == minTen) {
                            maxTen += MARGIN_PERCENTS;
                        }

                        minY = Math.pow(10d, minTen); // lower power of ten
                        maxY = Math.pow(10d, maxTen); // upper power of ten
                    } else {
                        final double marginY = (maxY - minY) * MARGIN_PERCENTS;
                        if (marginY > 0d) {
                            minY -= marginY;
                            maxY += marginY;
                        } else {
                            minY -= minY * MARGIN_PERCENTS;
                            maxY += maxY * MARGIN_PERCENTS;
                        }
                        if (maxY == minY) {
                            maxY = minY + 1d;
                        }
                    }
                    logger.debug("fixed rangeAxis: {} - {}", minY, maxY);

                    // Update Y axis:
                    if (!yUseLog) {
                        this.xyPlotPlot1.setRangeAxis(ChartUtils.createAxis(""));
                    }
                    if (this.xyPlotPlot1.getRangeAxis() instanceof BoundedNumberAxis) {
                        axis = (BoundedNumberAxis) this.xyPlotPlot1.getRangeAxis();
                        axis.setBounds(new Range(minY, maxY));
                        axis.setRange(minY, maxY);
                    }

                    // update Y axis Label:
                    String label = "";
                    if (yMeta != null) {
                        label = yMeta.getName();
                        if (yMeta != null && yMeta.getUnits() != Units.NO_UNIT) {
                            label += " (" + yMeta.getUnits().getStandardRepresentation() + ")";
                        }
                        this.xyPlotPlot1.getRangeAxis().setLabel(label);
                    }

                    if (yUseLog) {
                        // test logarithmic axis:
                        final BoundedLogAxis logAxis = new BoundedLogAxis("log " + label);
                        logAxis.setExpTickLabelsFlag(true);
                        logAxis.setAutoRangeNextLogFlag(true);

                        logAxis.setBounds(new Range(minY, maxY));
                        logAxis.setRange(minY, maxY);

                        this.xyPlotPlot1.setRangeAxis(logAxis);
                    }

                    // update plot's dataset (notify events):
                    this.xyPlotPlot1.setDataset(dataset);
                }
            }
        }

        if (showV2) {
            this.combinedXYPlot.add(this.xyPlotPlot1, 1);

            final Integer plotIndex = Integer.valueOf(1);
            this.plotMapping.put(this.xyPlotPlot1, plotIndex);
            this.plotIndexMapping.put(plotIndex, this.xyPlotPlot1);

        } else {
            // reset Vis2 dataset:
            this.xyPlotPlot1.setDataset(null);
        }

        if (plotDef.getYAxes().size() > 1) {
            if (oiFitsSubset.getNbOiTables() > 0) {
                final FastIntervalXYDataset<OITableSerieKey, OITableSerieKey> dataset = new FastIntervalXYDataset<OITableSerieKey, OITableSerieKey>();
                // reset dataset so free memory:
                this.xyPlotPlot2.setDataset(null);

                double minY = Double.POSITIVE_INFINITY;
                double maxY = Double.NEGATIVE_INFINITY;
                boolean yUseLog = false;

                PlotInfo info;
                Range axisRange;
                ColumnMeta yMeta = null;

                int tableIndex = 0;
                for (OITable oiTable : oiFitsSubset.getOiTables()) {

                    info = updatePlot(this.xyPlotPlot2, (OIData) oiTable, tableIndex, plotDef, 1, dataset);

                    if (info != null) {
                        showT3 = true;
                        xUseLog = info.xUseLog;
                        yUseLog = info.yUseLog;

                        // combine X range:
                        axisRange = info.xRange;
                        minX = Math.min(minX, axisRange.getLowerBound());
                        maxX = Math.max(maxX, axisRange.getUpperBound());

                        // combine Y range:
                        axisRange = info.yRange;
                        minY = Math.min(minY, axisRange.getLowerBound());
                        maxY = Math.max(maxY, axisRange.getUpperBound());

                        // update X axis Label:
                        if (xMeta == null && info.xMeta != null) {
                            xMeta = info.xMeta;
                        }
                        // update Y axis Label:
                        if (yMeta == null && info.yMeta != null) {
                            yMeta = info.yMeta;
                        }
                    }
                    tableIndex++;
                }

                if (showT3) {
                    logger.info("xyPlotT3: nbSeries = {}", dataset.getSeriesCount());

                    // TODO: fix boundaries according to standard data boundaries (T3 between -180-180 ...)
                    logger.debug("rangeAxis: {} - {}", minY, maxY);

                    // Add margin:
                    if (yUseLog) {
                        double minTen = Math.floor(Math.log10(minY));
                        double maxTen = Math.ceil(Math.log10(maxY));

                        if (maxTen == minTen) {
                            maxTen += MARGIN_PERCENTS;
                        }

                        minY = Math.pow(10d, minTen); // lower power of ten
                        maxY = Math.pow(10d, maxTen); // upper power of ten
                    } else {
                        final double marginY = (maxY - minY) * MARGIN_PERCENTS;
                        if (marginY > 0d) {
                            minY -= marginY;
                            maxY += marginY;
                        } else {
                            minY -= minY * MARGIN_PERCENTS;
                            maxY += maxY * MARGIN_PERCENTS;
                        }
                        if (maxY == minY) {
                            maxY = minY + 1d;
                        }
                    }
                    logger.debug("fixed rangeAxis: {} - {}", minY, maxY);

                    // Update Y axis:
                    if (!yUseLog) {
                        this.xyPlotPlot2.setRangeAxis(ChartUtils.createAxis(""));
                    }
                    if (this.xyPlotPlot2.getRangeAxis() instanceof BoundedNumberAxis) {
                        axis = (BoundedNumberAxis) this.xyPlotPlot2.getRangeAxis();
                        axis.setBounds(new Range(minY, maxY));
                        axis.setRange(minY, maxY);
                    }

                    // update Y axis Label:
                    String label = "";
                    if (yMeta != null) {
                        label = yMeta.getName();
                        if (yMeta != null && yMeta.getUnits() != Units.NO_UNIT) {
                            label += " (" + yMeta.getUnits().getStandardRepresentation() + ")";
                        }
                        this.xyPlotPlot2.getRangeAxis().setLabel(label);
                    }

                    if (yUseLog) {
                        // test logarithmic axis:
                        final BoundedLogAxis logAxis = new BoundedLogAxis("log " + label);
                        logAxis.setExpTickLabelsFlag(true);
                        logAxis.setAutoRangeNextLogFlag(true);

                        logAxis.setBounds(new Range(minY, maxY));
                        logAxis.setRange(minY, maxY);

                        this.xyPlotPlot2.setRangeAxis(logAxis);
                    }

                    // update plot's dataset (notify events):
                    this.xyPlotPlot2.setDataset(dataset);
                }
            }
        }

        if (showT3) {
            this.combinedXYPlot.add(this.xyPlotPlot2, 1);

            final Integer plotIndex = (showV2) ? Integer.valueOf(2) : Integer.valueOf(1);
            this.plotMapping.put(this.xyPlotPlot2, plotIndex);
            this.plotIndexMapping.put(plotIndex, this.xyPlotPlot2);

        } else {
            // reset T3 dataset:
            this.xyPlotPlot2.setDataset(null);
        }

        if (!showV2 && !showT3) {
            if (logger.isInfoEnabled()) {
                logger.info("updateChart : duration = {} ms.", 1e-6d * (System.nanoTime() - start));
            }
            return false;
        }

        // TODO: fix boundaries according to standard data boundaries (spatial freq >= 0)
        logger.debug("domainAxis: {} - {}", minX, maxX);
        
        // TODO: keep data info to help user define its own range

        // Add margin:
        if (xUseLog) {
            double minTen = Math.floor(Math.log10(minX));
            double maxTen = Math.ceil(Math.log10(maxX));

            if (maxTen == minTen) {
                maxTen += MARGIN_PERCENTS;
            }

            minX = Math.pow(10d, minTen); // lower power of ten
            maxX = Math.pow(10d, maxTen); // upper power of ten
        } else {
            final double marginX = (maxX - minX) * MARGIN_PERCENTS;
            if (marginX > 0d) {

                if (plotDef.getXAxis().isIncludeZero()) {
                    if (minX > 0d) {
                        minX = 0d;
                    }
                    maxX += marginX;
                } else {
                    minX -= marginX;
                    maxX += marginX;
                }

            } else {
                minX -= minX * MARGIN_PERCENTS;
                maxX += maxX * MARGIN_PERCENTS;
            }
            if (maxX == minX) {
                maxX = minX + 1d;
            }
        }
        logger.debug("fixed domainAxis: {} - {}", minX, maxX);

        if (!xUseLog) {
            this.combinedXYPlot.setDomainAxis(ChartUtils.createAxis(""));
        }
        if (this.combinedXYPlot.getDomainAxis() instanceof BoundedNumberAxis) {
            axis = (BoundedNumberAxis) this.combinedXYPlot.getDomainAxis();
            axis.setBounds(new Range(minX, maxX));
            axis.setRange(minX, maxX);
        }

        // update X axis Label:
        String label = "";
        if (xMeta != null) {
            label = xMeta.getName();
            if (xMeta != null && xMeta.getUnits() != Units.NO_UNIT) {
                label += " (" + xMeta.getUnits().getStandardRepresentation() + ")";
            }
            this.combinedXYPlot.getDomainAxis().setLabel(label);
        }

        if (xUseLog) {
            // test logarithmic axis:
            final BoundedLogAxis logAxis = new BoundedLogAxis("log " + label);
            logAxis.setExpTickLabelsFlag(true);
            logAxis.setAutoRangeNextLogFlag(true);

            logger.debug("logAxis domain: [{} - {}]", minX, maxX);

            logAxis.setBounds(new Range(minX, maxX));
            logAxis.setRange(minX, maxX);

            this.combinedXYPlot.setDomainAxis(logAxis);
        }


        if (logger.isInfoEnabled()) {
            logger.info("updateChart : duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }
        return true;
    }

    /**
     * Update the plot (dataset, axis ranges ...) using the given OIData table
     * TODO use column names and virtual columns (spatial ...)
     * @param plot XYPlot to update (dataset, renderer, axes)
     * @param oiData OIData table to use as data source
     * @param tableIndex table index to ensure serie uniqueness among collection
     * @param plotDef plot definition to use
     * @param yAxisIndex yAxis index to use in plot definition
     * @param dataset FastIntervalXYDataset to fill
     * @return rectangle giving data area or null if no data
     */
    private PlotInfo updatePlot(final XYPlot plot, final OIData oiData, final int tableIndex,
                                final PlotDefinition plotDef, final int yAxisIndex,
                                final FastIntervalXYDataset<OITableSerieKey, OITableSerieKey> dataset) {

        // Get yAxis data:
        final boolean isYData2D;
        final double[] yData1D;
        final double[] yData1DErr;
        final double[][] yData2D;
        final double[][] yData2DErr;

        final Axis yAxis = plotDef.getYAxes().get(yAxisIndex);
        final String yAxisName = yAxis.getName();
        final boolean yUseLog = yAxis.isLogScale();

        final ColumnMeta yMeta = oiData.getColumnMeta(yAxisName);

        if (yMeta == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("unsupported yAxis : {} on {}", yAxis.getName(), oiData);
            }
            return null;
        }
        logger.debug("yMeta:{}", yMeta);

        isYData2D = yMeta.isArray();

        if (isYData2D) {
            yData1D = null;
            yData1DErr = null;
            yData2D = oiData.getColumnAsDoubles(yAxisName);
            yData2DErr = oiData.getColumnAsDoubles(yMeta.getErrorColumnName());
        } else {
            yData1D = oiData.getColumnAsDouble(yAxisName);
            yData1DErr = oiData.getColumnAsDouble(yMeta.getErrorColumnName());
            yData2D = null;
            yData2DErr = null;
        }

        // Get xAxis data:
        final boolean isXData2D;
        final double[] xData1D;
        final double[] xData1DErr;
        final double[][] xData2D;
        final double[][] xData2DErr;

        final Axis xAxis = plotDef.getXAxis();
        final String xAxisName = xAxis.getName();
        final boolean xUseLog = xAxis.isLogScale();

        final ColumnMeta xMeta = oiData.getColumnMeta(xAxisName);

        if (xMeta == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("unsupported xAxis : {} on {}", xAxis.getName(), oiData);
            }
            return null;
        }
        logger.debug("xMeta:{}", yMeta);

        // TODO support scalling function on axes
        // final boolean doScaleX = (plotDef.getxAxisScalingFactor() != null);
        // final double xScale = (doScaleX) ? plotDef.getxAxisScalingFactor().doubleValue() : 0d;
        final boolean doScaleX = false;
        final double xScale = 1d;

        isXData2D = xMeta.isArray();

        if (isXData2D) {
            xData1D = null;
            xData1DErr = null;
            xData2D = oiData.getColumnAsDoubles(xAxisName);
            xData2DErr = oiData.getColumnAsDoubles(xMeta.getErrorColumnName());
        } else {
            xData1D = oiData.getColumnAsDouble(xAxisName);
            xData1DErr = oiData.getColumnAsDouble(xMeta.getErrorColumnName());
            xData2D = null;
            xData2DErr = null;
        }


        final boolean skipFlaggedData = plotDef.isSkipFlaggedData();

        int seriesCount = dataset.getSeriesCount();

        final int nRows = oiData.getNbRows();
        final int nWaves = oiData.getNWave();

        logger.debug("nRows - nWaves : {} - {}", nRows, nWaves);

        // standard columns:
        final short[][] staIndexes = oiData.getStaIndex();
        final short[] targetIds = oiData.getTargetId();
        final boolean[][] flags = oiData.getFlag();

        final boolean hasErrX = (xData2DErr != null) || (xData1DErr != null);
        final boolean hasErrY = (yData2DErr != null) || (yData1DErr != null);
        final boolean hasFlag = (flags != null);
        final boolean hasTargetId = (targetIds != null);

        final short[][] distinctStaIndexes = oiData.getDistinctStaIndexes();
        final int nStaIndexes = distinctStaIndexes.length;
        logger.debug("nStaIndexes: {}", nStaIndexes);

        final boolean checkStaIndex = nStaIndexes > 1;
        logger.debug("checkStaIndex: {}", checkStaIndex);

        final int nFlagged = oiData.getNFlagged();
        logger.debug("nFlagged: {}", nFlagged);

        // flag to check flags on each 2D data:
        final boolean checkFlaggedData = skipFlaggedData && hasFlag && (nFlagged > 0) && (isXData2D || isYData2D);
        logger.debug("checkFlaggedData: {}", checkFlaggedData);

        // flag to check targetId on each data row:
        final boolean checkTargetId = !oiData.hasSingleTarget() && hasTargetId;

        final short matchTargetId;
        if (checkTargetId) {
            // targetID can not be null as the OIData table is supposed to have the target:
            matchTargetId = oiData.getTargetId(getTargetName());

            logger.debug("matchTargetId: {}", matchTargetId);
        } else {
            matchTargetId = -1;
        }

        // TODO: perform correct palette when using different OIWaveLength tables:

        // Prepare palette
        final Color[] colors = new Color[nWaves];

        final IndexColorModel colorModel = RAINBOW_COLOR_MODEL;

        final int iMaxColor = colorModel.getMapSize() - 1;

        final float factor = ((float) iMaxColor) / nWaves;
        float value;

        final int alphaMask = Math.round(255 * 0.8f) << 24;

        for (int i = 0; i < nWaves; i++) {
            // invert palette to have (VIOLET - BLUE - GREEN - RED) ie color spectrum:
            value = iMaxColor - factor * i;

            colors[i] = new Color(ImageUtils.getRGB(colorModel, iMaxColor, value, alphaMask), true);
        }


        final FastXYErrorRenderer renderer = (FastXYErrorRenderer) plot.getRenderer();

        // try to fill dataset:

        // avoid loop on wavelength if no 2D data:
        final int nWaveChannels = (isXData2D || isYData2D) ? nWaves : 1;

        if (logger.isDebugEnabled()) {
            logger.debug("nbSeries to create : {}", nStaIndexes * nWaveChannels);
        }

        dataset.ensureCapacity(seriesCount + nStaIndexes * nWaveChannels);

        // Use FastIntervalXYDataset for performance (arrays XY intervals)
        boolean hasData = false;
        boolean hasDataFlag = false;
        boolean hasDataErrorX = false;
        boolean hasDataErrorY = false;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double[] xValue, xLower, xUpper, yValue, yLower, yUpper, tmp;

        boolean recycleArray = false;
        double[][] arrayPool = new double[6][];

        double x, xErr, y, yErr;

        OITableSerieKey serieKey;

        short[] currentStaIndex;

        int nSkipTarget = 0;
        int nSkipFlag = 0;

        long start, total = 0l;

        // TODO: support both 1D and 2D columns:

        // TODO: unroll loops (wave / baseline) ... and avoid repeated checks on rows (targetId, baseline ...)

        // Iterate on wave channels:
        for (int i, j = 0, k, idx; j < nWaveChannels; j++) {

            // Iterate on baselines:
            for (k = 0; k < nStaIndexes; k++) {

                // get the sta index array:
                currentStaIndex = (checkStaIndex) ? distinctStaIndexes[k] : null;

                // 1 serie per baseline and per spectral channel:
                if (recycleArray) {
                    recycleArray = false;
                    xValue = arrayPool[0];
                    xLower = arrayPool[1];
                    xUpper = arrayPool[2];
                    yValue = arrayPool[3];
                    yLower = arrayPool[4];
                    yUpper = arrayPool[5];
                } else {
                    xValue = new double[nRows];
                    xLower = new double[nRows];
                    xUpper = new double[nRows];
                    yValue = new double[nRows];
                    yLower = new double[nRows];
                    yUpper = new double[nRows];
                }

                idx = 0;

                for (i = 0; i < nRows; i++) {

                    // check sta indexes ?
                    if (checkStaIndex) {
                        // note: sta indexes are compared using pointer comparison:
                        if (staIndexes[i] != currentStaIndex) {
                            // data row does not coorespond to current baseline so skip it:
                            continue;
                        }
                    }

                    if (checkFlaggedData && flags[i][j]) {
                        if (skipFlaggedData) {
                            // data point is flagged so skip it:
                            nSkipFlag++;
                            continue;
                        } else {
                            hasDataFlag = true;
                        }
                    }

                    if (checkTargetId) {
                        if (targetIds[i] != matchTargetId) {
                            // data row does not coorespond to current target so skip it:
                            nSkipTarget++;
                            continue;
                        }
                    }

                    // TODO: filter data (wavelength, baseline ...)


                    // Process Y value:
                    y = (isYData2D) ? yData2D[i][j] : yData1D[i];

                    if (yUseLog && y < 0d) {
                        // keep only positive data:
                        y = Double.NaN;
                    }

                    if (!Double.isNaN(y)) {

                        // Process X value:
                        x = (isXData2D) ? xData2D[i][j] : xData1D[i];

                        if (xUseLog && x < 0d) {
                            // keep only positive data:
                            x = Double.NaN;
                        }

                        if (!Double.isNaN(x)) {
                            // Scale X value:
                            if (doScaleX) {
                                x *= xScale;
                            }

                            // Process X / Y Errors:
                            yErr = (hasErrY) ? ((isYData2D) ? yData2DErr[i][j] : yData1DErr[i]) : Double.NaN;
                            xErr = (hasErrX) ? ((isXData2D) ? xData2DErr[i][j] : xData1DErr[i]) : Double.NaN;

                            // Define Y data:
                            if (Double.isNaN(yErr)) {
                                yValue[idx] = y;
                                yLower[idx] = Double.NaN;
                                yUpper[idx] = Double.NaN;

                                // update Y boundaries:
                                if (y < minY) {
                                    minY = y;
                                }
                                if (y > maxY) {
                                    maxY = y;
                                }
                            } else {
                                hasDataErrorY = true;

                                // useLog: check if y - err < 0:
                                yValue[idx] = y;
                                yLower[idx] = (yUseLog && (y - yErr) < 0d) ? Double.NaN : (y - yErr);
                                yUpper[idx] = y + yErr;

                                // update Y boundaries including error:
                                if (yLower[idx] < minY) {
                                    minY = yLower[idx];
                                }
                                if (yUpper[idx] > maxY) {
                                    maxY = yUpper[idx];
                                }
                            }

                            // Define X data:
                            if (Double.isNaN(xErr)) {
                                xValue[idx] = x;
                                xLower[idx] = Double.NaN;
                                xUpper[idx] = Double.NaN;

                                // update X boundaries:
                                if (x < minX) {
                                    minX = x;
                                }
                                if (x > maxX) {
                                    maxX = x;
                                }

                            } else {
                                hasDataErrorX = true;

                                // Scale X error value:
                                if (doScaleX) {
                                    xErr *= xScale;
                                }

                                xValue[idx] = x;
                                xLower[idx] = (xUseLog && (x - xErr) < 0d) ? Double.NaN : (x - xErr);
                                xUpper[idx] = x + xErr;

                                // update X boundaries including error:
                                if (xLower[idx] < minX) {
                                    minX = xLower[idx];
                                }
                                if (xUpper[idx] > maxX) {
                                    maxX = xUpper[idx];
                                }
                            }

                            idx++;
                        } // x defined

                    } // y defined

                } // loop on data rows

                if (idx > 0) {
                    hasData = true;

                    // crop data arrays:
                    if (idx < nRows) {
                        recycleArray = true;
                        arrayPool[0] = xValue;
                        arrayPool[1] = xLower;
                        arrayPool[2] = xUpper;
                        arrayPool[3] = yValue;
                        arrayPool[4] = yLower;
                        arrayPool[5] = yUpper;

                        xValue = extract(xValue, idx);
                        xLower = extract(xLower, idx);
                        xUpper = extract(xUpper, idx);
                        yValue = extract(yValue, idx);
                        yLower = extract(yLower, idx);
                        yUpper = extract(yUpper, idx);
                    }

                    // TODO: add oiTable, i (row), j (nWave) in dataset:
                    serieKey = new OITableSerieKey(tableIndex, k, j);

                    start = System.nanoTime();

                    // Avoid any key conflict:
                    dataset.addSeries(serieKey, new double[][]{xValue, xLower, xUpper, yValue, yLower, yUpper});

                    total += System.nanoTime() - start;

                    // TODO: adjust renderer settings per Serie (color, shape ...) !
                    renderer.setSeriesPaint(seriesCount, colors[j], false);

                    seriesCount++;
                }

            } // iterate on baselines

        } // iterate on wave channels

        if (logger.isDebugEnabled()) {
            if (nSkipFlag > 0) {
                logger.debug("Nb SkipFlag: {}", nSkipFlag);
            }
            if (nSkipTarget > 0) {
                logger.debug("Nb SkipTarget: {}", nSkipTarget);
            }

            logger.debug("nSeries {} vs {}", seriesCount, dataset.getSeriesCount());
        }
        logger.warn("total addSeries duration = {} ms.", 1e-6d * total);

        if (!hasData) {
            return null;
        }

        // TODO: adjust renderer settings per Serie (color, shape ...) !

        // set shape depending on error (triangle or square):
        final Shape shape = getPointShape(!hasDataFlag);

        // enable/disable X error rendering (performance):
        renderer.setDrawXError(hasDataErrorX);

        // enable/disable Y error rendering (performance):
        renderer.setDrawYError(hasDataErrorY);

        // use deprecated method but defines shape once for ALL series (performance):
        renderer.setShape(shape);
        renderer.setLinesVisible(plotDef.isDrawLine());

        final PlotInfo info = new PlotInfo();
        info.xRange = new Range(minX, maxX);
        info.xMeta = xMeta;
        info.xUseLog = xUseLog;

        info.yRange = new Range(minY, maxY);
        info.yMeta = yMeta;
        info.yUseLog = yUseLog;
        return info;
    }

    private double[] extract(final double[] input, final int len) {
        final double[] output = new double[len];
        System.arraycopy(input, 0, output, 0, len);
        return output;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    /** drawing started time value */
    private long chartDrawStartTime = 0l;

    /**
     * Handle the chart progress event to log the chart rendering delay
     * @param event chart progress event
     */
    @Override
    public void chartProgress(final ChartProgressEvent event) {
        if (logger.isDebugEnabled()) {
            switch (event.getType()) {
                case ChartProgressEvent.DRAWING_STARTED:
                    this.chartDrawStartTime = System.nanoTime();
                    break;
                case ChartProgressEvent.DRAWING_FINISHED:
                    logger.debug("Drawing chart time[{}] = {} ms.", getTargetName(), 1e-6d * (System.nanoTime() - this.chartDrawStartTime));
                    this.chartDrawStartTime = 0l;
                    break;
                default:
            }
        }

        // DEBUG (TODO KILL ASAP):
        switch (event.getType()) {
            case ChartProgressEvent.DRAWING_STARTED:
                this.chartDrawStartTime = System.nanoTime();
                break;
            case ChartProgressEvent.DRAWING_FINISHED:
                logger.warn("Drawing chart time[{}] = {} ms.", getTargetName(), 1e-6d * (System.nanoTime() - this.chartDrawStartTime));
                this.chartDrawStartTime = 0l;
                break;
            default:
        }

        // Perform custom operations before/after chart rendering:
        // move JMMC annotations:
        if (this.xyPlotPlot1.getDomainAxis() != null) {
            this.aJMMCPlot1.setX(this.xyPlotPlot1.getDomainAxis().getUpperBound());
            this.aJMMCPlot1.setY(this.xyPlotPlot1.getRangeAxis().getLowerBound());
        }
        if (this.xyPlotPlot2.getDomainAxis() != null) {
            this.aJMMCPlot2.setX(this.xyPlotPlot2.getDomainAxis().getUpperBound());
            this.aJMMCPlot2.setY(this.xyPlotPlot2.getRangeAxis().getLowerBound());
        }
    }

    private void applyColorTheme() {
        // update theme at end :
        ChartUtilities.applyCurrentTheme(this.chart);

        if (this.xyPlotPlot1 != null) {
            this.xyPlotPlot1.setBackgroundPaint(Color.WHITE);
            this.xyPlotPlot1.setDomainGridlinePaint(Color.LIGHT_GRAY);
            this.xyPlotPlot1.setRangeGridlinePaint(Color.LIGHT_GRAY);
        }

        if (this.xyPlotPlot2 != null) {
            this.xyPlotPlot2.setBackgroundPaint(Color.WHITE);
            this.xyPlotPlot2.setDomainGridlinePaint(Color.LIGHT_GRAY);
            this.xyPlotPlot2.setRangeGridlinePaint(Color.LIGHT_GRAY);
        }
    }

    /**
     * Return the shape used to represent points on the plot
     * @param hasError flag indicating to return the shape associated to data with error or without
     * @return shape
     */
    private static Shape getPointShape(final boolean hasError) {

        if (hasError) {
            return new Rectangle2D.Double(-3d, -3d, 6d, 6d);
        }

        // equilateral triangle centered on its barycenter:
        final GeneralPath path = new GeneralPath();

        path.moveTo(0f, -4f);
        path.lineTo(3f, 2f);
        path.lineTo(-3f, 2f);
        path.lineTo(0f, -4f);

        return path;
    }

    /**
     * Return true if the plot has data (dataset not empty)
     * @return true if the plot has data 
     */
    public boolean isHasData() {
        return hasData;
    }

    /* --- OIFits helper : TODO move elsewhere --- */
    /**
     * Return the unique String values from given operator applied on given OIData tables
     * @param oiDataList OIData tables
     * @param set set instance to use
     * @param operator operator to get String values
     * @return unique String values
     */
    private static Set<String> getDistinct(final OIData[] oiDataList, final Set<String> set, final GetOIDataString operator) {
        String value;
        for (OIData oiData : oiDataList) {
            value = operator.getString(oiData);
            if (value != null) {
                logger.debug("getDistinct: {}", value);

                int pos = value.indexOf('_');

                if (pos != -1) {
                    value = value.substring(0, pos);
                }

                set.add(value);
            }
        }
        return set;
    }

    /**
     * Return the unique staNames values from given OIData tables
     * @param oiDataList OIData tables
     * @param set set instance to use
     */
    private static void getDistinctStaNames(final OIData[] oiDataList, final Set<String> set) {
        String staNames;
        for (OIData oiData : oiDataList) {
            for (short[] staIndexes : oiData.getDistinctStaIndex()) {
                staNames = oiData.getStaNames(staIndexes);

                logger.debug("staNames : {}", staNames);

                set.add(staNames);
            }
        }
    }

    /**
     * Return the unique staNames values from given OIData tables
     * @param oiDataList OIData tables
     * @param set set instance to use
     */
    private static void getDistinctStaConfs(final OIData[] oiDataList, final Set<String> set) {
        String staNames;
        for (OIData oiData : oiDataList) {
            for (short[] staIndexes : oiData.getDistinctStaConf()) {
                staNames = oiData.getStaNames(staIndexes);

                logger.debug("staConf : {}", staNames);

                set.add(staNames);
            }
        }
    }

    /**
     * Return the unique wave length ranges from given OIData tables
     * @param oiDataList OIData tables
     * @param set set instance to use
     */
    private static void getDistinctWaveLengthRange(final OIData[] oiDataList, final Set<String> set) {
        final StringBuilder sb = new StringBuilder();

        String wlenRange;
        float[] effWaveRange;
        for (OIData oiData : oiDataList) {
            effWaveRange = oiData.getEffWaveRange();

            if (effWaveRange != null) {
                sb.append("[").append(df4.format(1e6f * effWaveRange[0])).append(" \u00B5m - ").append(df4.format(1e6f * effWaveRange[1])).append(" \u00B5m]");

                wlenRange = sb.toString();
                sb.setLength(0);

                logger.debug("wlen range : {}", wlenRange);

                set.add(wlenRange);
            }
        }
    }

    private static void toString(final Set<String> set, final StringBuilder sb, final String internalSeparator, final String separator) {
        toString(set, sb, internalSeparator, separator, Integer.MAX_VALUE);
    }

    private static void toString(final Set<String> set, final StringBuilder sb, final String internalSeparator, final String separator, final int threshold, final String alternateText) {
        // hard coded limit:
        if (set.size() > threshold) {
            sb.append(alternateText);
        } else {
            toString(set, sb, internalSeparator, separator, Integer.MAX_VALUE);
        }
    }

    private static void toString(final Set<String> set, final StringBuilder sb, final String internalSeparator, final String separator, final int maxLength) {
        int n = 0;
        for (String v : set) {
            sb.append(v.replaceAll("\\s", internalSeparator)).append(separator);
            n++;
            if (n > maxLength) {
                return;
            }
        }
        if (n != 0) {
            // remove separator at the end:
            sb.setLength(sb.length() - separator.length());
        }
    }

    /**
     * Get String operator applied on any OIData table
     */
    private interface GetOIDataString {

        /**
         * Return a String value (keyword for example) for the given OIData table
         * @param oiData OIData table
         * @return String value
         */
        public String getString(final OIData oiData);
    }

    private Plot getPlot() {
        if (this.plot == null) {
            this.plot = ocm.getPlotRef(plotId);
        }
        return this.plot;
    }

    /**
     * Define the plot identifier, reset plot and fireOIFitsCollectionChanged on this instance if the plotId changed
     * @param plotId plot identifier
     */
    public void setPlotId(final String plotId) {
        final String prevPlotId = this.plotId;
        this.plotId = plotId;
        // force reset:
        this.plot = null;

        if (plotId != null && !ObjectUtils.areEquals(prevPlotId, plotId)) {
            logger.debug("setPlotId {}", plotId);

            // fire PlotChanged event to initialize correctly the widget:
            ocm.firePlotChanged(null, plotId, this); // null forces different source
        }
    }

    private PlotDefinition getPlotDefinition() {
        if (getPlot() == null) {
            return null;
        }
        return getPlot().getPlotDefinition();
    }

    private OIFitsFile getOiFitsSubset() {
        if (getPlot() == null || getPlot().getSubsetDefinition() == null) {
            return null;
        }
        return getPlot().getSubsetDefinition().getOIFitsSubset();
    }

    private String getTargetName() {
        if (getPlot() == null || getPlot().getSubsetDefinition() == null || getPlot().getSubsetDefinition().getTarget() == null) {
            return null;
        }
        return getPlot().getSubsetDefinition().getTarget().getTarget();
    }

    /**
     * Plot information used during data processing
     */
    private static class PlotInfo {

        /** x data range */
        Range xRange = null;
        /** x colum meta data */
        ColumnMeta xMeta = null;
        /** x log axis */
        boolean xUseLog;
        /** y data range */
        Range yRange = null;
        /** y colum meta data */
        ColumnMeta yMeta = null;
        /** y log axis */
        boolean yUseLog;
    }

    /*
     * OIFitsCollectionManagerEventListener implementation 
     */
    /**
     * Return the optional subject id i.e. related object id that this listener accepts
     * @param type event type
     * @return subject id (null means accept any event) or DISCARDED_SUBJECT_ID to discard event
     */
    public String getSubjectId(final OIFitsCollectionManagerEventType type) {
        switch (type) {
            case PLOT_CHANGED:
                return plotId;
            default:
        }
        return DISCARDED_SUBJECT_ID;
    }

    /**
     * Handle the given OIFits collection event
     * @param event OIFits collection event
     */
    @Override
    public void onProcess(final OIFitsCollectionManagerEvent event) {
        logger.debug("onProcess {}", event);

        switch (event.getType()) {
            case PLOT_CHANGED:
                /* store plot instance (reference) */
                plot = event.getPlot();

                updatePlot();
                break;
            default:
        }
        logger.debug("onProcess {} - done", event);
    }
}
