package cranfield;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CranfieldBatchSearch {

    public static void main(String[] args) throws Exception {

        String analyzerName = args[0].toUpperCase();
        String model = args[1].toUpperCase();

        Path dataDirectory = Path.of("data");
        Path indexPath = Path.of("index", analyzerName);
        Path resultsDirectory = Path.of("results");

        Files.createDirectories(resultsDirectory);

        List<CranfieldParser.CranfieldQuery> queries =
                CranfieldParser.parseQueries(
                        dataDirectory.resolve("cran.qry"));

        String runName = analyzerName + "_" + model;

        Path outputFile =
                resultsDirectory.resolve(runName + ".txt");

        int totalResults = 0;

        try (Analyzer analyzer =
                     CranfieldAnalyzers.create(analyzerName);
             Directory directory = FSDirectory.open(indexPath);
             DirectoryReader reader = DirectoryReader.open(directory);
             BufferedWriter writer = Files.newBufferedWriter(
                     outputFile, StandardCharsets.UTF_8)) {

            IndexSearcher searcher = new IndexSearcher(reader);

            if (model.equals("VSM")) {
                searcher.setSimilarity(new ClassicSimilarity());
            } else if (model.equals("BM25")) {
                searcher.setSimilarity(new BM25Similarity());
            } else {
                throw new IllegalArgumentException(
                        "Model must be VSM or BM25.");
            }

            QueryParser parser =
                    new QueryParser("contents", analyzer);

            for (CranfieldParser.CranfieldQuery cranfieldQuery
                    : queries) {

                Query query = parser.parse(
                        QueryParser.escape(cranfieldQuery.text));

                TopDocs topDocs = searcher.search(query, 1000);

                int rank = 1;

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {

                    Document document =
                            searcher.storedFields().document(scoreDoc.doc);

                    String documentId = document.get("id");

                    writer.write(
                            cranfieldQuery.id + " Q0 "
                            + documentId + " "
                            + rank + " "
                            + scoreDoc.score + " "
                            + runName
                    );

                    writer.newLine();

                    rank++;
                    totalResults++;
                }
            }
        }

        System.out.println("Analyzer: " + analyzerName);
        System.out.println("Ranking model: " + model);
        System.out.println("Queries processed: " + queries.size());
        System.out.println("Total results written: " + totalResults);
        System.out.println("Output file: " + outputFile);
        System.out.println("Batch search completed successfully.");
    }
}
