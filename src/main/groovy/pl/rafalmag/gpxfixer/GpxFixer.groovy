package pl.rafalmag.gpxfixer

import com.lexicalscope.jewel.cli.ArgumentValidationException
import com.lexicalscope.jewel.cli.CliFactory
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class GpxFixer {
    // eg. 1969-12-31T17:00:00Z
    static def DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    public static final String progressFormat = "%.2f %% done %n"

    public static void main(String[] args) {
        GpxFixerArguments result = getArguments(args)

        def is = new FileInputStream(result.inputPath)
        assert is != null
        def root = getRootFromStream(is)
        // "2013-02-16T17:00:00Z"
        def startTime = DATE_FORMAT.parse(result.startDate)
        def gpxFixer = new GpxFixer(root, startTime);


        def writer = new FileWriter(result.outputPath)
        gpxFixer.fixAndSave(writer);
        println("all done")
    }

    private static GpxFixerArguments getArguments(String[] args) {
        try {
            CliFactory.parseArguments(GpxFixerArguments, args);
        }
        catch (ArgumentValidationException e) {
            println 'Usage: java -jar gpxFixer.jar <input> <startDate>'
            println()
            println 'Application converts gpx file by fixing points timestamps. '
            println 'First point timestamp will be set to start date and offset will be calculated. '
            println 'Other points will be modified accordingly using the same time offset. '
            println()
            println(e.getMessage())
            System.exit(-1);
            throw new IllegalStateException("after system exit", e)
        }
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
        AtomicInteger index = new AtomicInteger(0)
        def timesSize = times.size();
        def executor = logProgress(index, timesSize)
        try {
            while (index.get() < timesSize) {
                Date time = DATE_FORMAT.parse(times[index.get()].toString())
                time = new Date(time.getTime() + offsetMs)
                times[index.get()] = DATE_FORMAT.format(time)
                index.incrementAndGet()
            }
        } finally {
            executor.shutdown()
        }
        printf(progressFormat, 1.0)
        println("First original time after convertion: ${times[0]}")
    }

    ExecutorService logProgress(AtomicInteger index, int max) {
        def executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            void run() {
                double progress = index.get() / (double) max * 100;
                printf(progressFormat, progress)
            }
        }, 0, 1, TimeUnit.SECONDS);
        executor
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