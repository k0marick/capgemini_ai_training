package com.todomanager.auth.oauth;

import java.util.Map;

public interface OAuthUserProfileMapper {

	AuthProvider provider();

	OAuthUserProfile map(Map<String, Object> attributes);

}
