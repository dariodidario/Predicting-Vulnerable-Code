/* Copyright 2004, 2005 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acegisecurity.ldap.search;

import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.ldap.LdapUserSearch;
import org.acegisecurity.ldap.LdapUtils;
import org.acegisecurity.ldap.InitialDirContextFactory;
import org.acegisecurity.ldap.LdapUserInfo;
import org.acegisecurity.ldap.LdapDataAccessException;
import org.springframework.util.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.directory.DirContext;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

/**
 * LdapUserSearch implementation which uses an Ldap filter to locate the user.
 *
 * @see SearchControls
 *
 * @author Robert Sanders
 * @author Luke Taylor
 * @version $Id$
 */
public class FilterBasedLdapUserSearch implements LdapUserSearch {
    //~ Static fields/initializers =============================================

    private static final Log logger = LogFactory.getLog(FilterBasedLdapUserSearch.class);

    //~ Instance fields ========================================================

    /**
     * Context name to search in, relative to the root DN of the configured
     * InitialDirContextFactory.
     */
    private String searchBase = "";

    /**
     * The LDAP SearchControls object used for the search. Shared between searches
     * so shouldn't be modified once the bean has been configured.
     */
    private SearchControls searchControls = new SearchControls();

    /**
     * The filter expression used in the user search. This is an LDAP
     * search filter (as defined in 'RFC 2254') with optional arguments. See the documentation
     * for the <tt>search</tt> methods in {@link javax.naming.directory.DirContext DirContext}
     * for more information.
     * <p>
     * In this case, the username is the only parameter.
     * </p>
     * Possible examples are:
     * <ul>
     * <li>(uid={0}) - this would search for a username match on the uid attribute.</li>
     * </ul>
     * TODO: more examples.
     *
     */
    private String searchFilter;

    private InitialDirContextFactory initialDirContextFactory;

    //~ Methods ================================================================

    public FilterBasedLdapUserSearch(String searchBase,
                                     String searchFilter,
                                     InitialDirContextFactory initialDirContextFactory) {
        Assert.notNull(initialDirContextFactory, "initialDirContextFactory must not be null");
        Assert.notNull(searchFilter, "searchFilter must not be null.");
        Assert.notNull(searchBase, "searchBase must not be null (an empty string is acceptable).");

        this.searchFilter = searchFilter;
        this.initialDirContextFactory = initialDirContextFactory;
        this.searchBase = searchBase;

        if(searchBase.length() == 0) {
            logger.info("SearchBase not set. Searches will be performed from the root: " +
                    initialDirContextFactory.getRootDn());
        }
    }

    //~ Methods ================================================================

    /**
     * Return the LdapUserInfo containing the user's information, or null if
     * no SearchResult is found.
     *
     * @param username the username to search for.
     */
    public LdapUserInfo searchForUser(String username) {
        DirContext ctx = initialDirContextFactory.newInitialDirContext();

        if (logger.isDebugEnabled()) {
            logger.debug("Searching for user '" + username + "', in context " + ctx +
                    ", with user search " + this.toString());
        }

        try {
            String[] args = new String[] { LdapUtils.escapeNameForFilter(username) };

            NamingEnumeration results = ctx.search(searchBase, searchFilter, args, searchControls);

            if (!results.hasMore()) {
                throw new UsernameNotFoundException("User " + username + " not found in directory.");
            }

            SearchResult searchResult = (SearchResult)results.next();

            if (results.hasMore()) {
               throw new BadCredentialsException("Expected a single user but search returned multiple results");
            }

            StringBuffer userDn = new StringBuffer(searchResult.getName());

            if (searchBase.length() > 0) {
                userDn.append(",");
                userDn.append(searchBase);
            }

            userDn.append(",");
            userDn.append(ctx.getNameInNamespace());

            return new LdapUserInfo(userDn.toString(), searchResult.getAttributes());

        } catch(NamingException ne) {
            throw new LdapDataAccessException("User Couldn't be found due to exception", ne);
        } finally {
            LdapUtils.closeContext(ctx);
        }
    }

    /**
     * If true then searches the entire subtree as identified by context,
     * if false (the default) then only searches the level identified by the context.
     */
    public void setSearchSubtree(boolean searchSubtree) {
        searchControls.setSearchScope(searchSubtree ?
                SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
    }

    /**
     * The time (in milliseconds) which to wait before the search fails;
     * the default is zero, meaning forever.
     */
    public void setSearchTimeLimit(int searchTimeLimit) {
        searchControls.setTimeLimit(searchTimeLimit);
    }

    /**
     * Sets the corresponding property on the SearchControls instance used
     * in the search.
     *
     */
    public void setDerefLinkFlag(boolean deref) {
        searchControls.setDerefLinkFlag(deref);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("[ searchFilter: '").append(searchFilter).append("', ");
        sb.append("searchBase: '").append(searchBase).append("'");
        sb.append(", scope: ").append(searchControls.getSearchScope() ==
                SearchControls.SUBTREE_SCOPE ? "subtree" : "single-level, ");
        sb.append("searchTimeLimit: ").append(searchControls.getTimeLimit());
        sb.append("derefLinkFlag: ").append(searchControls.getDerefLinkFlag()).append(" ]");

        return sb.toString();
    }
}
