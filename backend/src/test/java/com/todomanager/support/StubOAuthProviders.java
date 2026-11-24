package com.todomanager.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Stub Google (OIDC) and GitHub (OAuth 2.0) providers served by an embedded WireMock.
 * No real credentials or network access to providers are needed.
 */
public final class StubOAuthProviders {

	public static final String GOOGLE_CLIENT_ID = "test-google-client";
	public static final String GITHUB_CLIENT_ID = "test-github-client";

	private static final WireMockServer WM = new WireMockServer(WireMockConfiguration.options().dynamicPort());
	private static final RSAKey KEY;

	static {
		WM.start();
		try {
			KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
		}
		catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
		String base = base();
		WM.stubFor(get(urlPathEqualTo("/.well-known/openid-configuration")).willReturn(okJson("""
				{"issuer":"%1$s","authorization_endpoint":"%1$s/authorize","token_endpoint":"%1$s/token",
				 "userinfo_endpoint":"%1$s/userinfo","jwks_uri":"%1$s/jwks",
				 "response_types_supported":["code"],"subject_types_supported":["public"],
				 "id_token_signing_alg_values_supported":["RS256"]}""".formatted(base))));
		try {
			String jwks = new JWKSet(KEY.toPublicJWK()).toString();
			WM.stubFor(get(urlPathEqualTo("/jwks")).willReturn(okJson(jwks)));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		WM.stubFor(get(urlPathEqualTo("/github/user")).willReturn(okJson("""
				{"id":4242,"login":"octocat","name":null,"email":null,"avatar_url":"https://avatars/octo"}""")));
	}

	private StubOAuthProviders() {
	}

	public static String base() {
		return "http://localhost:" + WM.port();
	}

	public static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.security.oauth2.client.provider.google.issuer-uri", StubOAuthProviders::base);
		registry.add("spring.security.oauth2.client.provider.github.authorization-uri", () -> base() + "/github/authorize");
		registry.add("spring.security.oauth2.client.provider.github.token-uri", () -> base() + "/github/token");
		registry.add("spring.security.oauth2.client.provider.github.user-info-uri", () -> base() + "/github/user");
		registry.add("spring.security.oauth2.client.registration.google.client-id", () -> GOOGLE_CLIENT_ID);
		registry.add("spring.security.oauth2.client.registration.github.client-id", () -> GITHUB_CLIENT_ID);
	}

	/** Stubs the Google token and userinfo endpoints with an ID token bound to the given nonce. */
	public static void stubGoogleLogin(String sub, String email, Boolean emailVerified, String nonce) {
		try {
			JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
					.issuer(base())
					.subject(sub)
					.audience(GOOGLE_CLIENT_ID)
					.issueTime(new Date())
					.expirationTime(new Date(System.currentTimeMillis() + 300_000))
					.claim("nonce", nonce)
					.claim("name", "Google User")
					.claim("picture", "https://img/google.png");
			if (email != null) {
				claims.claim("email", email);
			}
			if (emailVerified != null) {
				claims.claim("email_verified", emailVerified);
			}
			JWTClaimsSet set = claims.build();
			SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), set);
			jwt.sign(new RSASSASigner(KEY));
			WM.stubFor(post(urlPathEqualTo("/token")).willReturn(okJson("""
					{"access_token":"g-access","token_type":"Bearer","expires_in":3600,"id_token":"%s"}"""
					.formatted(jwt.serialize()))));
		}
		catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
		String userinfoJson = """
				{"sub":"%s","name":"Google User","picture":"https://img/google.png"%s%s}""".formatted(sub,
				email != null ? ",\"email\":\"" + email + "\"" : "",
				emailVerified != null ? ",\"email_verified\":" + emailVerified : "");
		WM.stubFor(get(urlPathEqualTo("/userinfo")).willReturn(okJson(userinfoJson)));
	}

	/** Stubs the GitHub token endpoint; the profile is served by the static /github/user stub. */
	public static void stubGithubToken() {
		WM.stubFor(post(urlPathEqualTo("/github/token")).willReturn(aResponse()
				.withHeader("Content-Type", "application/json")
				.withBody("{\"access_token\":\"gh-access\",\"token_type\":\"bearer\",\"scope\":\"read:user\"}")));
	}

}
