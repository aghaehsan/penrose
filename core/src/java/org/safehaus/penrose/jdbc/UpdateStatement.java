package org.safehaus.penrose.jdbc;

import org.safehaus.penrose.source.SourceRef;
import org.safehaus.penrose.filter.Filter;

import java.util.Collection;
import java.util.ArrayList;

/**
 * @author Endi S. Dewata
 */
public class UpdateStatement extends Statement {

    protected SourceRef sourceRef;
    protected Collection<Assignment> assignments = new ArrayList<Assignment>();
    protected Filter filter;

    public SourceRef getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(SourceRef sourceRef) {
        this.sourceRef = sourceRef;
    }

    public Collection<Assignment> getAssignments() {
        return assignments;
    }

    public void addAssignment(Assignment assignment) {
        assignments.add(assignment);
    }

    public void setAssignments(Collection<Assignment> assignments) {
        this.assignments = assignments;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public boolean isEmpty() {
        return assignments.isEmpty();
    }
}
