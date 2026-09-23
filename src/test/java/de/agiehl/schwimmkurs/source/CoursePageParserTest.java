package de.agiehl.schwimmkurs.source;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoursePageParserTest {

    private final CoursePageParser parser = new CoursePageParser();
    private final URI sourceUrl = URI.create("https://example.org/courses");

    @Test
    void parsesCourseRowsAndNormalizesWhitespace() {
        var html = """
                <table id="courses">
                  <thead><tr><th>Termine ab</th><th>Freie Plätze</th><th>Buchungsoption</th></tr></thead>
                  <tbody>
                    <tr><td data-title="Termine ab"> 01.10.2026 </td><td data-title="Freie Plätze"> 2&nbsp;Plätze </td><td data-title="Buchungsoption">Buchen</td></tr>
                  </tbody>
                </table>
                """;

        var offers = parser.parse(html, sourceUrl, "#courses");

        assertThat(offers).singleElement().satisfies(offer -> {
            assertThat(offer.fields()).hasSize(3);
            assertThat(offer.readableDescription()).isEqualTo(
                    "Termine ab: 01.10.2026 | Freie Plätze: 2 Plätze | Buchungsoption: Buchen"
            );
        });
    }

    @Test
    void ignoresEmptyMessageRows() {
        var html = """
                <table id="courses"><tbody><tr><td colspan="9">Leider keine Termine.</td></tr></tbody></table>
                """;

        assertThat(parser.parse(html, sourceUrl, "#courses")).isEmpty();
    }

    @Test
    void rejectsUnexpectedPages() {
        assertThatThrownBy(() -> parser.parse("<html><body>Fehler</body></html>", sourceUrl, "#courses"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kurstabelle");
    }
}
