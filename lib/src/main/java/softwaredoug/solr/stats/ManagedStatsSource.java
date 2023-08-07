package softwaredoug.solr.stats;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.stats.StatsSource;

import java.io.IOException;

public class ManagedStatsSource extends StatsSource {

    private StatsSource fallback;

    private IndexSchema schema;

    private ManagedTextField override;

    public ManagedStatsSource(StatsSource fallback, IndexSchema schema) {
        this.fallback = fallback;
        this.schema = schema;
        this.override = getManagedTextFieldOverride();
    }

    private ManagedTextField getManagedTextFieldOverride() {
        ManagedTextField overrider = null;
        String overriderName = "";
        // Find any managed text field with override=true to use for all term freqs
        for (String fieldName : this.schema.getFields().keySet()) {
            ManagedTextField asManaged = fieldAsManagedTextField(fieldName);
            if (asManaged != null && asManaged.wants_to_override_all_field_stats()) {
                if (overrider != null) {
                    throw new IllegalArgumentException("Only one fieldtype can set to override all text field stats. " +
                        "you have set both " + fieldName + " and " + overriderName + " please only specify one");
                }
                overrider = asManaged;
                overriderName = fieldName;
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
            return directConversion;
        }
        if (override != null && schemaField.getType() instanceof TextField) {
            return override;
        }
        return null;
    }

    @Override
    public TermStatistics termStatistics(SolrIndexSearcher localSearcher, Term term, TermContext context) throws IOException {
        ManagedTextField fieldType = this.getBestManagedTextField(term.field());
        if (fieldType == null) {
            return this.fallback.termStatistics(localSearcher, term, context);
        }
        TermStatistics termStats = fieldType.termStatistics(term);
        if (termStats == null) {
            termStats = this.fallback.termStatistics(localSearcher, term, context);
        }
        return termStats;
    }

    @Override
    public CollectionStatistics collectionStatistics(SolrIndexSearcher localSearcher, String field) throws IOException {
        ManagedTextField fieldType = this.fieldAsManagedTextField(field);
        if (fieldType == null) {
            return this.fallback.collectionStatistics(localSearcher, field);
        }
        return fieldType.collectionStatistics(field);
    }
}
