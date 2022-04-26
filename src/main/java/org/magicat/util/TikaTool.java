package org.magicat.util;


import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.boilerpipe.BoilerpipeContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.*;


public class TikaTool {

    private static final Logger log = LoggerFactory.getLogger(TikaTool.class);

    public static String parseToPlainText(String fileName) throws IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler();
        AutoDetectParser parser = new AutoDetectParser();

        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(fileName)) {
            parser.parse(stream, handler, metadata);
            return handler.toString();
        }
    }

    public static String parseToHTML(String fileName) throws IOException, SAXException, TikaException {
        ContentHandler handler = new ToXMLContentHandler();

        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(fileName)) {
            parser.parse(stream, handler, metadata);
            return handler.toString();
        }
    }

    public static String parseBodyToHTML(String fileName) {
        ContentHandler handler = new BodyContentHandler(new ToXMLContentHandler());
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(fileName)) {
            parser.parse(stream, handler, metadata);
        } catch (TikaException|IOException|SAXException e) {
            log.error("ERROR occurred: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
        return handler.toString();

    }

/*
    public static void setPDFConfig(ParseContext context) {
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setOcrDPI(600);
        pdfConfig.setDetectAngles(true);
        pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.AUTO);
        //pdfConfig.setExtractInlineImages(true);
        //pdfConfig.setExtractMarkedContent(true);
        //pdfConfig.setExtractUniqueInlineImagesOnly(true);
        //pdfConfig.setExtractAnnotationText(true);
        //pdfConfig.setMaxMainMemoryBytes(6000000000L);
        pdfConfig.setEnableAutoSpace(true);
        pdfConfig.setSuppressDuplicateOverlappingText(true);

        context.set(PDFParserConfig.class, pdfConfig);

    }*/

    public static String cleanup(String text) {
        if (text == null) {
            log.error("Trying to clean up an empty PDF/Doc file!");
            return null;
        }
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder(100000);
        for (String line: lines) {
            if (line.length() == 0) continue;
            sb.append(line + "\n");
        }
        return sb.toString();
    }

    /*
    public static void setTesseract(ParseContext context) {
        TesseractOCRConfig ocrConfig = new TesseractOCRConfig();
        ocrConfig.setLanguage("eng");
        ocrConfig.setEnableImageProcessing(1);
        ocrConfig.setApplyRotation(true);
        context.set(TesseractOCRConfig.class, ocrConfig);
    }*/

    public static String parseDocument(String fileName) {
        BodyContentHandler handler = new BodyContentHandler(-1);
        AutoDetectParser parser = new AutoDetectParser();
        PDFParser parse;
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(fileName)) {
            parser.parse(stream, handler, metadata);
        } catch (TikaException|IOException|SAXException e) {
            log.error("An error occurred with converting {} to plain text", fileName);
            log.error(e.getMessage());
            e.printStackTrace();
            return null;
        }
        return handler.toString();
    }

    public static String parseDocumentHTML(String fileName) {
        ContentHandler handler = new ToXMLContentHandler();
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(fileName)) {
            parser.parse(stream, handler, metadata);
        } catch (TikaException|IOException|SAXException e) {
            log.error("An error occurred with converting {} to HTML", fileName);
            log.error(e.getMessage());
            e.printStackTrace();
            return null;
        }
        return handler.toString();
    }

    /*public static String iTextParse(String fileName) {
        try {
            PdfReader reader = new PdfReader(fileName);
            PrintWriter out = new PrintWriter(new FileOutputStream("outputPDF.txt"));
//Creating the rectangle
            Rectangle mediaBox = reader.getPageSize(1);
            //System.out.printf("%f %f %f %f\n", mediaBox.getLeft(), mediaBox.getTop(), mediaBox.getRight(), mediaBox.getBottom());
            Rectangle rect = new Rectangle(mediaBox.getLeft(), mediaBox.getBottom()+30, mediaBox.getRight(), mediaBox.getTop()-30);
//creating a filter based on the rectangle
            RenderFilter filter = new RegionTextRenderFilter(rect);
            TextExtractionStrategy strategy;
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                //setting the filter on the text extraction strategy
                strategy = new FilteredTextRenderListener(new LocationTextExtractionStrategy(), filter);
                out.println(PdfTextExtractor.getTextFromPage(reader, i, strategy));
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "OK";
    }*/

    public static String OfficeParseDocument(String fileName) {
        ParseContext parseContext = new ParseContext();
        AutoDetectParser parser = new AutoDetectParser();
        ContentHandler contentHandler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        TesseractOCRConfig config = new TesseractOCRConfig();
        config.setSkipOcr(true);

        OfficeParserConfig officeParserConfig = new OfficeParserConfig();
        officeParserConfig.setIncludeHeadersAndFooters(false);
        parseContext.set(OfficeParserConfig.class, officeParserConfig);
        parseContext.set(TesseractOCRConfig.class, config);

        try (InputStream stream = new FileInputStream(fileName)) {
            parser.parse(stream, new BoilerpipeContentHandler(contentHandler), metadata, parseContext);
        } catch (TikaException|IOException|SAXException e) {
            log.error("An error occurred with converting {} to HTML", fileName);
            log.error(e.getMessage());
            e.printStackTrace();
            return null;
        }
        /*Tika tika = new Tika();
        String s;
        try (InputStream stream = new FileInputStream(fileName)) {
            s = tika.parseToString(stream, metadata);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        System.out.println(s + "\n\n\n");*/
        return contentHandler.toString();
    }

    public static String OfficeParseHTMLDocument(String fileName) {
        ParseContext parseContext = new ParseContext();
        AutoDetectParser parser = new AutoDetectParser();
        ContentHandler contentHandler = new ToXMLContentHandler();
        Metadata metadata = new Metadata();
        TesseractOCRConfig config = new TesseractOCRConfig();
        config.setSkipOcr(true);

        PDFParserConfig pdfParserConfig = new PDFParserConfig();
        pdfParserConfig.setExtractUniqueInlineImagesOnly(true);
        OfficeParserConfig officeParserConfig = new OfficeParserConfig();
        officeParserConfig.setIncludeHeadersAndFooters(false);
        parseContext.set(OfficeParserConfig.class, officeParserConfig);
        parseContext.set(PDFParserConfig.class, pdfParserConfig);
        parseContext.set(TesseractOCRConfig.class, config);
        //
        //

        try (InputStream stream = new FileInputStream(fileName)) {
            parser.parse(stream, contentHandler, metadata, parseContext);
        } catch (TikaException|IOException|SAXException e) {
            log.error("An error occurred with converting {} to HTML", fileName);
            log.error(e.getMessage());
            e.printStackTrace();
            return null;
        }
        return contentHandler.toString();
    }

}
