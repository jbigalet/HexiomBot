package hexiombot;

import java.util.Arrays;

public class HexiomBot {

    public static void main(String[] args) {
        GameHandler gameHandler = new GameHandler(6);
//        gameHandler.debug();
        int[][] board = gameHandler.getBoard();
        printBoard(board);
    }
    
    
    private static void printBoard( int[][] board ){
        for( int i=0 ; i<board[0].length ; i++ ){
            for( int j=0 ; j<board.length; j++ ){
                String c = " ";
                if( board[j][i] == -2 )         c = "+ ";
                else if( board[j][i] == -1 )    c = ". ";
                else if( board[j][i] >= 0 )     c = board[j][i]%10 + (board[j][i] >= 10 ? "<" : " ");
                System.out.print(c);
            }
            System.out.println();
        }
    }
}
