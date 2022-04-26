package org.magicat.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.Gson;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

@EqualsAndHashCode
@ToString
@Document(collection = "Articles")
public class Article {

    @Id
    private ObjectId id;

    @TextIndexed
    private String title;

    private String citation;

    private String authors;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private DateTime publicationDate;

    private String pubAbstract;
    private String citations;

    //@Indexed(unique = true)
    private String doi;
    //@Indexed(unique = true)
    private String pii;

    @TextIndexed
    @Indexed(unique = true)
    private String pmId;
    //@Indexed(unique = true)
    private String pmcId;

    private String meshTerms;
    private String keywords;

    private String solrId;
    private String fulltext;
    private String supportingText;
    private String topics;

    private String pageNumbers;
    private String journal;
    private String volume;
    private String issue;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private DateTime pubDate;

    private Boolean hasFullText;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private DateTime lastChecked;

    @Transient
    @JsonSerialize
    @JsonDeserialize
    private String inSupporting;


    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public DateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(DateTime publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getPubAbstract() {
        return pubAbstract;
    }

    public void setPubAbstract(String pubAbstract) {
        this.pubAbstract = pubAbstract;
    }

    public String getCitations() {
        return citations;
    }

    public void setCitations(String citations) {
        this.citations = citations;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getPii() {
        return pii;
    }

    public void setPii(String pii) {
        this.pii = pii;
    }

    public String getPmId() {
        return pmId;
    }

    public void setPmId(String pmId) {
        this.pmId = pmId;
    }

    public String getPmcId() {
        return pmcId;
    }

    public void setPmcId(String pmcId) {
        this.pmcId = pmcId;
    }

    public String getMeshTerms() {
        return meshTerms;
    }

    public void setMeshTerms(String meshTerms) {
        this.meshTerms = meshTerms;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getSolrId() {
        return solrId;
    }

    public void setSolrId(String solrId) {
        this.solrId = solrId;
    }

    public String getFulltext() {
        return fulltext;
    }

    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    public String getSupportingText() {
        return supportingText;
    }

    public void setSupportingText(String supportingText) {
        this.supportingText = supportingText;
    }

    public String getPageNumbers() {
        return pageNumbers;
    }

    public void setPageNumbers(String pageNumbers) {
        this.pageNumbers = pageNumbers;
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public DateTime getPubDate() {
        return pubDate;
    }

    public void setPubDate(DateTime pubDate) {
        this.pubDate = pubDate;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public String getInSupporting() {
        return inSupporting;
    }

    public void setInSupporting(String inSupporting) {
        this.inSupporting = inSupporting;
    }

    public Boolean getHasFullText() {
        return hasFullText;
    }

    public void setHasFullText(Boolean hasFullText) {
        this.hasFullText = hasFullText;
    }

    public DateTime getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(DateTime lastChecked) {
        this.lastChecked = lastChecked;
    }

    public static String annotate(String text) {
        return "<b style={{backgroundColor: 'yellow'}}>" + text + "</b>";

    }

    public static Article fromJSON(String text) {
        return new Gson().fromJson(text, Article.class);
    }

    public static String toJSON(Article article) {
        return new Gson().toJson(article);
    }

    public static String toText(Article article) {
        StringBuilder sb = new StringBuilder(50000);
        sb.append(" {!title} ");
        sb.append(article.getTitle());
        sb.append(" {!keywords} ");
        if (article.getKeywords() != null) {
            String[] kw = article.getKeywords().split(";");
            for (int i = 0; i < kw.length; i++) {
                if (i != 0) sb.append(" , ");
                if (kw[i].split(":").length > 1) sb.append(kw[i].split(":")[1]);
            }
        }
        sb.append(" {!meshterms} ");
        if (article.getMeshTerms() != null) {

            String[] mt = article.getMeshTerms().split(";");
            for (int i = 0; i < mt.length; i++) {
                if (i != 0) sb.append(" , ");
                if (mt[i].split(":").length > 2) sb.append(mt[i].split(":")[2]);
            }
        }
        sb.append(" {!abstract} ");
        if (article.getPubAbstract() != null) sb.append(article.getPubAbstract());
        sb.append(" {!fulltext} ");
        if (article.getFulltext() != null) {
            sb.append(article.getFulltext());
        }
        return sb.toString();
    }

}
