/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.authentication.web.scheme;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Authenticated;
import org.openmrs.api.context.AuthenticationScheme;
import org.openmrs.api.context.BasicAuthenticated;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.Credentials;
import org.openmrs.api.context.UsernamePasswordAuthenticationScheme;
import org.openmrs.api.context.UsernamePasswordCredentials;
import org.openmrs.module.authentication.AuthenticationLogger;
import org.openmrs.module.authentication.credentials.AuthenticationCredentials;
import org.openmrs.module.authentication.credentials.PrimaryAuthenticationCredentials;
import org.openmrs.module.authentication.scheme.ConfigurableAuthenticationScheme;
import org.openmrs.module.authentication.web.AuthenticationSession;

import java.util.Properties;

/**
 * This is an implementation of a WebAuthenticationScheme that delegates to a UsernamePasswordAuthenticationScheme,
 * and supports basic authentication with a username and password.
 * This scheme supports configuration parameters that enable implementations to utilize it with their own login pages
 * This includes the ability to configure the `loginPage` that the user should be taken to, as well as the
 * `usernameParam` and `passwordParam` that should be read from the http request submission to authenticate.
 * This Step is also configured to handle setting the user's session Location if this is passed in the request,
 * to meet the requirements of existing user interfaces that allow location selection on the login page.
 */
public class BasicWebAuthenticationScheme implements WebAuthenticationScheme {

    public static final String LOGIN_PAGE = "loginPage";
    public static final String USERNAME_PARAM = "usernameParam";
    public static final String PASSWORD_PARAM = "passwordParam";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    private String schemeId;
    private String loginPage;
    private String usernameParam;
    private String passwordParam;


    public BasicWebAuthenticationScheme() {
        this.schemeId = getClass().getName();
    }

    /**
     * @return the configured schemeId
     */
    @Override
    public String getSchemeId() {
        return schemeId;
    }

    /**
     * @see ConfigurableAuthenticationScheme#configure(String, Properties)
     */
    @Override
    public void configure(String schemeId, Properties config) {
        this.schemeId = schemeId;
        loginPage = config.getProperty(LOGIN_PAGE, "/module/authentication/basicLogin.htm");
        usernameParam = config.getProperty(USERNAME_PARAM, USERNAME);
        passwordParam = config.getProperty(PASSWORD_PARAM, PASSWORD);
    }

    /**
     * @see WebAuthenticationScheme#getCredentials(AuthenticationSession)
     */
    @Override
    public AuthenticationCredentials getCredentials(AuthenticationSession session) {
        AuthenticationCredentials credentials = session.getAuthenticationContext().getCredentials(schemeId);
        if (credentials != null) {
            return credentials;
        }
        String username = session.getRequestParam(usernameParam);
        String password = session.getRequestParam(passwordParam);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            credentials = new PrimaryAuthenticationCredentials(schemeId, username, password);
            session.getAuthenticationContext().addCredentials(credentials);
            return credentials;
        }
        else {
            session.sendRedirect(loginPage);
            return null;
        }
    }

    /**
     * @see AuthenticationScheme#authenticate(Credentials)
     */
    @Override
    public Authenticated authenticate(Credentials credentials) throws ContextAuthenticationException {

        // Ensure the credentials provided are of the expected type
        if (!(credentials instanceof PrimaryAuthenticationCredentials)) {
            throw new ContextAuthenticationException("The credentials provided are invalid.");
        }

        PrimaryAuthenticationCredentials bac = (PrimaryAuthenticationCredentials) credentials;
        AuthenticationLogger.addToContext(AuthenticationLogger.USERNAME, bac.getUsername());
        Authenticated authenticated = authenticateWithUsernamePasswordCredentials(bac.toUsernamePasswordCredentials());
        return new BasicAuthenticated(authenticated.getUser(), schemeId);
    }

    /**
     * Method to delegate authentication to the UsernamePasswordAuthenticationScheme.
     * This is separated out in a separate method to allow easier mocking
     * @see UsernamePasswordAuthenticationScheme#authenticate(Credentials) 
     */
    protected Authenticated authenticateWithUsernamePasswordCredentials(UsernamePasswordCredentials credentials) {
        return new UsernamePasswordAuthenticationScheme().authenticate(credentials);
    }
}
