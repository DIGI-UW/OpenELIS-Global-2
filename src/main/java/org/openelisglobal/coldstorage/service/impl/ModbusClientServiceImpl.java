package org.openelisglobal.coldstorage.service.impl;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusRtuClient;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.serial.client.SerialPortClientTransport;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import com.fazecast.jSerialComm.SerialPort;
import io.netty.channel.ChannelOption;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import org.openelisglobal.coldstorage.config.FreezerMonitoringProperties;
import org.openelisglobal.coldstorage.service.ModbusClientService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.util.NetworkValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("unused")
public class ModbusClientServiceImpl implements ModbusClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModbusClientServiceImpl.class);

    private final FreezerMonitoringProperties config;

    public ModbusClientServiceImpl(FreezerMonitoringProperties config) {
        this.config = config;
    }

    @Override
    public Optional<ReadingResult> readCurrentValues(Freezer freezer) {
        int attempts = Math.max(1, config.getRetries() + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return Optional.of(readOnce(freezer));
            } catch (Exception ex) {
                LOGGER.warn("Modbus read attempt {}/{} failed for '{}': {}", attempt, attempts, freezer.getName(),
                        ex.getMessage());
                LOGGER.debug("Modbus read failure for '{}'", freezer.getName(), ex);
            }

            if (attempt < attempts) {
                backoffBeforeRetry(freezer, attempt);
            }
        }
        return Optional.empty();
    }

    /**
     * Waits a short, configurable delay between retry attempts. Retrying
     * back-to-back with zero delay is hard on an overwhelmed device and worse on a
     * shared RS-485 bus where other slaves may also be waiting for a turn. This
     * method runs on the per-device polling thread (see
     * {@link org.openelisglobal.coldstorage.service.impl.ModbusPollingService}),
     * never on the shared scheduler thread, so blocking here does not delay polling
     * of other devices.
     */
    private void backoffBeforeRetry(Freezer freezer, int attempt) {
        long backoffMillis = (long) config.getRetryBackoffMillis() * attempt;
        if (backoffMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Interrupted while backing off before Modbus retry for '{}'", freezer.getName());
        }
    }

    private ReadingResult readOnce(Freezer freezer) throws Exception {
        if (freezer.getProtocol() == Freezer.Protocol.TCP) {
            return readTcp(freezer);
        }
        return readRtu(freezer);
    }

    private ReadingResult readTcp(Freezer freezer) throws Exception {
        // NOTE: TOCTOU risk — hostname is resolved here for validation but resolved
        // again at connect time by Netty. In a LAN-only lab environment this is
        // acceptable; for internet-facing use, resolve to IP first and connect by IP.
        if (NetworkValidationUtil.isBlockedAddress(freezer.getHost())) {
            throw new IllegalArgumentException("Connection to this address is not permitted: " + freezer.getHost());
        }

        NettyTcpClientTransport transport = NettyTcpClientTransport.create(cfg -> {
            cfg.setHostname(freezer.getHost());
            cfg.setPort(freezer.getPort());
            cfg.setConnectTimeout(Duration.ofMillis(config.getConnectTimeoutMillis()));
            // SO_KEEPALIVE so a routed/VPN path that silently drops idle connections
            // (NAT/firewall connection-tracking expiry) is detected and torn down by the
            // OS instead of leaving a half-open socket that fails opaquely on next use
            // (GitHub issue #3904 cross-subnet disconnection reports).
            cfg.setBootstrapCustomizer(bootstrap -> bootstrap.option(ChannelOption.SO_KEEPALIVE, true));
        });

        ModbusTcpClient client = ModbusTcpClient.create(transport,
                builder -> builder.setRequestTimeout(Duration.ofMillis(config.getTimeoutMillis())));

        try {
            try {
                client.connect();
            } catch (Exception ex) {
                throw new ModbusConnectException(freezer.getHost(), freezer.getPort(), ex);
            }
            double temperature = readRegister(client, freezer, freezer.getTemperatureRegister(),
                    freezer.getTemperatureScale(), freezer.getTemperatureOffset());
            Double humidity = null;
            if (freezer.getHumidityRegister() != null) {
                humidity = readRegister(client, freezer, freezer.getHumidityRegister(), freezer.getHumidityScale(),
                        freezer.getHumidityOffset());
            }
            Double temperature2 = null;
            if (freezer.getTemperatureRegister2() != null) {
                temperature2 = readRegister(client, freezer, freezer.getTemperatureRegister2(),
                        freezer.getTemperatureScale2(), freezer.getTemperatureOffset2());
            }
            return new ReadingResult(temperature, humidity, temperature2);
        } finally {
            safeDisconnect(client);
        }
    }

    private ReadingResult readRtu(Freezer freezer) throws Exception {
        if (freezer.getSerialPort() == null || freezer.getSerialPort().isBlank()) {
            throw new IllegalArgumentException("Serial port must be configured for RTU devices");
        }

        SerialPortClientTransport transport = SerialPortClientTransport.create(cfg -> {
            cfg.setSerialPort(freezer.getSerialPort());
            cfg.setBaudRate(defaultInteger(freezer.getBaudRate(), 9600));
            cfg.setDataBits(defaultInteger(freezer.getDataBits(), 8));
            cfg.setStopBits(toStopBits(defaultInteger(freezer.getStopBits(), 1)));
            cfg.setParity(toParity(freezer.getParity()));

            // RS-485 half-duplex driver-enable (DE) timing. Off by default (rs485Mode =
            // false) so existing RS-232/point-to-point serial devices are unaffected.
            // digitalpetri modbus-serial 2.1.5 added these fields to
            // SerialPortTransportConfig for the Modbus-over-Serial-Line spec's RS-485
            // turnaround timing requirements (issue #3743).
            if (Boolean.TRUE.equals(freezer.getRs485Mode())) {
                cfg.setRs485Mode(true);
                cfg.setRs485RtsActiveHigh(Boolean.TRUE.equals(freezer.getRs485RtsActiveHigh()));
                cfg.setRs485Termination(Boolean.TRUE.equals(freezer.getRs485Termination()));
                cfg.setRs485RxDuringTx(Boolean.TRUE.equals(freezer.getRs485RxDuringTx()));
                cfg.setRs485DelayBefore(defaultInteger(freezer.getRs485DelayBeforeMs(), 0));
                cfg.setRs485DelayAfter(defaultInteger(freezer.getRs485DelayAfterMs(), 0));
            }
        });

        ModbusRtuClient client = ModbusRtuClient.create(transport,
                builder -> builder.setRequestTimeout(Duration.ofMillis(config.getTimeoutMillis())));

        try {
            client.connect();
            double temperature = readRegister(client, freezer, freezer.getTemperatureRegister(),
                    freezer.getTemperatureScale(), freezer.getTemperatureOffset());
            Double humidity = null;
            if (freezer.getHumidityRegister() != null) {
                humidity = readRegister(client, freezer, freezer.getHumidityRegister(), freezer.getHumidityScale(),
                        freezer.getHumidityOffset());
            }
            Double temperature2 = null;
            if (freezer.getTemperatureRegister2() != null) {
                temperature2 = readRegister(client, freezer, freezer.getTemperatureRegister2(),
                        freezer.getTemperatureScale2(), freezer.getTemperatureOffset2());
            }
            return new ReadingResult(temperature, humidity, temperature2);
        } finally {
            safeDisconnect(client);
        }
    }

    private double readRegister(ModbusClient client, Freezer freezer, int register, BigDecimal scale, BigDecimal offset)
            throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {
        int registerCount = defaultInteger(freezer.getRegisterCount(), 1);
        if (registerCount != 1 && registerCount != 2) {
            LOGGER.warn("Freezer '{}' has unsupported registerCount {}, defaulting to 1", freezer.getName(),
                    registerCount);
            registerCount = 1;
        }
        ReadHoldingRegistersResponse response = client.readHoldingRegisters(freezer.getSlaveId(),
                new ReadHoldingRegistersRequest(register, registerCount));
        Freezer.WordOrder wordOrder = freezer.getWordOrder() != null ? freezer.getWordOrder()
                : Freezer.WordOrder.BIG_ENDIAN;
        return convertScaledValue(response, registerCount, wordOrder, scale, offset);
    }

    /**
     * Decodes 1 or 2 raw 16-bit Modbus holding registers into a signed value,
     * according to the freezer's configured register width and word order, before
     * applying scale/offset. Defaults (1 register, big-endian/signed short) match
     * the original hardcoded behavior, so existing configured devices are
     * unaffected.
     */
    private double convertScaledValue(ReadHoldingRegistersResponse response, int registerCount,
            Freezer.WordOrder wordOrder, BigDecimal scale, BigDecimal offset) throws ModbusResponseException {
        byte[] registers = response != null ? response.registers() : null;
        int expectedBytes = registerCount * 2;
        if (registers == null || registers.length < expectedBytes) {
            throw new IllegalStateException("No register data returned from Modbus device");
        }

        long raw;
        if (registerCount == 1) {
            raw = toSignedShort(registers[0], registers[1]);
        } else {
            raw = toSignedInt32(registers, wordOrder);
        }

        double scaled = raw * (scale != null ? scale.doubleValue() : 1.0d);
        return scaled + (offset != null ? offset.doubleValue() : 0.0d);
    }

    private int toSignedShort(byte high, byte low) {
        int value = ((high & 0xFF) << 8) | (low & 0xFF);
        if ((value & 0x8000) != 0) {
            value -= 0x10000;
        }
        return value;
    }

    /**
     * Decodes two consecutive 16-bit registers (4 bytes) into a signed 32-bit
     * value. {@code BIG_ENDIAN} treats the first register as the most significant
     * word (each register itself is big-endian per the Modbus spec);
     * {@code LITTLE_ENDIAN} treats the second register as the most significant word
     * (a common "word-swapped" convention on non-compliant devices, e.g. DIY
     * ESP32+DS18B20 slaves - see issue #3743).
     */
    private long toSignedInt32(byte[] registers, Freezer.WordOrder wordOrder) {
        int highWord;
        int lowWord;
        if (wordOrder == Freezer.WordOrder.LITTLE_ENDIAN) {
            lowWord = ((registers[0] & 0xFF) << 8) | (registers[1] & 0xFF);
            highWord = ((registers[2] & 0xFF) << 8) | (registers[3] & 0xFF);
        } else {
            highWord = ((registers[0] & 0xFF) << 8) | (registers[1] & 0xFF);
            lowWord = ((registers[2] & 0xFF) << 8) | (registers[3] & 0xFF);
        }
        long value = ((long) (highWord & 0xFFFF) << 16) | (lowWord & 0xFFFF);
        if ((value & 0x80000000L) != 0) {
            value -= 0x100000000L;
        }
        return value;
    }

    private void safeDisconnect(ModbusClient client) {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception ex) {
            LOGGER.debug("Error disconnecting Modbus client: {}", ex.getMessage(), ex);
        }
    }

    private int toStopBits(int stopBits) {
        return switch (stopBits) {
        case 2 -> SerialPort.TWO_STOP_BITS;
        case 3 -> SerialPort.ONE_POINT_FIVE_STOP_BITS;
        default -> SerialPort.ONE_STOP_BIT;
        };
    }

    private int toParity(Freezer.Parity parity) {
        if (parity == null) {
            return SerialPort.NO_PARITY;
        }
        return switch (parity) {
        case EVEN -> SerialPort.EVEN_PARITY;
        case ODD -> SerialPort.ODD_PARITY;
        case MARK -> SerialPort.MARK_PARITY;
        case SPACE -> SerialPort.SPACE_PARITY;
        default -> SerialPort.NO_PARITY;
        };
    }

    private int defaultInteger(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Wraps a TCP connect failure with the host/port and underlying cause type
     * (e.g. connect timeout vs connection refused vs no route to host), so the
     * retry-loop log line in {@link #readCurrentValues} tells an operator whether a
     * cross-subnet/VPN link is timing out, actively refusing, or unreachable,
     * instead of a bare Netty exception message (GitHub issue #3904).
     */
    private static final class ModbusConnectException extends Exception {
        ModbusConnectException(String host, int port, Throwable cause) {
            super("Failed to connect to " + host + ":" + port + " (" + cause.getClass().getSimpleName() + ": "
                    + cause.getMessage() + ")", cause);
        }
    }
}
