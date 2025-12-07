package com.template.app.auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppleTokenVerificationRequest {

    @NotBlank(message = "ID token is required")
    private String idToken;

    /**
     * User's name provided by Apple on first login (optional)
     * Apple only provides this on the first authentication
     */
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    /**
     * User's email provided by Apple separately (optional)
     * Apple may provide email outside of the ID token
     */
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;
}
