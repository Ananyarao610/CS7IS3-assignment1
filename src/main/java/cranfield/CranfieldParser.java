
package cranfield;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CranfieldParser {

    // Represents one document from the Cranfield collection.
    public static class CranfieldDocument {
        public String id;
        public String title = "";
        public String author = "";
        public String publication = "";
        public String abstractText = "";

        // Combines title and abstract for searching.
        public String getSearchableText() {
            return title + " " + abstractText;
        }
    }

    // Represents one search query.
    public static class CranfieldQuery {
        public String id;
        public String text;

        public CranfieldQuery(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    // Represents one relevance judgement.
    public static class RelevanceJudgement {
        public String queryId;
        public String documentId;
        public int relevanceGrade;

        public RelevanceJudgement(
                String queryId,
                String documentId,
                int relevanceGrade) {

            this.queryId = queryId;
            this.documentId = documentId;
            this.relevanceGrade = relevanceGrade;
        }
    }

    // Normalises IDs so that 001 and 1 are treated as the same ID.
    private static String normaliseId(String id) {
        return Integer.toString(Integer.parseInt(id.trim()));
    }

    // Reads all documents from cran.all.1400.
    public static List<CranfieldDocument> parseDocuments(Path file)
            throws IOException {

        List<CranfieldDocument> documents = new ArrayList<>();
        List<String> lines = Files.readAllLines(
                file, StandardCharsets.UTF_8);

        CranfieldDocument currentDocument = null;
        String currentSection = "";

        for (String line : lines) {

            // Each new document begins with .I.
            if (line.startsWith(".I ")) {

                if (currentDocument != null) {
                    documents.add(currentDocument);
                }

                currentDocument = new CranfieldDocument();
                currentDocument.id =
                        normaliseId(line.substring(3));

                currentSection = "";
                continue;
            }

            // Identify the current document section.
            if (line.matches("^\\.[TABW]$")) {
                currentSection = line.substring(1);
                continue;
            }

            if (currentDocument == null ||
                    currentSection.isEmpty()) {
                continue;
            }

            String text = line.trim();

            if (text.isEmpty()) {
                continue;
            }

            // Add the text to the appropriate field.
            switch (currentSection) {
                case "T":
                    currentDocument.title += text + " ";
                    break;

                case "A":
                    currentDocument.author += text + " ";
                    break;

                case "B":
                    currentDocument.publication += text + " ";
                    break;

                case "W":
                    currentDocument.abstractText += text + " ";
                    break;

                default:
                    break;
            }
        }

        // Add the final document.
        if (currentDocument != null) {
            documents.add(currentDocument);
        }

        return documents;
    }

    // Reads all queries from cran.qry.
    public static List<CranfieldQuery> parseQueries(Path file)
            throws IOException {

        List<CranfieldQuery> queries = new ArrayList<>();
        List<String> lines = Files.readAllLines(
                file, StandardCharsets.UTF_8);

        String currentId = null;
        StringBuilder queryText = new StringBuilder();
        boolean readingQuery = false;

        for (String line : lines) {

            // A new query begins with .I.
            if (line.startsWith(".I ")) {

                if (currentId != null) {
                    queries.add(new CranfieldQuery(
                            currentId,
                            queryText.toString().trim()));
                }

                currentId = normaliseId(line.substring(3));
                queryText = new StringBuilder();
                readingQuery = false;
                continue;
            }

            // The query text starts after .W.
            if (line.equals(".W")) {
                readingQuery = true;
                continue;
            }

            if (readingQuery && !line.trim().isEmpty()) {
                queryText.append(line.trim()).append(" ");
            }
        }

        // Add the final query.
        if (currentId != null) {
            queries.add(new CranfieldQuery(
                    currentId,
                    queryText.toString().trim()));
        }

        return queries;
    }

    // Reads relevance judgements from cranqrel.
    public static List<RelevanceJudgement> parseRelevanceJudgements(
            Path file) throws IOException {

        List<RelevanceJudgement> judgements =
                new ArrayList<>();

        List<String> lines = Files.readAllLines(
                file, StandardCharsets.UTF_8);

        for (String line : lines) {

            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split("\\s+");

            if (parts.length < 3) {
                continue;
            }

            judgements.add(new RelevanceJudgement(
                    normaliseId(parts[0]),
                    normaliseId(parts[1]),
                    Integer.parseInt(parts[2])
            ));
        }

        return judgements;
    }

    // Tests all three parsers.
    public static void main(String[] args) throws IOException {

        Path dataDirectory = Path.of("data");

        List<CranfieldDocument> documents =
                parseDocuments(
                        dataDirectory.resolve("cran.all.1400"));

        List<CranfieldQuery> queries =
                parseQueries(
                        dataDirectory.resolve("cran.qry"));

        List<RelevanceJudgement> judgements =
                parseRelevanceJudgements(
                        dataDirectory.resolve("cranqrel"));

        System.out.println("===== PARSER TEST =====");

        System.out.println("Documents loaded: " +
                documents.size());

        System.out.println("Queries loaded: " +
                queries.size());

        System.out.println("Relevance judgements loaded: " +
                judgements.size());

        System.out.println("\n===== FIRST DOCUMENT =====");

        CranfieldDocument firstDocument = documents.get(0);

        System.out.println("ID: " + firstDocument.id);
        System.out.println("Title: " + firstDocument.title);
        System.out.println("Author: " + firstDocument.author);
        System.out.println("Publication: " +
                firstDocument.publication);
        System.out.println("Abstract: " +
                firstDocument.abstractText);

        System.out.println("\n===== FIRST QUERY =====");

        CranfieldQuery firstQuery = queries.get(0);

        System.out.println("ID: " + firstQuery.id);
        System.out.println("Text: " + firstQuery.text);

        System.out.println("\n===== FIRST RELEVANCE JUDGEMENT =====");

        RelevanceJudgement firstJudgement =
                judgements.get(0);

        System.out.println("Query ID: " +
                firstJudgement.queryId);

        System.out.println("Document ID: " +
                firstJudgement.documentId);

        System.out.println("Relevance grade: " +
                firstJudgement.relevanceGrade);
    }
}

