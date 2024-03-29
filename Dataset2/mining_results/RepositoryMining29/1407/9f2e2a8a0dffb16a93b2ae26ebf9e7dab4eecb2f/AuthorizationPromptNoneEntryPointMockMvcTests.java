package org.cloudfoundry.identity.uaa.mock.oauth;

import org.cloudfoundry.identity.uaa.mock.InjectedMockContextTest;
import org.cloudfoundry.identity.uaa.mock.util.MockMvcUtils;
import org.cloudfoundry.identity.uaa.oauth.OpenIdSessionStateCalculator;
import org.cloudfoundry.identity.uaa.oauth.UaaAuthorizationEndpoint;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.test.web.servlet.MvcResult;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.cloudfoundry.identity.uaa.mock.util.MockMvcUtils.CookieCsrfPostProcessor.cookieCsrf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthorizationPromptNoneEntryPointMockMvcTests extends InjectedMockContextTest {

    private String adminToken;

    @Before
    public void setup() throws Exception {
        BaseClientDetails client = new BaseClientDetails("ant", "", "openid", "implicit", "", "http://example.com/**");
        client.setAutoApproveScopes(Arrays.asList("openid"));
        adminToken = testClient.getClientCredentialsOAuthAccessToken("admin", "adminsecret", "clients.write uaa.admin");
        MockMvcUtils.createClient(getMockMvc(), adminToken, client);
    }

    @After
    public void cleanup() throws Exception {
        MockMvcUtils.deleteClient(getMockMvc(), adminToken, "ant", "");
    }

    @Test
    public void testSilentAuthHonorsAntRedirect_whenNotAuthenticated() throws Exception {
        getMockMvc().perform(
          get("/oauth/authorize?response_type=token&scope=openid&client_id=ant&prompt=none&redirect_uri=http://example.com/with/path.html")
        )
          .andExpect(redirectedUrl("http://example.com/with/path.html#error=login_required"));
    }

    @Test
    public void testSilentAuthHonorsAntRedirect_whenSessionHasBeenInvalidated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login(session);
        session.invalidate();

        getMockMvc().perform(
          get("/oauth/authorize?response_type=token&scope=openid&client_id=ant&prompt=none&redirect_uri=http://example.com/with/path.html")
            .session(session)
        )
          .andExpect(redirectedUrlPattern("http://example.com/**/*"));
    }

    @Test
    public void testSilentAuthentication_whenScopesNotAutoapproved() throws Exception {
        MockMvcUtils.deleteClient(getMockMvc(), adminToken, "ant", "");
        BaseClientDetails client = new BaseClientDetails("ant", "", "openid", "implicit", "", "http://example.com/**");
        MockMvcUtils.createClient(getMockMvc(), adminToken, client);

        MockHttpSession session = new MockHttpSession();
        login(session);

        getMockMvc().perform(
          get("/oauth/authorize?response_type=token&scope=openid&client_id=ant&prompt=none&redirect_uri=http://example.com/with/path.html")
            .session(session)
        )
          .andExpect(redirectedUrl("http://example.com/with/path.html#error=interaction_required"));
    }

    @Test
    public void testSilentAuthentication_testSessionStateIsCorrect() throws Exception {
        SecureRandom secureRandom = mock(SecureRandom.class);
        doNothing().when(secureRandom).nextBytes(any());

        OpenIdSessionStateCalculator sessionStateCalculator
          = (OpenIdSessionStateCalculator) getWebApplicationContext().getBean("openIdSessionStateCalculator");
        sessionStateCalculator.setSecureRandom(secureRandom);

        //we need to know session id when we are calculating session_state
        MockHttpSession session = new MockHttpSession(null, "12345") {
            public String changeSessionId() {
                return "12345";
            }
        };
        login(session);

        MvcResult result = getMockMvc().perform(
          get("/oauth/authorize?response_type=token&scope=openid&client_id=ant&prompt=none&redirect_uri=http://example.com/with/path.html")
            .session(session)
        )
          .andExpect(status().isFound())
          .andReturn();

        String redirectUrl = result.getResponse().getRedirectedUrl();
        Assert.assertThat(redirectUrl, containsString("session_state=707c310bc5aa38acc03d48a099fc999cd77f44df163178df1ca35863913f5711.0000000000000000000000000000000000000000000000000000000000000000"));
    }

    @Test
    public void testSilentAuthentication_RuntimeException_displaysErrorFragment() throws Exception {
        OpenIdSessionStateCalculator openIdSessionStateCalculator = mock(OpenIdSessionStateCalculator.class);
        UaaAuthorizationEndpoint uaaAuthorizationEndpoint = getWebApplicationContext().getBean(UaaAuthorizationEndpoint.class);
        uaaAuthorizationEndpoint.setOpenIdSessionStateCalculator(openIdSessionStateCalculator);

        when(openIdSessionStateCalculator.calculate(anyString(), anyString(), anyString())).thenThrow(NoSuchAlgorithmException.class);

        MockHttpSession session = new MockHttpSession();
        login(session);

        getMockMvc().perform(
          get("/oauth/authorize?response_type=token&scope=openid&client_id=ant&prompt=none&redirect_uri=http://example.com/with/path.html")
            .session(session)
        )
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("http://example.com/with/path.html#error=internal_server_error"));

    }

    @Test
    public void testSilentAuthentication_Returns400_whenInvalidRedirectUrlIsProvided() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login(session);

        getMockMvc().perform(
          get("/oauth/authorize?response_type=token&scope=openid&client_id=ant&prompt=none&redirect_uri=no good uri")
            .session(session)
        )
          .andExpect(status().is4xxClientError());
    }

    @Test
    public void nonSilentAuthentication_doesNotComputeSessionState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login(session);

        MvcResult result = getMockMvc().perform(
          get("/oauth/authorize?response_type=token&scope=openid&client_id=ant&redirect_uri=http://example.com/with/path.html")
            .session(session)
        )
          .andReturn();
        Assert.assertThat(result.getResponse().getRedirectedUrl(), not(containsString("session_state")));
    }

    private void login(MockHttpSession session) throws Exception {
        getMockMvc().perform(
          post("/login.do")
            .with(cookieCsrf())
            .param("username", "marissa")
            .param("password", "koala")
            .session(session)
        ).andExpect(redirectedUrl("/"));
    }
}