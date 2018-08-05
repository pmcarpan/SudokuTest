package io.github.pmcarpan.sudoku;

public class Main {

    public static void main(String[] args) {
        // extract matrix
        SudokuExtractor extractor = new SudokuExtractor("images/sudokubig.jpg");
        
        // try to solve
        SudokuSolver solver = new SudokuSolver(extractor.getExtractedArray());
        System.out.println("ORIGINAL MATRIX:\n" + solver);
        
        if (solver.solve()) {
            System.out.println("SOLVED MATRIX:\n" + solver);
        }
    }
    
}
