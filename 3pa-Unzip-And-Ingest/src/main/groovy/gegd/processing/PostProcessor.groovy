package gegd.processing

import org.apache.camel.Processor
import org.apache.camel.Exchange

public class PostProcessor implements Processor {

    private Map mount
    private String urlPrefix
    private String urlSuffix
    private String[] extensions

    private File logFile

    /**
     * Constructor.
     */
    public PostProcessor(mount, urlPrefix, urlSuffix, extensions) {
        this.mount = mount
        this.urlPrefix = urlPrefix
        this.urlSuffix = urlSuffix
        this.extensions = extensions
        this.logFile = new File("/${mount.bucket}/${mount.logFilePath}")
    }

    /**
     * Process to POST to omar stager the filename of the file corresponding
     * to the omd file in the exchange.
     *
     * @param exchange This exchange contains an omd file in which there should be a corresponding
     * image file for posting inside the same directory.
     */
    public void process(Exchange exchange) throws Exception {

        println "#"*80
        println "INSIDE POST PROCESSOR"
        println "#"*80

        ArrayList<Map> postMapList = new ArrayList<>()
        def ant = new AntBuilder()
        def filePath =  exchange.in.getHeaders().CamelFileAbsolutePath
        def filepathNoExtension = filePath.substring(0, filePath.lastIndexOf("."))
        String url = ''
        String postFilePath = ''
        File hisFile = new File("${filepathNoExtension}.his")

        if (hisFile.exists()) {
            println "-"*80
            println "his file exists"
            println "${filepathNoExtension}.his"
            println "-"*80
            exchange.in.setHeader("CamelHttpMethod", "stop-his-file-already-exists")
            Logger.logLine(("\n${filepathNoExtension}.his already staged!\n\n"), logFile)
            return
        }

        for (e in extensions) {
            postFilePath = "${filepathNoExtension}.${e}"
            File postFile = new File(postFilePath)
            url = postFile.exists() ? "${urlPrefix}${postFilePath}${urlSuffix}" : url
        }

        if (url != '')
            logProcess(postFilePath)
        else {
            println "@"*80
            println "no image for omd file"
            println "@"*80
            exchange.in.setHeader("CamelHttpMethod", "stop-omd-file-has-no-image")
            Logger.logLine(("\nomd file has no image!\n\n"), logFile)
            return
        }


        exchange.in.setHeader(Exchange.HTTP_URI, url)
        exchange.in.setHeader("CamelHttpMethod", "POST")

        logHttp(url)
    }

    private void logHttp(url) {
        Logger logger = new Logger("HTTP", "POST",
                                   "Sending https post to Omar Stager",
                                   "POST URL:",
                                   url, ColorScheme.http, logFile, false, ConsoleColors.WHITE)
        logger.log()
    }

    private void logProcess(postFilePath) {
        Logger logger = new Logger("Merge", "PostProcessor",
                                   "Found omd file of image file to be posted",
                                   "File found for POST operation:",
                                   postFilePath.split('/').last(), ColorScheme.splitter,
                                   logFile, false, ConsoleColors.FILENAME)
        logger.log()
    }
}