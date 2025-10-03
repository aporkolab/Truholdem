package com.truholdem.listener;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PortFileWriter implements ApplicationListener<WebServerInitializedEvent> {

    private static final String PORT_FILE = "../.backend-port";

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        Path portFile = Paths.get(PORT_FILE);

        try {
            Files.writeString(portFile, String.valueOf(port));
            portFile.toFile().deleteOnExit();
            System.out.println("Backend running on port " + port + " (written to " + portFile.toAbsolutePath() + ")");
        } catch (IOException e) {
            System.err.println("Could not write port file: " + e.getMessage());
        }
    }
}
