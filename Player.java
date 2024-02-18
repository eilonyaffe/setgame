package bguspl.set.ex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

        /**
     * The thread representing the current player.
     */
    protected BoundedQueue<Integer> commandsQueue;

    /**
     * The number of tokens the player can still place.
     */
    protected int tokensLeft;

    /**
     * The status of the player. 1=playing. 2=waiting for dealer response. 3=failed to make set, needs to remove tokens
     */
    protected int status;

    /**
     * the tokens the human player had placed
     */
    protected boolean[] placed_tokens;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;



    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.commandsQueue = new BoundedQueue<Integer>();
        this.tokensLeft = 3;
        this.status = 1;
        this.placed_tokens = new boolean[12];
        System.out.println("player created, id: " + id);
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
            while(!commandsQueue.isEmpty()){
                if(this.status==1){
                    int slotCommand = commandsQueue.remove();
                    if(this.placed_tokens[slotCommand]==true){ //means he wishes to remove a token //was slotCommand-5
                        this.table.removeToken(this.id, slotCommand);
                        this.placed_tokens[slotCommand]=false; //was slotCommand-5
                        this.tokensLeft++;
                    }
                    else if(this.placed_tokens[slotCommand]==false){ //means he wishes to place a token //was slotCommand-5
                        this.table.placeToken(this.id, slotCommand);
                        this.placed_tokens[slotCommand]=true; //was slotCommand-5
                        this.tokensLeft--;
                        if(tokensLeft==0) this.status=2; //TODO add calling to the dealer to check if made set
                    }
                }
                else if(this.status==3){ //means there's a token removal command in the queue
                    int slotCommand = commandsQueue.remove();    
                    this.table.removeToken(this.id, slotCommand);
                    this.placed_tokens[slotCommand]=false; //was slotCommand-5
                    this.tokensLeft++;
                    this.status = 1; //returns to play normally
                }
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            ArrayList<Integer> slotsGenerator = new ArrayList<Integer>();
            for (int i=0; i<12; i++) 
                slotsGenerator.add(i);

            while (!terminate) {
                // TODO implement player key press simulator
                Collections.shuffle(slotsGenerator); 
                for(int j=0;j<3;j++)
                    this.commandsQueue.add(slotsGenerator.get(j)); //the random 3 key presses
                while(!commandsQueue.isEmpty()){
                    try {
                        table.placeToken(this.id, commandsQueue.remove());
                        this.tokensLeft--;
                        Thread.sleep(4000); //EYTODO maybe change, now 4 seconds
                    } catch (InterruptedException ignored) {}
                }
                //EYTODO - need to check with the dealer if we found a set and act accordingly
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        //EYTODO maybe have additions to a full queue be on wait until the queue is not full?
        if(this.status==3 && this.placed_tokens[slot]==false){ //player has to only remove tokens now //was slotCommand-5
            //do nothing
        }
        else if(this.status==2){ //player awaits dealer's response
            //do nothing //EYTODO maybe change?
        }
        else{
            this.commandsQueue.add(slot);
            System.out.println("slot pressed by player: "+ id + " is: "+slot);
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        this.score++;
        env.ui.setScore(this.id, score);
        try {
            Thread.sleep(2000); //EYTODO maybe change, now 2 seconds
        } catch (InterruptedException ignored) {}
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
         //eilon- changed to avoid double increasing, was: env.ui.setScore(id, ++score); in this exact line, moved to top
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
         // TODO implement //EYTODO maybe also reset the queue here?
         env.ui.setFreeze(this.id, 5000); //EYTODO chech if works correctly
         try {
            Thread.sleep(5000); //EYTODO maybe change, now 5 seconds
        } catch (InterruptedException ignored) {}
    }

    public int score() {
        return score;
    }
}

class BoundedQueue<T> {
    List<Integer> lst;
    int capacity;
    BoundedQueue(){ this.capacity = 3; this.lst = new ArrayList<Integer>(); }

    public void add(Integer obj) {
        if(lst.size() < capacity)
            lst.add(obj);
    }

    public Integer remove() {
        Integer retValue = -1;
        if (lst.size() > 0){
            retValue = lst.remove(0);
            return retValue;
        }
        return retValue;
    }

    public boolean isEmpty() {
        return lst.isEmpty();
    }
}
