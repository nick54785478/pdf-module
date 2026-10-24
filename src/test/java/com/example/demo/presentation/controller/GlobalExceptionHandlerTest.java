package com.example.demo.presentation.controller;

import com.example.demo.presentation.exception.GlobalExceptionHandler;
import com.example.demo.presentation.resource.out.ErrorOccurredResource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 全域例外處理器單元測試 (GlobalExceptionHandler Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 測試各類檢核例外、業務例外與全域例外轉換為 ErrorOccurredResource 之邏輯。
 * 2. 測試方法皆以 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

	@Mock
	private HttpServletRequest request;

	/**
	 * Feature: 全域例外處理
	 * Scenario: 攔截 IllegalArgumentException，回傳 HTTP 400 與錯誤回應載體
	 *
	 * Given: 拋出包含業務錯誤訊息的 IllegalArgumentException
	 * When: 調用 handleIllegalArgument
	 * Then: 回傳狀態碼 400 Bad Request，且 ErrorOccurredResource 包含錯誤訊息與路徑
	 */
	@Test
	@DisplayName("攔截IllegalArgumentException-正常情境-回傳400與錯誤訊息")
	void handleIllegalArgument_success() {
		// Given
		when(request.getRequestURI()).thenReturn("/api/v1/quotations/openpdf");
		IllegalArgumentException ex = new IllegalArgumentException("報價單單號格式錯誤");

		// When
		ResponseEntity<ErrorOccurredResource> response = globalExceptionHandler.handleIllegalArgument(ex, request);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		ErrorOccurredResource body = response.getBody();
		assertNotNull(body);
		assertEquals(400, body.status());
		assertEquals("報價單單號格式錯誤", body.message());
		assertEquals("/api/v1/quotations/openpdf", body.path());
	}

	/**
	 * Feature: 全域例外處理
	 * Scenario: 攔截未預期 Exception，回傳 HTTP 500 與防洩漏安全訊息
	 *
	 * Given: 拋出未預期之 RuntimeException
	 * When: 調用 handleGeneralException
	 * Then: 回傳狀態碼 500 Internal Server Error，且 message 為系統安全提示文字
	 */
	@Test
	@DisplayName("攔截未預期Exception-正常情境-回傳500與安全提示")
	void handleGeneralException_success() {
		// Given
		when(request.getRequestURI()).thenReturn("/api/v1/quotations/openpdf");
		RuntimeException ex = new RuntimeException("資料庫連線超時");

		// When
		ResponseEntity<ErrorOccurredResource> response = globalExceptionHandler.handleGeneralException(ex, request);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		ErrorOccurredResource body = response.getBody();
		assertNotNull(body);
		assertEquals(500, body.status());
		assertTrue(body.message().contains("系統發生未預期錯誤"));
		assertFalse(body.details().isEmpty());
	}

	/**
	 * Feature: 全域例外處理
	 * Scenario: 攔截 ConstraintViolationException，回傳 HTTP 400 與詳細欄位錯誤
	 *
	 * Given: 包含單一約束違規的 ConstraintViolationException
	 * When: 調用 handleConstraintViolation
	 * Then: 回傳狀態碼 400 Bad Request，且 details 包含屬性與違規原因
	 */
	@Test
	@DisplayName("攔截ConstraintViolationException-正常情境-回傳400與約束細節")
	@SuppressWarnings("unchecked")
	void handleConstraintViolation_success() {
		// Given
		when(request.getRequestURI()).thenReturn("/api/v1/quotations/openpdf");
		ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
		Path path = mock(Path.class);
		when(path.toString()).thenReturn("id");
		when(violation.getPropertyPath()).thenReturn(path);
		when(violation.getMessage()).thenReturn("長度不可超過 50 個字元");

		ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

		// When
		ResponseEntity<ErrorOccurredResource> response = globalExceptionHandler.handleConstraintViolation(ex, request);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		ErrorOccurredResource body = response.getBody();
		assertNotNull(body);
		assertEquals(400, body.status());
		assertEquals("請求欄位條件約束違規", body.message());
		assertEquals(1, body.details().size());
		assertTrue(body.details().get(0).contains("長度不可超過 50 個字元"));
	}

	/**
	 * Feature: 全域例外處理
	 * Scenario: 攔截 MethodArgumentNotValidException，回傳 HTTP 400 與欄位檢核清單
	 *
	 * Given: 包含 FieldError 之 MethodArgumentNotValidException
	 * When: 調用 handleMethodArgumentNotValid
	 * Then: 回傳狀態碼 400 Bad Request，且 details 包含欄位名稱與驗證失敗原因
	 */
	@Test
	@DisplayName("攔截MethodArgumentNotValidException-正常情境-回傳400與欄位錯誤清單")
	void handleMethodArgumentNotValid_success() {
		// Given
		when(request.getRequestURI()).thenReturn("/api/v1/quotations/openpdf");
		BindingResult bindingResult = mock(BindingResult.class);
		FieldError fieldError = new FieldError("quotation", "quotationNo", "報價單號不可為空");
		when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

		MethodParameter parameter = mock(MethodParameter.class);
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

		// When
		ResponseEntity<ErrorOccurredResource> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, request);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		ErrorOccurredResource body = response.getBody();
		assertNotNull(body);
		assertEquals(400, body.status());
		assertEquals("請求資料檢核失敗", body.message());
		assertEquals(1, body.details().size());
		assertEquals("quotationNo: 報價單號不可為空", body.details().get(0));
	}
}
