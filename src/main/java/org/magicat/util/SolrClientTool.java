package org.magicat.util;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.*;
import org.apache.solr.client.solrj.response.Cluster;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.util.NamedList;
import org.magicat.config.ConfigProperties;
import org.magicat.model.SolrItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component("solrClientTool")
public class SolrClientTool {

    public final static Logger log = LoggerFactory.getLogger(SolrClientTool.class);

    private final static boolean cloud = ConfigProperties.solrCloud;
    private final static String httpUrl = "http://192.168.1.86:8983/solr"; // "http://aimlcoe.mskcc.org:8983/solr"
    private final List<String> solrCloudURLs = Arrays.asList("http://plbd01.mskcc.org:8983/solr", "http://plbd02.mskcc.org:8983/solr");

    private final SolrClient client = cloud ? new CloudSolrClient.Builder(solrCloudURLs).withParallelUpdates(true).build() : new HttpSolrClient.Builder().withBaseSolrUrl(httpUrl).allowCompression(true).build();

    private static int count = 0;
    private int reloadRate = 15000;

    private String defaultField;
    private String collection;
    private String parser = "edismax";

    @AllArgsConstructor
    @Getter
    @Setter
    public static class ResultMap {
        private Map<String, Object> explainMap;
        private SolrDocumentList docs;
        private Map<String, Map<String, List<String>>> highlightingMap;
    }


    public SolrClientTool() {
        if (cloud) HttpClientUtil.addRequestInterceptor(new SolrPreemptiveAuthInterceptor());
        defaultField = "text";
    }

    public static String randomId(int length) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 90; // letter 'Z'
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static String randomId() {
        return randomId(20);
    }

    @Bean
    public SolrClient solrClient() {
        return cloud ? new CloudSolrClient.Builder(solrCloudURLs).withParallelUpdates(true).build() : new HttpSolrClient.Builder().withBaseSolrUrl(httpUrl).allowCompression(true).build();
    }

    public static List<SolrItem> documentsToItems(SolrDocumentList sdl) {
        DocumentObjectBinder binder = new DocumentObjectBinder();
        return binder.getBeans(SolrItem.class, sdl);
    }

    @Deprecated // use paging here!
    public SolrDocumentList getAllDocuments() throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery("*:*");
        int maxArticles = 30000000;
        query.setRows(maxArticles);
        QueryResponse response = client.query("knowledge", query);
        return response.getResults();
    }

    /**
     *
     * @param collection The name of the collection (e.g. "knowledge")
     * @param queryText The string with the query (e.g. "pmid:12345 AND text:BRAF")
     * @return The list of documents that match the query.
     * @throws IOException
     * @throws SolrServerException
     */
    public SolrDocumentList findDismax(String collection, String queryText) throws IOException, SolrServerException {
        //queryText = queryText.replace("\\*", "*"); // not needed
        if (collection == null) collection = this.getCollection();
        SolrQuery query = new SolrQuery();
        query.setQuery(queryText).setStart(0).setRows(10000).setIncludeScore(true);
        query.setParam("mm", "100%");
        query.setParam("df", defaultField).setParam("defType", "dismax");
        QueryResponse response = client.query(collection, query);
        SolrDocumentList results = response.getResults();
        if (results.size() == 0) {
            log.info("No Solr query results found for {}", queryText);
        }
        return results;
    }

    public SolrDocumentList findDismax(String collection, String queryText, String fields, int nResults, String freqTerm) throws IOException, SolrServerException {
        //queryText = queryText.replace("\\*", "*"); // unescape *, dismax doesn't have wildcards! but necessary? probably not.
        if (collection == null) collection = this.getCollection();
        SolrQuery query = new SolrQuery();
        query.setParam("mm", "100%");
        query.setQuery(queryText).setFields(fields).setStart(0).setRows(nResults);
        query.setParam("df", defaultField);
        if (freqTerm != null && freqTerm.charAt(0) == '"' && freqTerm.substring(1).contains("\""))
            query.addSort(SolrQuery.SortClause.desc("termfreq(" + defaultField + ", " + freqTerm + ")"));
        else if (freqTerm != null)
            query.addSort(SolrQuery.SortClause.desc("termfreq(" + defaultField + ", " + '"' + freqTerm + '"' + ")"));
        query.addSort(SolrQuery.SortClause.desc("date"));
        query.setIncludeScore(true).setParam("defType", "dismax");
        QueryResponse response = client.query(collection, query);

        SolrDocumentList results = response.getResults();
        if (results.size() == 0) {
            log.info("No Solr query results found for {}", queryText);
        }
        return results;
    }

    public SolrDocumentList find(String collection, String queryText) throws IOException, SolrServerException {
        if (collection == null) collection = this.getCollection();
        SolrQuery query = new SolrQuery();
        query.setQuery(queryText).setStart(0).setRows(10000).setIncludeScore(true);
        query.setParam("mm", "100%");
        query.setParam("df", defaultField).setParam("defType", getParser()); // lucene or edismax (or dismax, etc)
        QueryResponse response = client.query(collection, query);
        /*if (response.getClusteringResponse() != null && response.getClusteringResponse().getClusters() != null) {
            List<Cluster> clusters = response.getClusteringResponse().getClusters();
            int item = 1;
            for (Cluster c : clusters) {
                log.info("Cluster " + item);
                log.info(c.toString());
                log.info(c.getLabels().toString());
                log.info(c.getDocs().toString());
                log.info("Subclusters:");
                int item2 = 1;
                if (c.getSubclusters() != null) for (Cluster c2 : c.getSubclusters()) {
                    log.info("Subcluster " + item2);
                    log.info(c.toString());
                    log.info(c.getLabels().toString());
                    log.info(c.getDocs().toString());
                    item2++;
                }
                item++;
            }
        }*/
        SolrDocumentList results = response.getResults();
        if (results.size() == 0) {
            log.info("No Solr query results found for {}", queryText);
        }
        return results;
    }

    public SolrDocumentList find(String collection, String queryText, String fields, int nResults, String freqTerm) throws IOException, SolrServerException {
        if (collection == null) collection = this.getCollection();
        SolrQuery query = new SolrQuery();
        query.setQuery(queryText).setStart(0).setRows(nResults);
        query.setParam("df", defaultField);
        if (freqTerm != null && freqTerm.charAt(0) == '"' && freqTerm.substring(1).contains("\"")) {
            query.setFields(fields, "termfreq(" + defaultField + ", " + freqTerm + ")");
            query.addSort(SolrQuery.SortClause.desc("termfreq(" + defaultField + ", " + freqTerm + ")"));
        }
        else if (freqTerm != null) {
            query.setFields(fields, "termfreq(" + defaultField + ", " + '"' + freqTerm + '"' + ")");
            query.addSort(SolrQuery.SortClause.desc("termfreq(" + defaultField + ", " + '"' + freqTerm + '"' + ")"));
        } else query.setFields(fields);
        //query.addSort(SolrQuery.SortClause.desc("date"));
        query.setIncludeScore(true).setParam("defType", "edismax");
        QueryResponse response = client.query(collection, query);

        SolrDocumentList results = response.getResults();
        if (results.size() == 0) {
            log.info("No Solr query results found for {}", queryText);
        }
        return results;
    }

    public SolrDocumentList deepPage(String query, int rows) throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery(query).setRows(rows).setParam("df", defaultField).setParam("defType", "edismax")
                .addSort(SolrQuery.SortClause.desc("id")).addSort(SolrQuery.SortClause.desc("score"));
        String cursorMark = CursorMarkParams.CURSOR_MARK_START;
        boolean done = false;
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        while (!done) {
            solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse response = client.query("knowledge", solrQuery);
            String nextCursorMark = response.getNextCursorMark();
            solrDocumentList.addAll(response.getResults());
            if (cursorMark.equals(nextCursorMark)) {
                done = true;
            }
            cursorMark = nextCursorMark;
        }
        return solrDocumentList;
    }

    public SolrDocumentList findClustering(String queryText) throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.setQuery(queryText);
        query.setStart(0).setRows(200).setIncludeScore(true);
        query.setParam("qt", "/clustering");
        //query.setParam("df", "all");
        query.setParam("defType", "edismax"); // lucene or edismax (or dismax, etc)
        QueryResponse response = client.query("knowledge", query);
        if (response.getClusteringResponse() != null && response.getClusteringResponse().getClusters() != null) {
            List<Cluster> clusters = response.getClusteringResponse().getClusters();
            int item = 1;
            for (Cluster c : clusters) {
                log.info("Cluster " + item);
                log.info(c.toString());
                log.info(c.getLabels().toString());
                log.info(c.getDocs().toString());
                log.info("Subclusters:");
                int item2 = 1;
                if (c.getSubclusters() != null) for (Cluster c2 : c.getSubclusters()) {
                    log.info("Subcluster " + item2);
                    log.info(c2.toString());
                    log.info(c2.getLabels().toString());
                    log.info(c2.getDocs().toString());
                    item2++;
                }
                item++;
            }
        }
        SolrDocumentList results = response.getResults();
        if (results.size() == 0) {
            log.info("No Solr query results found for {}", queryText);
        }
        return results;
    }


    public ResultMap queryCount(String collection, String phrase, String fq) throws IOException, SolrServerException {
        if (collection == null) collection = this.getCollection();
        SolrQuery query = new SolrQuery().setQuery(phrase).setParam("df", defaultField).setParam("fq", fq);
        query.setParam("debugQuery", "true").setFields("id,pmid,pmid_supporting");
        QueryResponse response = client.query(collection, query);
        return new ResultMap(response.getExplainMap(), response.getResults(), null);
    }


    public ResultMap queryHighlightFragments(String collection, String queryText, int nResults) throws IOException, SolrServerException {
        if (collection == null) collection = this.getCollection();
        SolrQuery query = new SolrQuery();
        query.setQuery(queryText).setFields("id,text,pmid,pmid_supporting");
        query.setHighlight(true).addHighlightField(defaultField).setHighlightSimplePre("<mark>").setHighlightSimplePost("</mark>");
        query.setIncludeScore(true);//.setParam("debugQuery", "true");
        query.setStart(0).setRows(nResults).setParam("defType", "edismax").setParam("df", defaultField);
        QueryResponse response = client.query(collection, query);
        SolrDocumentList results = response.getResults();
        if (results.size() == 0) {
            log.info("No Solr query results found for {}", query);
            return null;
        }
        return new ResultMap(response.getExplainMap(), results, response.getHighlighting());
    }

    public ResultMap queryHighlight(String collection, String queryText, int nResults) throws IOException, SolrServerException {
        if (collection == null) collection = this.getCollection();
        SolrQuery query = new SolrQuery();
        query.setQuery(queryText).setFields("id,pmid,pmid_supporting");
        query.setHighlight(true).addHighlightField(defaultField).setHighlightSimplePre("<mark>").setHighlightSimplePost("</mark>").setHighlightFragsize(0);
        query.setIncludeScore(true);//.setParam("debugQuery", "true");
        query.setStart(0).setRows(nResults).setParam("defType", "edismax").setParam("df", defaultField);
        QueryResponse response = client.query(collection, query);
        SolrDocumentList results = response.getResults();
        if (results.size() == 0) {
            log.info("No Solr query results found for {}", query);
            return null;
        }
        return new ResultMap(response.getExplainMap(), results, response.getHighlighting());
    }

    public ResultMap queryHighlight(String collection, String queryText, String fq, int nResults) throws IOException, SolrServerException {
        if (collection == null) collection = this.getCollection();
        SolrQuery query = new SolrQuery();
        query.setQuery(queryText).setFields("id,pmid,pmid_supporting");
        query.setHighlight(true).addHighlightField(defaultField).setHighlightSimplePre("<mark>").setHighlightSimplePost("</mark>").setHighlightFragsize(0);
        query.setIncludeScore(true).setParam("fq", fq); //.setParam("debugQuery", "true")
        query.setStart(0).setRows(nResults).setParam("defType", "edismax").setParam("df", defaultField);   // WAS lucene!!!!!
        QueryResponse response = client.query(collection, query);
        SolrDocumentList results = response.getResults();
        if (results.size() == 0) {
            log.info("No Solr query results found for {}", query);
            return null;
        }
        return new ResultMap(response.getExplainMap(), results, response.getHighlighting());
    }

    public SolrDocument getDocument(String collection, String id) throws SolrServerException, IOException {
        if (collection == null) collection = this.getCollection();
        return client.getById(collection, id);
    }


    public SolrDocumentList getDocuments(String collection, List<String> ids) throws SolrServerException, IOException {
        if (collection == null) collection = this.getCollection();
        return client.getById(collection, ids);
    }


    public boolean exists(String collection, String id) throws SolrServerException, IOException {
        if (collection == null) collection = this.getCollection();
        return getDocument(collection, id) != null;
    }


    public UpdateResponse add(String collection, String id, String title, String authors, String fileUrl, List<String> categories, String content)
            throws SolrServerException, IOException {
        if (collection == null) collection = this.getCollection();
        SolrInputDocument inputDoc = new SolrInputDocument();
        inputDoc.addField("id", id);
        inputDoc.addField("attr_pdf_docinfo_title", Lists.newArrayList(title));
        inputDoc.addField("attr_author", Lists.newArrayList(authors));
        inputDoc.addField("attr_fileurl", Lists.newArrayList(fileUrl));
        if (categories != null) inputDoc.addField("categories", StringUtils.join(categories, ","));
        inputDoc.addField("attr_content", Lists.newArrayList(content));

        UpdateResponse response = client.add(collection, inputDoc);

        if (++count % reloadRate == 0) {
            CollectionAdminRequest.reloadCollection(collection).process(client);
        }
        return response;
    }

    public UpdateResponse addItems(String collection, List<Map<String, Object>> baseValues) throws SolrServerException, IOException {
        if (collection == null) collection = this.getCollection();
        List<SolrInputDocument> inputDocs = new ArrayList<>();
        for (int i = 0; i < baseValues.size(); i++) {
            SolrInputDocument inputDoc = new SolrInputDocument();
            String id = Integer.toHexString(baseValues.get(i).hashCode());
            inputDoc.addField("id", id);
            //if (exists(collection, id)) {
            //    log.error("ID {} already exists in Solr collection", id);
            //    return null;
            //}
            for (String key : baseValues.get(i).keySet()) {
                inputDoc.addField(key, baseValues.get(i).get(key));
            }
            inputDocs.add(inputDoc);
        }
        UpdateResponse response = client.add(collection, inputDocs);
        client.commit(collection);
        return response;
    }

    public UpdateResponse addItem(String collection, Map<String, Object> baseValues, Map<String, Object> appendValues) throws SolrServerException, IOException {
        if (collection == null) collection = this.getCollection();
        SolrInputDocument inputDoc = new SolrInputDocument();
        String id = Integer.toHexString(baseValues.hashCode());
        inputDoc.addField("id", id);
        if (exists(collection, id)) {
            log.error("ID {} already exists in Solr collection", id);
            return null;
        }
        for (String key: baseValues.keySet()) {
            inputDoc.addField(key, baseValues.get(key));
        }
        if (appendValues != null) for (String key: appendValues.keySet()) {
            inputDoc.addField(key, appendValues.get(key));
        }
        UpdateResponse response = client.add(collection, inputDoc);
        if (++count % reloadRate == 0) client.commit(collection);
        return response;
    }

    public UpdateResponse addItem(Map<String, Object> baseValues) throws SolrServerException, IOException {
        return addItem(null, baseValues, null);
    }

    public UpdateResponse addItems(List<Map<String, Object>> baseValues) throws SolrServerException, IOException {
        return addItems(null, baseValues);
    }

    public void commit() throws SolrServerException, IOException {
        client.commit(getCollection());
    }

    public void commit(String collection) throws SolrServerException, IOException {
        client.commit(collection);
    }

    public UpdateResponse add(String collection, Map<String, List<String>> properties, Map<String, List<String>> append) throws SolrServerException, IOException {
        SolrInputDocument inputDoc = new SolrInputDocument();
        String pmid = properties.get("pmid").get(0);
        String id = Integer.toHexString(properties.hashCode()) + (pmid.length() > 3 ? pmid.substring(pmid.length()-3) : pmid);
        inputDoc.addField("id", id);
        if (exists(collection, id)) {
            log.error("ID {} already exists in Solr collection", id);
            return null;
        }
        for (String key: properties.keySet()) {
            inputDoc.addField(key, properties.get(key));
        }
        for (String key: append.keySet()) {
            inputDoc.addField(key, append.get(key));
        }
        UpdateResponse response = client.add(collection, inputDoc);
        //client.commit(collection);
        if (++count % reloadRate == 0) client.commit(collection);

        return response;
    }

    public UpdateResponse update(String collection, Map<String, List<String>> properties, Map<String, List<String>> append) throws SolrServerException, IOException {
        SolrInputDocument inputDoc = new SolrInputDocument();
        String pmid = properties.get("pmid").get(0);
        String id = Integer.toHexString(properties.hashCode()) + (pmid.length() > 3 ? pmid.substring(pmid.length()-3) : pmid);
        inputDoc.addField("id", id);
        for (String key: properties.keySet()) {
            inputDoc.addField(key, properties.get(key));
        }
        for (String key: append.keySet()) {
            inputDoc.addField(key, append.get(key));
        }
        UpdateResponse response = client.add(collection, inputDoc);
        if (++count % reloadRate == 0) client.commit(collection);

        return response;
    }

    public UpdateResponse addUpdateMany(String collection, List<String> id, List<String> title, List<String> authors, List<String> fileUrl, List<List<String>> categories, List<String> content)
            throws SolrServerException, IOException {
        List<SolrInputDocument> inputDocuments = new ArrayList<>();
        for (int i = 0; i < id.size(); i++) {
            SolrInputDocument inputDoc = new SolrInputDocument();
            inputDoc.addField("id", id.get(i));
            inputDoc.addField("attr_pdf_docinfo_title", Lists.newArrayList(title.get(i)));
            inputDoc.addField("attr_author", Lists.newArrayList(authors.get(i)));
            inputDoc.addField("attr_fileurl", Lists.newArrayList(fileUrl.get(i)));
            if (categories != null) inputDoc.addField("categories", StringUtils.join(categories.get(i), ","));
            inputDoc.addField("attr_content", Lists.newArrayList(content.get(i)));
            inputDocuments.add(inputDoc);
        }
        UpdateResponse response = client.add(collection, inputDocuments);
        client.commit(collection);
        return response;
    }

    public UpdateResponse addUpdateMany(String collection, List<Map<String, List<String>>> properties, List<Map<String, List<String>>> append) throws SolrServerException, IOException {
        // sanity check, properties and appended items should be the same length!
        if (properties.size() != append.size()) return null;
        List<SolrInputDocument> inputDocs = new ArrayList<>();
        for (int i = 0; i < properties.size(); i++) {
            Map<String, List<String>> map = properties.get(i);
            SolrInputDocument inputDoc = new SolrInputDocument();
            String pmid = map.get("pmid").get(0);
            String id = Integer.toHexString(map.hashCode()) + (pmid.length() > 3 ? pmid.substring(pmid.length()-3) : pmid);
            inputDoc.addField("id", id);
            for (String key : map.keySet()) {
                inputDoc.addField(key, map.get(key));
            }
            for (String key : append.get(i).keySet()) {
                inputDoc.addField(key, append.get(i).get(key));
            }
            inputDocs.add(inputDoc);
        }
        UpdateResponse response = client.add(collection, inputDocs);
        client.commit(collection);

        return response;
    }

    public UpdateResponse addUpdateDeleteMany(String collection, List<Map<String, List<String>>> properties, List<Map<String, List<String>>> append, List<String> newIds, List<String> deleteIds) throws SolrServerException, IOException {
        // sanity check, properties and appended items should be the same length!
        if (properties.size() != append.size()) return null;
        List<SolrInputDocument> inputDocs = new ArrayList<>();
        for (int i = 0; i < properties.size(); i++) {
            Map<String, List<String>> map = properties.get(i);
            SolrInputDocument inputDoc = new SolrInputDocument();
            inputDoc.addField("id", newIds.get(i));
            for (String key : map.keySet()) {
                inputDoc.addField(key, map.get(key));
            }
            for (String key : append.get(i).keySet()) {
                inputDoc.addField(key, append.get(i).get(key));
            }
            inputDocs.add(inputDoc);
        }
        if (deleteIds.size() > 0) client.deleteById(collection, deleteIds);
        UpdateResponse response = client.add(collection, inputDocs);
        client.commit(collection);
        return response;
    }

    public UpdateResponse add(String collection, Map<String, List<String>> properties) throws SolrServerException, IOException {
        SolrInputDocument inputDoc = new SolrInputDocument();
        String pmid = properties.get("pmid").get(0);
        String id = Integer.toHexString(properties.hashCode()) + (pmid.length() > 3 ? pmid.substring(pmid.length()-3) : pmid);
        inputDoc.addField("id", id);
        if (exists(collection, id)) return null;
        for (String key: properties.keySet()) {
            inputDoc.addField(key, properties.get(key));
        }
        UpdateResponse response = client.add(collection, inputDoc);
        if (++count % reloadRate == 0) CollectionAdminRequest.reloadCollection(collection).process(client);
        return response;
    }

    // ???
    public NamedList<Object> extract(String collection, String id, String name, String URL, List<String> categories, File file, String contentType)
            throws SolrServerException, IOException {
        ContentStreamUpdateRequest update = new ContentStreamUpdateRequest("/$core/update/extract");
        String fullUrl = file.toURI().toURL().toExternalForm();
        if (URL != null && !URL.equals("")) fullUrl = URL;
        update.addFile(file, contentType);
        update.setParam("id", id);
        update.setParam("literal.id", id);
        update.setParam("name", name);
        update.setParam("literal.name", name);
        update.setParam("fileUrl", fullUrl);
        update.setParam("literal.fileUrl", fullUrl);
        update.setParam("categories", StringUtils.join(categories, ","));
        update.setParam("literal.categories", StringUtils.join(categories, ","));
        update.setParam("uprefix", "attr_");
        update.setParam("fmap.content", "attr_content");
        update.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
        if (++count % reloadRate == 0) CollectionAdminRequest.reloadCollection(collection).process(client);
        return client.request(update);
    }

    public void refreshCollection(String collection) {
        try {
            log.info("Attempting to reload the Solr collection");
            CollectionAdminRequest.reloadCollection(collection).process(client);
        } catch (IOException|SolrServerException e) {
            e.printStackTrace();
        }
    }

    public UpdateResponse deleteMany(String collection, List<String> deleteIds) throws SolrServerException, IOException {
        if (deleteIds != null && deleteIds.size() > 0) return client.deleteById(collection, deleteIds);
        return null;
    }

    public UpdateResponse delete(String collection, String id) throws SolrServerException, IOException {
        UpdateResponse response = client.deleteById(collection, id);
        if (++count % reloadRate == 0) client.commit(collection);

        return response;
    }

    public UpdateResponse delete(String collection, String id, boolean commit) throws SolrServerException, IOException {
        UpdateResponse response = client.deleteById(collection, id);
        if (commit) client.commit(collection);
        return response;
    }

    public String getDefaultField() {
        return defaultField;
    }

    public void setDefaultField(String defaultField) {
        this.defaultField = defaultField;
    }

    public String getCollection () {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public int getReloadRate() {
        return reloadRate;
    }

    public void setReloadRate(int reloadRate) {
        this.reloadRate = reloadRate;
    }

    public String getParser() {
        return parser;
    }

    public void setParser(String parser) {
        this.parser = parser;
    }

    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!'  || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                    || c == '*' || c == '?' || c == '|' || c == '&'  || c == ';' || c == '/'
                    || Character.isWhitespace(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static String quote(String s) {
        return "\"" + s + "\"";
    }

    static class SolrPreemptiveAuthInterceptor implements HttpRequestInterceptor {

        //final static Logger log = LoggerFactory.getLogger(SolrPreemptiveAuthInterceptor.class);

        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            AuthState authState = (AuthState)context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);
            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                //log.info("No AuthState: set Basic Auth");

                HttpHost targetHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
                AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());

                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);

                Credentials creds = credsProvider.getCredentials(authScope);
                if (creds == null) {
                    //log.info("No Basic Auth credentials: add them");
                    creds = getCredentials(authScope);
                    credsProvider.setCredentials(authScope, creds);
                    context.setAttribute(HttpClientContext.CREDS_PROVIDER, credsProvider);
                }
                authState.update(new BasicScheme(), creds);
            }

        }


        private Credentials getCredentials(AuthScope authScope) {

            String user = System.getenv("solrUsername");
            String password = System.getenv("solrPassword");
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(authScope, credentials);
            //log.info("Creating Basic Auth credentials for user {}", user);

            return credentialsProvider.getCredentials(authScope);
        }

    }


}
