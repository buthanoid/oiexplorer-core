
package fr.jmmc.oiexplorer.core.model.oi;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;


/**
 * 
 *                 This type describes a plot instance.
 *             
 * 
 * <p>Java class for Plot complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Plot">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.jmmc.fr/oiexplorer-base/0.1}Identifiable">
 *       &lt;sequence>
 *         &lt;element name="plotDefinition" type="{http://www.w3.org/2001/XMLSchema}IDREF"/>
 *         &lt;element name="subsetDefinition" type="{http://www.w3.org/2001/XMLSchema}IDREF"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Plot", propOrder = {
    "plotDefinition",
    "subsetDefinition"
})
public class Plot
    extends Identifiable
{

    @XmlElement(required = true, type = Object.class)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected PlotDefinition plotDefinition;
    @XmlElement(required = true, type = Object.class)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected SubsetDefinition subsetDefinition;

    /**
     * Gets the value of the plotDefinition property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public PlotDefinition getPlotDefinition() {
        return plotDefinition;
    }

    /**
     * Sets the value of the plotDefinition property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setPlotDefinition(PlotDefinition value) {
        this.plotDefinition = value;
    }

    /**
     * Gets the value of the subsetDefinition property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public SubsetDefinition getSubsetDefinition() {
        return subsetDefinition;
    }

    /**
     * Sets the value of the subsetDefinition property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setSubsetDefinition(SubsetDefinition value) {
        this.subsetDefinition = value;
    }

}
