package pl.rafalmag.gpxfixer

import com.lexicalscope.jewel.cli.Option

public interface GpxFixerArguments {

    @Option(shortName = "i",
            longName = "input",
            description = "Input gpx file")
    String getInputPath()

    @Option(shortName = "s",
            longName = "startDate",
            description = "Start date")
    String getStartDate()

    @Option(shortName = "o",
            longName = "output",
            description = "Output gpx file, default: output.gpx",
            defaultValue = "output.gpx")
    String getOutputPath()

    @Option(helpRequest = true,
            description = "display help",
            shortName = "h",
            longName = "help")
    boolean getHelp()
}