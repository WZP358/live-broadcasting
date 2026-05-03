package cn.imhtb.live.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StartupPortCleaner implements EnvironmentPostProcessor, Ordered {

    private static final int DEFAULT_HTTP_PORT = 9000;
    private static final int DEFAULT_NETTY_WS_PORT = 10022;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("pulselive.startup-port-cleanup.enabled", Boolean.class, true)) {
            return;
        }

        cleanupPort(environment.getProperty("server.port", Integer.class, DEFAULT_HTTP_PORT));
        cleanupPort(DEFAULT_NETTY_WS_PORT);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 30;
    }

    private void cleanupPort(Integer port) {
        if (port == null || port <= 0 || !isWindows()) {
            return;
        }

        String currentPid = getCurrentPid();
        Set<String> pids = findListeningPids(port);
        pids.remove(currentPid);
        for (String pid : pids) {
            if (isPulseLiveProcess(pid)) {
                killProcess(pid, port);
            } else {
                log.warn("PulseLive skip cleanup for non-project process, port={}, pid={}", port, pid);
            }
        }
    }

    private String getCurrentPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int separator = name.indexOf('@');
        return separator > 0 ? name.substring(0, separator) : "";
    }

    private boolean isPulseLiveProcess(String pid) {
        String commandLine = getProcessCommandLine(pid).toLowerCase();
        String userDir = System.getProperty("user.dir", "").toLowerCase();
        return commandLine.contains("cn.imhtb.live.pulseliveapplication")
                || commandLine.contains("cn.imhtb.live.antliveapplication")
                || (userDir.contains("live") && commandLine.contains(userDir));
    }

    private String getProcessCommandLine(String pid) {
        String command = "wmic process where processid=" + pid + " get CommandLine /value";
        StringBuilder output = new StringBuilder();
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException exception) {
            log.warn("PulseLive process command line query failed, pid={}", pid, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("PulseLive process command line query interrupted, pid={}", pid, exception);
        }
        return output.toString();
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private Set<String> findListeningPids(int port) {
        Set<String> pids = new LinkedHashSet<>();
        String command = "netstat -ano -p tcp | findstr /R /C:\":" + port + " .*LISTENING\"";
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 0) {
                        pids.add(parts[parts.length - 1]);
                    }
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException exception) {
            log.warn("PulseLive startup port scan failed, port={}", port, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("PulseLive startup port scan interrupted, port={}", port, exception);
        }
        return pids;
    }

    private void killProcess(String pid, int port) {
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", "taskkill", "/PID", pid, "/F")
                    .redirectErrorStream(true)
                    .start();
            boolean exited = process.waitFor(10, TimeUnit.SECONDS);
            if (exited && process.exitValue() == 0) {
                log.info("PulseLive cleaned stale process before startup, port={}, pid={}", port, pid);
            } else {
                log.warn("PulseLive failed to clean stale process before startup, port={}, pid={}, exited={}",
                        port, pid, exited);
            }
        } catch (IOException exception) {
            log.warn("PulseLive startup port cleanup failed, port={}, pid={}", port, pid, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("PulseLive startup port cleanup interrupted, port={}, pid={}", port, pid, exception);
        }
    }
}
