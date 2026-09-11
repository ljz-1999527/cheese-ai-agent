package com.cheese.cheeseaiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.cheese.cheeseaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.util.List;

/**
 * PDF 生成工具（供 AI 智能体调用）
 * <p>
 * 使用 iText 库将文本内容生成为 PDF 文件，保存到项目运行目录下的 tmp/pdf 文件夹。
 * 内置了中文字体支持，可正常生成中文 PDF。
 */
public class PDFGenerationTool {

    /**
     * 根据内容生成 PDF 文件
     *
     * @param fileName  生成的 PDF 文件名
     * @param content   要写入 PDF 的内容
     * @param imageUrls 可选的网络图片 URL 列表（可为空）
     * @return 生成成功提示或错误信息
     */
    @Tool(description = "Generate a PDF file with given content and optional images. IMPORTANT: When the user requests PDF output (以 PDF 格式输出 / 生成 PDF / PDF 文件), you MUST use this tool, never writeFile. Pass network image URLs via imageUrls to render them into the PDF.", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content,
            @ToolParam(description = "Optional list of network image URLs to render into the PDF") List<String> imageUrls) {
        // 拼接 PDF 保存目录与完整路径
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        // 统计图片渲染成功/失败张数
        int successCount = 0;
        int failCount = 0;
        try {
            // 创建目录（目录不存在时自动创建）
            FileUtil.mkdir(fileDir);
            // 创建 PdfWriter 和 PdfDocument 对象（try-with-resources 自动关闭）
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                // 自定义字体（需要人工下载字体文件到特定目录）
//                String fontPath = Paths.get("src/main/resources/static/fonts/simsun.ttf")
//                        .toAbsolutePath().toString();
//                PdfFont font = PdfFontFactory.createFont(fontPath,
//                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                // 使用内置中文字体（iText 自带亚洲字体包，无需外部字体文件）
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);
                // 创建段落：将内容包装为 PDF 段落
                Paragraph paragraph = new Paragraph(content);
                // 添加段落
                document.add(paragraph);
                // 渲染网络图片：逐个下载并加入文档，单张失败跳过不中断整体生成
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    for (String url : imageUrls) {
                        if (url == null || url.trim().isEmpty()) {
                            continue;
                        }
                        try {
                            // 下载网络图片字节
                            byte[] imageBytes = HttpUtil.downloadBytes(url);
                            // 创建 ImageData 并包装为 Image 元素
                            Image image = new Image(ImageDataFactory.create(imageBytes));
                            // 自动缩放适配页面宽度
                            image.setAutoScale(true);
                            document.add(image);
                            successCount++;
                        } catch (Exception e) {
                            // 单张图片下载/解析失败：跳过该图，不中断整体生成
                            failCount++;
                        }
                    }
                }
            }
            String result = "PDF generated successfully to: " + filePath;
            // 附带图片渲染统计信息
            if (successCount > 0 || failCount > 0) {
                result += " (images rendered: " + successCount + " success, " + failCount + " failed)";
            }
            return result;
        } catch (IOException e) {
            // 生成失败时返回错误信息
            return "Error generating PDF: " + e.getMessage();
        }
    }
}
