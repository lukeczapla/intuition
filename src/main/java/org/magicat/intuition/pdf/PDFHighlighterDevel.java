package org.magicat.intuition.pdf;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import lombok.ToString;
//import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
//import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.pdmodel.interactive.measurement.PDViewportDictionary;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ToString
public class PDFHighlighterDevel extends PDFTextStripper {

    private final static Logger log = LoggerFactory.getLogger(PDFHighlighter.class);
    private List<String> terms = new ArrayList<>();
    private List<TextPosition> previous = null;

    private List<double[]> coordinates = new ArrayList<>();
    private List<String> tokenStream = new ArrayList<>();

    public PDFHighlighterDevel() throws IOException {
        super();
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public void highlight(String fileName, String outFileName, List<String> terms) {

        try {
            //Loading an existing document
            File file = new File(fileName);
            PDDocument document = PDDocument.load(file);// Loader.loadPDF(file);
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
            PDPage p = document.getPage(1);
            PDRectangle r = p.getArtBox();
            System.out.println("Top right: " + r.getUpperRightX() + "," + r.getUpperRightY() + " and lower left: " + r.getLowerLeftX() + "," + r.getLowerLeftY());
            //scan each page and highlight all the words inside them
            for (int page_index = 0; page_index < number_of_pages; page_index++) {
                //get current page
                PDPage page = document.getPage(page_index);

                PDRectangle rect = page.getMediaBox();
                System.out.println("Page " + page_index + " mediabox:");
                System.out.println("Top right: " + r.getUpperRightX() + "," + r.getUpperRightY() + " and lower left: " + r.getLowerLeftX() + "," + r.getLowerLeftY());

                //Get annotations for the selected page
                List<PDAnnotation> annotations = page.getAnnotations();

                //Define a color to use for highlighting text
                PDColor yellow = new PDColor(new float[]{1.0f, 0.412f, 0.706f}, PDDeviceRGB.INSTANCE);

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
                    PDAnnotationTextMarkup txtMark = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);//new PDAnnotationHighlight();//new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                    txtMark.setColor(yellow);
                    txtMark.setConstantOpacity((float) 0.3); // 30% transparent
                    PDRectangle position = new PDRectangle();
                    position.setLowerLeftX((float) minx);
                    position.setLowerLeftY((float) miny);
                    position.setUpperRightX((float) maxx);
                    position.setUpperRightY((float) ((float) maxy + height));
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
            System.out.println(e);
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
            PDPage p = document.getPage(1);
            PDRectangle r = p.getArtBox();
            System.out.println("Top right: " + r.getUpperRightX() + "," + r.getUpperRightY() + " and lower left: " + r.getLowerLeftX() + "," + r.getLowerLeftY());

            //scan each page and highlight all the words inside them
            for (int page_index = 0; page_index < number_of_pages; page_index++) {
                //get current page
                PDPage page = document.getPage(page_index);
                List<PDViewportDictionary> items = page.getViewports();
                System.out.println("Viewport Dictionaries:");
                for (PDViewportDictionary d : items) {
                    System.out.println(d.getName());
                }
                //Get annotations for the selected page
                List<PDAnnotation> annotations = page.getAnnotations();

                //Define a color to use for highlighting text
                PDColor yellow = new PDColor(new float[]{1, 1, 0}, PDDeviceRGB.INSTANCE);

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
                        txtMark.setColor(yellow);
                        txtMark.setConstantOpacity((float) 0.3); // 30% transparent
                        PDRectangle position = new PDRectangle();
                        position.setLowerLeftX((float) minx);
                        position.setLowerLeftY((float) miny);
                        position.setUpperRightX((float) maxx);
                        position.setUpperRightY((float) ((float) maxy + height));
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

    /**
     * Overrides the default functionality of PDFTextStripper.writeString()
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        String token = "";

        double minx = 0, maxx = 0, miny = 0, maxy = 0;
        double height = 0;
        double width = 0;
        int rotation = 0;
        if (previous != null) for (int i = 0; i < previous.size(); i++) {
            TextPosition text = previous.get(i);

            if (i == previous.size()-1 && text.toString().equals("-")) {
               // do nothing special
            } else if (i == previous.size()-1) {
                token += text + " "; // separate words
            } else token += text;
        }
        String prevToken = token;
        token = "";
        for (int i = 0; i < textPositions.size(); i++) {
            TextPosition text = textPositions.get(i);

            rotation = text.getRotation();

            if (text.getHeight() > height)
                height = text.getHeight();

            if (text.getWidth() > width)
                width = text.getWidth();

            if (i == textPositions.size() - 1) {  // if it is the end of the current group, it's the end of a word.
                token += text;
                for (String term: terms) {
                    if (token.toUpperCase().contains(term.toUpperCase())) {
                        int start = token.toUpperCase().indexOf(term.toUpperCase());
                        int end = start + term.length();

                        minx = textPositions.get(start).getX();
                        miny = textPositions.get(start).getY();
                        if (end > textPositions.size()-1) {
                            end--;
                            maxx = textPositions.get(end).getEndX();
                        } else maxx = textPositions.get(end).getX();
                        maxy = textPositions.get(end).getY();
                        tokenStream.add(token.substring(start, end));
                        double[] word_coordinates = {minx, miny, maxx, maxy, this.getCurrentPageNo(), height, width, rotation};
                        coordinates.add(word_coordinates);
                    }
                    if (!prevToken.equals("")) {
                        String sumToken = prevToken + token;
                        int wordIndex = sumToken.toUpperCase().indexOf(term.toUpperCase());
                        if (sumToken.toUpperCase().contains(term.toUpperCase()) && wordIndex < prevToken.length() && wordIndex+term.length() >= prevToken.length()) {
                            int start = wordIndex;  // from previous set
                            int end = previous.size() - 1;
                            minx = previous.get(start).getX();
                            miny = previous.get(start).getY();
                            maxx = previous.get(end).getEndX();
                            maxy = previous.get(end).getY();
                            tokenStream.add(sumToken.substring(start, end));
                            double[] word_coordinates = {minx, miny, maxx, maxy, this.getCurrentPageNo(), height, width, rotation};
                            coordinates.add(word_coordinates);
                            start = 0;
                            end = wordIndex+term.length()-prevToken.length(); // from current set
                            minx = textPositions.get(0).getX();
                            miny = textPositions.get(0).getY();
                            if (end > textPositions.size()-1) {
                                end--;
                                maxx = textPositions.get(end).getEndX();
                            } else maxx = textPositions.get(end).getX();
                            maxy = textPositions.get(end).getY();
                            tokenStream.add(token.substring(start, end));
                            word_coordinates = new double[] {minx, miny, maxx, maxy, this.getCurrentPageNo(), height, width, rotation};
                            coordinates.add(word_coordinates);
                        }
                    }
                }
                token = "";
            } else token += text;

        }
        previous = new ArrayList<>(textPositions);

    }

}
