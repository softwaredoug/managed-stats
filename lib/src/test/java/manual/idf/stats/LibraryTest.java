/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package manual.idf.stats;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LibraryTest extends SolrTestCaseJ4 {

    @Before
    public void setUp() throws Exception {
        initCore("solrconfig-ltr.xml", "schema.xml");
    }

    @After
    public void cleanup() {
        deleteCore();
    }

    @Test public void someLibraryMethodReturnsTrue() {
        System.out.println("Test!");
    }
}
