package softwaredoug.solr.stats;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.stats.LocalStatsSource;
import org.apache.solr.search.stats.StatsCache;
import org.apache.solr.search.stats.StatsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

public class ManagedStatsSource extends StatsSource {

    private final StatsCache.StatsCacheMetrics metrics;

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private StatsSource fallback;

    private IndexSchema schema;

    private ManagedTextField override;

    public ManagedStatsSource(StatsSource fallback, StatsCache.StatsCacheMetrics metrics, IndexSchema schema) {
        this.fallback = fallback;
        this.metrics = metrics;
        this.schema = schema;
        this.override = getManagedTextFieldOverride();
    }

    private ManagedTextField getManagedTextFieldOverride() {
        ManagedTextField overrider = null;
        String overriderName = "";
        // Find any managed text field with override=true to use for all term freqs
        for (Map.Entry<String, FieldType> entry : this.schema.getFieldTypes().entrySet()) {
            if (entry.getValue() instanceof ManagedTextField) {
                ManagedTextField asManaged = (ManagedTextField)(entry.getValue());
                String fieldTypeName = entry.getKey();
                log.trace("Is ManagedTextField? checking field type: {}", fieldTypeName);

                if (asManaged != null && asManaged.wantsToOverride()) {
                    if (overrider != null) {
                        throw new IllegalArgumentException("Only one fieldtype can set to override all text field stats. " +
                                "you have set both " + fieldTypeName + " and " + overriderName + " please only specify one");
                    }
                    overrider = asManaged;
                    overriderName = fieldTypeName;
                    log.info("ManagedStatsCache overrider is : {}", fieldTypeName);
                }
            }
        }
        return overrider;
    }

    private ManagedTextField fieldAsManagedTextField(String field) {
        SchemaField schemaField = this.schema.getField(field);
        ManagedTextField fieldType = null;
        if (ManagedTextField.class.isInstance(schemaField.getType())) {
            fieldType = (ManagedTextField) schemaField.getType();
        }
        return fieldType;
    }

    private ManagedTextField getBestManagedTextField(String field) {
        SchemaField schemaField = this.schema.getField(field);
        // always prefer direct one
        ManagedTextField directConversion = fieldAsManagedTextField(field);
        if (directConversion != null) {
            log.trace("Using direct conversion for field: {}", field);
            return directConversion;
        }
        if (override != null && schemaField.getType() instanceof TextField) {
            log.trace("Returning override for field: {} override is: {}", override, override.getTypeName());
            return override;
        }
        log.trace("No managed text field available in schema, skipping");
        return null;
    }

    @Override
    public TermStatistics termStatistics(SolrIndexSearcher localSearcher, Term term, int docFreq, long totalTermFreq) throws IOException {
        ManagedTextField fieldType = this.getBestManagedTextField(term.field());
        TermStatistics termStats = null;
        if (fieldType == null) {
            log.trace("Falling back: No ManagedTextField for field: {} term: {}", term.field(), term.text());
            return this.fallback.termStatistics(localSearcher, term, docFreq, totalTermFreq);
        }
        termStats = fieldType.termStatistics(term);
        if (termStats == null) {
            termStats = this.fallback.termStatistics(localSearcher, term, docFreq, totalTermFreq);
            log.trace("Falling back: No termStats in managed field for field: {} term: {}", term.field(), term.text());
            return this.fallback.termStatistics(localSearcher, term, docFreq, totalTermFreq);
        }
        log.trace("Found manual stats for field:{}, term:{} ", term.field(), term.text());
        return termStats;
    }

    @Override
    public CollectionStatistics collectionStatistics(SolrIndexSearcher localSearcher, String field) throws IOException {
        ManagedTextField fieldType = this.getBestManagedTextField(field);
        if (fieldType == null) {
            log.trace("Falling back -- no ManagedTextField -- for field: {}", field);
            return this.fallback.collectionStatistics(localSearcher, field);
        }
        CollectionStatistics rVal = fieldType.collectionStatistics(field);
        if (rVal == null) {
            log.trace("Falling back -- ManagedTextField -- does not track: {}", field);
            return this.fallback.collectionStatistics(localSearcher, field);
        }
        log.trace("Found manual stats for field: {}", field);
        return rVal;
    }
}
