
package fr.jmmc.oiexplorer.core.model.oi;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import fr.jmmc.oiexplorer.core.model.OIBase;


/**
 * 
 *                 This type describes a subset filter.
 *             
 * 
 * <p>Java class for SubsetFilter complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SubsetFilter"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="targetUID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="insModeUID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nightID" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="table" type="{http://www.jmmc.fr/oiexplorer-data-collection/0.1}TableUID" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubsetFilter", propOrder = {
    "targetUID",
    "insModeUID",
    "nightID",
    "tables"
})
public class SubsetFilter
    extends OIBase
{

    protected String targetUID;
    protected String insModeUID;
    protected Integer nightID;
    @XmlElement(name = "table")
    protected List<TableUID> tables;

    /**
     * Gets the value of the targetUID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTargetUID() {
        return targetUID;
    }

    /**
     * Sets the value of the targetUID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTargetUID(String value) {
        this.targetUID = value;
    }

    /**
     * Gets the value of the insModeUID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInsModeUID() {
        return insModeUID;
    }

    /**
     * Sets the value of the insModeUID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInsModeUID(String value) {
        this.insModeUID = value;
    }

    /**
     * Gets the value of the nightID property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNightID() {
        return nightID;
    }

    /**
     * Sets the value of the nightID property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNightID(Integer value) {
        this.nightID = value;
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
    
//--simple--preserve
    /**
     * Perform a deep-copy of the given other instance into this instance
     * 
     * Note: to be overriden in child class to perform deep-copy of class fields
     * @see OIBase#clone() 
     * 
     * @param other other instance
     */
    @Override
    public final void copy(final OIBase other) {
        final SubsetFilter filter = (SubsetFilter) other;

        // copy targetUID, insModeUID, nightID:
        this.targetUID = filter.getTargetUID();
        this.insModeUID = filter.getInsModeUID();
        this.nightID = filter.getNightID();

        // deep copy tables:
        this.tables = fr.jmmc.jmcs.util.ObjectUtils.deepCopyList(filter.tables);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        // identity check:
        if (this == obj) {
            return true;
        }
        final SubsetFilter other = (SubsetFilter) obj;
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.targetUID, other.targetUID)) {
            return false;
        }
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.insModeUID, other.insModeUID)) {
            return false;
        }
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.nightID, other.nightID)) {
            return false;
        }
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.tables, other.tables)) {
            return false;
        }
        return true;
    }

    /**
     * toString() implementation using string builder
     * @param sb string builder to append to
     */
    @Override
    public void toString(final StringBuilder sb, final boolean full) {
        super.toString(sb, full); // Identifiable

        if (full) {
            sb.append("{ targetUID='").append(this.targetUID).append('\'');
            sb.append(", insModeUID='").append(this.insModeUID).append('\'');
            sb.append(", nightID='").append(this.nightID).append('\'');

            sb.append(", tables=");
            fr.jmmc.jmcs.util.ObjectUtils.toString(sb, full, this.tables);
            sb.append('}');
        }
    }

    /**
     * returns a short String representation. used for logging for example
     *
     * @return a short string representation.
     */
    public String toShortString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(").append(targetUID == null ? "null-target" : targetUID);
        sb.append(",").append(insModeUID == null ? "null-insmode" : insModeUID);
        sb.append(",").append(nightID == null ? "null-night" : nightID);
        if (tables == null) {
            sb.append(",null-tables)");
        } else {
            sb.append(",[");
            for (int i = 0, s = tables.size(); i < s; i++) {
                sb.append(i == 0 ? "" : ",");
                tables.get(i).appendShortString(sb);
            }
            sb.append("])");
        }
        return sb.toString();
    }

    /**
     * Check bad references and update OIDataFile references
     * @param tableUIDs list of TableUID to process
     * @param mapIdOiDataFiles Map<ID, OIDataFile> index
     */
    protected static void updateOIDataFileReferences(final java.util.List<TableUID> tableUIDs,
                                                     final java.util.Map<String, OIDataFile> mapIdOiDataFiles) {
        if (tableUIDs != null) {
            for (final java.util.ListIterator<TableUID> it = tableUIDs.listIterator(); it.hasNext();) {
                final TableUID tableUID = it.next();
                final OIDataFile prev = tableUID.getFile();

                final OIDataFile updated = (mapIdOiDataFiles != null) ? mapIdOiDataFiles.get(prev.getId()) : null;
                if (updated != null) {
                    if (updated != prev) {
                        tableUID.setFile(updated);
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Removing missing OIDataFile reference: {}", prev.getId());
                    }
                    it.remove();
                }
            }
        }
    }
//--simple--preserve

}
