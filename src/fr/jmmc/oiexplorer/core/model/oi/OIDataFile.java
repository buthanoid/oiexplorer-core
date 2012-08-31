
package fr.jmmc.oiexplorer.core.model.oi;

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
 *                 This type describes an oidata file.
 *             
 * 
 * <p>Java class for OIDataFile complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OIDataFile">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}ID"/>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="file" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="checksum" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OIDataFile", propOrder = {
    "name",
    "description",
    "file",
    "checksum"
})
public class OIDataFile
    extends OIBase
{

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String name;
    protected String description;
    @XmlElement(required = true)
    protected String file;
    protected long checksum;

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
     * Gets the value of the file property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFile() {
        return file;
    }

    /**
     * Sets the value of the file property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFile(String value) {
        this.file = value;
    }

    /**
     * Gets the value of the checksum property.
     * 
     */
    public long getChecksum() {
        return checksum;
    }

    /**
     * Sets the value of the checksum property.
     * 
     */
    public void setChecksum(long value) {
        this.checksum = value;
    }
    
//--simple--preserve

  /** loaded oiFitsFile structure (read only) */
  @javax.xml.bind.annotation.XmlTransient
  private fr.jmmc.oitools.model.OIFitsFile oiFitsFile = null;

  /**
   * Return the loaded oiFitsFile structure
   * @return loaded oiFitsFile structure
   */
  public final fr.jmmc.oitools.model.OIFitsFile getOIFitsFile() {
    return this.oiFitsFile;
  }

  /**
   * Return the loaded oiFitsFile structure
   * @param loaded oiFitsFile structure
   */
  public final void setOIFitsFile(final fr.jmmc.oitools.model.OIFitsFile oiFitsFile) {
    this.oiFitsFile = oiFitsFile;
  }
    
//--simple--preserve

}
