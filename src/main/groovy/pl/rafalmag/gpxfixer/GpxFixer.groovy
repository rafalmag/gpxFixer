package pl.rafalmag.gpxfixer

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
    static final def DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    static final String PROGRESS_FORMAT = "%.2f %% done %n"

    def static GPathResult getRootFromStream(InputStream is) {
        is.withCloseable { new XmlSlurper().parse(it) }
    }

    final GPathResult root
    final Date startTime
    final Date xmlStartTime

    GpxFixer(InputStream is, Date startTime) {
        this(getRootFromStream(is),startTime)
    }

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
        printf(PROGRESS_FORMAT, 1.0)
        println("First original time after convertion: ${times[0]}")
    }

    ExecutorService logProgress(AtomicInteger index, int max) {
        def executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            void run() {
                double progress = index.get() / (double) max * 100;
                printf(PROGRESS_FORMAT, progress)
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