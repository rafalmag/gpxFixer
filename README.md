# gpxFixer

This project has tests running on Travis CI.

[![Build Status](https://travis-ci.org/rafalmag/gpxFixer.svg?branch=master)](https://travis-ci.org/rafalmag/gpxFixer)

```
Usage: java -jar gpxFixer.jar <input> <startDate>

Application converts gpx file by fixing points timestamps. 
First point timestamp will be set to start date and offset will be calculated. 
Other points will be modified accordingly using the same time offset. 

The options available are:
	[--help -h] : display help
	--input -i value : Input gpx file
	[--output -o value] : Output gpx file, default: output.gpx
	--startDate -s value : Start date
```
