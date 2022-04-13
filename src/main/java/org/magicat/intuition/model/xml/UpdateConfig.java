package org.mskcc.knowledge.model.xml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

@XmlRootElement(name = "Update")
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class UpdateConfig {
    @XmlElement(name = "Genes")
    private Boolean genes = false;
    @XmlElement(name = "Mutations")
    private Boolean mutations = false;
    //@XmlElement(name = "MutationsNonPoint")
    //private Boolean mutationsOther = false;
    @XmlElement(name = "Cancers")
    private Boolean cancers = false;
    @XmlElement(name = "Drugs")
    private Boolean drugs = false;
    @XmlElement(name = "GeneCleanup")
    private Boolean geneCleanup = false;
    @XmlElement(name = "Complete")
    private Boolean complete = false;
    @XmlElement(name = "Page")
    private Integer page = 0;
    @XmlElement(name = "PageSize")
    private Integer pageSize = 500000;

    public Boolean getGenes() {
        return genes;
    }

    public void setGenes(Boolean genes) {
        this.genes = genes;
    }

    public Boolean getMutations() {
        return mutations;
    }

    public void setMutations(Boolean mutations) {
        this.mutations = mutations;
    }

    public Boolean getCancers() {
        return cancers;
    }

    public void setCancers(Boolean cancers) {
        this.cancers = cancers;
    }

    public Boolean getDrugs() {
        return drugs;
    }

    public void setDrugs(Boolean drugs) {
        this.drugs = drugs;
    }

    public Boolean getGeneCleanup() {
        return geneCleanup;
    }

    public void setGeneCleanup(Boolean geneCleanup) {
        this.geneCleanup = geneCleanup;
    }

    public Boolean getComplete() {
        return complete;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void writeConfig() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(UpdateConfig.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(this, new File("UpdateConfig.xml"));
            marshaller.marshal(this, System.out);
        } catch(JAXBException e) {
            e.printStackTrace();
        }
    }

}

