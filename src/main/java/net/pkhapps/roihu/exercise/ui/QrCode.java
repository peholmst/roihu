package net.pkhapps.roihu.exercise.ui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/** A QR code of some text, drawn on the server and served to the browser as a PNG image. */
final class QrCode extends Composite<Image> {

    /** Pixels per module, so that a phone reads it from across a room without scaling blur. */
    private static final int SCALE = 8;
    /** The quiet zone around the code that readers need, in modules. */
    private static final int MARGIN = 4;

    private final byte[] png;

    QrCode(String text, String description) {
        png = draw(text);
        getContent().setSrc(DownloadHandler.fromInputStream(event -> new DownloadResponse(
                new ByteArrayInputStream(png), "qr-code.png", "image/png", png.length)));
        getContent().setAlt(description);
        getContent().addClassName("qr-code");
    }

    /** The image as served. */
    byte[] png() {
        return png.clone();
    }

    private static byte[] draw(String text) {
        try {
            var matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 0));
            var size = (matrix.getWidth() + 2 * MARGIN) * SCALE;
            var image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);
            for (var y = 0; y < size; y++) {
                for (var x = 0; x < size; x++) {
                    var moduleX = x / SCALE - MARGIN;
                    var moduleY = y / SCALE - MARGIN;
                    var dark = moduleX >= 0 && moduleY >= 0 && moduleX < matrix.getWidth()
                            && moduleY < matrix.getHeight() && matrix.get(moduleX, moduleY);
                    image.setRGB(x, y, dark ? 0x000000 : 0xFFFFFF);
                }
            }
            var out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (WriterException tooLong) {
            throw new IllegalArgumentException("Too long for a QR code: " + text, tooLong);
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
    }
}
