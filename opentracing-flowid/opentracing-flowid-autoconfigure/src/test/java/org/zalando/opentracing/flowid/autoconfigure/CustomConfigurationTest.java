package org.zalando.opentracing.flowid.autoconfigure;

import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zalando.opentracing.flowid.httpclient.FlowHttpRequestInterceptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("custom")
@EnableAutoConfiguration
class CustomConfigurationTest {

    @Configuration
    static class TestConfiguration {

        @Bean
        public Tracer tracer() {
            return new MockTracer();
        }

    }

    @Autowired(required = false)
    @Qualifier("flowFilter")
    private FilterRegistrationBean filter;

    @Autowired(required = false)
    private FlowHttpRequestInterceptor interceptor;

    @Test
    void doesntConfigureFilter() {
        assertThat(filter, is(nullValue()));
    }

    @Test
    void doesntConfigureInterceptor() {
        assertThat(interceptor, is(nullValue()));
    }

}
