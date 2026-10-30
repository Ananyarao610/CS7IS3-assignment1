package cranfield;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CranfieldIndexer {

    public static void main(String[] args) throws IOException {

        String analyzerName = args.length >= 1
                ? args[0].toUpperCase()
                : "STOP";

        Path dataDirectory = Path.of("data");
        Path indexPath = Path.of("index", analyzerName);

        List<CranfieldParser.CranfieldDocument> documents =
                CranfieldParser.parseDocuments(
                        dataDirectory.resolve("cran.all.1400"));

        System.out.println("Analyzer: " + analyzerName);
        System.out.println("Documents loaded: " + documents.size());

        try (Analyzer analyzer =
                     CranfieldAnalyzers.create(analyzerName);
             Directory directory = FSDirectory.open(indexPath)) {

            IndexWriterConfig config =
                    new IndexWriterConfig(analyzer);

            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try (IndexWriter writer =
                         new IndexWriter(directory, config)) {

                for (CranfieldParser.CranfieldDocument cranfieldDocument
                        : documents) {

                    Document luceneDocument = new Document();

                    luceneDocument.add(new StringField(
                            "id",
                            cranfieldDocument.id,
                            Field.Store.YES));

                    luceneDocument.add(new TextField(
                            "title",
                            cranfieldDocument.title,
                            Field.Store.YES));

                    luceneDocument.add(new TextField(
                            "contents",
                            cranfieldDocument.getSearchableText(),
                            Field.Store.YES));

                    luceneDocument.add(new StoredField(
                            "author",
                            cranfieldDocument.author));

                    luceneDocument.add(new StoredField(
                            "publication",
                            cranfieldDocument.publication));

                    writer.addDocument(luceneDocument);
                }

                writer.commit();
            }

            try (IndexReader reader =
                         org.apache.lucene.index.DirectoryReader.open(
                                 directory)) {

                System.out.println(
                        "Documents indexed: " + reader.numDocs());
            }
        }

        System.out.println("Indexing completed successfully.");
        System.out.println("Index location: " + indexPath);
    }
}
