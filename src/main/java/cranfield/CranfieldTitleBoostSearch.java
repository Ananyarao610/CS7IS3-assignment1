
package cranfield;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CranfieldTitleBoostSearch {

    public static void main(String[] args) throws Exception {

        String analyzerName = "STEM";
        float titleBoost = 2.0f;

        Path dataDirectory = Path.of("data");
        Path indexPath = Path.of("index", analyzerName);
        Path resultsDirectory = Path.of("results");

        Files.createDirectories(resultsDirectory);

        List<CranfieldParser.CranfieldQuery> queries =
                CranfieldParser.parseQueries(
                        dataDirectory.resolve("cran.qry"));

        String runName = "STEM_BM25_TITLE_BOOST_2";

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

            searcher.setSimilarity(new BM25Similarity());

            String[] fields = {"title", "contents"};

            Map<String, Float> boosts =
                    Map.of("title", titleBoost);

            MultiFieldQueryParser parser =
                    new MultiFieldQueryParser(fields, analyzer, boosts);

            for (CranfieldParser.CranfieldQuery cranfieldQuery
                    : queries) {

                Query query = parser.parse(
                        MultiFieldQueryParser.escape(
                                cranfieldQuery.text));

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
        System.out.println("Ranking model: BM25");
        System.out.println("Title boost: " + titleBoost);
        System.out.println("Queries processed: " + queries.size());
        System.out.println("Total results written: " + totalResults);
        System.out.println("Output file: " + outputFile);
        System.out.println("Title-boost search completed successfully.");
    }
}

