package com.kethy.utils;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

public class QRUtils {

    // 二维码背景色
    private static final int BLACK = 0xFF000000;
    // 二维码颜色
    private static final int WHITE = 0xFFFFFFFF;
    // 二维码尺寸大小
    private static final int QRCODE_SIZE = 300;
    // 二维码中间LOGO宽度
    private static final int LOGO_WIDTH = 60;
    // 二维码中间LOGO高度
    private static final int LOGO_HEIGHT = 60;
    // 二维码base64编辑集
    private static final String CHARSET = "UTF-8";
    // 二维码图片格式
    private static final String FORMAT_NAME = "JPG";

    /**
     * 强制去除二维码白边
     * 先根据内容生成二维码，再根据预设的大小进行缩放，预设的大小 - 缩放后的大小 = 白边大小
     * 白边的生成与二维码的内容多少(内容越多，生成的二维码越密集)以及设置的大小有关
     * 因为在生成二维码之后，才将白边裁掉，所以裁剪之后的二维码大小与预设的大小将不一致
     */
    private static BitMatrix deleteWhite(BitMatrix matrix) {
        int[] rec = matrix.getEnclosingRectangle();
        int resWidth = rec[2] + 1;
        int resHeight = rec[3] + 1;

        BitMatrix resMatrix = new BitMatrix(resWidth, resHeight);
        resMatrix.clear();
        for (int i = 0; i < resWidth; i++) {
            for (int j = 0; j < resHeight; j++) {
                if (matrix.get(i + rec[0], j + rec[1])) {
                    resMatrix.set(i, j);
                }
            }
        }
        return resMatrix;
    }

    /**
     * 设置QR二维码参数
     */
    private static Map<EncodeHintType, Object> getDecodeHintType() {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<>(3);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, CHARSET);
        hints.put(EncodeHintType.MARGIN, 1);
        return hints;
    }

    /**
     * 生成二维码
     *
     * @param content      扫描内容
     * @param logoImgPath  LOGO图片地址
     * @param needCompress 是否压缩
     * @return 二维码图片
     * @throws Exception 异常
     */
    private static BufferedImage createImage(String content,
                                             String logoImgPath,
                                             boolean needCompress) throws Exception {
        BitMatrix bitMatrix = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE,
                        getDecodeHintType());
        //强制去除白边
        //bitMatrix = deleteWhite(bitMatrix);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? BLACK : WHITE);
            }
        }
        if (null == logoImgPath || "".equals(logoImgPath)) {
            return image;
        }
        // 插入LOGO图片
        insertImage(image, logoImgPath, needCompress);
        return image;
    }

    /**
     * 插入LOGO
     *
     * @param source       二维码图片
     * @param logoImgPath  LOGO图片地址
     * @param needCompress 是否压缩
     * @throws Exception 异常
     */
    private static void insertImage(BufferedImage source,
                                    String logoImgPath,
                                    boolean needCompress) throws Exception {
        File file = new File(logoImgPath);
        if (!file.exists()) {
            System.err.println(logoImgPath + "该文件不存在!");
            return;
        }
        Image src = ImageIO.read(new File(logoImgPath));
        int width = src.getWidth(null);
        int height = src.getHeight(null);
        // 压缩LOGO
        if (needCompress) {
            if (width > LOGO_WIDTH) {
                width = LOGO_WIDTH;
            }
            if (height > LOGO_HEIGHT) {
                height = LOGO_HEIGHT;
            }
            Image image = src.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage tag = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = tag.getGraphics();
            // 绘制缩小后的LOGO图
            g.drawImage(image, 0, 0, null);
            g.dispose();
            src = image;
        }
        // 插入LOGO
        Graphics2D graph = source.createGraphics();
        int x = (QRCODE_SIZE - width) / 2;
        int y = (QRCODE_SIZE - height) / 2;
        graph.drawImage(src, x, y, width, height, null);
        Shape shape = new RoundRectangle2D.Float(x, y, width, width, 6, 6);
        graph.setStroke(new BasicStroke(3f));
        graph.draw(shape);
        graph.dispose();
    }

    /**
     * 生成二维码(内嵌LOGO)
     *
     * @param content      扫描内容
     * @param logoImgPath  LOGO图片地址
     * @param destPath     存放目录
     * @param needCompress 是否压缩LOGO
     * @throws Exception
     */
    public static String encode(String content,
                                String logoImgPath,
                                String destPath,
                                boolean needCompress) throws Exception {
        BufferedImage image = createImage(content, logoImgPath, needCompress);
        File file = new File(destPath);
        //当文件夹不存在时，自动创建文件夹
        if (!file.exists() && !file.isDirectory()) {
            file.mkdirs();
        }
        ImageIO.setCacheDirectory(file);
        String fileName = System.currentTimeMillis() + ".jpg";
        ImageIO.write(image, FORMAT_NAME,
                new File(destPath + File.separator + fileName));
        return fileName;
    }

    /**
     * 生成二维码
     *
     * @param content  扫描内容
     * @param destPath 存储地址
     * @throws Exception 异常
     */
    public static String encode(String content, String destPath) throws Exception {
        return encode(content, null, destPath, false);
    }

    /**
     * 生成二维码(内嵌LOGO,无需压缩logo图片)
     *
     * @param content     扫描内容
     * @param logoImgPath LOGO图片地址
     * @param destPath    存储地址
     * @throws Exception 异常
     */
    public static String encode(String content, String logoImgPath, String destPath) throws Exception {
        return encode(content, logoImgPath, destPath, false);
    }


    /**
     * 生成二维码(内嵌LOGO)
     *
     * @param content      扫描内容
     * @param logoImgPath  LOGO图片地址
     * @param output       输出流
     * @param needCompress 是否压缩LOGO
     * @throws Exception 异常
     */
    public static void encode(String content, String logoImgPath,
                              OutputStream output, boolean needCompress)
            throws Exception {
        BufferedImage image = createImage(content, logoImgPath, needCompress);
        ImageIO.write(image, FORMAT_NAME, output);
    }

    /**
     * 生成二维码
     *
     * @param content 扫描内容
     * @param output  输出流
     * @throws Exception 异常
     */
    public static void encode(String content, OutputStream output) throws Exception {
        encode(content, null, output, false);
    }

    /**
     * 解析二维码
     *
     * @param file 二维码图片
     * @throws Exception 异常
     */
    public static String decode(File file) throws Exception {
        BufferedImage image;
        image = ImageIO.read(file);
        if (Objects.isNull(image)) {
            return null;
        }
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result;
        Hashtable<DecodeHintType, Object> hints = new Hashtable<>(1);
        hints.put(DecodeHintType.CHARACTER_SET, CHARSET);
        result = new MultiFormatReader().decode(bitmap, hints);
        return result.getText();
    }

    /**
     * 解析二维码
     *
     * @param path 二维码图片地址
     * @throws Exception 异常
     */
    public static String decode(String path) throws Exception {
        return decode(new File(path));
    }

    // public static void main(String[] args) throws Exception {
    //     String text = "http://www.baidu.com";
    //     String destPath = "C:\\Users\\34639\\Desktop\\boxmoe";
    //     System.out.println(encode(text, null, destPath, true));
    // }
}