package org.magicat.intuition.pdf;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.magicat.intuition.pdf.TextRectangle.*;

@Getter
@Setter
@EqualsAndHashCode
public class Page implements Serializable {

    private final static Logger log = LoggerFactory.getLogger(Page.class);

    @Serial
    private static final long serialVersionUID = 12345678955L;
    private final int UNORDERED = Integer.MIN_VALUE;

    private String pageText;
    private float width, height;
    private int rotation = -1;
    private Map<Integer, TextRectangle> orderText = new TreeMap<>();
    private List<TextRectangle> text = new ArrayList<>();
    private Page previousPage = null;
    private Page nextPage = null;
    private int items = 0;

    private boolean isFirstPage = false;


    public Page(List<TextRectangle> rectangles) {
        if (rectangles == null || rectangles.size() == 0) return;
        //isOmittableTexts(rectangles);
        for (TextRectangle t : rectangles) {
            if (!isOmittableText(t)) {
                //if (items > max) max = items;
                t.setOrder(items++);
                orderText.put(t.getOrder(), t);
            } else {
                t.setOrder(UNORDERED);
            }
        }
        text.addAll(rectangles);

        width = rectangles.get(0).getPageWidth();
        height = rectangles.get(0).getPageHeight();

    }

    public void add(List<TextRectangle> rectangles) {
        if (rectangles == null || rectangles.size() == 0) return;
        for (TextRectangle t : rectangles) {
            if (!isOmittableText(t)) {
                //if (items > max) max = items;
                t.setOrder(items++);
                orderText.put(t.getOrder(), t);
            } else {
                t.setOrder(UNORDERED);
            }
        }
        text.addAll(rectangles);
    }

    /*
    public void assignRotation() {
        int count = 0;
        Map<Integer, Integer> rotations = new HashMap<>();
        for (TextRectangle textRectangle : text) {
            if (textRectangle.getRotation() == 0) count++;
            else {
                rotations.putIfAbsent(textRectangle.getRotation(), 0);
                rotations.put(textRectangle.getRotation(), rotations.get(textRectangle.getRotation())+1);
                count--;
            }
        }
        if (count > 0) {
            setRotation(0);
        } else {
            int max = 0;
            int bestRotation = 0;
            for (Integer rotation : rotations.keySet()) {
                if (rotations.get(rotation) > max) {
                    max = rotations.get(rotation);
                    bestRotation = rotation;
                }
            }
            setRotation(bestRotation);
        }
    }*/

    public String toSection(TextRectangle rectangle) {
        return toSection(rectangle.getOrder());
    }

    private String tr(String input) {
        if (input == null || input.length() == 0) return "";
        input = input.trim();
        if (StringUtils.isNumeric(input.split("[ \t.]")[0])) {
            int space = input.indexOf(" ");
            int tab = input.indexOf("\t");
            if (tab == -1 && space == -1) return input; // just a number with a period? not a heading
            else if (tab == -1) {
                input = input.substring(space+1);
                if (input.contains("|")) return input.replace("|", "").trim();
                else return input;
                //return input.substring(space+1);
            } else if (space == -1) {
                input = input.substring(tab+1);
                if (input.contains("|")) return input.replace("|", "").trim();
                else return input;
                //return input.substring(tab+1);
            } else {
                int pos = Math.min(space, tab);
                input = input.substring(pos+1);
                if (input.contains("|")) return input.replace("|", "").trim();
                else return input;
            }
        }
        return input;
    }

    public static Pair<String, String> figureTable(String input) {
        input = input.trim().replace("\t", " ");
        if (input.split(" ").length < 3) return null;
        Pattern p = Pattern.compile("(fig [\\d]{1,2})|(figure [\\d]{1,2})|(table [\\d]{1,2})", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(input);
        if (m.find() && m.start() == 0) {
            return new Pair<>(input.substring(0, m.end()), input.substring(m.end()).substring(input.substring(m.end()).indexOf(" ")+1));
            //String result1 = input.substring(m.start(), m.end());
            //String result2 = input.substring(input.indexOf(" ")+1);
        }
        return null;
    }

    public String toSection(int order) {
        TextRectangle t = orderText.get(order);
        if (t == null) return null;
        List<TextRectangle> sections = getNeighbors(t, orderText);
        //for (int i = 0; i < sections.size(); i++) {
        //    if (sections.get(i).getOrder() > order) {
        //        sections.add(i, t);
        //        break;
        //    }
        //}
        String text = compose(sections);
        List<TextRectangle> result = splitFontChange(sections);
        String value = "";
        if (result != null) value = compose(result);
        // FIGURE
        if (result != null && tr(value).toLowerCase().startsWith("fig") && value.length() < 12) return value;
        if (tr(text).toLowerCase().startsWith("fig") && text.length() < 15) return text;
        // TABLE
        if (result != null && tr(value).toLowerCase().startsWith("table") && value.length() < 15) return value;
        if (tr(text).toLowerCase().startsWith("table") && text.length() < 10) return text;
        if (tr(text).equalsIgnoreCase("Summary") || tr(text).toLowerCase().startsWith("summary") && text.length() < 25) return text;
        if (result != null && tr(value).toLowerCase().startsWith("summary") && value.length() < 15) return value;
        if (tr(text).equalsIgnoreCase("Introduction") || tr(text).toLowerCase().startsWith("introduction") && text.length() < 25) return text;
        if (result != null && tr(value).toLowerCase().startsWith("introduction") && value.length() < 15) return value;
        if (tr(text).equalsIgnoreCase("Results") || tr(text).toLowerCase().startsWith("results") && text.length() < 25) return text;
        if (result != null && tr(value).toLowerCase().startsWith("results") && value.length() < 15) return value;
        if (tr(text).equalsIgnoreCase("Methods") || tr(text).toLowerCase().startsWith("methods") && text.length() < 25) return text;
        if (result != null && tr(value).toLowerCase().startsWith("methods") && value.length() < 15) return value;
        if (tr(text).equalsIgnoreCase("Materials") || tr(text).toLowerCase().startsWith("materials") && text.length() < 25) return text;
        if (result != null && tr(value).toLowerCase().startsWith("materials") && value.length() < 25) return value;
        if (tr(text).equalsIgnoreCase("Discussion") || tr(text).toLowerCase().startsWith("discussion") && text.length() < 25) return text;
        if (result != null && tr(value).toLowerCase().startsWith("discussion")&& value.length() < 15) return value;
        if (tr(text).equalsIgnoreCase("Conclusion") || tr(text).toLowerCase().startsWith("conclusion") && text.length() < 25) return text;
        if (result != null && tr(value).toLowerCase().startsWith("conclusion") && value.length() < 15) return value;
        if (tr(text).equalsIgnoreCase("References") || StringUtils.getLevenshteinDistance(tr(text).toLowerCase(), "references") < 2 || (tr(text).toLowerCase().startsWith("references") || tr(text).toLowerCase().startsWith("citation")) && text.length() < 25) return text;
        if (result != null && (tr(value).toLowerCase().startsWith("references") || tr(value).toLowerCase().startsWith("citation")) && value.length() < 15) return value;
        if (tr(text).equalsIgnoreCase("Abstract") || tr(text).toLowerCase().startsWith("abstract") && text.length() < 20) return text;
        if (result != null && tr(value).toLowerCase().startsWith("abstract") && value.length() < 15) return value;
        if (result != null && (StringUtils.getLevenshteinDistance(tr(value).toLowerCase(), "acknowledgements") < 3 || tr(value).toLowerCase().startsWith("acknowledgements")) || tr(value).toLowerCase().startsWith("acknowledgments") && value.length() < 25) return value;
        if (tr(text).equalsIgnoreCase("Acknowledgements") || StringUtils.getLevenshteinDistance(tr(text).toLowerCase(), "acknowledgements") < 3 || tr(text).toLowerCase().startsWith("acknowledgements") && text.length() < 25) return text;
        if (tr(text).equalsIgnoreCase("Acknowledgments") || tr(text).toLowerCase().startsWith("acknowledgments") && text.length() < 25) return text;
        if (tr(text).equalsIgnoreCase("Experimental Procedures") || (tr(text).toLowerCase().startsWith("experimental") && text.length() < 30)) return text;
        if (tr(text).equalsIgnoreCase("Supplementary Material") || tr(text).equalsIgnoreCase("Supporting Material") || (tr(text).toLowerCase().startsWith("supporting") || tr(text).toLowerCase().startsWith("supplemetary")) && text.length() < 30) return text;
        if ((tr(text).toLowerCase().startsWith("supplementary") || tr(text).toLowerCase().startsWith("supporting")) && text.length() < 32) return text;
        if (result != null && (tr(value).toLowerCase().startsWith("supporting") || tr(value).toLowerCase().startsWith("supplementary")) && value.length() < 32) return value;
        if (result != null && tr(value).toLowerCase().startsWith("experimental") && value.length() < 30) return value;
        if (result != null && tr(value).toLowerCase().startsWith("notes") && value.length() < 15) return value;
        if (tr(text).toLowerCase().startsWith("notes") && text.length() < 15) return text;

        //if (result != null && value.length() > 6 && value.length() < 20) return value;
        return null;
    }

    public int getRotation() {
        //if (rotation == -1) assignRotation();
        return rotation;
    }

}

class Pair<A, B> {
    private A first;
    private B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A first() {
        return first;
    }

    public B second() {
        return second;
    }
}
