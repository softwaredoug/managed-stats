package softwaredoug.solr.stats;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;

import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;


import java.io.IOException;
import java.util.*;

public class ManagedTextField extends TextField implements ResourceLoaderAware {

    private String statsFile;
    private boolean override_all_fields;


    private List<AnalyzedTermStats> termStats;  // field -> termStats
    private HashMap<String, CollectionStatistics> fieldStats;

    @Override
    protected void init(IndexSchema schema, Map<String, String> args) {
        this.statsFile = args.remove("stats");
        this.termStats = new ArrayList<AnalyzedTermStats>();
        this.fieldStats = new HashMap<String, CollectionStatistics>();

        this.override_all_fields = false;
        String override = args.get("override");
        if (override != null) {
            this.override_all_fields = Boolean.valueOf(override);
        }

        super.init(schema, args);
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        final SolrResourceLoader solrResourceLoader = (SolrResourceLoader) loader;
        String typeName = getTypeName();
        Map<String, String> config = getNonFieldPropertyArgs();
        String currField = "";
        boolean fields = true;

        List<String> lines = solrResourceLoader.getLines(this.statsFile);
        for (String line : lines) {
            if (line.equals("fields")) {
                fields = true;
                continue;
            }
            if (line.equals("terms")) {
                fields = false;
                continue;
            }

            if (fields) {
                String[] statsHeader = line.split(",");
                if (statsHeader.length != 5) {
                    throw new IllegalArgumentException("Field stats should provide field name plus 4 stats"
                                                       + " you provided: " + line);
                }
                long[] stats = new long[4];
                int idx = 0;
                String fieldName = statsHeader[0];
                for (String globalStat : statsHeader) {
                    if (globalStat == fieldName) {
                        continue;
                    }
                    stats[idx] = Long.parseLong(globalStat);
                    idx++;
                }
                long docCount = stats[0];
                long maxDocs = stats[1];
                long sumTotalTermFreq = stats[2];
                long sumTotalDocFreq = stats[3];

                fieldStats.put(fieldName,
                                new CollectionStatistics(fieldName, maxDocs, docCount, sumTotalTermFreq, sumTotalDocFreq));
            }
            else {
                // Now for individual terms
                String unanalyzedTerm = null;
                String field = null;
                long docFreq = 0;
                long totalTermFreq = 0;
                String[] line_split = line.split(",");

                if (line_split.length < 4) {
                    throw new IllegalArgumentException("Error at line: " + line +
                            ": Managed stat row requires 3 comma separated values: term,docFreq,totalTermFreq");
                }

                field = line_split[0];
                docFreq = Long.parseLong(line_split[line_split.length - 2]);
                totalTermFreq = Long.parseLong(line_split[line_split.length - 1]);

                String[] remainder = Arrays.copyOfRange(line_split, 1, line_split.length - 2);
                unanalyzedTerm = String.join(",", remainder);

                if (docFreq > totalTermFreq) {
                    throw new IllegalArgumentException("Doc stats error at: <" + line + "> -- docFreq more than totalTermFreq not allowed");
                }
                this.termStats.add(new AnalyzedTermStats(field, unanalyzedTerm, docFreq, totalTermFreq));
            }
        }
    }

    public TermStatistics termStatistics(Term term) {
        // slow for correctness, very bad to do this here
        for (AnalyzedTermStats stats: this.termStats) {
            TermStatistics thisStat = stats.getStats(term.field(), this.getIndexAnalyzer());
            if (thisStat != null && thisStat.term().equals(term.bytes()) && term.field().equals(stats.getField())) {
                return thisStat;
            }
        }
        return null;
    }

    public CollectionStatistics collectionStatistics(String field) {
        return fieldStats.get(field);
    }

    @Override
    public IndexableField createField(SchemaField field, Object value) {
        return super.createField(field, value);
    }

    public boolean wants_to_override_all_field_stats() {
        return this.override_all_fields;
    }

    // TermStats read at the FieldType level before a field is created
    public static class AnalyzedTermStats {

        private BytesRef analyzedTerm;
        private String unanalyzedTerm;
        private String field;
        private long docFreq;
        private long totalTermFreq;

        AnalyzedTermStats(String field, String term, long docFreq, long totalTermFreq) {
            this.unanalyzedTerm = term;
            this.docFreq = docFreq;
            this.totalTermFreq = totalTermFreq;
            this.field = field;
        }

        public Term getTerm(String field, Analyzer indexAnalyzer) {
            if (!field.equals(this.field)) {
                return null;
            }

            if (this.analyzedTerm != null) {
                return new Term(field, this.analyzedTerm);
            } else {
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
                    this.analyzedTerm = termBytes;
                    return new Term(field, this.analyzedTerm);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public String getField() {
            return this.field;
        }

        public TermStatistics getStats(String field, Analyzer indexAnalyzer) {
            Term term = this.getTerm(field, indexAnalyzer);
            if (term != null) {
                return new TermStatistics(term.bytes(), this.docFreq, this.totalTermFreq);
            }
            return null;
        }

    }
}
