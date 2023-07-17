/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package softwaredoug.solr.stats;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.junit.*;

public class ManagedStatsTest extends SolrTestCaseJ4 {

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

    @Test
    public void testParsedFieldTypeCorrectly() {
        SolrCore core = this.h.getCore();
        IndexSchema schema = core.getLatestSchema();
        FieldType fieldType = schema.getFieldTypeByName("text_general");
        ManagedTextField asManagedField = (ManagedTextField)fieldType;
        assertNotNull("Field Type text_general not parsed from schema", asManagedField);
    }

    @Test
    public void testHasCollectionStats() {
        CollectionStatistics stats = this.managedField.collectionStatistics("foo");
        assertEquals(stats.docCount(), 10);
        assertEquals(stats.maxDoc(), 11);
        assertEquals(stats.sumTotalTermFreq(), 13);
        assertEquals(stats.sumDocFreq(), 13);
    }

    @Test
    public void testGetsStatsForField() {
        BytesRef text = new BytesRef("foo".getBytes());
        Term term = new Term("text", text);
        TermStatistics termStats = this.managedField.termStatistics(term);

        assertNotNull(termStats);
        assertEquals(term.bytes(), termStats.term());
        assertEquals(termStats.docFreq(), 2);      /*from the test fixture in resources*/
        assertEquals(termStats.totalTermFreq(), 8);
    }

    @Test
    public void testTextAnalyzed() {
        BytesRef text = new BytesRef("uppercase".getBytes());
        Term term = new Term("text", text);
        TermStatistics termStats = this.managedField.termStatistics(term);
        assertNotNull(termStats);
    }

    @Test
    public void testGetsNoStatsIfNoTermProduced() {
        BytesRef text = new BytesRef("stopword".getBytes());
        Term term = new Term("text", text);
        TermStatistics termStats = this.managedField.termStatistics(term);
        assertNull(termStats);
    }
    
    @Test
    public void testAnalyzesToMultipleTermsIgnored() {
        BytesRef text = new BytesRef("two terms".getBytes());
        Term term = new Term("text", text);
        TermStatistics termStats = this.managedField.termStatistics(term);
        assertNull(termStats);
    }
}
