package aforo.kong.scheduler;

import aforo.kong.service.KonnectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KonnectAutoRefreshScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KonnectAutoRefreshScheduler.class);

    private final KonnectService konnectService;

    public KonnectAutoRefreshScheduler(KonnectService konnectService) {
        this.konnectService = konnectService;
    }

    @Scheduled(fixedDelay = 120000)
    public void autoRefreshKonnectProducts() {
        logger.info("Starting Konnect auto-refresh job");
        try {
            konnectService.autoRefresh();
            logger.info("Completed Konnect auto-refresh job");
        } catch (Exception e) {
            logger.error("Error during Konnect auto-refresh", e);
        }
    }
}
