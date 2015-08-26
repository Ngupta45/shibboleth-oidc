package net.shibboleth.idp.oidc;

import net.shibboleth.idp.oidc.flow.OpenIdConnectResponse;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.openid.connect.web.AuthenticationTimeStamper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.webflow.execution.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.Map;

/**
 * OpenId Connect Utility methods that deal with setting and removing
 * session data.
 */
public class OpenIdConnectUtils {
    /** Flowscope attribute for the OIDC response. */
    private static final String FLOW_SCOPE_ATTRIBUTE_RESPONSE = "oidcResponse";

    /** Attribute name to store the authorization request. */
    private static final String ATTRIBUTE_OIDC_AUTHZ_REQUEST = "authorizationRequest";

    private static final String ATTRIBUTE_OIDC_AUTHZ_REQUEST_PARAMETERS = "OIDC_AUTHZ_REQUEST_PARAMS";

    /** Attribute name to store the openid connect client. */
    private static final String ATTRIBUTE_OIDC_CLIENT = "OIDC_CLIENT";

    private static final String PROMPTED = "PROMPT_FILTER_PROMPTED";

    private static final String PROMPT_REQUESTED = "PROMPT_FILTER_REQUESTED";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenIdConnectUtils.class);

    private OpenIdConnectUtils() {}


    public static AuthorizationRequest getAuthorizationRequest(final HttpServletRequest request) {
        HttpSession session = request.getSession();
        AuthorizationRequest authorizationRequest = (AuthorizationRequest)
                session.getAttribute(ATTRIBUTE_OIDC_AUTHZ_REQUEST);
        if (authorizationRequest != null) {
            LOGGER.debug("Authorization request found in session.");
        } else {
            LOGGER.debug("Authorization request not found in session.");
        }

        return authorizationRequest;
    }

    public static Map<String, String> getAuthorizationRequestParameters(final HttpServletRequest request) {
        HttpSession session = request.getSession();
        return (Map<String, String>) session.getAttribute(ATTRIBUTE_OIDC_AUTHZ_REQUEST_PARAMETERS);
    }

    public static void setAuthorizationRequest(final HttpServletRequest request,
                                               final AuthorizationRequest authorizationRequest,
                                               final Map<String, String> parameterMap) {
        HttpSession session = request.getSession();
        session.setAttribute(ATTRIBUTE_OIDC_AUTHZ_REQUEST, authorizationRequest);
        session.setAttribute(ATTRIBUTE_OIDC_AUTHZ_REQUEST_PARAMETERS, parameterMap);
    }

    public static void setClient(final HttpServletRequest request,
                               final ClientDetailsEntity client) {
        HttpSession session = request.getSession();
        session.setAttribute(ATTRIBUTE_OIDC_CLIENT, client);
    }

    public static ClientDetailsEntity getClient(final HttpServletRequest request) {
        HttpSession session = request.getSession();
        return (ClientDetailsEntity) session.getAttribute(ATTRIBUTE_OIDC_CLIENT);
    }

    public static void setRequestParameter(final HttpServletRequest request,
                                 final String parameter,
                                 final Object value) {
        HttpSession session = request.getSession();
        session.setAttribute(parameter, value);
    }

    public static void removeRequestParameter(final HttpServletRequest request,
                                           final String parameter) {
        HttpSession session = request.getSession();
        session.removeAttribute(parameter);
    }

    public static void setRequestPrompted(final HttpServletRequest request,
                                              final String parameter) {
        HttpSession session = request.getSession();
        session.removeAttribute(parameter);
    }

    public static void removeRequestPrompted(final HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.removeAttribute(PROMPTED);
    }

    public static void setPromptRequested(final HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute(PROMPT_REQUESTED, Boolean.TRUE);
    }

    public static Boolean isRequestPrompted(final HttpServletRequest request) {
        HttpSession session = request.getSession();
        return session.getAttribute(PROMPTED) == null;
    }

    public static Date getAuthenticationTimestamp(final HttpServletRequest request) {
        HttpSession session = request.getSession();
        return (Date) session.getAttribute(AuthenticationTimeStamper.AUTH_TIMESTAMP);
    }

    public static OpenIdConnectResponse getResponse(RequestContext context) {
        OpenIdConnectResponse response =
                context.getFlowScope().get(FLOW_SCOPE_ATTRIBUTE_RESPONSE, OpenIdConnectResponse.class);
        return response;
    }

    public static void setResponse(RequestContext context, OpenIdConnectResponse response) {
        context.getFlowScope().put(FLOW_SCOPE_ATTRIBUTE_RESPONSE, response);
    }
}