package com.pgs.assistant.knowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a markdown document into sections at "## " headings. Retrieval works better on
 * self-contained sections than on arbitrary fixed-size windows, and each chunk keeps a
 * human-readable citation (document title + section heading).
 */
public final class MarkdownSections {

    public record Section(String title, String heading, String text) {
    }

    private MarkdownSections() {
    }

    public static List<Section> split(String markdown) {
        String title = "Untitled";
        List<Section> sections = new ArrayList<>();
        String heading = null;
        StringBuilder body = new StringBuilder();

        for (String line : markdown.split("\\R")) {
            if (line.startsWith("# ")) {
                title = line.substring(2).trim();
            } else if (line.startsWith("## ")) {
                add(sections, title, heading, body);
                heading = line.substring(3).trim();
                body.setLength(0);
            } else {
                body.append(line).append('\n');
            }
        }
        add(sections, title, heading, body);
        return sections;
    }

    private static void add(List<Section> sections, String title, String heading, StringBuilder body) {
        String text = body.toString().strip();
        if (!text.isEmpty()) {
            sections.add(new Section(title, heading == null ? "Overview" : heading, text));
        }
    }
}
