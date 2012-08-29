
package fr.jmmc.oiexplorer.core.model.plot;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 This type describes a plot axis.
 *             
 * 
 * <p>Java class for Axis complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Axis">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="drawLine" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="logScale" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="includeZero" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="plotError" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="range" type="{http://www.jmmc.fr/oiexplorer-core-plot-definition/0.1}Range" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Axis", propOrder = {
    "name",
    "drawLine",
    "logScale",
    "includeZero",
    "plotError",
    "range"
})
public class Axis {

    @XmlElement(required = true)
    protected String name;
    protected boolean drawLine;
    protected boolean logScale;
    protected boolean includeZero;
    protected Boolean plotError;
    protected Range range;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the drawLine property.
     * 
     */
    public boolean isDrawLine() {
        return drawLine;
    }

    /**
     * Sets the value of the drawLine property.
     * 
     */
    public void setDrawLine(boolean value) {
        this.drawLine = value;
    }

    /**
     * Gets the value of the logScale property.
     * 
     */
    public boolean isLogScale() {
        return logScale;
    }

    /**
     * Sets the value of the logScale property.
     * 
     */
    public void setLogScale(boolean value) {
        this.logScale = value;
    }

    /**
     * Gets the value of the includeZero property.
     * 
     */
    public boolean isIncludeZero() {
        return includeZero;
    }

    /**
     * Sets the value of the includeZero property.
     * 
     */
    public void setIncludeZero(boolean value) {
        this.includeZero = value;
    }

    /**
     * Gets the value of the plotError property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isPlotError() {
        return plotError;
    }

    /**
     * Sets the value of the plotError property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPlotError(Boolean value) {
        this.plotError = value;
    }

    /**
     * Gets the value of the range property.
     * 
     * @return
     *     possible object is
     *     {@link Range }
     *     
     */
    public Range getRange() {
        return range;
    }

    /**
     * Sets the value of the range property.
     * 
     * @param value
     *     allowed object is
     *     {@link Range }
     *     
     */
    public void setRange(Range value) {
        this.range = value;
    }

}
