package bguspl.set.ex;

import java.util.ArrayList;
import java.util.Collections;

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
     * The commands list of the current player.
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
     * indicates whether the player made a correct set or not. -1 is neither. 1 is correct, 2 is wrong
     */
    protected int wasCorrect;

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
        this.wasCorrect=-1;
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
            while(!commandsQueue.lst.isEmpty()){
                
                int slotCommand = commandsQueue.lst.remove(0); //was  int slotCommand = commandsQueue.remove()
                System.out.println("slot command from: "+this.id + " was: "+slotCommand);
                if(this.status==1){
                    if(this.placed_tokens[slotCommand]==true){ //means he wishes to remove a token //was slotCommand-5
                        this.table.removeToken(this.id, slotCommand);
                        this.placed_tokens[slotCommand]=false; //was slotCommand-5
                        this.tokensLeft++;

                    }
                    else if(this.placed_tokens[slotCommand]==false){ //means he wishes to place a token //was slotCommand-5
                        this.table.placeToken(this.id, slotCommand);
                        this.placed_tokens[slotCommand]=true; //was slotCommand-5
                        this.tokensLeft--;

                        if(tokensLeft==0){
                            this.status=2; //TODO add calling to the dealer to check if made set
                            this.sendSetCards();
                            try {
                                Thread.sleep(1000); //EYTODO maybe change, now 1 seconds
                            } catch (InterruptedException ignored) {}
                            if(this.wasCorrect==1){
                                System.out.println("player: "+this.id +" was correct");
                                this.point();
                            }
                            else if(this.wasCorrect==2){
                                this.penalty();
                            }
                            else{
                                System.out.println("was correct wasnt updated: "+this.wasCorrect);
                            }
                            }
                        }
                }
                
                else if(this.status==3){ //means there are only token removal commands in the queue
                    this.table.removeToken(this.id, slotCommand);
                    this.placed_tokens[slotCommand]=false; //was slotCommand-5
                    this.tokensLeft++;
                    this.status = 1; //returns to play normally
                }
            
            }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }
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
                        Integer removed = commandsQueue.remove();
                        table.placeToken(this.id, removed);
                        this.placed_tokens[removed]=true; 
                        this.tokensLeft--;
                        Thread.sleep(4000); //EYTODO maybe change, now 4 seconds
                    } catch (InterruptedException ignored) {}

                    if (tokensLeft == 0){
                        this.status=2; //TODO add calling to the dealer to check if made set
                            this.sendSetCards();
                            try {
                                Thread.sleep(1000); //EYTODO maybe change, now 1 seconds
                            } catch (InterruptedException ignored) {}
                            if(this.wasCorrect==1){
                                System.out.println("player: "+this.id +" was correct");
                                this.point();
                            }
                            else if(this.wasCorrect==2){
                                this.penalty();
                                for (int i = 0; i < 12; i++){ //remove all tokens
                                    if (this.placed_tokens[i] == true){
                                        this.table.removeToken(this.id, i);
                                        this.placed_tokens[i]=false; //was slotCommand-5
                                        this.tokensLeft++;
                                    }
                                }
                                this.status = 1; //resumes to play
                            }
                            else{
                                System.out.println("was correct wasnt updated: "+this.wasCorrect);
                            }
                    }
                }

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
     * creates the alleged set of cards that the player chose, and sends it to the table
     */
    public void sendSetCards() {
        // EYTODO implement
        int[] cards = new int[3];
        int j=0;
        for(int i=0;j<3 && i<this.placed_tokens.length;i++){
            if(this.placed_tokens[i]==true){
                cards[j] = table.slotToCard[i];
                j++;
            }
        }
        LinkSafe link = new LinkSafe(cards, this);
        this.table.finishedPlayersCards.add(link); 
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
            if(this.table.tableReady)
                this.commandsQueue.add(slot);
            // System.out.println("slot pressed by player: "+ id + " is: "+slot);
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
        this.commandsQueue.Clear();
        this.placed_tokens = new boolean[12]; //resets the player's placed_tokens
        this.status = 1; //indicates he resumes to play
        this.wasCorrect=-1;

        env.ui.setFreeze(this.id, 1000); //EYTODO chech if works correctly
        try {
            Thread.sleep(1000); //EYTODO maybe change, now 1 seconds
        } catch (InterruptedException ignored) {}
        env.ui.setFreeze(this.id, 0); //"unfreeze"
        System.out.println("player status after point: "+status);

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
         //eilon- changed to avoid double increasing, was: env.ui.setScore(id, ++score); in this exact line, moved to top
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
         // TODO implement 
         int freezeTime = 5000;
         env.ui.setFreeze(this.id, freezeTime); //EYTODO chech if works correctly
         for(int i=1;i<6;i++){
            try {
                Thread.sleep(1000); //EYTODO maybe change, now total 5 seconds
            } catch (InterruptedException ignored) {}
            env.ui.setFreeze(this.id, freezeTime-(i*1000)); //descending until unfrozen
            this.commandsQueue.Clear();
         }
        this.status = 3;
        this.wasCorrect=-1;
    }

    /**
     * Returns the player's score
     */
    public int score() {
        return score;
    }
}

class BoundedQueue<T> {
    ArrayList<Integer> lst;
    int capacity;
    BoundedQueue(){ this.capacity = 3; this.lst = new ArrayList<Integer>(); }

    public void add(Integer obj) {
        if(lst.size() < capacity)
            lst.add(obj);
    }

    public Integer remove() {
        Integer retValue = -1;
        if (!lst.isEmpty()){
            retValue = lst.remove(0);
            return retValue;
        }
        return retValue;
    }

    public void Clear() {
        lst.clear();
    }

    public boolean isEmpty() {
        return lst.isEmpty();
    }
}
