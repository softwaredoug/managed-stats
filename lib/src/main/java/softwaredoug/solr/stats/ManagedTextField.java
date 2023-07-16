package softwaredoug.solr.stats;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.apache.solr.search.stats.CollectionStats;
import org.apache.solr.search.stats.TermStats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ManagedTextField extends TextField implements ResourceLoaderAware {

    private String statsFile;
    private long docCount;
    private long maxDocs;
    private long sumTotalTermFreq;
    private long sumTotalDocFreq;

    private List<AnalyzedTermStats> termStats;  // field -> termStats

    @Override
    protected void init(IndexSchema schema, Map<String, String> args) {
        this.statsFile = args.remove("stats");
        this.termStats = new ArrayList<AnalyzedTermStats>();

        super.init(schema, args);
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        final SolrResourceLoader solrResourceLoader = (SolrResourceLoader) loader;
        String typeName = getTypeName();
        Map<String, String> config = getNonFieldPropertyArgs();

        List<String> lines = solrResourceLoader.getLines(this.statsFile);
        String globalStats = lines.remove(0);
        String[] statsHeader = globalStats.split(",");
        if (statsHeader.length != 4) {
            throw new IllegalArgumentException("First line of stats should provide 4 total stats");
        }
        long[] stats = new long[4];
        int idx = 0;
        for (String globalStat : statsHeader) {
            stats[idx] = Long.parseLong(globalStat);
            idx++;
        }
        this.docCount = stats[0];
        this.maxDocs = stats[1];
        this.sumTotalTermFreq = stats[2];
        this.sumTotalDocFreq = stats[3];

        // Now for individual terms
        String unanalyzedTerm = null;
        long docFreq = 0;
        long totalTermFreq = 0;
        for (String line : lines) {
            String[] line_split = line.split(",");
            unanalyzedTerm = line_split[0];
            docFreq = Long.parseLong(line_split[1]);
            totalTermFreq = Long.parseLong(line_split[2]);
            this.termStats.add(new AnalyzedTermStats(unanalyzedTerm, docFreq, totalTermFreq));
        }
    }

    public TermStatistics termStatistics(Term term) {
        // slow for correctness, very bad to do this here
        for (AnalyzedTermStats stats: this.termStats) {
            TermStatistics thisStat = stats.getStats(term.field(), this.getIndexAnalyzer());
            if (thisStat.term().equals(term)) {
                return thisStat;
            }
        }
        return null;
    }

    public CollectionStatistics collectionStatistics(String field) {
        return new CollectionStatistics(field, this.maxDocs, this.docCount, this.sumTotalTermFreq, this.sumTotalDocFreq);
    }

    @Override
    public IndexableField createField(SchemaField field, Object value) {
        return super.createField(field, value);
    }

    // TermStats read at the FieldType level before a field is created
    public static class AnalyzedTermStats {

        private String analyzedTerm;
        private String unanalyzedTerm;
        private long docFreq;
        private long totalTermFreq;

        AnalyzedTermStats(String term, long docFreq, long totalTermFreq) {
            this.unanalyzedTerm = term;
            this.docFreq = docFreq;
            this.totalTermFreq = totalTermFreq;
        }

        public Term getTerm(String field, Analyzer indexAnalyzer) {
            if (this.analyzedTerm != null) {
                return new Term(field, this.analyzedTerm);
            }
            else {
                try (TokenStream source = indexAnalyzer.tokenStream(field, this.unanalyzedTerm)) {
                    source.reset();
                    TermToBytesRefAttribute termAtt = source.getAttribute(TermToBytesRefAttribute.class);
                    if (!source.incrementToken()) {
                        return null;
                    }
                    BytesRef termBytes = BytesRef.deepCopyOf(termAtt.getBytesRef());

                    if (source.incrementToken()) {
                        return null;
                    }
                    this.analyzedTerm = termBytes.toString();
                    return new Term(field, this.analyzedTerm);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        };

        public TermStatistics getStats(String field, Analyzer indexAnalyzer) {
            Term term = this.getTerm(field, indexAnalyzer);
            if (term != null) {
                return new TermStatistics(term.bytes(), this.docFreq, this.totalTermFreq);
            }
            return null;
        }
    }
}
