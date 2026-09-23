package de.agiehl.schwimmkurs.source;

import de.agiehl.schwimmkurs.config.SchwimmkursProperties;
import de.agiehl.schwimmkurs.domain.CourseOffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;

@Component
public class CoursePageClient {

    private final SchwimmkursProperties properties;
    private final CoursePageParser parser;
    private final RestClient restClient;

    public CoursePageClient(SchwimmkursProperties properties, CoursePageParser parser) {
        this.properties = properties;
        this.parser = parser;

        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.source().connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.source().readTimeout());
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.source().userAgent())
                .build();
    }

    public List<CourseOffer> fetchOffers() {
        var html = restClient.get()
                .uri(properties.source().url())
                .retrieve()
                .body(String.class);
        if (html == null || html.isBlank()) {
            throw new IllegalStateException("Die Kursseite hat keinen Inhalt geliefert.");
        }
        return parser.parse(html, properties.source().url(), properties.source().tableSelector());
    }
}
