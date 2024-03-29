/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.oauth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthentication;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthenticationDetails;
import org.cloudfoundry.identity.uaa.error.UaaException;
import org.cloudfoundry.identity.uaa.message.SimpleMessage;
import org.cloudfoundry.identity.uaa.oauth.approval.ApprovalStore;
import org.cloudfoundry.identity.uaa.oauth.client.ClientDetailsModification;
import org.cloudfoundry.identity.uaa.rest.AttributeNameMapper;
import org.cloudfoundry.identity.uaa.rest.QueryableResourceManager;
import org.cloudfoundry.identity.uaa.rest.SearchResults;
import org.cloudfoundry.identity.uaa.rest.SearchResultsFactory;
import org.cloudfoundry.identity.uaa.rest.SimpleAttributeNameMapper;
import org.cloudfoundry.identity.uaa.security.DefaultSecurityContextAccessor;
import org.cloudfoundry.identity.uaa.security.SecurityContextAccessor;
import org.cloudfoundry.identity.uaa.util.UaaPagingUtils;
import org.cloudfoundry.identity.uaa.util.UaaStringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.common.exceptions.BadClientCredentialsException;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.BaseClientDetails;
import org.springframework.security.oauth2.provider.ClientAlreadyExistsException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationService;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for listing and manipulating OAuth2 clients.
 * 
 * @author Dave Syer
 */
@Controller
@ManagedResource
public class ClientAdminEndpoints implements InitializingBean {

    private static final String SCIM_CLIENTS_SCHEMA_URI = "http://cloudfoundry.org/schema/scim/oauth-clients-1.0";

    private final Log logger = LogFactory.getLog(getClass());

    private static final Set<String> VALID_GRANTS = new HashSet<String>(Arrays.asList("implicit", "password",
                    "client_credentials", "authorization_code", "refresh_token"));

    private static final Collection<String> NON_ADMIN_INVALID_GRANTS = new HashSet<String>(Arrays.asList("password"));

    private static final Collection<String> NON_ADMIN_VALID_AUTHORITIES = new HashSet<String>(Arrays.asList("uaa.none"));

    private ClientRegistrationService clientRegistrationService;

    private QueryableResourceManager<ClientDetails> clientDetailsService;

    private AttributeNameMapper attributeNameMapper = new SimpleAttributeNameMapper(
                    Collections.<String, String> emptyMap());

    private SecurityContextAccessor securityContextAccessor = new DefaultSecurityContextAccessor();

    private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<String, AtomicInteger>();

    private AtomicInteger clientUpdates = new AtomicInteger();

    private AtomicInteger clientDeletes = new AtomicInteger();

    private AtomicInteger clientSecretChanges = new AtomicInteger();

    private Set<String> reservedClientIds = StringUtils.commaDelimitedListToSet("uaa");

    private ApprovalStore approvalStore;

    private AuthenticationManager authenticationManager;

    public ApprovalStore getApprovalStore() {
        return approvalStore;
    }

    public void setApprovalStore(ApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public void setAttributeNameMapper(AttributeNameMapper attributeNameMapper) {
        this.attributeNameMapper = attributeNameMapper;
    }

    /**
     * @param clientRegistrationService the clientRegistrationService to set
     */
    public void setClientRegistrationService(ClientRegistrationService clientRegistrationService) {
        this.clientRegistrationService = clientRegistrationService;
    }

    /**
     * @param clientDetailsService the clientDetailsService to set
     */
    public void setClientDetailsService(QueryableResourceManager<ClientDetails> clientDetailsService) {
        this.clientDetailsService = clientDetailsService;
    }

    public void setSecurityContextAccessor(SecurityContextAccessor securityContextAccessor) {
        this.securityContextAccessor = securityContextAccessor;
    }

    @ManagedMetric(metricType = MetricType.COUNTER, displayName = "Client Registration Count")
    public int getTotalClients() {
        return clientRegistrationService.listClientDetails().size();
    }

    @ManagedMetric(metricType = MetricType.COUNTER, displayName = "Client Update Count (Since Startup)")
    public int getClientUpdates() {
        return clientUpdates.get();
    }

    @ManagedMetric(metricType = MetricType.COUNTER, displayName = "Client Delete Count (Since Startup)")
    public int getClientDeletes() {
        return clientDeletes.get();
    }

    @ManagedMetric(metricType = MetricType.COUNTER, displayName = "Client Secret Change Count (Since Startup)")
    public int getClientSecretChanges() {
        return clientSecretChanges.get();
    }

    @ManagedMetric(displayName = "Errors Since Startup")
    public Map<String, AtomicInteger> getErrorCounts() {
        return errorCounts;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.state(clientRegistrationService != null, "A ClientRegistrationService must be provided");
        Assert.state(clientDetailsService != null, "A ClientDetailsService must be provided");
    }

    @RequestMapping(value = "/oauth/clients/{client}", method = RequestMethod.GET)
    @ResponseBody
    public ClientDetails getClientDetails(@PathVariable String client) throws Exception {
        try {
            return removeSecret(clientDetailsService.retrieve(client));
        } catch (InvalidClientException e) {
            throw new NoSuchClientException("No such client: " + client);
        } catch (BadClientCredentialsException e) {
            // Defensive check, in case the clientDetailsService starts throwing
            // these instead
            throw new NoSuchClientException("No such client: " + client);
        }
    }

    @RequestMapping(value = "/oauth/clients", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ClientDetails createClientDetails(@RequestBody BaseClientDetails client) throws Exception {
        ClientDetails details = validateClient(client, true);
        clientRegistrationService.addClientDetails(details);
        return removeSecret(client);
    }

    @RequestMapping(value = "/oauth/clients/tx", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @Transactional
    public ClientDetails[] createClientDetailsTx(@RequestBody BaseClientDetails[] clients) throws Exception {
        if (clients==null || clients.length==0) {
            throw new NoSuchClientException("Message body does not contain any clients.");
        }
        ClientDetails[] results = new ClientDetails[clients.length];
        for (int i=0; i<clients.length; i++) {
            results[i] = validateClient(clients[i], true);
        }
        return doInsertClientDetails(results);
    }

    protected ClientDetails[] doInsertClientDetails(ClientDetails[] details) {
        for (int i=0; i<details.length; i++) {
            clientRegistrationService.addClientDetails(details[i]);
            details[i] = removeSecret(details[i]);
        }
        return details;
    }
    
    @RequestMapping(value = "/oauth/clients/tx", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    @ResponseBody
    public ClientDetails[] updateClientDetailsTx(@RequestBody BaseClientDetails[] clients) throws Exception {
        if (clients==null || clients.length==0) {
            throw new InvalidClientDetailsException("No clients specified for update.");
        }
        ClientDetails[] details = new ClientDetails[clients.length];
        for (int i=0; i<clients.length; i++) {
            ClientDetails client = clients[i];;
            ClientDetails existing = getClientDetails(client.getClientId());
            if (existing==null) {
                throw new NoSuchClientException("Client "+client.getClientId()+" does not exist");
            } else {
                details[i] = syncWithExisting(existing, client);
            }
            details[i] = validateClient(details[i], false);
        }        
        return doProcessUpdates(details);
    }

    protected ClientDetails[] doProcessUpdates(ClientDetails[] details) {
        ClientDetails[] result = new ClientDetails[details.length];
        for (int i=0; i<result.length; i++) {
            clientRegistrationService.updateClientDetails(details[i]);
            clientUpdates.incrementAndGet();
            result[i] = removeSecret(details[i]);
        }
        return result;

    }

    
    
    @RequestMapping(value = "/oauth/clients/{client}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ClientDetails updateClientDetails(@RequestBody BaseClientDetails client,
                    @PathVariable("client") String clientId) throws Exception {
        Assert.state(clientId.equals(client.getClientId()),
                        String.format("The client id (%s) does not match the URL (%s)", client.getClientId(), clientId));
        ClientDetails details = client;
        try {
            ClientDetails existing = getClientDetails(clientId);
            if (existing==null) {
                //TODO - should we proceed? Previous code did by throwing a NPE and logging a warning
                logger.warn("Couldn't fetch client config, null, for client_id: " + clientId);
            } else {
                details = syncWithExisting(existing, client);
            }
        } catch (Exception e) {
            logger.warn("Couldn't fetch client config for client_id: " + clientId, e);
        }
        details = validateClient(details, false);
        clientRegistrationService.updateClientDetails(details);
        clientUpdates.incrementAndGet();
        return removeSecret(client);
    }

    @RequestMapping(value = "/oauth/clients/{client}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ClientDetails removeClientDetails(@PathVariable String client) throws Exception {
        ClientDetails details = clientDetailsService.retrieve(client);
        doProcessDeletes(new ClientDetails[] {details});
        return removeSecret(details);
    }

    @RequestMapping(value = "/oauth/clients/tx/delete", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    @ResponseBody
    public ClientDetails[] removeClientDetailsTx(@RequestBody BaseClientDetails[] details) throws Exception {
        ClientDetails[] result = new ClientDetails[details.length];
        for (int i=0; i<result.length; i++) {
            result[i] = clientDetailsService.retrieve(details[i].getClientId());
        }
        return doProcessDeletes(result);
    }

    @RequestMapping(value = "/oauth/clients/tx/modify", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    @ResponseBody
    public ClientDetailsModification[] modifyClientDetailsTx(@RequestBody ClientDetailsModification[] details) throws Exception {
        ClientDetailsModification[] result = new ClientDetailsModification[details.length];
        for (int i=0; i<result.length; i++) {
            if (ClientDetailsModification.ADD.equals(details[i].getAction())) {
                ClientDetails client = validateClient(details[i], true);
                clientRegistrationService.addClientDetails(client);
                clientUpdates.incrementAndGet();
                result[i] = new ClientDetailsModification(clientDetailsService.retrieve(details[i].getClientId()));
            } else if (ClientDetailsModification.DELETE.equals(details[i].getAction())) {
                result[i] = new ClientDetailsModification(clientDetailsService.retrieve(details[i].getClientId()));
                doProcessDeletes(new ClientDetails[]{result[i]});
                result[i].setApprovalsDeleted(true);
            } else if (ClientDetailsModification.UPDATE.equals(details[i].getAction())) {
                result[i] = updateClientNotSecret(details[i]);
            } else if (ClientDetailsModification.UPDATE_SECRET.equals(details[i].getAction())) {
                boolean approvalsDeleted = updateClientSecret(details[i]);
                result[i] = updateClientNotSecret(details[i]);
                result[i].setApprovalsDeleted(approvalsDeleted);
            } else if (ClientDetailsModification.SECRET.equals(details[i].getAction())) {
                boolean approvalsDeleted = updateClientSecret(details[i]);
                result[i] = details[i];
                result[i].setApprovalsDeleted(approvalsDeleted);
            } else {
                throw new InvalidClientDetailsException("Invalid action.");
            }
            result[i].setAction(details[i].getAction());
            result[i].setClientSecret(null);
        }
        return result;
    }

    private ClientDetailsModification updateClientNotSecret(ClientDetailsModification c) {
        ClientDetailsModification result = new ClientDetailsModification(clientDetailsService.retrieve(c.getClientId()));
        ClientDetails client = validateClient(c, false);
        clientRegistrationService.updateClientDetails(client);
        clientUpdates.incrementAndGet();
        return result;
    }

    private boolean updateClientSecret(ClientDetailsModification detail) {
        boolean deleteApprovals = !(authenticateClient(detail.getClientId(), detail.getClientSecret()));
        if (deleteApprovals) {
            clientRegistrationService.updateClientSecret(detail.getClientId(), detail.getClientSecret());
            deleteApprovals(detail.getClientId());
            detail.setApprovalsDeleted(true);
        }
        return deleteApprovals;
    }


    @RequestMapping(value = "/oauth/clients/tx/secret", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    @ResponseBody
    public ClientDetailsModification[] changeSecretTx(@RequestBody SecretChangeRequest[] change) {

        ClientDetailsModification[] clientDetails = new ClientDetailsModification[change.length];
        String clientId=null;
        try {
            for (int i=0; i<change.length; i++) {
                clientId = change[i].getClientId();
                clientDetails[i] = new ClientDetailsModification(clientDetailsService.retrieve(clientId));
                boolean oldPasswordOk = authenticateClient(clientId, change[i].getOldSecret());
                clientRegistrationService.updateClientSecret(clientId, change[i].getSecret());
                if (!oldPasswordOk) {
                    deleteApprovals(clientId);
                    clientDetails[i].setApprovalsDeleted(true);
                }
                clientDetails[i] = removeSecret(clientDetails[i]);
            }
        } catch (InvalidClientException e) {
            throw new NoSuchClientException("No such client: " + clientId);
        }
        clientSecretChanges.getAndAdd(change.length);
        return clientDetails;
    }


    protected ClientDetails[] doProcessDeletes(ClientDetails[] details) {
        ClientDetailsModification[] result = new ClientDetailsModification[details.length];
        for (int i=0; i<details.length; i++) {
            String clientId = details[i].getClientId();
            clientRegistrationService.removeClientDetails(clientId);
            deleteApprovals(clientId);
            clientDeletes.incrementAndGet();
            result[i] = removeSecret(details[i]);
            result[i].setApprovalsDeleted(true);
        }
        return result;
    }

    private void deleteApprovals(String clientId) {
        if (approvalStore!=null) {
            approvalStore.revokeApprovals(String.format("client_id eq '%s'", clientId));
        } else {
            throw new UnsupportedOperationException("No approval store configured on "+getClass().getName());
        }
    }


    @RequestMapping(value = "/oauth/clients", method = RequestMethod.GET)
    @ResponseBody
    public SearchResults<?> listClientDetails(
                    @RequestParam(value = "attributes", required = false) String attributesCommaSeparated,
                    @RequestParam(required = false, defaultValue = "client_id pr") String filter,
                    @RequestParam(required = false, defaultValue = "client_id") String sortBy,
                    @RequestParam(required = false, defaultValue = "ascending") String sortOrder,
                    @RequestParam(required = false, defaultValue = "1") int startIndex,
                    @RequestParam(required = false, defaultValue = "100") int count) throws Exception {
        List<ClientDetails> result = new ArrayList<ClientDetails>();
        List<ClientDetails> clients;
        try {
            clients = clientDetailsService.query(filter, sortBy, "ascending".equalsIgnoreCase(sortOrder));
            if (count > clients.size()) {
                count = clients.size();
            }
        } catch (IllegalArgumentException e) {
            throw new UaaException("Invalid filter expression: [" + filter + "]", HttpStatus.BAD_REQUEST.value());
        }
        for (ClientDetails client : UaaPagingUtils.subList(clients, startIndex, count)) {
            result.add(removeSecret(client));
        }

        if (!StringUtils.hasLength(attributesCommaSeparated)) {
            return new SearchResults<ClientDetails>(Arrays.asList(SCIM_CLIENTS_SCHEMA_URI), result, startIndex, count,
                            clients.size());
        }

        String[] attributes = attributesCommaSeparated.split(",");
        try {
            return SearchResultsFactory.buildSearchResultFrom(result, startIndex, count, clients.size(), attributes,
                            attributeNameMapper, Arrays.asList(SCIM_CLIENTS_SCHEMA_URI));
        } catch (SpelParseException e) {
            throw new UaaException("Invalid attributes: [" + attributesCommaSeparated + "]",
                            HttpStatus.BAD_REQUEST.value());
        } catch (SpelEvaluationException e) {
            throw new UaaException("Invalid attributes: [" + attributesCommaSeparated + "]",
                            HttpStatus.BAD_REQUEST.value());
        }
    }

    @RequestMapping(value = "/oauth/clients/{client}/secret", method = RequestMethod.PUT)
    public SimpleMessage changeSecret(@PathVariable String client, @RequestBody SecretChangeRequest change) {

        ClientDetails clientDetails;
        try {
            clientDetails = clientDetailsService.retrieve(client);
        } catch (InvalidClientException e) {
            throw new NoSuchClientException("No such client: " + client);
        }

        try {
            checkPasswordChangeIsAllowed(clientDetails, change.getOldSecret());
        } catch (IllegalStateException e) {
            throw new InvalidClientDetailsException(e.getMessage());
        }

        clientRegistrationService.updateClientSecret(client, change.getSecret());

        clientSecretChanges.incrementAndGet();

        return new SimpleMessage("ok", "secret updated");
    }

    @ExceptionHandler(InvalidClientDetailsException.class)
    public ResponseEntity<InvalidClientDetailsException> handleInvalidClientDetails(InvalidClientDetailsException e) {
        incrementErrorCounts(e);
        return new ResponseEntity<InvalidClientDetailsException>(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSuchClientException.class)
    public ResponseEntity<Void> handleNoSuchClient(NoSuchClientException e) {
        incrementErrorCounts(e);
        return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<InvalidClientDetailsException> handleClientAlreadyExists(ClientAlreadyExistsException e) {
        incrementErrorCounts(e);
        return new ResponseEntity<InvalidClientDetailsException>(new InvalidClientDetailsException(e.getMessage()),
                        HttpStatus.CONFLICT);
    }

    private void incrementErrorCounts(Exception e) {
        String series = UaaStringUtils.getErrorName(e);
        AtomicInteger value = errorCounts.get(series);
        if (value == null) {
            synchronized (errorCounts) {
                value = errorCounts.get(series);
                if (value == null) {
                    value = new AtomicInteger();
                    errorCounts.put(series, value);
                }
            }
        }
        value.incrementAndGet();
    }

    private ClientDetails validateClient(ClientDetails prototype, boolean create) {

        BaseClientDetails client = new BaseClientDetails(prototype);

        client.setAdditionalInformation(prototype.getAdditionalInformation());

        String clientId = client.getClientId();
        if (create && reservedClientIds.contains(clientId)) {
            throw new InvalidClientDetailsException("Not allowed: " + clientId + " is a reserved client_id");
        }

        Set<String> requestedGrantTypes = client.getAuthorizedGrantTypes();

        if (requestedGrantTypes.isEmpty()) {
            throw new InvalidClientDetailsException("An authorized grant type must be provided. Must be one of: "
                            + VALID_GRANTS.toString());
        }
        for (String grant : requestedGrantTypes) {
            if (!VALID_GRANTS.contains(grant)) {
                throw new InvalidClientDetailsException(grant + " is not an allowed grant type. Must be one of: "
                                + VALID_GRANTS.toString());
            }
        }

        if ((requestedGrantTypes.contains("authorization_code") || requestedGrantTypes.contains("password"))
                        && !requestedGrantTypes.contains("refresh_token")) {
            logger.debug("requested grant type missing refresh_token: " + clientId);

            requestedGrantTypes.add("refresh_token");
        }

        if (!securityContextAccessor.isAdmin()) {

            // Not admin, so be strict with grant types and scopes
            for (String grant : requestedGrantTypes) {
                if (NON_ADMIN_INVALID_GRANTS.contains(grant)) {
                    throw new InvalidClientDetailsException(grant
                                    + " is not an allowed grant type for non-admin caller.");
                }
            }

            if (requestedGrantTypes.contains("implicit") && requestedGrantTypes.contains("authorization_code")) {
                throw new InvalidClientDetailsException(
                                "Not allowed: implicit grant type is not allowed together with authorization_code");
            }

            String callerId = securityContextAccessor.getClientId();
            if (callerId != null) {

                // New scopes are allowed if they are for the caller or the new
                // client.
                String callerPrefix = callerId + ".";
                String clientPrefix = clientId + ".";

                ClientDetails caller = clientDetailsService.retrieve(callerId);
                Set<String> validScope = caller.getScope();
                for (String scope : client.getScope()) {
                    if (scope.startsWith(callerPrefix) || scope.startsWith(clientPrefix)) {
                        // Allowed
                        continue;
                    }
                    if (!validScope.contains(scope)) {
                        throw new InvalidClientDetailsException(scope + " is not an allowed scope for caller="
                                        + callerId + ". Must have prefix in [" + callerPrefix + "," + clientPrefix
                                        + "] or be one of: " + validScope.toString());
                    }
                }

            }
            else { // No client caller. Shouldn't happen in practice, but let's
                   // be defensive

                // New scopes are allowed if they are for the caller or the new
                // client.
                String clientPrefix = clientId + ".";

                for (String scope : client.getScope()) {
                    if (!scope.startsWith(clientPrefix)) {
                        throw new InvalidClientDetailsException(scope
                                        + " is not an allowed scope for null caller and client_id=" + clientId
                                        + ". Must start with '" + clientPrefix + "'");
                    }
                }
            }

            Set<String> validAuthorities = new HashSet<String>(NON_ADMIN_VALID_AUTHORITIES);
            if (requestedGrantTypes.contains("client_credentials")) {
                // If client_credentials is used then the client might be a
                // resource server
                validAuthorities.add("uaa.resource");
            }

            for (String authority : AuthorityUtils.authorityListToSet(client.getAuthorities())) {
                if (!validAuthorities.contains(authority)) {
                    throw new InvalidClientDetailsException(authority + " is not an allowed authority for caller="
                                    + callerId + ". Must be one of: " + validAuthorities.toString());
                }
            }

        }

        if (client.getAuthorities().isEmpty()) {
            client.setAuthorities(AuthorityUtils.commaSeparatedStringToAuthorityList("uaa.none"));
        }

        // The UAA does not allow or require resource ids to be registered
        // because they are determined dynamically
        client.setResourceIds(Collections.singleton("none"));

        if (client.getScope().isEmpty()) {
            client.setScope(Collections.singleton("uaa.none"));
        }

        if (requestedGrantTypes.contains("implicit")) {
            if (StringUtils.hasText(client.getClientSecret())) {
                throw new InvalidClientDetailsException("Implicit grant should not have a client_secret");
            }
        }
        if (create) {
            // Only check for missing secret if client is being created.
            if ((requestedGrantTypes.contains("client_credentials") || requestedGrantTypes
                            .contains("authorization_code"))
                            && !StringUtils.hasText(client.getClientSecret())) {
                throw new InvalidClientDetailsException(
                                "Client secret is required for client_credentials and authorization_code grant types");
            }
        }

        return client;

    }

    private void checkPasswordChangeIsAllowed(ClientDetails clientDetails, String oldSecret) {

        if (!securityContextAccessor.isClient()) {
            // Trusted client (not acting on behalf of user)
            throw new IllegalStateException("Only a client can change client secret");
        }

        String clientId = clientDetails.getClientId();

        // Call is by client
        String currentClientId = securityContextAccessor.getClientId();

        if (securityContextAccessor.isAdmin()) {

            // even an admin needs to provide the old value to change password
            if (clientId.equals(currentClientId) && !authenticateClient(clientId, oldSecret)) {
                throw new IllegalStateException("Previous secret is required even for admin");
            }

        }
        else {

            if (!clientId.equals(currentClientId)) {
                logger.warn("Client with id " + currentClientId + " attempting to change password for client "
                                + clientId);
                // TODO: This should be audited when we have non-authentication
                // events in the log
                throw new IllegalStateException("Bad request. Not permitted to change another client's secret");
            }

            // Client is changing their own secret, old password is required
            if (!authenticateClient(clientId, oldSecret)) {
                throw new IllegalStateException("Previous secret is required and must be valid");
            }

        }

    }

    private boolean authenticateClient(String clientId, String clientSecret) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(clientId,clientSecret);
        try {
            HttpServletRequest curRequest =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            if (curRequest != null) {
                authentication.setDetails(new UaaAuthenticationDetails(curRequest, clientId));
            }
        }catch (IllegalStateException x) {
            //ignore - means no thread bound request found
        }
        try {
            Authentication auth = authenticationManager.authenticate(authentication);
            return auth.isAuthenticated();
        } catch (AuthenticationException e) {
            return false;
        } catch (Exception e) {
            logger.debug("Unable to authenticate/validate "+clientId, e);
            return false;
        }
    }

    private ClientDetailsModification removeSecret(ClientDetails client) {
        if (client == null) {
            return null;
        }
        ClientDetailsModification details = new ClientDetailsModification(client);
        details.setClientSecret(null);
        return details;
    }

    private ClientDetails syncWithExisting(ClientDetails existing, ClientDetails input) {
        BaseClientDetails details = new BaseClientDetails(input);
        if (details.getAccessTokenValiditySeconds() == null) {
            details.setAccessTokenValiditySeconds(existing.getAccessTokenValiditySeconds());
        }
        if (details.getRefreshTokenValiditySeconds() == null) {
            details.setRefreshTokenValiditySeconds(existing.getRefreshTokenValiditySeconds());
        }
        if (details.getAuthorities() == null || details.getAuthorities().isEmpty()) {
            details.setAuthorities(existing.getAuthorities());
        }
        if (details.getAuthorizedGrantTypes() == null || details.getAuthorizedGrantTypes().isEmpty()) {
            details.setAuthorizedGrantTypes(existing.getAuthorizedGrantTypes());
        }
        if (details.getRegisteredRedirectUri() == null || details.getRegisteredRedirectUri().isEmpty()) {
            details.setRegisteredRedirectUri(existing.getRegisteredRedirectUri());
        }
        if (details.getResourceIds() == null || details.getResourceIds().isEmpty()) {
            details.setResourceIds(existing.getResourceIds());
        }
        if (details.getScope() == null || details.getScope().isEmpty()) {
            details.setScope(existing.getScope());
        }

        Map<String, Object> additionalInformation = new HashMap<String, Object>(existing.getAdditionalInformation());
        additionalInformation.putAll(input.getAdditionalInformation());
        for (String key : Collections.unmodifiableSet(additionalInformation.keySet())) {
            if (additionalInformation.get(key) == null) {
                additionalInformation.remove(key);
            }
        }
        details.setAdditionalInformation(additionalInformation);

        return details;
    }

}
