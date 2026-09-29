package com.mizal.pgs.assistant.knowledge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSectionsTest {

    @Test
    void splitsOnSecondLevelHeadingsAndKeepsTheTitle() {
        var sections = MarkdownSections.split("""
                # Decline reasons

                Intro text.

                ## card_declined
                The bank said no.

                ## insufficient_funds
                Not enough money.
                """);

        assertThat(sections).hasSize(3);
        assertThat(sections.get(0).heading()).isEqualTo("Overview");
        assertThat(sections.get(1).title()).isEqualTo("Decline reasons");
        assertThat(sections.get(1).heading()).isEqualTo("card_declined");
        assertThat(sections.get(2).text()).isEqualTo("Not enough money.");
    }

    @Test
    void skipsEmptySections() {
        assertThat(MarkdownSections.split("# Title\n## Empty\n\n## Filled\ntext")).hasSize(1);
    }
}
