package bguspl.set.ex;

import java.util.ArrayList;
import java.util.List;

import bguspl.set.Env;

public class ThreadSafeList {

    private List<Integer> list = new ArrayList<>();
    private final Env env;
    private int slot;

    public ThreadSafeList(Env _env, int _slot){
        this.env = _env;
        this.slot = _slot;
    }


    public synchronized boolean add(int value) {
        if (!list.contains(value)) { //EYTODO maybe change?
            list.add(value);
            env.ui.placeToken(value, this.slot);
            return true;
        }
        return false;
    }

    public synchronized boolean remove(int value) {
        if (list == null) return false;
        boolean exists = list.remove(Integer.valueOf(value));
        if(exists){
            env.ui.removeToken(value, this.slot);
        }
        return exists;
    }

    public synchronized boolean contains(int value) {
        if (list == null) return false;
        return list.contains(value);
    }

    public synchronized int size() {
        if (list == null) return -1;
        return list.size();
    }

    //EYTODO delete later, used for testing
    public void print() {
        for(int i=0;i<this.list.size();i++){
            System.out.println(this.list.get(i)); 
        }
    }
}


