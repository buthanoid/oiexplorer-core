package fr.jmmc.oiexplorer.core.model.oi;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import fr.jmmc.oiexplorer.core.model.OIBase;

/**
 * 
 *                 This type describes an OIData table unique identifier among the OIDataCollection
 *             
 * 
 * <p>Java class for TableUID complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TableUID">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="file" type="{http://www.w3.org/2001/XMLSchema}IDREF"/>
 *         &lt;element name="extName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="extNb" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TableUID", propOrder = {
    "file",
    "extName",
    "extNb"
})
public class TableUID
        extends OIBase {

    @XmlElement(required = true, type = Object.class)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected OIDataFile file;
    protected String extName;
    protected Integer extNb;

    /**
     * Gets the value of the file property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public OIDataFile getFile() {
        return file;
    }

    /**
     * Sets the value of the file property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setFile(OIDataFile value) {
        this.file = value;
    }

    /**
     * Gets the value of the extName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExtName() {
        return extName;
    }

    /**
     * Sets the value of the extName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExtName(String value) {
        this.extName = value;
    }

    /**
     * Gets the value of the extNb property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getExtNb() {
        return extNb;
    }

    /**
     * Sets the value of the extNb property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setExtNb(Integer value) {
        this.extNb = value;
    }

//--simple--preserve
    /**
     * Constructor for JAXB
     */
    public TableUID() {
    }

    /**
     * Constructor
     * @param file oidata file Identifier
     */
    public TableUID(final OIDataFile file) {
        this(file, null, null);
    }

    /**
     * Constructor
     * @param file oidata file Identifier
     * @param extName oidata table name
     * @param extNb oidata table number
     */
    public TableUID(final OIDataFile file, final String extName, final Integer extNb) {
        this.file = file;
        this.extName = extName;
        this.extNb = extNb;
    }

    @Override
    public String toString() {
        return "TableUID[" + file + ((extName != null) ? ' ' + extName + '#' + extNb : "") + ']';
    }
//--simple--preserve
}
