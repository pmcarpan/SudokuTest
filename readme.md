# A basic sudoku extractor + solver

### Requirements:

1. [OpenCV](https://opencv.org/)
    * [OpenCV for Java](https://opencv-java-tutorials.readthedocs.io/en/latest/)
2. [Tesseract](https://github.com/tesseract-ocr/tesseract) / [Tess4j](http://tess4j.sourceforge.net/)
    * [Tess4j installation guide](http://tess4j.sourceforge.net/tutorial/)

### Useful Classes:

1. SudokuExtractor - extracts the 2D matrix from given image
2. SingleDigitOCR - performs OCR to extract a single digit
3. SudokuSolver - tries to solve a given 2D sudoku matrix

### Sample Output:

```
ORIGINAL MATRIX:
-------------------------
| 0 0 0 | 6 0 4 | 7 0 0 | 
| 7 0 6 | 0 0 0 | 0 0 9 | 
| 0 0 0 | 0 0 5 | 0 8 0 | 
-------------------------
| 0 7 0 | 0 2 0 | 0 9 3 | 
| 8 0 0 | 0 0 0 | 0 0 5 | 
| 4 3 0 | 0 1 0 | 0 7 0 | 
-------------------------
| 0 5 0 | 2 0 0 | 0 0 0 | 
| 3 0 0 | 0 0 0 | 2 0 8 | 
| 0 0 2 | 3 0 1 | 0 0 0 | 
-------------------------

SOLVED MATRIX:
-------------------------
| 5 8 3 | 6 9 4 | 7 2 1 | 
| 7 1 6 | 8 3 2 | 5 4 9 | 
| 2 9 4 | 1 7 5 | 3 8 6 | 
-------------------------
| 6 7 1 | 5 2 8 | 4 9 3 | 
| 8 2 9 | 7 4 3 | 1 6 5 | 
| 4 3 5 | 9 1 6 | 8 7 2 | 
-------------------------
| 1 5 8 | 2 6 7 | 9 3 4 | 
| 3 6 7 | 4 5 9 | 2 1 8 | 
| 9 4 2 | 3 8 1 | 6 5 7 | 
-------------------------
```
