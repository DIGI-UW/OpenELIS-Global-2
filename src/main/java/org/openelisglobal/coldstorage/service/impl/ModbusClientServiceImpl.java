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
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import org.openelisglobal.coldstorage.config.FreezerMonitoringProperties;
import org.openelisglobal.coldstorage.service.ModbusClientService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
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
        }
        return Optional.empty();
    }

    private ReadingResult readOnce(Freezer freezer) throws Exception {
        if (freezer.getProtocol() == Freezer.Protocol.TCP) {
            return readTcp(freezer);
        }
        return readRtu(freezer);
    }

    private ReadingResult readTcp(Freezer freezer) throws Exception {
        NettyTcpClientTransport transport = NettyTcpClientTransport.create(cfg -> {
            cfg.setHostname(freezer.getHost());
            cfg.setPort(freezer.getPort());
            cfg.setConnectTimeout(Duration.ofMillis(config.getTimeoutMillis()));
        });

        ModbusTcpClient client = ModbusTcpClient.create(transport,
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
            return new ReadingResult(temperature, humidity);
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
            return new ReadingResult(temperature, humidity);
        } finally {
            safeDisconnect(client);
        }
    }

    private double readRegister(ModbusClient client, Freezer freezer, int register, BigDecimal scale, BigDecimal offset)
            throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {
        ReadHoldingRegistersResponse response = client.readHoldingRegisters(freezer.getSlaveId(),
                new ReadHoldingRegistersRequest(register, 1));
        return convertScaledValue(response, scale, offset);
    }

    private double convertScaledValue(ReadHoldingRegistersResponse response, BigDecimal scale, BigDecimal offset)
            throws ModbusResponseException {
        byte[] registers = response != null ? response.registers() : null;
        if (registers == null || registers.length < 2) {
            throw new IllegalStateException("No register data returned from Modbus device");
        }
        int raw = toSignedShort(registers[0], registers[1]);
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
}
