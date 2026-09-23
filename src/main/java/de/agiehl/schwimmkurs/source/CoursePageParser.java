package de.agiehl.schwimmkurs.source;

import de.agiehl.schwimmkurs.domain.CourseOffer;
import de.agiehl.schwimmkurs.domain.OfferField;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class CoursePageParser {

    public List<CourseOffer> parse(String html, URI sourceUrl, String tableSelector) {
        var document = Jsoup.parse(html, sourceUrl.toString());
        var table = document.selectFirst(tableSelector);
        if (table == null) {
            throw new IllegalStateException("Die erwartete Kurstabelle wurde nicht gefunden: " + tableSelector);
        }

        var headings = table.select("thead tr").stream()
                .map(row -> directChildren(row, "th"))
                .filter(cells -> !cells.isEmpty())
                .findFirst()
                .orElseGet(List::of)
                .stream()
                .map(Element::text)
                .map(CoursePageParser::normalize)
                .toList();

        return table.select("tbody > tr").stream()
                .map(row -> toOffer(row, headings))
                .flatMap(List::stream)
                .toList();
    }

    private List<CourseOffer> toOffer(Element row, List<String> headings) {
        var cells = directChildren(row, "td");
        if (cells.size() <= 1 || cells.stream().anyMatch(cell -> cell.hasAttr("colspan"))) {
            return List.of();
        }

        var fields = new ArrayList<OfferField>();
        for (int index = 0; index < cells.size(); index++) {
            var cell = cells.get(index);
            var label = normalize(cell.attr("data-title"));
            if (label.isBlank() && index < headings.size()) {
                label = headings.get(index);
            }
            if (label.isBlank()) {
                label = "Spalte " + (index + 1);
            }
            fields.add(new OfferField(label, normalize(cell.text())));
        }
        return List.of(new CourseOffer(fields));
    }

    private static List<Element> directChildren(Element parent, String tagName) {
        return parent.children().stream()
                .filter(child -> child.tagName().equals(tagName))
                .toList();
    }

    private static String normalize(String value) {
        return value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }
}
