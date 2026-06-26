package fi.natroutter.foxbot.commands.sticker;

import fi.natroutter.foxlib.FoxLib;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

class StickerResizer {

    public FileUpload resize(File sticker, StickerSize size) throws IOException {
        BufferedImage source = ImageIO.read(sticker);
        if (source == null) {
            throw new IOException("Unsupported image file: " + sticker.getName());
        }

        int target = size.pixels();
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min((double) target / width, (double) target / height);
        int resizedWidth = Math.max(1, (int) Math.round(width * scale));
        int resizedHeight = Math.max(1, (int) Math.round(height * scale));
        int x = (target - resizedWidth) / 2;
        int y = (target - resizedHeight) / 2;

        BufferedImage resizedSticker = resizeImage(source, resizedWidth, resizedHeight);
        if (resizedWidth < width || resizedHeight < height) {
            resizedSticker = sharpen(resizedSticker);
        }

        BufferedImage resized = transparentImage(target, target);
        Graphics2D graphics = resized.createGraphics();
        applyQualityHints(graphics);
        graphics.drawImage(resizedSticker, x, y, null);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(resized, "png", output);

        String fileName = FoxLib.getBasename(sticker) + "-" + size.key() + ".png";
        return FileUpload.fromData(output.toByteArray(), fileName);
    }

    private BufferedImage resizeImage(BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage current = toArgb(source);
        int width = current.getWidth();
        int height = current.getHeight();

        while (width != targetWidth || height != targetHeight) {
            if (width > targetWidth) {
                width = Math.max(targetWidth, width / 2);
            } else {
                width = targetWidth;
            }

            if (height > targetHeight) {
                height = Math.max(targetHeight, height / 2);
            } else {
                height = targetHeight;
            }

            BufferedImage next = transparentImage(width, height);
            Graphics2D graphics = next.createGraphics();
            applyQualityHints(graphics);
            graphics.drawImage(current, 0, 0, width, height, null);
            graphics.dispose();
            current = next;
        }

        return current;
    }

    private BufferedImage toArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }

        BufferedImage converted = transparentImage(source.getWidth(), source.getHeight());
        Graphics2D graphics = converted.createGraphics();
        applyQualityHints(graphics);
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return converted;
    }

    private BufferedImage sharpen(BufferedImage image) {
        float[] sharpenKernel = {
                0.0f, -0.08f, 0.0f,
                -0.08f, 1.32f, -0.08f,
                0.0f, -0.08f, 0.0f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, sharpenKernel), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(image, transparentImage(image.getWidth(), image.getHeight()));
    }

    private BufferedImage transparentImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private void applyQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }
}
