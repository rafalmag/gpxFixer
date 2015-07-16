package pl.rafalmag.gpxfixer

import com.lexicalscope.jewel.cli.ArgumentValidationException
import com.lexicalscope.jewel.cli.CliFactory

class Main {

    static void main(String[] args) {
        GpxFixerArguments result = getArguments(args)

        // "2013-02-16T17:00:00Z"
        def startTime = GpxFixer.DATE_FORMAT.parse(result.startDate)
        def gpxFixer
        new FileInputStream(result.inputPath).withCloseable {
            gpxFixer = new GpxFixer(it, startTime);
        }
        def writer = new FileWriter(result.outputPath)
        gpxFixer.fixAndSave(writer);
        println("all done")
    }

    static GpxFixerArguments getArguments(String[] args) {
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
}
