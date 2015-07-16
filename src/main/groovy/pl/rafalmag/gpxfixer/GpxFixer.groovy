package pl.rafalmag.gpxfixer

import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import java.text.SimpleDateFormat

class GpxFixer {
    // eg. 1969-12-31T17:00:00Z
    static def DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    public static void main(String[] args) {
        CliBuilder cli = getCli()
        def options = cli.parse(args)
        if (options == null) {
            System.exit(-1);
        }

        def is = new FileInputStream(options.input)
        assert is != null
        def root = getRootFromStream(is)
        // "2013-02-16T17:00:00Z"
        def startTime = DATE_FORMAT.parse(options.startDate)
        def gpxFixer = new GpxFixer(root, startTime);
        def writer = new FileWriter("output.gpx")
        gpxFixer.fixAndSave(writer);
        println("done")
    }

    private static CliBuilder getCli() {
        def header = 'Application converts gpx file by fixing points timestampts. ' +
                'First point timestamp will be set to start date and offset will be calculated. ' +
                'Other points will be modified accordingly using the same time offset. ' +
                'Output will be written to output.gpx\n\n' +
                'Options:'
        def cli = new CliBuilder(usage: 'java -jar gpxFixer.jar <input> <startDate>', header: header, stopAtNonOption: true)
        cli.i(longOpt: 'input', required: true, args: 1, argName: 'inputFile', "Input gpx file")
        cli.s(longOpt: 'startDate', required: true, args: 1, argName: 'startDate', "Start date")
        cli.h(longOpt: 'help', 'print this message')
        cli
    }

    def static GPathResult getRootFromStream(InputStream is) {
        is.withCloseable { new XmlSlurper().parse(it) }
    }

    final GPathResult root
    final Date startTime
    final Date xmlStartTime

    GpxFixer(GPathResult root, Date startTime) {
        this.root = root
        this.startTime = startTime
        def xmlStartTimeString = root.trk.time.toString();
        this.xmlStartTime = DATE_FORMAT.parse(xmlStartTimeString)
        println("Input start time ${DATE_FORMAT.format(startTime)}")
        println("Xml start time $xmlStartTimeString")
    }

    void fixAndSave(Writer writer) {
        fixMasterTime()
        fixPointsTimes()
        save(writer)
    }

    void fixMasterTime() {
        root.trk.time = DATE_FORMAT.format(startTime)
    }

    void fixPointsTimes() {
        long offsetMs = calculateOffsetMs()
        def times = root.trk.trkseg.trkpt.time
        println("First original time: ${times[0]}")
        println("Times to convert: ${times.size()}")
        (0..<times.size()).each {
            Date time = DATE_FORMAT.parse(times[it].toString())
            time = new Date(time.getTime() + offsetMs)
            times[it] = DATE_FORMAT.format(time)
        }
        println("First original time after convertion: ${times[0]}")
    }

    long calculateOffsetMs() {
        startTime.getTime() - xmlStartTime.getTime()
    }

    String convertToString(GPathResult doc) {
        def defaultNamespace = doc.lookupNamespace('')
        if (defaultNamespace) {
            def docWithNamespace = {
                mkp.declareNamespace("": defaultNamespace)
                out << doc
            }
            new StreamingMarkupBuilder().bind(docWithNamespace)
        } else {
            XmlUtil.serialize(doc as GPathResult)
        }
    }

    void save(Writer writer) {
        writer.withCloseable { it.write(convertToString(root)) }
    }
}