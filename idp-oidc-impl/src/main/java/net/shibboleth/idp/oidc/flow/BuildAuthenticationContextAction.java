/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements. See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.shibboleth.idp.oidc.flow;

import com.google.common.base.Strings;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.oidc.config.OIDCConstants;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds an authentication context message from an incoming request.
 */
public class BuildAuthenticationContextAction extends AbstractProfileAction {
    /**
     * The Log.
     */
    private final Logger log = LoggerFactory.getLogger(BuildAuthenticationContextAction.class);

    /**
     * The Available authentication flows.
     */
    private List<AuthenticationFlowDescriptor> availableAuthenticationFlows;

    /**
     * The Authentication principal weight map.
     */
    private Map<AuthnContextClassRefPrincipal, Integer> authenticationPrincipalWeightMap;

    @Autowired
    private ClientDetailsEntityService clientService;

    /**
     * The Client service.
     */
    @Autowired
    private ClientDetailsEntityService clientService;

    /**
     * Instantiates a new authentication context action.
     */
    public BuildAuthenticationContextAction() {
    }

    /**
     * Sets available authentication flows.
     *
     * @param flows the flows
     */
    public void setAvailableAuthenticationFlows(final List<AuthenticationFlowDescriptor> flows) {
        this.availableAuthenticationFlows = flows;
    }

    /**
     * Sets authentication principal weight map.
     *
     * @param map the map
     */
    public void setAuthenticationPrincipalWeightMap(final Map<AuthnContextClassRefPrincipal, Integer> map) {
        this.authenticationPrincipalWeightMap = map;
    }


    @Nonnull
    @Override
    protected Event doExecute(@Nonnull final RequestContext springRequestContext,
                              @Nonnull final ProfileRequestContext profileRequestContext) {
        log.debug("{} Building authentication context", getLogPrefix());
        final AuthenticationContext ac = new AuthenticationContext();


        final OIDCAuthorizationRequestContext authZContext =
                profileRequestContext.getSubcontext(OIDCAuthorizationRequestContext.class);
        if (authZContext == null) {
            log.warn("No authorization request could be located in the profile request context");
            return Events.Failure.event(this);
        }

        final AuthorizationRequest authorizationRequest = authZContext.getAuthorizationRequest();
        if (authorizationRequest == null || Strings.isNullOrEmpty(authorizationRequest.getClientId())) {
            log.warn("Authorization request could not be loaded from session");
            return Events.Failure.event(this);
        }

        ac.setForceAuthn(authZContext.isForceAuthentication());

        final List<Principal> principals = new ArrayList<>();
        if (authorizationRequest.getExtensions().containsKey(OIDCConstants.ACR_VALUES)) {
            final String[] acrValues = authorizationRequest.getExtensions()
                    .get(OIDCConstants.ACR_VALUES).toString().split(" ");
            for (final String acrValue : acrValues) {
                final AuthnContextClassRefPrincipal requestedPrincipal =
                        new AuthnContextClassRefPrincipal(acrValue.trim());
                for (final AuthenticationFlowDescriptor flow : this.availableAuthenticationFlows) {
                    if (!principals.contains(requestedPrincipal) 
                            && flow.getSupportedPrincipals().contains(requestedPrincipal)) {
                        principals.add(requestedPrincipal);
                    }
                }
            }

        }

        if (principals.isEmpty()) {
            final AuthnContextClassRefPrincipal[] principalArray =
                    this.authenticationPrincipalWeightMap.keySet().toArray(new AuthnContextClassRefPrincipal[]{});
            Arrays.sort(principalArray, new WeightedComparator());
            principals.add(principalArray[principalArray.length - 1]);
        }

        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        ac.addSubcontext(rpc, true);


        profileRequestContext.addSubcontext(ac, true);
        profileRequestContext.setBrowserProfile(true);
        return Events.Success.event(this);
    }



    /**
     * A {@link Comparator} that compares the mapped weights of the two operands, using a weight of zero
     * for any unmapped values.
     */
    private class WeightedComparator implements Comparator {

        @Override
        public int compare(final Object o1, final Object o2) {
            final int weight1 =
                    authenticationPrincipalWeightMap.containsKey(o1) ? authenticationPrincipalWeightMap.get(o1) : 0;
            final int weight2 =
                    authenticationPrincipalWeightMap.containsKey(o2) ? authenticationPrincipalWeightMap.get(o2) : 0;
            if (weight1 < weight2) {
                return -1;
            } else if (weight1 > weight2) {
                return 1;
            }

            return 0;
        }

    }
}
