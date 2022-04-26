package org.magicat.config;

// very simple way to switch systems quickly;
public class ConfigProperties {

    // quickly switch the properties of MongoDB or Apache Solr
    public static final boolean mongoOverride = true;   // false on-prem
    public static final boolean solrCloud = false;      // true on-prem

}
