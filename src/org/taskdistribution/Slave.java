package org.taskdistribution;

/**
 * @author Bela Ban
 */
public interface Slave {
    Object handle(Task task);
}
