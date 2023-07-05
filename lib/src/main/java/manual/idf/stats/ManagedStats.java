package manual.idf.stats;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.rest.BaseSolrResource;
import org.apache.solr.rest.ManagedResource;
import org.apache.solr.rest.ManagedResourceStorage;
import org.apache.solr.util.plugin.SolrCoreAware;

import java.util.HashMap;
import java.util.Map;


// Reference
// https://github.com/apache/solr/blob/main/solr/modules/ltr/src/java/org/apache/solr/ltr/store/rest/ManagedFeatureStore.java
    public class ManagedStats extends ManagedResource {

        public static class FieldStats {
            private String fieldName;
            private long sumTotalTermFreq;
            private long docCount;
            private long numDocs;

            public FieldStats(String fieldName, long sumTotalTermFreq,
                              long docCount, long numDocs) {
                this.fieldName = fieldName;
                this.docCount = docCount;
                this.sumTotalTermFreq = sumTotalTermFreq;
                this.numDocs = numDocs;
            }

            public long getDocCount() {
                return docCount;
            }

            public long getNumDocs() {
                return numDocs;
            }

            public long getSumTotalTermFreq() {
                return sumTotalTermFreq;
            }

            public String getFieldName() {
                return this.fieldName
            }
        }

    Map<String, FieldStats> fieldStats;

    public ManagedStats(
            String resourceId, SolrResourceLoader loader, ManagedResourceStorage.StorageIO storageIO)
            throws SolrException {
        super(resourceId, loader, storageIO);
        fieldStats = new HashMap<String, FieldStats>();
    }
    @Override
    protected void onManagedDataLoadedFromStorage(NamedList<?> managedInitArgs, Object managedData) throws SolrException {
        // Parse structure:
        //
        //  {
        //   “fields”:
        //      “title”: {
        //          ...
        //        },
        //      "body": {
        //       }
        //     }

        this.fieldStats.clear();

        if (managedData instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> fields = (Map<String, Object>)managedData;
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                Map<String, Object> fieldConfig = (Map<String, Object>)entry.getValue();
                addFieldData(entry.getKey(), fieldConfig);
            }
        }
    }

    protected void addFieldData(String field, Map<String, Object> fieldConfig) {
        Map<String, Object> stats = (Map<String, Object>)fieldConfig.get("stats");
        long docCount = (long)stats.get("docCount");
        long sumTotalTermFreq = (long)stats.get("sumTotalTermFreq");
        long numDocs = (long)stats.get("numDocs");
        FieldStats fieldStats = new FieldStats(field, sumTotalTermFreq, docCount, numDocs);
        this.fieldStats.put(field, fieldStats);
    }

    @Override
    protected Object applyUpdatesToManagedData(Object updates) {
        return null;
    }

    @Override
    public void doDeleteChild(BaseSolrResource endpoint, String childId) {

    }

    @Override
    public void doGet(BaseSolrResource endpoint, String childId) {

    }

    @Override
    void doPut() {

    }

}
