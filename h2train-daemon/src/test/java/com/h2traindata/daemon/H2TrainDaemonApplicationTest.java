package com.h2traindata.daemon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {
        "app.persistence.type=memory",
        "app.bus.type=logging",
        "spring.datasource.url=jdbc:h2:mem:h2train-daemon-test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "app.strava.client-id=12345",
        "app.strava.client-secret=test-secret",
        "app.strava.redirect-uri=http://localhost:8080/auth/strava/callback",
        "app.fitbit.enabled=false",
        "app.sync.poll-interval-ms=3600000"
})
class H2TrainDaemonApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void startsWithoutServletWebServer() {
        assertThat(applicationContext.containsBean("dispatcherServlet")).isFalse();
    }
}
