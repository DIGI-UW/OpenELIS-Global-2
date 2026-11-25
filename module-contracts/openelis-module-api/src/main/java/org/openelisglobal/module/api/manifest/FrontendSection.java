package org.openelisglobal.module.api.manifest;

import java.util.ArrayList;
import java.util.List;

/**
 * The module's prebuilt frontend, shipped inside the JAR under
 * {@code META-INF/resources/modules/<id>/}. The shell loads {@code entry} as a
 * Module Federation remote named after the module id (hyphens replaced by
 * underscores) and renders each route's {@code expose} behind the permission
 * gate.
 */
public class FrontendSection {

    private String entry = "remoteEntry.js";
    private String messages = "messages/{locale}.json";
    private List<RouteDeclaration> routes = new ArrayList<>();
    private List<SlotDeclaration> slots = new ArrayList<>();

    /** The remote entry file, relative to the module's asset directory. */
    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    /**
     * The message file pattern, relative to the asset directory; {@code {locale}}
     * is replaced by the active locale.
     */
    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public List<RouteDeclaration> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteDeclaration> routes) {
        this.routes = routes == null ? new ArrayList<>() : routes;
    }

    public List<SlotDeclaration> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotDeclaration> slots) {
        this.slots = slots == null ? new ArrayList<>() : slots;
    }

    /**
     * One page: an absolute path under {@code /m/<id>/}, the exposed component that
     * renders it and the module permission key or core role that gates it (at least
     * one of the two).
     */
    public static class RouteDeclaration {

        private String path;
        private boolean exact = true;
        private String expose;
        private String permission;
        private String role;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isExact() {
            return exact;
        }

        public void setExact(boolean exact) {
            this.exact = exact;
        }

        /** The federation expose name, for example {@code ./FollowUpListPage}. */
        public String getExpose() {
            return expose;
        }

        public void setExpose(String expose) {
            this.expose = expose;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    /** One contribution to a named extension slot of a core page. */
    public static class SlotDeclaration {

        private String slot;
        private String expose;
        private int order = 100;
        private String permission;

        public String getSlot() {
            return slot;
        }

        public void setSlot(String slot) {
            this.slot = slot;
        }

        public String getExpose() {
            return expose;
        }

        public void setExpose(String expose) {
            this.expose = expose;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }
    }
}
