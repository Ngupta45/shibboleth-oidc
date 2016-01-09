package net.shibboleth.idp.oidc.filter;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import net.shibboleth.idp.oidc.util.OidcUtils;
import org.apache.http.client.utils.URIBuilder;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.openid.connect.request.ConnectRequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authorization initial filter that is executed by Spring Security.
 * Evaluates the OpenId Connect authorization request, builds it and passes
 * it down to underlying flows.
 */
@Component("authzRequestFilter")
public class AuthorizationRequestFilter extends GenericFilterBean {
    private static final String PROFILE_OIDC_AUTHORIZE = "/profile/oidc/authorize";

    private final Logger log = LoggerFactory.getLogger(AuthorizationRequestFilter.class);

    @Autowired
    private OAuth2RequestFactory authRequestFactory;

    @Autowired
    private ClientDetailsEntityService clientService;

    @Autowired
    private RedirectResolver redirectResolver;

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res,
                         final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        if (shouldIgnoreProfileRequestBasedOnPath(request)) {
            chain.doFilter(request, response);
            return;
        }

        if (isProcessingExistingInboundAuthorizationRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        log.debug("Evaluating authorization request");
        try {
            log.debug("Constructing authorization request");
            final Map<String, String> requestParameters = createRequestMap(request.getParameterMap());
            final AuthorizationRequest authRequest = authRequestFactory.createAuthorizationRequest(requestParameters);

            if (Strings.isNullOrEmpty(authRequest.getClientId())) {
                throw new InvalidClientException("No client id is specified in the authorization request");
            }


            final Object loginHint = authRequest.getExtensions().get(ConnectRequestParameters.LOGIN_HINT);
            if (loginHint != null) {
                OidcUtils.setRequestParameter(request, ConnectRequestParameters.LOGIN_HINT, loginHint);
                log.debug("Saved login hint {} into session", loginHint);
            } else {
                OidcUtils.removeSessionAttribute(request, ConnectRequestParameters.LOGIN_HINT);
                log.debug("Removed login hint attribute from session");
            }

            log.debug("Loading client by id {}", authRequest.getClientId());
            final ClientDetailsEntity client = clientService.loadClientByClientId(authRequest.getClientId());
            OidcUtils.setAuthorizationRequest(request, authRequest, requestParameters);
            log.debug("Saved authorization request");
            log.debug("Found client {}.", client.getClientId());
            OidcUtils.setClient(request, client);
            log.debug("Saved client request");

            boolean invokeFilterChain = false;
            final String prompt = (String) authRequest.getExtensions().get(ConnectRequestParameters.PROMPT);
            if (prompt != null) {
                log.debug("Authorization request contains prompt {}", prompt);
                invokeFilterChain = checkForPrompts(prompt, response, request);
            } else if (authRequest.getExtensions().get(ConnectRequestParameters.MAX_AGE) != null ||
                    client.getDefaultMaxAge() != null) {
                log.debug("Authorization request or client configuration contains max age");
                checkForMaxAge(request);
                invokeFilterChain = true;
            } else {
                log.debug("Evaluated authorization request. Invoking filter chain normally");
                invokeFilterChain = true;
            }


            if (invokeFilterChain) {

                chain.doFilter(req, res);
            }

        } catch (final InvalidClientException e) {
            log.debug("Invalid client specified in the request", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Determine profile path for filter execution.
     * Ignores all request patterns that do not start
     * with {@value #PROFILE_OIDC_AUTHORIZE}.
     *
     * @param request the request
     * @return true if this request should be ignored. Otherwise, true to process..
     * @throws IOException thrown if an error occurs when determining the profile path.
     */
    private boolean shouldIgnoreProfileRequestBasedOnPath(final HttpServletRequest request)
            throws IOException {
        final String servletPath = request.getServletPath();
        final String pathInfo = request.getPathInfo();
        if (Strings.isNullOrEmpty(servletPath) || Strings.isNullOrEmpty(pathInfo)) {
            log.debug("No servlet path available. Not an authorization request. Invoking filter chain");
            return true;
        }

        final String path = servletPath.concat(pathInfo);
        if (!path.startsWith(PROFILE_OIDC_AUTHORIZE)) {
            log.debug("{} not an authorization request. Invoking filter chain normally", path);
            return true;
        }
        return false;
    }

    /**
     * Determines if the identify provider is processing
     * and already-fetched authZ request that is in-flight.
     *
     * @param request the request
     * @return true if working on same authZ request, false otherwise.
     */
    private boolean isProcessingExistingInboundAuthorizationRequest(final HttpServletRequest request) {
        final AuthorizationRequest authorizationRequest = OidcUtils.getAuthorizationRequest(request);
        if (authorizationRequest != null) {
            log.debug("Found existing authorization request for client id {}", authorizationRequest.getClientId());
            return true;
        }
        return false;
    }

    /**
     * Check for max age. Determines max-age either from client configuration
     * or from the authorization request. Tries to figure out if an existing
     * authentication session bound to spring security is too old, and if so,
     * it will clear it out.
     *
     * @param request the request
     */
    private void checkForMaxAge(final HttpServletRequest request) {

        final ClientDetailsEntity client = OidcUtils.getClient(request);
        Integer max = client != null ? client.getDefaultMaxAge() : null;
        log.debug("Client configuration set to max age {}", max);

        final AuthorizationRequest authRequest = OidcUtils.getAuthorizationRequest(request);
        final String maxAge = (String) authRequest.getExtensions().get(ConnectRequestParameters.MAX_AGE);
        log.debug("Authorization request contains max age {}", maxAge);
        if (maxAge != null) {
            max = Integer.parseInt(maxAge);
        }

        if (max != null) {
            log.debug("Evaluated max age to use as {}", max);
            final Date authTime = OidcUtils.getAuthenticationTimestamp(request);
            log.debug("Authentication time set to {}", authTime);
            final Date now = new Date();
            if (authTime != null) {
                final long seconds = (now.getTime() - authTime.getTime()) / 1000;
                if (seconds > max) {
                    log.debug("Authentication is too old: {}. Clearing authentication context", authTime);
                    SecurityContextHolder.getContext().setAuthentication(null);
                }
            }
        }
        log.debug("Resuming with filter chain");
    }

    /**
     * Check for prompts in the authorization request. Evaluates
     * if redirects to the client should execute
     * or authentication cleared forcing the user
     * to authenticate again.
     *
     * @param prompt the prompt
     * @param response the response
     * @param request the request
     * @return the boolean
     * @throws IOException the IO exception
     */
    private boolean checkForPrompts(final String prompt,
                                    final HttpServletResponse response,
                                    final HttpServletRequest request)
            throws IOException {

        final ClientDetailsEntity client = OidcUtils.getClient(request);
        final AuthorizationRequest authRequest = OidcUtils.getAuthorizationRequest(request);

        final List<String> prompts = Splitter.on(ConnectRequestParameters.PROMPT_SEPARATOR)
                    .splitToList(Strings.nullToEmpty(prompt));
        if (prompts.contains(ConnectRequestParameters.PROMPT_NONE)) {
            log.debug("Prompt contains {}", ConnectRequestParameters.PROMPT_NONE);
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null) {
                log.debug("Authentication context is empty");
            } else {
                log.info("Client requested no prompt");
                if (client != null && authRequest.getRedirectUri() != null) {
                    final String url = redirectResolver.resolveRedirect(authRequest.getRedirectUri(), client);

                    try {
                        final URIBuilder uriBuilder = new URIBuilder(url);

                        uriBuilder.addParameter(ConnectRequestParameters.ERROR,
                                ConnectRequestParameters.LOGIN_REQUIRED);
                        if (!Strings.isNullOrEmpty(authRequest.getState())) {
                            uriBuilder.addParameter(ConnectRequestParameters.STATE, authRequest.getState());
                        }
                        log.debug("Resolved redirect url {}", uriBuilder.toString());
                        response.sendRedirect(uriBuilder.toString());
                        return true;

                    } catch (final URISyntaxException e) {
                        log.error("Can't build redirect URI for prompt=none, sending error instead", e);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                        return true;
                    }
                }

                log.warn("Access denied. Either client is not found or no redirect uri is specified");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                return true;
            }
        } else if (prompts.contains(ConnectRequestParameters.PROMPT_LOGIN)) {
            log.debug("Prompt contains {}", ConnectRequestParameters.PROMPT_LOGIN);

            if (OidcUtils.isRequestPrompted(request)) {
                OidcUtils.setPromptRequested(request);

                final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(null);
                    log.debug("Cleared authentication from context. Proceeding with filter chain");

                } else {
                    log.debug("Authentication is not found in the context. Proceeding with filter chain");

                }
            } else {
                OidcUtils.removeRequestPrompted(request);
            }
        } else {
            log.debug("Prompt is not supported. Proceeding with filter chain");

        }
        return false;
    }

    /**
     * Creates a map of request parameters. Uses the first parameter value
     * in case multi-valued parameters are found
     * @param parameterMap the original request parameters map
     * @return newly built parameters map
     */
    private Map<String, String> createRequestMap(final Map<String, String[]> parameterMap) {
        final Map<String, String> requestMap = new HashMap<>();
        for (final String key : parameterMap.keySet()) {
            final String[] val = parameterMap.get(key);
            if (val != null && val.length > 0) {
                log.debug("Added request parameter {} with value {}", key, val[0]);
                requestMap.put(key, val[0]);
            }
        }

        return requestMap;
    }

}
