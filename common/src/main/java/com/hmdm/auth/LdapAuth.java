/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.auth;

import com.google.inject.Inject;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.User;
import com.hmdm.persistence.domain.UserRole;
import com.hmdm.persistence.mapper.UserMapper;
import com.hmdm.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;
import java.util.UUID;

@Singleton
public class LdapAuth implements HmdmAuthInterface {

    private static final Logger logger = LoggerFactory.getLogger(LdapAuth.class);

    private static final String[] USER_ATTRS = {"cn", "displayName", "givenName", "sn", "mail"};

    private final UnsecureDAO unsecureDAO;
    private final UserMapper userMapper;
    private final String ldapHost;
    private final int ldapPort;
    private final String ldapBaseDn;
    private final boolean ldapAdminBind;
    private final String ldapAdminDn;
    private final String ldapAdminPassword;
    private final String ldapUsernameAttribute;
    private final String ldapUserDn;
    private final String ldapDefaultRole;
    private final int ldapCustomerId;

    @Inject
    public LdapAuth(UnsecureDAO unsecureDAO,
                    UserMapper userMapper,
                    @Named("ldap.host") String ldapHost,
                    @Named("ldap.port") int ldapPort,
                    @Named("ldap.base.dn") String ldapBaseDn,
                    @Named("ldap.admin.bind") boolean ldapAdminBind,
                    @Named("ldap.admin.dn") String ldapAdminDn,
                    @Named("ldap.admin.password") String ldapAdminPassword,
                    @Named("ldap.username.attribute") String ldapUsernameAttribute,
                    @Named("ldap.user.dn") String ldapUserDn,
                    @Named("ldap.default.role") String ldapDefaultRole,
                    @Named("ldap.customer.id") int ldapCustomerId) {
        this.unsecureDAO = unsecureDAO;
        this.userMapper = userMapper;
        this.ldapHost = ldapHost;
        this.ldapPort = ldapPort;
        this.ldapBaseDn = ldapBaseDn;
        this.ldapAdminBind = ldapAdminBind;
        this.ldapAdminDn = ldapAdminDn;
        this.ldapAdminPassword = ldapAdminPassword;
        this.ldapUsernameAttribute = ldapUsernameAttribute;
        this.ldapUserDn = ldapUserDn;
        this.ldapDefaultRole = ldapDefaultRole;
        this.ldapCustomerId = ldapCustomerId;
    }

    @Override
    public User findUser(String login) {
        User user = unsecureDAO.findByLoginOrEmail(login);
        if (user != null) {
            return user;
        }

        if (ldapHost.isEmpty() || !ldapAdminBind || ldapDefaultRole.isEmpty()) {
            return null;
        }

        logger.info("LDAP: user '{}' not found locally, searching LDAP for auto-provisioning", login);
        LdapContext adminCtx = null;
        try {
            adminCtx = createAdminContext();
            if (adminCtx == null) {
                logger.warn("LDAP: admin bind failed, cannot auto-provision user '{}'", login);
                return null;
            }
            Attributes attrs = searchUser(adminCtx, login);
            if (attrs == null) {
                logger.info("LDAP: user '{}' not found in directory", login);
                return null;
            }
            return provisionUser(login, attrs);
        } catch (NamingException e) {
            logger.error("LDAP: error searching for user '{}'", login, e);
            return null;
        } finally {
            close(adminCtx);
        }
    }

    @Override
    public boolean authenticate(User user, String password) {
        if (ldapHost.isEmpty() || user.getCustomerId() != ldapCustomerId) {
            boolean match = PasswordUtil.passwordMatch(password, user.getPassword());
            if (!match) {
                unsecureDAO.setUserLoginFailTime(user, System.currentTimeMillis());
            }
            return match;
        }

        try {
            String userDn = resolveUserDn(user.getLogin());
            if (userDn == null) {
                logger.warn("LDAP: cannot resolve DN for user '{}'", user.getLogin());
                unsecureDAO.setUserLoginFailTime(user, System.currentTimeMillis());
                return false;
            }
            boolean ok = tryUserBind(userDn, password);
            if (!ok) {
                unsecureDAO.setUserLoginFailTime(user, System.currentTimeMillis());
            }
            return ok;
        } catch (Exception e) {
            logger.error("LDAP: authentication error for user '{}'", user.getLogin(), e);
            unsecureDAO.setUserLoginFailTime(user, System.currentTimeMillis());
            return false;
        }
    }

    private String resolveUserDn(String login) throws NamingException {
        if (ldapAdminBind) {
            LdapContext adminCtx = createAdminContext();
            if (adminCtx == null) return null;
            try {
                return findUserDn(adminCtx, login);
            } finally {
                close(adminCtx);
            }
        } else {
            return buildUserDn(login);
        }
    }

    private LdapContext createAdminContext() throws NamingException {
        return createContext(ldapAdminDn, ldapAdminPassword);
    }

    private LdapContext createContext(String principal, String credentials) throws NamingException {
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + ldapHost + ":" + ldapPort + "/");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put("com.sun.jndi.ldap.read.timeout", "10000");
        try {
            return new InitialLdapContext(env, null);
        } catch (AuthenticationException e) {
            logger.warn("LDAP: bind failed for principal '{}'", principal);
            return null;
        }
    }

    private boolean tryUserBind(String userDn, String password) {
        LdapContext ctx = null;
        try {
            ctx = createContext(userDn, password);
            return ctx != null;
        } catch (NamingException e) {
            return false;
        } finally {
            close(ctx);
        }
    }

    private String findUserDn(LdapContext ctx, String login) throws NamingException {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[0]);
        controls.setCountLimit(1);
        String filter = "(" + ldapUsernameAttribute + "=" + escapeLdap(login) + ")";
        NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDn, filter, controls);
        try {
            return results.hasMore() ? results.next().getNameInNamespace() : null;
        } finally {
            try { results.close(); } catch (NamingException ignored) {}
        }
    }

    private Attributes searchUser(LdapContext ctx, String login) throws NamingException {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(USER_ATTRS);
        controls.setCountLimit(1);
        String filter = "(" + ldapUsernameAttribute + "=" + escapeLdap(login) + ")";
        NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDn, filter, controls);
        try {
            return results.hasMore() ? results.next().getAttributes() : null;
        } finally {
            try { results.close(); } catch (NamingException ignored) {}
        }
    }

    private User provisionUser(String login, Attributes attrs) {
        UserRole role = userMapper.findUserRoleByName(ldapDefaultRole);
        if (role == null) {
            logger.error("LDAP: auto-provisioning failed — role '{}' not found in HMDM", ldapDefaultRole);
            return null;
        }

        String displayName = getAttr(attrs, "displayName");
        if (displayName == null) displayName = getAttr(attrs, "cn");
        if (displayName == null) displayName = login;

        String email = getAttr(attrs, "mail");

        User user = new User();
        user.setLogin(login);
        user.setName(displayName);
        user.setEmail(email != null ? email : "");
        user.setPassword(PasswordUtil.getHashFromRaw(UUID.randomUUID().toString()));
        user.setCustomerId(ldapCustomerId);
        user.setUserRole(role);
        user.setAllDevicesAvailable(true);
        user.setAllConfigAvailable(true);
        user.setAuthToken(PasswordUtil.generateToken());

        try {
            userMapper.insert(user);
            logger.info("LDAP: auto-provisioned user '{}' with role '{}'", login, ldapDefaultRole);
            return unsecureDAO.findByLogin(login);
        } catch (Exception e) {
            logger.error("LDAP: failed to insert auto-provisioned user '{}'", login, e);
            return null;
        }
    }

    private String buildUserDn(String login) {
        if (!ldapUserDn.isEmpty()) {
            return ldapUserDn.replace("{0}", escapeLdap(login));
        }
        return ldapUsernameAttribute + "=" + escapeLdap(login) + "," + ldapBaseDn;
    }

    private String getAttr(Attributes attrs, String name) {
        try {
            Attribute attr = attrs.get(name);
            if (attr != null) {
                Object val = attr.get();
                return val != null ? val.toString() : null;
            }
        } catch (NamingException ignored) {}
        return null;
    }

    private String escapeLdap(String input) {
        return input
                .replace("\\", "\\5c")
                .replace("*",  "\\2a")
                .replace("(",  "\\28")
                .replace(")",  "\\29")
                .replace("\0", "\\00");
    }

    private void close(LdapContext ctx) {
        if (ctx != null) {
            try { ctx.close(); } catch (NamingException ignored) {}
        }
    }
}
