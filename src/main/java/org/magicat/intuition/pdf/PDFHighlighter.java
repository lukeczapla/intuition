package org.mskcc.knowledge.pdf;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.*;

import lombok.*;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.mskcc.knowledge.model.Article;
import org.mskcc.knowledge.repository.ArticleRepository;
import org.mskcc.knowledge.service.FullTextService;
import org.mskcc.knowledge.util.SpellChecking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ToString
public class PDFHighlighter extends PDFTextStripper {

    private final static Logger log = LoggerFactory.getLogger(PDFHighlighter.class);
    private List<String> terms = new ArrayList<>();
    private List<TextPosition> previous = null;  // holds the value of the previous line of text for spanning words
    private List<TextPosition> previous2 = null;  // used for 3 line spanning amino acids and nucleic acids
    private List<PageText> textp = new ArrayList<>(); // all text positions saved
    private Map<Integer, List<PageText>> pageMap = new HashMap<>(); // mapped by page number
    private int currentPage = -1;
    @Getter
    private Document document;
    @Getter
    private Map<Integer, List<ImageItem>> imageMap;
    private List<Page> pages = new ArrayList<>();
    private FullTextService fullTextService;

    private List<double[]> coordinates = new ArrayList<>();
    private List<String> tokenStream = new ArrayList<>();

    private ArticleRepository articleRepository;

    private boolean printText = false;
    private boolean highlightDNA = false;
    private boolean highlightProteinSequences = false;
    private float[] color;

    private static final String nucleicAcidSequence = "[ATGC]{2}[ATGC]*";
    private static final String nucleicAcidChain = "((5'-)" + nucleicAcidSequence + "(-3'))" + "|" + "((3'-)" + nucleicAcidSequence + "(-5'))";
    private static final String aminoAcidSequence = "[ACDEFGHIKLMNPQRSTVWY]{8}[ACDEFGHIKLMNPQRSTVWY]*";


    public PDFHighlighter(float[] color) throws IOException {
        super();
        this.color = color;
    }

    public PDFHighlighter() throws IOException {
        this(new float[] {0.0f, 1.0f, 1.0f});
    }

    public PDFHighlighter useHighlightDNA() {
        highlightDNA = true;
        return this;
    }

    public PDFHighlighter useHighlightProteinSequences() {
        highlightProteinSequences = true;
        return this;
    }

    public PDFHighlighter usePrintText() {
        printText = true;
        return this;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public void setFullTextService(FullTextService fullTextService) {
        this.fullTextService = fullTextService;
    }

    public void setArticleRepository(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public void highlight(String fileName, String outFileName, List<String> terms) {
        try {
            //Loading an existing document
            File file = new File(fileName);
            PDDocument document = PDDocument.load(file);//Loader.loadPDF(file);
            setTerms(terms);

            //extended PDFTextStripper class
            PDFTextStripper stripper = this;

            //Get number of pages
            int number_of_pages = document.getDocumentCatalog().getPages().getCount();

            //The method writeText will invoke an override version of writeString
            Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            stripper.writeText(document, dummy);

            //Print collected information
            //System.out.println(tokenStream);
            //System.out.println(tokenStream.size());
            //System.out.println(coordinates.size());

            double page_height;
            double page_width;
            double width, height, minx, maxx, miny, maxy;
            int rotation;

            //scan each page and highlight all the words inside them
            for (int page_index = 0; page_index < number_of_pages; page_index++) {
                //get current page
                PDPage page = document.getPage(page_index);

                //Get annotations for the selected page
                List<PDAnnotation> annotations = page.getAnnotations();

                //Define a color to use for highlighting text
                PDColor c = new PDColor(color, PDDeviceRGB.INSTANCE);

                //Page height and width
                page_height = page.getMediaBox().getHeight();
                page_width = page.getMediaBox().getWidth();

                //Scan collected coordinates
                for (int i = 0; i < coordinates.size(); i++) {
                    //if the current coordinates are not related to the current
                    //page, ignore them
                    if ((int)coordinates.get(i)[4] != (page_index + 1)) continue;

                    //get rotation of the page...portrait..landscape..
                    rotation = (int) coordinates.get(i)[7];

                    //page rotation of +90 degrees
                    if (rotation == 90) {
                        height = coordinates.get(i)[5];
                        width = coordinates.get(i)[6];
                        width = (page_height * width) / page_width;

                        //define coordinates of a rectangle
                        maxx = coordinates.get(i)[1];
                        minx = coordinates.get(i)[1] - height;
                        miny = coordinates.get(i)[0];
                        maxy = coordinates.get(i)[0] + width;
                    } else {   // We should add here the cases -90/-180 degrees
                        height = coordinates.get(i)[5];
                        minx = coordinates.get(i)[0];
                        maxx = coordinates.get(i)[2];
                        miny = page_height - coordinates.get(i)[1];
                        maxy = page_height - coordinates.get(i)[3] + height;
                    }

                    //Add an annotation for each scanned word
                    PDAnnotationTextMarkup txtMark = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);// new PDAnnotationHighlight();//new PDAnnotationTextMarkup(PDAnnotationTextMarkup.  .SUB_TYPE_HIGHLIGHT);
                    txtMark.setColor(c);
                    txtMark.setConstantOpacity((float) 1.0); // 100% opaque
                    PDRectangle position = new PDRectangle();
                    position.setLowerLeftX((float) minx);
                    position.setLowerLeftY((float) miny);
                    position.setUpperRightX((float) maxx);
                    position.setUpperRightY((float)(maxy + 2.0*height));
                    txtMark.setRectangle(position);

                    float[] quads = new float[8];
                    quads[0] = position.getLowerLeftX();  // x1
                    quads[1] = position.getUpperRightY() - 2; // y1
                    quads[2] = position.getUpperRightX(); // x2
                    quads[3] = quads[1]; // y2
                    quads[4] = quads[0];  // x3
                    quads[5] = position.getLowerLeftY() - 2; // y3
                    quads[6] = quads[2]; // x4
                    quads[7] = quads[5]; // y5
                    txtMark.setQuadPoints(quads);
                    txtMark.setContents(tokenStream.get(i));
                    annotations.add(txtMark);
                }
            }

            //Saving the document in a new file
            File highlighted_doc = new File(outFileName);
            document.save(highlighted_doc);
            document.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            e.getStackTrace();
        }

    }

    public byte[] highlight(byte[] input, List<String> terms) {
        try {
            //Loading an existing document
            PDDocument document = PDDocument.load(input);//Loader.loadPDF(input);
            setTerms(terms);

            //extended PDFTextStripper class
            PDFTextStripper stripper = this;

            //Get number of pages
            int number_of_pages = document.getDocumentCatalog().getPages().getCount();

            //The method writeText will invoke an overrided version of writeString
            Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());

            // Calls our writeString() method
            stripper.writeText(document, dummy);

            double page_height;
            double page_width;
            double width, height, minx, maxx, miny, maxy;
            int rotation;

            //scan each page and highlight all the words inside them
            for (int page_index = 0; page_index < number_of_pages; page_index++) {
                //get current page
                PDPage page = document.getPage(page_index);


                //Get annotations for the selected page
                List<PDAnnotation> annotations = page.getAnnotations();

                //Define a color to use for highlighting text
                PDColor c = new PDColor(color, PDDeviceRGB.INSTANCE);

                //Page height and width
                page_height = page.getMediaBox().getHeight();
                page_width = page.getMediaBox().getWidth();

                //Scan collected coordinates
                for (int i = 0; i < coordinates.size(); i++) {
                    //if the current coordinates are not related to the current
                    //page, ignore them
                    //if (!getTerms().contains(tokenStream.get(i))) continue;
                    if ((int) coordinates.get(i)[4] != (page_index + 1))
                        continue;
                    else {
                        //get rotation of the page...portrait..landscape..
                        rotation = (int) coordinates.get(i)[7];

                        //page rotation of +90 degrees
                        if (rotation == 90) {
                            height = coordinates.get(i)[5];
                            width = coordinates.get(i)[6];
                            width = (page_height * width) / page_width;

                            //define coordinates of a rectangle
                            maxx = coordinates.get(i)[1];
                            minx = coordinates.get(i)[1] - height;
                            miny = coordinates.get(i)[0];
                            maxy = coordinates.get(i)[0] + width;
                        } else {   // We should add here the cases -90/-180 degrees
                            height = coordinates.get(i)[5];
                            minx = coordinates.get(i)[0];
                            maxx = coordinates.get(i)[2];
                            miny = page_height - coordinates.get(i)[1];
                            maxy = page_height - coordinates.get(i)[3] + height;
                        }

                        //Add an annotation for each scanned word
                        PDAnnotationTextMarkup txtMark = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);//new PDAnnotationHighlight();//new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                        txtMark.setColor(c);
                        txtMark.setConstantOpacity((float) 1.0);
                        PDRectangle position = new PDRectangle();
                        //position.setLowerLeftX((float) minx);
                        //position.setLowerLeftY((float) miny);
                        //position.setUpperRightX((float) maxx);
                        //position.setUpperRightY((float)(maxy + height));
                        position.setLowerLeftX((float)minx);
                        position.setLowerLeftY((float)miny);
                        position.setUpperRightX((float)maxx);
                        position.setUpperRightY((float)(maxy+2.0*height));
                        txtMark.setRectangle(position);

                        float[] quads = new float[8];
                        quads[0] = position.getLowerLeftX();  // x1
                        quads[1] = position.getUpperRightY() - 2; // y1
                        quads[2] = position.getUpperRightX(); // x2
                        quads[3] = quads[1]; // y2
                        quads[4] = quads[0];  // x3
                        quads[5] = position.getLowerLeftY() - 2; // y3
                        quads[6] = quads[2]; // x4
                        quads[7] = quads[5]; // y5
                        txtMark.setQuadPoints(quads);
                        txtMark.setContents(tokenStream.get(i));
                        annotations.add(txtMark);
                    }
                }
            }

            //Saving the document in a new file
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    public static String textPositionString(TextPosition t) {
        String result = '"' + t.toString() + "\" at " + t.getX() + ", " + t.getY() + " to " + t.getEndX() + ", " + t.getEndY();
        return result;
    }

    @RequiredArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class PageText {
        @NonNull
        public List<TextPosition> positions;
        @NonNull
        public Integer page;

        public String message;
        public float fontSize;
        public PDFont font;
        public int pageRotation, textRotation;
        public float firstX, firstY, lastX, lastY, endX, endY, pageX, pageY;

        public String getText() {
            StringBuilder result = new StringBuilder();
            for (TextPosition tp : positions) {
                result.append(tp.toString());
            }
            font = positions.get(0).getFont();
            fontSize = positions.get(0).getFontSize();
            pageRotation = positions.get(0).getRotation();
            firstX = positions.get(0).getX(); firstY = positions.get(0).getY(); lastX = positions.get(positions.size()-1).getX(); lastY = positions.get(positions.size()-1).getY();
            endX = lastX+positions.get(positions.size()-1).getWidth(); endY = lastY+positions.get(positions.size()-1).getHeight(); pageX = positions.get(0).getPageWidth(); pageY = positions.get(0).getPageHeight();
            if (firstX == lastX) textRotation = 90;
            else textRotation = 0;
            message = result.toString();
            return message;
        }

        public String toString() {
            return '"' + getText() + '"' + " starting at (" + firstX + ", " + firstY + ") ending at (" + lastX + ", "
                    + lastY + ")  end=(" + endX + ", " + endY + ")  page=(" + positions.get(0).getPageWidth() + ", " + positions.get(0).getPageHeight() + ") fontsize=" + positions.get(0).getFontSize()
                    + " and pageDimensions=(" + pageX + ", " + pageY + ") textRotation=" + getTextRotation() + " pageRotation=" + pageRotation + "  font=" + getFont() + "\n";
        }

    }

    /**
     * Overrides the default functionality of PDFTextStripper.writeString()
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        //log.info("Call to writeString, {} positions", textPositions.size());
        //for (TextPosition t : textPositions) System.out.println(textPositionString(t));
        String token = "";
        String token2 = "";

        double minx = 0, maxx = 0, miny = 0, maxy = 0;
        double height = 0;
        double width = 0;
        int rotation = 0;
        if (previous != null) for (int i = 0; i < previous.size(); i++) {
            TextPosition text = previous.get(i);

            if (i == previous.size()-1 && text.toString().equals("-")) {
                token2 = token + "-";
            } else if (i == previous.size()-1 && !text.toString().equals(" ")) {
                token += text + " "; // separate words
            } else if (i == previous.size()-1 && text.toString().equals(" ")) {
                token += text; // they are separated words already
            } else token += text;
        }
        String prevToken = token;
        token = "";
        if (this.getCurrentPageNo() != currentPage) {
            currentPage = this.getCurrentPageNo();
            TextRectangle rect = new TextRectangle(textPositions, currentPage);
            pages.add(new Page(Collections.singletonList(rect)));
            if (pages.size() > 1) {
                for (int i = 0; i < pages.size()-1; i++) pages.get(i).setNextPage(pages.get(i+1));
                for (int i = 1; i < pages.size(); i++) pages.get(i).setPreviousPage(pages.get(i-1));
            }
        } else {
            pages.get(pages.size()-1).add(Collections.singletonList(new TextRectangle(textPositions, currentPage)));
        }
        textp.add(new PageText(textPositions, this.getCurrentPageNo()));
        for (int i = 0; i < textPositions.size(); i++) {
            TextPosition text = textPositions.get(i);
            rotation = text.getRotation();

            if (text.getHeight() > height)
                height = text.getHeight();

            if (text.getWidth() > width)
                width = text.getWidth();

            if (i == textPositions.size() - 1) {  // if it is the end of the current group, it's the end of a word.
                token += text;
                //if (highlightDNA) {
                //}
                token = token.replace(".", " "); // Added 3/9/22
                boolean first = true;
                boolean first2 = true;
                for (String term: terms) {
                    if (token.toUpperCase().contains(term.toUpperCase()) && (token.toUpperCase().indexOf(term.toUpperCase()) + term.length() == token.length()
                    || isValid(token.charAt(token.toUpperCase().indexOf(term.toUpperCase()) + term.length())))) {
                        int start = token.toUpperCase().indexOf(term.toUpperCase());
                        int end = start + term.length();

                        minx = textPositions.get(start).getX();
                        miny = textPositions.get(start).getY();
                        if (end > textPositions.size()-1) {
                            while (end > textPositions.size()-1) end--;
                            maxx = textPositions.get(end).getX() + textPositions.get(end).getWidth();
                        } else maxx = textPositions.get(end).getX();
                        maxy = textPositions.get(end).getY()+textPositions.get(end).getHeight();
                        tokenStream.add(token.substring(start, end+1)); //JAN2022
                        double[] word_coordinates = {minx, miny, maxx, maxy, this.getCurrentPageNo(), height, width, rotation};
                        coordinates.add(word_coordinates);
                    }
                    if (!prevToken.equals("")) {
                        String sumToken = prevToken + token;
                        sumToken = sumToken.replace(".", " ");  // added 3/9/22
                        if (first) {
                            first = false;
                            if (printText) System.out.println(sumToken);
                        }
                        int wordIndex = sumToken.toUpperCase().indexOf(term.toUpperCase());
                        if (sumToken.toUpperCase().contains(term.toUpperCase()) && wordIndex < prevToken.length() && wordIndex + term.length() >= prevToken.length()
                        && (sumToken.toUpperCase().indexOf(term.toUpperCase())+term.length() == sumToken.length() || isValid(sumToken.charAt(sumToken.toUpperCase().indexOf(term.toUpperCase())+term.length())))) {
                            int start = wordIndex;  // from previous set
                            int end = previous.size() - 1;
                            minx = previous.get(start).getX();
                            miny = previous.get(start).getY();
                            maxx = previous.get(end).getX()+previous.get(end).getWidth();
                            maxy = previous.get(end).getY();
                            tokenStream.add(sumToken.substring(start, end+1)); //JAN2022
                            double[] word_coordinates = {minx, miny, maxx, maxy, this.getCurrentPageNo(), height, width, rotation};
                            coordinates.add(word_coordinates);
                            start = 0;
                            end = wordIndex + term.length() - prevToken.length(); // from current set
                            minx = textPositions.get(0).getX();
                            miny = textPositions.get(0).getY();
                            if (end > textPositions.size() - 1) {
                                end--;
                                maxx = textPositions.get(end).getX()+textPositions.get(end).getWidth();
                            } else maxx = textPositions.get(end).getX();
                            maxy = textPositions.get(end).getY();
                            tokenStream.add(token.substring(start, end+1)); //JAN2022
                            word_coordinates = new double[]{minx, miny, maxx, maxy, this.getCurrentPageNo(), height, width, rotation};
                            coordinates.add(word_coordinates);
                        }
                        if (!token2.equals("")) {
                            sumToken = token2 + token;
                            sumToken = sumToken.replace(".", " ");  // added 3/9/22
                            if (first2) {
                                first2 = false;
                                if (printText) System.out.println(sumToken);
                            }
                            wordIndex = sumToken.toUpperCase().indexOf(term.toUpperCase());
                            if (sumToken.toUpperCase().contains(term.toUpperCase()) && wordIndex < token2.length() && wordIndex + term.length() >= token2.length()
                                    && (sumToken.toUpperCase().indexOf(term.toUpperCase()) + term.length() == sumToken.length() || isValid(sumToken.charAt(sumToken.toUpperCase().indexOf(term.toUpperCase())+term.length())))) {
                                int start = wordIndex;  // from previous set
                                int end = previous.size() - 1;
                                minx = previous.get(start).getX();
                                miny = previous.get(start).getY();
                                maxx = previous.get(end).getX()+previous.get(end).getWidth();
                                maxy = previous.get(end).getY();
                                tokenStream.add(sumToken.substring(start, end+1));  // JAN2022
                                double[] word_coordinates = {minx, miny, maxx, maxy, this.getCurrentPageNo(), height, width, rotation};
                                coordinates.add(word_coordinates);
                                start = 0;
                                end = wordIndex + term.length() - token2.length(); // from current set
                                minx = textPositions.get(0).getX();
                                miny = textPositions.get(0).getY();
                                if (end > textPositions.size() - 1) {
                                    end--;
                                    maxx = textPositions.get(end).getX()+textPositions.get(end).getWidth();
                                } else maxx = textPositions.get(end).getX();
                                maxy = textPositions.get(end).getY();
                                tokenStream.add(token.substring(start, end+1));  // JAN2022
                                word_coordinates = new double[] {minx, miny, maxx, maxy, this.getCurrentPageNo(), height, width, rotation};
                                coordinates.add(word_coordinates);
                            }
                        }
                    }
                }
                token = "";
            } else token += text;
        }
        if (previous != null) previous2 = new ArrayList<>(previous);
        previous = new ArrayList<>(textPositions);

    }

    private boolean isValid(char c) {
        return c == ' ' || c == '\n' || c == ',' || c == ')' || c == '.' || c == '-';
    }

    public Map<Integer, List<PageText>> getPageMap() {
        return pageMap;
    }

    public boolean analyze(Article article, SpellChecking spellChecking) {
        if (article == null) {
            log.error("Article is not specified");
            return false;
        }

        if (pages.size() == 0) {
            byte[] pdf = fullTextService.getMainPDF(article.getPmId());
            if (pdf == null) {
                log.error("PMID {} PDF does not exist in the FullText repository", article.getPmId());
                return false;
            }
            log.info("Loaded PDF for PMID {}", article.getPmId());
            try {
                PDDocument PDFdocument = PDDocument.load(pdf);//Loader.loadPDF(pdf);
                PDFTextStripper stripper = this;
                //The method writeText will invoke an overridden version of writeString
                Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
                stripper.writeText(PDFdocument, dummy);
                for (int i = 0; i < PDFdocument.getNumberOfPages(); i++) {
                    pages.get(i).setRotation(PDFdocument.getPage(i).getRotation());
                }
                imageMap = PDFImageStream.process(PDFdocument);
                PDFdocument.close();
            } catch (IOException e) {
                log.error("Error: {}", e.getMessage());
                return false;
            }
        }
        log.info("{} pages in PDF", pages.size());
        if (spellChecking != null) document = new Document(article, pages, imageMap, spellChecking);
        else document = new Document(article, pages, imageMap);
        return true;
    }

    public boolean analyze(Article article) {
        return analyze(article, null);
    }

    public Map<Integer, List<ImageItem>> getImagesByPage(PDDocument document) throws IOException {
        Map<Integer, List<ImageItem>> result = new TreeMap<>();
        int number_of_pages = document.getDocumentCatalog().getPages().getCount();
        for (int i = 0; i < number_of_pages; i++) {
            PDPage page = document.getPage(i);
            result.put(i, getImagesFromResources(page.getResources()));
        }
        return result;
    }

    private List<ImageItem> getImagesFromResources(PDResources resources) throws IOException {
        List<ImageItem> images = new ArrayList<>();
        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);
            if (xObject instanceof PDFormXObject) {
                images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
            } else if (xObject instanceof PDImageXObject) {
                PDImageXObject pdImageXObject = (PDImageXObject)xObject;
                Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
                images.add(new ImageItem(pdImageXObject.getImage(), ctm.getTranslateX(), ctm.getTranslateY()));
            }
        }
        return images;
    }

    public String analyze(String PMID) {
        if (textp.size() == 0) {
            log.info("Trying to analyze blank record, trying FullTextService for PMID and then using a default case!");
            try {
                if (fullTextService != null) {
                    byte[] pdf = fullTextService.getMainPDF(PMID);
                    if (pdf == null) {
                        log.error("PMID {} does not exist in the FullText repository", PMID);
                        return null;
                    }
                    log.info("Loaded PDF for PMID {}", PMID);
                    PDDocument document = PDDocument.load(pdf);//Loader.loadPDF(pdf);
                    PDFTextStripper stripper = this;
                    //The method writeText will invoke an overridden version of writeString
                    Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
                    stripper.writeText(document, dummy);
                } else {
                    log.error("No fullTextService, using default PDF");
                    File file = new File("PNAS.pdf");
                    PDDocument document = PDDocument.load(file);//Loader.loadPDF(file);
                    PDFTextStripper stripper = this;
                    //The method writeText will invoke an overridden version of writeString
                    Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
                    log.info("Calling writeText on dummy file 300.pdf");
                    stripper.writeText(document, dummy);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
                return null;
            }
        }
        Map<Integer, PageText> textOrder = new HashMap<>();
        Map<PageText, Integer> textPage = new HashMap<>();
        Set<Integer> pages = new TreeSet<>();
        int numbering = 0;
        for (PageText pt : textp) {
            textOrder.put(numbering++, pt);
            textPage.put(pt, pt.getPage());
            pages.add(pt.getPage());
            pageMap.putIfAbsent(pt.getPage(), new ArrayList<>());
            List<PageText> oldValues = pageMap.get(pt.getPage());
            pt.getText();
            oldValues.add(pt);
            pageMap.put(pt.getPage(), oldValues);
        } // pages usually 612 x 792
        Article a = articleRepository.findByPmId(PMID);
        StringBuilder textRep = new StringBuilder(10000);
        boolean inReferences = false;
        int refNumber = 1;
        float titleY = 0f;
        for (Integer page : pages) {
            List<PageText> ptList = pageMap.get(page);
            boolean titleFound = false;
            if (page == 1) {
                titleY = findTitle(a.getTitle(), ptList);
            }
            for (int j = 0; j < ptList.size(); j++) {
                //PageText pt = ptList.get(j);
                boolean done = false;
                if (inReferences) {
                    int steps = 0;
                    while (inReferences && j+steps < ptList.size()) {
                        do {
                            if (j > 0 && (ptList.get(j+steps).getTextRotation() != ptList.get(j+steps).getPageRotation() || ptList.get(j+steps).getFirstY()
                                    == ptList.get(j+steps-1).getFirstY())) j++;
                            else steps++;
                        } while (steps < 10 && j + steps < ptList.size() && !ptList.get(j + steps).getMessage().contains(refNumber + "."));
                        if (j + steps < ptList.size() && ptList.get(j + steps).getMessage().contains(refNumber + ".")) {
                            refNumber++;
                            j += steps;
                            steps = 0;
                        }
                        else if (steps == 10) {
                            inReferences = false;
                        }
                    }
                    j += steps;
                }
                if (j >= ptList.size()) continue;
                if (page == 1 && ptList.get(j).getFirstY() < titleY) continue;
                //if (j != 0 && pt.getMessage().equals(ptList.get(j-1).getMessage())) continue;
                if (ptList.get(j).getTextRotation() != ptList.get(j).getPageRotation()) continue;
                if (ptList.get(j).getFirstY() < ptList.get(j).getPageY()*0.06313 || ptList.get(j).getFirstY() > ptList.get(j).getPageY()*0.93687) continue;
                if ((ptList.get(j).getMessage().contains("www.") || ptList.get(j).getMessage().contains("      ") || ptList.get(j).getFontSize() < 9.0f ||
                        (j > 0 && !ptList.get(j).getFont().equals(ptList.get(j-1).getFont()))) && (ptList.get(j).firstY < ptList.get(j).getPageY()*0.11616 || ptList.get(j).firstY > ptList.get(j).getPageY()*0.88383)) continue;
                if (ptList.get(j).getMessage().contains("      ")) continue;
                if (ptList.get(j).getFontSize() <= 8 && ptList.get(j).getMessage().length() > 10) continue;
                if (j != 0 && ptList.get(j).getFirstY() == ptList.get(j-1).getFirstY()) {
                    textRep.append(" ").append(ptList.get(j).getMessage());
                    done = true;
                } else if (j != 0 && ptList.get(j).getFirstY() > ptList.get(j-1).getFirstY() && j < ptList.size()-1 && ptList.get(j-1).getFirstY() != ptList.get(j+1).getFirstY()) {
                    textRep.append("\n");//.append(ptList.get(j).getMessage());
                    //done = true;
                }
                if (!done && j != 0 && !ptList.get(j).getFont().equals(ptList.get(j-1).getFont()) && ptList.get(j).getFirstY() > ptList.get(j-1).getFirstY()
                    && j < ptList.size()-1 && ptList.get(j+1).getFirstY() != ptList.get(j-1).getFirstY()) textRep.append("\n");
                if (page == 1 && !titleFound && a.getTitle().contains(ptList.get(j).getMessage())) {
                    //titleY = ptList.get(j).getFirstY();
                    do {
                        j++;
                    } while (j < ptList.size()-1 && !isSection(ptList, j));
                    //log.info("Winning sentence: {}  isSection={}", ptList.get(j).getMessage(), isSection(ptList, j));
                    if (ptList.get(j).getMessage().equalsIgnoreCase("abstract") || ptList.get(j).getMessage().equalsIgnoreCase("abstract:")) {
                        do {
                            j++;
                        } while (!isSection(ptList, j));
                        //log.info(ptList.get(j).getMessage());
                    }

                    titleFound = true;
                    //done = true;
                }
                if (isSection(ptList, j) && ptList.get(j).getMessage().equalsIgnoreCase("references")) {
                    inReferences = true;
                    int steps = 0;
                    while (inReferences && j+steps < ptList.size()) {
                        do {
                            if (j > 0 && (ptList.get(j+steps).getTextRotation() != ptList.get(j+steps).getPageRotation() || ptList.get(j+steps).getFirstY()
                                == ptList.get(j+steps-1).getFirstY())) {
                                j++;
                            }
                            else {
                                steps++;
                            }
                        } while (steps < 10 && j + steps < ptList.size() && !ptList.get(j + steps).getMessage().contains(refNumber + "."));
                        if (j + steps < ptList.size() && ptList.get(j + steps).getMessage().contains(refNumber + ".")) {
                            refNumber++;
                            j += steps;
                            steps = 0;
                        }
                        else if (steps == 10) {
                            inReferences = false;
                        }
                    }
                    j += steps;
                }
                if (!done && j < ptList.size()) textRep.append(ptList.get(j).getMessage());
                //if (j > 0 && ptList.get(j-1).getFirstY() != ptList.get(j).getFirstY() && (ptList.get(j).getMessage().endsWith(".") || isSection(ptList, j))) textRep.append("\n");

            }
            if (pages.contains(page+1)) textRep.append("\n{P}\n");
        }
        return textRep.toString();
    }

    private float findTitle(String title, List<PageText> ptList) {
        for (int i = 0; i < ptList.size(); i++) {
            if (title.contains(ptList.get(i).getMessage()) && title.indexOf(ptList.get(i).getMessage()) < 2 &&
                    (ptList.get(i).getMessage().length() > title.length()/2 || title.contains(ptList.get(i+1).getMessage()))) return ptList.get(i).getFirstY();
        }
        return -1f;
    }

    public static boolean isSection(List<PageText> ptList, int j) {
        String text = ptList.get(j).getMessage().trim();
        if (text.equalsIgnoreCase("Summary")) return true;
        if (text.equalsIgnoreCase("Introduction")) return true;
        if (text.equalsIgnoreCase("Results")) return true;
        if (text.equalsIgnoreCase("Methods")) return true;
        if (text.equalsIgnoreCase("Materials and Methods")) return true;
        if (text.equalsIgnoreCase("Discussion")) return true;
        if (text.equalsIgnoreCase("Conclusion")) return true;
        if (text.equalsIgnoreCase("References")) return true;
        if (text.equalsIgnoreCase("Abstract:")) return true;
        if (text.matches(".*[ ()0-9\\-].*")) return false;
        //if (j > 0 && ptList.get(j).getFirstX() == ptList.get(j-1).getFirstX()) return false;
        //if (j < ptList.size()-1 && ptList.get(j).getFirstX() == ptList.get(j+1).getFirstX()) return false;
        if (text.toUpperCase().equals(text) && text.length() > 5 && text.split(" ").length < 2) return true;
        return false;
    }

}
