package cn.imhtb.live.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalServiceLifecycle {

    private final LocalServiceProperties properties;
    private final Environment environment;
    private final List<Process> startedProcesses = new ArrayList<>();

    private Path projectRoot;

    @EventListener(ApplicationReadyEvent.class)
    public void startLocalServices() {
        if (!properties.isEnabled()) {
            log.info("PulseLive local service auto-start is disabled.");
            return;
        }

        projectRoot = resolveProjectRoot();
        if (projectRoot == null) {
            log.warn("PulseLive project root was not found. Skip local service auto-start.");
            return;
        }

        properties.getServices().forEach(this::startService);
    }

    @PreDestroy
    public void stopLocalServices() {
        if (!properties.isEnabled()) {
            return;
        }

        Path root = projectRoot != null ? projectRoot : resolveProjectRoot();
        if (root != null) {
            properties.getShutdownPorts().forEach(this::stopPort);
        }

        startedProcesses.forEach(process -> {
            if (process.isAlive()) {
                process.destroy();
            }
        });
    }

    private void startService(String serviceName) {
        if (!StringUtils.hasText(serviceName)) {
            return;
        }

        ServiceSpec spec = getServiceSpec(serviceName);
        if (spec == null) {
            log.warn("Unknown PulseLive local service: {}", serviceName);
            return;
        }

        try {
            if (isServiceAvailable(spec)) {
                log.info("PulseLive local service is already available: {}, ports={}", serviceName, spec.getPorts());
                return;
            }

            ProcessBuilder builder = createServiceProcess(spec);
            if (builder == null) {
                log.warn("Unknown PulseLive local service: {}", serviceName);
                return;
            }

            Path logFile = redirectLog(builder, projectRoot, serviceName);
            Process process = builder.start();
            startedProcesses.add(process);
            if (waitForService(spec, process)) {
                log.info("Started PulseLive local service: {}, ports={}, log={}", serviceName, spec.getPorts(), logFile);
            } else if (!process.isAlive()) {
                log.warn("PulseLive local service exited before ready: {}, exitCode={}, log={}",
                        serviceName, process.exitValue(), logFile);
            } else {
                log.warn("PulseLive local service was started but is not ready yet: {}, ports={}, log={}",
                        serviceName, spec.getPorts(), logFile);
            }
        } catch (IOException exception) {
            log.warn("Failed to start PulseLive local service: {}", serviceName, exception);
        }
    }

    private ServiceSpec getServiceSpec(String serviceName) {
        if ("local-live".equals(serviceName)) {
            return new ServiceSpec(serviceName, Arrays.asList(
                    getIntProperty("pulselive.local-live-rtmp-port", "PULSELIVE_LOCAL_LIVE_RTMP_PORT", 1935),
                    getIntProperty("pulselive.local-live-http-port", "PULSELIVE_LOCAL_LIVE_HTTP_PORT", 8080)
            ));
        }

        if ("maxine-denoise".equals(serviceName)) {
            return new ServiceSpec(serviceName, Collections.singletonList(
                    getIntProperty("pulselive.denoise-port", "PULSELIVE_DENOISE_PORT", 18765)
            ));
        }

        if ("live-guard".equals(serviceName)) {
            return new ServiceSpec(serviceName, Collections.singletonList(
                    getIntProperty("pulselive.guard-port", "PULSELIVE_GUARD_PORT", 8000)
            ));
        }

        if ("live-caption".equals(serviceName)) {
            return new ServiceSpec(serviceName, Collections.singletonList(
                    getIntProperty("pulselive.caption-port", "PULSELIVE_CAPTION_PORT", 8200)
            ));
        }

        return null;
    }

    private ProcessBuilder createServiceProcess(ServiceSpec spec) {
        if ("local-live".equals(spec.getName())) {
            Path localLiveDir = projectRoot.resolve("local-services").resolve("live-server");
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "npm", "start");
            builder.directory(localLiveDir.toFile());
            builder.environment().put("RTMP_PORT", String.valueOf(spec.getPorts().get(0)));
            builder.environment().put("HTTP_PORT", String.valueOf(spec.getPorts().get(1)));
            return builder;
        }

        if ("maxine-denoise".equals(spec.getName())) {
            ProcessBuilder builder = new ProcessBuilder(
                    resolvePython().toString(),
                    projectRoot.resolve("ai-services").resolve("deepfilternet3").resolve("server").resolve("server.py").toString(),
                    "--port",
                    String.valueOf(spec.getPorts().get(0))
            );
            builder.directory(projectRoot.toFile());
            return builder;
        }

        if ("live-guard".equals(spec.getName())) {
            Path guardDir = projectRoot.resolve("ai-services").resolve("vision-guard").resolve("server");
            ProcessBuilder builder = new ProcessBuilder(resolvePython().toString(), "vision_guard.py");
            builder.directory(guardDir.toFile());
            builder.environment().put("YOLO_CONFIG_DIR", guardDir.resolve(".ultralytics").toString());
            return builder;
        }

        if ("live-caption".equals(spec.getName())) {
            Path captionDir = projectRoot.resolve("ai-services").resolve("live-agent");
            ProcessBuilder builder = new ProcessBuilder(
                    resolvePython().toString(),
                    "stt_server.py",
                    "--port",
                    String.valueOf(spec.getPorts().get(0))
            );
            builder.directory(captionDir.toFile());
            builder.environment().put("PULSELIVE_WHISPER_MODEL",
                    getStringProperty("pulselive.whisper-model", "PULSELIVE_WHISPER_MODEL", "tiny"));
            return builder;
        }

        return null;
    }

    private Path resolvePython() {
        Path venvPython = projectRoot.resolve("ai-services").resolve("vision-guard").resolve("server")
                .resolve("venv").resolve("Scripts").resolve("python.exe");
        if (Files.exists(venvPython)) {
            return venvPython;
        }

        Path projectPython = projectRoot.resolve(".pythonlibs").resolve("Scripts").resolve("python.exe");
        if (Files.exists(projectPython)) {
            return projectPython;
        }

        return Paths.get("python");
    }

    private boolean isServiceAvailable(ServiceSpec spec) {
        for (Integer port : spec.getPorts()) {
            if (!isPortOpen(port)) {
                return false;
            }
        }
        return true;
    }

    private boolean waitForService(ServiceSpec spec, Process process) {
        long deadline = System.currentTimeMillis() + getIntProperty(
                "pulselive.local-services.ready-timeout-seconds",
                "PULSELIVE_LOCAL_SERVICES_READY_TIMEOUT_SECONDS",
                90
        ) * 1000L;

        while (System.currentTimeMillis() < deadline) {
            if (isServiceAvailable(spec)) {
                return true;
            }
            if (!process.isAlive()) {
                return false;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean isPortOpen(Integer port) {
        if (port == null || port <= 0) {
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 300);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private int getIntProperty(String propertyName, String envName, int defaultValue) {
        Integer value = environment.getProperty(propertyName, Integer.class);
        if (value != null) {
            return value;
        }

        value = environment.getProperty(envName, Integer.class);
        return value != null ? value : defaultValue;
    }

    private String getStringProperty(String propertyName, String envName, String defaultValue) {
        String value = environment.getProperty(propertyName);
        if (StringUtils.hasText(value)) {
            return value;
        }

        value = environment.getProperty(envName);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private void stopPort(Integer port) {
        if (port == null || port <= 0) {
            return;
        }

        String command = "for /f \"tokens=5\" %A in ('netstat -ano ^| findstr /R /C:\":" + port
                + " .*LISTENING\"') do @if not \"%A\"==\"0\" taskkill /PID %A /F >nul 2>nul";
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", command).start();
            int exitCode = process.waitFor();
            log.info("PulseLive local service port cleanup finished: port={}, exitCode={}", port, exitCode);
        } catch (IOException exception) {
            log.warn("Failed to cleanup PulseLive local service port: {}", port, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while cleaning PulseLive local service port: {}", port, exception);
        }
    }

    private Path resolveProjectRoot() {
        if (StringUtils.hasText(properties.getProjectRoot())) {
            Path configuredRoot = Paths.get(properties.getProjectRoot()).toAbsolutePath().normalize();
            return Files.exists(configuredRoot) ? configuredRoot : null;
        }

        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.exists(candidate.resolve("local-services").resolve("live-server").resolve("package.json"))
                    && Files.exists(candidate.resolve("ai-services").resolve("deepfilternet3").resolve("server").resolve("server.py"))
                    && Files.exists(candidate.resolve("ai-services").resolve("vision-guard").resolve("server").resolve("vision_guard.py"))) {
                return candidate;
            }
        }
        return null;
    }

    private Path redirectLog(ProcessBuilder builder, Path root, String scriptName) throws IOException {
        Path logDir = root.resolve(".runlogs").resolve("backend-services");
        Files.createDirectories(logDir);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String safeName = scriptName.replaceAll("[^a-zA-Z0-9._-]", "_");
        File logFile = logDir.resolve(safeName + "-" + timestamp + ".log").toFile();
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        return logFile.toPath();
    }

    private static class ServiceSpec {

        private final String name;
        private final List<Integer> ports;

        private ServiceSpec(String name, List<Integer> ports) {
            this.name = name;
            this.ports = ports;
        }

        private String getName() {
            return name;
        }

        private List<Integer> getPorts() {
            return ports;
        }
    }
}
