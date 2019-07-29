package server.core;

import server.core.exception.PlayException;
import server.core.exception.UsefulException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Build {

    public static final List<String> sharedClasses;

    static {
        List<String> list = new ArrayList<String>();
        list.add(server.core.BuildLink.class.getName());
        list.add(server.core.BuildDocHandler.class.getName());
        list.add(ReloadableServer.class.getName());
        list.add(UsefulException.class.getName());
        list.add(PlayException.class.getName());
        list.add(PlayException.InterestingLines.class.getName());
        list.add(PlayException.RichDescription.class.getName());
        list.add(PlayException.ExceptionSource.class.getName());
        list.add(PlayException.ExceptionAttachment.class.getName());
        sharedClasses = Collections.unmodifiableList(list);
    }
}

