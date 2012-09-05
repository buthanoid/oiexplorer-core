/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmal.image.ColorModels;
import fr.jmmc.jmal.image.ImageUtils;
import fr.jmmc.oiexplorer.core.gui.action.ExportPDFAction;
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
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionEventListener;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.event.GenericEvent;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEventType;
import fr.jmmc.oiexplorer.core.model.event.PlotEvent;
import fr.jmmc.oiexplorer.core.model.oi.Plot;
import fr.jmmc.oiexplorer.core.model.oi.SubsetDefinition;
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
import org.jfree.chart.axis.LogarithmicAxis;
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
 * This panel presents the interferometer plot (station, base lines ...)
 * 
 * @author bourgesl
 */
public final class Vis2Panel extends javax.swing.JPanel implements ChartProgressListener, EnhancedChartMouseListener, ChartMouseSelectionListener,
                                                                   PDFExportable, OIFitsCollectionEventListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(Vis2Panel.class.getName());
    /** flag to enable log axis to display log(Vis2) */
    private final static boolean USE_LOG_SCALE = false;
    /** default color model (aspro - Rainbow) */
    private final static IndexColorModel RAINBOW_COLOR_MODEL = ColorModels.getColorModel("Rainbow");
    /** data margin in percents */
    private final static double MARGIN_PERCENTS = 5d / 100d;
    /** double formatter for wave lengths */
    private final static NumberFormat df4 = new DecimalFormat("0.000#");

    /* members */
    /** OIFitsCollectionManager singleton */
    private final OIFitsCollectionManager ocm = OIFitsCollectionManager.getInstance();
    /** plot identifier */
    private String plotId = OIFitsCollectionManager.CURRENT;
    /** plot object to be plotted */
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
    /* TODO: List<XYPlot */
    /** xy plot instance for VIS2 */
    private XYPlot xyPlotV2;
    /** xy plot instance for T3 */
    private XYPlot xyPlotT3;
    /** JMMC annotation */
    private XYTextAnnotation aJMMCV2 = null;
    /** JMMC annotation */
    private XYTextAnnotation aJMMCT3 = null;

    /**
     * Constructor
     */
    public Vis2Panel() {
        ocm.getPlotEventNotifier().register(this);

        initComponents();
        postInit();
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
                toString(distinct, sb, "_", "_");
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
                toString(distinct, sb, "_", "_");
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
                toString(distinct, sb, "-", "_");
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
                toString(distinct, sb, "_", "_");
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
        final boolean useSelectionSupport = false;

        final boolean showLegend = false;

        // create chart and add listener :
        this.combinedXYPlot = new CombinedDomainXYPlot(ChartUtils.createAxis("UV radius (M\u03BB)"));
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

        this.xyPlotV2 = createScientificScatterPlot(null, "V²", usePlotCrossHairSupport);

        this.aJMMCV2 = ChartUtils.createJMMCAnnotation(Constants.JMMC_ANNOTATION);
        this.xyPlotV2.getRenderer().addAnnotation(this.aJMMCV2, Layer.BACKGROUND);

        this.xyPlotT3 = createScientificScatterPlot(null, "Closure phase (deg)", usePlotCrossHairSupport);

        this.aJMMCT3 = ChartUtils.createJMMCAnnotation(Constants.JMMC_ANNOTATION);
        this.xyPlotT3.getRenderer().addAnnotation(this.aJMMCT3, Layer.BACKGROUND);

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

            final XYPlot plot = this.plotIndexMapping.get(plotIndex);
            if (plot == null) {
                return;
            }

            final ValueAxis domainAxis = plot.getDomainAxis();
            final double domainValue = domainAxis.java2DToValue(point2D.getX(), dataArea, plot.getDomainAxisEdge());

            final ValueAxis rangeAxis = plot.getRangeAxis();
            final double rangeValue = rangeAxis.java2DToValue(point2D.getY(), dataArea, plot.getRangeAxisEdge());

            logger.warn("Mouse coordinates are (" + i + ", " + j + "), in data space = (" + domainValue + ", " + rangeValue + ")");

            // aspect ratio:
            final double xRatio = dataArea.getWidth() / Math.abs(domainAxis.getUpperBound() - domainAxis.getLowerBound());
            final double yRatio = dataArea.getHeight() / Math.abs(rangeAxis.getUpperBound() - rangeAxis.getLowerBound());

            // find matching data ie. closest data point according to its screen distance to the mouse clicked point:
            Point2D dataPoint = findDataPoint(plot, domainValue, rangeValue, xRatio, yRatio);

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
        logger.warn("mouseSelected: rectangle {}", selection);

        // find data points:
        final List<Point2D> points = findDataPoints(selection);

        this.selectionOverlay.setRectSelArea(selection);

        // push data points to overlay for rendering:
        this.selectionOverlay.setPoints(points);
    }

    /**
     * Find data point closest in FIRST dataset to the given coordinates X / Y
     * @param plot xy plot to get its dataset
     * @param anchorX domain axis coordinate
     * @param anchorY range axis coordinate
     * @param xRatio pixels per data on domain axis
     * @param yRatio pixels per data on range axis
     * @return found Point2D (data coordinates) or Point2D(NaN, NaN)
     */
    private static Point2D findDataPoint(final XYPlot plot, final double anchorX, final double anchorY, final double xRatio, final double yRatio) {
        final XYDataset dataset = plot.getDataset();

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

            logger.warn("findDataPoint: duration = {} ms.", 1e-6d * (System.nanoTime() - startTime));

            if (matchItem != -1) {
                final double matchX = dataset.getXValue(matchSerie, matchItem);
                final double matchY = dataset.getYValue(matchSerie, matchItem);

                logger.warn("Matching item [serie = " + matchSerie + ", item = " + matchItem + "] : (" + matchX + ", " + matchY + ")");

                return new Point2D.Double(matchX, matchY);
            }
        }

        logger.warn("No Matching item.");

        return new Point2D.Double(Double.NaN, Double.NaN);
    }

    /**
     * Find data points inside the given Shape (data coordinates)
     * @param shape shape to use
     * @return found list of Point2D (data coordinates) or empty list
     */
    private List<Point2D> findDataPoints(final Shape shape) {
        final XYDataset dataset = this.xyPlotV2.getDataset();

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

            logger.warn("findDataPoints: duration = {} ms.", 1e-6d * (System.nanoTime() - startTime));
            if (false) {
                logger.warn("Matching points: {}", points);
            }
        }
        return points;
    }

    /**
     * Plot the generated file synchronously.
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
        this.xyPlotV2.setNotify(false);
        this.xyPlotT3.setNotify(false);
        try {
            // reset title:
            ChartUtils.clearTextSubTitle(this.chart);

            removeAllSubPlots();

            // reset dataset:
            this.xyPlotV2.setDataset(null);
            this.xyPlotT3.setDataset(null);

            // useless:
//            applyColorTheme();

            this.resetOverlays();

            this.chartPanel.setVisible(this.hasData);

        } finally {
            // restore chart & plot notifications:
            this.xyPlotT3.setNotify(true);
            this.xyPlotV2.setNotify(true);
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
        this.xyPlotV2.setNotify(false);
        this.xyPlotT3.setNotify(false);

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
            this.hasData = updateChart();

            if (this.hasData) {
                applyColorTheme();
            }

            this.resetOverlays();

            this.chartPanel.setVisible(this.hasData);

        } finally {
            // restore chart & plot notifications:
            this.xyPlotT3.setNotify(true);
            this.xyPlotV2.setNotify(true);
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

        if (!plotDef.getYAxes().isEmpty()) {

            if (oiFitsSubset.getNbOiTables() > 0) {
                // use new dataset so free memory:
                this.xyPlotV2.setDataset(new FastIntervalXYDataset<OITableSerieKey, OITableSerieKey>());

                double minY = Double.POSITIVE_INFINITY;
                double maxY = Double.NEGATIVE_INFINITY;

                PlotInfo info;
                Rectangle2D.Double dataArea;
                ColumnMeta yMeta = null;

                int tableIndex = 0;
                for (OITable oiTable : oiFitsSubset.getOiTables()) {

                    info = updatePlot(this.xyPlotV2, (OIData) oiTable, tableIndex, plotDef, 0);

                    if (info != null) {
                        showV2 = true;
                        dataArea = info.dataArea;

                        // combine X range:
                        minX = Math.min(minX, dataArea.getX());
                        maxX = Math.max(maxX, dataArea.getMaxX());

                        // combine Y range:
                        minY = Math.min(minY, dataArea.getY());
                        maxY = Math.max(maxY, dataArea.getMaxY());

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
                    logger.info("xyPlotV2: nbSeries = {}", this.xyPlotV2.getDataset().getSeriesCount());

                    // TODO: fix boundaries according to standard data boundaries (VIS between 0-1 ...)

                    // Add margin:
                    if (!USE_LOG_SCALE) {
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

                    // Update Y axis:

                    if (this.xyPlotV2.getRangeAxis() instanceof BoundedNumberAxis) {
                        axis = (BoundedNumberAxis) this.xyPlotV2.getRangeAxis();
                        axis.setBounds(new Range(minY, maxY));
                        axis.setRange(minY, maxY);
                    }

                    // update Y axis Label:
                    String label;
                    if (yMeta != null) {
                        label = yMeta.getName();
                        if (yMeta != null && yMeta.getUnits() != Units.NO_UNIT) {
                            label += " (" + yMeta.getUnits().getStandardRepresentation() + ")";
                        }
                        this.xyPlotV2.getRangeAxis().setLabel(label);
                    }

                    if (USE_LOG_SCALE) {
                        // test logarithmic axis:
                        final LogarithmicAxis logAxis = new LogarithmicAxis("log " + label);
                        logAxis.setExpTickLabelsFlag(true);
                        logAxis.setAutoRangeNextLogFlag(true);

                        logger.debug("logAxis range: [{} - {}]", minY, maxY);

                        logAxis.setRange(minY, maxY);

                        this.xyPlotV2.setRangeAxis(logAxis);
                    }
                }
            }
        }

        if (showV2) {
            this.combinedXYPlot.add(this.xyPlotV2, 1);

            final Integer plotIndex = Integer.valueOf(1);
            this.plotMapping.put(this.xyPlotV2, plotIndex);
            this.plotIndexMapping.put(plotIndex, this.xyPlotV2);

        } else {
            // reset Vis2 dataset:
            this.xyPlotV2.setDataset(null);
        }

        if (plotDef.getYAxes().size() > 1) {
            if (oiFitsSubset.getNbOiTables() > 0) {
                // use new dataset so free memory:
                this.xyPlotT3.setDataset(new FastIntervalXYDataset<OITableSerieKey, OITableSerieKey>());

                double minY = Double.POSITIVE_INFINITY;
                double maxY = Double.NEGATIVE_INFINITY;

                PlotInfo info;
                Rectangle2D.Double dataArea;
                ColumnMeta yMeta = null;

                int tableIndex = 0;
                for (OITable oiTable : oiFitsSubset.getOiTables()) {

                    info = updatePlot(this.xyPlotT3, (OIData) oiTable, tableIndex, plotDef, 1);

                    if (info != null) {
                        showT3 = true;
                        dataArea = info.dataArea;

                        // combine X range:
                        minX = Math.min(minX, dataArea.getX());
                        maxX = Math.max(maxX, dataArea.getMaxX());

                        // combine Y range:
                        minY = Math.min(minY, dataArea.getY());
                        maxY = Math.max(maxY, dataArea.getMaxY());

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
                    logger.info("xyPlotT3: nbSeries = {}", this.xyPlotT3.getDataset().getSeriesCount());

                    // TODO: fix boundaries according to standard data boundaries (T3 between -180-180 ...)

                    // Add margin:
                    if (!USE_LOG_SCALE) {
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

                    // Update Y axis:

                    if (this.xyPlotT3.getRangeAxis() instanceof BoundedNumberAxis) {
                        axis = (BoundedNumberAxis) this.xyPlotT3.getRangeAxis();
                        axis.setBounds(new Range(minY, maxY));
                        axis.setRange(minY, maxY);
                    }

                    // update Y axis Label:
                    if (yMeta != null) {
                        String label = yMeta.getName();
                        if (yMeta != null && yMeta.getUnits() != Units.NO_UNIT) {
                            label += " (" + yMeta.getUnits().getStandardRepresentation() + ")";
                        }
                        this.xyPlotT3.getRangeAxis().setLabel(label);
                    }
                }
            }
        }

        if (showT3) {
            this.combinedXYPlot.add(this.xyPlotT3, 1);

            final Integer plotIndex = (showV2) ? Integer.valueOf(2) : Integer.valueOf(1);
            this.plotMapping.put(this.xyPlotT3, plotIndex);
            this.plotIndexMapping.put(plotIndex, this.xyPlotT3);

        } else {
            // reset T3 dataset:
            this.xyPlotT3.setDataset(null);
        }

        if (!showV2 && !showT3) {
            if (logger.isInfoEnabled()) {
                logger.info("updateChart : duration = {} ms.", 1e-6d * (System.nanoTime() - start));
            }
            return false;
        }

        // TODO: fix boundaries according to standard data boundaries (spatial freq >= 0)

        if (plotDef.getXAxis().isIncludeZero()) {
            // fix minX to include zero spatial frequency:
            if (minX > 0d) {
                minX = 0d;
            }
        }

        // Add margin:
        final double marginX = (maxX - minX) * MARGIN_PERCENTS;
        if (marginX > 0d) {
            minX -= marginX;
            maxX += marginX;
        } else {
            minX -= minX * MARGIN_PERCENTS;
            maxX += maxX * MARGIN_PERCENTS;
        }
        if (maxX == minX) {
            maxX = minX + 1d;
        }

        logger.debug("domainAxis: {} - {}", minX, maxX);

        axis = (BoundedNumberAxis) this.combinedXYPlot.getDomainAxis();
        axis.setBounds(new Range(minX, maxX));
        axis.setRange(minX, maxX);

        // update X axis Label:
        if (xMeta != null) {
            String label = xMeta.getName();
            if (xMeta != null && xMeta.getUnits() != Units.NO_UNIT) {
                label += " (" + xMeta.getUnits().getStandardRepresentation() + ")";
            }
            this.combinedXYPlot.getDomainAxis().setLabel(label);
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
     * @return rectangle giving data area or null if no data
     */
    private PlotInfo updatePlot(final XYPlot plot, final OIData oiData, final int tableIndex,
                                final PlotDefinition plotDef, final int yAxisIndex) {

        // Get yAxis data:
        final boolean isYData2D;
        final double[] yData1D;
        final double[] yData1DErr;
        final double[][] yData2D;
        final double[][] yData2DErr;

        final Axis yAxis = plotDef.getYAxes().get(yAxisIndex);
        final String yAxisName = yAxis.getName();
        logger.info("yAxis:{}", yAxisName);

        final ColumnMeta yMeta = oiData.getColumnMeta(yAxisName);
        logger.info("yMeta:{}", yMeta);

        if (yMeta == null) {
            logger.info("unsupported yAxis : {} on {}", yMeta, oiData);
            return null;
        }
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

        final String xAxis = plotDef.getXAxis().getName();
        logger.info("xAxis:{}", xAxis);

        //TODO support scalling function on axes
        // final boolean doScaleX = (plotDef.getxAxisScalingFactor() != null);
        // final double xScale = (doScaleX) ? plotDef.getxAxisScalingFactor().doubleValue() : 0d;
        final boolean doScaleX = false;
        final double xScale = 1d;

        final ColumnMeta xMeta = oiData.getColumnMeta(xAxis);
        logger.info("xMeta:{}", xMeta);

        if (xMeta == null) {
            logger.info("unsupported xAxis : {} on {}", xMeta, oiData);
            return null;
        }
        isXData2D = xMeta.isArray();

        if (isXData2D) {
            xData1D = null;
            xData1DErr = null;
            xData2D = oiData.getColumnAsDoubles(xAxis);
            xData2DErr = oiData.getColumnAsDoubles(xMeta.getErrorColumnName());
        } else {
            xData1D = oiData.getColumnAsDouble(xAxis);
            xData1DErr = oiData.getColumnAsDouble(xMeta.getErrorColumnName());
            xData2D = null;
            xData2DErr = null;
        }


        final boolean skipFlaggedData = plotDef.isSkipFlaggedData();

        @SuppressWarnings("unchecked")
        final FastIntervalXYDataset<OITableSerieKey, OITableSerieKey> dataset = (FastIntervalXYDataset<OITableSerieKey, OITableSerieKey>) plot.getDataset();
        int seriesCount = dataset.getSeriesCount();

        final int nRows = oiData.getNbRows();
        final int nWaves = oiData.getNWave();

        logger.warn("nRows - nWaves : {} - {}", nRows, nWaves);

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
        logger.warn("nStaIndexes: {}", nStaIndexes);

        final boolean checkStaIndex = nStaIndexes > 1;
        logger.warn("checkStaIndex: {}", checkStaIndex);

        final int nFlagged = oiData.getNFlagged();
        logger.warn("nFlagged: {}", nFlagged);

        // flag to check flags on each 2D data:
        final boolean checkFlaggedData = skipFlaggedData && hasFlag && (nFlagged > 0) && (isXData2D || isYData2D);
        logger.warn("checkFlaggedData: {}", checkFlaggedData);

        // flag to check targetId on each data row:
        final boolean checkTargetId = !oiData.hasSingleTarget() && hasTargetId;

        final short matchTargetId;
        if (checkTargetId) {
            // targetID can not be null as the OIData table is supposed to have the target:
            matchTargetId = oiData.getTargetId(getTargetName());

            logger.warn("matchTargetId: {}", matchTargetId);
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

        logger.warn("nbSeries to create : {}", nStaIndexes * nWaveChannels);

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

                    if (USE_LOG_SCALE && y < 0d) {
                        // keep only positive data:
                        y = Double.NaN;
                    }

                    if (!Double.isNaN(y)) {

                        // Process X value:
                        x = (isXData2D) ? xData2D[i][j] : xData1D[i];

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

                                // USE_LOG_SCALE: check if y - err < 0:
                                yValue[idx] = y;
                                yLower[idx] = (USE_LOG_SCALE && (y - yErr) < 0d) ? 0d : (y - yErr);
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
                                xLower[idx] = x - xErr;
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

        if (nSkipFlag > 0) {
            logger.warn("Nb SkipFlag: {}", nSkipFlag);
        }
        if (nSkipTarget > 0) {
            logger.warn("Nb SkipTarget: {}", nSkipTarget);
        }

        if (logger.isDebugEnabled()) {
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
        info.dataArea = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
        info.xMeta = xMeta;
        info.yMeta = yMeta;
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
        if (this.xyPlotV2.getDomainAxis() != null) {
            this.aJMMCV2.setX(this.xyPlotV2.getDomainAxis().getUpperBound());
            this.aJMMCV2.setY(this.xyPlotV2.getRangeAxis().getLowerBound());
        }
        if (this.xyPlotT3.getDomainAxis() != null) {
            this.aJMMCT3.setX(this.xyPlotT3.getDomainAxis().getUpperBound());
            this.aJMMCT3.setY(this.xyPlotT3.getRangeAxis().getLowerBound());
        }
    }

    private void applyColorTheme() {
        // update theme at end :
        ChartUtilities.applyCurrentTheme(this.chart);

        if (this.xyPlotV2 != null) {
            this.xyPlotV2.setBackgroundPaint(Color.WHITE);
            this.xyPlotV2.setDomainGridlinePaint(Color.LIGHT_GRAY);
            this.xyPlotV2.setRangeGridlinePaint(Color.LIGHT_GRAY);
        }

        if (this.xyPlotT3 != null) {
            this.xyPlotT3.setBackgroundPaint(Color.WHITE);
            this.xyPlotT3.setDomainGridlinePaint(Color.LIGHT_GRAY);
            this.xyPlotT3.setRangeGridlinePaint(Color.LIGHT_GRAY);
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
        for (String v : set) {
            sb.append(v.replaceAll("\\s", internalSeparator)).append(separator);
        }
        sb.setLength(sb.length() - separator.length());
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

    /**
     * Handle the given OIFits collection event
     * @param event OIFits collection event
     */
    @Override
    public void onProcess(GenericEvent<OIFitsCollectionEventType> event) {
        logger.warn("onProcess : {}", event);

        switch (event.getType()) {
            case PLOT_CHANGED:
                final Plot eventPlot = ((PlotEvent) event).getPlot();

                // TODO: see EventNotifier: subject ?

                // check plot identifier:
                if (eventPlot.getName().equals(this.plotId)) {
                    /* store plot instance */
                    this.plot = eventPlot;

                    logger.warn("plot.subset: {}", this.plot.getSubsetDefinition());
                    if (getPlot().getSubsetDefinition() != null) {
                        logger.warn("plot.subset target: {}", getTargetName());
                        logger.warn("plot.subset oifits: {}", getOiFitsSubset());
                    }
                    logger.warn("plot.definition: {}", getPlotDefinition());
                    if (getPlot().getPlotDefinition() != null) {
                        logger.warn("plot.def xAxis: {}", getPlotDefinition().getXAxis());
                        logger.warn("plot.def yAxes: {}", getPlotDefinition().getYAxes());
                    }

                    updatePlot();
                }
                break;
            default:
        }
    }

    private Plot getPlot() {
        if (this.plot == null) {
            this.plot = ocm.getPlot(plotId);
        }
        return this.plot;
    }

    /**
     * Define the plot identifier and reset plot
     * @param plotId subset identifier
     */
    public void setPlotId(final String plotId) {
        this.plotId = plotId;
        // force reset:
        this.plot = null;
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
     * TODO: refine following class
     */
    private static class PlotInfo {

        protected Rectangle2D.Double dataArea = null;
        protected ColumnMeta xMeta = null;
        protected ColumnMeta yMeta = null;
    }
}
