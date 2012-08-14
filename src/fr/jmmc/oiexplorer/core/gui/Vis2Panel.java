/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

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
import fr.jmmc.jmal.image.ColorModels;
import fr.jmmc.jmal.image.ImageUtils;
import fr.jmmc.oiexplorer.core.gui.action.ExportPDFAction;
import fr.jmmc.oiexplorer.core.model.TargetUID;
import fr.jmmc.oiexplorer.core.util.Constants;
import fr.jmmc.oitools.model.OIData;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OIT3;
import fr.jmmc.oitools.model.OIVis2;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

/**
 * This panel presents the interferometer plot (station, base lines ...)
 * @author bourgesl
 */
public final class Vis2Panel extends javax.swing.JPanel implements ChartProgressListener, EnhancedChartMouseListener, ChartMouseSelectionListener,
                                                                   PDFExportable {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(Vis2Panel.class.getName());
    /** flag to enable log axis to display log(Vis2) */
    private final static boolean USE_LOG_SCALE = false;
    /** default color model (aspro - Rainbow) */
    private final static IndexColorModel RAINBOW_COLOR_MODEL = ColorModels.getColorModel("Rainbow");
    /** scaling factor to Mega Lambda for U,V points */
    private final static double MEGA_LAMBDA_SCALE = 1e-6;
    /** data margin in percents */
    private final static double MARGIN_PERCENTS = 5d / 100d;
    /** double formatter for wave lengths */
    private final static NumberFormat df4 = new DecimalFormat("0.000#");

    /* members */
    /** current target */
    private TargetUID target = null;
    /** current oifits file */
    private OIFitsFile oiFitsFile = null;
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
    /** xy plot instance for VIS2 */
    private XYPlot xyPlotV2;
    /** xy plot instance for T3 */
    private XYPlot xyPlotT3;
    /** JMMC annotation */
    private XYTextAnnotation aJMMCV2 = null;
    /** JMMC annotation */
    private XYTextAnnotation aJMMCT3 = null;
    /** scaling factor on X data */
    private double plotXScalingFactor = MEGA_LAMBDA_SCALE;
    /** chart panel */
    private ChartPanel chartPanel;
    /** crosshair overlay */
    private CombinedCrosshairOverlay crosshairOverlay = null;
    /** selection overlay */
    private SelectionOverlay selectionOverlay = null;

    /**
     * Constructor
     */
    public Vis2Panel() {
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
        if (this.oiFitsFile != null) {
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
        if (this.oiFitsFile != null) {
            final boolean hasVis2 = this.oiFitsFile.hasOiVis2();
            final boolean hasT3 = this.oiFitsFile.hasOiT3();

            final Set<String> distinct = new LinkedHashSet<String>();

            // TODO: fix this code as the plot will become generic !

            // TODO: keep values from dataset: insName, baselines, dateObs ... IF HAS DATA (filtered)

            final StringBuilder sb = new StringBuilder(32);

            if (hasVis2) {
                sb.append("Vis2_");
            }
            if (this.oiFitsFile.hasOiT3()) {
                sb.append("T3_");
            }

            // Add target name:
            final String targetName = this.target.getTarget();
            final String altName = targetName.replaceAll(Constants.REGEXP_INVALID_TEXT_CHARS, "_");

            sb.append(altName).append('_');

            // Add distinct arrNames:
            final GetOIDataString arrNameOperator = new GetOIDataString() {
                public String getString(final OIData oiData) {
                    return oiData.getArrName();
                }
            };
            if (hasVis2) {
                getDistinct(this.oiFitsFile.getOiVis2(), distinct, arrNameOperator);
            }
            if (hasT3) {
                getDistinct(this.oiFitsFile.getOiT3(), distinct, arrNameOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, "_", "_");
            }

            // Add unique insNames:
            final GetOIDataString insNameOperator = new GetOIDataString() {
                public String getString(final OIData oiData) {
                    return oiData.getInsName();
                }
            };
            distinct.clear();
            if (hasVis2) {
                getDistinct(this.oiFitsFile.getOiVis2(), distinct, insNameOperator);
            }
            if (hasT3) {
                getDistinct(this.oiFitsFile.getOiT3(), distinct, insNameOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, "_", "_");
            }

            // Add unique baselines:
            distinct.clear();
            if (hasVis2) {
                getDistinctStaNames(this.oiFitsFile.getOiVis2(), distinct);
            }
            if (hasT3) {
                getDistinctStaNames(this.oiFitsFile.getOiT3(), distinct);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, "-", "_");
            }

            // Add unique dateObs:
            final GetOIDataString dateObsOperator = new GetOIDataString() {
                public String getString(final OIData oiData) {
                    return oiData.getDateObs();
                }
            };
            distinct.clear();
            if (hasVis2) {
                getDistinct(this.oiFitsFile.getOiVis2(), distinct, dateObsOperator);
            }
            if (hasT3) {
                getDistinct(this.oiFitsFile.getOiT3(), distinct, dateObsOperator);
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

        this.xyPlotV2 = createScientificScatterPlot("UV radius (M\u03BB)", "V²", usePlotCrossHairSupport);
        this.xyPlotV2.setNoDataMessage("No square visiblity data (OI_VIS2)");

        this.aJMMCV2 = ChartUtils.createXYTextAnnotation(Constants.JMMC_ANNOTATION, 0, 0);
        this.aJMMCV2.setTextAnchor(TextAnchor.BOTTOM_RIGHT);
        this.aJMMCV2.setPaint(Color.DARK_GRAY);
        this.xyPlotV2.getRenderer().addAnnotation(this.aJMMCV2, Layer.BACKGROUND);

        this.xyPlotT3 = createScientificScatterPlot("UV radius (M\u03BB)", "Closure phase (deg)", usePlotCrossHairSupport);
        this.xyPlotT3.setNoDataMessage("No closure phase data (OI_T3)");

        this.aJMMCT3 = ChartUtils.createXYTextAnnotation(Constants.JMMC_ANNOTATION, 0, 0);
        this.aJMMCT3.setTextAnchor(TextAnchor.BOTTOM_RIGHT);
        this.aJMMCT3.setPaint(Color.DARK_GRAY);
        this.xyPlotT3.getRenderer().addAnnotation(this.aJMMCT3, Layer.BACKGROUND);

        final ValueAxis domainAxis = this.xyPlotV2.getDomainAxis();

        // create chart and add listener :
        this.combinedXYPlot = new CombinedDomainXYPlot(domainAxis);
        this.combinedXYPlot.setGap(10.0D);
        this.combinedXYPlot.setOrientation(PlotOrientation.VERTICAL);
        /*        
         this.combinedXYPlot.add(this.xyPlotV2, 1);
         this.combinedXYPlot.add(this.xyPlotT3, 1);
         */
        this.plotMapping.put(this.xyPlotV2, Integer.valueOf(1));
        this.plotIndexMapping.put(Integer.valueOf(1), this.xyPlotV2);
        this.plotMapping.put(this.xyPlotT3, Integer.valueOf(2));
        this.plotIndexMapping.put(Integer.valueOf(2), this.xyPlotT3);

        configureCrosshair(this.combinedXYPlot, usePlotCrossHairSupport);

        this.chart = ChartUtils.createChart(null, this.combinedXYPlot, false);
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

            for (Integer plotIndex : this.plotMapping.values()) {
                crosshairOverlay.addDomainCrosshair(plotIndex, createCrosshair());
                crosshairOverlay.addRangeCrosshair(plotIndex, createCrosshair());
            }

            this.chartPanel.addOverlay(crosshairOverlay);
        }

        if (useSelectionSupport || !usePlotCrossHairSupport) {
            this.chartPanel.addChartMouseListener(this);
        }

        applyColorTheme();

        this.add(this.chartPanel);
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

        final XYPlot plot = ChartUtils.createScatterPlot(null, xAxisLabel, yAxisLabel, null, PlotOrientation.VERTICAL, false, false, false);

        // enlarge right margin to have last displayed value:
        plot.setInsets(new RectangleInsets(2d, 10d, 2d, 20d));

        configureCrosshair(plot, usePlotCrossHairSupport);

        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // use custom units :
        plot.getRangeAxis().setStandardTickUnits(ChartUtils.createScientificTickUnits());
        plot.getDomainAxis().setStandardTickUnits(ChartUtils.createScientificTickUnits());

        final FastXYErrorRenderer renderer = (FastXYErrorRenderer) plot.getRenderer();

        // only display Y error:
        renderer.setDrawXError(false);

        // force to use the base shape
        renderer.setAutoPopulateSeriesShape(false);

        // reset colors :
        renderer.setAutoPopulateSeriesPaint(false);
        renderer.clearSeriesPaints(false);

        // define deprecated methods to set renderer options for ALL series (performance):
        renderer.setDrawOutlines(false);
        renderer.setShapesVisible(true);
        renderer.setShapesFilled(true);
        renderer.setLinesVisible(false);
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

            logger.warn("findDataPoint: time = {} ms.", 1e-6d * (System.nanoTime() - startTime));

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

            logger.warn("findDataPoints: time = {} ms.", 1e-6d * (System.nanoTime() - startTime));
            if (false) {
                logger.warn("Matching points: {}", points);
            }
        }
        return points;
    }

    /**
     * Plot the generated file synchronously.
     * This code must be executed by the Swing Event Dispatcher thread (EDT)
     * @param target target unique identifier to use
     * @param oiFitsFile OIFits file to use
     */
    public void plot(final TargetUID target, final OIFitsFile oiFitsFile) {
        logger.debug("plot : {}", oiFitsFile);

        // memorize plot data:
        this.target = target;
        this.oiFitsFile = oiFitsFile;

        // refresh the plot :
        logger.debug("plot : refresh");

        final long start = System.nanoTime();

        this.updatePlot();

        if (logger.isInfoEnabled()) {
            logger.info("plot : duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }
    }

    /**
     * Reset plot
     */
    public void resetPlot() {
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

            this.resetOverlays();

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
            this.combinedXYPlot.remove((XYPlot) plot);
        }

    }

    /**
     * Refresh the plot using chart data.
     * This code is executed by the Swing Event Dispatcher thread (EDT)
     */
    private void updatePlot() {

        if (this.oiFitsFile == null) {
            resetPlot();
            return;
        }
        this.hasData = false;

        // disable chart & plot notifications:
        this.chart.setNotify(false);
        this.xyPlotV2.setNotify(false);
        this.xyPlotT3.setNotify(false);

        try {
            // title :
            ChartUtils.clearTextSubTitle(this.chart);

            final boolean hasVis2 = this.oiFitsFile.hasOiVis2();
            final boolean hasT3 = this.oiFitsFile.hasOiT3();

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
                getDistinct(this.oiFitsFile.getOiVis2(), distinct, arrNameOperator);
            }
            if (hasT3) {
                getDistinct(this.oiFitsFile.getOiT3(), distinct, arrNameOperator);
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
                getDistinct(this.oiFitsFile.getOiVis2(), distinct, insNameOperator);
            }
            if (hasT3) {
                getDistinct(this.oiFitsFile.getOiT3(), distinct, insNameOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, " ", " / ");
            }

            sb.append(" ");

            // Add wavelength ranges:
            distinct.clear();
            if (hasVis2) {
                getDistinctWaveLengthRange(this.oiFitsFile.getOiVis2(), distinct);
            }
            if (hasT3) {
                getDistinctWaveLengthRange(this.oiFitsFile.getOiT3(), distinct);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, " ", " / ");
            }

            sb.append(" - ");

            // Add unique baselines:
            distinct.clear();
            if (hasVis2) {
                getDistinctStaNames(this.oiFitsFile.getOiVis2(), distinct);
            }
            if (hasT3) {
                getDistinctStaNames(this.oiFitsFile.getOiT3(), distinct);
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
                getDistinct(this.oiFitsFile.getOiVis2(), distinct, dateObsOperator);
            }
            if (hasT3) {
                getDistinct(this.oiFitsFile.getOiT3(), distinct, dateObsOperator);
            }
            if (!distinct.isEmpty()) {
                toString(distinct, sb, " ", " / ");
            }

            sb.append(" - Source: ").append(this.target.getTarget());

            ChartUtils.addSubtitle(this.chart, sb.toString());

            // change the scaling factor ?
            setPlotXScalingFactor(MEGA_LAMBDA_SCALE);

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

        removeAllSubPlots();

        boolean showV2 = false;
        boolean showT3 = false;

        BoundedNumberAxis axis;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;

        if (this.oiFitsFile.hasOiVis2()) {
            this.xyPlotV2.setDataset(new DefaultIntervalXYDataset());

            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            Rectangle2D.Double dataArea;

            for (OIVis2 vis2 : this.oiFitsFile.getOiVis2()) {

                dataArea = updatePlot(this.xyPlotV2, vis2,
                        vis2.getSpatialFreq(), null,
                        vis2.getVis2Data(), vis2.getVis2Err(),
                        vis2.getTargetId(), vis2.getFlag());

                if (dataArea != null) {
                    showV2 = true;

                    // combine X range:
                    minX = Math.min(minX, dataArea.getX());
                    maxX = Math.max(maxX, dataArea.getMaxX());

                    // combine Y range:
                    minY = Math.min(minY, dataArea.getY());
                    maxY = Math.max(maxY, dataArea.getMaxY());
                }
            }

            if (showV2) {
                // TODO: fix boundaries according to standard data boundaries (VIS between 0-1 ...)

                // Add margin:
                if (!USE_LOG_SCALE) {
                    final double marginY = (maxY - minY) * MARGIN_PERCENTS;
                    minY -= marginY;
                    maxY += marginY;
                }

                // Update Y axis:

                if (this.xyPlotV2.getRangeAxis() instanceof BoundedNumberAxis) {
                    axis = (BoundedNumberAxis) this.xyPlotV2.getRangeAxis();
                    axis.setBounds(new Range(minY, maxY));
                    axis.setRange(minY, maxY);
                }

                if (USE_LOG_SCALE) {
                    // test logarithmic axis:
                    final LogarithmicAxis logAxis = new LogarithmicAxis("log(V²)");
                    logAxis.setExpTickLabelsFlag(true);
                    //      logAxis.setAllowNegativesFlag(true);
                    //      logAxis.setStrictValuesFlag(false);
                    logAxis.setAutoRangeNextLogFlag(true);

                    logger.debug("logAxis range: [{} - {}]", minY, maxY);

                    logAxis.setRange(minY, maxY);

                    this.xyPlotV2.setRangeAxis(logAxis);
                }
            }
        }

        if (showV2) {
            this.combinedXYPlot.add(this.xyPlotV2, 1);
        } else {
            // reset Vis2 dataset:
            this.xyPlotV2.setDataset(null);
        }

        if (this.oiFitsFile.hasOiT3()) {
            this.xyPlotT3.setDataset(new DefaultIntervalXYDataset());

            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            Rectangle2D.Double dataArea;

            for (OIT3 t3 : this.oiFitsFile.getOiT3()) {

                dataArea = updatePlot(this.xyPlotT3, t3,
                        t3.getSpatialFreq(), null,
                        t3.getT3Phi(), t3.getT3PhiErr(),
                        t3.getTargetId(), t3.getFlag());

                if (dataArea != null) {
                    showT3 = true;

                    // combine X range:
                    minX = Math.min(minX, dataArea.getX());
                    maxX = Math.max(maxX, dataArea.getMaxX());

                    // combine Y range:
                    minY = Math.min(minY, dataArea.getY());
                    maxY = Math.max(maxY, dataArea.getMaxY());
                }
            }

            if (showT3) {
                // TODO: fix boundaries according to standard data boundaries (T3 between -180-180 ...)

                // Add margin:
                if (!USE_LOG_SCALE) {
                    final double marginY = (maxY - minY) * MARGIN_PERCENTS;
                    minY -= marginY;
                    maxY += marginY;
                }

                // Update Y axis:

                if (this.xyPlotT3.getRangeAxis() instanceof BoundedNumberAxis) {
                    axis = (BoundedNumberAxis) this.xyPlotT3.getRangeAxis();
                    axis.setBounds(new Range(minY, maxY));
                    axis.setRange(minY, maxY);
                }
            }
        }

        if (showT3) {
            this.combinedXYPlot.add(this.xyPlotT3, 1);
        } else {
            // reset T3 dataset:
            this.xyPlotT3.setDataset(null);
        }

        if (!showV2 && !showT3) {
            return false;
        }

        // TODO: fix boundaries according to standard data boundaries (spatial freq >= 0)

        // Add margin:
        final double marginX = (maxX - minX) * MARGIN_PERCENTS;
        minX -= marginX;
        maxX += marginX;

        // fix minX to include zero spatial frequency:
        if (minX > 0d) {
            minX = 0d;
        }

        axis = (BoundedNumberAxis) this.combinedXYPlot.getDomainAxis();
        axis.setBounds(new Range(minX, maxX));
        axis.setRange(minX, maxX);

        return true;
    }

    /**
     * Update the plot (dataset, axis ranges ...) using the given OIData table
     * TODO use column names and virtual columns (spatial ...)
     * @param plot XYPlot to update (dataset, renderer, axes)
     * @param table OIData table to use as data source
     * @param xData data to plot on X axis
     * @param xDataErr data error to plot on X axis (may be null)
     * @param yData data to plot on Y axis
     * @param yDataErr data error to plot on Y axis (may be null)
     * @param targetIds targetId column data to optionally filter targets
     * @param flags OIData flags (may be null)
     * @return rectangle giving data area or null if no data
     */
    private Rectangle2D.Double updatePlot(final XYPlot plot, final OIData table,
                                          final double[][] xData, final double[][] xDataErr,
                                          final double[][] yData, final double[][] yDataErr,
                                          final short[] targetIds, final boolean[][] flags) {

        // TODO: externalize flags handling:
        final boolean skipFlaggedData = true;

        final DefaultIntervalXYDataset dataset = (DefaultIntervalXYDataset) plot.getDataset();

        final int nbSeries = dataset.getSeriesCount();

        final int nRows = table.getNbRows();
        final int nWaves = table.getNWave();

        final boolean hasErrX = (xDataErr != null);
        final boolean hasErrY = (yDataErr != null);
        final boolean hasFlag = (flags != null);
        final boolean hasTargetId = (targetIds != null);

        // flag to check flags on each data:
        final boolean checkFlaggedData = hasFlag && skipFlaggedData;

        // flag to check targetId on each data row:
        final boolean checkTargetId = !table.hasSingleTarget() && hasTargetId;

        final Short matchTargetId;
        if (checkTargetId) {
            matchTargetId = table.getTargetId(this.target.getTarget());

            logger.warn("matchTargetId: {}", matchTargetId);
        } else {
            matchTargetId = null;
        }

        logger.warn("checkFlaggedData: {}", checkFlaggedData);


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

        // try to fill with squared visibilities:

        // Use DefaultIntervalXYDataset for performance (arrays XY intervals)
        boolean hasData = false;
        boolean hasDataFlag = false;
        boolean hasDataErrorX = false;
        boolean hasDataErrorY = false;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double[] xValue, xLower, xUpper, yValue, yLower, yUpper;

        double x, xErr, y, yErr;

        int nSkip = 0;

        for (int nSerie, j = 0; j < nWaves; j++) {

            // 1 color per spectral channel (i.e. per Serie) :
            xValue = new double[nRows];
            xLower = new double[nRows];
            xUpper = new double[nRows];
            yValue = new double[nRows];
            yLower = new double[nRows];
            yUpper = new double[nRows];

            for (int i = 0; i < nRows; i++) {

                if (checkTargetId) {
                    if (!matchTargetId.equals(targetIds[i])) {
                        // data row does not coorespond to current target so skip it:
                        nSkip++;
                        continue;
                    }
                }

                if (flags[i][j]) {
                    if (skipFlaggedData) {
                        // data point is flagged so skip it:
                        nSkip++;
                        continue;
                    } else {
                        hasDataFlag = true;
                    }
                }

                // Process Y value:
                y = yData[i][j];

                if (USE_LOG_SCALE && y < 0d) {
                    // keep only positive data:
                    y = Double.NaN;
                }

                if (Double.isNaN(y)) {
                    xValue[i] = Double.NaN;
                    xLower[i] = Double.NaN;
                    xUpper[i] = Double.NaN;
                    yValue[i] = Double.NaN;
                    yLower[i] = Double.NaN;
                    yUpper[i] = Double.NaN;
                } else {

                    // Process X value:
                    x = xData[i][j];

                    if (Double.isNaN(x)) {
                        xValue[i] = Double.NaN;
                        xLower[i] = Double.NaN;
                        xUpper[i] = Double.NaN;
                        yValue[i] = Double.NaN;
                        yLower[i] = Double.NaN;
                        yUpper[i] = Double.NaN;
                    } else {
                        hasData = true;

                        // Scale X value:
                        x = toPlotXScale(x);

                        // Process X / Y Errors:
                        yErr = (hasErrY) ? yDataErr[i][j] : Double.NaN;
                        xErr = (hasErrX) ? xDataErr[i][j] : Double.NaN;

                        // Define Y data:
                        if (Double.isNaN(yErr)) {
                            yValue[i] = y;
                            yLower[i] = Double.NaN;
                            yUpper[i] = Double.NaN;

                            // update Y boundaries:
                            if (y < minY) {
                                minY = y;
                            }
                            if (y > maxY) {
                                maxY = y;
                            }

                        } else {
                            hasDataErrorY = true;

                            yValue[i] = y;
                            // USE_LOG_SCALE: check if y - err < 0:
                            yLower[i] = (USE_LOG_SCALE && (y - yErr) < 0d) ? 0d : (y - yErr);
                            yUpper[i] = y + yErr;

                            // update Y boundaries including error:
                            if (yLower[i] < minY) {
                                minY = yLower[i];
                            }
                            if (yUpper[i] > maxY) {
                                maxY = yUpper[i];
                            }
                        }

                        // Define X data:
                        if (Double.isNaN(xErr)) {
                            xValue[i] = x;
                            xLower[i] = Double.NaN;
                            xUpper[i] = Double.NaN;

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
                            xErr = toPlotXScale(xErr);

                            xValue[i] = x;
                            xLower[i] = x - xErr;
                            xUpper[i] = x + xErr;

                            // update X boundaries including error:
                            if (xLower[i] < minX) {
                                minX = xLower[i];
                            }
                            if (xUpper[i] > maxX) {
                                maxX = xUpper[i];
                            }
                        }
                    }
                }
            }

            nSerie = nbSeries + j;

            // TODO: add oiTable, i (row), j (nWave) in dataset:
            dataset.addSeries("OIDATA W" + nSerie, new double[][]{xValue, xLower, xUpper, yValue, yLower, yUpper});

            renderer.setSeriesPaint(nSerie, colors[j], false);
        }

        if (nSkip > 0) {
            logger.warn("nSkip: {}", nSkip);
        }

        if (!hasData) {
            return null;
        }

        // TODO: adjust renderer settings per Serie !!

        // set shape depending on error (triangle or square):
        final Shape shape = getPointShape(hasDataFlag);

        // enable/disable X error rendering (performance):
        renderer.setDrawXError(hasDataErrorX);

        // enable/disable Y error rendering (performance):
        renderer.setDrawYError(hasDataErrorY);

        // use deprecated method but defines shape once for ALL series (performance):
        renderer.setShape(shape);

        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
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
                    logger.debug("Drawing chart time = {} ms.", 1e-6d * (System.nanoTime() - this.chartDrawStartTime));
                    this.chartDrawStartTime = 0l;
                    break;
                default:
            }
        }

        // DEBUG:
        switch (event.getType()) {
            case ChartProgressEvent.DRAWING_STARTED:
                this.chartDrawStartTime = System.nanoTime();
                break;
            case ChartProgressEvent.DRAWING_FINISHED:
                logger.warn("Drawing chart time = {} ms.", 1e-6d * (System.nanoTime() - this.chartDrawStartTime));
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

        this.xyPlotV2.setBackgroundPaint(Color.WHITE);
        this.xyPlotV2.setDomainGridlinePaint(Color.LIGHT_GRAY);
        this.xyPlotV2.setRangeGridlinePaint(Color.LIGHT_GRAY);

        this.xyPlotT3.setBackgroundPaint(Color.WHITE);
        this.xyPlotT3.setDomainGridlinePaint(Color.LIGHT_GRAY);
        this.xyPlotT3.setRangeGridlinePaint(Color.LIGHT_GRAY);
    }

    /**
     * Define the scaling factor on X data
     * @param plotXScalingFactor new value
     */
    private void setPlotXScalingFactor(final double plotXScalingFactor) {
        this.plotXScalingFactor = plotXScalingFactor;
    }

    /**
     * Convert the given x value to the plot scale
     * @param value x value
     * @return scaled x value in the plot unit
     */
    private double toPlotXScale(final double value) {
        return this.plotXScalingFactor * value;
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
                logger.info("getDistinct: {}", value);

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

                logger.info("staNames : {}", staNames);

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
                logger.info("wlen range : {}", wlenRange);

                set.add(wlenRange);
            }
        }
    }

    private static void toString(final Set<String> set, final StringBuilder sb, final String internalSeparator, final String separator) {
        for (String v : set) {
            sb.append(v.replaceAll("\\s", internalSeparator)).append(separator);
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
}
