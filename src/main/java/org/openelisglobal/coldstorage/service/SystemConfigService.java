package org.openelisglobal.coldstorage.service;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.time.OffsetDateTime;
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemConfigService {

    private static final String MONITORING_ENABLED_KEY = "freezer.monitoring.enabled";
    private static final String MODBUS_TIMEOUT_MILLIS_KEY = "freezer.modbus.timeout.ms";
    private static final int DEFAULT_TIMEOUT_MILLIS = 2000;

    private final SiteInformationService siteInformationService;
    private final SiteInformationDomainService siteInformationDomainService;

    private SiteInformationDomain siteIdentityDomain;

    @Autowired
    public SystemConfigService(SiteInformationService siteInformationService,
            SiteInformationDomainService siteInformationDomainService) {
        this.siteInformationService = siteInformationService;
        this.siteInformationDomainService = siteInformationDomainService;
    }

    @PostConstruct
    private void initialize() {
        siteIdentityDomain = siteInformationDomainService.getByName("siteIdentity");
    }

    /**
     * DTO for system configuration response (maintains API compatibility)
     */
    public static class SystemConfigDTO {
        private Integer modbusTcpPort = 502;
        private Integer bacnetUdpPort = 47808;
        private Boolean monitoringEnabled = Boolean.TRUE;
        private Integer modbusTimeoutMillis = DEFAULT_TIMEOUT_MILLIS;
        private Boolean twoFactorAuthEnabled = Boolean.FALSE;
        private Integer sessionTimeoutMinutes = 30;
        private String systemVersion;
        private String databaseVersion;
        private OffsetDateTime lastUpdate;
        private Long uptimeSeconds = 0L;

        // Getters and setters
        public Integer getModbusTcpPort() {
            return modbusTcpPort;
        }

        public void setModbusTcpPort(Integer modbusTcpPort) {
            this.modbusTcpPort = modbusTcpPort;
        }

        public Integer getBacnetUdpPort() {
            return bacnetUdpPort;
        }

        public void setBacnetUdpPort(Integer bacnetUdpPort) {
            this.bacnetUdpPort = bacnetUdpPort;
        }

        public Boolean getMonitoringEnabled() {
            return monitoringEnabled;
        }

        public void setMonitoringEnabled(Boolean monitoringEnabled) {
            this.monitoringEnabled = monitoringEnabled;
        }

        public Integer getModbusTimeoutMillis() {
            return modbusTimeoutMillis;
        }

        public void setModbusTimeoutMillis(Integer modbusTimeoutMillis) {
            this.modbusTimeoutMillis = modbusTimeoutMillis;
        }

        public Boolean getTwoFactorAuthEnabled() {
            return twoFactorAuthEnabled;
        }

        public void setTwoFactorAuthEnabled(Boolean twoFactorAuthEnabled) {
            this.twoFactorAuthEnabled = twoFactorAuthEnabled;
        }

        public Integer getSessionTimeoutMinutes() {
            return sessionTimeoutMinutes;
        }

        public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
            this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        }

        public String getSystemVersion() {
            return systemVersion;
        }

        public void setSystemVersion(String systemVersion) {
            this.systemVersion = systemVersion;
        }

        public String getDatabaseVersion() {
            return databaseVersion;
        }

        public void setDatabaseVersion(String databaseVersion) {
            this.databaseVersion = databaseVersion;
        }

        public OffsetDateTime getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(OffsetDateTime lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

        public Long getUptimeSeconds() {
            return uptimeSeconds;
        }

        public void setUptimeSeconds(Long uptimeSeconds) {
            this.uptimeSeconds = uptimeSeconds;
        }
    }

    @Transactional(readOnly = true)
    public SystemConfigDTO getGlobalConfig() {
        SystemConfigDTO config = new SystemConfigDTO();

        // Read from SiteInformation
        SiteInformation modbusTcpPort = siteInformationService.getSiteInformationByName("freezer.modbus.tcp.port");
        if (modbusTcpPort != null && modbusTcpPort.getValue() != null) {
            try {
                config.setModbusTcpPort(Integer.parseInt(modbusTcpPort.getValue()));
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        SiteInformation bacnetUdpPort = siteInformationService.getSiteInformationByName("freezer.bacnet.udp.port");
        if (bacnetUdpPort != null && bacnetUdpPort.getValue() != null) {
            try {
                config.setBacnetUdpPort(Integer.parseInt(bacnetUdpPort.getValue()));
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        config.setMonitoringEnabled(isMonitoringEnabled());
        config.setModbusTimeoutMillis(getModbusTimeoutMillis());

        // Runtime metadata (computed on-the-fly)
        config.setLastUpdate(OffsetDateTime.now());
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        config.setUptimeSeconds(uptimeMillis / 1000);

        // System version and database version should use existing versioning
        // infrastructure
        // For now, set to null (can be enhanced later)
        config.setSystemVersion(null);
        config.setDatabaseVersion(null);

        return config;
    }

    @Transactional
    public SystemConfigDTO saveConfig(SystemConfigDTO config) {
        // Save modbus TCP port
        SiteInformation modbusTcpPort = siteInformationService.getSiteInformationByName("freezer.modbus.tcp.port");
        if (modbusTcpPort == null) {
            modbusTcpPort = new SiteInformation();
            modbusTcpPort.setName("freezer.modbus.tcp.port");
            modbusTcpPort.setDescription("Modbus TCP port for freezer monitoring (default: 502)");
            modbusTcpPort.setValueType("text");
            modbusTcpPort.setEncrypted(false);
            modbusTcpPort.setDomain(siteIdentityDomain);
            modbusTcpPort.setGroup(0);
            siteInformationService.persistData(modbusTcpPort, true);
        }
        modbusTcpPort.setValue(String.valueOf(config.getModbusTcpPort()));
        siteInformationService.persistData(modbusTcpPort, false);

        // Save BACnet UDP port
        SiteInformation bacnetUdpPort = siteInformationService.getSiteInformationByName("freezer.bacnet.udp.port");
        if (bacnetUdpPort == null) {
            bacnetUdpPort = new SiteInformation();
            bacnetUdpPort.setName("freezer.bacnet.udp.port");
            bacnetUdpPort.setDescription("BACnet UDP port for freezer monitoring (default: 47808)");
            bacnetUdpPort.setValueType("text");
            bacnetUdpPort.setEncrypted(false);
            bacnetUdpPort.setDomain(siteIdentityDomain);
            bacnetUdpPort.setGroup(0);
            siteInformationService.persistData(bacnetUdpPort, true);
        }
        bacnetUdpPort.setValue(String.valueOf(config.getBacnetUdpPort()));
        siteInformationService.persistData(bacnetUdpPort, false);

        saveSetting(MONITORING_ENABLED_KEY, "Enable/disable freezer Modbus polling (default: true)",
                String.valueOf(config.getMonitoringEnabled()));
        saveSetting(MODBUS_TIMEOUT_MILLIS_KEY,
                "Modbus connect/request timeout in milliseconds - raise this for devices reached over a "
                        + "routed subnet or VPN (default: " + DEFAULT_TIMEOUT_MILLIS + ")",
                String.valueOf(config.getModbusTimeoutMillis()));

        // Update runtime metadata
        config.setLastUpdate(OffsetDateTime.now());
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        config.setUptimeSeconds(uptimeMillis / 1000);

        return config;
    }

    private void saveSetting(String name, String description, String value) {
        SiteInformation setting = siteInformationService.getSiteInformationByName(name);
        if (setting == null) {
            setting = new SiteInformation();
            setting.setName(name);
            setting.setDescription(description);
            setting.setValueType("text");
            setting.setEncrypted(false);
            setting.setDomain(siteIdentityDomain);
            setting.setGroup(0);
            siteInformationService.persistData(setting, true);
        }
        setting.setValue(value);
        siteInformationService.persistData(setting, false);
    }

    /**
     * Live-read kill switch for {@code ModbusPollingService}'s poll cycle - a plain
     * DB row rather than a static property so an admin can toggle monitoring
     * without a restart. Defaults to enabled so a fresh install behaves the same as
     * the previous static-property default.
     */
    @Transactional(readOnly = true)
    public boolean isMonitoringEnabled() {
        SiteInformation setting = siteInformationService.getSiteInformationByName(MONITORING_ENABLED_KEY);
        return setting == null || setting.getValue() == null || Boolean.parseBoolean(setting.getValue());
    }

    /**
     * Live-read Modbus connect/request timeout, in milliseconds. Read fresh on
     * every poll (see {@code ModbusClientServiceImpl}) rather than cached, so a
     * timeout tweak while troubleshooting a flaky cross-subnet/VPN link takes
     * effect on the next poll cycle, not after a restart.
     */
    @Transactional(readOnly = true)
    public int getModbusTimeoutMillis() {
        SiteInformation setting = siteInformationService.getSiteInformationByName(MODBUS_TIMEOUT_MILLIS_KEY);
        if (setting != null && setting.getValue() != null) {
            try {
                return Integer.parseInt(setting.getValue());
            } catch (NumberFormatException e) {
                // fall through to default
            }
        }
        return DEFAULT_TIMEOUT_MILLIS;
    }
}
