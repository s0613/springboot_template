package com.template.app.auth.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2UserInfo {
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_PICTURE_LENGTH = 500;
    private static final int MAX_PROVIDER_ID_LENGTH = 255;

    private String providerId;
    private String provider;
    private String email;
    private String name;
    private String picture;

    public static OAuth2UserInfoBuilder builder() {
        return new OAuth2UserInfoBuilder();
    }

    public static class OAuth2UserInfoBuilder {
        private String providerId;
        private String provider;
        private String email;
        private String name;
        private String picture;

        public OAuth2UserInfoBuilder providerId(String providerId) {
            this.providerId = truncate(providerId, MAX_PROVIDER_ID_LENGTH);
            return this;
        }

        public OAuth2UserInfoBuilder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public OAuth2UserInfoBuilder email(String email) {
            this.email = truncate(email, MAX_EMAIL_LENGTH);
            return this;
        }

        public OAuth2UserInfoBuilder name(String name) {
            this.name = truncate(name, MAX_NAME_LENGTH);
            return this;
        }

        public OAuth2UserInfoBuilder picture(String picture) {
            this.picture = truncate(picture, MAX_PICTURE_LENGTH);
            return this;
        }

        public OAuth2UserInfo build() {
            return new OAuth2UserInfo(providerId, provider, email, name, picture);
        }

        private String truncate(String value, int maxLength) {
            if (value == null) {
                return null;
            }
            return value.length() > maxLength ? value.substring(0, maxLength) : value;
        }
    }
}
