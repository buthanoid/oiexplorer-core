/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.oiexplorer.core.model.oi;

import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.oiexplorer.core.model.OIBase;
import fr.jmmc.oiexplorer.core.model.plot.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Describes a filter for one column in a OITable. Defines a list of accepted values (for string values), or a list of
 * accepted ranges (for numeric values). The actual filtering does not happen in this class.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GenericFilter", propOrder = {
    "columnName", "dataType", "acceptedValues", "acceptedRanges", "enabled"})
public class GenericFilter extends Identifiable {

    /**
     * Constant for string data.
     */
    public static final String DATATYPE_STRING = "STRING";

    /**
     * Constant for numeric data.
     */
    public static final String DATATYPE_NUMERIC = "NUMERIC";

    /**
     * Name of the column concerned by this filter. Example: OIFitsConstants.COLUMN_UCOORD
     */
    @XmlElement(required = true)
    private String columnName;

    /**
     * DataType of the column concerned by this filter. Must be either DATATYPE_STRING or DATATYPE_NUMERIC
     */
    @XmlElement(required = true)
    private String dataType;

    /**
     * List of accepted String values for the column. Only used when dataType is DATATYPE_STRING. When empty, it means
     * every value is accepted
     */
    @XmlElement(name = "acceptedValue")
    private List<String> acceptedValues;

    /**
     * List of accepted ranges of double values for the column. Only used when dataType is DATATYPE_NUMERIC. When empty,
     * it means every value is accepted
     */
    @XmlElement(name = "acceptedRange")
    private List<Range> acceptedRanges;

    /**
     * When true, the filter is used. If false, the filter is unused (but still exported in the XML)
     */
    @XmlElement(required = true)
    private boolean enabled;

    /**
     * @return the columnName
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * @return the dataType. Can be either DATATYPE_STRING or DATATYPE_NUMERIC
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * @return the acceptedValues. Use only if this.dataType is DATATYPE_STRING
     */
    public List<String> getAcceptedValues() {
        return acceptedValues;
    }

    /**
     * @return the acceptedRanges. Use only if this.dataType is DATATYPE_NUMERIC
     */
    public List<Range> getAcceptedRanges() {
        return acceptedRanges;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param columnName the columnName to set
     */
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * @param dataType the dataType to set. Must be either DATATYPE_STRING or DATATYPE_NUMERIC
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * @param acceptedValues the acceptedValues to set. Use only if this.dataType is DATATYPE_STRING
     */
    public void setAcceptedValues(List<String> acceptedValues) {
        this.acceptedValues = acceptedValues;
    }

    /**
     * @param acceptedRanges the acceptedRanges to set. Use only if this.dataType is DATATYPE_NUMERIC
     */
    public void setAcceptedRanges(List<Range> acceptedRanges) {
        this.acceptedRanges = acceptedRanges;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Perform a deep-copy EXCEPT Identifiable attributes of the given other instance into this instance. To be
     * overriden in child class to perform deep-copy of class fields.
     *
     * @param otherOIBase other instance whose values will be copied
     * @throws ClassCastException when otherOIBase is not a GenericFilter
     */
    @Override
    public void copyValues(final OIBase otherOIBase) {

        // super.copyValues(otherOIBase); no need to call Identifiable.copyValues

        final GenericFilter other = (GenericFilter) otherOIBase;

        this.columnName = other.columnName;
        this.dataType = other.dataType;

        // set null if other is null
        if (other.acceptedValues == null) {
            this.acceptedValues = null;
        }
        else if (this.acceptedValues == null) {
            this.acceptedValues = new ArrayList<>(other.acceptedValues);
        }
        else { // reuse the local list container if it exists
            this.acceptedValues.clear();
            this.acceptedValues.addAll(other.acceptedValues);
        }

        if (other.acceptedRanges == null) {
            this.acceptedRanges = null;
        }
        else {
            if (this.acceptedRanges == null) {
                this.acceptedRanges = new ArrayList<>(other.acceptedRanges.size());
            }
            else {
                this.acceptedRanges.clear();
            }
            for (Range otherRange : other.acceptedRanges) { // clone each Range
                this.acceptedRanges.add((Range) otherRange.clone());
            }
        }

        this.enabled = other.enabled;
    }

    /**
     * Deep comparison.
     *
     * @param otherObject to compare. False if its class is not strictly GenericFilter
     * @return true or false
     */
    @Override
    public boolean equals(Object otherObject) {

        if (!super.equals(otherObject)) {
            return false;
        }

        GenericFilter other = (GenericFilter) otherObject;

        return Objects.equals(this.columnName, other.columnName)
                && Objects.equals(this.dataType, other.dataType)
                && (this.enabled == other.enabled)
                && Objects.equals(this.acceptedValues, other.acceptedValues)
                && Objects.equals(this.acceptedRanges, other.acceptedRanges);
    }

    /**
     * toString() implementation using string builder
     *
     * @param sb string builder to append to
     */
    @Override
    public void toString(final StringBuilder sb, final boolean full) {
        super.toString(sb, full);

        if (full) {
            sb.append(", columnName=").append(columnName);
            sb.append(", dataType=").append(dataType);
            sb.append(", acceptedValues=");
            ObjectUtils.toString(sb, full, acceptedValues);
            sb.append(", acceptedRanges=");
            ObjectUtils.toString(sb, full, acceptedRanges);
            sb.append(", enabled=").append(enabled);
        }

        sb.append(" }");
    }

}
