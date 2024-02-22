package bguspl.set.ex;

public class Link {
    public int[] cards;
    protected Player player;

    public Link(int[] _cards, Player _player){
        this.cards=_cards; 
        this.player=_player;
    }

    public boolean containsCards(int[] cardsToRemove){
        boolean contains = false;
        for(int i=0;!contains && i<3;i++){
            int card = cardsToRemove[i];
            for(int cardInLink: this.cards){
                if(cardInLink==card){
                    contains = true;
                    break;
                }
            }
        }
        return contains;
    }
}
