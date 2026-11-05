/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.netfilter.rest;

import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Device;
import com.hmdm.plugins.netfilter.persistence.domain.NetFilterProfile;
import com.hmdm.plugins.netfilter.persistence.domain.NetFilterProfileRule;
import com.hmdm.plugins.netfilter.persistence.domain.NetFilterRule;
import com.hmdm.plugins.netfilter.persistence.domain.NetFilterSettings;
import com.hmdm.plugins.netfilter.persistence.mapper.NetFilterMapper;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;

@Singleton
@Path("/plugins/netfilter")
public class NetFilterResource {

    private static final Logger logger = LoggerFactory.getLogger(NetFilterResource.class);

    private final NetFilterMapper mapper;
    private final UnsecureDAO unsecureDAO;

    @Inject
    public NetFilterResource(NetFilterMapper mapper, UnsecureDAO unsecureDAO) {
        this.mapper = mapper;
        this.unsecureDAO = unsecureDAO;
    }

    // =========================================================================
    // Legacy endpoints (customer-level settings & rules) — kept for compat
    // =========================================================================

    @GET
    @Path("/private/settings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSettings() {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                NetFilterSettings settings = mapper.findSettings(user.getCustomerId());
                if (settings == null) {
                    settings = new NetFilterSettings();
                    settings.setCustomerId(user.getCustomerId());
                    settings.setEnabled(false);
                    settings.setFilterMode(NetFilterSettings.MODE_BLOCKLIST);
                }
                return Response.OK(settings);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to get netfilter settings", e);
            return Response.INTERNAL_ERROR();
        }
    }

    @PUT
    @Path("/private/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveSettings(NetFilterSettings settings) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                settings.setCustomerId(user.getCustomerId());
                mapper.saveSettings(settings);
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to save netfilter settings", e);
            return Response.INTERNAL_ERROR();
        }
    }

    @GET
    @Path("/private/rules")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRules() {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                List<NetFilterRule> rules = mapper.findRules(user.getCustomerId());
                return Response.OK(rules);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to list netfilter rules", e);
            return Response.INTERNAL_ERROR();
        }
    }

    @POST
    @Path("/private/rules")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addRule(NetFilterRule rule) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (rule.getDomain() == null || rule.getDomain().trim().isEmpty()) {
                    return Response.ERROR("domain is required");
                }
                if (rule.getRuleType() == null
                        || (!rule.getRuleType().equals("ALLOW") && !rule.getRuleType().equals("BLOCK"))) {
                    return Response.ERROR("ruleType must be ALLOW or BLOCK");
                }
                rule.setCustomerId(user.getCustomerId());
                rule.setDomain(rule.getDomain().trim().toLowerCase());
                rule.setEnabled(true);
                rule.setCreatedAt(System.currentTimeMillis());
                mapper.insertRule(rule);
                return Response.OK(rule);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to add netfilter rule", e);
            return Response.INTERNAL_ERROR();
        }
    }

    @PUT
    @Path("/private/rules/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRule(@PathParam("id") int id, NetFilterRule rule) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                rule.setId(id);
                rule.setCustomerId(user.getCustomerId());
                if (rule.getDomain() != null) {
                    rule.setDomain(rule.getDomain().trim().toLowerCase());
                }
                int updated = mapper.updateRule(rule);
                if (updated == 0) {
                    return Response.ERROR("Rule not found");
                }
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to update netfilter rule {}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @DELETE
    @Path("/private/rules/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRule(@PathParam("id") int id) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                int deleted = mapper.deleteRule(id, user.getCustomerId());
                if (deleted == 0) {
                    return Response.ERROR("Rule not found");
                }
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to delete netfilter rule {}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =========================================================================
    // Profiles
    // =========================================================================

    @GET
    @Path("/private/profiles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProfiles() {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                List<NetFilterProfile> profiles = mapper.findProfiles(user.getCustomerId());
                return Response.OK(profiles);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to list netfilter profiles", e);
            return Response.INTERNAL_ERROR();
        }
    }

    @POST
    @Path("/private/profiles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfile(NetFilterProfile profile) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (profile.getName() == null || profile.getName().trim().isEmpty()) {
                    return Response.ERROR("name is required");
                }
                if (profile.getFilterMode() == null
                        || (!profile.getFilterMode().equals(NetFilterSettings.MODE_ALLOWLIST)
                            && !profile.getFilterMode().equals(NetFilterSettings.MODE_BLOCKLIST))) {
                    return Response.ERROR("filterMode must be ALLOWLIST or BLOCKLIST");
                }
                profile.setCustomerId(user.getCustomerId());
                profile.setName(profile.getName().trim());
                profile.setEnabled(true);
                profile.setCreatedAt(System.currentTimeMillis());
                mapper.insertProfile(profile);
                return Response.OK(profile);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to create netfilter profile", e);
            return Response.INTERNAL_ERROR();
        }
    }

    @PUT
    @Path("/private/profiles/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(@PathParam("id") int id, NetFilterProfile profile) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (mapper.findProfileById(id, user.getCustomerId()) == null) {
                    return Response.ERROR("Profile not found");
                }
                if (profile.getName() == null || profile.getName().trim().isEmpty()) {
                    return Response.ERROR("name is required");
                }
                if (profile.getFilterMode() == null
                        || (!profile.getFilterMode().equals(NetFilterSettings.MODE_ALLOWLIST)
                            && !profile.getFilterMode().equals(NetFilterSettings.MODE_BLOCKLIST))) {
                    return Response.ERROR("filterMode must be ALLOWLIST or BLOCKLIST");
                }
                profile.setId(id);
                profile.setCustomerId(user.getCustomerId());
                profile.setName(profile.getName().trim());
                mapper.updateProfile(profile);
                return Response.OK(profile);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to update netfilter profile {}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @DELETE
    @Path("/private/profiles/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteProfile(@PathParam("id") int id) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                int deleted = mapper.deleteProfile(id, user.getCustomerId());
                if (deleted == 0) {
                    return Response.ERROR("Profile not found");
                }
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to delete netfilter profile {}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =========================================================================
    // Profile rules
    // =========================================================================

    @GET
    @Path("/private/profiles/{id}/rules")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProfileRules(@PathParam("id") int id) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (mapper.findProfileById(id, user.getCustomerId()) == null) {
                    return Response.ERROR("Profile not found");
                }
                List<NetFilterProfileRule> rules = mapper.findProfileRules(id);
                return Response.OK(rules);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to list rules for profile {}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @POST
    @Path("/private/profiles/{id}/rules")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addProfileRule(@PathParam("id") int id, NetFilterProfileRule rule) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (mapper.findProfileById(id, user.getCustomerId()) == null) {
                    return Response.ERROR("Profile not found");
                }
                if (rule.getDomain() == null || rule.getDomain().trim().isEmpty()) {
                    return Response.ERROR("domain is required");
                }
                if (rule.getRuleType() == null
                        || (!rule.getRuleType().equals("ALLOW") && !rule.getRuleType().equals("BLOCK"))) {
                    return Response.ERROR("ruleType must be ALLOW or BLOCK");
                }
                rule.setProfileId(id);
                rule.setDomain(rule.getDomain().trim().toLowerCase());
                rule.setEnabled(true);
                rule.setCreatedAt(System.currentTimeMillis());
                mapper.insertProfileRule(rule);
                return Response.OK(rule);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to add rule to profile {}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @PUT
    @Path("/private/profiles/{id}/rules/{ruleId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfileRule(@PathParam("id") int id, @PathParam("ruleId") int ruleId, NetFilterProfileRule rule) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (mapper.findProfileById(id, user.getCustomerId()) == null) {
                    return Response.ERROR("Profile not found");
                }
                rule.setId(ruleId);
                rule.setProfileId(id);
                if (rule.getDomain() != null) {
                    rule.setDomain(rule.getDomain().trim().toLowerCase());
                }
                int updated = mapper.updateProfileRule(rule);
                if (updated == 0) {
                    return Response.ERROR("Rule not found");
                }
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to update rule {} in profile {}", ruleId, id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @DELETE
    @Path("/private/profiles/{id}/rules/{ruleId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteProfileRule(@PathParam("id") int id, @PathParam("ruleId") int ruleId) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (mapper.findProfileById(id, user.getCustomerId()) == null) {
                    return Response.ERROR("Profile not found");
                }
                int deleted = mapper.deleteProfileRule(ruleId, id);
                if (deleted == 0) {
                    return Response.ERROR("Rule not found");
                }
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to delete rule {} in profile {}", ruleId, id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =========================================================================
    // Profile-to-configuration mapping
    // =========================================================================

    @GET
    @Path("/private/profiles/{id}/configs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProfileConfigs(@PathParam("id") int id) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (mapper.findProfileById(id, user.getCustomerId()) == null) {
                    return Response.ERROR("Profile not found");
                }
                List<Integer> configIds = mapper.findProfileConfigIds(id);
                return Response.OK(configIds);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to list configs for profile {}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @POST
    @Path("/private/profiles/{id}/configs/{configurationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addConfigToProfile(@PathParam("id") int id, @PathParam("configurationId") int configurationId) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (mapper.findProfileById(id, user.getCustomerId()) == null) {
                    return Response.ERROR("Profile not found");
                }
                mapper.addConfigToProfile(id, configurationId);
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to add config {} to profile {}", configurationId, id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @DELETE
    @Path("/private/profiles/{id}/configs/{configurationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeConfigFromProfile(@PathParam("id") int id, @PathParam("configurationId") int configurationId) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (mapper.findProfileById(id, user.getCustomerId()) == null) {
                    return Response.ERROR("Profile not found");
                }
                int deleted = mapper.removeConfigFromProfile(id, configurationId);
                if (deleted == 0) {
                    return Response.ERROR("Configuration not found in profile");
                }
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to remove config {} from profile {}", configurationId, id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =========================================================================
    // Device public endpoint — merge all profiles for the device's config
    // =========================================================================

    @GET
    @Path("/public/config/{deviceNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceConfig(@PathParam("deviceNumber") String deviceNumber) {
        try {
            Device device = unsecureDAO.getDeviceByNumber(deviceNumber);
            if (device == null) {
                return Response.ERROR("Device not found");
            }

            List<String> filterModes = mapper.findFilterModesByDeviceNumber(deviceNumber);
            List<NetFilterProfileRule> rawRules = mapper.findEnabledProfileRulesByDeviceNumber(deviceNumber);

            if (filterModes.isEmpty()) {
                NetFilterDeviceConfig config = new NetFilterDeviceConfig();
                config.setEnabled(false);
                config.setFilterMode(NetFilterSettings.MODE_BLOCKLIST);
                config.setRules(Collections.emptyList());
                return Response.OK(config);
            }

            // ALLOWLIST wins if any profile uses it (most restrictive default)
            String effectiveMode = filterModes.contains(NetFilterSettings.MODE_ALLOWLIST)
                    ? NetFilterSettings.MODE_ALLOWLIST
                    : NetFilterSettings.MODE_BLOCKLIST;

            // Merge rules: BLOCK wins when same domain appears in multiple profiles
            Map<String, String> merged = new LinkedHashMap<>();
            for (NetFilterProfileRule r : rawRules) {
                String existing = merged.get(r.getDomain());
                if (existing == null || "ALLOW".equals(existing)) {
                    merged.put(r.getDomain(), r.getRuleType());
                }
                // If existing is already BLOCK, keep it (BLOCK wins)
            }

            List<NetFilterProfileRule> resolvedRules = new ArrayList<>();
            for (Map.Entry<String, String> entry : merged.entrySet()) {
                NetFilterProfileRule r = new NetFilterProfileRule();
                r.setDomain(entry.getKey());
                r.setRuleType(entry.getValue());
                r.setEnabled(true);
                resolvedRules.add(r);
            }

            NetFilterDeviceConfig config = new NetFilterDeviceConfig();
            config.setEnabled(true);
            config.setFilterMode(effectiveMode);
            config.setRules(resolvedRules);
            return Response.OK(config);
        } catch (Exception e) {
            logger.error("Failed to get netfilter device config for {}", deviceNumber, e);
            return Response.INTERNAL_ERROR();
        }
    }

    public static class NetFilterDeviceConfig {
        private boolean enabled;
        private String filterMode;
        private List<NetFilterProfileRule> rules;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getFilterMode() { return filterMode; }
        public void setFilterMode(String filterMode) { this.filterMode = filterMode; }

        public List<NetFilterProfileRule> getRules() { return rules; }
        public void setRules(List<NetFilterProfileRule> rules) { this.rules = rules; }
    }
}
