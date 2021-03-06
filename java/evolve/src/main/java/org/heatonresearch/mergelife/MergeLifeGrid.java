/*
 * MergeLife, Copyrighr 2018 by Jeff Heaton
 * http://www.heatonresearch.com/mergelife/
 * MIT License
 */
package org.heatonresearch.mergelife;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

public class MergeLifeGrid {
    /**
     * The two grids (double buffering) for MergeLife. Thge first dimension is the
     * grid to use (2 grids supported).  The second and third are the rows and columns.
     * The fourth dimension are the RGB values.
     */
    private final int[][][][] grid;

    /**
     * The merged grid.  This is an average of the 3 RGB values for one of the two
     * RGB grids.
     */
    private final int[][] mergeGrid;

    /**
     * The mode of the grid, this is the most common value in the merged grid.  This is
     * the merged RGB values of the background color.
     */
    private int modeGrid;

    /**
     * The current grid, flips between 0 and 1 for each step/generation.
     */
    private int currentGrid = 0;

    /**
     * The current step, how many update steps have occured.
     */
    private int currentStep = 0;

    /**
     * Create a MergeLife grid with the specified number of rows and columns.
     * @param rows The number of rows in the grid.
     * @param cols The number of columns in the grid.
     */
    public MergeLifeGrid(int rows, int cols) {
        this.grid = new int[2][rows][cols][3];
        this.mergeGrid = new int[rows][cols];
    }

    /**
     * Randomize the grid to random RGA colors.
     * @param desiredGrid Which grid to randomize (0 or 1)
     * @param rnd A random number generator to use.
     */
    public void randomize(int desiredGrid, Random rnd) {
        int grid[][][] = getGrid(desiredGrid);

        for(int row = 0; row<getRows(); row++) {
            for (int col = 0; col < getCols(); col++) {
                for(int i=0;i<3;i++) {
                    grid[row][col][i] = (int)(256 * rnd.nextDouble());
                }
            }
        }

        this.currentStep = 0;
        this.currentGrid = desiredGrid;
    }

    /**
     * Get one of the two grids.  There are two grids to support double buffering.
     * @param desiredGrid The desired grid (0 or 1).
     * @return A 3D array holding the grid (with RGB values).
     */
    public int[][][] getGrid(int desiredGrid) {
        if( desiredGrid<0 || desiredGrid>1) {
            throw new MergeLifeException("Only grids 0 or 1 are supported.");
        }
        return this.grid[desiredGrid];
    }

    /**
     * @return The merged grid, this is the average of the 3 RGB values in one of the
     * main grids.  This value is calculated by calling the method calculateModeGrid.
     */
    public int[][] getMergeGrid() {
        return this.mergeGrid;
    }

    /**
     * @return The number of rows in a grid.
     */
    public int getRows() {
        return this.mergeGrid.length;
    }

    /**
     * @return The number of columns in the grid.
     */
    public int getCols() {
        return this.mergeGrid[0].length;
    }

    /**
     * @return Get the most common merged value from the last time that the method
     * calculateModeGrid was called.
     */
    public int getModeGrid() {
        return this.modeGrid;
    }


    public void calculateModeGrid(int desiredGrid) {
        int[] hist = new int[256];

        // First calculate the merged grid, also build up a histogram of the avrage 0-255 colors.
        for(int row = 0; row<getRows(); row++) {
            for(int col = 0; col<getCols(); col++) {
                int[] px = this.grid[desiredGrid][row][col];
                int a = (px[0]+px[1]+px[2])/3;
                this.mergeGrid[row][col] = a;
                hist[a]++;
            }
        }

        // Now determine which merged color (0-255) is used the most.  This is the grid mode.
        int maxIndex = -1;
        for(int i=0;i<hist.length;i++) {
            if( maxIndex==-1 || hist[i]>hist[maxIndex]) {
                maxIndex =i;
            }
        }
        this.modeGrid = maxIndex;
    }

    /**
     * Count the neighbors next to the given cell.
     * @param row The row of the current cell.
     * @param col The column of the current cell.
     * @return The neighbor count.
     */
    public int countNeighbors(int row, int col) {
        final int[] xHat = {0, 0, -1, 1, -1, 1, 1, -1};
        final int[] yHat = {-1, 1, 0, 0, -1, 1, -1, 1};

        int sum = 0;

        for (int i = 0; i < xHat.length; i++)
        {
            final int neighborRow = yHat[i] + row;
            final int neighborCol = xHat[i] + col;
            if (neighborRow >= 0 &&
                    neighborCol >= 0 &&
                    neighborRow < getRows() &&
                    neighborCol < getCols())
            {
                sum += this.mergeGrid[neighborRow][neighborCol];
            } else {
                sum += getModeGrid();
            }
        }
        return sum;
    }

    /**
     * Peform one step/generation on the grid with the specified rule.
     * @param rule The rule to apply.
     */
    public void step(MergeLifeRule rule) {
        int targetGrid = currentGrid==0 ? 1:0;
        calculateModeGrid(this.currentGrid);

        int[][][] m = getGrid(this.currentGrid);
        calculateModeGrid(this.currentGrid);
        //int b = getModeGrid();

        for (int row = 0; row < getRows(); row += 1) {
            int[][] line = getGrid(this.currentGrid)[row];
            int[][] linePrime = getGrid(targetGrid)[row];
            for (int col = 0; col < line.length; col += 1) {
                int c = countNeighbors(row,col);

                for(MergeLifeRule.SubRule subRule: rule.getSubRules()) {
                    if( c < subRule.getAlpha() ) {
                        int dPrime = subRule.getGamma()+1;

                        if( subRule.getBeta()<0) {
                            dPrime=(dPrime%8)+1;
                        }

                        for(int j=0;j<3;j++) {
                            int delta = MergeLifeRule.ColorTable[dPrime-1][j] - line[col][j];
                            delta = (int)Math.floor(delta*Math.abs(subRule.getBeta()));
                            linePrime[col][j] = line[col][j] + delta;
                        }
                        break;
                    }
                }
            }
        }
        this.currentGrid = currentGrid==0 ? 1:0;
        this.currentStep += 1;
    }

    /**
     * Save the current grid as a PNG file.
     * @param desiredGrid The grid to save (0 or 1).
     * @param zoom How many pixels should each sell contain.
     * @param file The filename.
     * @throws IOException An error occured writing the file.
     */
    public void savePNG(int desiredGrid, int zoom, File file) throws IOException {
        VisualizeGridImage viz = new VisualizeGridImage(this, zoom);
        BufferedImage img = viz.visualize(desiredGrid);
        ImageIO.write(img,"png", file);
    }

    /**
     * Load a grid from a PNG file.
     * @param zoom The zoom size that the PNG was saved with.
     * @param file The file to load.
     */
    public static MergeLifeGrid loadPNG(int zoom, File file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file);
        byte[] pixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        MergeLifeGrid grid = new MergeLifeGrid(bufferedImage.getHeight(),bufferedImage.getWidth());
        int[][][] gridData = grid.getGrid(grid.getCurrentGrid());

        int colWidth = 3*zoom;
        int rowWidth = grid.getCols()*colWidth;

        for(int row = 0;row<gridData.length;row++) {
            for(int col = 0;col<gridData[row].length;col++) {
                int offset = (row*rowWidth)+(col*colWidth);
                for(int i=0;i<3;i++) {
                    gridData[row][col][i] = pixels[offset]&0xff;
                }
            }
        }
        return grid;
    }

    /**
     * @return The current step.
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * Set the current step.
     * @param currentStep The current step.
     */
    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    /**
     * @return The current grid (0 or 1).
     */
    public int getCurrentGrid() {
        return currentGrid;
    }

    /**
     * Set the current grid.
     * @param currentGrid The current grid (0 or 1).
     */
    public void setCurrentGrid(int currentGrid) {
        this.currentGrid = currentGrid;
    }

    /**
     * Flatten the grid into an array of bytes.
     * @param desiredGrid The grid to use.
     * @return An array of bytes.
     */
    public byte[] toBytes(int desiredGrid) {
        byte[] result = new byte[getRows()*getCols()*3];

        int idx = 0;
        for(int row=0;row<getRows();row++) {
            for(int col=0;col<getCols();col++) {
                for(int i=0;i<3;i++) {
                    result[idx++] = (byte)this.grid[desiredGrid][row][col][i];
                }
            }
        }
        return result;
    }

    /**
     * Generate a SHA256 hash from the grid.  This can be a quick way to compare multiple grids without the
     * need to store the entire grid.
     * @param desiredGrid The grid to generate a SHA256 hash from.
     * @return The hash string.
     */
    public String toSHA256(int desiredGrid) throws NoSuchAlgorithmException {
        byte[] b = toBytes(desiredGrid);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(b);

        StringBuffer hexString = new StringBuffer();

        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
