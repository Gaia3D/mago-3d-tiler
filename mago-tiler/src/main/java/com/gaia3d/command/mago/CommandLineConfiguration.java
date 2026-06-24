package com.gaia3d.command.mago;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public interface CommandLineConfiguration {
    Options createOptions();

    CommandLine createCommandLine(Options options, String[] args) throws ParseException;
}
