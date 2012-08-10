/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.util.XmlFactory;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OITable;
import fr.jmmc.oitools.model.XmlOutputVisitor;

import java.io.IOException;
import java.io.StringReader;
import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel presents a simple HTML representation of an OIFits file or OITable
 * @author bourgesl
 */
public final class OIFitsHtmlPanel extends javax.swing.JPanel {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(OIFitsHtmlPanel.class.getName());
    /** XSLT file path */
    private final static String XSLT_FILE = "fr/jmmc/oiexplorer/core/resource/oiview.xsl";
    /** empty document */
    private static Document emptyDocument = null;

    /* member */
    /** flag to know if the last document was empty to avoid JEditorPane refresh calls */
    private boolean isEmpty = false;
    /** internal XmlOutputVisitor instance */
    private final XmlOutputVisitor xmlSerializer;

    /** Creates new form OIFitsPanel */
    public OIFitsHtmlPanel() {
        initComponents();

        // use formatter and verbose output:
        this.xmlSerializer = new XmlOutputVisitor(true, true);
    }

    /**
     * Update panel with the given OIFits structure
     * @param oiFitsFile OIFits structure
     */
    public void updateOIFits(final OIFitsFile oiFitsFile) {
        String xmlDesc = null;

        if (oiFitsFile != null) {
            final long start = System.nanoTime();

            oiFitsFile.accept(this.xmlSerializer);

            xmlDesc = xmlSerializer.toString();

            if (logger.isDebugEnabled()) {
                logger.debug("XmlOutputVisitor: {} ms.", 1e-6d * (System.nanoTime() - start));
            }
        }

        update(xmlDesc);
    }

    /**
     * Update panel with the given OITable structure
     * @param oiTable OITable structure
     */
    public void updateOIFits(final OITable oiTable) {
        String xmlDesc = null;

        if (oiTable != null) {
            final long start = System.nanoTime();

            oiTable.accept(this.xmlSerializer);

            xmlDesc = xmlSerializer.toString();

            if (logger.isDebugEnabled()) {
                logger.debug("XmlOutputVisitor: {} ms.", 1e-6d * (System.nanoTime() - start));
            }
        }

        update(xmlDesc);
    }

    /**
     * Update document with the given xml representation of an OIFits file or OITable
     * @param xmlDesc xml representation of an OIFits file or OITable
     */
    public void update(final String xmlDesc) {
        String document = "";

        if (xmlDesc != null) {
            final long start = System.nanoTime();

            // use an XSLT to transform the XML document to an HTML representation :
            document = XmlFactory.transform(xmlDesc, XSLT_FILE);

            if (logger.isDebugEnabled()) {
                logger.debug("transform: {} ms.", 1e-6d * (System.nanoTime() - start));
            }
        }

        if (document.length() > 0) {

            final long start = System.nanoTime();
            try {
                this.jOutputPane.read(new StringReader(document), null);
                this.jOutputPane.setCaretPosition(0);
            } catch (IOException ioe) {
                logger.error("IO exception : ", ioe);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("html: {} ms.", 1e-6d * (System.nanoTime() - start));
            }

            this.isEmpty = false;
        } else {
            if (!isEmpty) {
                // reset content when the observation changed :
                this.jOutputPane.setDocument(emptyDocument);
                this.isEmpty = true;
            }
        }
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

        jScrollPane = new javax.swing.JScrollPane();
        jOutputPane = createEditorPane();

        setLayout(new java.awt.BorderLayout());

        jScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setPreferredSize(new java.awt.Dimension(300, 300));
        jScrollPane.setViewportView(jOutputPane);

        add(jScrollPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane jOutputPane;
    private javax.swing.JScrollPane jScrollPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Create an html editor pane
     * @return html editor pane
     */
    private static JEditorPane createEditorPane() {
        final JEditorPane pane = new JEditorPane();

        // add a HTMLEditorKit to the editor pane
        pane.setEditorKit(new HTMLEditorKit());

        pane.setContentType("text/html");
        pane.setEditable(false);

        emptyDocument = pane.getEditorKit().createDefaultDocument();

        return pane;
    }

    /**
     * Return the flag to enable/disable the number formatter
     * @return flag to enable/disable the number formatter
     */
    public boolean isFormat() {
        return this.xmlSerializer.isFormat();
    }

    /**
     * Define the flag to enable/disable the number formatter
     * @param format flag to enable/disable the number formatter
     */
    public void setFormat(final boolean format) {
        this.xmlSerializer.setFormat(format);
    }

    /**
     * Return the flag to enable/disable the verbose output
     * @return flag to enable/disable the verbose output
     */
    public boolean isVerbose() {
        return this.xmlSerializer.isVerbose();
    }

    /**
     * Define the flag to enable/disable the verbose output
     * @param verbose flag to enable/disable the verbose output
     */
    public void setVerbose(final boolean verbose) {
        this.xmlSerializer.setVerbose(verbose);
    }
}
