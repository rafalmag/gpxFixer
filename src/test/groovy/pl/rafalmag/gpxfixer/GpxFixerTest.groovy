package pl.rafalmag.gpxfixer

import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.XMLUnit
import org.junit.Before
import org.junit.Test

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual


class GpxFixerTest {

    @Before
    void setUpXmlUnit(){
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    void testFixAndSave1() {
        //given
        def inputPath = "/input1.gpx"
        def expectedOutputPath = "/output1.gpx"
        def startDate = "2013-02-19T13:24:38Z"
        // when + then
        shouldFixInputFile(inputPath, startDate, expectedOutputPath)
    }

    @Test
    void testFixAndSave2() {
        //given
        def inputPath = "/input2.gpx"
        def expectedOutputPath = "/output2.gpx"
        def startDate = "2013-02-16T14:02:00Z"
        // when + then
        shouldFixInputFile(inputPath, startDate, expectedOutputPath)
    }

    private void shouldFixInputFile(String inputPath, String startDate, String expectedOutputPath) {
        // given
        def input = getClass().getResource(inputPath)
        def date = GpxFixer.DATE_FORMAT.parse(startDate);
        StringWriter gpxFixedResultWriter = new StringWriter();

        //when
        def fixer = new GpxFixer(input.newInputStream(), date);
        fixer.fixAndSave(gpxFixedResultWriter)

        //then
        def expectedOutput = getClass().getResource(expectedOutputPath).text
        Diff diff = new Diff(expectedOutput, gpxFixedResultWriter.toString())
        assertXMLEqual(diff, true);
    }
}
