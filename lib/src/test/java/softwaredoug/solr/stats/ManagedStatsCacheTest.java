package softwaredoug.solr.stats;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.After;
import org.junit.Before;

public class ManagedStatsCacheTest extends SolrTestCaseJ4 {

    private ManagedTextField managedField;

    @Before
    public void setupStats() throws Exception {
        // All tests relative to this
        initCore("solrconfig.xml", "schema.xml", "build/resources/test/solr");

        SolrCore core = this.h.getCore();
        IndexSchema schema = core.getLatestSchema();
        FieldType fieldType = schema.getFieldTypeByName("text_general");
        this.managedField = (ManagedTextField)fieldType;

    }

    @After
    public void cleanup() {
        deleteCore();
    }


    public void indexDocs() {

        assertU(adoc("id", "1",
                     "text_general", "Democratic Order op Planets"));
        assertU(adoc("id", "2", "text_general", "Tool"
                ));
    }
}
