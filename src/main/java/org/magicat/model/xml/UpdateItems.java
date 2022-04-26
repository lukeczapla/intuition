package org.magicat.model.xml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@XmlRootElement(name = "UpdateItems")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpdateItems {

    @XmlElement(name="PageSize")
    private Integer pageSize;

    @XmlElementWrapper(name="Items")
    @XmlElement(name="Item")
    private List<Item> items;

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void writeConfig() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(UpdateItems.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(this, new File("UpdateItems.xml"));
            marshaller.marshal(this, System.out);
        } catch(JAXBException e) {
            e.printStackTrace();
        }
    }

}
