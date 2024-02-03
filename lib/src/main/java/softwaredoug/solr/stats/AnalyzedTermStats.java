package softwaredoug.solr.stats;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Map;

// TermStats read at the FieldType level before a field is created
public class AnalyzedTermStats {

    private BytesRef analyzedTerm;
    private String unanalyzedTerm;
    private String field;
    private long docFreq;
    private long totalTermFreq;

    private Overrides.AnalysisOption analysisOption;

    AnalyzedTermStats(String field, String term, long docFreq, long totalTermFreq,
                      Overrides.AnalysisOption analysisOption) {
        this.unanalyzedTerm = term;
        this.field = field;
        this.docFreq = docFreq;
        this.totalTermFreq = totalTermFreq;
        this.field = field;
        this.analysisOption = analysisOption;
    }

    public Term getTerm(String field, Map<Overrides.AnalysisOption, Analyzer> options) {
        if (!field.equals(this.field)) {
            return null;
        }
        if (this.analyzedTerm != null) {
            return new Term(field, this.analyzedTerm);
        }

        Analyzer analyzer = options.get(this.analysisOption);

        if (this.analysisOption == Overrides.AnalysisOption.RAW || analyzer == null) {
            this.analyzedTerm = new BytesRef(this.unanalyzedTerm);
            return new Term(field, this.analyzedTerm);
        }

        try (TokenStream source = analyzer.tokenStream(field, this.unanalyzedTerm)) {
            source.reset();
            TermToBytesRefAttribute termAtt = source.getAttribute(TermToBytesRefAttribute.class);
            if (!source.incrementToken()) {
                return null;
            }
            BytesRef termBytes = BytesRef.deepCopyOf(termAtt.getBytesRef());

            if (source.incrementToken()) {
                return null;
            }
            this.analyzedTerm = termBytes;
            return new Term(field, this.analyzedTerm);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getField() {
        return this.field;
    }

    public TermStatistics getStats(String field, Map<Overrides.AnalysisOption, Analyzer> options) {
        Term term = this.getTerm(field, options);
        if (term != null) {
            return new TermStatistics(term.bytes(), this.docFreq, this.totalTermFreq);
        }
        return null;
    }

}