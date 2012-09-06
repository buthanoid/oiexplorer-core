
package fr.jmmc.oiexplorer.core.model.oi;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 This type describes a generic view instance of a subset.
 *             
 * 
 * <p>Java class for View complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="View">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.jmmc.fr/oiexplorer-base/0.1}Identifiable">
 *       &lt;sequence>
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
@XmlType(name = "View", propOrder = {
    "type",
    "subsetDefinition"
})
@XmlSeeAlso({
    Plot.class
})
public class View
    extends Identifiable
{

    @XmlElement(required = true)
    protected String type;
    @XmlElement(required = true, type = Object.class)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected SubsetDefinition subsetDefinition;

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
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