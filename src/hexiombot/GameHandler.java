package hexiombot;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.List;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class GameHandler {

    static final Color kongregateGrey = new Color( 51, 51, 51 );
    static final int greyThreshold = 1;
    
    static final Color boardGrey = new Color(108, 108, 108);
    static final Color openGrey = new Color(102, 102, 102);    
    
    int size;
    Rectangle gameRect;
    Robot robot;
    {
        try {
            robot = new Robot();
        } catch ( AWTException exception ) {
            System.err.println("Impossible to create the robot");
            System.exit(1);
        }
    }
    
    public GameHandler( int size ) {
        this.size = size;
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        BufferedImage screenshot = robot.createScreenCapture( new Rectangle(screenSize) );
        
        try {
            ImageIO.write(screenshot, "png", new File("screen.png"));
        } catch (IOException ex) {}
        
            // we get the game rect:
            // we start on the middle left of the screen, got to the right until the kongregate grey starts
            // then a bit more to the right until its gone
            // then up until it appears again
            // the right until the pixel below is not grey anymore
            // and finally to the bottom
        int x = 0;
        int y = screenSize.height/2;
        while( colorDiff(kongregateGrey, new Color(screenshot.getRGB(x, y))) > greyThreshold )
            x++;
        while( colorDiff(kongregateGrey, new Color(screenshot.getRGB(x, y))) < greyThreshold )
            x++;

        while( colorDiff(kongregateGrey, new Color(screenshot.getRGB(x, y))) > greyThreshold )
            y--;
        
        int right = x;
        while( colorDiff(kongregateGrey, new Color(screenshot.getRGB(right, y+1))) > greyThreshold )
            right++;
        
        int bottom = screenSize.height/2;
        while( colorDiff(kongregateGrey, new Color(screenshot.getRGB(x, bottom))) > greyThreshold )
            bottom++;
        
//        System.out.println(x + " " + y + " " + right + " " + bottom);
        
        gameRect = new Rectangle(x, y, right-x, bottom-y);
        
            // get the real rect around the board
        while( colorDiff(boardGrey, new Color(screenshot.getRGB((int)gameRect.getCenterX(), y))) > greyThreshold )
            y++;
        while( colorDiff(boardGrey, new Color(screenshot.getRGB(x, (int)gameRect.getCenterY()))) > greyThreshold )
            x++;
        gameRect = new Rectangle(x, y, gameRect.width-2*(x-gameRect.x), gameRect.height-2*(y-gameRect.y));

        screenshot = robot.createScreenCapture( gameRect );
        try {
            ImageIO.write(screenshot, "png", new File("crop.png"));
        } catch (IOException ex) {}
        
        for(int i=0 ; i<=6 ; i++)
            new File("numbers/"+i).mkdirs();
    }
    
    public int[][] getBoard() {
        BufferedImage screenshot = robot.createScreenCapture( new Rectangle(gameRect) );
        int[][] board = new int[size*2+1][size*2+1];
        for( int[] row: board )
            Arrays.fill(row, -2);
        
        for( int i=0 ; i<size*2-1 ; i++ )
            for( int j=firstRow(i) ; j<firstRow(i)+colSize(i) ; j++ )
                board[i+1][j+1] = getCell(screenshot, i, j);
        
        try {
            ImageIO.write(screenshot, "png", new File("test.png"));
        } catch (IOException ex) {}
        
        return board;
    }
    
    private Point getClosestColoredPixel( BufferedImage screenshot, int i, int j, Color color, int threshold, int maxRadius ){
        int r = 0;
//        System.out.println(i + " " + j);
        while( r < maxRadius ){
                // x & y are the offsets
            for( int x = -r ; x <= r ; x++ )
                for( int ys = -1 ; ys <= +1 ; ys +=2 ){ // +- radius
                    int y = (int)(ys*Math.sqrt(r*r-x*x));
                    if( i+x >= 0 && i+x < screenshot.getWidth()
                            && j+y >= 0 && j+y < screenshot.getHeight() )
//                    System.out.println((i+x) + " x " + (j+y));
                        if( colorDiff(color, new Color(screenshot.getRGB(i+x, j+y))) < threshold )
                            return new Point(i+x, j+y);
                }
            r++;
        }

        return null;
    }
    
    /*    2
        2.24.
        .22.2
        3...3
         33.
    */
    private int firstRow( int c ){
        return (int)Math.ceil( (2*size-1 - colSize(c))/2d );
    }
    
    private int colSize( int c ){
        int distFromCenter = Math.abs( size-1 - c );
        return 2*size-1 - distFromCenter;
    }
    
    private int getCell( BufferedImage screenshot, int i, int j ){
        int cx = (int)(((i+.5)/(double)(2*size-1))*gameRect.width);
        int cy = (int)(((j+.5*((i+size)%2))/(double)(2*size-1))*gameRect.height);
//        screenshot.setRGB(cx, cy, Color.black.getRGB());
        if( colorDiff(boardGrey, new Color(screenshot.getRGB(cx, cy))) < greyThreshold )
            return -2;
        if( colorDiff(openGrey, new Color(screenshot.getRGB(cx, cy))) < greyThreshold )
            return -1;
        
        Point closestWhite = getClosestColoredPixel(screenshot, cx, cy, Color.white, 100, 20);
//        screenshot.setRGB(closestWhite.x, closestWhite.y, Color.black.getRGB());
        ArrayList<Point> surface = new ArrayList<>();
        getWhiteSurface(screenshot, closestWhite.x, closestWhite.y, surface);
//        for( Point p: surface )
//            screenshot.setRGB(p.x, p.y, Color.black.getRGB());
        
        int res;
        
        BufferedImage surfaceImg = surfaceToImage(surface, 1);
        Integer holeDetect = holeDetection( surfaceImg );
        if( holeDetect != null ){
            res = holeDetect;
        } else {
            if( is5( surfaceImg ) )
                res = 5;
            else if( is3( surfaceImg ) )
                res = 3;
            else if( is2( surfaceImg ) )
                res = 2;
            else
                res = 1;
        }

        try {
            ImageIO.write(surfaceToImage(surface, 5), "png", new File("numbers/"+res+"/"+i+"x"+j+".png"));
        } catch (IOException ex) {}

            // check if its fixed
        Point closestGrey = getClosestColoredPixel(screenshot, closestWhite.x, closestWhite.y, new Color(122, 122, 122), 1, (int)(gameRect.height/(double)size/4));
        if( closestGrey != null )
            res += 10;
        
        return res;
    }
    
    private boolean is5( BufferedImage surface ){
        // 2 black points, vertically separated by white
        // first got white on top/left/bot
        // second on top/left/bot/right
        for( int i=0 ; i<surface.getWidth() ; i++ )
            for( int j=0 ; j<surface.getHeight() ; j++ )
                if( blackAndGotWhiteAround(surface, i, j, new boolean[] {true, false, true, true}))
                    for( int y=j+1 ; y<surface.getHeight() ; y++ )
                        if( surface.getRGB(i, y) == Color.white.getRGB() )
                            for( int yy=y+1 ; yy<surface.getHeight() ; yy++ )
                                if( blackAndGotWhiteAround(surface, i, yy, new boolean[] {true, true, true, true}))
                                    return true;
        return false;
    }
    
    private boolean is3( BufferedImage surface ){
        for( int i=0 ; i<surface.getWidth() ; i++ )
            for( int j=0 ; j<surface.getHeight() ; j++ )
                if( blackAndGotWhiteAround(surface, i, j, new boolean[] {true, true, true, false}))
                    for( int y=j+1 ; y<surface.getHeight() ; y++ )
                        if( surface.getRGB(i, y) == Color.white.getRGB() )
                            for( int yy=y+1 ; yy<surface.getHeight() ; yy++ )
                                if( blackAndGotWhiteAround(surface, i, yy, new boolean[] {true, true, true, false}))
                                    return true;
        return false;
    }
    
    private boolean is2( BufferedImage surface ){
        for( int i=0 ; i<surface.getWidth() ; i++ )
            for( int j=0 ; j<surface.getHeight() ; j++ )
                if( blackAndGotWhiteAround(surface, i, j, new boolean[] {true, true, true, true}))
                    return true;
        return false;
    }
    
        // around = top/right/bot/left
    private boolean blackAndGotWhiteAround( BufferedImage image, int i, int j, boolean[] around ){
        if( image.getRGB(i, j) == Color.white.getRGB() )
            return false;

        if( around[0] != gotWhiteOnTop(image, i, j) )
            return false;
        if( around[1] != gotWhiteOnRight(image, i, j) )
            return false;
        if( around[2] != gotWhiteOnBottom(image, i, j) )
            return false;
        if( around[3] != gotWhiteOnLeft(image, i, j) )
            return false;
        return true;
    }
    
    private boolean gotWhiteOnTop( BufferedImage image, int i, int j ){
        for( int y=j ; y>=0 ; y-- )
            if( image.getRGB(i,y) == Color.white.getRGB() )
                return true;
        return false;
    }
    private boolean gotWhiteOnBottom( BufferedImage image, int i, int j ){
        for( int y=j ; y<image.getHeight() ; y++ )
            if( image.getRGB(i,y) == Color.white.getRGB() )
                return true;
        return false;
    }
    private boolean gotWhiteOnLeft( BufferedImage image, int i, int j ){
        for( int x=i ; x>=0 ; x-- )
            if( image.getRGB(x,j) == Color.white.getRGB() )
                return true;
        return false;
    }
    private boolean gotWhiteOnRight( BufferedImage image, int i, int j ){
        for( int x=i ; x<image.getWidth() ; x++ )
            if( image.getRGB(x,j) == Color.white.getRGB() )
                return true;
        return false;
    }
    
    private Integer holeDetection( BufferedImage surface ){
        spreadBlack( surface, 0, 0 );

        int holeX = 0, holeY = 0, holeCount = 0, holeBottom = 0;
        for( int i=0 ; i<surface.getWidth() ; i++ )
            for( int j=0 ; j<surface.getHeight() ; j++ )
                if( surface.getRGB(i, j) == Color.black.getRGB() ){
                    holeCount++;
                    holeX += i;
                    holeY += j;
                    holeBottom = Math.max(holeBottom, j);
                }
        
        if( holeCount == 0 )
            return null;
        
            // if the hole bottom is to far, its a 4
        if( surface.getHeight()-holeBottom > gameRect.height/(double)size/10 )
            return 4;
        
            // if hole center is at the bottom, its a 6
        if( holeY/(double)holeCount > surface.getHeight()/(double)2 )
            return 6;
        
        return 0;
    }
    
    private boolean isThereBlack( BufferedImage image ){
        for( int i=0 ; i<image.getWidth() ; i++ )
            for( int j=0 ; j<image.getHeight() ; j++ )
                if( image.getRGB(i, j) == Color.black.getRGB() )
                    return true;
        return false;
    }
    
    private void spreadBlack( BufferedImage image, int i, int j ){
        if( i<0 || j<0 || i>=image.getWidth() || j >= image.getHeight() )
            return;
        if( image.getRGB(i, j) != Color.black.getRGB() )
            return;
        
        image.setRGB(i, j, Color.pink.getRGB());
        
        for( int x=i-1 ; x<=i+1 ; x++ )
            for( int y=j-1 ; y<=j+1 ; y++ )
                spreadBlack(image, x, y);
    }
    
    private BufferedImage surfaceToImage( ArrayList<Point> surface, int surfaceDebugOffset ){
            // get bounds
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        int left = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        for( Point p: surface ){
            top = Math.min(top, p.y);
            bottom = Math.max(bottom, p.y);
            left = Math.min(left, p.x);
            right = Math.max(right, p.x);
        }
        
        BufferedImage image = new BufferedImage(right-left+1+surfaceDebugOffset*2, bottom-top+1+surfaceDebugOffset*2, BufferedImage.TYPE_INT_RGB);
        for( Point p: surface )
            image.setRGB(   p.x-left+surfaceDebugOffset, 
                            p.y-top+surfaceDebugOffset, 
                            Color.white.getRGB());
        return image;
    }
    
    private void getWhiteSurface( BufferedImage screenshot, int i, int j, ArrayList<Point> currentSurface ) {
        if( currentSurface.contains( new Point(i, j) )
                || colorDiff(Color.white, new Color(screenshot.getRGB(i, j))) > 100 )
            return;
        
        currentSurface.add( new Point(i, j) );
        
        for( int x = i-1 ; x<=i+1 ; x++ )
            for( int y = j-1 ; y<=j+1 ; y++ )
                getWhiteSurface(screenshot, x, y, currentSurface);
    }

    private double colorDiff( Color c1, Color c2 ){
        int red = c1.getRed() - c2.getRed();
        int green = c1.getGreen() - c2.getGreen();
        int blue = c1.getBlue() - c2.getBlue();
        return Math.sqrt(red*red + green*green + blue*blue);
    }


    public void debug(){
        // save a screenshot, where all non white pixels are black
        BufferedImage screenshot = robot.createScreenCapture( new Rectangle(gameRect) );
        for( int i=0 ; i<gameRect.width ; i++ )
            for( int j=0 ; j<gameRect.height ; j++ )
                if( colorDiff(Color.white, new Color(screenshot.getRGB(i, j))) > 100 )
                    screenshot.setRGB(i, j, Color.black.getRGB());
                else
                    screenshot.setRGB(i, j, Color.white.getRGB());
        try {
            ImageIO.write(screenshot, "png", new File("blackwhite.png"));
        } catch (IOException ex) {}
    }
}
