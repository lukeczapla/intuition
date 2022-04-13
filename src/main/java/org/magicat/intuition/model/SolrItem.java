package org.magicat.intuition.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.List;

@ToString
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class SolrItem {

    @Field("id")
    private String id;

    @Field("pmid")
    private List<Long> pmid;

    @Field("pmid_supporting")
    private List<String> pmid_supporting;

    @Field("text")
    private List<String> text;

    @Field("text_ws")
    private List<String> text_ws;

    @Field("hasFullText")
    private List<Boolean> hasFullText;

    @Field("authors")
    private List<String> authors;

    @Field("date")
    private List<DateTime> date;

    public SolrItem() {
        // nothing to do here, AllArgsConstructor and Builder is more useful
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Long> getPmid() {
        return pmid;
    }

    public void setPmid(List<Long> pmid) {
        this.pmid = pmid;
    }

    public List<String> getPmid_supporting() {
        return pmid_supporting;
    }

    public void setPmid_supporting(List<String> pmid_supporting) {
        this.pmid_supporting = pmid_supporting;
    }

    public List<String> getText() {
        return text;
    }

    public void setText(List<String> text) {
        this.text = text;
    }

    public List<String> getText_ws() {
        return text_ws;
    }

    public void setText_ws(List<String> text_ws) {
        this.text_ws = text_ws;
    }

    public List<Boolean> getHasFullText() {
        return hasFullText;
    }

    public void setHasFullText(List<Boolean> hasFullText) {
        this.hasFullText = hasFullText;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public List<DateTime> getDate() {
        return date;
    }

    public void setDate(List<DateTime> date) {
        this.date = date;
    }

    /*@SuppressWarnings("unchecked")
    public static SolrItem fromSolrDocument(SolrDocument solrDocument) {
        Collection<String> fields = solrDocument.getFieldNames();
        SolrItemBuilder builder = new SolrItemBuilder();
        if (fields.contains("id")) builder.id((String)solrDocument.get("id"));
        if (fields.contains("pmid")) builder.pmid(((List<Long>)solrDocument.get("pmid")).get(0));
        if (fields.contains("pmid_supporting")) builder.pmid_supporting(((List<String>)solrDocument.get("pmid")).get(0));
        if (fields.contains("text")) builder.text(((List<String>)solrDocument.get("text")).get(0));
        if (fields.contains("authors")) builder.authors(((List<String>)solrDocument.get("authors")).get(0));
        if (fields.contains("date")) builder.date(DateTime.parse(((List<String>)solrDocument.get("date")).get(0)));
        return builder.build();
    }*/

}
