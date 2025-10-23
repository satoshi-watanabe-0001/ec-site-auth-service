package com.ecsite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {

  private String status;
  private String message;
  private UserResponse data;
  private AuthTokenResponse tokens;
}
