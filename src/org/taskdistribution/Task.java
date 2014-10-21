package org.taskdistribution;

import java.io.Serializable;

/** A task contains all of the necessary data that is shipped to a slave. The execute() method then uses that data
 * to execute and returns a result, which is sent back to the master who submitted the task
 * @author Bela Ban
 */
public interface Task extends Serializable {
    public abstract Object execute();
}
