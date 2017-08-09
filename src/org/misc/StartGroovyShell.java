package org.misc;

import org.codehaus.groovy.tools.shell.AnsiDetector;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.codehaus.groovy.tools.shell.util.Logger;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jgroups.util.Util;

import java.io.*;

/**
 * Starter for Groovy shell. Doesn't set a SecurityManager, compared to org.codehaus.groovy.tools.shell.Main (from
 * which this class was copied). Allows for a script to be passed at startup, to initialize the shell.
 */
public class StartGroovyShell {
    static {
        AnsiConsole.systemInstall(); // Install the system adapters
        Ansi.setDetector(new AnsiDetector()); // Register jline ansi detector
    }

    protected static void start(File init_script) throws Exception {
        IO io=new IO();
        Logger.io=io;

        final Groovysh shell=new Groovysh(io);
        if(init_script != null) {
            InputStream in=new FileInputStream(init_script);
            String line;
            while((line=Util.readLine(in)) != null) {
                line=line.trim();
                if(!line.isEmpty()) {
                    // System.out.println("evaluating \"" + line + "\"");
                    shell.execute(line);
                }
            }
        }

        // Add a hook to display some status when shutting down...
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if(shell.getHistory() != null) {
                try {
                    shell.getHistory().flush();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }));

        int code=shell.run("");
        System.exit(code);
    }



    public static void main(String[] args) throws Exception {
        StartGroovyShell tmp=new StartGroovyShell();
        File file=null;

        for(int i=0; i < args.length; i++) {
            if(args[i].equals("-T")) {
                setTerminalType(args[++i]);
                continue;
            }
            if(args[i].equals("-C")) {
                setColor(args[++i]);
                continue;
            }
            if(args[i].startsWith("-D")) {
                setSystemProperty(args[i].substring(2));
                continue;
            }
            if(args[i].equals("-file")) {
                file=new File(args[++i]);
                continue;
            }
            help();
            return;
        }

        start(file);
    }

    protected static void help() {
        System.out.println("StartGroovyShell -C <true|false>: enable or disable use of ANSI colors\n" +
                             "    -Dkey=val      Define a system property\n" +
                             "    -T <type>        Specify the terminal TYPE to use\n" +
                             "    -file <startup script");
    }


    static void setTerminalType(String type) {
        assert type != null;
        type=type.toLowerCase();
        if(type.equals("auto"))
            type=null;
        else if(type.startsWith("unix"))
            type="jline.UnixTerminal";
        else if(type.startsWith("win"))
            type="jline.WindowsTerminal";
        else if(type.equals("false") || type.equals("off") || type.equals("none")) {
            type="jline.UnsupportedTerminal";
            // Disable ANSI, for some reason UnsupportedTerminal reports ANSI as enabled, when it shouldn't
            Ansi.setEnabled(false);
        }
        if(type != null)
            System.setProperty("jline.terminal", type);
    }

    static void setColor(String value) {
        if (value == null)
            Ansi.setEnabled(true);
        else
            Ansi.setEnabled(Boolean.valueOf(value));
    }

    static void setSystemProperty(final String nameValue) {
        String name, value;

        if (nameValue.indexOf('=') > 0) {
            String[] tmp=nameValue.split("=", 2);
            name=tmp[0];
            value=tmp[1];
        }
        else {
            name=nameValue;
            value=Boolean.TRUE.toString();
        }
        System.setProperty(name, value);
    }
}
