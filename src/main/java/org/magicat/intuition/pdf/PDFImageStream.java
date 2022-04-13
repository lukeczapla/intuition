package org.magicat.intuition.pdf;

import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Getter
@Setter
public class PDFImageStream extends PDFStreamEngine {

    private Map<Integer, List<ImageItem>> imageMap = new TreeMap<>();
    private int pageNumber;

    public PDFImageStream() {
        addOperator(new Concatenate());
        addOperator(new DrawObject());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new SetMatrix());
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();
        if ("Do".equals(operation)) {
            COSName objectName = (COSName)operands.get(0);
            // get the PDF object
            PDXObject xobject = getResources().getXObject(objectName);
            // check if the object is an image object
            if (xobject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject)xobject;

                Matrix mat = getGraphicsState().getCurrentTransformationMatrix();
                imageMap.get(pageNumber).add(new ImageItem(image.getImage(), mat.getTranslateX(), mat.getTranslateY()));

            } else if (xobject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject)xobject;
                showForm(form);
            }
        } else {
            super.processOperator(operator, operands);
        }
    }

    public static Map<Integer, List<ImageItem>> process(PDDocument document) {
        PDFImageStream printer = new PDFImageStream();
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            printer.setPageNumber(i);
            printer.getImageMap().putIfAbsent(printer.getPageNumber(), new ArrayList<>());
            PDPage page = document.getPage(i);
            try {
                printer.processPage(page);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return printer.getImageMap();
    }

}
