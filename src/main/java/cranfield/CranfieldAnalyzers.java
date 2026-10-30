package cranfield;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

public class CranfieldAnalyzers {

    public static Analyzer create(String name) {

        switch (name.toUpperCase()) {

            case "NO_STOP":
                return new StandardAnalyzer(CharArraySet.EMPTY_SET);

            case "STOP":
                return new StandardAnalyzer();

            case "STEM":
                return new EnglishAnalyzer();

            default:
                throw new IllegalArgumentException(
                        "Unknown analyzer. Use NO_STOP, STOP or STEM.");
        }
    }
}
