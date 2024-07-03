//
// Copyright (c)2015, dblp Team (University of Trier and
// Schloss Dagstuhl - Leibniz-Zentrum fuer Informatik GmbH)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// (1) Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//
// (2) Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// (3) Neither the name of the dblp team nor the names of its contributors
// may be used to endorse or promote products derived from this software
// without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DBLP TEAM BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.dblp.mmdb.Field;
import org.dblp.mmdb.Person;
import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.RecordDb;
import org.dblp.mmdb.RecordDbInterface;
import org.dblp.mmdb.TableOfContents;
import org.xml.sax.SAXException;


@SuppressWarnings("javadoc")
class DblpExampleParser {

    public static void main(String[] args) {

        // we need to raise entityExpansionLimit because the dblp.xml has millions of entities
        System.setProperty("entityExpansionLimit", "10000000");

        if (args.length != 2) {
            System.err.format("Usage: java %s <dblp-xml-file> <dblp-dtd-file>\n", DblpExampleParser.class.getName());
            System.exit(0);
        }
        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];

        System.out.println("building the dblp main memory DB ...");
        RecordDbInterface dblp;
        try {
            dblp = new RecordDb(dblpXmlFilename, dblpDtdFilename, false);
        } catch (final IOException ex) {
            System.err.println("cannot read dblp XML: " + ex.getMessage());
            return;
        } catch (final SAXException ex) {
            System.err.println("cannot parse XML: " + ex.getMessage());
            return;
        }
        System.out.format("MMDB ready: %d publs, %d pers\n\n", dblp.numberOfPublications(), dblp.numberOfPersons());

        findMcts(dblp);

    }

    private static void findMcts(RecordDbInterface dblp) {
        Map<String, Integer> yearCountMap = new HashMap<>();

        try (FileWriter writer = new FileWriter("MCTS_articles.csv")) {
            // Write the CSV header
            writer.append("Title,Authors,Year\n");

            // Iterate over all publications
            for (Publication publ : dblp.getPublications()) {
                if (publ.getFields("title").isEmpty()) {
                    System.out.println("No title field found for publication");
                    continue;
                }
                Optional<Field> field = publ.getFields("title").stream().findFirst();
                if (field.isEmpty()) {
                    System.out.println("Title field empty for publication");
                    continue;
                }
                String title = field.get().value();
                // Check if the title contains "MCTS" (case insensitive)
                if (title != null && title.toLowerCase().contains("mcts")) {
                    // Retrieve the authors
                    StringBuilder authors = new StringBuilder();
                    Collection<PersonName> authorList = publ.getNames();
                    for (PersonName author : authorList) {
                        authors.append(author.getPrimaryName().name()).append(";");
                    }
                    // Remove the last semicolon
                    if (authors.length() > 0) {
                        authors.setLength(authors.length() - 1);
                    }

                    // Retrieve the year
                    String year = String.valueOf(publ.getYear());
                    yearCountMap.put(year, yearCountMap.getOrDefault(year, 0) + 1);
                    // Write the details to the CSV file
                    writer.append("\"").append(title).append("\",\"").append(authors.toString()).append("\",")
                            .append(year).append("\n");
                }
            }
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the CSV file: " + e.getMessage());
        }

        try (FileWriter writer = new FileWriter("MCTS_year_counts.csv")) {
            // Write the CSV header
            writer.append("Year,Count\n");

            // Write the year counts
            for (Map.Entry<String, Integer> entry : yearCountMap.entrySet()) {
                writer.append(entry.getKey()).append(",").append(entry.getValue().toString()).append("\n");
            }
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the CSV file: " + e.getMessage());
        }
    }
}
