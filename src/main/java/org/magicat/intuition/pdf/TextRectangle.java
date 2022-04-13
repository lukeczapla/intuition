package org.magicat.intuition.pdf;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * TextRectangle - the concept here is to abstract the PDFBox information out of the TextPosition objects and provide an easier interface layer for the PDFBox
 * functionality.
 */
@Getter
@Setter
@EqualsAndHashCode
public class TextRectangle implements Serializable {

    @Serial
    private static final long serialVersionUID = 82340467L;

    public static final Logger log = LoggerFactory.getLogger(TextRectangle.class);

    transient private List<TextPosition> textPositions;
    private int page, order, rotation;
    private float pageWidth, pageHeight;
    private float widthOfSpace;
    private float topX, topY;
    private float bottomX, bottomY;
    private float width, height;
    private float midX, midY;
    private float fontSize;
    private String fontName;
    private boolean possibleTable = false;
    private boolean isOmittable = false;
    private boolean isIndented = false;
    private String text;

    private float dx, dy;

    public TextRectangle(List<TextPosition> textPositions, int page) {
        this.textPositions = textPositions;
        this.page = page;
        if (this.textPositions == null || this.textPositions.size() == 0) return;
        this.rotation = getAngle(textPositions.get(0));
        this.fontSize = textPositions.get(0).getFontSizeInPt();
        this.fontName = textPositions.get(0).getFont().getName();
        this.widthOfSpace = textPositions.get(0).getWidthOfSpace();
        this.width = textPositions.get(0).getWidth();
        this.height = this.textPositions.get(0).getFont().getFontDescriptor().getCapHeight()/1000.0f*this.textPositions.get(0).getFontSizeInPt();
        this.topX = this.textPositions.get(0).getX();
        this.topY = this.textPositions.get(0).getY();
        this.pageWidth = this.textPositions.get(0).getPageWidth();
        pageHeight = this.textPositions.get(0).getPageHeight();
        bottomX = this.textPositions.get(this.textPositions.size()-1).getEndX();// getX() + this.textPositions.get(this.textPositions.size()-1).getWidth();
        bottomY = this.textPositions.get(0).getY() + this.textPositions.get(0).getFont().getFontDescriptor().getCapHeight()/1000.0f*this.textPositions.get(0).getFontSizeInPt();
        for (TextPosition textPosition : this.textPositions)
            bottomY = Math.max(bottomY, this.textPositions.get(0).getY() + textPosition.getFont().getFontDescriptor().getCapHeight() / 1000.0f * textPosition.getFontSizeInPt());
        //bottomY = this.textPositions.get(this.textPositions.size()-1).getEndY();// getY() + this.textPositions.get(this.textPositions.size()-1).getHeight();
        midX = (topX + bottomX)/2.0f;
        midY = (topY + bottomY)/2.0f;
        this.dx = bottomX-topX;
        this.dy = bottomY-topY;
        StringBuilder sb = new StringBuilder();
        for (TextPosition t : this.textPositions) {
            sb.append(t.toString());
        }
        text = sb.toString();
        isOmittableText(this);
    }

    public TextRectangle(float topX, float topY, float bottomX, float bottomY, String text, int page, int rotation) {
        this.topX = topX;
        this.topY = topY;
        this.bottomX = bottomX;
        this.bottomY = bottomY;
        this.midX = (topX + bottomX)/2.0f;
        this.midY = (topY + bottomY)/2.0f;
        this.dx = bottomX-topX;
        this.dy = bottomY-topY;
        this.text = text;
        this.page = page;
        this.rotation = rotation;
    }

    public TextRectangle(TextRectangle other) {
        this.topX = other.topX;
        this.topY = other.topY;
        this.bottomX = other.bottomX;
        this.bottomY = other.bottomY;
        this.midX = other.midX;
        this.midY = other.midY;
        this.dx = other.dx;
        this.dy = other.dy;
        this.text = other.text;
        this.page = other.page;
        this.rotation = other.rotation;
        this.fontName = other.fontName;
        this.fontSize = other.fontSize;
        this.textPositions = other.textPositions;
        this.isIndented = other.isIndented;
        this.isOmittable = other.isOmittable;
        this.possibleTable = other.possibleTable;
        this.width = other.width;
        this.height = other.height;
        this.widthOfSpace = other.widthOfSpace;
        this.order = other.order;
        this.pageWidth = other.pageWidth;
        this.pageHeight = other.pageHeight;
    }

    static int getAngle(TextPosition text)
    {
        Matrix m = text.getTextMatrix().clone();
        m.concatenate(text.getFont().getFontMatrix());
        return (int) Math.round(Math.toDegrees(Math.atan2(m.getShearY(), m.getScaleY())));
    }

    public static boolean isOmittableText(TextRectangle t) {
        if (t.getRotation() == 90) {
            t.setOmittable(t.getMidX() < t.getPageWidth()*0.05 || t.getMidX() > t.getPageWidth()*0.95);
            return t.getMidX() < t.getPageWidth()*0.05 || t.getTopX() > t.getPageWidth()*0.95;
        }
        t.setOmittable(t.getMidY() < t.getPageHeight()*0.05 || t.getMidY() > t.getPageHeight()*0.95);
        return t.getMidY() < t.getPageHeight()*0.05 || t.getMidY() > t.getPageHeight()*0.95;
    }


    public static boolean isOmittableTexts(List<TextRectangle> rectangles, int page) {
        if (rectangles == null) return false;
        rectangles.forEach(TextRectangle::isOmittableText);
        if (rectangles.stream().filter(TextRectangle::isOmittable).count() == rectangles.size()) return true;
        if (page > 0) return false;
        String data = compose(rectangles);
        if (data.toLowerCase().contains("all rights reserved") || data.toLowerCase().contains("number:") || data.toLowerCase().contains("open access") || data.toLowerCase().contains("creative commons")) {
            setOmittables(rectangles);
            return true;
        }
        if (data.toLowerCase().contains("email") || data.toLowerCase().contains("u.s.") || data.toLowerCase().contains("doe") || data.toLowerCase().contains("phone") || data.toLowerCase().contains("fax") || data.toLowerCase().contains("corresponding author") || data.toLowerCase().contains("resulting proof")) {
            setOmittables(rectangles);
            return true;
        }
        if (data.toLowerCase().contains("non-commercial") || data.toLowerCase().contains("the author") || data.toLowerCase().contains("grant") || data.toLowerCase().contains("award")) {
            setOmittables(rectangles);
            return true;
        }
        if (data.contains("University") || data.contains("©") || data.contains("\u0002") || data.toLowerCase().contains("manuscript") || data.toLowerCase().contains("legal disclaimers")) {
            setOmittables(rectangles);
            return true;
        }
        if (data.toLowerCase().contains("advance access") || data.toLowerCase().contains("publisher") || data.toLowerCase().contains("contributed equally")) {
            setOmittables(rectangles);
            return true;
        }
        if (data.toLowerCase().contains("properly cited") || data.toLowerCase().contains("current address") || data.toLowerCase().contains("whom correspondence should")) {
            setOmittables(rectangles);
            return true;
        }
        return false;
    }


    private static String getDefaultFont(List<TextRectangle> items) {
        Map<String, Integer> fontCount = new HashMap<>();
        String fontName = "";
        int max = 0;
        for (TextRectangle r: items) {
            fontCount.putIfAbsent(r.getFontName(),  0);
            fontCount.put(r.getFontName(), fontCount.get(r.getFontName())+1);
            if (fontCount.get(r.getFontName()) > max) {
                max = fontCount.get(r.getFontName());
                fontName = r.getFontName();
            }
        }
        return fontName;
    }

    public static List<TextRectangle> getNeighborsAdjacentFont(TextRectangle a, Map<Integer, TextRectangle> pageItems, boolean up) {
        List<TextRectangle> neighbors = getNeighbors(a, pageItems);
        //Map<String, Integer> fontCount = new HashMap<>();
        String fontName = getDefaultFont(neighbors);
        List<TextRectangle> visited = new ArrayList<>();
        if (up) {
            if (a.getRotation() == 0) {
                float ypos = a.getTopY();
                if (neighbors.size() > 0) ypos = neighbors.get(0).getTopY();
                for (int i = neighbors.size() > 1 ? neighbors.get(0).getOrder() - 1 : a.getOrder() - 1; i >= 0; i--) {
                    if (pageItems.get(i).isOmittable() || visited.contains(pageItems.get(i))) continue;
                    //line above
                    if (pageItems.get(i).getTopY() < ypos && Math.abs(pageItems.get(i).getTopY() - ypos) > 0.8*a.getHeight()) {
                        List<TextRectangle> result = getNeighbors(pageItems.get(i), pageItems);
                        String resultFontName = getDefaultFont(result);
                        if (resultFontName.equals(fontName)) return result;
                        else visited.addAll(result);
                    }
                    // line wrapping to next column
                    if (pageItems.get(i).getTopY() > ypos && Math.abs(pageItems.get(i).getTopY() - ypos) > a.getPageHeight()/4.0 && Math.abs(pageItems.get(i).getTopX() - neighbors.get(0).getTopX()) > a.getPageWidth()/4.0) {
                        List<TextRectangle> result = getNeighbors(pageItems.get(i), pageItems);
                        String resultFontName = getDefaultFont(result);
                        if (resultFontName.equals(fontName)) return result;
                        else visited.addAll(result);
                    }
                }
            } else { // rotation 90 degrees
                float xpos = a.getTopX();
                if (neighbors.size() > 0) xpos = neighbors.get(0).getTopX();
                for (int i = neighbors.size() > 1 ? neighbors.get(0).getOrder() - 1 : a.getOrder() - 1; i >= 0; i--) {
                    if (pageItems.get(i).isOmittable() || visited.contains(pageItems.get(i))) continue;
                    //line above
                    if (pageItems.get(i).getTopX() < xpos && Math.abs(pageItems.get(i).getTopX() - xpos) > 0.8*a.getHeight()) {
                        List<TextRectangle> result = getNeighbors(pageItems.get(i), pageItems);
                        String resultFontName = getDefaultFont(result);
                        if (resultFontName.equals(fontName)) return result;
                        else visited.addAll(result);
                    }
                    // line wrapping to next column
                    if (pageItems.get(i).getTopX() > xpos && Math.abs(pageItems.get(i).getTopY() - neighbors.get(0).getTopY()) > a.getPageHeight()/4.0 && Math.abs(pageItems.get(i).getTopX() - xpos) > a.getPageWidth()/4.0) {
                        List<TextRectangle> result = getNeighbors(pageItems.get(i), pageItems);
                        String resultFontName = getDefaultFont(result);
                        if (resultFontName.equals(fontName)) return result;
                        else visited.addAll(result);
                    }
                }
            }
        } else { // down
            if (a.getRotation() == 0) {
                float ypos = a.getTopY();
                if (neighbors.size() > 1) ypos = neighbors.get(neighbors.size()-1).getTopY();
                for (int i = (neighbors.size() > 0 ? neighbors.get(neighbors.size()-1).getOrder() + 1 : a.getOrder() + 1); pageItems.get(i) != null; i++) {
                    if (pageItems.get(i).isOmittable() || visited.contains(pageItems.get(i))) continue;
                    if (pageItems.get(i).getTopY() > ypos && Math.abs(pageItems.get(i).getTopY() - ypos) > 0.8*a.getHeight()) {
                        List<TextRectangle> result = getNeighbors(pageItems.get(i), pageItems);
                        String resultFontName = getDefaultFont(result);
                        if (resultFontName.equals(fontName)) return result;
                        else visited.addAll(result);
                    }
                    if (pageItems.get(i).getTopY() < ypos && Math.abs(pageItems.get(i).getTopY() - ypos) > a.getPageHeight()/4.0 && Math.abs(pageItems.get(i).getTopX() - neighbors.get(0).getTopX()) > a.getPageWidth()/4.0) {
                        List<TextRectangle> result = getNeighbors(pageItems.get(i), pageItems);
                        String resultFontName = getDefaultFont(result);
                        if (resultFontName.equals(fontName)) return result;
                        else visited.addAll(result);
                    }
                }
            } else { // rotation 90
                float xpos = a.getTopX();
                if (neighbors.size() > 1) xpos = neighbors.get(neighbors.size()-1).getTopX();
                for (int i = (neighbors.size() > 0 ? neighbors.get(neighbors.size()-1).getOrder() + 1 : a.getOrder() + 1); pageItems.get(i) != null; i++) {
                    if (pageItems.get(i).isOmittable() || visited.contains(pageItems.get(i))) continue;
                    if (pageItems.get(i).getTopX() > xpos && Math.abs(pageItems.get(i).getTopX() - xpos) > 0.8*a.getHeight()) {
                        List<TextRectangle> result = getNeighbors(pageItems.get(i), pageItems);
                        String resultFontName = getDefaultFont(result);
                        if (resultFontName.equals(fontName)) return result;
                        else visited.addAll(result);
                    }
                    if (pageItems.get(i).getTopX() < xpos && Math.abs(pageItems.get(i).getTopY() - neighbors.get(0).getTopY()) > a.getPageHeight()/4.0 && Math.abs(pageItems.get(i).getTopX() - xpos) > a.getPageWidth()/4.0) {
                        List<TextRectangle> result = getNeighbors(pageItems.get(i), pageItems);
                        String resultFontName = getDefaultFont(result);
                        if (resultFontName.equals(fontName)) return result;
                        else visited.addAll(result);
                    }
                }
            }
        }
        return null;
    }


    public static List<TextRectangle> getNeighborsAdjacent(TextRectangle a, Map<Integer, TextRectangle> pageItems, boolean up) {
        List<TextRectangle> neighbors = getNeighbors(a, pageItems);
        //List<TextRectangle> visited = new ArrayList<>();
        if (up) {
          if (a.getRotation() == 0) {
              float ypos = a.getTopY();
              if (neighbors.size() > 0) ypos = neighbors.get(0).getTopY();
              for (int i = neighbors.size() > 1 ? neighbors.get(0).getOrder() - 1 : a.getOrder() - 1; i >= 0; i--) {
                if (isOmittableText(pageItems.get(i))) continue;
                //line above
                if (pageItems.get(i).getTopY() < ypos && Math.abs(pageItems.get(i).getTopY() - ypos) > 0.8*a.getHeight()) {
                    return getNeighbors(pageItems.get(i), pageItems);
                }
                // line wrapping to next column
                if (pageItems.get(i).getTopY() > ypos && Math.abs(pageItems.get(i).getTopY() - ypos) > a.getPageHeight()/4.0 && Math.abs(pageItems.get(i).getTopX() - neighbors.get(0).getTopX()) > a.getPageWidth()/4.0) {
                    return getNeighbors(pageItems.get(i), pageItems);
                }
              }
          } else { // rotation 90
              float xpos = a.getTopX();
              if (neighbors.size() > 0) xpos = neighbors.get(0).getTopX();
              for (int i = neighbors.size() > 1 ? neighbors.get(0).getOrder() - 1 : a.getOrder() - 1; i >= 0; i--) {
                  if (isOmittableText(pageItems.get(i))) continue;
                  //line above
                  if (pageItems.get(i).getTopX() < xpos && Math.abs(pageItems.get(i).getTopX() - xpos) > 0.8*a.getHeight()) {
                      return getNeighbors(pageItems.get(i), pageItems);
                  }
                  // line wrapping to next column
                  if (pageItems.get(i).getTopX() > xpos && Math.abs(pageItems.get(i).getTopY() - neighbors.get(0).getTopY()) > a.getPageHeight()/4.0 && Math.abs(pageItems.get(i).getTopX() - xpos) > a.getPageWidth()/4.0) {
                      return getNeighbors(pageItems.get(i), pageItems);
                  }
              }
          }
        } else { // down
            if (a.getRotation() == 0) {
                float ypos = a.getTopY();
                if (neighbors.size() > 1) ypos = neighbors.get(neighbors.size()-1).getTopY();
                for (int i = (neighbors.size() > 0 ? neighbors.get(neighbors.size()-1).getOrder() + 1 : a.getOrder() + 1); pageItems.get(i) != null; i++) {
                    if (isOmittableText(pageItems.get(i))) continue;
                    if (pageItems.get(i).getTopY() > ypos && Math.abs(pageItems.get(i).getTopY() - ypos) > 0.8*a.getHeight()) {
                        return getNeighbors(pageItems.get(i), pageItems);
                    }
                    if (pageItems.get(i).getTopY() < ypos && Math.abs(pageItems.get(i).getTopY() - ypos) > a.getPageHeight()/4.0 && Math.abs(pageItems.get(i).getTopX() - neighbors.get(0).getTopX()) > a.getPageWidth()/4.0) {
                        return getNeighbors(pageItems.get(i), pageItems);
                    }
                }
            } else { // rotation 90
                float xpos = a.getTopX();
                if (neighbors.size() > 1) xpos = neighbors.get(neighbors.size()-1).getTopX();
                for (int i = (neighbors.size() > 0 ? neighbors.get(neighbors.size()-1).getOrder() + 1 : a.getOrder() + 1); pageItems.get(i) != null; i++) {
                    if (isOmittableText(pageItems.get(i))) continue;
                    if (pageItems.get(i).getTopX() > xpos && Math.abs(pageItems.get(i).getTopX() - xpos) > 0.8*a.getHeight()) {
                        return getNeighbors(pageItems.get(i), pageItems);
                    }
                    if (pageItems.get(i).getTopX() < xpos && Math.abs(pageItems.get(i).getTopY() - neighbors.get(0).getTopY()) > a.getPageHeight()/4.0 && Math.abs(pageItems.get(i).getTopX() - xpos) > a.getPageWidth()/4.0) {
                        return getNeighbors(pageItems.get(i), pageItems);
                    }
                }
            }
        }
        return null;
    }

    public static List<TextRectangle> getNeighbors(TextRectangle a, Map<Integer, TextRectangle> pageItems) {
        int order = a.getOrder();
        int max = 0;
        if (pageItems instanceof TreeMap) max = ((TreeMap<Integer, TextRectangle>)pageItems).lastKey();
        else for (Integer value: pageItems.keySet()) if (value > max) max = value;
        List<TextRectangle> result2 = new ArrayList<>();
        for (int i = order+1; i <= max; i++) {
            //if (pageItems.get(i).isOmittable()) continue;
            if (pageItems.get(i) != null && sameLine(a, pageItems.get(i))) result2.add(pageItems.get(i));
            else if (i+1 <= max && pageItems.get(i+1) != null && sameLine(a, pageItems.get(i+1))) {
                result2.add(pageItems.get(i));
                result2.add(pageItems.get(i+1));
                i++;
            }
            else break;
        }
        List<TextRectangle> result = new ArrayList<>();
        for (int i = order-1; i >= 0; i--) {
            //if (pageItems.get(i).isOmittable()) continue;
            if (pageItems.get(i) != null && sameLine(a, pageItems.get(i))) result.add(pageItems.get(i));
            else if (i-1 >= 0 && pageItems.get(i-1) != null && sameLine(a, pageItems.get(i-1))) {
                result.add(pageItems.get(i));
                result.add(pageItems.get(i-1));
                i--;
            }
            else break;
        }
        Collections.reverse(result);
        result.add(a);
        result.addAll(result2);
        return result;
    }

    public static boolean sameLine(TextRectangle A, TextRectangle B) {
        final List<Float> Alist = new ArrayList<>();
        final List<Float> Blist = new ArrayList<>();
        if (A.getRotation() == 0) {
            A.getTextPositions().forEach(x -> Alist.add(x.getY()));
            B.getTextPositions().forEach(x -> Blist.add(x.getY()));
            for (Float ay: Alist) for (Float by: Blist) {
                if (Math.abs(ay - by) < 0.5*Math.max(A.getHeight(), B.getHeight())) return true;
            }
            return B.textPositions.get(0).getY() < A.textPositions.get(A.textPositions.size() - 1).getY() && B.textPositions.get(0).getFontSizeInPt() < A.textPositions.get(A.textPositions.size() - 1).getFontSizeInPt()
                    && B.textPositions.get(0).getY() + B.textPositions.get(0).getFontSizeInPt()+3.0 > A.textPositions.get(0).getY();
        } else {
            A.getTextPositions().forEach(x -> Alist.add(x.getX()));
            B.getTextPositions().forEach(x -> Blist.add(x.getX()));
            for (Float ax: Alist) for (Float bx: Blist) {
                if (Math.abs(ax - bx) < 0.5*Math.max(A.getHeight(), B.getHeight())) return true;
            }
            return false;
        }
        //if (A.getRotation() == 0 && Math.abs(A.getTopY() - B.getTopY()) < 0.5*A.getHeight()) return true;
        //return A.getRotation() == 90 && Math.abs(A.getTopX() - B.getTopX()) < 0.5 * A.getHeight();
    }

    public static float distanceX(TextRectangle A, TextRectangle B) {
        return Math.abs(B.topX - A.bottomX);
    }

    public static float distanceY(TextRectangle A, TextRectangle B) {
        return Math.abs(B.topY - A.bottomY);
    }

    public static boolean commonFont(List<TextRectangle> line1, List<TextRectangle> line2) {
        final Set<String> fonts = new HashSet<>();
        line1.forEach(x -> x.getTextPositions().forEach(y -> fonts.add(y.getFont().getName() + y.getFontSizeInPt())));
        for (TextRectangle t : line2) {
            for (TextPosition tp : t.getTextPositions())
                if (fonts.contains(tp.getFont().getName()+tp.getFontSizeInPt())) return true;
        }
        return false;
    }

    public static String checkSpace(TextRectangle A, TextRectangle B) {
        //A.getTextPositions().get(0).getFont().getStringWidth(" ")/1000.0*A.getTextPositions().get(0).getFontSizeInPt();
        //float w1 = (float) A.getTextPositions().stream().mapToDouble(x -> {try { return x.getFont().getStringWidth(" ") / 1000.0 * x.getFontSizeInPt(); } catch (IOException e) {log.info(e.getMessage()); return 0.0;}}).max().orElseThrow(NoSuchElementException::new);
        //float w2 = (float) B.getTextPositions().stream().mapToDouble(x -> {try { return x.getFont().getStringWidth(" ") / 1000.0 * x.getFontSizeInPt(); } catch (IOException e) {log.info(e.getMessage()); return 0.0;}}).max().orElseThrow(NoSuchElementException::new);
        float w1 = (float)A.getTextPositions().stream().mapToDouble(TextPosition::getWidthOfSpace).max().orElseThrow(NoSuchElementException::new);
        float w2 = (float)B.getTextPositions().stream().mapToDouble(TextPosition::getWidthOfSpace).max().orElseThrow(NoSuchElementException::new);
        float spaceWidth = Math.max(w1, w2);
        //float spaceWidth = Math.max(A.getWidthOfSpace(), B.getWidthOfSpace());
        if (A.getRotation() == 0 && distanceX(A, B) < 0.4*spaceWidth) return ""; // 0.6?
        if (A.getRotation() == 0 && distanceX(A, B) > 5.0*spaceWidth) return "\t";
        if (A.getRotation() == 90 && distanceY(A, B) < 0.4*spaceWidth) return "";
        if (A.getRotation() == 90 && distanceY(A, B) > 5.0*spaceWidth) return "\t";
        return " ";
    }

    public static String checkSpaceTable(TextRectangle A, TextRectangle B) {
        float w1 = (float)A.getTextPositions().stream().mapToDouble(TextPosition::getWidthOfSpace).max().orElseThrow(NoSuchElementException::new);
        float w2 = (float)B.getTextPositions().stream().mapToDouble(TextPosition::getWidthOfSpace).max().orElseThrow(NoSuchElementException::new);
        float spaceWidth = Math.max(w1, w2); //Math.max(A.getWidthOfSpace(), B.getWidthOfSpace());
        if (A.getRotation() == 0 && distanceX(A, B) < 0.3*spaceWidth) return ""; // 0.6?
        if (A.getRotation() == 0 && distanceX(A, B) > 3.0*spaceWidth) return "\t";
        if (A.getRotation() == 90 && distanceY(A, B) < 0.3*spaceWidth) return "";
        if (A.getRotation() == 90 && distanceY(A, B) > 3.0*spaceWidth) return "\t";
        return " ";
    }

    // ***
    public static List<TextRectangle> splitFontChange(List<TextRectangle> rectangles) {
        float fontSize = rectangles.get(0).getFontSize();
        String fontName = rectangles.get(0).getFontName();
        List<TextRectangle> result = new ArrayList<>();
        result.add(rectangles.get(0));
        for (int i = 1; i < rectangles.size(); i++) {
            if (rectangles.get(i).getFontName().equals(fontName) && rectangles.get(i).getFontSize() == fontSize) result.add(rectangles.get(i));
        }
        if (result.size() != rectangles.size()) return result;
        else return null; // no change occurred, all items the same as first
    }

    // ***
    public static List<TextRectangle> splitFontChangeRemainder(List<TextRectangle> rectangles, List<TextRectangle> splitFont) {
        List<TextRectangle> result = new ArrayList<>(rectangles);
        if (splitFont == null || splitFont.size() == 0) return result;
        for (int i = 0; i < result.size(); i++) {
            if (splitFont.contains(result.get(i))) {
                result.remove(i);
                i--;
            }
        }
        return result;
    }

    public static boolean isLoneItem(TextRectangle A, Map<Integer, TextRectangle> pageItems) {
        List<TextRectangle> result;
        return (result = getNeighbors(A, pageItems)).size() == 1 && result.get(0).equals(A);
    }

    /**
     * Detects indentation in the line, relative to the previous line, for paragraphs and other aspects of PDF parsing.
     * @param lineA the previous line with items in order to check against (such as from getNeighbors)
     * @param lineB the line of interest with items in order (can be the line below or other line in paragraph) to see if its indented.
     * @return true if lineB is indented related to lineA
     */
    public static boolean isIndented(List<TextRectangle> lineA, List<TextRectangle> lineB) {
        if (lineA.get(0).getRotation() == 0 && lineA.get(0).getTopX() < lineB.get(0).getTopX() && Math.abs(lineB.get(0).getTopX() - lineA.get(0).getTopX()) > 20.0 * lineA.get(0).getWidthOfSpace()) return false;
        else if (lineA.get(0).getRotation() == 90 && lineA.get(0).getTopY() < lineB.get(0).getTopY() && Math.abs(lineA.get(0).getTopY() - lineB.get(0).getTopY()) > 20.0 * lineA.get(0).getWidthOfSpace()) return false;
        if (lineA.get(0).isIndented() && lineA.get(0).getRotation() == 0 && Math.abs(lineB.get(0).getTopX() - lineA.get(0).getTopX()) < 2.0 * lineA.get(0).getWidthOfSpace()) {
            lineB.get(0).setIndented(true);
            return true;
        }
        if (lineA.get(0).isIndented() && lineA.get(0).getRotation() == 90 && Math.abs(lineB.get(0).getTopY() - lineA.get(0).getTopY()) < 2.0 * lineA.get(0).getWidthOfSpace()) {
            lineB.get(0).setIndented(true);
            return true;
        }
        if (lineA.get(0).isIndented() && lineA.get(0).getRotation() == 0 && lineA.get(0).getTopX() > lineB.get(0).getTopX() && Math.abs(lineB.get(0).getTopX() - lineA.get(0).getTopX()) > 1.0 * lineA.get(0).getWidthOfSpace()) {
            return false; // unindented, default isIndented already false
        }
        if (lineA.get(0).isIndented() && lineA.get(0).getRotation() == 90 && lineA.get(0).getTopY() > lineB.get(0).getTopY() && Math.abs(lineB.get(0).getTopY() - lineA.get(0).getTopY()) > 1.0 * lineA.get(0).getWidthOfSpace()) {
            return false; // unindented, default isIndented already false
        }
        if (lineA.get(0).getRotation() == 0 && lineA.get(0).getTopX() < lineB.get(0).getTopX() && Math.abs(lineB.get(0).getTopX() - lineA.get(0).getTopX()) > 2.0 * lineA.get(0).getWidthOfSpace()) {
            lineB.get(0).setIndented(true);
            return true;
        }
        if (lineA.get(0).getRotation() == 90 && lineA.get(0).getTopY() < lineB.get(0).getTopY() && Math.abs(lineA.get(0).getTopY() - lineB.get(0).getTopY()) > 2.0 * lineA.get(0).getWidthOfSpace()) {
            lineB.get(0).setIndented(true);
            return true;
        }
        return false;
    }

    public static boolean isTable(List<TextRectangle> line) {
        int threshold = line.size()/2;
        int count = 0;
        for (TextRectangle item: line) {
            if (item.isPossibleTable()) {
                count++;
                if (count >= threshold) return true;
            }
        }
        return false;
    }

    public static float lineWidth(List<TextRectangle> line) {
        if (line == null || line.size() == 0) return 0.0f;
        if (line.get(0).getRotation() == 0) {
            return line.get(line.size()-1).getBottomX() - line.get(0).getTopX();
        } else return line.get(line.size()-1).getBottomY() - line.get(0).getTopY();
    }

    public static List<TextRectangle> getLine(TextRectangle item, Map<Integer, TextRectangle> all) {
        int order = item.getOrder();
        List<TextRectangle> result2 = new ArrayList<>();
        int max = 0;
        if (all instanceof TreeMap) max = ((TreeMap<Integer, TextRectangle>)all).lastKey();
        else for (Integer value: all.keySet()) if (value > max) max = value;
        for (int i = order; i <= max; i++) {
            if (all.get(i) == null) continue;
            float height = Math.max(all.get(i).getHeight(), item.getHeight());
            if (item.getRotation() == 0 && Math.abs(all.get(i).getTopY() - item.getTopY()) < 0.6*height) result2.add(all.get(i));
            else if (item.getRotation() == 90 && Math.abs(all.get(i).getTopX() - item.getTopX()) < 0.6*height) result2.add(all.get(i));
            else break;
        }
        List<TextRectangle> result = new ArrayList<>();
        for (int i = order-1; i >= 0; i--) {
            if (all.get(i) == null) continue;
            float height = Math.max(all.get(i).getHeight(), item.getHeight());
            if (item.getRotation() == 0 && Math.abs(all.get(i).getTopY() - item.getTopY()) < 0.6*height) result.add(all.get(i));
            else if (item.getRotation() == 90 && Math.abs(all.get(i).getTopX() - item.getTopX()) < 0.6*height) result.add(all.get(i));
            else break;
        }
        Collections.reverse(result);
        result.addAll(result2);
        return result;
    }

    public static String compose(List<TextRectangle> items) {
        StringBuilder result = new StringBuilder(items.get(0).toString());   //(items.get(0).getText());
        for (int i = 0; i < items.size()-1; i++) {
            TextRectangle item1 = items.get(i), item2 = items.get(i+1);
            //if (sameLine(item1, item2)) {   // removed 3/9
                String separator = checkSpace(item1, item2);
                result.append(separator).append(item2.toString(item1));//.append(item2.getText());
                //if (distanceX(item1, item2) < 0.8*item1.getWidthOfSpace()) result += item2.getText();
                //else if (distanceX(item1, item2) > 3*item1.getWidthOfSpace()) {
                if (separator.equals("\t")) { // start new if
                    item1.setPossibleTable(true);
                    item2.setPossibleTable(true);
                } // end new if
                //    result += "\t" + item2.getText();
                //}
                //else result += " " + item2.getText();
            //}
        }
        return result.toString().replace("ﬁ", "fi"); // FOR NOW!!
    }

    public static String composeNormal(List<TextRectangle> items) {
        StringBuilder result = new StringBuilder(items.get(0).getText());   //(items.get(0).getText());
        for (int i = 0; i < items.size()-1; i++) {
            TextRectangle item1 = items.get(i), item2 = items.get(i+1);
            if (sameLine(item1, item2)) {
                String separator = checkSpace(item1, item2);
                result.append(separator).append(item2.getText());//.append(item2.getText());
                //if (distanceX(item1, item2) < 0.8*item1.getWidthOfSpace()) result += item2.getText();
                //else if (distanceX(item1, item2) > 3*item1.getWidthOfSpace()) {
                if (separator.equals("\t")) { // start new if
                    item1.setPossibleTable(true);
                    item2.setPossibleTable(true);
                } // end new if
                //    result += "\t" + item2.getText();
                //}
                //else result += " " + item2.getText();
            }
        }
        return result.toString().replace("ﬁ", "fi"); // FOR NOW!!
    }

    private static boolean intersects(float xmin1, float xmax1, float xmin2, float xmax2) {
        return ((xmin2 >= xmin1 && xmin2 <= xmax1) || (xmax2 >= xmin1 && xmax2 <= xmax1) || (xmin2 <= xmin1 && xmax2 >= xmax1));
    }

    private static boolean intersects(TextRectangle item1, TextRectangle item2) {
        float xStart1 = item1.textPositions.get(0).getX();
        float xEnd1 = item1.textPositions.get(item1.textPositions.size()-1).getEndX();
        float xStart2 = item2.textPositions.get(0).getX();
        float xEnd2 = item2.textPositions.get(item2.textPositions.size()-1).getEndX();
        return intersects(xStart1, xEnd1, xStart2, xEnd2);
        //return intersects(item1.textPositions.get(0).getX(), item1.textPositions.get(item1.textPositions.size()-1).getEndX(),
        //        item2.textPositions.get(0).getX(), item2.textPositions.get(item2.textPositions.size()-1).getEndX());
    }

    private static boolean intersects(List<TextRectangle> item1, List<TextRectangle> item2) {
        float xStart1 = item1.get(0).textPositions.get(0).getX();
        float xEnd1 = item1.get(item1.size()-1).textPositions.get(item1.get(item1.size()-1).textPositions.size()-1).getEndX();
        float xStart2 = item2.get(0).textPositions.get(0).getX();
        float xEnd2 = item2.get(item2.size()-1).textPositions.get(item2.get(item2.size()-1).textPositions.size()-1).getEndX();
        return intersects(xStart1, xEnd1, xStart2, xEnd2);
    }

    private static Set<Integer> findInterSections(List<TextRectangle> leftMost, Map<Integer, List<List<TextRectangle>>> clusterMap) {
        Set<Integer> result = new TreeSet<>();
        for (int i = 0; i < clusterMap.size(); i++) {
            if (clusterMap.get(i).size() > 0 && intersects(leftMost, clusterMap.get(i).get(0))) {
                result.add(i);
            }
        }
        return result;
    }

    public static boolean completelyLeft(List<TextRectangle> item, List<TextRectangle> check) {
        float xStart = item.get(0).textPositions.get(0).getX();
        float xEnd = item.get(item.size()-1).textPositions.get(item.get(item.size()-1).textPositions.size()-1).getEndX();
        float xStart2 = check.get(0).textPositions.get(0).getX();
        //float xEnd2 = check.textPositions.get(check.textPositions.size()-1).getEndX();
        return (xStart < xStart2 & xEnd < xStart2);
    }

    public static boolean toLeft(List<TextRectangle> item, List<TextRectangle> check) {
        return item.get(0).textPositions.get(0).getX() < check.get(0).textPositions.get(0).getX();
    }

    // returns the new paragraph with every line adjusted
    public static String adjustTableLines(List<List<TextRectangle>> previousLines, List<TextRectangle> line) {
        final Map<Integer, List<List<TextRectangle>>> clusterMap = new TreeMap<>();
        for (int i = 0; i < previousLines.size(); i++) {
            clusterMap.put(i, clusterTableLine(previousLines.get(i)));
        }
        clusterMap.put(previousLines.size(), clusterTableLine(line));
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < clusterMap.size(); i++) rows.add("");
        while (clusterMap.keySet().stream().filter(x -> clusterMap.get(x).size() == 0).count() != clusterMap.size()) {
            int index = -1;
            List<TextRectangle> leftMost = null;
            for (int i = 0; i < clusterMap.size(); i++) {
                if (index == -1 && clusterMap.get(i).size() > 0) {
                    index = i;
                    leftMost = clusterMap.get(i).get(0);
                } else if (index != -1 && clusterMap.get(i).size() > 0 && toLeft(clusterMap.get(i).get(0), leftMost)) {
                    index = i;
                    leftMost = clusterMap.get(i).get(0);
                }
            }
            Set<Integer> items = findInterSections(leftMost, clusterMap);
            for (int i = 0; i < clusterMap.size(); i++) {
                if (items.contains(i)) {
                    rows.set(i, rows.get(i) + compose(clusterMap.get(i).get(0)) + "\t");
                    List<List<TextRectangle>> element = clusterMap.get(i);
                    element.remove(0);
                    clusterMap.put(i, element);
                } else {
                    rows.set(i, rows.get(i) + "\t");
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            sb.append(rows.get(i));
            if (i != rows.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    public static List<List<TextRectangle>> clusterTableLine(List<TextRectangle> items) {
        List<List<TextRectangle>> result = new ArrayList<>();
        List<TextRectangle> current = new ArrayList<>();
        String separator;
        for (int i = 0; i < items.size()-1; i++) {
            TextRectangle item1 = items.get(i), item2 = items.get(i+1);
            //if (sameLine(item1, item2)) {   // ALREADY ASSUMED SAME LINE? 3/9/22
                separator = checkSpaceTable(item1, item2);
                if (separator.equals("\t")) {
                    current.add(item1);
                    result.add(current);
                    current = new ArrayList<>();
                } else {
                    current.add(item1);
                }
            //}
        }
        current.add(items.get(items.size()-1));
        result.add(current);
        return result;
    }


    public static String composeTable(List<TextRectangle> items) {
        List<Integer> positions = new ArrayList<>();
        positions.add(0);
        StringBuilder result = new StringBuilder(items.get(0).toString().replace("ﬁ", "fi"));  //(items.get(0).getText());
        for (int i = 0; i < items.size()-1; i++) {
            TextRectangle item1 = items.get(i), item2 = items.get(i+1);
            if (sameLine(item1, item2)) {
                String separator = checkSpaceTable(item1, item2);
                result.append(separator);
                positions.add(result.toString().length());
                result.append(item2.toString().replace("ﬁ", "fi")); //append(item2.getText());
                //if (distanceX(item1, item2) < 0.8*item1.getWidthOfSpace()) result += item2.getText();
                //else if (distanceX(item1, item2) > 3*item1.getWidthOfSpace()) {
                if (separator.equals("\t")) { // start new if
                    item1.setPossibleTable(true);
                    item2.setPossibleTable(true);
                } // end new if
                //    result += "\t" + item2.getText();
                //}
                //else result += " " + item2.getText();
            }
        }
        return result.toString();
    }

    public static void setOmittables(List<TextRectangle> rectangles) {
        rectangles.forEach(x -> x.setOmittable(true));
    }
    public String toString(TextRectangle previous) {
        String result = "";
        for (int i = 0; i < textPositions.size(); i++) {
            TextPosition t = textPositions.get(i);
            float midY = t.getY()+t.getFont().getFontDescriptor().getCapHeight()/2000*t.getFontSizeInPt();
            float Y = t.getY();
            float fontSize = t.getFontSizeInPt();
            boolean superscript = false;
            if (i == 0) {
                TextPosition p = previous.textPositions.get(previous.textPositions.size()-1);
                if (p.getFontSizeInPt() > t.getFontSizeInPt() && t.getY()+t.getFontSizeInPt() < p.getY() + p.getFont().getFontDescriptor().getCapHeight()/2000.0*p.getFontSizeInPt()) {
                    result += "^^(" + t.toString().replace("ﬂ", "fl") + ")";
                    superscript = true;
                }
            }
            if (!superscript) for (int j = 0; j < textPositions.size(); j++) {
                if (j == i) continue;
                TextPosition t2 = textPositions.get(j);
                float midY2 = t2.getY()+t2.getFont().getFontDescriptor().getCapHeight()/2000*t2.getFontSizeInPt();
                float Y2 = t2.getY();
                float fontSize2 = t2.getFontSizeInPt();
                if (midY < Y2 && fontSize < fontSize2) {
                    result += "^^(" + t.toString().replace("ﬂ", "fl");
                    TextPosition t3 = i+1 < textPositions.size() ? textPositions.get(i+1) : null;
                    while (i+1 < textPositions.size() && t3.getY() + t3.getFont().getFontDescriptor().getCapHeight()/2000*t3.getFontSizeInPt() < Y2 && t3.getFontSizeInPt() < fontSize2) {
                        i++;
                        result += textPositions.get(i).toString().replace("ﬂ", "fl");
                        if (i+1 < textPositions.size()) t3 = textPositions.get(i+1);
                    }
                    result += ")";
                    superscript = true;
                    break;
                }
                else if (Y > midY2 && fontSize < fontSize2) { // NEW!
                    result += "__(" + t.toString().replace("ﬂ", "fl");
                    TextPosition t3 = i+1 < textPositions.size() ? textPositions.get(i+1) : null;
                    while (i+1 < textPositions.size() && t3.getY() > midY2 && t3.getFontSizeInPt() < fontSize2) {
                        i++;
                        result += textPositions.get(i);
                        if (i+1 < textPositions.size()) t3 = textPositions.get(i+1);
                    }
                    result += ")";
                    superscript = true;
                    break;
                } // END NEW!
            }
            if (!superscript) result += t.toString().replace("ﬂ", "fl");
        }
        return result;
        //return compose(Collections.singletonList(this));
    }

    public String toString() {
        String result = "";
        for (int i = 0; i < textPositions.size(); i++) {
            TextPosition t = textPositions.get(i);
            float midY = t.getY()+t.getFont().getFontDescriptor().getCapHeight()/2000*t.getFontSizeInPt();
            float Y = t.getY();
            float fontSize = t.getFontSizeInPt();
            boolean superscript = false;
            for (int j = 0; j < textPositions.size(); j++) {
                if (j == i) continue;
                TextPosition t2 = textPositions.get(j);
                float midY2 = t2.getY()+t2.getFont().getFontDescriptor().getCapHeight()/2000*t2.getFontSizeInPt();
                float Y2 = t2.getY();
                float fontSize2 = t2.getFontSizeInPt();
                if (midY < Y2 && fontSize < fontSize2) {
                    result += "^^(" + t.toString().replace("ﬂ", "fl");
                    TextPosition t3 = i+1 < textPositions.size() ? textPositions.get(i+1) : null;
                    while (i+1 < textPositions.size() && t3.getY() + t3.getFont().getFontDescriptor().getCapHeight()/2000*t3.getFontSizeInPt() < Y2 && t3.getFontSizeInPt() < fontSize2) {
                        i++;
                        result += textPositions.get(i).toString().replace("ﬂ", "fl");
                        if (i+1 < textPositions.size()) t3 = textPositions.get(i+1);
                    }
                    result += ")";
                    superscript = true;
                    break;
                }
                else if (Y > midY2 && fontSize < fontSize2) { // NEW!
                    result += "__(" + t.toString().replace("ﬂ", "fl");
                    TextPosition t3 = i+1 < textPositions.size() ? textPositions.get(i+1) : null;
                    while (i+1 < textPositions.size() && t3.getY() > midY2 && t3.getFontSizeInPt() < fontSize2) {
                        i++;
                        result += textPositions.get(i);
                        if (i+1 < textPositions.size()) t3 = textPositions.get(i+1);
                    }
                    result += ")";
                    superscript = true;
                    break;
                } // END NEW!
            }
            if (!superscript) result += t.toString().replace("ﬂ", "fl");
        }
        return result;
        //return compose(Collections.singletonList(this));
    }

}
