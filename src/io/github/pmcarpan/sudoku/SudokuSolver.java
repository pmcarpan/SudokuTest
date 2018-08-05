package io.github.pmcarpan.sudoku;
public class SudokuSolver {

    private static final int LENGTH = 9, EMPTY = 0;
    private final int[][] board;

    public SudokuSolver(int[][] board) {
        if (board == null || board.length != LENGTH)
            throw new IllegalArgumentException("Input board invalid");
        
        for (int i = 0; i < LENGTH; i++) 
            if (board[i] == null || board[i].length != LENGTH) 
                throw new IllegalArgumentException("Input board invalid");

        if (!digitsAreValid(board))
            throw new IllegalArgumentException("Input board digit(s) invalid");
        
        this.board = new int[LENGTH][LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            this.board[i] = board[i].clone();
        }
    }

    private boolean digitsAreValid(int[][] b) {
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 0; j < LENGTH; j++) {
                if (b[i][j] < 0 || b[i][j] > 9)
                    return false;
            }
        }
        return true;
    }
    
    private boolean isInRow(int row, int num) {
        for (int j = 0; j < LENGTH; j++)
            if (board[row][j] == num)
                return true;

        return false;
    }

    private boolean isInCol(int col, int num) {
        for (int i = 0; i < LENGTH; i++)
            if (board[i][col] == num)
                return true;

        return false;
    }

    private boolean isInGrid(int row, int col, int num) {
        int r = (row / 3) * 3, c = (col / 3) * 3;
        for (int i = r; i < r + 3; i++) 
            for (int j = c; j < c + 3; j++) 
                if (board[i][j] == num)
                    return true;

        return false;
    }

    private boolean isValid(int row, int col, int num) {
        return !isInRow(row, num) && !isInCol(col, num) && !isInGrid(row, col, num);
    }

    public boolean solve() {
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 0; j < LENGTH; j++) {
                if (board[i][j] == EMPTY) {
                    for (int k = 1; k <= LENGTH; k++) {
                        if (isValid(i, j, k)) {
                            board[i][j] = k;
                            if (solve())
                                return true;
                            else
                                board[i][j] = EMPTY;
                        } // if isValid
                    } // for k
                    return false;
                } // if EMPTY
            } //for j
        } // for i
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            if (i % 3 == 0) 
                sb.append("-------------------------\n");
            for (int j = 0; j < LENGTH; j++) {
                if (j % 3 == 0)
                    sb.append("| ");
                sb.append(board[i][j] + " ");
            }
            sb.append("| ");
            sb.append("\n");
        }
        sb.append("-------------------------\n");
        return sb.toString();
    }

    public static void main(String[] args) {
        int[][] testBoard = {
                {0,5,0,0,0,0,0,1,0},
                {0,0,2,4,0,7,5,0,0},
                {0,1,0,0,6,0,0,7,0},
                {0,0,8,9,0,6,1,0,0},
                {3,0,0,0,0,0,0,0,8},
                {0,0,4,8,0,3,7,0,0},
                {0,6,0,0,9,0,0,3,0},
                {0,0,3,1,0,4,9,0,0},
                {0,4,0,0,0,0,0,2,0},
        };
        SudokuSolver s = new SudokuSolver(testBoard);
        System.out.println(s);
        if (s.solve()) {
            System.out.println(s);
        }
        else {
            System.out.println("NOT SOLVABLE");
        }
    }

}
