package com.example.demo.presentation.exception;

import com.example.demo.presentation.resource.out.ErrorOccurredResource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import com.lowagie.text.exceptions.BadPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表現層全域例外處理器 (Presentation Global Exception Handler - Inbound Adapter)
 * <p>
 * 集中攔截並處理控制器層拋出的檢核例外、業務例外與安全性密碼錯誤例外，
 * 封裝為標準 {@link ErrorOccurredResource} 回應物件回傳給前端或呼叫端。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 攔截 Spring MVC @Valid 請求本體檢核失敗例外 (MethodArgumentNotValidException)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorOccurredResource> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpServletRequest request) {

		List<String> details = new ArrayList<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
		}

		log.warn("【檢核失敗】請求本體資料不合法: path={}, errors={}", request.getRequestURI(), details);

		ErrorOccurredResource errorResource = new ErrorOccurredResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"請求資料檢核失敗",
				request.getRequestURI(),
				Collections.unmodifiableList(details)
		);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResource);
	}

	/**
	 * 攔截 Spring 6 / Boot 4 方法參數檢核例外 (HandlerMethodValidationException)
	 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorOccurredResource> handleHandlerMethodValidation(
			HandlerMethodValidationException ex, HttpServletRequest request) {

		List<String> details = new ArrayList<>();
		ex.getParameterValidationResults().forEach(result ->
				result.getResolvableErrors().forEach(error -> {
					if (error.getDefaultMessage() != null) {
						details.add(error.getDefaultMessage());
					}
				})
		);

		log.warn("【檢核失敗】請求參數不合法 (HandlerMethodValidation): path={}, errors={}", request.getRequestURI(), details);

		ErrorOccurredResource errorResource = new ErrorOccurredResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"請求參數檢核失敗",
				request.getRequestURI(),
				Collections.unmodifiableList(details)
		);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResource);
	}

	/**
	 * 攔截 Jakarta Bean Validation 條件違規例外 (ConstraintViolationException)
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorOccurredResource> handleConstraintViolation(
			ConstraintViolationException ex, HttpServletRequest request) {

		List<String> details = new ArrayList<>();
		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			details.add(violation.getPropertyPath() + ": " + violation.getMessage());
		}

		log.warn("【檢核失敗】欄位條件違規: path={}, errors={}", request.getRequestURI(), details);

		ErrorOccurredResource errorResource = new ErrorOccurredResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"請求欄位條件約束違規",
				request.getRequestURI(),
				Collections.unmodifiableList(details)
		);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResource);
	}

	/**
	 * 攔截 PDFBox 密碼錯誤例外 (InvalidPasswordException)
	 */
	@ExceptionHandler(InvalidPasswordException.class)
	public ResponseEntity<ErrorOccurredResource> handleInvalidPassword(
			InvalidPasswordException ex, HttpServletRequest request) {

		log.warn("【PDF 解密失敗】PDFBox 密碼錯誤或未授權存取: path={}, message={}", request.getRequestURI(), ex.getMessage());

		ErrorOccurredResource errorResource = new ErrorOccurredResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"PDF 解密失敗：密碼錯誤或未授權存取",
				request.getRequestURI(),
				List.of("所提供之解密密碼不正確，或檔案為受密碼保護之 PDF")
		);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResource);
	}

	/**
	 * 攔截 OpenPDF 密碼錯誤例外 (BadPasswordException)
	 */
	@ExceptionHandler(BadPasswordException.class)
	public ResponseEntity<ErrorOccurredResource> handleBadPassword(
			BadPasswordException ex, HttpServletRequest request) {

		log.warn("【PDF 解密失敗】OpenPDF 密碼錯誤或未授權存取: path={}, message={}", request.getRequestURI(), ex.getMessage());

		ErrorOccurredResource errorResource = new ErrorOccurredResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"PDF 解密失敗：密碼錯誤或未授權存取",
				request.getRequestURI(),
				List.of("所提供之解密密碼不正確，或檔案為受密碼保護之 PDF")
		);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResource);
	}

	/**
	 * 攔截非法的業務參數例外 (IllegalArgumentException)
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorOccurredResource> handleIllegalArgument(
			IllegalArgumentException ex, HttpServletRequest request) {

		log.warn("【業務參數錯誤】path={}, message={}", request.getRequestURI(), ex.getMessage());

		ErrorOccurredResource errorResource = new ErrorOccurredResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI(),
				List.of(ex.getMessage())
		);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResource);
	}

	/**
	 * 攔截全域未處理之未預期例外 (Exception)
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorOccurredResource> handleGeneralException(
			Exception ex, HttpServletRequest request) {

		log.error("【系統非預期錯誤】path={}", request.getRequestURI(), ex);

		ErrorOccurredResource errorResource = new ErrorOccurredResource(
				LocalDateTime.now(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				"系統發生未預期錯誤，請聯絡系統管理員",
				request.getRequestURI(),
				List.of(ex.getClass().getSimpleName() + ": " + (ex.getMessage() != null ? ex.getMessage() : "無詳細訊息"))
		);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResource);
	}
}
