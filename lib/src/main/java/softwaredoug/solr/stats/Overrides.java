package softwaredoug.solr.stats;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;

public class Overrides {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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


    // Source of truth for the overrides because we don't know how to analyze them yet
    private List<AnalyzedTermStats> termStats;  // field -> termStats

    // Once analyzed, we can save them here so we don't need to lookup again
    private Map<Term, TermStatistics> termStatsCached; // field,term -> once processed
    // Global collection stats for each field
    private Map<String, CollectionStatistics> fieldStats;

    public Overrides(List<String> lines, String typeName, Map<String, String > config) {

        fieldStats = new HashMap<String, CollectionStatistics>();
        termStats = new ArrayList<AnalyzedTermStats>();
        termStatsCached = new HashMap<Term, TermStatistics>();

        Map<String, AnalysisOption> analysisOptionMap = new HashMap<>();
        String currField = "";
        boolean fields = true;

        // loop and parse the lines in the file
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
            } else {
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
                this.getTermStats().add(new AnalyzedTermStats(field, unanalyzedTerm, docFreq, totalTermFreq, analysisOption));
            }
        }

        log.info("ManagedTextField created. Storing stats for {} fields; {} terms",
                this.fieldStats.size(), this.getTermStats().size());
    }

    public TermStatistics findBestOverride(Term term, Map<Overrides.AnalysisOption, Analyzer> analyzerOptions) {
        TermStatistics fromCache = this.termStatsCached.get(term);
        if (fromCache != null) {
            return fromCache;
        }

        for (AnalyzedTermStats stats: this.getTermStats()) {
            TermStatistics thisStat = stats.getStats(term.field(), analyzerOptions);
            if (thisStat != null && thisStat.term().equals(term.bytes()) && term.field().equals(stats.getField())) {
                log.trace("Override term stats found for term: {}", term);
                this.termStatsCached.put(term, thisStat);
                return thisStat;
            }
        }
        return null;
    }

    public List<AnalyzedTermStats> getTermStats() {
        return termStats;
    }

    public Map<String, CollectionStatistics> getFieldStats() {
        return fieldStats;
    }
}
