package cranfield;

import java.nio.file.Path;

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

public class CranfieldSearch {

    public static void main(String[] args) throws Exception {

        String analyzerName = args.length >= 1
                ? args[0].toUpperCase()
                : "STOP";

        String model = args.length >= 2
                ? args[1].toUpperCase()
                : "BM25";

        String searchText = args.length >= 3
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                : "what similarity laws must be obeyed when constructing "
                  + "aeroelastic models of heated high speed aircraft";

        Path indexPath = Path.of("index", analyzerName);

        try (Analyzer analyzer =
                     CranfieldAnalyzers.create(analyzerName);
             Directory directory = FSDirectory.open(indexPath);
             DirectoryReader reader = DirectoryReader.open(directory)) {

            IndexSearcher searcher = new IndexSearcher(reader);

            if (model.equals("VSM")) {
                searcher.setSimilarity(new ClassicSimilarity());
            } else if (model.equals("BM25")) {
                searcher.setSimilarity(new BM25Similarity());
            } else {
                throw new IllegalArgumentException(
                        "Unknown model. Use VSM or BM25.");
            }

            QueryParser parser =
                    new QueryParser("contents", analyzer);

            Query query =
                    parser.parse(QueryParser.escape(searchText));

            TopDocs results = searcher.search(query, 10);

            System.out.println("Analyzer: " + analyzerName);
            System.out.println("Ranking model: " + model);
            System.out.println("Search query: " + searchText);
            System.out.println("Results found: " + results.totalHits.value);

            System.out.println("\n===== TOP 10 RESULTS =====");

            int rank = 1;

            for (ScoreDoc scoreDoc : results.scoreDocs) {

                Document document =
                        searcher.storedFields().document(scoreDoc.doc);

                System.out.println("\nRank: " + rank);
                System.out.println("Document ID: " + document.get("id"));
                System.out.println("Score: " + scoreDoc.score);
                System.out.println("Title: " + document.get("title"));

                rank++;
            }
        }
    }
}
