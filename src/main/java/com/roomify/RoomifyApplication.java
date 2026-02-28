package com.roomify;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.roomify.shared.exception.TechniqueApiException;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class RoomifyApplication {

    private static final Logger LOG = LoggerFactory.getLogger(RoomifyApplication.class);

    /**
     * Main
     *
     * @param args String[]
     * @throws TechniqueApiException exception générale
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RoomifyApplication.class);
        Environment env = app.run(args).getEnvironment();

        String port = env.getProperty("server.port");
        String appName = env.getProperty("spring.application.name");
        try {
            LOG.info(
                    """
                            ----------------------------------------------------------
                            \t\
                            Application '{}' is running! Access URLs:
                            \t\
                            Local: \t\thttp://localhost:{}
                            \t\
                            External: \thttp://{}:{}
                            ----------------------------------------------------------""",
                    appName,
                    port,
                    InetAddress.getLocalHost().getHostAddress(),
                    port);
        } catch (UnknownHostException u) {
            throw new TechniqueApiException("Impossible de démarrer l'application", u);
        }
    }
}
