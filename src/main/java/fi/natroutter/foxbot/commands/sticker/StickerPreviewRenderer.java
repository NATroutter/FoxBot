package fi.natroutter.foxbot.commands.sticker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

final class StickerPreviewRenderer {

    private static final Logger log = LoggerFactory.getLogger(StickerPreviewRenderer.class);

    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final int CELL_WIDTH = WIDTH / 2;
    private static final int CELL_HEIGHT = HEIGHT / 2;
    private static final int PADDING = 22;
    private static final int NAME_HEIGHT = 34;
    private static final int MAX_IMAGE_DIMENSION = 4096;

    byte[] renderPage(List<StickerDescriptor> stickers) throws IOException {
        BufferedImage sheet = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        applyQualityHints(graphics);

        graphics.setColor(new Color(0x2B2D31));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        for (int index = 0; index < StickerPickerListener.PAGE_SIZE; index++) {
            int x = (index % 2) * CELL_WIDTH;
            int y = (index / 2) * CELL_HEIGHT;
            StickerDescriptor sticker = index < stickers.size() ? stickers.get(index) : null;
            drawCell(graphics, x, y, index + 1, sticker);
        }

        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(sheet, "png", output);
        return output.toByteArray();
    }

    private void drawCell(Graphics2D graphics, int x, int y, int number, StickerDescriptor sticker) {
        graphics.setColor(new Color(0x313338));
        graphics.fillRect(x + 1, y + 1, CELL_WIDTH - 2, CELL_HEIGHT - 2);
        graphics.setColor(new Color(0x44474E));
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawRect(x + 1, y + 1, CELL_WIDTH - 2, CELL_HEIGHT - 2);

        int imageX = x + PADDING;
        int imageY = y + PADDING + 10;
        int imageWidth = CELL_WIDTH - PADDING * 2;
        int imageHeight = CELL_HEIGHT - PADDING * 2 - NAME_HEIGHT;
        drawChecker(graphics, imageX, imageY, imageWidth, imageHeight);

        if (sticker != null) {
            drawSticker(graphics, sticker, imageX, imageY, imageWidth, imageHeight);
            drawName(graphics, sticker.name(), imageX, y + CELL_HEIGHT - PADDING - 4, imageWidth);
        }

        drawBadge(graphics, x + 14, y + 14, number);
    }

    private void drawSticker(Graphics2D graphics, StickerDescriptor sticker, int x, int y, int width, int height) {
        if (sticker.file() == null) {
            drawPlaceholder(graphics, x, y, width, height, "Missing image");
            log.warn("Sticker preview source is missing for {}", sticker.id());
            return;
        }

        try {
            BufferedImage source = ImageIO.read(sticker.file());
            if (source == null) {
                drawPlaceholder(graphics, x, y, width, height, "Unsupported image");
                log.warn("Sticker preview could not decode {}", sticker.file().getAbsolutePath());
                return;
            }
            if (source.getWidth() > MAX_IMAGE_DIMENSION || source.getHeight() > MAX_IMAGE_DIMENSION) {
                drawPlaceholder(graphics, x, y, width, height, "Image too large");
                log.warn("Sticker preview rejected oversized image {}", sticker.file().getAbsolutePath());
                return;
            }

            double scale = Math.min((double) width / source.getWidth(), (double) height / source.getHeight());
            int targetWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int targetHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
            int targetX = x + (width - targetWidth) / 2;
            int targetY = y + (height - targetHeight) / 2;
            graphics.drawImage(source, targetX, targetY, targetWidth, targetHeight, null);
        } catch (IOException e) {
            drawPlaceholder(graphics, x, y, width, height, "Missing image");
            log.warn("Sticker preview failed for {}", sticker.file().getAbsolutePath(), e);
        }
    }

    private void drawChecker(Graphics2D graphics, int x, int y, int width, int height) {
        int square = 18;
        Color dark = new Color(0x3A3D44);
        Color light = new Color(0x454851);
        for (int yy = y; yy < y + height; yy += square) {
            for (int xx = x; xx < x + width; xx += square) {
                boolean alternate = ((xx - x) / square + (yy - y) / square) % 2 == 0;
                graphics.setColor(alternate ? dark : light);
                graphics.fillRect(xx, yy, Math.min(square, x + width - xx), Math.min(square, y + height - yy));
            }
        }
    }

    private void drawPlaceholder(Graphics2D graphics, int x, int y, int width, int height, String message) {
        graphics.setColor(new Color(0x202225, true));
        graphics.fillRect(x, y, width, height);
        graphics.setColor(new Color(0xED4245));
        graphics.setStroke(new BasicStroke(3f));
        graphics.drawLine(x + 24, y + 24, x + width - 24, y + height - 24);
        graphics.drawLine(x + width - 24, y + 24, x + 24, y + height - 24);
        drawCenteredText(graphics, message, x, y + height / 2 + 30, width);
    }

    private void drawBadge(Graphics2D graphics, int x, int y, int number) {
        graphics.setColor(new Color(0x5865F2));
        graphics.fill(new RoundRectangle2D.Double(x, y, 46, 38, 14, 14));
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        FontMetrics metrics = graphics.getFontMetrics();
        String label = String.valueOf(number);
        graphics.drawString(label, x + (46 - metrics.stringWidth(label)) / 2, y + 27);
    }

    private void drawName(Graphics2D graphics, String name, int x, int baseline, int width) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        graphics.setColor(new Color(0xF2F3F5));
        FontMetrics metrics = graphics.getFontMetrics();
        String text = fitText(name, metrics, width);
        graphics.drawString(text, x + (width - metrics.stringWidth(text)) / 2, baseline);
    }

    private void drawCenteredText(Graphics2D graphics, String text, int x, int baseline, int width) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        graphics.setColor(new Color(0xF2F3F5));
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(text, x + (width - metrics.stringWidth(text)) / 2, baseline);
    }

    private String fitText(String text, FontMetrics metrics, int width) {
        if (metrics.stringWidth(text) <= width) {
            return text;
        }

        String suffix = "...";
        int maxWidth = width - metrics.stringWidth(suffix);
        StringBuilder fitted = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (metrics.stringWidth(fitted + text.substring(i, i + 1)) > maxWidth) {
                break;
            }
            fitted.append(text.charAt(i));
        }
        return fitted + suffix;
    }

    private void applyQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }
}
