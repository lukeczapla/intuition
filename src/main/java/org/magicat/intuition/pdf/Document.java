package org.mskcc.knowledge.pdf;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.text.TextPosition;
//import org.apache.solr.common.util.Pair;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.mskcc.knowledge.model.Article;
import org.mskcc.knowledge.model.FullText;
import org.mskcc.knowledge.repository.FullTextRepository;
import org.mskcc.knowledge.util.SolrClientTool;
import org.mskcc.knowledge.util.SpellChecking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@EqualsAndHashCode
public class Document implements Serializable {

    private final static Logger log = LoggerFactory.getLogger(Document.class);

    @Serial
    private static final long serialVersionUID = 12345678939L;

    private List<Page> pages = new ArrayList<>();
    private transient Article article = null;
    private List<Section> sections = new ArrayList<>();
    private List<Section> nonTableFigureSections = null;
    private List<Section> tableFigureSections = null;
    private Map<Integer, List<ImageItem>> imageMap = null;
    private transient SpellChecking spellChecking = null;

    private String title, pubAbstract, authors;
    private boolean skipAbstract = false, abstractPage2 = false, tagsAdded = false;

    private float titleY = -1.0f, abstractY = -1.0f;


    public Document() {

    }

    public Document(Article article, List<Page> pages) {
        this.article = article;
        this.pages = pages;
        if (article != null) {
            this.title = article.getTitle();
            this.pubAbstract = article.getPubAbstract();
            this.authors = article.getAuthors();
            skipAbstract = article.getPubAbstract() == null || article.getPubAbstract().length() < 10;
        }
    }

    public Document(Article article, List<Page> pages, Map<Integer, List<ImageItem>> imageMap) {
        this(article, pages);
        this.imageMap = imageMap;
        spellChecking = new SpellChecking();
    }

    public Document(Article article, List<Page> pages, Map<Integer, List<ImageItem>> imageMap, SpellChecking spellChecking) {
        this(article, pages);
        this.imageMap = imageMap;
        this.spellChecking = spellChecking;
    }

    public void reload(Article article, List<Page> pages, Map<Integer, List<ImageItem>> imageMap) {
        this.article = article;
        this.pages = pages;
        this.imageMap = imageMap;
        this.sections = new ArrayList<>();
        if (article != null) {
            this.title = article.getTitle();
            this.pubAbstract = article.getPubAbstract();
            this.authors = article.getAuthors();
            skipAbstract = article.getPubAbstract() == null || article.getPubAbstract().length() < 10;
        }
    }

    public static Document readDocument(Article article, FullTextRepository fullTextRepository, GridFsTemplate gridFsTemplate) {
        if (article == null || article.getPmId() == null) {
            log.error("Document lacks PMID to write to database");
            return null;
        }
        String pmid = article.getPmId();
        try {
            Optional<FullText> oft = fullTextRepository.findById(pmid);
            FullText ft;
            if (oft.isPresent()) ft = oft.get();
            else {
                log.error("No FullText collection item with PMID {}", pmid);
                return null;
            }
            if (ft.getDocumentResourceId() == null) {
                log.error("FullText Document for PMID {} lacks a saved serialized Object, has a null documentResourceId", pmid);
                return null;
            }
            String resourceId = ft.getDocumentResourceId();
            GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceId)));
            if (file != null) {
                log.info("SUCCESS: Reading serialized Document object");
                ObjectInputStream oi = new ObjectInputStream(gridFsTemplate.getResource(file).getContent());
                return (Document)oi.readObject();
            }
            log.error("FullText Document for PMID {} contains no gridFS data", pmid);
            return null;
        } catch (IOException | ClassNotFoundException e) {
            log.error("Exception reading serialized Document object: {}", e.getMessage());
            return null;
        }
    }

    public void writeDocument(FullTextRepository fullTextRepository, GridFsTemplate gridFsTemplate) {
        if (article == null || article.getPmId() == null) {
            log.error("Document lacks PMID to write to database");
            return;
        }
        String pmid = article.getPmId();
        String code = SolrClientTool.randomId(10);
        String fileName = "serialization-" + code + ".bin";
        try {
            FileOutputStream f = new FileOutputStream(fileName);
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(this);
            o.close();
            f.close();
            Optional<FullText> oft = fullTextRepository.findById(pmid);
            FullText ft;
            if (oft.isPresent()) ft = oft.get();
            else {
                ft = new FullText();
                ft.setPmId(pmid);
            }
            Binary binary = new Binary(Files.readAllBytes(new File(fileName).toPath()));
            ObjectId oid = gridFsTemplate.store(new ByteArrayInputStream(binary.getData()), fileName, "application/octet-stream");
            ft.setDocumentResourceId(oid.toString());
            log.info("Document saved with resourceId {}", oid);
            fullTextRepository.save(ft);
        } catch (IOException e) {
            log.error("Could not serialize document: {}", e.getMessage());
            //return;
        }
        if (!(new File(fileName).delete())) {
            log.error("Could not delete serialization file {}", fileName);
        }
    }

    public static int bestCommonAlignment(String matcher, String matched) {
        int best = 1000;
        if (matcher.length() > matched.length()) return best;
        for (int i = 0; i < matched.length()-matcher.length(); i++) {
            int x = StringUtils.getLevenshteinDistance(matcher, matched.substring(i, i+matcher.length()));
            if (x < best) best = x;
        }
        //System.out.println(best);
        return best;
    }

    public void stripArticleData() {
        log.info("stripArticleData()");
        if (getPages() == null || getPages().size() == 0) {
            log.error("Cannot strip article data, no pages read yet!");
            return;
        }
        if (article == null) {
            log.error("Cannot strip article data, no Article item 'article' set");
            return;
        }
        Page first = getPages().get(0);
        List<TextRectangle> items = first.getText();
        List<TextRectangle> visited = new ArrayList<>();
        String abstractText = pubAbstract.replace("\n", "").replace(" ", "").toLowerCase();
        String titleText = title.replace(" ", "").toLowerCase();
        int abstractLength = abstractText.length();
        int titleLength = titleText.length();
        for (int i = 0; i < items.size(); i++) {
            TextRectangle item = items.get(i);
            if (item == null || item.isOmittable() || visited.contains(item)) continue;
            List<TextRectangle> lineItems = TextRectangle.getNeighbors(item, first.getOrderText());
            String text = TextRectangle.composeNormal(lineItems);
            List<TextRectangle> header = TextRectangle.splitFontChange(lineItems);
            if (header != null) {
                text = TextRectangle.composeNormal(TextRectangle.splitFontChangeRemainder(lineItems, header));
            }
            visited.addAll(lineItems);
            if ((titleLength > 3 && titleText.contains(text.replace(" ", "").toLowerCase())) || (text.replace(" ", "").length() >= 20 && bestCommonAlignment(text.replace(" ", "").toLowerCase(), titleText.replace(" ", "").toLowerCase()) < 4))  {
                titleLength -= text.replace(" ", "").length();
                log.info("Title: {}", text.replace(" ", ""));
                lineItems.forEach(x -> x.setOmittable(true));
                titleY = lineItems.get(0).getTopY();
                boolean found;
                do {
                    found = false;
                    List<TextRectangle> nextLineItems = TextRectangle.getNeighborsAdjacent(item, first.getOrderText(), false);
                    if (nextLineItems == null) continue;
                    String nextLineText = TextRectangle.composeNormal(nextLineItems);
                    if ((titleText.contains(nextLineText.replace(" ", "").toLowerCase())) || (text.replace(" ", "").length() >= 20 && bestCommonAlignment(text.replace(" ", "").toLowerCase(), titleText.replace(" ", "").toLowerCase()) < 4)) {
                        log.info("Title: {}", nextLineText.replace(" ", ""));
                        titleLength -= nextLineText.replace(" ", "").length();
                        nextLineItems.forEach(x -> x.setOmittable(true));
                        visited.addAll(nextLineItems);
                        found = true;
                    }
                    item = nextLineItems.get(0);
                } while (found && titleLength > 3);
            }
            /*if (authors.replace(" ", "").toLowerCase().contains(text.replace(" ", "").toLowerCase())) {
               lineItems.forEach(x -> x.setOmittable(true));
               boolean found;
               do {
                   found = false;
                   List<TextRectangle> nextLineItems = TextRectangle.getNeighborsAdjacent(item, first.getOrderText(), false);
                   if (nextLineItems == null) continue;
                   String nextLineText = TextRectangle.composeNormal(nextLineItems);
                   if (authors.replace(" ", "").toLowerCase().contains(nextLineText.replace(" ", "").toLowerCase())) {
                       nextLineItems.forEach(x -> x.setOmittable(true));
                       visited.addAll(nextLineItems);
                       found = true;
                   }
                   item = nextLineItems.get(0);

               } while (found);
            }*/
            if (text.endsWith("-")) text = text.substring(0, text.length()-1);
            if (!skipAbstract && text.length() > 10 && abstractLength > 5 && abstractText.contains(text.replace(" ", "").toLowerCase()) || (text.replace(" ", "").length() >= 20 && bestCommonAlignment(text.replace(" ", "").toLowerCase(), pubAbstract.replace(" ", "").toLowerCase()) < 4)) {
                log.info("Found abstract: {}", text.replace(" ", ""));
                abstractLength -= text.replace(" ", "").length();
                lineItems.forEach(x -> x.setOmittable(true));
                abstractY = lineItems.get(0).getTopY();
                boolean found;
                do {
                    found = false;
                    List<TextRectangle> nextLineItems = TextRectangle.getNeighborsAdjacent(item, first.getOrderText(), false);
                    if (nextLineItems == null) continue;
                    text = TextRectangle.composeNormal(nextLineItems);
                    if (text.endsWith("-")) text = text.substring(0, text.length());
                    if (abstractText.contains(text.replace(" ", "").toLowerCase()) || (text.replace(" ", "").length() >= 20 && bestCommonAlignment(text.replace(" ", "").toLowerCase(), pubAbstract.replace(" ", "").toLowerCase()) < 3)) {
                        abstractLength -= text.replace(" ", "").length();
                        nextLineItems.forEach(x -> x.setOmittable(true));
                        if (nextLineItems.get(0).getTopY()+nextLineItems.get(0).getFontSize() > abstractY) abstractY = nextLineItems.get(0).getTopY() + nextLineItems.get(0).getFontSize();
                        visited.addAll(nextLineItems);
                        found = true;
                        log.info("Found abstract: {}", text.replace(" ", ""));
                    }
                    //System.out.println(abstractLength + " " + found);
                    item = nextLineItems.get(0);

                } while (found && abstractLength > 5);
            }
        }
        int originalAbstract = pubAbstract.replace("\n", "").replace(" ", "").toLowerCase().length();
        if ((abstractLength > 5 && abstractLength <= originalAbstract)) {
            //visited.clear();
            first = pages.get(1);
            items = first.getText();
            for (int i = 0; i < items.size() && abstractLength > 5; i++) {
                TextRectangle item = items.get(i);
                if (item == null || item.isOmittable() || visited.contains(item)) continue;
                List<TextRectangle> lineItems = TextRectangle.getNeighbors(item, first.getOrderText());

                String text = TextRectangle.composeNormal(lineItems);
                String text2 = TextRectangle.compose(lineItems).replace("^^", "").replace("__", "");
                List<TextRectangle> header = TextRectangle.splitFontChange(lineItems);
                if (header != null) {
                    text = TextRectangle.composeNormal(TextRectangle.splitFontChangeRemainder(lineItems, header));
                }
                visited.addAll(lineItems);
                if ((text.length() > 10 && pubAbstract.replace("\n", "").replace(" ", "").toLowerCase().contains(text.replace(" ", "").toLowerCase()) || (text.replace(" ", "").length() >= 20 && bestCommonAlignment(text.replace(" ", "").toLowerCase(), pubAbstract.replace(" ", "").toLowerCase()) < 3))
                || (text2.length() > 10 && pubAbstract.replace("\n", "").replace(" ", "").toLowerCase().contains(text2.replace(" ", "").toLowerCase()) || (text2.replace(" ", "").length() >= 20 && bestCommonAlignment(text2.replace(" ", "").toLowerCase(), pubAbstract.replace(" ", "").toLowerCase()) < 3))) {
                    log.info("Found abstract on page 2 outside: {}", text.replace(" ", ""));
                    abstractY = lineItems.get(0).getTopY() + lineItems.get(0).getFontSize();
                    abstractPage2 = true;
                    lineItems.forEach(x -> x.setOmittable(true));
                    boolean found;
                    do {
                        abstractLength -= text.replace(" ", "").length();
                        found = false;
                        List<TextRectangle> nextLineItems = TextRectangle.getNeighborsAdjacent(item, first.getOrderText(), false);
                        if (nextLineItems == null) continue;
                        item = nextLineItems.get(0);
                        text = TextRectangle.composeNormal(nextLineItems);
                        text2 = TextRectangle.compose(nextLineItems).replace("^^", "").replace("__", "");
                        visited.addAll(nextLineItems);
                        if (text.endsWith("-")) text = text.substring(0, text.length()-1);
                        if (text2.endsWith("-")) text2 = text2.substring(0, text2.length()-1);
                        if (pubAbstract.replace(" ", "").toLowerCase().contains(text.replace(" ", "").toLowerCase()) || (text.replace(" ", "").length() >= 20 && bestCommonAlignment(text.replace(" ", "").toLowerCase(), pubAbstract.replace(" ", "").toLowerCase()) < 4)
                        || (pubAbstract.replace(" ", "").toLowerCase().contains(text2.replace(" ", "").toLowerCase()) || (text2.replace(" ", "").length() >= 20 && bestCommonAlignment(text2.replace(" ", "").toLowerCase(), pubAbstract.replace(" ", "").toLowerCase()) < 4))) {
                            log.info("Found abstract on page 2 inside: {}", text.replace(" ", ""));
                            nextLineItems.forEach(x -> x.setOmittable(true));
                            found = true;
                            if (nextLineItems.get(0).getTopY()+nextLineItems.get(0).getFontSize() > abstractY) abstractY = nextLineItems.get(0).getTopY() + nextLineItems.get(0).getFontSize();
                            abstractPage2 = true;
                        }
                    } while (found && abstractLength > 5);
                }
            }
        }
    }

    @Getter
    @Setter
    private static class Stack {
        private List<Section> sections = new ArrayList<>();
        private List<String> paragraphs = new ArrayList<>();

        public void push(Section section, String paragraph) {
            sections.add(section);
            paragraphs.add(paragraph);
        }

        public Section pop() {
            if (isEmpty()) {
                log.error("Empty Stack object, cannot call pop() - returning null");
                return null;
            }
            Section result = sections.remove(sections.size()-1);
            result.getParagraphs().add(paragraphs.remove(paragraphs.size()-1));
            return result;
        }

        public Section getTopSection() {
            if (sections.isEmpty()) return null;
            return sections.get(sections.size()-1);
        }

        public String getTopParagraph() {
            if (paragraphs.isEmpty()) return null;
            return paragraphs.get(paragraphs.size()-1);
        }

        public Section getPrevSection() {
            if (sections.size() < 2) return null;
            return sections.get(sections.size()-2);
        }

        public void set(Section section, String paragraph) {
            sections.set(sections.size()-1, section);
            paragraphs.set(paragraphs.size()-1, paragraph);
        }

        public boolean isEmpty() {
            return sections.size() == 0;
        }
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return (float)Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    }

    private String addTextParagraph(String paragraph, String lineText) {
        if (paragraph == null || paragraph.length() == 0) return lineText;
        else if (paragraph.charAt(paragraph.length()-1) != ' ') {
            if (paragraph.charAt(paragraph.length()-1) == '-') {
                if (paragraph.length() > 1 && Character.toUpperCase(paragraph.charAt(paragraph.length()-2)) == paragraph.charAt(paragraph.length()-2))
                    return paragraph + lineText;
                else if (!spellChecking.checkSpelling((paragraph.substring(paragraph.lastIndexOf(" ")+1, paragraph.length()-1)+lineText.substring(0, (lineText.contains(" ") ? lineText.indexOf(" "):lineText.length()))).toLowerCase()) && !spellChecking.checkSpelling(paragraph.substring(paragraph.lastIndexOf(" ")+1, paragraph.length()-1)+lineText.substring(0, (lineText.contains(" ") ? lineText.indexOf(" "): lineText.length()))))
                    return paragraph + lineText;
                else return paragraph.substring(0, paragraph.length()-1) + lineText;
            } else return paragraph + " " + lineText;
        } else return paragraph + lineText;
    }

    private static double computeSimilarity(String s1, String s2) {
        int length = Math.max(s1.length(), s2.length());
        int dist = StringUtils.getLevenshteinDistance(s1, s2);
        return 100.0*(length-dist)/length;
    }

    public static boolean isNumbered(String input) {
        input = input.trim();
        Pattern p = Pattern.compile("[\\d]{1,3}[.]");
        Matcher m = p.matcher(input);
        //return m.find() && m.start() == 0 && !input.startsWith("0");
        if (!m.find() || m.start() != 0 || input.startsWith("0")) return false;
        p = Pattern.compile("10.[\\d]{4}"); // DOI codes in references
        m = p.matcher(input);
        if (m.find() && m.start() == 0) return false;
        return true;
    }

    private void removeHeaderFooter() {
        List<Integer> headerOrders = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        List<Integer> footerOrders = new ArrayList<>();
        List<String> footers = new ArrayList<>();
        for (Page page: getPages()) {
            float ymin = page.getOrderText().get(0).getTopY(), ymax = page.getOrderText().get(0).getTopY();
            int order = 0, order2 = 0;
            for (Integer o : page.getOrderText().keySet()) {
                if (page.getOrderText().get(o).getTopY() < ymin) {
                    ymin = page.getOrderText().get(o).getTopY();
                    order = o;
                }
                if (page.getOrderText().get(o).getTopY() > ymax) {
                    ymax = page.getOrderText().get(o).getTopY();
                    order2 = o;
                }
            }
            headerOrders.add(order);
            headers.add(TextRectangle.compose(TextRectangle.getNeighbors(page.getOrderText().get(order), page.getOrderText())));
            footerOrders.add(order2);
            footers.add(TextRectangle.compose(TextRectangle.getNeighbors(page.getOrderText().get(order2), page.getOrderText())));
        }
        for (int i = 0; i < pages.size()-1; i++) {
            if (computeSimilarity(headers.get(i), headers.get(i+1)) > 75.0) {
                TextRectangle.setOmittables(TextRectangle.getNeighbors(pages.get(i).getOrderText().get(headerOrders.get(i)), pages.get(i).getOrderText()));
                TextRectangle.setOmittables(TextRectangle.getNeighbors(pages.get(i+1).getOrderText().get(headerOrders.get(i+1)), pages.get(i+1).getOrderText()));
            }
            if (computeSimilarity(footers.get(i), footers.get(i+1)) > 75.0) {
                TextRectangle.setOmittables(TextRectangle.getNeighbors(pages.get(i).getOrderText().get(footerOrders.get(i)), pages.get(i).getOrderText()));
                TextRectangle.setOmittables(TextRectangle.getNeighbors(pages.get(i+1).getOrderText().get(footerOrders.get(i+1)), pages.get(i+1).getOrderText()));
            }
        }
        for (int i = 0; i < pages.size()-2; i++) {
            if (computeSimilarity(headers.get(i), headers.get(i+2)) > 75.0) {
                TextRectangle.setOmittables(TextRectangle.getNeighbors(pages.get(i).getOrderText().get(headerOrders.get(i)), pages.get(i).getOrderText()));
                TextRectangle.setOmittables(TextRectangle.getNeighbors(pages.get(i+2).getOrderText().get(headerOrders.get(i+2)), pages.get(i+2).getOrderText()));
            }
            if (computeSimilarity(footers.get(i), footers.get(i+2)) > 75.0) {
                TextRectangle.setOmittables(TextRectangle.getNeighbors(pages.get(i).getOrderText().get(footerOrders.get(i)), pages.get(i).getOrderText()));
                TextRectangle.setOmittables(TextRectangle.getNeighbors(pages.get(i+2).getOrderText().get(footerOrders.get(i+2)), pages.get(i+2).getOrderText()));
            }
        }
        //StringUtils.getLevenshteinDistance()
    }

    public void annotate() {
        log.info("annotate()");
        Stack stack = new Stack();
        sections = new ArrayList<>();
        Section top = new Section("No Header");
        sections.add(top);
        stack.push(top, "");
        List<Float> pageOneY = new ArrayList<>();
        List<TextRectangle> previousLine = null;
        String previous = "";
        boolean currentTable = false;
        boolean wasNumbered = false;
        boolean lastTableLine = false;
        List<TextRectangle> visited = new ArrayList<>();
        Page pOne = getPages().get(0);
        for (int j = 0; j < pOne.getItems(); j++) {
            TextRectangle rectangle = pOne.getOrderText().get(j);
            if (rectangle == null) continue;
            List<TextRectangle> line = TextRectangle.getNeighbors(rectangle, pOne.getOrderText());
            log.info("Page1: {} {}", TextRectangle.compose(line), rectangle.isOmittable());
            if (TextRectangle.lineWidth(line) > 0.7*pOne.getWidth() && rectangle.getTopY() > 0.7*pOne.getHeight() && line.get(0).getFontSize() <= 8.0f) {
                log.info("Added to PageOneY list: {} y = {}", TextRectangle.compose(line), line.get(0).getTopY());
                pageOneY.add(rectangle.getTopY());
            }
        }
        removeHeaderFooter();
        for (int i = abstractPage2 ? 1: 0; i < getPages().size(); i++) {
            Page page = getPages().get(i);
            //page.assignRotation();
            List<ImageItem> imageList = null;
            boolean[] possibleDuplicate = null;
            if (imageMap != null && imageMap.get(i) != null) {
                imageList = imageMap.get(i);
                possibleDuplicate = new boolean[imageList.size()];
                for (int k = 0; k < imageList.size(); k++) {
                    for (int j = 0; j < getPages().size(); j++) {
                        if (j == i) continue;
                        if (imageMap != null && imageMap.get(j) != null) {
                            List<ImageItem> imageListJ = imageMap.get(j);
                            ImageItem item1 = imageList.get(k);
                            for (ImageItem item2 : imageListJ) {
                                if (Math.abs(item1.getTopX() - item2.getTopX()) < 1.0f && Math.abs(item1.getTopY() - item2.getTopY()) < 1.0f && item1.getWidth() == item2.getWidth() && item1.getHeight() == item2.getHeight()) {
                                    possibleDuplicate[k] = true;
                                    log.info("Possible duplicate image {} on page {} starting with 0", k, i);
                                    break;
                                }
                            }
                            if (possibleDuplicate[k]) break;
                        }
                    }
                }
            }

            log.info("Page {} Items {}", i, page.getItems());
            List<List<TextRectangle>> previousTableLines = new ArrayList<>();
            for (int j = 0; j < page.getItems(); j++) {
                TextRectangle rectangle = page.getOrderText().get(j);
                if (rectangle == null) continue;

                if (rectangle.isOmittable() || visited.contains(rectangle)) {
                    if (i == 0 && rectangle.isOmittable()) log.info("Omittable {}", rectangle);
                    continue;
                }
                if (rectangle.getRotation() != page.getRotation()) {
                    rectangle.setOmittable(true);
                    List<TextRectangle> line = TextRectangle.getNeighbors(rectangle, page.getOrderText());
                    if (i == 0) log.info("Omittable {}", TextRectangle.compose(line));
                    TextRectangle.setOmittables(line);
                    continue;
                }
                if (titleY >= 0.0f && i == 0 && rectangle.getTopY() < titleY) {
                    System.out.println("above title: " + TextRectangle.compose(TextRectangle.getNeighbors(rectangle, page.getOrderText())));
                    continue;
                }
                if (abstractY >= 0.0f && i == (abstractPage2 ? 1 : 0) && rectangle.getTopY() < abstractY) {
                    System.out.println("above abstract:" + TextRectangle.compose(TextRectangle.getNeighbors(rectangle, page.getOrderText())));
                    continue;
                }

                List<TextRectangle> line = TextRectangle.getNeighbors(rectangle, page.getOrderText());

                if (i == 0) {
                    boolean found = false;
                    for (Float y : pageOneY) {
                        if (rectangle.getBottomY() >= y) {  // changed TopY to BottomY
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        TextRectangle.setOmittables(line);
                        log.info("Omittable below pageOneY: {}", TextRectangle.compose(line));
                        continue;
                    }
                }

                if (TextRectangle.isOmittableTexts(line, i)) continue;
                List<TextRectangle> header = TextRectangle.splitFontChange(line);
                String title;
                String lineText = TextRectangle.compose(line);
               /* if (rectangle.getText().equals("Mutantsc") || rectangle.getText().equals("disease),66")) {
                    System.out.println("GOT YA " + rectangle.getText());
                    System.out.println(rectangle);
                    for (TextPosition t : rectangle.getTextPositions()) {
                        System.out.println(t.toString() + " fontsize:" + t.getFontSizeInPt() + " font:" + t.getFont().getName() + " y=" + t.getY() + " yend=" + (t.getY()+t.getFont().getFontDescriptor().getCapHeight()/1000*t.getFontSizeInPt()));
                    }
                }*/
                Pair<String, String> figureTableTitle = Page.figureTable(lineText);
                if (((title = page.toSection(rectangle.getOrder())) != null && (previousLine == null || !TextRectangle.commonFont(previousLine, header != null ? header: line))) || (figureTableTitle != null && previousLine != null && !TextRectangle.commonFont(previousLine, line))) {
                    wasNumbered = false;
                    if (title == null) title = figureTableTitle.first();
                    if (currentTable && (title.toLowerCase().startsWith("table") || title.toLowerCase().startsWith("fig"))) {
                        stack.pop();
                    }
                    else if (stack.getSections().size() > 1 && (!title.toLowerCase().startsWith("table") && !title.toLowerCase().startsWith("fig"))) {
                        stack.pop();
                        currentTable = false;
                    } else if (stack.getSections().size() > 1) currentTable = true;
                    //log.info("Section: {} is a currentTable: {}", title, currentTable);
                    Section section = new Section(title);
                    if (title.toLowerCase().startsWith("table")) {
                        section.setTable(true);
                        //lineText = TextRectangle.composeTable(line).first(); // NEW!!
                    }

                    if (currentTable && title.toLowerCase().startsWith("fig") && imageMap != null) {
                        if (imageList != null && imageList.size() == 1) {
                            section.setImage(imageList.remove(0));
                        } else if (imageList != null && imageList.size() > 1) {
                            int start = 0;
                            if (possibleDuplicate[0]) start = 1;
                            float distMin = distance(line.get(0).getTopX(), line.get(0).getTopY(), imageList.get(start).getTopX(), imageList.get(start).getTopY());
                            int index = start;
                            for (int k = 1; k < imageList.size(); k++) {
                                if (k == start || possibleDuplicate[k]) continue;
                                float dist = distance(line.get(0).getTopX(), line.get(0).getTopY(), imageList.get(k).getTopX(), imageList.get(k).getTopY());
                                if (dist < distMin) {
                                    distMin = dist;
                                    index = k;
                                }
                            }
                            section.setImage(imageList.remove(index));
                            for (int k = index; k < possibleDuplicate.length-1; k++) {
                                if (possibleDuplicate[k+1]) possibleDuplicate[k] = true;
                            }
                        }
                    }
                    String paragraph = "";
                    if (header != null || figureTableTitle != null) {
                        if (header != null) {
                            List<TextRectangle> remainder = TextRectangle.splitFontChangeRemainder(line, header);
                            paragraph = TextRectangle.compose(remainder);
                        } else paragraph = figureTableTitle.second();
                        final Section sx = section;
                        if (header != null) header.forEach(x -> x.getTextPositions().forEach(y -> sx.getFontNames().add(y.getFont().getName() + y.getFontSizeInPt())));
                        section.getWidths().add(TextRectangle.lineWidth(line));
                    }
                    previous = "Header";
                    sections.add(section);
                    stack.push(section, paragraph);
                    visited.addAll(line);
                } else { // Not a title of a section, just more text
                    if (previousLine != null && !isNumbered(lineText) && !wasNumbered && previous.equals(stack.getTopSection().getHeading()) && previousLine.get(0).getPage() == line.get(0).getPage() && (TextRectangle.isIndented(previousLine, line) || stack.getTopSection().bigDistance(previousLine, line, page.getRotation()))) {
                        Section section = stack.getTopSection();
                        String paragraph = stack.getTopParagraph();
                        //String lineText = TextRectangle.compose(line);
                        if (currentTable) {
                            Section last = stack.getTopSection();
                            if (last.getFontNames() != null && last.getFontNames().size() > 2) {
                                boolean found = false;
                                // new
                                final List<String> lineFonts = new ArrayList<>();
                                line.forEach(x -> x.getTextPositions().forEach(y -> lineFonts.add(y.getFont().getName()+y.getFontSizeInPt())));
                                // new
                                //String lineFont = line.get(0).getFontName() + line.get(0).getFontSize(); // old
                                for (String font : last.getFontNames()) {
                                    for (String lineFont: lineFonts) {
                                        if (font.equals(lineFont)) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (found) break;
                                }
                                if (!found && stack.getPrevSection() != null) {
                                    for (String font: stack.getPrevSection().getFontNames()) {
                                        for (String lineFont: lineFonts) {
                                            if (font.equals(lineFont)) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (found) break;
                                    }
                                    if (found) {
                                        stack.pop();
                                        log.info("IPop on top is Section {}", stack.getTopSection().getHeading());
                                        line.get(0).setIndented(false);
                                        section = stack.getTopSection();
                                        previous = section.getHeading();
                                        currentTable = section.getHeading().toLowerCase().startsWith("table") || section.getHeading().toLowerCase().startsWith("fig");
                                        paragraph = stack.getTopParagraph();
                                        if (section.getPreviousLine() != null && !TextRectangle.commonFont(section.getPreviousLine(), line) && paragraph != null && paragraph.length() > 0) {
                                            section.getParagraphs().add(paragraph);
                                            stack.set(section, lineText);
                                        } else {
                                            paragraph = addTextParagraph(paragraph, lineText);
                                            stack.set(section, paragraph);
                                        }
                                        //paragraph = addTextParagraph(paragraph, lineText);
                                        final Section sx = section;
                                        line.forEach(x -> sx.getFontNames().add(x.getFontName() + x.getFontSize()));
                                        sx.setPreviousLine(previousLine);
                                        //stack.set(section, paragraph);
                                        visited.addAll(line);
                                        previousLine = line;
                                        continue;
                                    }
                                }
                            }
                            if (section.isTable()) {
                                lineText = TextRectangle.composeTable(line);
                            }
                            if (section.isTable() && TextRectangle.isTable(line)) {
                                if (paragraph == null || paragraph.length() == 0) {
                                    paragraph = lineText;
                                    stack.set(section, paragraph);
                                }
                                else if (!lastTableLine) {
                                    section.getParagraphs().add(paragraph);
                                    stack.set(section, lineText);
                                }
                                else {
                                    //paragraph += "\n" + lineText;
                                    // TESTING! LINE BELOW
                                    paragraph = TextRectangle.adjustTableLines(previousTableLines, line);
                                    stack.set(section, paragraph);
                                }
                                final Section sx = section;
                                line.forEach(x -> x.getTextPositions().forEach(y -> sx.getFontNames().add(y.getFont().getName() + y.getFontSizeInPt())));
                                //section.getFontNames().add(line.get(0).getFontName() + line.get(0).getFontSize());
                                sx.setPreviousLine(previousLine);
                                section.getWidths().add(TextRectangle.lineWidth(line));
                                visited.addAll(line);
                                previousLine = line;
                                previous = section.getHeading();
                                lastTableLine = true;
                                previousTableLines.add(previousLine);
                                continue;
                            } else {
                                lineText = TextRectangle.compose(line);  // trying!
                                lastTableLine = false;
                                previousTableLines.clear();
                            }
                        }
                        final Section sx = section;
                        line.forEach(x -> sx.getFontNames().add(x.getFontName()+x.getFontSize()));
                        sx.setPreviousLine(previousLine);
                        if (paragraph != null && paragraph.length() > 0) section.getParagraphs().add(paragraph);
                        stack.set(section, lineText);
                    } else {  // not indented, could be new page
                        Section section = stack.getTopSection();
                        String paragraph = stack.getTopParagraph();
                        //String lineText = TextRectangle.compose(line);
                        if (currentTable) {
                            //if (lineText.startsWith("S6/S7 α-sheet")) {
                            //    System.out.println("GOT YA");
                            //}
                            Section last = stack.getTopSection();
                            if (last.getFontNames() != null && last.getFontNames().size() > 2) {
                                boolean found = false;
                                final List<String> lineFonts = new ArrayList<>();
                                line.forEach(x -> x.getTextPositions().forEach(y -> lineFonts.add(y.getFont().getName()+y.getFontSizeInPt())));
                                for (String font : last.getFontNames()) {
                                    for (String lineFont: lineFonts) {
                                        if (font.equals(lineFont)) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (found) break;
                                }
                                if (!found && stack.getPrevSection() != null) {
                                    for (String font: stack.getPrevSection().getFontNames()) {
                                        for (String lineFont: lineFonts) {
                                            if (font.equals(lineFont)) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (found) break;
                                    }
                                    if (found) {
                                        log.info("NPop before is Section {}", stack.getTopSection().getHeading());
                                        stack.pop();
                                        log.info("NPop on top is Section {}", stack.getTopSection().getHeading());
                                        section = stack.getTopSection();
                                        previous = section.getHeading();
                                        currentTable = section.getHeading().toLowerCase().startsWith("table") || section.getHeading().toLowerCase().startsWith("fig");
                                        paragraph = stack.getTopParagraph();
                                        // NEW 3/8
                                        if (section.getPreviousLine() != null && !TextRectangle.commonFont(section.getPreviousLine(), line) && paragraph != null && paragraph.length() > 0) {
                                            section.getParagraphs().add(paragraph);
                                            stack.set(section, lineText);
                                        } else {
                                            paragraph = addTextParagraph(paragraph, lineText);
                                            stack.set(section, paragraph);
                                        }
                                        final Section sx = section;
                                        line.forEach(x -> sx.getFontNames().add(x.getFontName() + x.getFontSize()));
                                        sx.setPreviousLine(previousLine);
                                        visited.addAll(line);
                                        previousLine = line;
                                        continue;
                                    }
                                }
                            }
                            if (section.isTable()) {
                                lineText = TextRectangle.composeTable(line);
                            }
                            if (section.isTable() && TextRectangle.isTable(line)) {
                                if (paragraph == null || paragraph.length() == 0) {
                                    paragraph = lineText;
                                    stack.set(section, paragraph);
                                }
                                else if (!lastTableLine) {
                                    section.getParagraphs().add(paragraph);
                                    stack.set(section, lineText);
                                }
                                else {
                                    //paragraph += "\n" + lineText;
                                    // TESTING! Line below
                                    paragraph = TextRectangle.adjustTableLines(previousTableLines, line);
                                    stack.set(section, paragraph);
                                }
                                final Section sx = section;
                                line.forEach(x -> sx.getFontNames().add(x.getFontName() + x.getFontSize()));
                                sx.setPreviousLine(previousLine);
                                section.getWidths().add(TextRectangle.lineWidth(line));
                                visited.addAll(line);
                                previous = section.getHeading();
                                previousLine = line;
                                lastTableLine = true;
                                previousTableLines.add(previousLine);
                                continue;
                            } else {
                                //previous = section.getHeading();
                                if (section.isTable() && paragraph.contains("\n")) {
                                    section.getParagraphs().add(paragraph);
                                    section.setPreviousLine(previousLine);
                                    lineText = TextRectangle.compose(line); // trying!
                                    stack.set(section, lineText);
                                    previous = section.getHeading();
                                    previousLine = line;
                                    visited.addAll(line);
                                    previousTableLines.clear();
                                    lastTableLine = false;
                                    continue;
                                } else paragraph = addTextParagraph(paragraph, lineText);
                            }
                        }
                        else { // not indented, not table
                            if (previousLine != null && !isNumbered(lineText) && TextRectangle.commonFont(previousLine, line)) {
                                paragraph = addTextParagraph(paragraph, lineText);
                            }
                            else if ((paragraph != null && paragraph.length() > 0) || isNumbered(lineText)) {
                                if (!isNumbered(lineText)) {
                                    wasNumbered = false;
                                    if (TextRectangle.commonFont(previousLine, line)) {
                                        paragraph = addTextParagraph(paragraph, lineText);
                                        stack.set(section, paragraph);
                                    } else {
                                        section.getParagraphs().add(stack.getTopParagraph());
                                        stack.set(section, lineText);
                                    }
                                    //section.getParagraphs().add(stack.getTopParagraph());
                                    //stack.set(section, lineText);
                                } else {
                                    wasNumbered = true;
                                    // TEST STUFF (3/7) after && on next line. works!
                                    if (paragraph.length() > 0 && (previous.toLowerCase().startsWith("ref") || !TextRectangle.commonFont(previousLine, line)) || (header != null && isNumbered(TextRectangle.compose(header)))) {
                                        section.getParagraphs().add(stack.getTopParagraph());
                                        stack.set(section, lineText);
                                    } else {
                                        paragraph = addTextParagraph(paragraph, lineText);
                                        stack.set(section, paragraph);
                                    }
                                }
                                previousLine = line;
                                visited.addAll(line);
                                previous = section.getHeading();
                                section.setPreviousLine(previousLine);
                                continue;
                            } else {
                                paragraph = addTextParagraph(paragraph, lineText);
                            }
                        }
                        final Section sx = stack.getTopSection();
                        line.forEach(x -> sx.getFontNames().add(x.getFontName()+x.getFontSize()));
                        sx.setPreviousLine(previousLine);
                        sx.getWidths().add(TextRectangle.lineWidth(line));
                        stack.set(sx, paragraph);
                        previous = section.getHeading();
                        wasNumbered = false;
                    }
                    visited.addAll(line);
                }

                previousLine = line;

            }
        }
        Section section = stack.getTopSection();
        String paragraph = stack.getTopParagraph();
        if (paragraph != null && paragraph.length() > 0) section.getParagraphs().add(paragraph);
        for (Section s: sections) s.cleanup();
    }

    public void addTags() {
        List<List<String>> figureTableNames = new ArrayList<>();
        List<Boolean> figureTablePlaced = new ArrayList<>();
        tableFigureSections = new ArrayList<>();
        for (Section section : sections) {
            if (section.getHeading().toLowerCase().startsWith("table") || section.getHeading().toLowerCase().startsWith("fig")) {
                tableFigureSections.add(section);
                List<String> names = new ArrayList<>();
                String heading = section.getHeading();
                if (heading.endsWith(".")) heading = heading.substring(0, heading.length()-1);
                names.add(heading);
                if (heading.toLowerCase().startsWith("fig") && !heading.toLowerCase().contains("figure")) {
                    String synonym = heading;
                    if (synonym.contains(" ")) synonym = "Figure" + synonym.substring(synonym.indexOf(" "));
                    names.add(synonym);
                }
                figureTableNames.add(names);
                figureTablePlaced.add(false);
            }
        }
        nonTableFigureSections = new ArrayList<>();
        for (Section section: sections) {
            if (!section.getHeading().toLowerCase().startsWith("table") && !section.getHeading().toLowerCase().startsWith("fig")) {
                nonTableFigureSections.add(section);
                for (int i = 0; i < figureTableNames.size(); i++) {
                    if (figureTablePlaced.get(i)) continue;
                    for (int j = 0; j < section.getParagraphs().size(); j++) {
                        String paragraph = section.getParagraphs().get(j);
                        for (String name: figureTableNames.get(i)) {
                            if (paragraph.toLowerCase().contains(name.toLowerCase())) {
                                int position = paragraph.toLowerCase().indexOf(name.toLowerCase());
                                paragraph = paragraph.substring(0, position) + "${" + paragraph.substring(position, position+name.length()) + "}" + paragraph.substring(position+name.length());
                                section.getParagraphs().set(j, paragraph);
                                figureTablePlaced.set(i, true);
                                break;
                            }
                        }
                        if (figureTablePlaced.get(i)) break;
                    }
                }
            }
        }
        tagsAdded = true;
    }

    public String toHTML() {
        if (article == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<h1><b>").append(article.getTitle()).append("</b></h1>");
        sb.append("<i>").append(article.getAuthors()).append("</i>");
        sb.append("<h3>").append(article.getCitation()).append("</h3>");
        sb.append("<h3>ABSTRACT</h3><p><i>").append(article.getPubAbstract()).append("</i></p>");
        if (!tagsAdded) addTags();
        for (Section section: nonTableFigureSections) sb.append(section.toHTML(tableFigureSections));
        for (Section section: tableFigureSections) sb.append(section.toHTML());
        return sb.toString();
    }

    public String toString() {
        return (article != null ? title + " " + article.getJournal() + " " + article.getCitation() : "") + " with " + pages.size() + " pages in PDF";
    }

}
