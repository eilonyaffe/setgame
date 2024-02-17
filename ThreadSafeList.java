package bguspl.set.ex;

import java.util.ArrayList;
import java.util.List;

public class ThreadSafeList {
    
    private List<Integer> list = new ArrayList<>();

    public synchronized boolean add(int value) {
        if (!list.contains(value)) { //EYTODO maybe change?
            list.add(value);
            
            return true;
        }
        return false;
    }

    public synchronized boolean remove(int value) {
        if (list == null) return false;
        return list.remove(Integer.valueOf(value));
    }

    public synchronized boolean contains(int value) {
        if (list == null) return false;
        return list.contains(value);
    }

    public synchronized int size() {
        if (list == null) return -1;
        return list.size();
    }
}


