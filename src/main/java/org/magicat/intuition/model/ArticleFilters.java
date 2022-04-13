package org.magicat.intuition.model;

import com.fasterxml.jackson.annotation.JsonView;

import java.util.Date;

public class ArticleFilters {

    @JsonView(Views.ArticleFilters.class)
    private Integer[] groups;
    @JsonView(Views.ArticleFilters.class)
    private String[] values;

    @JsonView(Views.ArticleFilters.class)
    private String searchTerms;

    @JsonView(Views.ArticleFilters.class)
    private Integer limit;

    @JsonView(Views.ArticleFilters.class)
    private Date date;

    @JsonView(Views.ArticleFilters.class)
    private String authors;

    public Integer[] getGroups() {
        return groups;
    }

    public void setGroups(Integer[] groups) {
        this.groups = groups;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public String getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(String searchTerms) {
        this.searchTerms = searchTerms;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }
}
