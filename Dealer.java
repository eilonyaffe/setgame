package bguspl.set.ex;

import bguspl.set.Env;
import bguspl.set.ThreadLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;


    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

        /**
     * The system time when starting the 60 seconds loop
     */
    private long startTime = Long.MAX_VALUE;

    /**
     * The system time when starting the 60 seconds loop
     */
    private long timeElapsed = Long.MAX_VALUE;
 

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        createAndRunPlayerThreads(); //EY new

        while (!shouldFinish()) {
            placeCardsOnTable();
            this.table.tableReady = true; //indicates player can start placing cards
            this.table.hints(); //EYTODO delete later
            timerLoop();
            updateTimerDisplay(true); //EY i changed to true
            removeAllCardsFromTable();
            this.table.tableReady = false;

        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Creates and runs all player threads
     */
    private void createAndRunPlayerThreads() {
        for (int i = 0; i < players.length; i++){
            Player a = players[i];
            Thread playerThread = new Thread(a);
            playerThread.start();
        }
    }


    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() { //EYTODO think what cards should i remove. probably remove a set with 3 tokens. should be called when a player annouces he finished
        // TODO implement
        if(!this.table.finishedPlayersCards.isEmpty()){
            Link removedLink = this.table.finishedPlayersCards.removeFirst();
            int[] cardsSet = removedLink.cards;
            Player player = removedLink.player;
            boolean success = this.env.util.testSet(cardsSet);
            System.out.println("set made by player: "+player.id +" is: "+success);
            if(success){
                player.wasCorrect=1; //indicates the player thread to award itself

                for(int card: cardsSet){
                    int slot = this.table.cardToSlot[card];
                    ThreadSafeList slotObj = this.table.getSlot(slot);
                    int[] playersWithToken = slotObj.getPlayers();
                    for(int playerToReturn : playersWithToken){ //first returning tokens to players
                        Player currPlayer = this.players[playerToReturn];
                        currPlayer.tokensLeft++;
                        System.out.println("removed card from slot: "+ slot +" for player: "+currPlayer.id +" tokens left: "+currPlayer.tokensLeft);
                        currPlayer.placed_tokens[slot]=false;
                    }
                    slotObj.removeAll();
                    this.table.removeCard(slot);
                }
                // player.wasCorrect=1; //indicates the player thread to award itself //was here
                this.updateTimerDisplay(true);
            }
            else{ 
                player.wasCorrect=2; //indicates the player thread to penalize itself
                
            }
            this.table.hints(); //EYTODO delete later 
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() { 
        // TODO implement
        this.dealerShuffle();
        while(!deck.isEmpty()){
            for(int slot=0;slot<12;slot++){
                if(table.slotToCard[slot]==null){
                    table.placeCard(deck.remove(0), slot);
                }
            }
            break;
        }
    }

    /**
     * shuffles the deck
     */
    private void dealerShuffle() { //EYTODO new
        if(!deck.isEmpty()){
            Collections.shuffle(deck); 
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        if(this.startTime == Long.MAX_VALUE && this.reshuffleTime == Long.MAX_VALUE){
            this.startTime = System.currentTimeMillis();
            this.reshuffleTime = System.currentTimeMillis() + 61000; //EY: dont change!
            this.timeElapsed = System.currentTimeMillis() + 61000;
        }

        try {
            Thread.sleep(1000); //EYTODO maybe change, now 1 seconds
            this.timeElapsed -= 1000;
        } catch (InterruptedException ignored) {}

    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if(reset){
            this.startTime = Long.MAX_VALUE;
            this.reshuffleTime = Long.MAX_VALUE;
            this.timeElapsed = Long.MAX_VALUE;
        }
        else{
            env.ui.setCountdown(timeElapsed-startTime, false);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        for(Player player:players){
            player.status=2;
            player.commandsQueue.Clear();
            player.tokensLeft = 3;
            player.placed_tokens = new boolean[12];
        }
        
        for(int slot=0;slot<12;slot++){
                if(table.slotToCard[slot]!=null){
                    deck.add(table.slotToCard[slot]);
                    table.removeCard(slot);
                }
        }
        this.table.removeAllTokens();
        for(Player player: players){
            player.status = 1;
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
    }
}
