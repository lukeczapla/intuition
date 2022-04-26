package org.magicat.pdf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collector;

@Getter
@Setter
@EqualsAndHashCode
public class Section implements Serializable {

    private final transient static Logger log = LoggerFactory.getLogger(Section.class);

    @Serial
    private static final long serialVersionUID = 12345678911L;

    private boolean isTable = false;
    //@JsonView(Views.Section.class)
    private String heading;
    //@JsonView(Views.Section.class)
    private List<String> paragraphs = new ArrayList<>();
    private ImageItem image = null;
    private List<String[][]> rowsColumns = null;
    @JsonIgnore
    private transient List<TextRectangle> previousLine = null;
    @JsonIgnore
    private transient List<String> fontNames = new ArrayList<>();
    @JsonIgnore
    private transient List<Float> widths = new ArrayList<>();
    @JsonIgnore
    private transient List<Float> lineDistances = new ArrayList<>();

    public Section(String heading) {
        this.heading = heading;
    }

    private static final Collector<Float, float[], Float> VARIANCE_COLLECTOR = Collector.of(
            () -> new float[3], // {count, mean, M2}
            (acu, d) -> { // See chapter about Welford's online algorithm and https://math.stackexchange.com/questions/198336/how-to-calculate-standard-deviation-with-streaming-inputs
                acu[0]++; // Count
                float delta = d - acu[1];
                acu[1] += delta / acu[0]; // Mean
                acu[2] += delta * (d - acu[1]); // M2
            },
            (acuA, acuB) -> { // See chapter about "Parallel algorithm" : only called if stream is parallel ...
                float delta = acuB[1] - acuA[1];
                float count = acuA[0] + acuB[0];
                acuA[2] = acuA[2] + acuB[2] + delta * delta * acuA[0] * acuB[0] / count; // M2
                acuA[1] += delta * acuB[0] / count;  // Mean
                acuA[0] = count; // Count
                return acuA;
            },
            acu -> acu[2] / (acu[0] - 1.0f), // Var = M2 / (count - 1)
            Collector.Characteristics.UNORDERED);

    public boolean bigDistance(List<TextRectangle> line, List<TextRectangle> nextLine, int pageRotation) {
        float distance;
        if (pageRotation == 0) {
            distance = nextLine.get(0).getTopY() - line.get(0).getTopY();
            lineDistances.add(distance);
            int size = lineDistances.size();
            //if (lineDistances.size() == 1) return false;
            for (int i = 1; i < Math.min(nextLine.size(), line.size()); i++) lineDistances.add(nextLine.get(i).getTopY() - line.get(i).getTopY());

            float average = lineDistances.stream().reduce(Float::sum).get() / lineDistances.size();
            float stdDev = (float)Math.sqrt(lineDistances.stream().collect(VARIANCE_COLLECTOR));
            //System.out.printf("%f %f %f\n", distance, average, stdDev);
            if (size == 1) return false;
            return distance > average + stdDev && distance - average > 0.5; // if it's that tiny difference (<=0.5), it's still not a paragraph indicator!

        } else {
            distance = nextLine.get(0).getTopX() - line.get(0).getTopX();
            lineDistances.add(distance);
            if (lineDistances.size() == 1) return false;
            else {
                float average = lineDistances.stream().reduce(Float::sum).get() / lineDistances.size();
                float stdDev = (float)Math.sqrt(lineDistances.stream().collect(VARIANCE_COLLECTOR));
                return distance > average + stdDev;
            }
        }
    }

    public static String sub(String input) {
        while (input.contains("^^(")) {
            String end = input.substring(input.indexOf("^^(")).substring(input.substring(input.indexOf("^^(")).indexOf(")")+1);
            input = input.substring(0, input.indexOf("^^(")) + "<sup>" + input.substring(input.indexOf("^^(")+3).substring(0, input.substring(input.indexOf("^^(")+3).indexOf(")")) + "</sup>" + end;
        }
        while (input.contains("__(")) {
            String end = input.substring(input.indexOf("__(")).substring(input.substring(input.indexOf("__(")).indexOf(")")+1);
            input = input.substring(0, input.indexOf("__(")) + "<sub>" + input.substring(input.indexOf("__(")+3).substring(0, input.substring(input.indexOf("__(")+3).indexOf(")")) + "</sub>" + end;
        }
        String remainder = input;
        boolean found = false;
        while (remainder.contains("http://")) {
            found = true;
            int position = remainder.indexOf("http://");
            input = remainder.substring(0, position) + "<a href='";
            remainder = remainder.substring(position);
            int p1 = remainder.indexOf(" ");
            int p2 = remainder.indexOf(")");
            if (p1 != -1 && p2 != -1) {
                int position2 = Math.min(p1, p2);
                input += remainder.substring(0, position2) + "'>" + remainder.substring(0, position2) + "</a>";
                remainder = remainder.substring(position2);
            } else if (p1 != -1) {
                input += remainder.substring(0, p1) + "'>" + remainder.substring(0, p1) + "</a>";
                remainder = remainder.substring(p1);
            } else if (p2 != -1) {
                input += remainder.substring(0, p2) + "'>" + remainder.substring(0, p2) + "</a>";
                remainder = remainder.substring(p2);
            } else {
                input += remainder + "'>" + remainder + "</a>";
                remainder = "";
            }
        }
        if (found) input += remainder;
        remainder = input;
        found = false;
        while (remainder.contains("https://")) {
            found = true;
            int position = remainder.indexOf("https://");
            input = remainder.substring(0, position) + "<a href='";
            remainder = remainder.substring(position);
            int p1 = remainder.indexOf(" ");
            int p2 = remainder.indexOf(")");
            if (p1 != -1 && p2 != -1) {
                int position2 = Math.min(p1, p2);
                input += remainder.substring(0, position2) + "'>" + remainder.substring(0, position2) + "</a>";
                remainder = remainder.substring(position2);
            } else if (p1 != -1) {
                input += remainder.substring(0, p1) + "'>" + remainder.substring(0, p1) + "</a>";
                remainder = remainder.substring(p1);
            } else if (p2 != -1) {
                input += remainder.substring(0, p2) + "'>" + remainder.substring(0, p2) + "</a>";
                remainder = remainder.substring(p2);
            } else {
                input += remainder + "'>" + remainder + "</a>";
                remainder = "";
            }
        }
        if (found) input += remainder;
        return input;
    }

    public void cleanup() {
        for (int i = 0; i < paragraphs.size(); i++) {
            if (paragraphs.get(i).length() < 2 && !StringUtils.isAlphanumeric(paragraphs.get(i))) {
                paragraphs.remove(i--);
            }
        }
        if (isTable()) {
            rowsColumns = new ArrayList<>();
            int rows = 0, columns = 0;
            for (String paragraph: paragraphs) {
                if (!paragraph.contains("\n")) continue;
                String[] lines = paragraph.split("\n");
                if (lines.length > rows) rows = lines.length;
                for (String line: lines) {
                    String[] tabSeperated = line.split("\t");
                    if (tabSeperated.length > columns) columns = tabSeperated.length;
                }
            }
            for (String paragraph: paragraphs) {
                if (!paragraph.contains("\n")) continue;
                String[][] table = new String[rows][columns];
                String[] lines = paragraph.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    String[] tabSeperated = line.split("\t");
                    System.arraycopy(tabSeperated, 0, table[i], 0, tabSeperated.length);
                    if (tabSeperated.length < columns) for (int j = tabSeperated.length; j < columns; j++) table[i][j] = "";
                }
                rowsColumns.add(table);
            }
        }
    }

    public String toHTML() {
        StringBuilder sb = new StringBuilder();
        if (image != null) {
            int width = image.getWidth(), height = image.getHeight();
            int value = Math.max(image.getWidth(), image.getHeight());
            if (value > 1500) {
                width /= 2; height /= 2;
            }
            sb.append("<img src=\"data:image/png;base64, ").append(Base64.getEncoder().encodeToString(image.getByteImage().getData()))
                    .append("\" alt=\"").append(heading).append("\" width=\"").append(width).append("\" height=\"").append(height).append("\" />");
        }
        if (paragraphs.size() > 1 || (paragraphs.size() == 1 && !paragraphs.get(0).equals(""))) {
            if (!heading.equals("No Header")) sb.append("<h3><b>").append(sub(heading)).append("</b></h3>");
            for (String paragraph : paragraphs) {
                if (paragraph.length() < 2 && (!StringUtils.isAlphanumeric(paragraph))) continue;
                if (isTable() && paragraph.contains("\n")) {
                    String[] lines = paragraph.split("\n");
                    sb.append("<table border='1' style='border-collapse:collapse'>");
                    for (String line: lines) {
                        sb.append("<tr>");
                        String[] markers = line.split("\t");
                        for (String marker: markers) {
                            sb.append("<td>").append(sub(marker)).append("</td>");
                        }
                        sb.append("</tr>");
                    }
                    sb.append("</table>");
                }
                else if ((paragraph.length() <= 120 && !heading.toLowerCase().startsWith("ref")) || Document.isNumbered(paragraph)) {
                    sb.append("<h4><b><i>").append(sub(paragraph)).append("</i></b></h4>");
                } else {
                    sb.append("<p>").append(sub(paragraph)).append("</p>");
                }
            }
        }
        return sb.toString();
    }

    public String toHTML(List<Section> figureTableSections) {
        StringBuilder sb = new StringBuilder();
        if (image != null) {
            int width = image.getWidth(), height = image.getHeight();
            int value = Math.max(image.getWidth(), image.getHeight());
            if (value > 1500) {
                width /= 2; height /= 2;
            }
            sb.append("<img src=\"data:image/png;base64, ").append(Base64.getEncoder().encodeToString(image.getByteImage().getData()))
                    .append("\" alt=\"").append(heading).append("\" width=\"").append(width).append("\" height=\"").append(height).append("\" />");
        }
        if (paragraphs.size() > 1 || (paragraphs.size() == 1 && !paragraphs.get(0).equals(""))) {
            List<Section> toAdd = new ArrayList<>();
            if (!heading.equals("No Header")) sb.append("<h3><b>").append(sub(heading)).append("</b></h3>");
            for (String paragraph : paragraphs) {
                if (paragraph.length() < 2 && (!StringUtils.isAlphanumeric(paragraph))) continue;
                if (paragraph.length() > 3) {
                    while (paragraph.contains("${")) {
                        int position = paragraph.indexOf("${");
                        int position2 = paragraph.substring(position).indexOf("}");
                        String sectionHeading = paragraph.substring(position+2, position+position2);
                        paragraph = paragraph.substring(0, position) + sectionHeading + paragraph.substring(position+position2+1);
                        for (int i = 0; i < figureTableSections.size(); i++) {
                            Section figureTableSection = figureTableSections.get(i);
                            if (figureTableSection == null) continue;
                            String figureTableHeading = figureTableSection.getHeading();
                            if (figureTableHeading.endsWith(".")) figureTableHeading = figureTableHeading.substring(0, figureTableHeading.length()-1);
                            if (figureTableHeading.equalsIgnoreCase(sectionHeading)) {
                                toAdd.add(figureTableSection);
                                figureTableSections.remove(i--);
                            }
                        }
                    }
                }
                if (isTable() && paragraph.contains("\n")) {
                    String[] lines = paragraph.split("\n");
                    sb.append("<table border='1' style='border-collapse:collapse'>");
                    for (String line: lines) {
                        sb.append("<tr>");
                        String[] markers = line.split("\t");
                        for (String marker: markers) {
                            sb.append("<td>").append(sub(marker)).append("</td>");
                        }
                        sb.append("</tr>");
                    }
                    sb.append("</table>");
                }
                else if ((paragraph.length() <= 120 && !heading.toLowerCase().startsWith("ref")) || Document.isNumbered(paragraph)) {
                    sb.append("<h4><b><i>").append(sub(paragraph)).append("</i></b></h4>");
                } else {
                    sb.append("<p>").append(sub(paragraph)).append("</p>");
                }
                for (Section section: toAdd) {
                    sb.append("<hr/>");
                    sb.append(section.toHTML());
                    sb.append("<hr/>");
                }
                toAdd.clear();
            }
        }
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(heading).append(image != null ? " (Image @ " + image.getTopX() + ", " + image.getTopY() + ")": "").append("\n\n");
        for (String paragraph : paragraphs) {
            sb.append("\t").append(paragraph).append("\n");
        }
        return sb.toString();
    }

}
