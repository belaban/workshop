package org.taskdistribution;

/**
 * @author Bela Ban
 */
public interface Master {
    Object submit(Task task, long timeout) throws Exception;
}
