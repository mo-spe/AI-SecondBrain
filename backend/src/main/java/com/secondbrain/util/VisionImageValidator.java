package com.secondbrain.util;

import com.secondbrain.exception.BusinessException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/** 读取图片头和尺寸，避免在模型调用前完整解码不可信的大图。 */
public final class VisionImageValidator {
    private static final long MAX_PIXELS = 24_000_000L;
    private static final int MAX_SIDE = 8_000;

    private VisionImageValidator() {
    }

    /**
     * 核对声明的 MIME、真实编码与尺寸，限制异常图片占用内存。
     *
     * @param bytes 图片字节
     * @param mimeType 请求声明的 MIME
     */
    public static void validate(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0 || !("image/jpeg".equals(mimeType)
                || "image/png".equals(mimeType))) {
            throw new BusinessException(400, "仅支持 JPEG 或 PNG 图片");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new BusinessException(400, "图片内容无法读取，请重新选择");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException(400, "图片内容无法读取，请重新选择");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                boolean matchesMime = "image/jpeg".equals(mimeType)
                        ? ("jpeg".equals(format) || "jpg".equals(format))
                        : "png".equals(format);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (!matchesMime || width <= 0 || height <= 0 || width > MAX_SIDE
                        || height > MAX_SIDE || (long) width * height > MAX_PIXELS) {
                    throw new BusinessException(400, "图片格式或尺寸不符合要求，请先裁剪后重试");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BusinessException(400, "图片内容无法读取，请重新选择");
        }
    }
}
