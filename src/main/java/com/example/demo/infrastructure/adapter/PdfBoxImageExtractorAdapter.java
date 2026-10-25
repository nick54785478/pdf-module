package com.example.demo.infrastructure.adapter;

import com.example.demo.application.port.out.PdfImageExtractorPort;
import com.example.demo.application.shared.dto.PdfImageGottenResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Apache PDFBox 圖片抽取適配器 (Outbound Adapter)
 * <p>
 * 遵循六角形架構規範：
 * 1. 位於基礎設施層 {@code infrastructure.adapter}。
 * 2. 命名規範結尾為 {@code Adapter}。
 * 3. 實作應用層 {@link PdfImageExtractorPort} 輸出端口，使用 Apache PDFBox 3.x 進行 PDF 資源遍歷與圖片抽取。
 * 4. 嚴格不對外暴露 Spring 或 Web 相依之技術類別 (如 {@code InputStreamResource})，以純 Java 原生型別傳遞。
 * </p>
 */
@Slf4j
@Component
public class PdfBoxImageExtractorAdapter implements PdfImageExtractorPort {

	@Override
	public Optional<PdfImageGottenResult> extractFirstImage(byte[] pdfBytes) {
		if (pdfBytes == null || pdfBytes.length == 0) {
			return Optional.empty();
		}
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			return extractFirstImageFromDocument(document);
		} catch (IOException e) {
			log.error("由 byte[] 讀取 PDF 抽取第一張圖片時發生異常", e);
			return Optional.empty();
		}
	}

	@Override
	public Optional<PdfImageGottenResult> extractFirstImage(InputStream inputStream) {
		byte[] pdfBytes = readAllBytesSafely(inputStream);
		return extractFirstImage(pdfBytes);
	}

	@Override
	public Optional<String> extractFirstImageAsBase64(byte[] pdfBytes) {
		return extractFirstImage(pdfBytes).map(PdfImageGottenResult::toBase64);
	}

	@Override
	public List<PdfImageGottenResult> extractAllImages(byte[] pdfBytes) {
		if (pdfBytes == null || pdfBytes.length == 0) {
			return Collections.emptyList();
		}
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			return extractAllImagesFromDocument(document);
		} catch (IOException e) {
			log.error("由 byte[] 讀取 PDF 抽取全部圖片時發生異常", e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<PdfImageGottenResult> extractAllImages(InputStream inputStream) {
		byte[] pdfBytes = readAllBytesSafely(inputStream);
		return extractAllImages(pdfBytes);
	}

	@Override
	public byte[] extractAllImagesAsZip(byte[] pdfBytes) {
		List<PdfImageGottenResult> images = extractAllImages(pdfBytes);
		return zipImages(images);
	}

	@Override
	public byte[] extractAllImagesAsZip(InputStream inputStream) {
		List<PdfImageGottenResult> images = extractAllImages(inputStream);
		return zipImages(images);
	}

	@Override
	public String extractAllImagesAsZipBase64(byte[] pdfBytes) {
		byte[] zipBytes = extractAllImagesAsZip(pdfBytes);
		return (zipBytes != null && zipBytes.length > 0) ? Base64.getEncoder().encodeToString(zipBytes) : "";
	}

	/**
	 * 從 PDDocument 中尋找並回傳第一張可抽取的圖片
	 */
	private Optional<PdfImageGottenResult> extractFirstImageFromDocument(PDDocument document) throws IOException {
		int pageIndex = 0;
		for (PDPage page : document.getPages()) {
			pageIndex++;
			PDResources resources = page.getResources();
			if (resources == null) {
				continue;
			}
			for (COSName name : resources.getXObjectNames()) {
				PDXObject xobject = resources.getXObject(name);
				Optional<PdfImageGottenResult> imageResult = extractImageFromXObject(xobject, name, pageIndex);
				if (imageResult.isPresent()) {
					return imageResult;
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * 從 PDDocument 中遍歷並抽取全部頁面的所有圖片
	 */
	private List<PdfImageGottenResult> extractAllImagesFromDocument(PDDocument document) throws IOException {
		List<PdfImageGottenResult> imageList = new ArrayList<>();
		int pageIndex = 0;
		for (PDPage page : document.getPages()) {
			pageIndex++;
			extractImagesFromPage(page, pageIndex, imageList);
		}
		return imageList;
	}

	/**
	 * 抽取特定頁面中的所有 XObject 圖片資源
	 */
	private void extractImagesFromPage(PDPage page, int pageIndex, List<PdfImageGottenResult> targetList) throws IOException {
		PDResources resources = page.getResources();
		if (resources == null) {
			return;
		}
		for (COSName name : resources.getXObjectNames()) {
			PDXObject xobject = resources.getXObject(name);
			extractImageFromXObject(xobject, name, pageIndex).ifPresent(targetList::add);
		}
	}

	/**
	 * 解析單一 PDXObject 是否為圖片並轉換為 DTO
	 */
	private Optional<PdfImageGottenResult> extractImageFromXObject(PDXObject xobject, COSName name, int pageIndex) {
		if (!(xobject instanceof PDImageXObject image)) {
			return Optional.empty();
		}
		try {
			String format = Optional.ofNullable(image.getSuffix())
					.filter(s -> !s.isBlank())
					.orElse("png");
			BufferedImage bufferedImage = image.getImage();
			if (bufferedImage == null) {
				return Optional.empty();
			}
			ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, format, imgBaos);
			byte[] imageBytes = imgBaos.toByteArray();
			String imageName = name != null ? name.getName() : "image";

			log.info("成功自第 {} 頁抽取圖片 [name={}, format={}, bytes={}]", pageIndex, imageName, format, imageBytes.length);
			return Optional.of(new PdfImageGottenResult(imageBytes, format, pageIndex, imageName));
		} catch (IOException e) {
			log.warn("自第 {} 頁轉換圖片 XObject 失敗", pageIndex, e);
			return Optional.empty();
		}
	}

	/**
	 * 將圖片清單封裝壓縮為 ZIP 壓縮檔位元組陣列
	 */
	private byte[] zipImages(List<PdfImageGottenResult> images) {
		if (images == null || images.isEmpty()) {
			return new byte[0];
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			int index = 1;
			for (PdfImageGottenResult img : images) {
				String entryName = String.format("image_%03d_p%d_%s", index++, img.pageNumber(), img.getFileName());
				zos.putNextEntry(new ZipEntry(entryName));
				zos.write(img.data());
				zos.closeEntry();
			}
		} catch (IOException e) {
			log.error("壓縮 PDF 抽取圖片為 ZIP 時發生錯誤", e);
			return new byte[0];
		}
		return baos.toByteArray();
	}

	/**
	 * 安全讀取 InputStream 二進位位元組陣列
	 */
	private byte[] readAllBytesSafely(InputStream inputStream) {
		if (inputStream == null) {
			return new byte[0];
		}
		try {
			return inputStream.readAllBytes();
		} catch (IOException e) {
			log.error("讀取 PDF InputStream 時發生異常", e);
			return new byte[0];
		}
	}
}
