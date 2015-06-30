
package com.microsoft.schemas.xrm._2011.contracts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import com.microsoft.schemas.xrm._2011.metadata.EntityMetadata;


/**
 * <p>Java class for EntityMetadataCollection complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EntityMetadataCollection">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="EntityMetadata" type="{http://schemas.microsoft.com/xrm/2011/Metadata}EntityMetadata" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EntityMetadataCollection", propOrder = {
    "entityMetadatas"
})
public class EntityMetadataCollection
    implements Serializable
{

    private final static long serialVersionUID = 1L;
    @XmlElement(name = "EntityMetadata", nillable = true)
    protected List<EntityMetadata> entityMetadatas;

    /**
     * Gets the value of the entityMetadatas property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the entityMetadatas property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEntityMetadatas().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EntityMetadata }
     * 
     * 
     */
    public List<EntityMetadata> getEntityMetadatas() {
        if (entityMetadatas == null) {
            entityMetadatas = new ArrayList<EntityMetadata>();
        }
        return this.entityMetadatas;
    }

}
