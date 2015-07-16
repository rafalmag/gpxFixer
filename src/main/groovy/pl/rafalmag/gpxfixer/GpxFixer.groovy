package pl.rafalmag.gpxfixer

import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import java.text.SimpleDateFormat

public class GpxFixer {
    // eg. 1969-12-31T17:00:00Z
    static final def DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    public static void main(String[] args) {
        def is = new FileInputStream(args[0])
        assert is != null
        def root = getRootFromStream(is)
        // "2013-02-16T17:00:00Z"
        def startTime = DATE_FORMAT.parse(args[1])
        def gpxFixer = new GpxFixer(root, startTime);
        def writer = new FileWriter("output.gpx")
        gpxFixer.fixAndSave(writer);
        println("done")
    }

    private def static GPathResult getRootFromStream(InputStream is) {
        try {
            new XmlSlurper().parse(is)
        } finally {
            try { is.close() } catch (IOException ignore) {}
        }
    }

    private final GPathResult root
    private final Date startTime
    private final Date xmlStartTime

    public GpxFixer(GPathResult root, Date startTime) {
        this.root = root
        this.startTime = startTime
        this.xmlStartTime = DATE_FORMAT.parse(root.trk.time.toString())
        println("Start time $xmlStartTime")
    }

    def fixAndSave(Writer writer) {
        fixMasterTime()
        fixPointsTimes()
        save(writer)
    }

    private def void fixMasterTime() {
        root.trk.time = DATE_FORMAT.format(startTime)
    }

    private def void fixPointsTimes() {
        long offsetMs = calculateOffsetMs()
        def times = root.trk.trkseg.trkpt.time
        println("First original time: ${times[0]}")
        println("Times to convert: ${times.size()}")
        (0..<times.size()).each {
            Date time = DATE_FORMAT.parse(times[it].toString())
            time = new Date(time.getTime() + offsetMs)
            times[it] = DATE_FORMAT.format(time)
        }
        println("First original time converted: ${times[0]}")
    }

    private def long calculateOffsetMs() {
        long offsetMs = startTime.getTime() - xmlStartTime.getTime()
        offsetMs
    }

    private String convertToString(GPathResult doc) {
        def defaultNamespace = doc.lookupNamespace('')

        if (defaultNamespace) {
            def docWithNamespace = {
                mkp.declareNamespace("": defaultNamespace)
                out << doc
            }
            return new StreamingMarkupBuilder().bind(docWithNamespace)
        } else {
            return XmlUtil.serialize(doc as GPathResult)
        }
    }

    private def void save(Writer writer) {
        try {
            writer.write(convertToString(root))
        } finally {
            writer.close()
        }
    }

}