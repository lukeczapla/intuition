package org.magicat.intuition.pdf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.*;

@Getter
@Setter
@EqualsAndHashCode
public class ImageItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 895321550L;

    private ByteImage byteImage;
    @JsonIgnore
    private transient RenderedImage image;
    private float topX, topY;
    private int height, width;

    public ImageItem(RenderedImage image, float topX, float topY) {
        this.image = image;
        this.topX = topX;
        this.topY = topY;
        this.height = image.getHeight();
        this.width = image.getWidth();
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();) {
            ImageIO.write(image, "png", b);
            this.byteImage = new ByteImage(b.toByteArray(), "output.png", "png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveImage(String path) {
        File file = new File(path);
        try {
            ImageIO.write(getImage(), "PNG", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveByteImage(String path) {
        if (!path.toLowerCase().endsWith(".png")) path += ".png";
        try (FileOutputStream outputStream = new FileOutputStream(path)) {
            outputStream.write(byteImage.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreImage() {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(byteImage.getData());) {
            image = ImageIO.read(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

@Getter
@Setter
@AllArgsConstructor
class ByteImage implements Serializable {
    @Serial
    private static final long serialVersionUID = 49818L;

    private byte[] data;
    private String name;
    private String format;
}
