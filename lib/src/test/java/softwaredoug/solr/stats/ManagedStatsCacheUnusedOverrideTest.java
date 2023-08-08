package softwaredoug.solr.stats;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.junit.After;
import org.junit.Before;
public class ManagedStatsCacheUnusedOverrideTest extends SolrTestCaseJ4 {
    private ManagedTextField managedField;

    @Before
    public void setupStats() throws Exception {
        // All tests relative to this
        initCore("solrconfig.xml", "schema_unused_override.xml", "build/resources/test/solr");

        SolrCore core = this.h.getCore();
        IndexSchema schema = core.getLatestSchema();
        FieldType fieldType = schema.getFieldTypeByName("text_general");
        this.managedField = (ManagedTextField)fieldType;

        indexDocs();
    }

    @After
    public void cleanup() {
        deleteCore();
    }

    public void testSearchNonManagedFieldGetsRightDocFreq() {
        assertQ(
                "search uses doc count",
                req(
                        "q", "burritos",
                        "qf", "not_managed",
                        "defType", "edismax",
                        "debug", "true"),
                "//lst[@name='explain']/str[@name='3' and contains(text(),\"10 = n, number of documents containing term\")]");

    }

    public void testSearchNonManagedFieldGetsRightDocCount() {
        assertQ(
                "search uses doc count",
                req(
                        "q", "burritos",
                        "qf", "not_managed",
                        "defType", "edismax",
                        "debug", "true"),
                "//lst[@name='explain']/str[@name='3' and contains(text(),\"50 = N, total number of documents with field\")]");

    }


    public void indexDocs() {

        assertU(adoc("id", "1",
                "not_managed", "Democratic Order op Planets"));
        assertU(adoc("id", "2", "not_managed", "tacos"
        ));
        assertU(adoc("id", "3", "not_managed", "burritos"
        ));
        assertU(adoc("id", "4", "not_managed", "nachos"
        ));
        assertU(commit());
    }
}
