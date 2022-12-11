package org.magicat.util;

import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Replaces Python pubmed.py to query terms and return top number of hits by date (most recent)
 */
public class Pubmed {

    public static List<Integer> getIds(String term, int count) {
        List<Integer> ids = new ArrayList<>();
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=" + URLEncoder.encode(term, StandardCharsets.UTF_8) + "&retmax=" + count + "&sort=date");
            XPath xPath = XPathFactory.newDefaultInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.compile("//Id").evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node n = nodeList.item(i);
                ids.add(Integer.parseInt(n.getTextContent()));
            }
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public static String getXML(List<Integer> ids) {
        StringBuilder xmlData = new StringBuilder();
        List<List<Integer>> partitions = Lists.partition(ids,200);
        for (int count = 0; count < partitions.size(); count++) {
            List<Integer> items = partitions.get(count);
            StringBuilder idList = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                idList.append(items.get(i)).append(i != items.size() - 1 ? "," : "");
            }
            String url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=" + idList + "&rettype=xml";
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .timeout(Duration.of(10, ChronoUnit.SECONDS))
                        .GET()
                        .build();
                HttpClient client = HttpClient.newBuilder().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String result = response.body();
                if (xmlData.length() == 0) {
                    if (count == partitions.size() - 1) xmlData = new StringBuilder(result);
                    else xmlData = new StringBuilder(result.substring(0, result.indexOf("</PubmedArticleSet>")));
                } else {
                    if (count == partitions.size() - 1) xmlData.append(result.substring(result.indexOf("<PubmedArticle>")));
                    else xmlData.append(result, result.indexOf("<PubmedArticle>"), result.indexOf("</PubmedArticleSet>"));
                }
            } catch (URISyntaxException | IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return xmlData.toString();
    }

}
