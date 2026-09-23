package de.agiehl.schwimmkurs.service;

import de.agiehl.schwimmkurs.state.RunLock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MonitorRunner implements ApplicationRunner {

    private final MonitorService monitorService;
    private final RunLock runLock;

    public MonitorRunner(MonitorService monitorService, RunLock runLock) {
        this.monitorService = monitorService;
        this.runLock = runLock;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (var ignored = runLock.acquire()) {
            monitorService.check();
        }
    }
}
