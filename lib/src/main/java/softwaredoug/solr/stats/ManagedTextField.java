package softwaredoug.solr.stats;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;

import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

public class ManagedTextField extends TextField implements ResourceLoaderAware {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String statsFile;
    private boolean override_all_fields;

    private List<AnalyzedTermStats> termStats;  // field -> termStats
    private HashMap<String, CollectionStatistics> fieldStats;

    // How to analyze terms in the file
    public enum AnalysisOption {
        RAW, INDEX, QUERY, OVERRIDE
        // Raw, no analysis done to file
        // Index, used the field name in the term stat's row index analyzer
        // Query, use the field name in the term stat's row query analyzer
        // Override, use the overriders analyzer
    }

    public static AnalysisOption DEFAULT_ANALYSIS = AnalysisOption.QUERY;

    AnalysisOption parseAnalysisOption(String option) {
        switch (option.toLowerCase()) {
            case "raw":
                return AnalysisOption.RAW;
            case "index":
                return AnalysisOption.INDEX;
            case "query":
                return AnalysisOption.QUERY;
            case "override":
                return AnalysisOption.OVERRIDE;
            default:
                throw new IllegalArgumentException("Illegal analysis option provided: {}".format(option));
        }

    }

    @Override
    protected void init(IndexSchema schema, Map<String, String> args) {
        this.statsFile = args.remove("stats");
        this.termStats = new ArrayList<AnalyzedTermStats>();
        this.fieldStats = new HashMap<String, CollectionStatistics>();

        this.override_all_fields = false;
        String override = args.remove("override");
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
        Map<String, AnalysisOption> analysisOptionMap = new HashMap<>();
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
                if (statsHeader.length > 6 || statsHeader.length < 5) {
                    throw new IllegalArgumentException("Field stats should provide field name plus 4 stats"
                                                       + " you provided: " + line);
                }
                long[] stats = new long[4];
                AnalysisOption howToAnalyze = DEFAULT_ANALYSIS;
                int idx = 0;
                String fieldName = statsHeader[0];
                for (String globalStat : statsHeader) {
                    if (globalStat == fieldName) {
                        continue;
                    }
                    switch (idx) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                            stats[idx] = Long.parseLong(globalStat);
                            idx++;
                            break;
                            // optionally configure the analyzer
                        case 4:
                            howToAnalyze = parseAnalysisOption(globalStat);
                            analysisOptionMap.put(fieldName, howToAnalyze);
                    }
                }
                long docCount = stats[0];
                long maxDocs = stats[1];
                long sumTotalTermFreq = stats[2];
                long sumTotalDocFreq = stats[3];
                log.warn("*****************************************");
                log.warn("USING MANAGED STATS FOR: {} ", fieldName);
                fieldStats.put(fieldName,
                                new CollectionStatistics(fieldName, maxDocs, docCount, sumTotalTermFreq, sumTotalDocFreq));
            }
            else {
                // Now for individual terms
                AnalysisOption analysisOption = DEFAULT_ANALYSIS;
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
                analysisOption = analysisOptionMap.get(field);
                if (analysisOption == null) {
                    analysisOption = DEFAULT_ANALYSIS;
                }
                String[] remainder = Arrays.copyOfRange(line_split, 1, line_split.length - 2);
                unanalyzedTerm = String.join(",", remainder);

                if (docFreq > totalTermFreq) {
                    throw new IllegalArgumentException("Doc stats error at: <" + line + "> -- docFreq more than totalTermFreq not allowed");
                }
                log.debug("Loaded term stats for field: {} term: {}", field, unanalyzedTerm);
                this.termStats.add(new AnalyzedTermStats(field, unanalyzedTerm, docFreq, totalTermFreq, analysisOption));
            }
        }

        log.info("ManagedTextField created. Storing stats for {} fields; {} terms",
                 this.fieldStats.size(), this.termStats.size());
    }

    public boolean wantsToOverride() {
        return this.override_all_fields;
    }

    public TermStatistics termStatistics(Term term, Map<ManagedTextField.AnalysisOption, Analyzer> analyzerOptions) {
        // slow for correctness, very bad to do this here
        for (AnalyzedTermStats stats: this.termStats) {
            TermStatistics thisStat = stats.getStats(term.field(), analyzerOptions);
            if (thisStat != null && thisStat.term().equals(term.bytes()) && term.field().equals(stats.getField())) {
                return thisStat;
            }
        }
        return null;
    }

    public CollectionStatistics collectionStatistics(String field) {
        log.trace("Lookup stats for field: {} fieldStats len: {}", field, fieldStats.size());
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

        private AnalysisOption analysisOption;

        AnalyzedTermStats(String field, String term, long docFreq, long totalTermFreq,
                          AnalysisOption analysisOption) {
            this.unanalyzedTerm = term;
            this.field = field;
            this.docFreq = docFreq;
            this.totalTermFreq = totalTermFreq;
            this.field = field;
            this.analysisOption = analysisOption;
        }

        public Term getTerm(String field, Map<AnalysisOption, Analyzer> options) {
            if (!field.equals(this.field)) {
                return null;
            }
            if (this.analyzedTerm != null) {
                return new Term(field, this.analyzedTerm);
            }

            Analyzer analyzer = options.get(this.analysisOption);

            if (this.analysisOption == AnalysisOption.RAW || analyzer == null) {
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

        public TermStatistics getStats(String field, Map<AnalysisOption, Analyzer> options) {
            Term term = this.getTerm(field, options);
            if (term != null) {
                return new TermStatistics(term.bytes(), this.docFreq, this.totalTermFreq);
            }
            return null;
        }

    }
}
