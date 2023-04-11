package com.oconeco.helpers

import groovy.cli.commons.CliBuilder
import groovy.cli.commons.OptionAccessor
import org.apache.log4j.Logger


//import groovy.cli.picocli.CliBuilder
//import groovy.cli.picocli.OptionAccessor

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   7/31/22, Sunday
 * @description:
 */

/**
 * General helper class trying to standardize args specific to solr operations
 * probably can be deprecated or removed, but leaving for now
 */
class SolrCrawlConfig {
    static final Logger log = Logger.getLogger(this.class.name)
    OptionAccessor options = null
    ConfigObject config = null


    SolrCrawlConfig(String toolName, String[] args) {
        CliBuilder cli = new CliBuilder()
//        CliBuilder cli = new CliBuilder(usage: "${toolName}.groovy --config=configLocate.groovy", width: 160)
        cli.with {
            h longOpt: 'help', 'Show usage information'
            c longOpt: 'config', args: 1, required: true, argName: 'ConfigFile', 'optional configuration file/url to be parsed/slurped with configSlurper'
            d longOpt: 'datasource', 'Name/Label and directory to crawl', args: '2', valueSeparator: ':', required: false
            f longOpt: 'folders', 'Parent folder(s) to crawl', args: '+', valueSeparator: ',', required: false
            n longOpt: 'name', args: 1, required: false, 'Name of crawl to be added to all solr input docs (files and folders)'
            s longOpt: 'solrUrl', args: 1, required: false, 'Solr url with protocol, host, and port (if any)'
            w longOpt: 'wipeContent', required: false, 'Clear data from previous crawl (beware--easy to really bork things with this..)'
        }

        options = cli.parse(args)
        if (!options) System.exit(-1)

        if (options.help) {
            cli.usage()
            System.exit(0)
        }

        if(options.datasource){
            def ds = options.datasource
            log.info "Datasource(s): $ds"
        }

        if(options.folders){
            def folders = options.folders
            def fs = options.fs
            log.info "Folders(s): $fs"
        }

        if (options.config) {
            String cfg = options.config
            ConfigSlurper configSlurper = new ConfigSlurper()

            File f = new File(options.config)
            if (f.exists()) {
                log.info "Config file: ${f.path}"
                config = configSlurper.parse(f.toURI().toURL())
            } else {
                URL cfgUrl = SolrCrawlConfig.class.getClassLoader().getResource('configs/configLocate.groovy')
                log.info "Config url resolved to: $cfgUrl"
//                String myAppName = 'MyFusionApp'
//                Map cliArgs = [appName: myAppName]
//                configSlurper.setBinding(cliArgs)
                config = configSlurper.parse(cfgUrl)
//                log.warn "Cannot seem to find file: ${cfg}"
            }

        } else {
            log.warn "No Config file found, cancelling..."
            cli.usage()
            System.exit(-1)
        }

        if (options.solrUrl) {
            log.info "Using Solr url from Command line (overriding config file): ${options.solrUrl}"
            config.solrUrl = options.solrUrl
        } else if(config.solrUrl){
            log.info "Using Solr url from CONFIG file): ${config.solrUrl}"
        } else {
            log.info "No solr url given in command line args NOR config file, this should not be possible, exiting..."
            System.exit(-2)
        }

        if(options.folders){
            log.info "Setting config.folders from options.folders (${options.folders})"
            config.folders = options.folders
        }
    }

}
