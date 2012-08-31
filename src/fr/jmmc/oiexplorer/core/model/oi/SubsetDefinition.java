
package fr.jmmc.oiexplorer.core.model.oi;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import fr.jmmc.oiexplorer.core.model.OIBase;


/**
 * 
 *                 This type describes a subset definition.
 *             
 * 
 * <p>Java class for SubsetDefinition complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SubsetDefinition">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}ID"/>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="target" type="{http://www.jmmc.fr/oiexplorer-data-collection/0.1}TargetUID"/>
 *         &lt;element name="table" type="{http://www.jmmc.fr/oiexplorer-data-collection/0.1}TableUID" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="filter" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubsetDefinition", propOrder = {
    "name",
    "description",
    "target",
    "tables",
    "filters"
})
public class SubsetDefinition
    extends OIBase
{

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String name;
    protected String description;
    @XmlElement(required = true)
    protected TargetUID target;
    @XmlElement(name = "table")
    protected List<TableUID> tables;
    @XmlElement(name = "filter")
    protected List<String> filters;

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
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the target property.
     * 
     * @return
     *     possible object is
     *     {@link TargetUID }
     *     
     */
    public TargetUID getTarget() {
        return target;
    }

    /**
     * Sets the value of the target property.
     * 
     * @param value
     *     allowed object is
     *     {@link TargetUID }
     *     
     */
    public void setTarget(TargetUID value) {
        this.target = value;
    }

    /**
     * Gets the value of the tables property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tables property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTables().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TableUID }
     * 
     * 
     */
    public List<TableUID> getTables() {
        if (tables == null) {
            tables = new ArrayList<TableUID>();
        }
        return this.tables;
    }

    /**
     * Gets the value of the filters property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the filters property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFilters().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getFilters() {
        if (filters == null) {
            filters = new ArrayList<String>();
        }
        return this.filters;
    }
    
//--simple--preserve

  /** subset oiFitsFile structure (read only) */
  @javax.xml.bind.annotation.XmlTransient
  private fr.jmmc.oitools.model.OIFitsFile oiFitsSubset = null;

  /**
   * Return the subset oiFitsFile structure
   * @return subset oiFitsFile structure
   */
  public final fr.jmmc.oitools.model.OIFitsFile getOIFitsSubset() {
    return this.oiFitsSubset;
  }

  /**
   * Return the subset oiFitsFile structure
   * @param oiFitsSubset subset oiFitsFile structure
   */
  public final void setOIFitsSubset(final fr.jmmc.oitools.model.OIFitsFile oiFitsSubset) {
    this.oiFitsSubset = oiFitsSubset;
  }
    
//--simple--preserve

}
