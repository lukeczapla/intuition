package org.magicat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.solr.client.solrj.beans.Field;

import java.util.List;

@ToString
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class SequenceItem {

    @Field("id")
    private String id;

    @Field("chromosome")
    private List<String> chromosome;

    @Field("name")
    private List<String> name;

    @Field("position")
    private List<Long> position;

    @Field("genbank")
    private List<String> genbank;

    @Field("refseq")
    private List<String> refseq;

    @Field("seq")
    private List<String> seq;

    public SequenceItem() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getChromosome() {
        return chromosome;
    }

    public void setChromosome(List<String> chromosome) {
        this.chromosome = chromosome;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public List<Long> getPosition() {
        return position;
    }

    public void setPosition(List<Long> position) {
        this.position = position;
    }

    public List<String> getGenbank() {
        return genbank;
    }

    public void setGenbank(List<String> genbank) {
        this.genbank = genbank;
    }

    public List<String> getRefseq() {
        return refseq;
    }

    public void setRefseq(List<String> refseq) {
        this.refseq = refseq;
    }

    public List<String> getSeq() {
        return seq;
    }

    public void setSeq(List<String> seq) {
        this.seq = seq;
    }
}
