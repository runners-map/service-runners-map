package com.service.runnersmap.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.service.runnersmap.common.code.BaseCode;
import com.service.runnersmap.common.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {
  @JsonProperty("isSuccess")
  private final Boolean isSuccess;
  private final String code;
  private final String message;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final T payload;
  public static <T> ResponseEntity<ApiResponse<T>> onSuccess(BaseCode code, T payload) {
    ApiResponse<T> response = new ApiResponse<>(true, code.getReasonHttpStatus().getCode(), code.getReasonHttpStatus().getMessage(), payload);
    return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
  }

  public static <T> ResponseEntity<ApiResponse<T>> onFailure(BaseErrorCode code) {
    ApiResponse<T> response = new ApiResponse<>(false, code.getReasonHttpStatus().getCode(), code.getReasonHttpStatus().getMessage(), null);
    return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
  }
}