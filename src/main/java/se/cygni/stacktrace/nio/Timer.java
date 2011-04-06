package se.cygni.stacktrace.nio;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class Timer {

    private Deque<Event> stack = new ArrayDeque<Event>();    
    private Map<String, Summary> summaries = new HashMap<String, Summary>();
    
    public static class Summary implements Comparable<Summary> {
        private String name;
        private double mean;
        private double q;
        private int count;
        
        public Summary(String name) {
            this.name = name;
        }
        
        public void update(long duration) {
            count++;
            if (count == 1) {
                mean = duration;
            } else {
                double mean2 = mean + (duration - mean)/count;
                q = q + (duration - mean)*(duration - mean2);
                mean = mean2;
            }
        }
        
        public String toString() {
            return String.format("%-40s %8.0f ms %8.0f stdv %8s iter", name, getMean(), getStdv(), count);
        }

        private double getMean() {
            return mean;
        }

        private double getStdv() {
            return Math.sqrt(q/count);
        }

        public int compareTo(Summary o) {
            return name.compareTo(o.name);
        }
    }
    
    public class Event {
        private long start;
        private long duration;
        private String name;
        private String fullName;

        public Event(String fullName, String name) {
            this.fullName = fullName;
            this.name = name;
            start = System.currentTimeMillis();
        }
        
        public void stop() {
            duration = System.currentTimeMillis() - start;
            
        }
        
        public String getName() {
            return name;
        }
        
        public String getFullName() {
            return fullName;
        }
        
        @Override
        public String toString() {
            return String.format("%-40s %8s ms", name, duration);
        }

        public long getDuration() {
            return duration;
        }
    }

    public Event startEvent(String name) {
        Event e = new Event(getPath(name), name);
        stack.push(e);
        return e;
    }
    
    public void stopEvent(Event e) {
        e.stop();
        Event e2 = stack.pop();
        if (e2 != e) {
            throw new RuntimeException("Event not properly stopped: " + e.getFullName());
        }
        updateSummary(e);
        System.out.println(e);
    }
    
    private String getPath(String name) {
        Event parent = stack.peek();
        return parent == null ? name : parent.getFullName() + "." + name;
    }
    
    private void updateSummary(Event e) {
        if (e.getFullName() != e.getName()) { 
            updateSummary(e.getFullName(), e);
        }
        updateSummary(e.getName(), e);
    }

    private void updateSummary(String name, Event e) {
        Summary s = summaries.get(name);
        if (s == null) {
            s = new Summary(name);
            summaries.put(name, s);
        }
        s.update(e.getDuration());
    }
    
    public void printSummaries() {
        ArrayList<Summary> l = new ArrayList<Summary>(summaries.values());
        Collections.sort(l);
        
        System.out.println();
        System.out.println("Total");
        System.out.println("=====");
        for (Summary summary : l) {
            System.out.println(summary);
        }
    }
}
