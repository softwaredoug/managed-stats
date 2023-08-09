package softwaredoug.solr.stats;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.junit.After;
import org.junit.Before;

public class ManagedStatsCacheOverrideMissingFieldTest  extends SolrTestCaseJ4 {
    private ManagedTextField managedField;

    @Before
    public void setupStats() throws Exception {
        // All tests relative to this
        initCore("solrconfig.xml", "schema_override_field_not_overridden.xml", "build/resources/test/solr");

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


    public void testSearchNonManagedFieldIsNowActuallyManaged() {
        assertQ(
                "search uses doc count",
                req(
                        "q", "cat",
                        "qf", "not_managed_or_in_override",
                        "defType", "edismax"),
                "*[count(//doc)=2]");
    }

    public void indexDocs() {

        assertU(adoc("id", "1",
                "text", "Democratic Order op Planets"));
        assertU(adoc("id", "2", "text", "Tool", "not_managed", "tacos",
                "not_managed_or_in_override", "cat hat"
        ));
        assertU(adoc("id", "3", "text", "foo", "not_managed", "burritos",
                "not_managed_or_in_override", "fat cat"

        ));
        assertU(adoc("id", "4", "text", "bar", "not_managed", "nachos",
                "not_managed_or_in_override", "sausage brat"
        ));
        assertU(commit());
    }
}
