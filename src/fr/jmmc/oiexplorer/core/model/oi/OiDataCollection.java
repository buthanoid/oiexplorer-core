
package fr.jmmc.oiexplorer.core.model.oi;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;


/**
 * 
 *                 This type describes a collection of oidata ressources.
 *             
 * 
 * <p>Java class for OIDataCollection complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OIDataCollection">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="file" type="{http://www.jmmc.fr/oiexplorer-data-collection/0.1}OIDataFile" maxOccurs="unbounded"/>
 *         &lt;element name="plotDefinition" type="{http://www.jmmc.fr/oiexplorer-core-plot-definition/0.1}PlotDefinition" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OIDataCollection", propOrder = {
    "files",
    "plotDefinitions"
})
@XmlRootElement(name = "oiDataCollection")
public class OiDataCollection {

    @XmlElement(name = "file", required = true)
    protected List<OIDataFile> files;
    @XmlElement(name = "plotDefinition")
    protected List<PlotDefinition> plotDefinitions;

    /**
     * Gets the value of the files property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the files property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFiles().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link OIDataFile }
     * 
     * 
     */
    public List<OIDataFile> getFiles() {
        if (files == null) {
            files = new ArrayList<OIDataFile>();
        }
        return this.files;
    }

    /**
     * Gets the value of the plotDefinitions property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the plotDefinitions property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPlotDefinitions().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PlotDefinition }
     * 
     * 
     */
    public List<PlotDefinition> getPlotDefinitions() {
        if (plotDefinitions == null) {
            plotDefinitions = new ArrayList<PlotDefinition>();
        }
        return this.plotDefinitions;
    }

}
